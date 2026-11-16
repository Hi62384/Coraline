"""Small isolated source/API + CPU mesh check. No Gradle, game, GL or mixer."""
from pathlib import Path
from ctypes import wintypes
import hashlib,json,os,subprocess,threading,time
import check_numbered_candidate as monitor
from run_low_memory_release import job

root=monitor.ROOT
folder=root/'build-character-cast/nautical-source-checks'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
cached=root/'build-character-cast/classes/java/main'
if not (cached/'com/coraline/feast/VisitData.class').is_file():raise RuntimeError('Compiled project API prerequisite missing')
def fingerprints():return {str(p.relative_to(cached)):hashlib.sha256(p.read_bytes()).hexdigest() for p in cached.rglob('*.class')}
before=fingerprints()
deps=[Path(line) for line in (root/monitor.CP).read_text(encoding='utf-8-sig').splitlines() if line]
allowed=Path(r'C:\Users\User1\.gradle\caches')
if not deps or any(not p.is_absolute() or not p.is_relative_to(allowed) or not p.is_file() or p.suffix.lower() not in ('.jar','.zip') for p in deps):raise RuntimeError('Unverified cached dependency path')
sources=[root/'src/main/java/com/coraline'/name for name in (
    'feast/NauticalOpeningPlan.java','feast/FinalChapterRecordings.java','feast/NauticalOpeningShow.java','feast/ShowDebugControls.java',
    'feast/VisitAudio.java','feast/TheatreVisit.java','client/ScenePreludeMusic.java','client/VisitEventClient.java',
    'client/ShowActorPoseClient.java','client/renderer/NauticalStageRenderer.java','client/renderer/TheatreOceanPresentation.java')]+[root/'tools/NauticalIntegrationSmoke.java',root/'src/wybietest/java/com/coraline/feast/NauticalOpeningGameTests.java']
sources += [root/'src/main/java/com/coraline'/name for name in (
    'palace/PalaceSitePlan.java','palace/PalaceSiteData.java','palace/PalaceJourneyTiming.java','palace/PalaceWellProgress.java','palace/PalaceKeyTransaction.java','palace/PalaceKeyCheckpoint.java',
    'feast/PalaceJourney.java','feast/PalaceKeyCustody.java','feast/RescueProps.java','client/renderer/RescuePresentation.java')]
sources += [root/'src/wybietest/java/com/coraline/feast/PalaceJourneyGameTests.java']
sources += [root/'src/main/java/com/coraline'/name for name in (
    'palace/PalaceGenerationRules.java','palace/PalaceTerrainPlan.java','palace/PalaceLandmarkPlan.java','palace/PalaceVirginTerrain.java','palace/PalaceWorldGeneration.java','otherworld/RosewoodHouse.java')]
sources += [root/'src/main/java/com/coraline'/name for name in ('feast/CinematicPropEntity.java','client/renderer/CinematicPropRenderer.java','client/renderer/PalaceSignPresentation.java')]
sources += [root/'src/wybietest/java/com/coraline/palace/PalaceGenerationGameTests.java']
sources += [root/'src/main/java/com/coraline'/name for name in ('palace/PalaceTransportProgress.java','feast/PalaceTransport.java','feast/PalaceCommands.java','client/renderer/PalaceTransportPresentation.java')]
sources += [root/'src/main/java/com/coraline'/name for name in ('palace/PalaceWybieIdentity.java','feast/CanonicalRealWybie.java','entity/CharacterEntity.java','wybie/WybieVisitorEntity.java','feast/ShowActors.java','feast/FirstEscapeStory.java')]
sources += [root/'src/wybietest/java/com/coraline/feast/CanonicalWybieGameTests.java']
sources += [root/'src/main/java/com/coraline'/name for name in ('palace/PalaceHandCombat.java','feast/PalaceWellMotion.java','feast/PalaceWellEncounter.java','feast/WybieBicycleEntity.java')]
sources += [root/'src/main/java/com/coraline/client/renderer/CharacterModel.java']
sources += [root/'src/wybietest/java/com/coraline/feast/PalaceWellGameTests.java']
cp=os.pathsep.join(map(str,[folder,cached,*deps]))
commands={'compile':[str(monitor.JAVA/'javac.exe'),'-J-Xmx96m','-J-XX:+UseSerialGC','-J-XX:MaxMetaspaceSize=80m','-J-XX:ReservedCodeCacheSize=24m','--release','17','-encoding','UTF-8','-proc:none','-implicit:none','-sourcepath',str(folder),'-cp',cp,'-d',str(folder),*map(str,sources)],
          'geometry':[str(monitor.JAVA/'java.exe'),'-Xmx64m','-XX:+UseSerialGC','-XX:MaxMetaspaceSize=64m','-XX:ReservedCodeCacheSize=24m','-cp',cp,'NauticalIntegrationSmoke']}
report={'scope':'Selected source against fingerprinted existing project/cached Minecraft API, then CPU mesh/blocking only; NOT whole-source, Gradle/Mixin, GL, game, audio playback, release or visual acceptance',
        'source_sha256':{str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'cached_api_sha256':before,'steps':{}}
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
            if fingerprints()!=before or any(hashlib.sha256((root/p).read_bytes()).hexdigest()!=digest for p,digest in report['source_sha256'].items()):result['status']='inputs_changed_during_check'
            if label=='geometry' and 'NAUTICAL_CPU_CHECKS_PASSED' not in (folder/(label+'.log')).read_text(errors='replace'):result['status']='assertion_marker_missing'
            report['steps'][label]=result
    finally:
        api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print(monitor.tail_bytes(folder/(label+'.log')),flush=True)
    if result.get('status')!='passed' or result.get('exit_code')!=0:raise SystemExit(1)
print('Report: '+str(folder/'report.json'))
