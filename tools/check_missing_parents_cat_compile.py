"""Focused Java API compile from cached dependencies, not a release or native acceptance.

Writes only a fresh isolated build output. Never changes cached production classes,
mixins/refmaps, source assets or a JAR. Existing RAM floors and owned job limits apply.
"""
from pathlib import Path
import ctypes, hashlib, json, os, subprocess, threading, time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job,compact_memory_probe

ROOT=Path(__file__).resolve().parents[1]
SOURCES=[
    'feast/MissingParentsCatTimeline.java','feast/MissingParentsCat.java',
    'feast/MissingParentsCatDebug.java','feast/MissingParentsTea.java',
    'feast/ShowActors.java','feast/CinematicPropEntity.java','feast/StoryTunnel.java','feast/StoryContinuationDebug.java',
    'feast/ParentRescueStory.java','otherworld/DoorBlockEntity.java',
    'client/renderer/DoorPortalRenderer.java','client/renderer/MissingParentsPresentation.java',
    'client/renderer/CharacterModel.java','client/ShowActorPoseClient.java']
TESTS=['feast/MissingParentsTeaGameTests.java','feast/StoryContinuationGameTests.java',
       'feast/MissingParentsCatGameTests.java','otherworld/RescueKitchenGameTests.java']

def main():
    if ROOT!=monitor.ROOT or os.name!='nt':raise RuntimeError('Unexpected workspace/platform')
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
    heap_mib=globals().get('HEAP_MIB',256)
    if heap_mib not in (192,256):raise RuntimeError('Unexpected isolated javac heap')
    cached=ROOT/'build-character-cast/classpath/runClient_minecraftClasspath.txt'
    raw=cached.read_bytes();jars=[Path(p) for p in raw.decode('utf-8-sig').splitlines() if p]
    allowed=Path(r'C:\Users\User1\.gradle\caches')
    if not jars or len(jars)>600 or any(not p.is_absolute() or not p.is_relative_to(allowed) or p.suffix.lower() not in ('.jar','.zip') or not p.is_file() for p in jars):
        raise RuntimeError('Invalid cached dependency list')
    compiled=Path(globals().get('COMPILED',ROOT/'build-character-cast/classes/java/main')).resolve()
    if not compiled.is_relative_to(ROOT/'build-character-cast'):raise RuntimeError('Compiled dependency path escaped the build workspace')
    source_root=ROOT/'src/main/java/com/coraline'
    full_source={str(p.relative_to(source_root)).replace('\\','/') for p in source_root.rglob('*.java')}==set(SOURCES)
    if not full_source and not (compiled/'com/coraline/feast/VisitData.class').is_file():raise RuntimeError('Cached production APIs unavailable')
    extra=list(globals().get('EXTRA_SOURCES',[]))
    if 'src/rendercheck/java/com/coraline/qa/LeanWorldVisualCheck.java' in extra and 'src/rendercheck/java/com/coraline/qa/PalaceBackgroundVisualCheck.java' not in extra:
        extra.append('src/rendercheck/java/com/coraline/qa/PalaceBackgroundVisualCheck.java')
    sources=[ROOT/'src/main/java/com/coraline'/p for p in SOURCES]+[ROOT/'src/wybietest/java/com/coraline'/p for p in TESTS]+[ROOT/p for p in extra]
    output=ROOT/'build-character-cast/cat-focused-compile'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())
    output.mkdir(parents=True,exist_ok=False);classes=output/'classes';classes.mkdir();empty=output/'empty-sourcepath';empty.mkdir()
    quote=lambda p:'"'+str(p).replace('\\','/')+'"'
    arguments=['-encoding','UTF-8','--release','17','-proc:none','-implicit:none','-sourcepath',quote(empty),'-classpath',quote(os.pathsep.join(map(str,jars if full_source else [compiled,*jars]))),'-d',quote(classes),*map(quote,sources)]
    argfile=output/'javac.args';argfile.write_text('\n'.join(arguments)+'\n',encoding='utf-8')
    report={'scope':'Focused cached-API compile only; NOT full Gradle, mixin, native, visual, audio or release acceptance',
        'source_sha256':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
        'cached_classpath_sha256':hashlib.sha256(raw).hexdigest(),'cached_dependency_count':len(jars),'full_source_compile':full_source,'start_free_gib':monitor.free_gib()}
    if globals().get('BASE_SOURCE_COMPILE'):report['base_source_compile']=globals()['BASE_SOURCE_COMPILE']
    api,handle=job(768)
    print('Focused compiler report: '+str(output/'report.json'),flush=True)
    try:
        with (output/'output.log').open('xb',buffering=0) as stream:
            child=subprocess.Popen([str(monitor.JAVA/'javac.exe'),f'-J-Xmx{heap_mib}m','-J-XX:+UseSerialGC','-J-XX:ActiveProcessorCount=2','-J-XX:MaxMetaspaceSize=128m','-J-XX:ReservedCodeCacheSize=32m','@'+str(argfile)],cwd=ROOT,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,bufsize=0)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
            reached,errors=threading.Event(),[]
            reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,stream,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            available=compact_memory_probe(child,report) if globals().get('TRIM_OWNED_COMPILER',False) else monitor.free_gib
            report.update(monitor.watch_child(child,300,available,monitor.process_memory,log_limit=reached));reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():report['status']='output_capture_failed'
            if any(hashlib.sha256((ROOT/p).read_bytes()).hexdigest()!=digest for p,digest in report['source_sha256'].items()):report['status']='source_changed_during_compile'
    finally:
        api.CloseHandle(handle);(output/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(monitor.tail_bytes(output/'output.log'));print(json.dumps({k:v for k,v in report.items() if k!='source_sha256'},indent=2))
    return 0 if report.get('status')=='passed' and report.get('exit_code')==0 else 1

if __name__=='__main__':raise SystemExit(main())
