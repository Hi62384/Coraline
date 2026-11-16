"""Compile the production observer against test-only transport doubles, under normal RAM guards."""
import json,os,subprocess,threading,time,hashlib
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

root=monitor.ROOT
folder=root/'build-character-cast/playback-observation'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
sources=[root/'src/main/java/com/coraline/client/ShowPlaybackClient.java',*sorted((root/'tools/playback-observation-fixture').rglob('*.java'))]
commands={'compile':[str(monitor.JAVA/'javac.exe'),'-J-Xmx64m','-proc:none','-d',str(folder),*map(str,sources)],'observe':[str(monitor.JAVA/'java.exe'),'-Xmx32m','-cp',str(folder),'PlaybackObservationSmoke']}
report={'scope':'Production observer with test-only transport doubles; NOT Minecraft/network/audio/scene acceptance','source_sha256':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'steps':{}}
for label,command in commands.items():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
    api,handle=job(256)
    try:
        with (folder/(label+'.log')).open('xb',buffering=0) as log:
            child=subprocess.Popen(command,cwd=root,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise RuntimeError('Cannot bind owned unit test')
            reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            result=monitor.watch_child(child,90,monitor.free_gib,monitor.process_memory,log_limit=reached);reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():result['status']='capture_failed'
            report['steps'][label]=result
    finally:
        api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(monitor.tail_bytes(folder/(label+'.log')),flush=True)
    if result.get('status')!='passed' or result.get('exit_code')!=0:raise SystemExit(1)
print('Report: '+str(folder/'report.json'))
