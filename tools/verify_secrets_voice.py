"""Verify the complete sixty-line resource contract, without subjective acting claims."""
from pathlib import Path
import hashlib
import json
import math
import subprocess

ROOT = Path(__file__).resolve().parents[1]
manifest = json.loads((ROOT/'tools/other_mother_secrets.json').read_text(encoding='utf-8'))
assets = ROOT/'src/main/resources/assets/coraline'
sounds = json.loads((assets/'sounds.json').read_text())
lengths = json.loads((assets/'visit_audio_lengths.json').read_text())
captions = json.loads((assets/'story_subtitles.json').read_text(encoding='utf-8'))
assert hashlib.sha256((ROOT/manifest['reference']).read_bytes()).hexdigest() == manifest['reference_sha256']
names = set()
groups = {}
total = 0
for profile in manifest['states'].values():
    name = profile['sound_name']
    assert name not in names
    names.add(name)
    groups.setdefault((profile['event'],profile['dialogue_state']),set()).add(profile['variation'])
    source = ROOT/manifest['output']/profile['source_directory']/profile['filename']
    relative = 'visit/'+manifest['resource_folder']+'/'+profile['resource_path']
    runtime = assets/'sounds'/(relative+'.ogg')
    assert source.is_file() and runtime.is_file(), name
    assert sounds[name]['sounds'] == [{'name':'coraline:'+relative,'stream':True}], name
    durations = [float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(path)],text=True)) for path in (source,runtime)]
    assert abs(durations[0]-durations[1]) < .05, name
    assert lengths[name] == math.ceil(durations[1]*20), name
    assert captions[name]['speaker'] == 'Other Mother'
    assert ' '.join(c['text'] for c in captions[name]['cues']) == profile['lines'][0], name
    total += durations[0]
assert len(names)==60 and len(groups)==30 and all(v=={1,2} for v in groups.values())
print(f'PASS: 60 masters, 60 streamed OGGs, all 30 event/state pairs, exact captions and timings; {total:.2f}s total.')
