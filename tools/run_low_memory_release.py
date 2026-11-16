"""Serial capped Gradle attempt; only our Windows job tree may be stopped.

Uses the installed 8.8 distribution, a tiny launcher and a 224/384 MiB build
heap, stage-specific aggregate job limits, and the established 640/384 MiB
available-memory start/stop thresholds. Tests run separately to release Gradle's
compiler memory first. Packaging still performs normal resource optimization.
No synthesis, application shutdown, machine settings or persistent JVM changes.
"""
from pathlib import Path
import argparse, ctypes, json, os, subprocess, threading, time
from ctypes import wintypes
import check_numbered_candidate as monitor

ROOT=Path(__file__).resolve().parents[1]
DIST=Path(r'C:\Users\User1\.gradle\wrapper\dists\gradle-8.8-bin\dl7vupf4psengwqhwktix4v1\gradle-8.8')

def trim_owned_working_set(process):
    """Evict only the exact Popen child's resident pages; Windows reloads them normally."""
    empty=ctypes.WinDLL('psapi',use_last_error=True).EmptyWorkingSet
    empty.argtypes=[wintypes.HANDLE];empty.restype=wintypes.BOOL
    if not empty(wintypes.HANDLE(int(process._handle))):raise ctypes.WinError(ctypes.get_last_error())

def compact_memory_probe(process,report):
    last_trim=[float('-inf')]
    def available():
        free=monitor.free_gib()
        prior=report.get('minimum_free_before_trim_gib')
        report['minimum_free_before_trim_gib']=free if prior is None else min(prior,free)
        # Never rescue a measurement below the emergency floor: return it
        # unchanged so the existing monitor immediately stops this job.
        if free<monitor.STOP_FREE_GIB or process.poll() is not None:return free
        now=time.monotonic()
        # Reclaim our resident pages before a competing app's allocation pushes
        # the machine near the emergency floor. This never changes that floor.
        if free<1.5 and now-last_trim[0]>=.25 and monitor.process_memory(process)['working_set_bytes']>=64*1024**2:
            trim_owned_working_set(process);last_trim[0]=now
            report['owned_working_set_trims']=report.get('owned_working_set_trims',0)+1
            return monitor.free_gib()
        return free
    return available
class Basic(ctypes.Structure):
    _fields_=[('ProcessTime',ctypes.c_longlong),('JobTime',ctypes.c_longlong),('Flags',wintypes.DWORD),('MinWorking',ctypes.c_size_t),('MaxWorking',ctypes.c_size_t),('ActiveLimit',wintypes.DWORD),('Affinity',ctypes.c_size_t),('Priority',wintypes.DWORD),('Scheduling',wintypes.DWORD)]
class IO(ctypes.Structure):
    _fields_=[(name,ctypes.c_ulonglong) for name in ('ReadOps','WriteOps','OtherOps','ReadBytes','WriteBytes','OtherBytes')]
class Limits(ctypes.Structure):
    _fields_=[('Basic',Basic),('IO',IO),('ProcessMemory',ctypes.c_size_t),('JobMemory',ctypes.c_size_t),('PeakProcessMemory',ctypes.c_size_t),('PeakJobMemory',ctypes.c_size_t)]
def job(limit_mib=960):
    api=ctypes.WinDLL('kernel32',use_last_error=True)
    api.CreateJobObjectW.argtypes=[ctypes.c_void_p,wintypes.LPCWSTR];api.CreateJobObjectW.restype=wintypes.HANDLE
    api.SetInformationJobObject.argtypes=[wintypes.HANDLE,ctypes.c_int,ctypes.c_void_p,wintypes.DWORD];api.SetInformationJobObject.restype=wintypes.BOOL
    api.AssignProcessToJobObject.argtypes=[wintypes.HANDLE,wintypes.HANDLE];api.AssignProcessToJobObject.restype=wintypes.BOOL
    api.CloseHandle.argtypes=[wintypes.HANDLE];api.CloseHandle.restype=wintypes.BOOL
    handle=api.CreateJobObjectW(None,None)
    if not handle:raise ctypes.WinError(ctypes.get_last_error())
    limits=Limits();limits.Basic.Flags=0x2000|0x200;limits.JobMemory=limit_mib*1024*1024
    if not api.SetInformationJobObject(handle,9,ctypes.byref(limits),ctypes.sizeof(limits)):
        error=ctypes.get_last_error();api.CloseHandle(handle);raise ctypes.WinError(error)
    return api,handle
def ready_memory(minimum_mib,wait_seconds=0):
    """Optional HIGHER launch floor; never relax the established start/stop guard."""
    if minimum_mib not in (640,1024,1536,2048,3072) or wait_seconds not in (0,30,45):raise RuntimeError('Unsupported bounded memory gate')
    floor=max(monitor.START_FREE_GIB,minimum_mib/1024);free=monitor.free_gib()
    if free>=floor:return free
    if not wait_seconds:raise RuntimeError('Insufficient memory for the selected start floor; no build child started')
    print(f'Waiting at most {wait_seconds}s for {minimum_mib} MiB before build launch; stop guard unchanged.',flush=True)
    deadline=time.monotonic()+wait_seconds
    while free<floor:
        if time.monotonic()>=deadline:raise RuntimeError('Selected build start floor unavailable; no build child started')
        time.sleep(1);free=monitor.free_gib()
    return free

def main():
    parser=argparse.ArgumentParser();parser.add_argument('stage',choices=['qa-resources','compile','production-compile','native','native-ready','render','render-ready','package','finish-reobf']);parser.add_argument('--test-classes');parser.add_argument('--export-only',action='store_true');parser.add_argument('--lean-package',action='store_true',help='Use a 256 MiB Gradle heap for a serial package attempt; all normal resource/check tasks remain enabled.');parser.add_argument('--lean-prepare',action='store_true');parser.add_argument('--lean-compile',action='store_true',help='Use a smaller compiler build heap while preserving admission, job and emergency limits.');parser.add_argument('--render-classes');parser.add_argument('--single-jvm',action='store_true',help='Request matching launcher/build options; Gradle may still fork a single-use daemon.');parser.add_argument('--reuse-verified-audio',metavar='AUDITED_JAR_SHA256',help='Validate/reuse existing recordings and captions; still run normal resource optimizer/checks.');parser.add_argument('--audited-audio-jar',help='Previous release inside build-character-cast/libs; requires its exact --reuse-verified-audio SHA256.')
    parser.add_argument('--minimum-start-free-mib',type=int,choices=[640,1024,1536,2048,3072],default=640)
    parser.add_argument('--stacktrace',action='store_true',help='Include Gradle startup/compiler diagnostics without changing tasks or safety caps.')
    parser.add_argument('--genuine-audio',action='store_true',help='Require complete genuine dialogue and reviewed captions; no temporary-audio property or placeholder archive label.')
    parser.add_argument('--compact-driver',action='store_true',help='Use a smaller Gradle driver for serial production compilation/packaging; compiler, tasks and memory guards unchanged.')
    parser.add_argument('--compact-heap-mib',type=int,choices=(192,256,384),default=192,help='Bound the single compact build heap; 384 MiB supports the full production compiler and annotation processors.')
    parser.add_argument('--build-only',action='store_true',help='Explicitly skip test suites for a genuine release; retain compilation, normal resource optimization and Forge reobfuscation.')
    parser.add_argument('--memory-wait-seconds',type=int,choices=[0,30,45],default=0)
    parser.add_argument('--export-imports',help='Export actual genuine audio task graph only; do not execute any build task.')
    parser.add_argument('--prepared-imports',help='Use the completed exact serial import graph after checking every input/output hash.')
    args=parser.parse_args()
    if args.build_only and not (args.stage=='package' and args.genuine_audio):raise RuntimeError('Build-only requires genuine release packaging')
    if (args.export_imports or args.prepared_imports) and (args.stage!='package' or not args.genuine_audio or args.export_imports and args.prepared_imports):raise RuntimeError('Serialized import mode requires one genuine release mode')
    if args.genuine_audio and (args.reuse_verified_audio or args.audited_audio_jar):raise RuntimeError('Genuine release must run the complete current import/caption pipeline')
    if args.lean_compile and args.stage!='production-compile':raise RuntimeError('Lean compilation is production-source-only')
    if args.compact_driver and args.stage not in ('production-compile','package'):raise RuntimeError('Compact driver is production build only')
    if args.lean_package and args.stage!='package':raise RuntimeError('Lean packaging is package-only')
    if args.reuse_verified_audio and not (args.stage=='package' or args.stage in ('native','render') and args.export_only):raise RuntimeError('Prepared audio reuse requires packaging or export-only resource preparation')
    if args.audited_audio_jar and not args.reuse_verified_audio:raise RuntimeError('An audited audio JAR requires its SHA256 pin')
    if args.test_classes and (args.stage not in ('native','native-ready') or not __import__('re').fullmatch(r'[A-Za-z0-9_]+GameTests\.java(?:,[A-Za-z0-9_]+GameTests\.java)*',args.test_classes)):raise RuntimeError('Invalid QA-only class selection')
    if ROOT!=monitor.ROOT or os.name!='nt':raise RuntimeError('Unexpected workspace/platform')
    free=ready_memory(args.minimum_start_free_mib,args.memory_wait_seconds)
    native=args.stage in ('native','native-ready','render','render-ready')
    if args.lean_prepare and (not native or not args.export_only):raise RuntimeError('Lean preparation is export-only QA')
    if args.render_classes and (args.stage not in ('render','render-ready') or not __import__('re').fullmatch(r'[A-Za-z0-9_]+VisualCheck\.java(?:,[A-Za-z0-9_]+VisualCheck\.java)*',args.render_classes)):raise RuntimeError('Invalid render harness selection')
    job_limit=2048 if native else 1536 if args.stage=='package' else 1280 if args.stage=='finish-reobf' else 960
    caps=['-Xms32m','-Xmx224m','-XX:+UseSerialGC','-Xss512k','-XX:MaxMetaspaceSize=160m','-XX:ReservedCodeCacheSize=32m','-XX:MaxDirectMemorySize=64m'] if args.stage=='finish-reobf' else ['-Xms32m','-Xmx384m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=4','-XX:MaxMetaspaceSize=192m','-XX:ReservedCodeCacheSize=64m','-XX:MaxDirectMemorySize=96m']
    if args.lean_package:caps=['-Xms32m','-Xmx256m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:MaxMetaspaceSize=160m','-XX:ReservedCodeCacheSize=32m','-XX:MaxDirectMemorySize=64m']
    if args.lean_compile:caps=['-Xms32m','-Xmx256m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:MaxMetaspaceSize=160m','-XX:ReservedCodeCacheSize=32m','-XX:MaxDirectMemorySize=64m']
    if args.lean_prepare:caps=['-Xms32m','-Xmx256m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:MaxMetaspaceSize=160m','-XX:ReservedCodeCacheSize=32m','-XX:MaxDirectMemorySize=64m']
    if args.compact_driver:
        caps=['-Xms16m','-Xmx192m','-XX:+UseSerialGC','-XX:MinHeapFreeRatio=10','-XX:MaxHeapFreeRatio=20','-XX:-ShrinkHeapInSteps','-XX:ActiveProcessorCount=2','-XX:MaxMetaspaceSize=160m','-XX:ReservedCodeCacheSize=32m','-XX:MaxDirectMemorySize=32m','-Djdk.nio.maxCachedBufferSize=1048576']
        caps[1]='-Xmx'+str(args.compact_heap_mib)+'m'
    opens=['java.base/java.util','java.base/java.lang','java.base/java.lang.invoke','java.prefs/java.util.prefs','java.base/java.nio.charset','java.base/java.net','java.base/java.util.concurrent.atomic']
    # Gradle may require a single-use daemon even with matching launcher flags.
    # Keep the client tiny instead of allocating a second full build heap.
    launcher_caps=['-Xms16m','-Xmx64m','-XX:+UseSerialGC','-Xss512k','-XX:MaxMetaspaceSize=96m','-XX:ReservedCodeCacheSize=32m','-XX:MaxDirectMemorySize=16m']
    # Matching removes one reason to fork, but Gradle may still need a daemon
    # for instrumentation/default options. Never claim a single process solely
    # because this option was requested.
    if args.single_jvm:launcher_caps=list(caps)
    single_owner_vm=args.single_jvm and args.stage in ('production-compile','package','finish-reobf') and not args.lean_package
    vm=[*[f'--add-opens={part}=ALL-UNNAMED' for part in opens],*([] if single_owner_vm else launcher_caps),'-Dfile.encoding=UTF-8','-Duser.country=US','-Duser.language=en','-Duser.variant=']
    command=[str(monitor.JAVA/'java.exe'),*vm,'-javaagent:'+str(DIST/'lib/agents/gradle-instrumentation-agent-8.8.jar'),'-classpath',str(DIST/'lib/gradle-launcher-8.8.jar'),'org.gradle.launcher.GradleMain','--offline','--no-daemon','--max-workers=1','-Dorg.gradle.jvmargs='+' '.join(caps),'--project-cache-dir','.gradle-character-cast','-PisolatedActivityBuild','-PcharacterCastBuild']
    if args.lean_compile or args.lean_package:command.append('-PisolatedLowMemoryCompiler')
    if single_owner_vm and (args.compact_driver or args.stage=='finish-reobf'):command.append('-PcompactSingleRelease')
    for value,property_name in ((args.export_imports,'exportSerializedImports'),(args.prepared_imports,'preparedSerializedImports')):
        if value:
            path=Path(value).resolve()
            if not path.is_relative_to(ROOT/'build-character-cast') or path.suffix!='.json':raise RuntimeError('Serialized import path escaped owned build output')
            command.append('-P'+property_name+'='+str(path))
    if args.stacktrace:command.append('--stacktrace')
    if args.stage=='qa-resources' and not args.genuine_audio:raise RuntimeError('Code-only QA must not invoke the temporary audio pipeline')
    if not args.genuine_audio:command.append('-PtemporaryEventAudio')
    if args.stage=='finish-reobf':
        # Resume only the final failed tool, not an assembly from partial classes.
        # Require the successful normal optimizer output and exact completed jar.
        import zipfile
        version=__import__('re').search(r'^mod_version=([A-Za-z0-9._-]+)$',(ROOT/'gradle.properties').read_text(),__import__('re').MULTILINE)
        if not version:raise RuntimeError('Missing safe release version')
        jar_path=ROOT/'build-character-cast/libs'/('coraline-'+version[1]+('' if args.genuine_audio else '-placeholder-audio')+'.jar')
        asset_report=ROOT/'build-character-cast/reports/asset-size.json'
        if not asset_report.is_file() or asset_report.stat().st_mtime>jar_path.stat().st_mtime:raise RuntimeError('Normal optimized jar prerequisite missing')
        with zipfile.ZipFile(jar_path) as archive:
            if archive.testzip() is not None or any('GameTests' in p or 'VisualCheck' in p for p in archive.namelist()):raise RuntimeError('Invalid production input jar')
        # The completed archive is authoritative; do not rebuild/replace its
        # classes from a later interrupted compiler directory. Verify every
        # source predates it and every AP shadow mapping against the proven
        # previous runtime before excluding the redundant compile task.
        if any(p.stat().st_mtime>jar_path.stat().st_mtime for p in (ROOT/'src/main/java').rglob('*.java')):raise RuntimeError('Java sources changed after completed input archive')
        from verify_packaged_mixins import members
        shadow=ROOT/'build-character-cast/generated/mixin-shadow.srg'
        rows=[line.split() for line in shadow.read_text(encoding='utf-8').splitlines() if line.strip()]
        declared=sum(p.read_text(encoding='utf-8').count('@Shadow') for p in (ROOT/'src/main/java/com/coraline/mixin').glob('*.java'))
        if len(rows)!=declared:raise RuntimeError('Incomplete AP shadow mappings')
        previous=ROOT/'build-character-cast/libs/coraline-1.15.99.jar'
        with zipfile.ZipFile(previous) as old,zipfile.ZipFile(jar_path) as archive:
            for row in rows:
                if row[0] not in ('FD:','MD:'):raise RuntimeError('Unexpected AP mapping kind')
                original=row[1];mapped=row[2] if row[0]=='FD:' else row[3]
                cls,name=original.rsplit('/',1);runtime_cls,runtime_name=mapped.rsplit('/',1)
                if cls!=runtime_cls:raise RuntimeError('Unexpected AP class remapping')
                before=members(archive.read(cls+'.class'));after=members(old.read(cls+'.class'))
                descriptors=[desc for field,desc in before if field==name]
                if not descriptors or any((runtime_name,desc) not in after for desc in descriptors):raise RuntimeError('Unverified AP runtime shadow: '+original)
        print('Verified complete compiled input and '+str(len(rows))+' AP shadow mappings; final task will not touch compiled classes.',flush=True)
        print('Resuming final Forge reobfuscation of completed optimized input, SHA256='+__import__('hashlib').sha256(jar_path.read_bytes()).hexdigest(),flush=True)
    if args.stage in ('native-ready','render-ready'):
        prerequisites=['reports/asset-size.json']
        if args.stage=='native-ready':prerequisites.append('resources/main/data/coraline/structures/empty.nbt')
        for name in prerequisites:
            if not (ROOT/'build-character-cast'/name).is_file():raise RuntimeError('Prepared QA prerequisite missing: '+name)
    stages={'qa-resources':['-PgameTests','-PexportNativeLaunch','-PqaCodeOnlyResources','processResources'],'compile':['-PgameTests','-PwybieVisitTests','compileJava'], 'production-compile':['compileJava'], 'native':['-PgameTests','-PwybieVisitTests','-PfreshGameTests','runGameTestServer'], 'native-ready':['-PgameTests','-PwybieVisitTests','-PfreshGameTests','runGameTestServer','-x','processResources'], 'render':['-PrenderChecks','-PvisitEventsVisualCheck','runClient'], 'render-ready':['-PrenderChecks','-PvisitEventsVisualCheck','runClient','-x','processResources'], 'package':['check','jar'], 'finish-reobf':['reobfJar','-x','jar','-x','compileJava','-x','clothingSrgMappings']}
    command+=(['jar','verifyReleaseJarSize'] if args.build_only else stages[args.stage])
    if args.stage=='package' and args.genuine_audio and not args.build_only:
        command.append('verifyReleaseJarSize')
    prepared_audio=None
    if args.reuse_verified_audio:
        from verify_prepared_audio import verify
        import re
        version=re.search(r'^mod_version=([A-Za-z0-9._-]+)$',(ROOT/'gradle.properties').read_text(),re.MULTILINE)
        if not version:raise RuntimeError('Missing safe release version')
        previous=ROOT/'build-character-cast/libs'/('coraline-'+version[1]+'-placeholder-audio.jar')
        if args.audited_audio_jar:
            previous=Path(args.audited_audio_jar).resolve(strict=True)
            if previous.parent!=(ROOT/'build-character-cast/libs').resolve() or not re.fullmatch(r'coraline-[A-Za-z0-9._-]+-placeholder-audio\.jar',previous.name):raise RuntimeError('Unexpected audited audio archive')
        prepared_audio=verify(ROOT,previous,args.reuse_verified_audio,preserve_captions=True,added_original_manifest='rescue_chapters_audio.json' if (ROOT/'src/main/resources/assets/coraline/rescue_chapters_audio.json').is_file() else None,added_final_originals=(ROOT/'src/main/resources/assets/coraline/final_chapter_original_audio.json').is_file())
        names=re.findall(r"tasks\.register\('([^']+)'",(ROOT/'build.gradle').read_text())
        names=[name for name in names if name.startswith('import') and ('Audio' in name or name in ('importParentVoices','importOtherFatherActivityIntros'))]
        if not names or any(name in ('processResources','check','jar') for name in names):raise RuntimeError('Invalid prepared importer selection')
        for name in names:command+=['-x',name]
        # Explicit user deferral: package the proven existing text, do not run
        # transcription, subtitle generation or an importer that can synthesize.
        command+=['-x','buildStorySubtitles']
        prepared_audio['reused_import_tasks']=names
        print('Prepared audio verified: '+json.dumps(prepared_audio),flush=True)
    if args.test_classes:command+=['-PwybieTestClasses='+args.test_classes]
    if args.render_classes:command+=['-PrenderCheckClasses='+args.render_classes]
    if args.export_only:
        if not native:raise RuntimeError('Launch export is QA-only')
        command+=['-PexportNativeLaunch']
    environment=monitor.child_environment();environment['JAVA_HOME']=str(monitor.JAVA)
    # All shared audio/caption manifests are UTF-8, including non-ASCII speech.
    # Windows' legacy default code page must not reinterpret a prior import.
    environment['PYTHONUTF8']='1'
    environment['PYTHONIOENCODING']='utf-8'
    # Forge's reobfuscator is a separate JVM with no inherited Gradle heap cap.
    # JAVA_TOOL_OPTIONS provides child defaults; explicit launcher/daemon flags
    # still win, so the thin launcher does not become another full-size JVM.
    environment['JAVA_TOOL_OPTIONS']=('-Xms16m -Xmx768m -XX:+UseSerialGC -XX:ActiveProcessorCount=4 -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -XX:MaxDirectMemorySize=96m' if native else '-Xms16m -Xmx512m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=64m')
    if args.lean_package or args.stage=='finish-reobf' or args.compact_driver and args.stage=='package':
        # The full current release exceeds Forge's 256 MiB remapper heap. Keep
        # Gradle at 256/224 MiB and give its one serial remapper 384 MiB.
        # Aggregate job limits and the 640/384 MiB safety floors are unchanged.
        environment['JAVA_TOOL_OPTIONS']='-Xms16m -Xmx384m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=48m'
    if args.stage=='finish-reobf':
        environment['JAVA_TOOL_OPTIONS']=environment['JAVA_TOOL_OPTIONS'].replace('-Xmx256m','-Xmx384m')
    if single_owner_vm:
        # Supply matching VM caps exactly once. Duplicated inherited options
        # made Gradle reject the launcher even when their effective values matched.
        environment['JAVA_TOOL_OPTIONS']=' '.join(caps)
    environment['PATH']=str(ROOT/'.voice-tools/index-tts/.venv/Scripts')+';C:\\ffmpeg;'+environment['PATH']
    # The Windows job bounds all descendants, including any Forge tools. Do not
    # override launcher settings through _JAVA_OPTIONS and duplicate its heap.
    directory=ROOT/'build-character-cast/low-memory-release'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());directory.mkdir(parents=True,exist_ok=False)
    free=ready_memory(args.minimum_start_free_mib,args.memory_wait_seconds)
    report={'stage':args.stage,'test_suites_skipped_by_request':args.build_only,'test_classes':args.test_classes,'command':command,'launcher_caps':launcher_caps,'build_caps':caps,'matched_launcher_requested':args.single_jvm,'child_vm_defaults':environment['JAVA_TOOL_OPTIONS'],'start_free_gib':free,'minimum_start_free_mib':args.minimum_start_free_mib,'aggregate_job_limit_mib':job_limit,'stop_free_mib':384,'voice_generation':False}
    if prepared_audio:report['prepared_audio']=prepared_audio
    log=directory/'output.log';api,handle=job(job_limit);child=None
    print('Low-memory '+args.stage+'; report: '+str(directory/'report.json'),flush=True)
    try:
        with log.open('xb') as output:
            child=subprocess.Popen(command,cwd=ROOT,env=environment,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,bufsize=0)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):
                error=ctypes.get_last_error();child.kill();child.wait();raise ctypes.WinError(error)
            reached,errors=threading.Event(),[]
            reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,output,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            available=compact_memory_probe(child,report) if args.compact_driver else monitor.free_gib
            report.update(monitor.watch_child(child,1800,available,monitor.process_memory,log_limit=reached))
            # Close only our aggregate job before waiting for output inherited by children.
            api.CloseHandle(handle);handle=None
            reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():report.update(status='output_capture_failed',errors=errors)
            child.stdout.close()
            report['single_use_daemon_forked']='single-use Daemon process will be forked' in log.read_text(encoding='utf-8',errors='replace')
            if args.export_imports and report.get('status')=='failed' and report.get('exit_code')==1 and Path(args.export_imports).is_file() and 'SERIALIZED_IMPORT_GRAPH_EXPORTED: no build tasks executed' in log.read_text(encoding='utf-8',errors='replace'):
                report.update(status='import_graph_exported',no_build_tasks_executed=True)
            if args.stage in ('native','native-ready') and not args.export_only and report.get('status')=='passed':
                transcript=log.read_text(encoding='utf-8',errors='replace')
                # Forge can return exit zero even when mod loading failed.
                completion=__import__('re').search(r'All ([1-9]\d*) required tests passed',transcript)
                if 'Failed to start the minecraft server' in transcript or 'Mod loading error has occurred' in transcript or not completion:
                    report.update(status='native_acceptance_failed',reason='Missing concrete test completion or Minecraft loading failure')
                else:report['required_tests_passed']=int(completion.group(1))
            if args.stage in ('render','render-ready') and not args.export_only and report.get('status')=='passed':
                transcript=log.read_text(encoding='utf-8',errors='replace')
                if 'FAMILY_GAMES_NATIVE_CAPTURED' not in transcript:report.update(status='native_acceptance_failed',reason='Native visual harness did not reach its completion marker')
    finally:
        if handle:api.CloseHandle(handle)
        (directory/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(monitor.tail_bytes(log),flush=True);print(json.dumps({key:value for key,value in report.items() if key!='command'}),flush=True)
    return 0 if report.get('status')=='passed' and report.get('exit_code')==0 or report.get('status')=='import_graph_exported' else 1
if __name__=='__main__':raise SystemExit(main())
