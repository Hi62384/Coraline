"""Serial, bounded native decoder regression; no game world or voice synthesis."""
from pathlib import Path
import hashlib, json, os, re, subprocess, threading, time
import zipfile
from import_numbered_original_audio import ogg_measure
import check_numbered_candidate as monitor
from run_low_memory_release import job
from ctypes import wintypes

root=Path(__file__).resolve().parents[1]
output=root/'build-character-cast/native-audio-check'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())
output.mkdir(parents=True,exist_ok=False)
dependencies=(root/monitor.CP).read_text(encoding='utf-8-sig').splitlines()
cp=os.pathsep.join([str(output),str(root/'build-character-cast/classes/java/main'),*dependencies])
report={'scope':'Minecraft OggAudioStream/STB PCM suffix only; no OpenAL or scene acceptance','steps':{}}
commands={
 'compile':[str(monitor.JAVA/'javac.exe'),'-J-Xmx128m','-proc:none','-cp',cp,'-d',str(output),str(root/'tools/NativeOggContinuitySmoke.java')],
 'decode':[str(monitor.JAVA/'java.exe'),'-Xmx128m','-XX:MaxDirectMemorySize=64m','-cp',cp,'NativeOggContinuitySmoke',str(root/'build-character-cast/resources/main/assets/coraline/sounds/numbered_originals')]
}
for label,command in commands.items():
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Insufficient memory before '+label)
    api,handle=job()
    try:
        with (output/(label+'.log')).open('xb') as log:
            child=subprocess.Popen(command,cwd=root,env=monitor.child_environment(),stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):
                child.kill();child.wait();raise RuntimeError('Cannot bind owned child')
            reached,errors=threading.Event(),[]
            reader=threading.Thread(target=monitor.drain_bounded,args=(child.stdout,log,monitor.MAX_LOG_BYTES,reached,errors),daemon=True);reader.start()
            result=monitor.watch_child(child,600,monitor.free_gib,monitor.process_memory,log_limit=reached)
            reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():result['status']='output_capture_failed'
            report['steps'][label]=result
    finally:
        api.CloseHandle(handle)
        (output/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(monitor.tail_bytes(output/(label+'.log')),flush=True)
    if result.get('status')!='passed' or result.get('exit_code')!=0:raise SystemExit(1)
observed={name:(int(frames),int(size)) for name,frames,size in re.findall(r'(\w+\.ogg) nativeFrames=(\d+) frameBytes=(\d+)',(output/'decode.log').read_text())}
runtime=root/'build-character-cast/resources/main/assets/coraline'
ledger=json.loads((runtime/'numbered_original_audio.json').read_text())['recordings']
identities={}
version=re.search(r'^mod_version=([A-Za-z0-9._-]+)$',(root/'gradle.properties').read_text(encoding='utf-8'),re.MULTILINE)
if not version:raise RuntimeError('Missing release version')
release=root/'build-character-cast/libs'/('coraline-'+version[1]+'.jar')
archive=zipfile.ZipFile(release)
report['release_jar_sha256']=hashlib.sha256(release.read_bytes()).hexdigest()
for event,entry in ledger.items():
    path=runtime/'sounds'/(entry['resource']+'.ogg')
    frames,size=observed[path.name]
    digest=hashlib.sha256(path.read_bytes()).hexdigest()
    packed=archive.read('assets/coraline/sounds/'+entry['resource']+'.ogg')
    measured=ogg_measure(packed)
    # Import provenance describes the unoptimized source export. Check actual
    # delivery bytes and Ogg granules after rate/quality optimization instead.
    if path.read_bytes()!=packed or frames!=measured['samples'] or size!=measured['channels']*2:
        raise RuntimeError('Native frame count or packaged recording identity mismatch: '+event)
    identities[event]={'native_frames':frames,'frame_bytes':size,'runtime_sha256':digest,
        'runtime_sample_rate':measured['sample_rate'],'source_export_sha256':entry['ogg_sha256']}
archive.close()
report['verified_recordings']=identities
report['status']='native_decoder_continuity_passed_not_scene_acceptance'
(output/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('Report: '+str(output/'report.json'))
