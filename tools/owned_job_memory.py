"""Read memory only for processes in the Windows job created by this attempt.

Windows venv launchers are tiny parent processes, not the model interpreter.
Never use their working set alone as the model's memory measurement. This is
diagnostic only: global available RAM and the kernel job cap remain the guards.
"""
import ctypes
from ctypes import wintypes
import types

class JobProcesses(ctypes.Structure):
    _fields_=[('assigned',wintypes.DWORD),('listed',wintypes.DWORD),('ids',ctypes.c_size_t*64)]


def aggregate(counters):
    if not counters:raise RuntimeError('No readable owned job process')
    keys=('peak_working_set_bytes','working_set_bytes','peak_pagefile_bytes','private_bytes')
    return {key:sum(row[key] for row in counters) for key in keys}


def measure(api,handle,read_process):
    # The PID inventory comes ONLY from our own job handle, never a global
    # process-name/parent-PID scan. Bound this two-process model tree to64PIDs.
    query=api.QueryInformationJobObject
    query.argtypes=[wintypes.HANDLE,ctypes.c_int,ctypes.c_void_p,wintypes.DWORD,ctypes.c_void_p]
    query.restype=wintypes.BOOL
    inventory=JobProcesses()
    if not query(handle,3,ctypes.byref(inventory),ctypes.sizeof(inventory),None):raise ctypes.WinError(ctypes.get_last_error())
    if inventory.assigned>64 or inventory.listed>64:raise RuntimeError('Owned model job exceeds bounded PID inventory')
    api.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD]
    api.OpenProcess.restype=wintypes.HANDLE
    counters=[]
    for pid in inventory.ids[:inventory.listed]:
        process=api.OpenProcess(0x410,False,pid)
        if not process:
            error=ctypes.get_last_error()
            if error==87:continue # This exact job member exited between queries.
            raise ctypes.WinError(error)
        try:counters.append(read_process(types.SimpleNamespace(_handle=process)))
        finally:api.CloseHandle(process)
    return dict(aggregate(counters),owned_processes=len(counters))
