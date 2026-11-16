"""Compile render-fixture deltas over a source-validated successful full-source snapshot.

Never substitutes for a production compile, optimizer, release build or native acceptance.
"""
from pathlib import Path
import argparse,ctypes,hashlib,json,os,re,shutil,subprocess,threading,time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

root=monitor.ROOT
parser=argparse.ArgumentParser();parser.add_argument('--base-report',required=True);args=parser.parse_args()
base=(root/args.base_report).resolve();area=(root/'build-character-cast/cat-focused-compile').resolve()
if not base.is_relative_to(area) or base.name!='report.json' or not re.fullmatch(r'\d{8}T\d{6}Z',base.parent.name):raise RuntimeError('Unexpected full-source base report')
original=json.loads(base.read_text());hashes=original.get('source_sha256',{})
if original.get('status')!='passed' or original.get('exit_code')!=0 or original.get('full_source_compile') is not True:raise RuntimeError('Successful complete base compilation required')
digest=lambda path:hashlib.sha256(path.read_bytes()).hexdigest()
normalized={key.replace('\\','/'):value for key,value in hashes.items()}
required={str(p.relative_to(root)).replace('\\','/') for p in (root/'src/main/java').rglob('*.java')}
if not required.issubset(normalized):raise RuntimeError('Base did not compile every production source')
changed=[]
for relative,value in normalized.items():
    source=(root/relative).resolve()
    if not source.is_relative_to(root) or not source.is_file():raise RuntimeError('Missing original source')
    if digest(source)!=value:
        if not relative.startswith('src/rendercheck/java/') or source.suffix!='.java':raise RuntimeError('Production/test source changed; full compile required: '+relative)
        changed.append(source)
if not changed:raise RuntimeError('No render-fixture delta to compile')
raw=(root/monitor.CP).read_bytes()
if hashlib.sha256(raw).hexdigest()!=original.get('cached_classpath_sha256'):raise RuntimeError('Dependency classpath changed')
jars=[Path(line) for line in raw.decode('utf-8-sig').splitlines() if line]
allowed=Path(r'C:\Users\User1\.gradle\caches')
if not jars or any(not p.is_absolute() or not p.is_relative_to(allowed) or not p.is_file() or p.suffix.lower() not in ('.jar','.zip') for p in jars):raise RuntimeError('Unexpected cached dependency')
classes=base.parent/'classes';members=list(classes.rglob('*'))
if classes.is_symlink() or any(p.is_symlink() or not p.resolve().is_relative_to(classes.resolve()) for p in members) or sum(p.stat().st_size for p in members if p.is_file())>128*1024*1024:raise RuntimeError('Unexpected base class tree')
if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged 640 MiB start floor')
folder=area/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(exist_ok=False)
destination=folder/'classes';shutil.copytree(classes,destination);empty=folder/'empty-sourcepath';empty.mkdir()
quote=lambda p:'"'+str(p).replace('\\','/')+'"'
arguments=['-encoding','UTF-8','--release','17','-proc:none','-implicit:none','-sourcepath',quote(empty),'-classpath',quote(os.pathsep.join(map(str,[classes,*jars]))),'-d',quote(destination),*map(quote,changed)]
argfile=folder/'javac.args';argfile.write_text('\n'.join(arguments)+'\n',encoding='utf8')
report={'scope':'Incremental render-fixture compile over validated full-source base; NOT a fresh full compile, production release or native acceptance','full_source_compile':False,'validated_full_source_base':str(base),'validated_base_report_sha256':digest(base),'source_sha256':{key:digest(root/key) for key in normalized},'delta_sources':[str(p.relative_to(root)) for p in changed],'cached_classpath_sha256':hashlib.sha256(raw).hexdigest(),'compiler_heap_mib':96,'start_free_gib':monitor.free_gib()}
api,handle=job(384)
print('Incremental QA compiler report: '+str(folder/'report.json'),flush=True)
try:
    with (folder/'output.log').open('xb',buffering=0) as log:
        child=subprocess.Popen([str(monitor.JAVA/'javac.exe'),'-J-Xmx96m','-J-XX:+UseSerialGC','-J-XX:ActiveProcessorCount=2','@'+str(argfile)],cwd=root,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
        if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();child.wait();raise ctypes.WinError(ctypes.get_last_error())
        reached,errors=threading.Event(),[];reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
        report.update(monitor.watch_child(child,180,monitor.free_gib,monitor.process_memory,log_limit=reached));reader.join(timeout=10)
        if reader.is_alive() or errors or reached.is_set():report['status']='capture_failed'
        if any(digest(root/p)!=value for p,value in report['source_sha256'].items()):report['status']='source_changed_during_compile'
finally:
    api.CloseHandle(handle);(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
print(monitor.tail_bytes(folder/'output.log'));print(json.dumps({key:report.get(key) for key in ('status','exit_code','min_free_gib','delta_sources')},indent=2))
raise SystemExit(0 if report.get('status')=='passed' and report.get('exit_code')==0 else 1)
