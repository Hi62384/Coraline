"""Verify the built JAR's additive event dialogue/audio without accepting silent placeholders.

This complements verify_visit_events_audio.py and verify_secrets_revision_release.py.
It proves packaged identity/timing/caption coverage, not human listening or speaker
similarity. The existing check_welcome_audio.py --transcribe --speaker-check gate
must still pass for all generated manifests before release.
"""
from pathlib import Path
import argparse, hashlib, json, math, re, shutil, subprocess, tempfile, zipfile
from visit_event_audio_catalog import EVENT_MANIFESTS, event_directory, check_reference, effective_recordings
from import_numbered_original_audio import EXPECTED_IDS, load_manifest, ogg_measure
from original_story_audio_catalog import originals as supplied_originals
from voice_recording_proof import validate as validate_voice, legacy_recordings
from incidental_dialogue_catalog import recordings as incidental_recordings
ROOT=Path(__file__).resolve().parents[1]
PREFIX='assets/coraline/'


def file_sha256(path):
    digest=hashlib.sha256()
    with Path(path).open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):digest.update(block)
    return digest.hexdigest()


def verify(jar, root=ROOT, decode=True):
    root=root.resolve();ffmpeg=shutil.which('ffmpeg') if decode else None
    if decode and not ffmpeg:raise RuntimeError('Existing ffmpeg is required for full decode validation')
    generated={};effective=effective_recordings(root);quality_cache={}
    legacy=legacy_recordings(root)
    if len(legacy)!=285 or legacy.keys() & effective.keys():raise AssertionError('Incomplete/overlapping legacy generated dialogue catalogue')
    incidental=incidental_recordings(root)
    if len(incidental)!=78 or incidental.keys() & (legacy.keys() | effective.keys()):raise AssertionError('Incomplete/overlapping incidental dialogue catalogue')
    voice_proofs=[validate_voice(root,record,quality_cache) for record in [*effective.values(),*legacy.values(),*incidental.values()]]
    for filename in EVENT_MANIFESTS:
        manifest=json.loads((root/filename).read_text(encoding='utf-8'));check_reference(root,manifest)
        directory=event_directory(manifest);father='otherfather' in manifest['reference']
        for state,profile in manifest['states'].items():
            for index,line in enumerate(profile['lines'],1):
                name=state if father else 'other_mother_events_'+directory+'_'+state.replace('/','_')+f'_{index:02d}'
                if name in generated:raise AssertionError('Duplicate runtime sound ID: '+name)
                generated[name]=('Other Father' if father else 'Other Mother',effective[name]['text'])
    if len(generated)!=583:raise AssertionError(f'Expected 269 original +314 new generated lines, got {len(generated)}')
    all_generated={**generated,**{name:('Other Father' if 'otherfather' in record['reference'] else 'Other Mother',record['text']) for name,record in {**legacy,**incidental}.items()}}
    originals=load_manifest(root);supplied=supplied_originals(root)
    original_transcripts=json.loads((root/'tools/story_transcripts.json').read_text(encoding='utf-8'))
    original_proofs=json.loads((root/'tools/numbered_original_transcript_sources.json').read_text(encoding='utf-8'))
    for name,pin in supplied.items():
        if file_sha256(root/pin['source'])!=pin['source_sha256']:
            raise AssertionError('Supplied original master changed: '+name)
        if pin['music']:continue
        transcript=original_transcripts.get(name,[]);proof=original_proofs.get(name,{})
        digest=hashlib.sha256(json.dumps(transcript,sort_keys=True,ensure_ascii=False).encode('utf-8')).hexdigest()
        if not transcript or proof.get('source_sha256')!=pin['source_sha256'] or proof.get('transcript_sha256')!=digest or proof.get('review_status')!='approved' or proof.get('quality_warnings',[]):
            raise AssertionError('Matching quality-checked original transcript missing: '+name)
    with zipfile.ZipFile(jar) as z, tempfile.TemporaryDirectory(prefix='coraline-audio-check-') as temporary:
        names=z.namelist()
        if len(names)!=len(set(names)):raise AssertionError('Duplicate ZIP entries')
        for name in names:
            low=name.lower()
            if low.endswith(('.wav','.mp3','.flac','.blend','.glb','.safetensors','.pt','.pth')) or '/.voice-tools/' in low or 'gametests.class' in low or 'visualcheck' in low:
                raise AssertionError('Master/model/test payload leaked into release: '+name)
        read=lambda name:json.loads(z.read(PREFIX+name))
        sounds=read('sounds.json');lengths=read('visit_audio_lengths.json');captions=read('story_subtitles.json')
        incidental_runtime=read('incidental_dialogue.json')
        expected_incidental={(name,'Other Father' if 'otherfather' in record['reference'] else 'Other Mother',record['text']) for name,record in incidental.items()}
        if (len(incidental_runtime)!=len(expected_incidental) or
                {(r['sound'],r['speaker'],r['text']) for r in incidental_runtime}!=expected_incidental):
            raise AssertionError('Packaged incidental dispatch differs from exact speaker/script catalogue')
        if len(captions)!=len(all_generated)+sum(not pin['music'] for pin in supplied.values()):
            raise AssertionError('Incomplete/unexpected full subtitle inventory')
        runtime=read('numbered_event_dialogue.json');provenance=read('numbered_original_audio.json')['recordings']
        authored=json.loads((root/'src/main/resources/assets/coraline/numbered_event_dialogue.json').read_text(encoding='utf-8'))
        if runtime!=authored or len(runtime)!=376:raise AssertionError('Packaged numbered dialogue changed or incomplete')
        text_only=[key for key,value in runtime.items() if not value.get('sound')]
        if len(text_only)!=62:raise AssertionError('Expected 62 explicit unresolved-voice text lines')
        if any('wybie' in value['speaker'].lower() for value in runtime.values()):raise AssertionError('Other Wybie must remain silent')
        if set(provenance)!=EXPECTED_IDS:raise AssertionError('Original audio provenance incomplete')
        measured={}
        for name in list(all_generated)+list(supplied):
            event=sounds.get(name,{})
            entries=event.get('sounds',[])
            if len(entries)!=1:raise AssertionError('Missing/ambiguous registered sound: '+name)
            entry=entries[0];resource=entry if isinstance(entry,str) else entry['name']
            if ':' not in resource:resource='coraline:'+resource
            namespace,path=resource.split(':',1)
            if namespace!='coraline' or not re.fullmatch(r'[a-z0-9_/.-]+',path) or '..' in path.split('/'):
                raise AssertionError('Unexpected sound resource: '+resource)
            payload=z.read('assets/'+namespace+'/sounds/'+path+'.ogg');info=ogg_measure(payload)
            if lengths.get(name)!=info['ticks']:raise AssertionError('Audio timing differs from Vorbis granules: '+name)
            if name in all_generated and (info['seconds']<=1.5 or info['channels']!=1):raise AssertionError('Generated speech is empty/short/non-mono: '+name)
            if name in originals:
                pin=originals[name];proof=provenance[name]
                if path!=pin['resource'] or proof['source_sha256']!=pin['source_sha256'] or proof['ogg_sha256']!=hashlib.sha256(payload).hexdigest():
                    raise AssertionError('Reused original identity/provenance mismatch: '+name)
                for field in ('ticks','samples','sample_rate','channels'):
                    if proof[field]!=info[field]:raise AssertionError('Original metadata mismatch: '+name+'/'+field)
                if pin['music']:
                    if name in captions:raise AssertionError('Music must not receive invented spoken captions: '+name)
                else:
                    caption=captions.get(name,{})
                    expected=' '.join(segment['text'].strip() for segment in original_transcripts[name] if segment['start']<info['ticks']/20)
                    actual=' '.join(cue['text'] for cue in caption.get('cues',[]))
                    if caption.get('speaker')!=pin['speaker'] or ' '.join(actual.split())!=' '.join(expected.split()):
                        raise AssertionError('Packaged original caption differs from source-matched transcript: '+name)
            if name in supplied and name not in originals:
                pin=supplied[name]
                if hashlib.sha256(payload).hexdigest()!=pin['ogg_sha256']:
                    raise AssertionError('Supplied runtime recording changed: '+name)
                if pin['music']:
                    if name in captions:raise AssertionError('Music must not receive invented spoken captions: '+name)
                else:
                    expected=' '.join(segment['text'].strip() for segment in original_transcripts[name] if segment['start']<info['ticks']/20)
                    actual=' '.join(cue['text'] for cue in captions.get(name,{}).get('cues',[]))
                    if captions.get(name,{}).get('speaker')!=pin['speaker'] or ' '.join(actual.split())!=' '.join(expected.split()):
                        raise AssertionError('Supplied original caption differs from reviewed transcript: '+name)
            if name in all_generated:
                speaker,text=all_generated[name];caption=captions.get(name,{})
                if caption.get('speaker')!=speaker or ' '.join(cue['text'] for cue in caption.get('cues',[]))!=' '.join(text.split()):
                    raise AssertionError('Exact generated caption or speaker mismatch: '+name)
            if name in captions:
                end=0
                for cue in captions[name]['cues']:
                    start=float(cue['start']);until=float(cue['end'])
                    if not math.isfinite(start) or not math.isfinite(until) or start<0 or start<end-.002 or until<=start or until>info['ticks']/20+.002 or not cue['text'].strip():
                        raise AssertionError('Invalid/out-of-recording subtitle interval: '+name)
                    end=until
            if decode:
                audio=Path(temporary)/(name+'.ogg');audio.write_bytes(payload)
                subprocess.run([ffmpeg,'-nostdin','-v','error','-threads','1','-i',str(audio),'-f','null','-'],check=True,timeout=180)
                audio.unlink()
            measured[name]=info['ticks']
    return {'jar':str(jar),'jar_sha256':file_sha256(jar),
            'generated_recordings':len(all_generated),'generated_event_recordings':len(generated),'legacy_generated_recordings':len(legacy),'incidental_generated_recordings':len(incidental),'original_recordings':len(originals),'numbered_written_lines':376,
            'supplied_speech_captions':sum(not pin['music'] for pin in supplied.values()),
            'generated_master_quality_checks':len(voice_proofs),
            'legacy_validation_receipts_without_per_check_hash':sum(not proof['validation_source_hash_present'] for proof in voice_proofs),
            'explicit_text_only_lines':62,'decoded_all':decode,'audio_timing_checked':len(measured),
            'human_listening':False,'speaker_quality_gate':'Run existing manifest transcription and speaker checks separately; this verifier does not replace them.'}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('jar',type=Path);parser.add_argument('--report',type=Path)
    args=parser.parse_args();result=verify(args.jar)
    if args.report:args.report.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(result,indent=2))
