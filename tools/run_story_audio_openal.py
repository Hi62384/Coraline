"""Tiny guarded OpenAL observer test; no game, user saves, synthesis or resource edits."""
from pathlib import Path
import json,os,subprocess,threading,time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

root=monitor.ROOT
folder=root/'build-character-cast/story-audio-openal'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
deps=(root/monitor.CP).read_text(encoding='utf-8-sig').splitlines()
cp=os.pathsep.join([str(folder),*deps])
sources=['src/main/java/com/coraline/client/audio/'+name+'.java' for name in ('CloseOnceAudioStream','PlayedAudioFrames','ContinuousAudioStream','StoryAudioMixerCursor')]+['tools/StoryAudioOpenALSmoke.java']
commands={'compile':[str(monitor.JAVA/'javac.exe'),'-J-Xmx96m','-proc:none','-cp',cp,'-d',str(folder),*[str(root/name) for name in sources]],'mixer':[str(monitor.JAVA/'java.exe'),'-Xmx96m','-XX:MaxDirectMemorySize=32m','-cp',cp,'StoryAudioOpenALSmoke',str(root/'src/main/resources/assets/coraline')]}
report={'scope':'Real OpenAL consumed-original-frame observer only; muted, unity pitch; NOT Minecraft mixin/scene or audible acceptance','steps':{}}
for label,command in commands.items():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
    api,handle=job(384)
    try:
        with (folder/(label+'.log')).open('xb',buffering=0) as log:
            child=subprocess.Popen(command,cwd=root,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise RuntimeError('Cannot bind owned diagnostic process')
            reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            result=monitor.watch_child(child,180,monitor.free_gib,monitor.process_memory,log_limit=reached);reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():result['status']='capture_failed'
            report['steps'][label]=result
    finally:
        api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(monitor.tail_bytes(folder/(label+'.log')),flush=True)
    if result.get('status')!='passed' or result.get('exit_code')!=0:raise SystemExit(1)
print('Report: '+str(folder/'report.json'))
