"""Run real NBT component assertions; this is not a Minecraft playthrough."""
from pathlib import Path
import ctypes,hashlib,json,os,subprocess,sys,time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job
ROOT=Path(__file__).resolve().parents[1]
def main(*,check_source='GhostEscapeStateCheck.java',class_name='com.coraline.feast.GhostEscapeStateCheck',marker='GHOST_ESCAPE_NBT_OK checks=',report_group='ghost-escape-state-checks',label='Ghost escape',program_args=()):
    compiled=Path(sys.argv[1]).resolve(strict=True)
    if not compiled.is_relative_to(ROOT/'build-character-cast/cat-focused-compile'):raise ValueError('Unexpected compiler receipt')
    n=json.loads(compiled.read_text(encoding='utf-8'))
    if n.get('status')!='passed' or n.get('exit_code')!=0 or not n.get('full_source_compile'):raise ValueError('Full current source compile required')
    for name,digest in n['source_sha256'].items():
        if hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=digest:raise ValueError('Source changed after compile: '+name)
    if not any(name.replace('\\','/').endswith('/'+check_source) for name in n['source_sha256']):raise ValueError('Missing actual assertions')
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged Java start floor')
    jars=[Path(p) for p in (ROOT/'build-character-cast/classpath/runClient_minecraftClasspath.txt').read_text(encoding='utf-8-sig').splitlines() if p]
    if any(not p.is_relative_to(Path(r'C:\Users\User1\.gradle\caches')) or not p.is_file() for p in jars):raise ValueError('Unexpected dependency')
    folder=ROOT/'build-character-cast'/report_group/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    report=dict(scope='Actual Java/NBT component only; no physical game, visual or release acceptance',compiled_report=str(compiled),start_free_gib=monitor.free_gib(),program_args=list(program_args))
    command=[str(monitor.JAVA/'java.exe'),'-Xmx192m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:MaxMetaspaceSize=128m','-cp',os.pathsep.join(map(str,[compiled.parent/'classes',*jars])),class_name,*program_args]
    api,handle=job(512)
    try:
        with (folder/'output.log').open('xb') as stream:
            child=subprocess.Popen(command,cwd=ROOT,env=monitor.child_environment(),stdout=stream,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
            report.update(monitor.watch_child(child,90,monitor.free_gib,monitor.process_memory))
        output=(folder/'output.log').read_text(encoding='utf-8',errors='replace');print(output)
        if marker not in output:report['status']='component_assertions_failed'
    finally:
        api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(label+' component report: '+str(folder/'report.json'));return 0 if report.get('status')=='passed' else 1
if __name__=='__main__':raise SystemExit(main())
