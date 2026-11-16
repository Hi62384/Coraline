"""Compile with the task's idle voice controller held, without changing its
finite approval or any memory/voice settings. An independent watchdog owns the
hold and an exact duplicated handle to the suspended compilation runner.
"""
from pathlib import Path
import argparse,ctypes,json,os,subprocess,sys,time
ROOT=Path(__file__).resolve().parents[1]
REPORT=ROOT/'build-character-cast/verified-staged-audio/20261031T163603Z/report.json'
APPROVAL=ROOT/'build-character-cast/voice-window-approvals/human-renewal-20261031T163041Z.json'
def processes():
 script='Get-CimInstance Win32_Process | Select-Object ProcessId,ParentProcessId,CommandLine | ConvertTo-Json -Compress'
 result=subprocess.run(['powershell','-NoProfile','-NonInteractive','-Command',script],capture_output=True,text=True,check=True,creationflags=subprocess.CREATE_NO_WINDOW)
 rows=json.loads(result.stdout);return {int(row['ProcessId']):row for row in rows}
def descendants(rows,parent):
 found=set();todo=[parent]
 while todo:
  current=todo.pop()
  for pid,row in rows.items():
   if int(row['ParentProcessId'])==current and pid not in found:found.add(pid);todo.append(pid)
 return found
def api():
 from ctypes import wintypes as w
 kernel=ctypes.WinDLL('kernel32',use_last_error=True);native=ctypes.WinDLL('ntdll')
 kernel.OpenProcess.argtypes=[w.DWORD,w.BOOL,w.DWORD];kernel.OpenProcess.restype=w.HANDLE
 kernel.CloseHandle.argtypes=[w.HANDLE];kernel.CloseHandle.restype=w.BOOL
 kernel.WaitForSingleObject.argtypes=[w.HANDLE,w.DWORD];kernel.WaitForSingleObject.restype=w.DWORD
 kernel.TerminateProcess.argtypes=[w.HANDLE,w.UINT];kernel.TerminateProcess.restype=w.BOOL
 kernel.GetCurrentProcess.restype=w.HANDLE
 kernel.DuplicateHandle.argtypes=[w.HANDLE,w.HANDLE,w.HANDLE,ctypes.POINTER(w.HANDLE),w.DWORD,w.BOOL,w.DWORD];kernel.DuplicateHandle.restype=w.BOOL
 for name in ('NtSuspendProcess','NtResumeProcess'):
  method=getattr(native,name);method.argtypes=[w.HANDLE];method.restype=ctypes.c_long
 return kernel,native,w
def write(path,data):
 temporary=path.with_suffix('.pending');temporary.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8');os.replace(temporary,path)
def stop_child(kernel,handle):
 if handle and kernel.WaitForSingleObject(handle,0)!=0:
  kernel.TerminateProcess(handle,125)
  while kernel.WaitForSingleObject(handle,1000)!=0:time.sleep(.1)
def watchdog(control):
 kernel,native,w=api();state=json.loads(control.read_text(encoding='utf-8'));pid=int(state['controller']);rows=processes()
 if pid not in rows or 'finish_human_renewed_audio_20261031_163041.py' not in (rows[pid].get('CommandLine') or ''):raise RuntimeError('The approved controller is absent')
 voice=kernel.OpenProcess(0x00100800,False,pid);owner=kernel.OpenProcess(0x00100000,False,int(state['wrapper']));held=False;child=None
 if not voice or not owner:raise ctypes.WinError(ctypes.get_last_error())
 try:
  if native.NtSuspendProcess(voice)!=0:raise RuntimeError('The idle hold was refused')
  held=True;rows=processes();latest=json.loads(REPORT.read_text(encoding='utf-8'))
  if descendants(rows,pid) or latest.get('status')!='waiting_for_headroom':raise RuntimeError('A voice worker was admitted; compilation must wait')
  (control.parent/'watchdog.ready').write_text('idle and held',encoding='utf-8')
  while True:
   exited=kernel.WaitForSingleObject(owner,0)==0
   try:state=json.loads(control.read_text(encoding='utf-8'))
   except (OSError,ValueError):
    if exited:break
    time.sleep(.1);continue
   if child is None and state.get('compile_handle'):
    child=w.HANDLE(int(state['compile_handle']));(control.parent/'child.received').write_text(str(state['compile_pid']),encoding='utf-8')
   if exited or state.get('state')=='release':break
   time.sleep(.1)
 finally:
  # A heavy child is resumed only after this watchdog owns its exact handle.
  # Killing its guarded runner closes the existing kill-on-close Gradle job.
  stop_child(kernel,child)
  if child:kernel.CloseHandle(child)
  if held:native.NtResumeProcess(voice)
  (control.parent/'watchdog.resumed').write_text('same controller resumed',encoding='utf-8')
  kernel.CloseHandle(owner);kernel.CloseHandle(voice)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--watchdog',type=Path);parser.add_argument('--cached-sources',action='store_true',help='Compile every main source against cached dependencies; no Mixin or release acceptance.');parser.add_argument('--worker-sources',action='store_true',help='Compile the three current worker edits against an unchanged-source-pinned successful base.');args=parser.parse_args()
 if args.worker_sources and args.cached_sources:raise RuntimeError('Choose one compilation scope')
 if args.watchdog:return watchdog(args.watchdog)
 approval=json.loads(APPROVAL.read_text(encoding='utf-8'));report=json.loads(REPORT.read_text(encoding='utf-8'))
 if approval.get('human_authorization')!='Approved another six-hour guarded voice window via trusted user-authorization relay' or approval.get('window_seconds')!=21600 or approval.get('consumed') is not True or report.get('retry_failed_models') is not False:raise RuntimeError('The original approval changed')
 if approval.get('opened_utc')!='2026-10-31T16:30:41Z' or approval.get('expires_utc')!='2026-10-31T22:30:41Z' or report.get('completion_window_started_utc')!=approval['opened_utc'] or report.get('required_start_free_gib')!=3.875 or report.get('quality_start_free_gib')!=2 or report.get('worker_max_new_takes')!=1:raise RuntimeError('The fixed approved window or guards changed')
 import check_numbered_candidate as memory
 free=memory.free_gib()
 if free<2:raise RuntimeError(f'Compilation needs the unchanged 2 GiB start floor; measured {free:.3f}')
 pid=int(report['controller_pid']);rows=processes();row=rows.get(pid)
 if row is None:raise RuntimeError('Controller exited; inspect its report before using the ordinary build launcher')
 if 'finish_human_renewed_audio_20261031_163041.py' not in (row.get('CommandLine') or '') or report.get('status')!='waiting_for_headroom' or descendants(rows,pid):raise RuntimeError('This task voice controller is not idle')
 kernel,native,w=api();voice=kernel.OpenProcess(0x00100800,False,pid)
 if not voice:raise ctypes.WinError(ctypes.get_last_error())
 folder=ROOT/'build-character-cast/idle-voice-compile'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False);control=folder/'control.json'
 state={'controller':pid,'wrapper':os.getpid(),'state':'preparing','compile_pid':None,'compile_handle':None,'approval_expiry_unchanged':True};write(control,state)
 helper=subprocess.Popen([sys.executable,str(Path(__file__).resolve()),'--watchdog',str(control)],cwd=ROOT,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,creationflags=subprocess.CREATE_NO_WINDOW);child=None;target=None;stream=None
 try:
  limit=time.monotonic()+20
  while not (folder/'watchdog.ready').exists():
   if helper.poll() is not None or time.monotonic()>limit:raise RuntimeError('The idle controller could not be held safely')
   time.sleep(.1)
  print('Same idle voice controller held; its fixed approval deadline is unchanged.',flush=True)
  command=[sys.executable,'tools/run_low_memory_release.py','production-compile','--lean-compile','--genuine-audio','--minimum-start-free-mib','2048','--stacktrace']
  if args.cached_sources:command=[sys.executable,'tools/compile_main_sources.py']
  if args.worker_sources:command=[sys.executable,'tools/compile_current_worker_sources.py']
  state['compile_mode']='worker-edits-pinned-base' if args.worker_sources else 'all-main-cached-apis' if args.cached_sources else 'production-gradle';write(control,state)
  stream=(folder/'compile.log').open('xb',buffering=0)
  child=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW|0x00000004)
  target=kernel.OpenProcess(0x0040,False,helper.pid);duplicate=w.HANDLE()
  if not target or not kernel.DuplicateHandle(kernel.GetCurrentProcess(),w.HANDLE(int(child._handle)),target,ctypes.byref(duplicate),0x00100001,False,0):raise ctypes.WinError(ctypes.get_last_error())
  state.update(compile_pid=child.pid,compile_handle=int(duplicate.value));write(control,state)
  limit=time.monotonic()+10
  while not (folder/'child.received').exists():
   if helper.poll() is not None or time.monotonic()>limit:raise RuntimeError('The compilation watchdog did not acknowledge its child handle')
   time.sleep(.1)
  if native.NtResumeProcess(w.HANDLE(int(child._handle)))!=0:raise RuntimeError('The guarded compilation runner could not start')
  while child.poll() is None:
   if helper.poll() is not None:raise RuntimeError('Resume watchdog exited; stop compilation before releasing the voice controller')
   time.sleep(.25)
  state['compile_exit_code']=child.returncode;write(control,state);print('Guarded compilation exit:',child.returncode,flush=True)
 finally:
  if child:stop_child(kernel,w.HANDLE(int(child._handle)))
  try:state['state']='release';write(control,state)
  finally:
   try:helper.wait(timeout=30)
   except subprocess.TimeoutExpired:native.NtResumeProcess(voice)
   if not (folder/'watchdog.resumed').exists():native.NtResumeProcess(voice)
   if stream:stream.close()
   if target:kernel.CloseHandle(target)
   kernel.CloseHandle(voice)
   print('The original voice controller is released; no approval was extended.',flush=True)
 if state.get('compile_exit_code')!=0:raise SystemExit(state.get('compile_exit_code',1))
if __name__=='__main__':main()
