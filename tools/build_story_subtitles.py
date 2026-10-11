"""Build dialogue captions; transcribe legacy recordings once, never during a normal build."""
from pathlib import Path
import json, re, sys, subprocess

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/coraline'
CACHE = ROOT / 'tools/story_transcripts.json'
sounds = json.loads((ASSETS/'sounds.json').read_text())
captions = {}
legacy = ['wybie_line_a', 'wybie_line_b', 'wybie_line_c', 'other_mother_intro',
          'other_father_enter_scene', 'other_father_intro', 'other_father_dinners_ready',
          'other_father_golden_chicken_breast', 'other_parents_waiting',
          'other_mother_visit_again', 'other_mother_supper_0', 'other_mother_supper_1',
          'other_mother_supper_2', 'see_you_soon']

def speaker(name):
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
if '--transcribe' in sys.argv:
    import torch, whisper
    torch.set_num_threads(6)
    model = whisper.load_model('base.en', device='cpu', download_root=str(ROOT/'.voice-tools/whisper'))
    for name in legacy:
        if name in cache: continue
        resource = sounds[name]['sounds'][0]['name'].split(':')[1]
        audio = ASSETS/'sounds'/f'{resource}.ogg'
        result = model.transcribe(str(audio), language='en', fp16=False, condition_on_previous_text=False)
        cache[name] = [{'start': s['start'], 'end': s['end'], 'text': s['text'].strip()} for s in result['segments']]
        CACHE.write_text(json.dumps(cache, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
        print(name, result['text'], flush=True)

for name in legacy:
    if name not in cache: raise RuntimeError(f'Missing cached transcript: {name}')
    captions[name] = {'speaker': speaker(name), 'cues': [cue for segment in cache[name] for cue in chunks(segment['text'], segment['start'], segment['end'])]}

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
for name, event in sounds.items():
    if name.startswith(('mom_', 'dad_', 'parent_voice_')) or name == 'angry_parent':
        assert name not in captions and 'subtitle' not in event, f'Normal parent subtitle leaked: {name}'
assert all(name in captions for name in legacy), 'Story caption coverage lost'
assert 'other_father_song' not in captions, 'Piano song must not have subtitles'
(ASSETS/'story_subtitles.json').write_text(json.dumps(captions, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
print(f'Built subtitles for {len(captions)} story recordings.')
