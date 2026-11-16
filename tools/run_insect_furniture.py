"""Run only our background Blender with the existing RAM and bounded-log guards."""
import ctypes, json, subprocess, threading, time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

def main():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
    folder=monitor.ROOT/'artwork/insect_furniture_v2'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    api,handle=job(1024)
    try:
        with (folder/'blender.log').open('xb',buffering=0) as output:
            child=subprocess.Popen([r'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe','--background','--factory-startup','--threads','1','--python',str(monitor.ROOT/'tools/build_insect_furniture.py')],cwd=monitor.ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,bufsize=0)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
            reached,errors=threading.Event(),[]
            reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,output,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            result=monitor.watch_child(child,240,monitor.free_gib,monitor.process_memory,log_limit=reached);reader.join(timeout=10)
            if reader.is_alive() or errors:result['status']='capture_failed'
        (folder/'report.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf8')
        print(monitor.tail_bytes(folder/'blender.log'));print(json.dumps(result,indent=2))
        return 0 if result.get('status')=='passed' and result.get('exit_code')==0 else 1
    finally:api.CloseHandle(handle)
if __name__=='__main__':raise SystemExit(main())
