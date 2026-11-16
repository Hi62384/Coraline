"""Stop only a positively identified, disposable packaged QA client.

Used for abandoned desktop-isolated launches and intentional QA disconnects.
Never targets a launcher, another application or an ordinary player save.
"""
from pathlib import Path
import argparse
import ctypes
from ctypes import wintypes
import datetime
import json
import subprocess
import check_numbered_candidate as monitor

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('report',type=Path);parser.add_argument('--pid',type=int)
args=parser.parse_args();path=args.report.resolve(strict=True)
if path.name!='report.json' or path.parent.parent!=(root/'build-character-cast/packaged-client').resolve():raise RuntimeError('Expected owned packaged QA report')
data=json.loads(path.read_text(encoding='utf-8'))
if data.get('production_jar_only') is not True or Path(data['game_directory']).resolve()!=path.parent:raise RuntimeError('Not a disposable packaged QA client')
pid=data.get('pid',args.pid)
if type(pid) is not int or pid<=0 or args.pid is not None and data.get('pid',args.pid)!=args.pid:raise RuntimeError('Missing/mismatched captured QA PID')
api=ctypes.WinDLL('kernel32',use_last_error=True)
api.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];api.OpenProcess.restype=wintypes.HANDLE
handle=api.OpenProcess(0x1001,False,pid)
if not handle:raise ctypes.WinError(ctypes.get_last_error())
try:
    api.QueryFullProcessImageNameW.argtypes=[wintypes.HANDLE,wintypes.DWORD,wintypes.LPWSTR,ctypes.POINTER(wintypes.DWORD)]
    image=ctypes.create_unicode_buffer(32768);length=wintypes.DWORD(len(image))
    if not api.QueryFullProcessImageNameW(handle,0,image,ctypes.byref(length)) or Path(image.value).resolve()!=(monitor.JAVA/'java.exe').resolve():raise RuntimeError('QA executable identity mismatch')
    api.GetProcessTimes.argtypes=[wintypes.HANDLE,*([ctypes.POINTER(wintypes.FILETIME)]*4)]
    times=[wintypes.FILETIME() for _ in range(4)]
    if not api.GetProcessTimes(handle,*[ctypes.byref(t) for t in times]):raise ctypes.WinError(ctypes.get_last_error())
    created=(((times[0].dwHighDateTime<<32)|times[0].dwLowDateTime)-116444736000000000)/1e7
    expected=data.get('process_started_epoch')
    if expected is None:expected=datetime.datetime.strptime(path.parent.name,'%Y%m%dT%H%M%SZ').replace(tzinfo=datetime.timezone.utc).timestamp()
    if abs(created-expected)>(5 if 'process_started_epoch' in data else 120):raise RuntimeError('Captured QA process creation time mismatch')
    result=subprocess.run([str(monitor.JAVA/'jcmd.exe'),str(pid),'VM.command_line'],capture_output=True,text=True,timeout=20)
    command=result.stdout.replace('/','\\').lower()
    if result.returncode or str(path.parent).lower() not in command or data.get('profile_name','PackagedQA').lower() not in command:raise RuntimeError('QA game directory/profile not confirmed in actual VM command')
    api.TerminateProcess.argtypes=[wintypes.HANDLE,wintypes.UINT]
    if not api.TerminateProcess(handle,1):raise ctypes.WinError(ctypes.get_last_error())
    print('Stopped only verified disposable QA client PID '+str(pid))
finally:
    api.CloseHandle.argtypes=[wintypes.HANDLE];api.CloseHandle(handle)
