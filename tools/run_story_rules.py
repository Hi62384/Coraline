"""Tiny pure timeline/rule checks with the existing RAM guard and owned job."""
import ctypes,json,os,subprocess,threading,time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

root=monitor.ROOT
out=root/'build-character-cast/story-rules'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());out.mkdir(parents=True,exist_ok=False)
sources=['src/main/java/com/coraline/feast/CocoaBeetlesChoreography.java','src/main/java/com/coraline/feast/RescueChallengeMath.java','src/main/java/com/coraline/feast/RescueWebLayout.java','src/main/java/com/coraline/feast/StoryPlaybackClock.java','src/wybietest/java/com/coraline/feast/CocoaBeetlesChoreographySmoke.java','src/wybietest/java/com/coraline/feast/RescueChallengeRulesSmoke.java','src/wybietest/java/com/coraline/feast/RescueWebRulesSmoke.java']
commands={'compile':[str(monitor.JAVA/'javac.exe'),'-J-Xmx96m','-proc:none','-d',str(out),*[str(root/name) for name in sources]]}
for name in ('CocoaBeetlesChoreographySmoke','RescueChallengeRulesSmoke','RescueWebRulesSmoke'):commands[name]=[str(monitor.JAVA/'java.exe'),'-Xmx64m','-cp',str(out),'com.coraline.feast.'+name]
report={'scope':'Pure deterministic choreography/challenge rules only; not Minecraft acceptance','steps':{}}
for label,command in commands.items():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
    api,handle=job(256)
    try:
        with (out/(label+'.log')).open('xb',buffering=0) as log:
            child=subprocess.Popen(command,cwd=root,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
            reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            result=monitor.watch_child(child,90,monitor.free_gib,monitor.process_memory,log_limit=reached);reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():result['status']='capture_failed'
            report['steps'][label]=result
    finally:
        api.CloseHandle(handle);(out/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(monitor.tail_bytes(out/(label+'.log')),flush=True)
    if result.get('status')!='passed' or result.get('exit_code')!=0:raise SystemExit(1)
print('Report: '+str(out/'report.json'))
