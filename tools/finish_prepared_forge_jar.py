"""Resume the exact official Forge tool prepared by the completed normal build.
No class assembly or resource changes; publish only after mapped-shadow/ZIP audits.
"""
from pathlib import Path
import ctypes,hashlib,json,os,re,shutil,subprocess,threading,time,zipfile
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job,ready_memory
from verify_packaged_mixins import verify,members
ROOT=monitor.ROOT
PIN='6305d3588f7b9b2855881342dfe2768c14791962c7405c748805a54f18b0a62f'
def sha(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def main():
    ready_memory(1024,45)
    source=ROOT/'build-character-cast/libs/coraline-1.16.0.jar'
    if sha(source)!=PIN:raise RuntimeError('Prepared normal-build input changed')
    normal=ROOT/'build-character-cast/low-memory-release/20261103T234447Z'
    log=(normal/'output.log').read_text(encoding='utf-8')
    if not all(mark in log for mark in ('> Task :processResources','> Task :check','> Task :reobfJar')):raise RuntimeError('Normal optimized/check pipeline incomplete')
    if any(p.stat().st_mtime>source.stat().st_mtime for p in (ROOT/'src/main/java').rglob('*.java')):raise RuntimeError('Source newer than completed compiled archive')
    prepared=(ROOT/'build-character-cast/reobfJar/log.txt').read_text(encoding='utf-8')
    args_line=next(line for line in prepared.splitlines() if line.startswith('Arguments: '))
    args=args_line.removeprefix("Arguments: '").removesuffix("'").split(', ')
    tool=Path(next(line[3:] for line in prepared.splitlines() if line.startswith(' - ')))
    cache=Path(r'C:/Users/User1/.gradle/caches').resolve()
    if not tool.resolve().is_relative_to(cache) or tool.name!='renamer-2.0.4-all.jar' or 'Main class: net.minecraftforge.renamer.Main' not in prepared:raise RuntimeError('Unexpected Forge tool')
    if args[:2]!=['--input',str(source)]:raise RuntimeError('Unexpected original input')
    libs=[];mapping=None
    for i in range(0,len(args)-1,2):
        flag,value=args[i:i+2]
        if flag not in ('--input','--output','--names','--lib'):raise RuntimeError('Unexpected tool argument')
        path=Path(value).resolve(strict=True)
        if flag=='--lib':
            if not path.is_relative_to(cache) or path.suffix!='.jar':raise RuntimeError('Untrusted library')
            libs.append(path)
        elif flag=='--names':mapping=path
        elif not path.is_relative_to(ROOT):raise RuntimeError('Path outside workspace')
    if args[-1]!='--disable-abstract-param' or not 20<len(libs)<200 or mapping!=ROOT/'build-character-cast/reobfJar/mappings.tsrg':raise RuntimeError('Incomplete prepared tool arguments')
    run=ROOT/'build-character-cast/forge-final-resume'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());run.mkdir(parents=True)
    output=run/'mapped.jar';args[args.index('--output')+1]=str(output)
    command=[str(monitor.JAVA/'java.exe'),'-Xms16m','-Xmx384m','-XX:+UseSerialGC','-XX:ActiveProcessorCount=2','-XX:MaxMetaspaceSize=128m','-XX:ReservedCodeCacheSize=32m','-XX:MaxDirectMemorySize=48m','-Djdk.nio.maxCachedBufferSize=1048576','-cp',str(tool),'net.minecraftforge.renamer.Main',*args]
    report={'status':'running','scope':'Official Forge Renamer exact prepared inputs, after normal compile/resources/check; no manual class/resource assembly','input_sha256':PIN,'tool_sha256':sha(tool),'mapping_sha256':sha(mapping),'command':command,'start_free_gib':monitor.free_gib(),'minimum_start_free_mib':1024,'emergency_free_mib':384,'heap_mib':384,'aggregate_job_mib':768}
    print('Official Forge final-tool report: '+str(run/'report.json'),flush=True)
    api,handle=job(768);child=None
    try:
        env=monitor.child_environment();env['JAVA_TOOL_OPTIONS']=''
        with (run/'output.log').open('xb',buffering=0) as log:
            child=subprocess.Popen(command,cwd=run,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
            reached,errors=threading.Event(),[]
            reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            report.update(monitor.watch_child(child,1800,monitor.free_gib,monitor.process_memory,log_limit=reached))
            api.CloseHandle(handle);handle=None;reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():raise RuntimeError('Incomplete tool log')
        if report.get('status')!='passed' or report.get('exit_code')!=0:raise RuntimeError('Official remapper did not complete under guards')
        verify(output)
        with zipfile.ZipFile(source) as raw,zipfile.ZipFile(output) as mapped:
            if mapped.testzip() is not None or {i.filename for i in raw.infolist() if not i.is_dir()}!={i.filename for i in mapped.infolist() if not i.is_dir()}:raise RuntimeError('Archive entries/integrity changed')
            for name in raw.namelist():
                if not name.endswith(('.class','/')) and raw.read(name)!=mapped.read(name):raise RuntimeError('Packaged resource changed: '+name)
            shadow=(ROOT/'build-character-cast/generated/mixin-shadow.srg').read_text().splitlines()
            for line in shadow:
                row=line.split();old=row[1];new=row[2] if row[0]=='FD:' else row[3]
                cls,name=old.rsplit('/',1);_,runtime=new.rsplit('/',1)
                before=members(raw.read(cls+'.class'));after=members(mapped.read(cls+'.class'))
                if not any(field==name for field,desc in before) or any((runtime,desc) not in after for field,desc in before if field==name):raise RuntimeError('Runtime shadow remapping failed: '+old)
            report['all_resources_byte_identical']=True;report['zip_integrity']='passed';report['all_14_ap_shadows_verified']=True
        if sha(source)!=PIN:raise RuntimeError('Input changed during finalization')
        shutil.copyfile(source,run/'unmapped-input.jar')
        staged=source.with_suffix('.jar.ready');shutil.copyfile(output,staged);staged.replace(source)
        report.update(status='published',jar=str(source),sha256=sha(source),bytes=source.stat().st_size)
    finally:
        if handle is not None:api.CloseHandle(handle)
        (run/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ('status','jar','sha256','bytes')}))
    return 0
if __name__=='__main__':raise SystemExit(main())
