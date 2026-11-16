"""Build dialogue captions; transcribe legacy recordings once, never during a normal build."""
from pathlib import Path
import json, re, sys, subprocess, hashlib, stat
from visit_event_audio_catalog import EVENT_MANIFESTS, event_directory, effective_recordings
from original_story_audio_catalog import originals as original_catalog
from incidental_dialogue_catalog import recordings as incidental_recordings

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/coraline'
CACHE = ROOT / 'tools/story_transcripts.json'
ORIGINALS = original_catalog(ROOT)
ORIGINAL_TRANSCRIPTS = ROOT/'tools/numbered_original_transcript_sources.json'
original_proof = json.loads(ORIGINAL_TRANSCRIPTS.read_text(encoding='utf-8')) if ORIGINAL_TRANSCRIPTS.exists() else {}
def transcript_hash(segments):
    return hashlib.sha256(json.dumps(segments,sort_keys=True,ensure_ascii=False).encode('utf-8')).hexdigest()
def original_verified(name, segments):
    proof=original_proof.get(name,{})
    return proof.get('source_sha256')==ORIGINALS[name]['source_sha256'] and proof.get('transcript_sha256')==transcript_hash(segments) and proof.get('review_status')=='approved' and not proof.get('quality_warnings',[])

def original_input(name):
    """Read only the declared original master; reject changed bytes or reparse paths."""
    entry=ORIGINALS[name]
    relative=Path(entry['source'])
    if relative.is_absolute() or '..' in relative.parts:
        raise RuntimeError('Invalid original source path: '+name)
    path=ROOT
    for part in relative.parts:
        path=path/part
        info=path.lstat()
        if stat.S_ISLNK(info.st_mode) or getattr(info,'st_file_attributes',0)&0x400:
            raise RuntimeError('Original source is a reparse point: '+name)
    if not stat.S_ISREG(info.st_mode) or info.st_size!=entry['source_bytes']:
        raise RuntimeError('Original source size/type changed: '+name)
    digest=hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):digest.update(block)
    if digest.hexdigest()!=entry['source_sha256']:
        raise RuntimeError('Original source identity changed: '+name)
    return path
sounds = json.loads((ASSETS/'sounds.json').read_text())
temporary_audio='--temporary-audio' in sys.argv
temporary_path=ASSETS/'temporary_event_audio.json'
temporary_report=json.loads(temporary_path.read_text()) if temporary_audio and temporary_path.exists() else {}
temporary_events={entry['event']:entry for entry in temporary_report.get('temporary_events',[])}
active_temporary=[name for name,event in sounds.items() if any(isinstance(entry,dict) and str(entry.get('name','')).startswith('coraline:temporary_event_audio/') for entry in event.get('sounds',[]))]
if active_temporary and not temporary_audio and not ('--originals-only' in sys.argv and '--transcribe' in sys.argv):
    raise RuntimeError('Temporary event audio is active; use the explicitly labelled temporaryEventAudio build or replace it with genuine voices')
if temporary_audio and set(active_temporary)!=set(temporary_events):
    raise RuntimeError('Temporary audio inventory disagrees with actual sound mappings')
captions = {}
legacy = ['wybie_line_a', 'wybie_line_b', 'wybie_line_c', 'other_mother_intro',
          'other_father_enter_scene', 'other_father_intro', 'other_father_dinners_ready',
          'other_father_golden_chicken_breast', 'other_parents_waiting',
          'other_mother_visit_again', 'other_mother_supper_0', 'other_mother_supper_1',
          'other_mother_supper_2', 'see_you_soon']
visits = [f'{who}_visit_{letter}' for who,letters in [('wybie','abcdefghijkl'),('bobinsky','abcdefghijk'),('tea','abcdefghij')] for letter in letters]
legacy += visits
legacy += [name for name,entry in ORIGINALS.items() if not entry['music']]
legacy=list(dict.fromkeys(legacy))

def speaker(name):
    if name in ORIGINALS: return ORIGINALS[name]['speaker']
    if name.startswith('bobinsky_visit_'): return 'Mr. Bobinsky'
    if name.startswith('tea_visit_'): return 'Miss Spink & Miss Forcible'
    return 'Wybie' if name.startswith('wybie') else 'Other Father' if name.startswith('other_father') else 'Other Parents' if name == 'other_parents_waiting' else 'Other Mother'

def chunks(text, start, end):
    words = text.strip().split()
    groups = []
    for word in words:
        if not groups or len(groups[-1]) + len(word) > 84: groups.append(word)
        else: groups[-1] += ' ' + word
    total = sum(len(group) for group in groups) or 1
    at = start
    result = []
    for group in groups:
        until = at + (end-start)*len(group)/total
        result.append({'start': round(at, 3), 'end': round(until, 3), 'text': group})
        at = until
    return result

cache = json.loads(CACHE.read_text(encoding='utf-8')) if CACHE.exists() else {}
originals_only='--originals-only' in sys.argv
if originals_only and '--transcribe' not in sys.argv:
    raise RuntimeError('--originals-only requires --transcribe; normal builds always compile every caption')
if '--transcribe' in sys.argv or '--review-visits' in sys.argv:
    # Fail before importing a model if any original master is missing or changed.
    original_inputs={name:original_input(name) for name,entry in ORIGINALS.items() if not entry['music']}
    model_name='small.en' if '--accurate' in sys.argv else 'base.en'
    checkpoint=ROOT/'.voice-tools/whisper'/f'{model_name}.pt'
    if not checkpoint.is_file():
        raise RuntimeError('Existing local transcription checkpoint is missing: '+model_name)
    import torch, whisper
    torch.set_num_threads(6)
    # An explicit existing checkpoint path cannot trigger a model download.
    if '--memory-efficient' in sys.argv:
        # Same local checkpoint and float32 model as whisper.load_model, but
        # memory-map its read-only tensor storage instead of retaining a second
        # full private copy while constructing the CPU model. No model export,
        # precision reduction, network download or source-recording edit.
        state=torch.load(str(checkpoint),map_location='cpu',mmap=True,weights_only=True)
        model=whisper.model.Whisper(whisper.model.ModelDimensions(**state['dims']))
        model.load_state_dict(state['model_state_dict'])
        del state
    else:
        model = whisper.load_model(str(checkpoint), device='cpu')
    for name in legacy:
        if originals_only and name not in original_inputs: continue
        if name in cache and not ('--review-visits' in sys.argv and name in visits) and (name not in ORIGINALS or original_verified(name,cache[name])): continue
        resource = sounds[name]['sounds'][0]['name'].split(':')[1]
        audio = original_inputs[name] if name in original_inputs else ASSETS/'sounds'/f'{resource}.ogg'
        result = model.transcribe(str(audio), language='en', fp16=False, condition_on_previous_text=False, temperature=0, beam_size=5)
        if name in original_inputs: original_input(name)  # Do not save proof for changed input.
        cache[name] = [{'start': s['start'], 'end': s['end'], 'text': s['text'].strip()} for s in result['segments']]
        CACHE.write_text(json.dumps(cache, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
        if name in ORIGINALS:
            warnings=[]
            if not result['segments'] or not result['text'].strip(): warnings.append('empty_transcription')
            for index,segment in enumerate(result['segments']):
                if segment.get('avg_logprob',0)<-1.0 or segment.get('no_speech_prob',0)>.6 or segment.get('compression_ratio',0)>2.4: warnings.append('uncertain_segment_'+str(index))
            original_proof[name]={'source_sha256':ORIGINALS[name]['source_sha256'],'transcript_sha256':transcript_hash(cache[name]),'engine':'whisper small.en' if '--accurate' in sys.argv else 'whisper base.en','review':'automated transcription; no human listening claimed','review_status':'pending','quality_warnings':warnings}
            ORIGINAL_TRANSCRIPTS.write_text(json.dumps(original_proof,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
        print(name, result['text'], flush=True)

if originals_only:
    pending=[name for name,entry in ORIGINALS.items() if not entry['music'] and (name not in cache or not original_verified(name,cache[name]))]
    if pending: raise RuntimeError('Original speech requires transcript review: '+', '.join(pending))
    print('Original speech transcript proofs saved. Full subtitle export remains a separate normal build step.')
    raise SystemExit(0)

for name in legacy:
    if temporary_audio and name in ORIGINALS and (name not in cache or not original_verified(name,cache[name])):
        # Unreviewed transcription is not dialogue. Keep developer status out of gameplay.
        captions[name]={'speaker':speaker(name),'cues':[]}
        continue
    if name not in cache: raise RuntimeError(f'Missing cached transcript: {name}')
    if name in ORIGINALS and not original_verified(name,cache[name]): raise RuntimeError('Original recording needs a matching, quality-checked transcript: '+name)
    duration = json.loads((ASSETS/'visit_audio_lengths.json').read_text()).get(name,0)/20 if name in visits or name in ORIGINALS else max(s['end'] for s in cache[name])
    captions[name] = {'speaker': speaker(name), 'cues': [cue for segment in cache[name] if segment['start']<duration for cue in chunks(segment['text'], segment['start'], min(duration,segment['end']))]}

lengths = json.loads((ASSETS/'visit_audio_lengths.json').read_text())
for path in sorted((ROOT/'tools').glob('other_mother_*.json')):
    manifest = json.loads(path.read_text(encoding='utf-8'))
    prefix = manifest.get('prefix', 'welcome')
    for state, profile in manifest['states'].items():
        for index, line in enumerate(profile['lines'], 1):
            name = f'other_mother_welcome_{state}_{index:02d}' if prefix == 'welcome' else f'other_mother_{state.replace("/", "_")}_{index:02d}'
            if 'sound_prefix' in manifest: name = f'{manifest["sound_prefix"]}_{index:02d}'
            if 'sound_name' in profile: name = profile['sound_name']
            captions[name] = {'speaker': 'Other Mother', 'cues': chunks(line, 0, lengths[name]/20)}

father_manifest_path = ROOT/'tools/other_father_activity_intros.json'
if father_manifest_path.exists():
    manifest = json.loads(father_manifest_path.read_text(encoding='utf-8'))
    for state, profile in manifest['states'].items():
        for line in profile['lines']:
            name = profile['sound_name']
            captions[name] = {'speaker': 'Other Father', 'cues': chunks(line, 0, lengths[name]/20)}

# Normal parents are deliberately excluded from both dialogue and vanilla sound subtitles.
effective_event_voices = effective_recordings(ROOT)
for name,record in incidental_recordings(ROOT).items():
    captions[name]={'speaker':'Other Father' if 'otherfather' in record['reference'] else 'Other Mother',
                    'cues':chunks(record['text'],0,lengths[name]/20)}
for filename in EVENT_MANIFESTS:
    path=ROOT/filename
    if not path.exists(): raise RuntimeError('Missing required event voice manifest: '+filename)
    manifest=json.loads(path.read_text(encoding='utf-8'));father='otherfather' in manifest['reference']
    category=event_directory(manifest)
    for state,profile in manifest['states'].items():
        for index,line in enumerate(profile['lines'],1):
            name=state if father else 'other_mother_events_'+category+'_'+state.replace('/','_')+f'_{index:02d}'
            line=effective_event_voices[name]['text']
            voice_speaker='Other Father' if father else 'Other Mother'
            if name in temporary_events:voice_speaker+=' (temporary audio)'
            captions[name]={'speaker':voice_speaker,'cues':chunks(line,0,lengths[name]/20)}

# Normal parents are deliberately excluded from both dialogue and vanilla sound subtitles.
for name, event in sounds.items():
    if name.startswith(('mom_', 'dad_', 'parent_voice_')) or name == 'angry_parent':
        assert name not in captions and 'subtitle' not in event, f'Normal parent subtitle leaked: {name}'
assert all(name in captions for name in legacy), 'Story caption coverage lost'
assert 'other_father_song' not in captions, 'Piano song must not have subtitles'
(ASSETS/'story_subtitles.json').write_text(json.dumps(captions, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
# These lines are also used directly by server captions/conversation panels.
# Keep them identical to the spoken take, not the archived alternate script.
numbered_path = ASSETS/'numbered_event_dialogue.json'
if numbered_path.exists():
    numbered = json.loads(numbered_path.read_text(encoding='utf-8'))
    for entry in numbered.values():
        recording = effective_event_voices.get(entry.get('sound'))
        if recording:
            entry['text'] = recording['text']
    numbered_path.write_text(json.dumps(numbered, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
print(f'Built subtitles for {len(captions)} story recordings.')
