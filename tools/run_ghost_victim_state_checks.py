"""Guarded actual Java/NBT state checks; no server or graphical claim."""
from pathlib import Path
import ctypes,json,hashlib,os,subprocess,time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

ROOT=Path(__file__).resolve().parents[1]


def main():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged small-Java start floor')
    reports=sorted((ROOT/'build-character-cast/cat-focused-compile').glob('*/report.json'),reverse=True)
    chosen=None
    for path in reports:
        n=json.loads(path.read_text(encoding='utf-8'))
        if any(key.replace('\\','/').endswith('/GhostVictimStateCheck.java') for key in n.get('source_sha256',{})):chosen=(path,n);break
    if chosen is None:raise ValueError('Staged state checks were not compiled')
    path,compiled=chosen
    if compiled.get('status')!='passed' or compiled.get('exit_code')!=0:raise ValueError('Successful staged Java compile required')
    for name,digest in compiled['source_sha256'].items():
        if hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=digest:raise ValueError('Staged source changed after compilation')
    cached=ROOT/'build-character-cast/classpath/runClient_minecraftClasspath.txt'
    jars=[Path(line) for line in cached.read_text(encoding='utf-8-sig').splitlines() if line]
    if any(not p.is_relative_to(Path(r'C:\Users\User1\.gradle\caches')) or not p.is_file() for p in jars):raise ValueError('Unexpected cached Java dependency')
    folder=ROOT/'build-character-cast/ghost-victim-state-checks'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    report=dict(scope='Actual pure Java/NBT state checks only, not native Minecraft gameplay',compiled_report=str(path),start_free_gib=monitor.free_gib())
    command=[str(monitor.JAVA/'java.exe'),'-Xmx192m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:MaxMetaspaceSize=128m','-cp',os.pathsep.join(map(str,[path.parent/'classes',ROOT/'build-character-cast/classes/java/main',*jars])),'com.coraline.feast.GhostVictimStateCheck']
    api,handle=job(512)
    try:
        with (folder/'output.log').open('xb') as stream:
            child=subprocess.Popen(command,cwd=ROOT,env=monitor.child_environment(),stdout=stream,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
            report.update(monitor.watch_child(child,90,monitor.free_gib,monitor.process_memory))
        log=(folder/'output.log').read_text(encoding='utf-8',errors='replace')
        if 'GHOST_VICTIM_NBT_STATE_OK checks=49;' not in log:report.update(status='state_checks_failed')
        print(log,flush=True)
    finally:
        api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print('Staged state report: '+str(folder/'report.json'),flush=True)
    return 0 if report.get('status')=='passed' and report.get('exit_code')==0 else 1


if __name__=='__main__':raise SystemExit(main())
