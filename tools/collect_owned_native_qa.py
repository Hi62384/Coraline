"""Request one GC of a positively identified owned native QA JVM only.

Reclaims loading garbage before a second client. Does not alter memory guards,
heap limits, other processes, world data or machine settings.
"""
import ctypes,datetime,json,subprocess
from ctypes import wintypes
from pathlib import Path
import argparse
import check_numbered_candidate as monitor

root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('report',type=Path);p.add_argument('--finish-stepwise-shrink',action='store_true',help='Finish Serial GC gradual heap release, at most four total collections');args=p.parse_args();report=args.report.resolve(strict=True)
if report.name!='report.json' or report.parent.parent!=(root/'build-character-cast/prepared-native').resolve():raise RuntimeError('Expected owned native QA report')
data=json.loads(report.read_text());pid=data.get('pid');cwd=Path(data['cwd']).resolve()
if type(pid) is not int or pid<=0 or cwd.parent!=root or not cwd.name.startswith('run-tests-isolated-'):raise RuntimeError('Invalid owned native launch')
stamp=report.parent/'explicit-gc.json'
previous=json.loads(stamp.read_text()) if stamp.exists() else None
if previous and (not args.finish_stepwise_shrink or previous.get('stepwise_shrink_finished')):raise RuntimeError('Owned QA collection budget already used')
if args.finish_stepwise_shrink and (not previous or previous.get('pid')!=pid or previous.get('verified_owned_native_launch')!=str(report)):raise RuntimeError('Expected same owned QA initial collection')
api=ctypes.WinDLL('kernel32',use_last_error=True)
api.OpenProcess.argtypes=[wintypes.DWORD,wintypes.BOOL,wintypes.DWORD];api.OpenProcess.restype=wintypes.HANDLE
handle=api.OpenProcess(0x1000,False,pid)
if not handle:raise ctypes.WinError(ctypes.get_last_error())
try:
    api.QueryFullProcessImageNameW.argtypes=[wintypes.HANDLE,wintypes.DWORD,wintypes.LPWSTR,ctypes.POINTER(wintypes.DWORD)]
    image=ctypes.create_unicode_buffer(32768);size=wintypes.DWORD(len(image))
    if not api.QueryFullProcessImageNameW(handle,0,image,ctypes.byref(size)) or Path(image.value).resolve()!=(monitor.JAVA/'java.exe').resolve():raise RuntimeError('Owned QA executable mismatch')
    api.GetProcessTimes.argtypes=[wintypes.HANDLE,*([ctypes.POINTER(wintypes.FILETIME)]*4)]
    times=[wintypes.FILETIME() for _ in range(4)]
    if not api.GetProcessTimes(handle,*[ctypes.byref(t) for t in times]):raise ctypes.WinError(ctypes.get_last_error())
    created=(((times[0].dwHighDateTime<<32)|times[0].dwLowDateTime)-116444736000000000)/1e7
    expected=datetime.datetime.strptime(report.parent.name,'%Y%m%dT%H%M%SZ').replace(tzinfo=datetime.timezone.utc).timestamp()
    if abs(created-expected)>120:raise RuntimeError('Owned QA creation time mismatch')
    cmd=[str(monitor.JAVA/'jcmd.exe'),str(pid)]
    identity=subprocess.run([*cmd,'VM.command_line'],capture_output=True,text=True,timeout=20)
    command=identity.stdout.replace('/','\\').lower()
    class_root=Path(data['isolated_compiler_report']).resolve().parent/'classes'
    if data.get('connected_class_staging_report'):
        staged=Path(data['connected_class_staging_report']).resolve(strict=True)
        if staged.parent.parent!=(root/'build-character-cast/connected-class-staging').resolve() or staged.name!='staging-report.json':raise RuntimeError('Unexpected staging identity')
        if json.loads(staged.read_text()).get('original')!=str(class_root):raise RuntimeError('Staged compiler identity mismatch')
        class_root=staged.parent
    if identity.returncode or '-dcoraline.connectedtransactionscheck=true' not in command or str(class_root).lower() not in command:raise RuntimeError('Owned transaction QA command not confirmed')
    before=monitor.free_gib();collections=[]
    # Serial GC normally returns unused heap in progressively larger steps.
    # Bound these requests; never change the heap, admission or emergency limits.
    for _ in range(3 if args.finish_stepwise_shrink else 1):
        result=subprocess.run([*cmd,'GC.run'],capture_output=True,text=True,timeout=30)
        collections.append({'exit_code':result.returncode,'result':result.stdout.strip(),'free_gib_after':monitor.free_gib()})
        if result.returncode or monitor.free_gib()>=3.15:break
    stamp.write_text(json.dumps({'pid':pid,'verified_owned_native_launch':str(report),'free_gib_before':before,'free_gib_after':monitor.free_gib(),'exit_code':result.returncode,'collections':collections,'initial_collection':previous,'stepwise_shrink_finished':args.finish_stepwise_shrink},indent=2)+'\n')
    print(stamp.read_text())
    if result.returncode:raise RuntimeError('Owned QA collection did not complete')
finally:
    api.CloseHandle.argtypes=[wintypes.HANDLE];api.CloseHandle(handle)
