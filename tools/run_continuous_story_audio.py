"""Bounded native decoder equality check against the actual existing story Oggs."""
from pathlib import Path
import json,os,subprocess,threading,time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

root=monitor.ROOT
folder=root/'build-character-cast/continuous-story-audio'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
deps=(root/monitor.CP).read_text(encoding='utf-8-sig').splitlines()
cp=os.pathsep.join([str(folder),str(root/'build-character-cast/classes/java/main'),*deps])
commands={
 'compile':[str(monitor.JAVA/'javac.exe'),'-J-Xmx128m','-proc:none','-cp',cp,'-d',str(folder),*[str(root/p) for p in ['src/main/java/com/coraline/client/audio/CloseOnceAudioStream.java','src/main/java/com/coraline/client/audio/PlayedAudioFrames.java','src/main/java/com/coraline/client/audio/ContinuousAudioStream.java','src/main/java/com/coraline/feast/StoryAudioContinuity.java','tools/ContinuousStoryAudioSmoke.java','tools/PlayedAudioFramesSmoke.java']]],
 'observer':[str(monitor.JAVA/'java.exe'),'-Xmx64m','-cp',cp,'PlayedAudioFramesSmoke'],
 'decode':[str(monitor.JAVA/'java.exe'),'-Xmx128m','-XX:MaxDirectMemorySize=64m','-cp',cp,'ContinuousStoryAudioSmoke',str(root/'src/main/resources/assets/coraline')]
}
report={'scope':'STB decoded PCM continuity; no audible OpenAL/scene acceptance','steps':{}}
for label,cmd in commands.items():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
    api,handle=job(512)
    try:
        with (folder/(label+'.log')).open('xb',buffering=0) as log:
            child=subprocess.Popen(cmd,cwd=root,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise RuntimeError('Cannot bind owned test process')
            reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            result=monitor.watch_child(child,180,monitor.free_gib,monitor.process_memory,log_limit=reached);reader.join(timeout=10)
            if reader.is_alive() or errors:result['status']='capture_failed'
            report['steps'][label]=result
    finally:
        api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(monitor.tail_bytes(folder/(label+'.log')),flush=True)
    if result.get('status')!='passed' or result.get('exit_code')!=0:raise SystemExit(1)
print('Report: '+str(folder/'report.json'))
