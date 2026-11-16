"""Bounded transcription of existing originals only; never synthesizes speech."""
from pathlib import Path
import argparse, ctypes, json, subprocess, sys, threading, time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job
from owned_job_memory import measure as measure_owned_job
if hasattr(sys.stdout,'reconfigure'):sys.stdout.reconfigure(encoding='utf-8',errors='backslashreplace')

root=Path(__file__).resolve().parents[1]
voice_python=root/'.voice-tools/index-tts/.venv/Scripts/python.exe'
def start_floor(turbo,low_memory_attempt,model_limit_mib):
    if low_memory_attempt and not turbo:raise RuntimeError('Approved low-memory attempt is for the pinned Turbo draft check only')
    return monitor.START_FREE_GIB if low_memory_attempt else 4 if turbo else 2 if model_limit_mib==2048 else 4

parser=argparse.ArgumentParser();parser.add_argument('--base',action='store_true');parser.add_argument('--review-drafts',action='store_true');parser.add_argument('--turbo',action='store_true');parser.add_argument('--low-memory-attempt',action='store_true',help='Use the user-approved guarded voice low-memory mode; keep384MiB emergency stop and existing model/job limits.');parser.add_argument('--model-limit-mib',type=int,choices=[2048,3072],default=2048);args=parser.parse_args()
if args.turbo and (not args.review_drafts or args.base):raise RuntimeError('Turbo requires its own comparison draft mode')
if not voice_python.is_file():raise RuntimeError('The established local voice/ASR environment is missing')
start_gib=start_floor(args.turbo,args.low_memory_attempt,args.model_limit_mib)
if monitor.free_gib()<start_gib:raise RuntimeError(f'Original transcription needs {start_gib} GiB available before loading the cached model')
output=root/'build-character-cast/original-transcription'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())
output.mkdir(parents=True,exist_ok=False)
environment=monitor.child_environment()
environment['PATH']='C:\\ffmpeg;'+environment['PATH']
environment['PYTHONUTF8']='1'
command=[str(voice_python),str(root/'tools/build_story_subtitles.py'),'--transcribe','--originals-only','--memory-efficient']
if args.review_drafts:command=[str(voice_python),str(root/'tools/compare_original_transcripts.py')]
elif not args.base:command.append('--accurate')
if args.turbo:command.append('--turbo')
if args.low_memory_attempt:command.append('--low-memory-attempt')
commit_limit=8192 if args.turbo else args.model_limit_mib
api,handle=job(commit_limit)
report={'scope':'existing-source ASR only; no voice generation or human listening','command':command,'aggregate_limit_mib':commit_limit,'start_floor_gib':start_gib,'approved_low_memory_attempt':args.low_memory_attempt,'stop_free_mib':384,'note':'CUDA commit ceiling is not permission to exceed the physical-memory stop guard.'}
print('Original transcription report: '+str(output/'report.json'),flush=True)
try:
    with (output/'output.log').open('xb',buffering=0) as stream:
        child=subprocess.Popen(command,cwd=root,env=environment,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,bufsize=0)
        if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):
            child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
        reached,errors=threading.Event(),[]
        reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,stream,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
        report.update(monitor.watch_child(child,1800,monitor.free_gib,
            lambda process:measure_owned_job(api,handle,monitor.process_memory),log_limit=reached))
        report['memory_measurement_scope']='aggregate_owned_job_members'
        reader.join(timeout=10)
        if reader.is_alive() or errors or reached.is_set():report['status']='output_capture_failed'
finally:
    api.CloseHandle(handle)
    (output/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(monitor.tail_bytes(output/'output.log'),flush=True)
print('Report: '+str(output/'report.json'),flush=True)
raise SystemExit(0 if report.get('status')=='passed' and report.get('exit_code')==0 else 1)
