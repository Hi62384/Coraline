"""Verify all event masters, runtime mappings, timing, captions and real Ogg decoding."""
import argparse, hashlib, json, math, subprocess, wave, zipfile
from pathlib import Path
from visit_event_audio_catalog import EVENT_MANIFESTS, event_directory, effective_recordings

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
MANIFESTS={Path(filename).name:sum(len(profile['lines']) for profile in json.loads((ROOT/filename).read_text(encoding='utf-8'))['states'].values()) for filename in EVENT_MANIFESTS}
assert sum(MANIFESTS.values())==583, 'Incomplete full dialogue catalogue'
parser=argparse.ArgumentParser();parser.add_argument('--jar');parser.add_argument('--manifest',choices=list(MANIFESTS),action='append');args=parser.parse_args()
jar=zipfile.ZipFile(args.jar) if args.jar else None
read=lambda name:jar.read('assets/coraline/'+name) if jar else (ASSETS/name).read_bytes()
sounds=json.loads(read('sounds.json'));lengths=json.loads(read('visit_audio_lengths.json'));captions=json.loads(read('story_subtitles.json'));total=0
effective=effective_recordings(ROOT)
for filename,count in MANIFESTS.items():
    if args.manifest and filename not in args.manifest: continue
    manifest=json.loads((ROOT/'tools'/filename).read_text());source=ROOT/manifest['output']
    assert sum(len(p['lines']) for p in manifest['states'].values())==count
    assert hashlib.sha256((ROOT/manifest['reference']).read_bytes()).hexdigest()==manifest['reference_sha256']
    father='otherfather' in manifest['reference'];category=event_directory(manifest)
    for group,profile in manifest['states'].items():
        for index,text in enumerate(profile['lines'],1):
            name=group if father else 'other_mother_events_'+category+'_'+group.replace('/','_')+f'_{index:02d}'
            recording=effective[name];text=recording['text'];path=ROOT/recording['source']
            canonical=recording.get('shared_recording',name)
            if recording['resource'].startswith('compact_events/'):
                report_dir=path.parent.parent;key=canonical+'/line_01.wav'
            else:
                origin=json.loads((ROOT/recording['manifest']).read_text())
                report_dir=ROOT/origin['output']
                original_profile=recording['profile']
                key=recording['state']+'/'+original_profile.get('filename',f"line_{recording['index']:02d}.wav")
            report=json.loads((report_dir/'generation_report.json').read_text())
            validation=json.loads((report_dir/'validation_report.json').read_text())
            checked={c['file']:c for c in validation['checks']}
            assert validation['reference_unchanged'] and key in checked
            assert not checked[key].get('needs_review') and checked[key].get('pcm_decode')=='pass'
            assert report['lines'][key]['sha256']==hashlib.sha256(path.read_bytes()).hexdigest() and report['lines'][key]['text']==text
            if 'source_sha256' in checked[key]:assert checked[key]['source_sha256']==report['lines'][key]['sha256'],name
            if 'expected_text_sha256' in checked[key]:assert checked[key]['expected_text_sha256']==hashlib.sha256(text.encode('utf-8')).hexdigest(),name
            with wave.open(str(path),'rb') as audio: seconds=audio.getnframes()/audio.getframerate();assert audio.getnchannels()==1 and audio.getsampwidth()==2
            name=group if father else 'other_mother_events_'+category+'_'+group.replace('/','_')+f'_{index:02d}'
            asset=sounds[name]['sounds'][0]['name'].removeprefix('coraline:');raw=read('sounds/'+asset+'.ogg');assert raw.startswith(b'OggS')
            subprocess.run(['ffmpeg','-v','error','-i','pipe:0','-f','null','-'],input=raw,check=True,stdout=subprocess.DEVNULL)
            assert abs(lengths[name]-math.ceil(seconds*20))<=2 and lengths[name]>0
            track=captions[name];assert track['speaker']==('Other Father' if father else 'Other Mother') and track['cues']
            assert ' '.join(c['text'] for c in track['cues']).split()==text.split(),name
            total+=1
if jar:
    assert not any(n.endswith(('.wav','.mp3','.blend','.glb','.blueprint.json')) or any(t in n for t in ['GameTests','VisualCheck','RenderCheck']) for n in jar.namelist())
    assert 'META-INF/VISIT_EVENT_AUDIO_NOTICE.txt' in jar.namelist()
print(f'Validated all {total} genuine recordings, reference checksums, PCM/Ogg decoding, runtime timing and complete caption text'+(' in release JAR.' if jar else '.'))
