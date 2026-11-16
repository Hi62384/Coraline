"""Small guarded planner/NBT check; no game, Gradle, terrain or user-save access."""
from pathlib import Path
import hashlib,json,os,subprocess,threading,time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job
from inspect_final_chapter_recordings import inspect_recordings

root=monitor.ROOT
folder=root/'build-character-cast/palace-site-checks'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
deps=[Path(line) for line in (root/monitor.CP).read_text(encoding='utf-8-sig').splitlines() if line]
allowed=Path(r'C:\Users\User1\.gradle\caches')
if not deps or any(not p.is_absolute() or not p.is_relative_to(allowed) or not p.is_file() or p.suffix.lower() not in ('.jar','.zip') for p in deps):raise RuntimeError('Unverified cached dependency path')
cp=os.pathsep.join([str(folder),*map(str,deps)])
recordings=inspect_recordings();recording_file=folder/'supplied-recordings.json';recording_file.write_text(json.dumps(recordings,indent=2)+'\n',encoding='utf8')
sources=[root/'src/main/java/com/coraline/palace'/name for name in ('PalaceSitePlan.java','PalaceSiteData.java','PalaceJourneyTiming.java','PalaceWellProgress.java','PalaceKeyTransaction.java','PalaceKeyCheckpoint.java')]+[root/'src/main/java/com/coraline/feast/NauticalOpeningPlan.java',root/'src/main/java/com/coraline/feast/FinalChapterRecordings.java',root/'tools/PalaceSiteSmoke.java',root/'tools/PalaceWellSmoke.java',root/'tools/PalaceKeySmoke.java']
sources += [root/'src/main/java/com/coraline/palace'/name for name in ('PalaceGenerationRules.java','PalaceTerrainPlan.java','PalaceLandmarkPlan.java','PalaceVirginTerrain.java')]
sources += [root/'tools/PalaceGenerationSmoke.java']
sources += [root/'src/main/java/com/coraline/palace/PalaceTransportProgress.java',root/'tools/PalaceTransportSmoke.java']
sources += [root/'src/main/java/com/coraline/palace/PalaceWybieIdentity.java',root/'tools/PalaceWybieSmoke.java']
sources += [root/'src/main/java/com/coraline/palace/PalaceHandCombat.java',root/'tools/PalaceCombatSmoke.java']
commands={'compile':[str(monitor.JAVA/'javac.exe'),'-J-Xmx96m','-J-XX:+UseSerialGC','-J-XX:MaxMetaspaceSize=64m','-J-XX:ReservedCodeCacheSize=24m','--release','17','-proc:none','-implicit:none','-sourcepath',str(folder),'-cp',cp,'-d',str(folder),*map(str,sources)],
          'rules':[str(monitor.JAVA/'java.exe'),'-Xmx64m','-XX:+UseSerialGC','-XX:MaxMetaspaceSize=64m','-XX:ReservedCodeCacheSize=24m','-cp',cp,'PalaceSiteSmoke',str(recording_file)]}
report={'scope':'Actual deterministic planner/NBT reload, owner-bound well progression and original-duration choreography math only; NOT whole mod compile, Minecraft, worldgen, terrain, LOS, inventory custody, speaker-content review or chapter acceptance','source_sha256':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'original_recordings':recordings,'steps':{}}
for label,command in commands.items():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
    api,handle=job(384)
    try:
        with (folder/(label+'.log')).open('xb',buffering=0) as log:
            child=subprocess.Popen(command,cwd=root,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise RuntimeError('Cannot bind owned small check')
            reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            result=monitor.watch_child(child,90,monitor.free_gib,monitor.process_memory,log_limit=reached);reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():result['status']='capture_failed'
            if any(hashlib.sha256((root/p).read_bytes()).hexdigest()!=digest for p,digest in report['source_sha256'].items()):result['status']='source_changed_during_check'
            if label=='rules' and 'PALACE_SITE_RULES_PASSED' not in (folder/(label+'.log')).read_text(errors='replace'):result['status']='assertion_marker_missing'
            report['steps'][label]=result
    finally:
        api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(monitor.tail_bytes(folder/(label+'.log')),flush=True)
    if result.get('status')!='passed' or result.get('exit_code')!=0:raise SystemExit(1)
print('Report: '+str(folder/'report.json'))
