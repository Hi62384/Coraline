"""Explicit temporary build: missing voices get quiet cue audio and authored captions.

Never writes to a voice master/reference directory. Genuine existing runtime Oggs
remain byte-identical; genuine masters without an export are converted once.
Normal genuine-voice validators are deliberately unchanged and will reject this
build. The temporary inventory is a separate, packaged replacement checklist.
"""
from pathlib import Path
import hashlib, json, math, os, stat, subprocess
from visit_event_audio_catalog import EVENT_MANIFESTS, event_directory, check_reference, effective_recordings

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
FFMPEG=Path('C:/ffmpeg/ffmpeg.exe')
FFMPEG_PIN='0d47da15b8729af52277273ac44d138d5b5fe7b2fe4e09c1f1fa847713509e72'

def sha(path):
    h=hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):h.update(block)
    return h.hexdigest()

def local(relative):
    part=Path(relative)
    if part.is_absolute() or '..' in part.parts:raise ValueError('Unsafe project path')
    path=ROOT
    for component in part.parts:
        path=path/component
        if path.exists():
            info=path.lstat()
            if stat.S_ISLNK(info.st_mode) or getattr(info,'st_file_attributes',0)&0x400:raise ValueError('Reparse path refused: '+str(path))
    return path

def ffmpeg(*args):
    subprocess.run([str(FFMPEG),'-v','error','-nostdin',*map(str,args)],check=True,timeout=120)

def duration(path):
    # Decode the actual packaged Ogg, not a guessed metadata duration.
    result=subprocess.run([str(FFMPEG),'-v','error','-nostdin','-i',str(path),'-f','s16le','-ac','1','-ar','24000','-'],stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True,timeout=120)
    return len(result.stdout)/(24000*2)

def main():
    if sha(FFMPEG)!=FFMPEG_PIN:raise RuntimeError('Approved FFmpeg identity changed')
    sounds_path=ASSETS/'sounds.json';lengths_path=ASSETS/'visit_audio_lengths.json'
    sounds=json.loads(sounds_path.read_text());lengths=json.loads(lengths_path.read_text())
    entries=[];genuine=[];shared={};created=[];measured={}
    effective=effective_recordings(ROOT)
    for filename in EVENT_MANIFESTS:
        manifest=json.loads(local(filename).read_text());check_reference(ROOT,manifest)
        father='otherfather' in manifest['reference'];category=event_directory(manifest)
        for state,profile in manifest['states'].items():
            for index,text in enumerate(profile['lines'],1):
                name=state if father else 'other_mother_events_'+category+'_'+state.replace('/','_')+f'_{index:02d}'
                recording=effective[name];text=recording['text']
                src=local(recording['source']);asset=recording['resource']
                target=local(f'src/main/resources/assets/coraline/sounds/{asset}.ogg')
                if src.is_file():
                    source_hash=sha(src)
                    if not target.is_file():
                        target.parent.mkdir(parents=True,exist_ok=True)
                        ffmpeg('-i',src,'-c:a','libvorbis','-q:a','4',target);created.append(str(target.relative_to(ROOT)))
                    if asset not in measured:measured[asset]=duration(target)
                    seconds=measured[asset]
                    if sha(src)!=source_hash:raise RuntimeError('Genuine master changed during export')
                    genuine.append({'event':name,'source':str(src.relative_to(ROOT)),'source_sha256':source_hash,'resource':asset,'ogg_sha256':sha(target),'decode_validated':True,'voice_quality_not_newly_validated':True})
                else:
                    # Caption reading time controls gates; this is explicitly not synthesized dialogue.
                    seconds=max(2,math.ceil(len(text.split())/2.6+.5))
                    asset=f'temporary_event_audio/cue_{seconds}s'
                    target=local(f'src/main/resources/assets/coraline/sounds/{asset}.ogg')
                    if asset not in shared:
                        target.parent.mkdir(parents=True,exist_ok=True)
                        if not target.exists():
                            ffmpeg('-f','lavfi','-i','sine=frequency=660:sample_rate=24000:duration=0.18','-af',f'afade=t=out:st=0.08:d=0.1,volume=0.03,apad=whole_dur={seconds}','-t',seconds,'-c:a','libvorbis','-q:a','3',target)
                            created.append(str(target.relative_to(ROOT)))
                        actual=duration(target)
                        if abs(actual-seconds)>.03:raise RuntimeError('Temporary cue duration mismatch')
                        shared[asset]={'seconds':actual,'sha256':sha(target),'decode_validated':True}
                    seconds=shared[asset]['seconds']
                    entries.append({'event':name,'speaker':'Other Father' if father else 'Other Mother','text':text,'manifest':filename,'missing_master':str(src.relative_to(ROOT)),'replacement_resource':recording['resource'],'temporary_resource':asset,'seconds':seconds,'ticks':math.ceil(seconds*20),'ogg_sha256':shared[asset]['sha256']})
                sounds[name]={'sounds':[{'name':'coraline:'+asset,'stream':True}]}
                lengths[name]=math.ceil(seconds*20)
    report={'format':1,'build_kind':'placeholder-audio','temporary_events':entries,'shared_temporary_recordings':shared,'genuine_events':genuine,'created_runtime_files':created,'replacement_plan':'Generate only missing_master targets with the approved own-character reference and existing quality gates. Run the normal importer/subtitle builder without temporaryEventAudio, then genuine validators. Never reuse this report as voice-quality proof.'}
    for path,data in [(sounds_path,sounds),(lengths_path,lengths),(ASSETS/'temporary_event_audio.json',report)]:
        encoded=json.dumps(data,indent=2,ensure_ascii=False)+'\n';path.write_text(encoded,encoding='utf-8');assert json.loads(path.read_text())==data
    notice=local('src/main/resources/META-INF/TEMPORARY_EVENT_AUDIO_NOTICE.txt')
    notice.write_text(f'PLACEHOLDER-AUDIO BUILD: {len(entries)} missing parent lines use quiet cue audio and authored text. Genuine masters and existing runtime exports are preserved. This is not a fully voiced release. See assets/coraline/temporary_event_audio.json for IDs, hashes and replacement steps.\n',encoding='utf-8')
    print(f'Temporary audio prepared: {len(entries)} events, {len(shared)} shared cues; {len(genuine)} genuine events preserved/exported. No voices synthesized.',flush=True)

if __name__=='__main__':main()
