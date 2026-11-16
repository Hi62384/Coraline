"""Pinned candidate verification; optional exact source application only after all gates.

Runs a tiny Windows handle/backup fixture, one javac, then the already-reviewed tabletop
smoke. --apply-after-pass explicitly permits the pinned source-only application.
No Gradle, Minecraft, Mixin loading, audio synthesis, network, install or release.
"""
from pathlib import Path, PureWindowsPath
from contextlib import ExitStack
from ctypes import wintypes
import argparse,ctypes,hashlib,json,os,stat,subprocess,sys,time,types,uuid,threading
ROOT=Path(r'C:\Users\User1\Documents\Downloads\Coraline')
JAVA=Path(r'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin')
CP='build-character-cast/classpath/runClient_minecraftClasspath.txt'
HELPER='tools/apply_numbered_specs.py'
SMOKE='tools/TabletopRulesSmoke.java'
START_FREE_GIB=640/1024
STOP_FREE_GIB=384/1024
JAVAC_CAPS=['-J-Xmx192m','-J-XX:MaxMetaspaceSize=96m','-J-XX:ReservedCodeCacheSize=32m']
SMOKE_CAPS=['-Xmx128m','-XX:MaxMetaspaceSize=64m','-XX:ReservedCodeCacheSize=32m']
def child_environment():
    """Use a child-only essentials allowlist; never alter the user's environment."""
    allowed = {'SYSTEMROOT', 'WINDIR', 'SYSTEMDRIVE', 'TEMP', 'TMP',
               'USERPROFILE', 'APPDATA', 'LOCALAPPDATA', 'HOMEDRIVE', 'HOMEPATH'}
    result = {name.upper(): value for name, value in os.environ.items() if name.upper() in allowed}
    system_root = result.get('SYSTEMROOT')
    if not system_root or not PureWindowsPath(system_root).is_absolute() or ';' in system_root:
        raise RuntimeError('Missing or invalid Windows system root for bounded child environment')
    windows = PureWindowsPath(system_root)
    result['PATH'] = ';'.join((str(JAVA), str(windows / 'System32'), str(windows)))
    return result

def bootstrap_regular(path):
    """Validate the named chain before loading any project helper code."""
    for current in (*reversed(path.parents),path):
        info=current.lstat()
        if stat.S_ISLNK(info.st_mode) or getattr(info,'st_file_attributes',0)&0x400:
            raise RuntimeError('Bootstrap reparse/link refused: '+str(current))
        if current!=path and not stat.S_ISDIR(info.st_mode):raise RuntimeError('Bootstrap parent is not a directory')
        if current==path and (not stat.S_ISREG(info.st_mode) or info.st_nlink!=1 or info.st_size>1024*1024):
            raise RuntimeError('Bootstrap file is not a bounded unaliased regular file')

def compiled_fingerprints(guard,root,directory):
    """Pin the existing compiled API dependency tree, bounded to this one directory."""
    guard.no_reparse(root,directory,kind='directory',required=True)
    result={};total=0
    for parent,dirs,files in os.walk(directory,followlinks=False):
        for name in dirs:guard.no_reparse(root,Path(parent)/name,kind='directory',required=True)
        for name in files:
            path=Path(parent)/name
            raw=guard._read(root,path,limit=4*1024*1024);total+=len(raw)
            if len(result)>=10000 or total>128*1024*1024:raise RuntimeError('Compiled API tree exceeds verification bounds')
            result[path.relative_to(directory).as_posix()]=guard.digest(raw)
    if not result:raise RuntimeError('Compiled API dependency tree is empty')
    return result

class Memory(ctypes.Structure):
    _fields_=[('length',ctypes.c_ulong),('load',ctypes.c_ulong),('total',ctypes.c_ulonglong),('available',ctypes.c_ulonglong),('page_total',ctypes.c_ulonglong),('page_available',ctypes.c_ulonglong),('virtual_total',ctypes.c_ulonglong),('virtual_available',ctypes.c_ulonglong),('extended',ctypes.c_ulonglong)]
def free_gib():
    memory=Memory();memory.length=ctypes.sizeof(memory)
    if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)):raise RuntimeError('Cannot verify available physical memory')
    return memory.available/1024**3

class ProcessMemoryCounters(ctypes.Structure):
    # POINTER(Structure) types are retained by ctypes. Defining this class inside
    # the20Hz sampling function leaked one permanent type per sampled process.
    _fields_=[('cb',wintypes.DWORD),('faults',wintypes.DWORD),
              ('peak_working_set',ctypes.c_size_t),('working_set',ctypes.c_size_t),
              ('quota_peak_paged',ctypes.c_size_t),('quota_paged',ctypes.c_size_t),
              ('quota_peak_nonpaged',ctypes.c_size_t),('quota_nonpaged',ctypes.c_size_t),
              ('pagefile',ctypes.c_size_t),('peak_pagefile',ctypes.c_size_t),('private',ctypes.c_size_t)]

def process_memory(process):
    # The handle belongs only to the Popen child created by this invocation.
    query=ctypes.WinDLL('psapi',use_last_error=True).GetProcessMemoryInfo
    query.argtypes=[wintypes.HANDLE,ctypes.POINTER(ProcessMemoryCounters),wintypes.DWORD];query.restype=wintypes.BOOL
    counters=ProcessMemoryCounters();counters.cb=ctypes.sizeof(counters)
    if not query(wintypes.HANDLE(int(process._handle)),ctypes.byref(counters),counters.cb):raise ctypes.WinError(ctypes.get_last_error())
    return {'peak_working_set_bytes':int(counters.peak_working_set),'working_set_bytes':int(counters.working_set),'peak_pagefile_bytes':int(counters.peak_pagefile),'private_bytes':int(counters.private)}

def watch_child(process,seconds,available,measure,*,clock=time.monotonic,sleep=time.sleep,log_limit=None):
    """Bound only this child; never enumerate, stop, or alter other processes."""
    started=clock();out={'status':'running','min_free_gib':None,'peak_working_set_bytes':0,'peak_pagefile_bytes':0,'peak_sampled_private_bytes':0,'memory_samples':0}
    try:
        while True:
            code=process.poll()
            if code is not None:
                out.update(status='passed' if code==0 else 'failed',exit_code=code)
                break
            sample=measure(process);out['memory_samples']+=1;free=available()
            out['min_free_gib']=free if out['min_free_gib'] is None else min(free,out['min_free_gib'])
            for key in ('peak_working_set_bytes','peak_pagefile_bytes'):out[key]=max(out[key],sample[key])
            out['peak_sampled_private_bytes']=max(out['peak_sampled_private_bytes'],sample['private_bytes'])
            if log_limit is not None and log_limit.is_set():out['status']='log_size_cutoff';break
            if free<STOP_FREE_GIB:out['status']='low_memory_cutoff';break
            if clock()-started>=seconds:out['status']='timed_out';break
            sleep(.05)
    except Exception as error:
        code=process.poll()
        if code is not None:out.update(status='passed' if code==0 else 'failed',exit_code=code,final_measurement_note=str(error))
        else:out.update(status='monitor_failed',error=str(error))
    finally:
        if process.poll() is None:
            process.kill() # Exact child handle only, no taskkill or broad process-name matching.
            out['exit_code']=process.wait(timeout=10)
            out['own_child_stopped']=True
        else:out.setdefault('exit_code',process.returncode)
        # Query the retained handle once more after exit when Windows still exposes the peak.
        try:
            final=measure(process);out['memory_samples']+=1
            for key in ('peak_working_set_bytes','peak_pagefile_bytes'):out[key]=max(out[key],final[key])
            out['peak_sampled_private_bytes']=max(out['peak_sampled_private_bytes'],final['private_bytes'])
        except Exception as error:out['final_measurement_note']=str(error)
        if not out['memory_samples']:out['memory_measurement_available']=False
        else:out['memory_measurement_available']=True
    return out


GRADLE_CACHE=Path(r'C:\Users\User1\.gradle\caches')
MAX_LOG_BYTES=8*1024*1024
MAX_DEPENDENCY_BYTES=256*1024*1024
MAX_ALL_DEPENDENCIES=2*1024*1024*1024

def require_empty(guard,root,path,label):
    guard.no_reparse(root,path,kind='directory',required=True)
    with os.scandir(path) as listing:
        if next(listing,None) is not None:raise RuntimeError(label+' must remain empty')

def tail_bytes(path,limit=20000):
    with path.open('rb') as stream:
        stream.seek(0,os.SEEK_END);stream.seek(max(0,stream.tell()-limit))
        return stream.read(limit).decode('utf-8',errors='replace')

def drain_bounded(source,destination,limit,reached,errors):
    """The project log never exceeds the declared cap; excess pipe bytes are discarded."""
    written=0
    try:
        while True:
            chunk=source.read(65536)
            if not chunk:break
            keep=min(len(chunk),max(0,limit-written))
            if keep:
                destination.write(chunk[:keep]);destination.flush();written+=keep
            if keep<len(chunk):reached.set()
    except Exception as error:
        errors.append(str(error));reached.set()

class HeldInputs:
    """Windows deny-write/delete handles for exact declared inputs; not a sandbox."""
    def __init__(self,guard,stack):self.guard=guard;self.stack=stack;self.files={};self.parents=set()
    def pin(self,root,path,*,limit,expected=None):
        key=str(path).casefold()
        if key in self.files:
            record=self.files[key]
            if expected is not None and record['sha256']!=expected:raise RuntimeError('Conflicting input pin')
            return record['sha256']
        if path.parent not in self.parents:
            self.stack.enter_context(self.guard._pin_directories(root,[path.parent]));self.parents.add(path.parent)
        if os.name=='nt':
            # Keep a raw Win32 handle, not one CRT descriptor per cached class.
            # Temporary hashing reads coexist with this deny-write/delete hold.
            self.guard.no_reparse(root,path,required=True)
            kernel,handle=self.guard._windows_handle(path)
            self.stack.callback(kernel.CloseHandle,handle)
            with self.guard._file(root,path,limit=limit) as temporary:
                info=os.fstat(temporary.fileno());digest=self.hash_stream(temporary,limit)
                after=os.fstat(temporary.fileno())
            stream=None
        else:
            stream=self.stack.enter_context(self.guard._file(root,path,limit=limit))
            info=os.fstat(stream.fileno());digest=self.hash_stream(stream,limit)
            after=os.fstat(stream.fileno())
        if (info.st_size,info.st_mtime_ns)!=(after.st_size,after.st_mtime_ns):raise RuntimeError('Input changed while hashing: '+str(path))
        if expected is not None and digest!=expected:raise RuntimeError('Input content pin mismatch: '+str(path))
        self.files[key]={'root':root,'path':path,'stream':stream,'limit':limit,'sha256':digest,
                         'identity':(info.st_dev,info.st_ino),'size':info.st_size}
        return digest
    @staticmethod
    def hash_stream(stream,limit):
        stream.seek(0);digest=hashlib.sha256();total=0
        while True:
            chunk=stream.read(min(1024*1024,limit-total+1))
            if not chunk:break
            total+=len(chunk)
            if total>limit:raise RuntimeError('Held input exceeds declared size bound')
            digest.update(chunk)
        return digest.hexdigest()
    def verify(self,stage):
        for record in self.files.values():
            path=record['path'];self.guard.no_reparse(record['root'],path,required=True)
            named=path.lstat()
            if record['stream'] is None:
                with self.guard._file(record['root'],path,limit=record['limit']) as temporary:
                    opened=os.fstat(temporary.fileno());digest=self.hash_stream(temporary,record['limit'])
            else:
                opened=os.fstat(record['stream'].fileno());digest=self.hash_stream(record['stream'],record['limit'])
            if ((named.st_dev,named.st_ino)!=record['identity'] or (opened.st_dev,opened.st_ino)!=record['identity']
                    or opened.st_size!=record['size'] or digest!=record['sha256']):
                raise RuntimeError(stage+': held input changed '+str(path))

def lock_tree(guard,root,directory,held):
    guard.no_reparse(root,directory,kind='directory',required=True)
    result={};total=0
    for parent,dirs,files in os.walk(directory,followlinks=False):
        for name in dirs:guard.no_reparse(root,Path(parent)/name,kind='directory',required=True)
        for name in files:
            path=Path(parent)/name;total+=path.lstat().st_size
            if len(result)>=10000 or total>128*1024*1024:raise RuntimeError('Compiled tree exceeds verification bounds')
            result[path.relative_to(directory).as_posix()]=held.pin(root,path,limit=4*1024*1024)
    if not result:raise RuntimeError('Compiled tree is empty')
    return result

def assert_native_read_denials(guard,root,sample,expected):
    if os.name!='nt':raise RuntimeError('Native sharing checks require Windows')
    with guard._file(root,sample) as stream:
        for operation in (lambda:sample.write_bytes(b'foreign'),sample.unlink):
            try:operation()
            except PermissionError:pass
            else:raise RuntimeError('Native readonly handle failed to deny write/delete')
        if stream.read()!=expected:raise RuntimeError('Native locked bytes changed')

def native_guard_fixture(guard,output):
    """Disposable real helper apply/readback plus real readonly-handle denial checks."""
    import base64
    fixture=output/'guard-fixture'
    with ExitStack() as stack:
        guard._mkdir_pinned(ROOT,fixture,stack,exclusive=True)
        guard._mkdir_pinned(ROOT,fixture/'docs',stack)
        sample=fixture/'docs/existing.md';old=b'original\r\n'
        with guard._file(ROOT,sample,write=True,create=True) as stream:guard._write_all(stream,old)
        assert_native_read_denials(guard,ROOT,sample,old)
        def row(name,base,data):return {'path':name,'base_sha256':base,'candidate_sha256':guard.digest(data),
            'content_base64':base64.b64encode(data).decode(),'fingerprint_mode':'utf8-lf'}
        entries=guard.validate_payload(fixture,{'format':1,'project':'Coraline','scope':'numbered-spec-source-only','files':[
            row('docs/existing.md',guard.digest(b'original\n'),b'updated\n'),row('docs/new.md',None,b'created\n')]})
        result=guard.apply(fixture,entries,guard.inspect_targets(fixture,entries),guard.digest(b'disposable native fixture'))
        recovery=Path(result['recovery_manifest']);audit=json.loads(guard._read(fixture,recovery,limit=guard.LIMIT_PAYLOAD))
        existing=next(x for x in audit['files'] if x['path']=='docs/existing.md')
        created=next(x for x in audit['files'] if x['path']=='docs/new.md')
        if (sample.read_bytes()!=b'updated\r\n' or (fixture/'docs/new.md').read_bytes()!=b'created\n'
            or guard._read(fixture,recovery.parent/existing['backup'])!=old
            or audit['status']!='applied_source_only' or not created['creation_intent'] or not created['applied']):
            raise RuntimeError('Native helper apply/backup/readback fixture failed')
        again=guard.apply(fixture,entries,guard.inspect_targets(fixture,entries),guard.digest(b'disposable native fixture'))
        if again['applied']!=0 or again['recovery_manifest'] is not None:raise RuntimeError('Native identical apply was not a no-op')
    return {'status':'passed','windows_handle_backend':os.name=='nt','production_source_writes':0,
            'readonly_deny_write_delete':True,'raw_crlf_backup':True,'creation_intent':True,'identical_noop':True,
            'junctions_tested':False,'power_loss_durability_verified':False}

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('payload','helper','checker','classpath','smoke'):parser.add_argument('--'+name+'-sha256',required=True)
    parser.add_argument('--apply-after-pass',action='store_true');args=parser.parse_args(argv)
    if sys.platform!='win32' or Path(__file__).absolute()!=ROOT/'tools/check_numbered_candidate.py':raise RuntimeError('Only the exact Windows Coraline checker path is supported')
    if free_gib()<START_FREE_GIB:raise RuntimeError('Below 640 MiB free; no fixture, compile or source application started')
    for name in ('payload','helper','checker','classpath','smoke'):
        value=getattr(args,name+'_sha256')
        if len(value)!=64 or any(x not in '0123456789abcdef' for x in value):raise ValueError('Invalid required SHA256 pin')
    helper_path=ROOT/HELPER;bootstrap_regular(helper_path);bootstrap_regular(Path(__file__))
    helper_raw=helper_path.read_bytes()
    if hashlib.sha256(helper_raw).hexdigest()!=args.helper_sha256:raise RuntimeError('Helper pin mismatch')
    if hashlib.sha256(Path(__file__).read_bytes()).hexdigest()!=args.checker_sha256:raise RuntimeError('Checker pin mismatch')
    guard=types.ModuleType('numbered_apply_guard');guard.__file__=str(helper_path);sys.modules[guard.__name__]=guard
    exec(compile(helper_raw,str(helper_path),'exec'),guard.__dict__)
    entries=guard.load_payload(ROOT,args.payload_sha256);before=guard.inspect_targets(ROOT,entries)
    if any(row['state']=='CONFLICT' for row in before):
        print(json.dumps({'stopped':'baseline conflict; no source writes or compiler','targets':before},indent=2));return 3
    cp_raw=guard._read(ROOT,ROOT/CP);smoke_raw=guard._read(ROOT,ROOT/SMOKE)
    pins={'tools/check_numbered_candidate.py':args.checker_sha256,HELPER:args.helper_sha256,
          guard.PAYLOAD:args.payload_sha256,CP:args.classpath_sha256,SMOKE:args.smoke_sha256}
    if guard.digest(cp_raw)!=args.classpath_sha256 or guard.digest(smoke_raw)!=args.smoke_sha256:raise RuntimeError('Classpath or smoke pin mismatch')
    jars=[Path(x) for x in cp_raw.decode('utf-8-sig').splitlines() if x]
    if not jars or len(jars)>600 or len(set(map(str,jars)))!=len(jars):raise RuntimeError('Invalid cached dependency list')
    for path in jars:
        if (not path.is_absolute() or not path.is_relative_to(GRADLE_CACHE) or '..' in path.parts
                or path.suffix.lower() not in ('.jar','.zip')):raise RuntimeError('Unexpected cached dependency path: '+str(path))
    compiled=ROOT/'build-character-cast/classes/java/main'
    output=ROOT/'build-character-cast/numbered-candidate-check'/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+uuid.uuid4().hex[:8])
    with ExitStack() as stack:
        guard._mkdir_pinned(ROOT,output,stack,exclusive=True)
        source=output/'candidate';classes=output/'classes';empty=output/'empty-sourcepath'
        for path in (source,classes,empty):guard._mkdir_pinned(ROOT,path,stack,exclusive=True)
        held=HeldInputs(guard,stack)
        for name,pin in pins.items():held.pin(ROOT,ROOT/name,limit=guard.LIMIT_PAYLOAD if name==guard.PAYLOAD else guard.LIMIT_FILE,expected=pin)
        dependencies={};total=0
        for path in jars:
            total+=path.lstat().st_size
            if total>MAX_ALL_DEPENDENCIES:raise RuntimeError('Dependency total exceeds two GiB bound')
            dependencies[str(path)]=held.pin(GRADLE_CACHE,path,limit=MAX_DEPENDENCY_BYTES)
        compiled_pins=lock_tree(guard,ROOT,compiled,held)
        report={'started_utc':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),'payload_sha256':args.payload_sha256,
                'pins':pins,'targets':before,'dependency_sha256':dependencies,'dependency_bytes':total,
                'compiled_api_files':len(compiled_pins),'free_gib':free_gib(),'apply_requested':args.apply_after_pass,
                'limits':'Direct Java17 candidate compile and tabletop smoke; optional exact source-only application. No full Gradle/Mixin/GameTests/gameplay/visual/audio/release/install verification. No sandbox or power-loss durability claim.'}
        candidate_class_pins=None;java_sources=[]
        def save():(output/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
        def unchanged(stage):
            require_empty(guard,ROOT,empty,'Explicit sourcepath')
            if candidate_class_pins is None:require_empty(guard,ROOT,classes,'Pre-javac classes directory')
            elif compiled_fingerprints(guard,ROOT,classes)!=candidate_class_pins:raise RuntimeError(stage+': compiled candidate tree changed')
            held.verify(stage)
            if compiled_fingerprints(guard,ROOT,compiled)!=compiled_pins:raise RuntimeError(stage+': compiled API tree changed')
            now=guard.inspect_targets(ROOT,entries)
            if now!=before:raise RuntimeError(stage+': Windows source baseline changed')
        def run(name,command,seconds):
            nonlocal candidate_class_pins
            unchanged('before '+name);available=free_gib()
            if available<START_FREE_GIB:raise RuntimeError('Before '+name+': below 640 MiB free; child not started')
            log=output/(name+'.log');reached=threading.Event();errors=[]
            report[name]={'status':'running','command':command,'timeout_seconds':seconds,'free_gib':available,
                          'log':str(log),'maximum_log_bytes':MAX_LOG_BYTES};save();started=time.monotonic()
            with log.open('xb') as stream:
                process=subprocess.Popen(command,cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,env=child_environment())
                reader=threading.Thread(target=drain_bounded,args=(process.stdout,stream,MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
                outcome=watch_child(process,seconds,free_gib,process_memory,log_limit=reached)
                reader.join(timeout=10)
                if reader.is_alive():raise RuntimeError('Owned child output did not close; no later stage started')
                process.stdout.close()
            if reached.is_set():outcome['status']='log_size_cutoff' if not errors else 'log_read_failed'
            if errors:outcome['log_errors']=errors
            report[name].update(outcome,elapsed_seconds=time.monotonic()-started);save();print(tail_bytes(log),flush=True)
            if outcome['status']!='passed' or type(outcome.get('exit_code')) is not int or outcome['exit_code']!=0:raise RuntimeError(name+' did not pass with an actual zero exit; no later stage')
            if name=='javac':
                candidate_class_pins=lock_tree(guard,ROOT,classes,held)
                report['candidate_class_files']=len(candidate_class_pins)
                report['candidate_class_tree_sha256']=guard.digest(json.dumps(candidate_class_pins,sort_keys=True,separators=(',',':')).encode());save()
            unchanged('after '+name)
        try:
            for entry in entries:
                path=source/entry.name;guard._mkdir_pinned(ROOT,path.parent,stack)
                with guard._file(ROOT,path,write=True,create=True) as stream:guard._write_all(stream,entry.data)
                held.pin(ROOT,path,limit=guard.LIMIT_FILE,expected=guard.digest(entry.data))
                if path.suffix=='.java':java_sources.append(path)
            expected_java=sum(entry.path.suffix.lower()=='.java' for entry in entries)
            if len(java_sources)!=expected_java:raise RuntimeError('A Java payload source was excluded from compilation')
            smoke=output/'TabletopRulesSmoke.java'
            with guard._file(ROOT,smoke,write=True,create=True) as stream:guard._write_all(stream,smoke_raw)
            held.pin(ROOT,smoke,limit=guard.LIMIT_FILE,expected=args.smoke_sha256);java_sources.append(smoke)
            if not 1<=len(java_sources)<=200:raise RuntimeError('Unexpected explicit Java input count')
            report['java_sources']=[str(p.relative_to(output)) for p in java_sources];save()
            unchanged('before native helper fixture');report['native_guard_fixture']=native_guard_fixture(guard,output);save();unchanged('after native helper fixture')
            # Fresh compiler output is never an input dependency of that compilation.
            compile_cp=os.pathsep.join([str(compiled),*map(str,jars)])
            smoke_cp=os.pathsep.join([str(classes),str(compiled),*map(str,jars)])
            quote=lambda value:'"'+str(value).replace('\\','/').replace('"','\\"')+'"'
            arguments=['-encoding','UTF-8','--release','17','-proc:none','-implicit:none','-sourcepath',quote(empty),'-classpath',quote(compile_cp),'-d',quote(classes),*[quote(p) for p in java_sources]]
            argfile=output/'javac.args';raw='\n'.join(arguments).encode()
            with guard._file(ROOT,argfile,write=True,create=True) as stream:guard._write_all(stream,raw)
            held.pin(ROOT,argfile,limit=guard.LIMIT_FILE,expected=guard.digest(raw));save();print('Candidate check output: '+str(output),flush=True)
            run('javac',[str(JAVA/'javac.exe'),*JAVAC_CAPS,'@'+str(argfile)],300)
            run('tabletop-smoke',[str(JAVA/'java.exe'),*SMOKE_CAPS,'-cp',smoke_cp,'TabletopRulesSmoke'],120)
            report['status']='candidate_checks_passed';save()
            if args.apply_after_pass:
                if report['native_guard_fixture'].get('windows_handle_backend') is not True:raise RuntimeError('Source application needs real Windows fixture success')
                unchanged('before source application')
                result=guard.apply(ROOT,entries,before,args.payload_sha256)
                if result['status'] not in ('applied_source_only','already_present_source_only'):raise RuntimeError('Source application did not complete')
                if not all(row['state']=='already_present_identical' for row in guard.inspect_targets(ROOT,entries)):raise RuntimeError('Final applied source differs from compiled candidate')
                report['application']=result;report['status']='candidate_checks_passed_source_applied';save();print('PINNED_SOURCE_APPLICATION_OK',flush=True)
            else:print('PINNED_CANDIDATE_CHECKS_OK',flush=True)
            return 0
        except Exception as error:
            report['status']='stopped';report['error']=str(error);save();print('Stopped: '+str(error),flush=True);return 1

if __name__=='__main__':raise SystemExit(main())
