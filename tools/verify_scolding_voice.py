"""Verify exact text, reference identity, full runtime duration and all ten resources."""
from pathlib import Path
import hashlib
import json
import math
import subprocess

ROOT = Path(__file__).resolve().parents[1]
manifest = json.loads((ROOT/'tools/other_mother_scolding.json').read_text(encoding='utf-8'))
assets = ROOT/'src/main/resources/assets/coraline'
sounds = json.loads((assets/'sounds.json').read_text())
lengths = json.loads((assets/'visit_audio_lengths.json').read_text())
captions = json.loads((assets/'story_subtitles.json').read_text(encoding='utf-8'))
assert hashlib.sha256((ROOT/manifest['reference']).read_bytes()).hexdigest() == manifest['reference_sha256']
names = set()
total = 0
generation = json.loads((ROOT/manifest['output']/'generation_report.json').read_text(encoding='utf-8'))
for key, profile in manifest['states'].items():
    name = profile['sound_name']
    assert name not in names
    names.add(name)
    assert ' '.join(segment['text'] for segment in profile['segments']) == profile['lines'][0]
    source = ROOT/manifest['output']/profile['source_directory']/profile['filename']
    relative = 'visit/'+manifest['resource_folder']+'/'+profile['resource_path']
    runtime = assets/'sounds'/(relative+'.ogg')
    assert source.is_file() and runtime.is_file(), name
    generated = generation['lines'][key+'/'+profile['filename']]
    assert generated['sha256'] == hashlib.sha256(source.read_bytes()).hexdigest(), name
    assert generated['text'] == profile['lines'][0] and generated['peak_dbfs'] < -.1, name
    for path in (source, runtime):
        subprocess.run(['ffmpeg','-v','error','-i',str(path),'-f','null','-'],check=True)
    assert sounds[name]['sounds'] == [{'name':'coraline:'+relative,'stream':True}], name
    durations = [float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(path)],text=True)) for path in (source,runtime)]
    assert abs(durations[0]-durations[1]) < .05, name
    assert lengths[name] == math.ceil(durations[1]*20), name
    assert captions[name]['speaker'] == 'Other Mother'
    assert ' '.join(c['text'] for c in captions[name]['cues']) == profile['lines'][0], name
    total += durations[0]
assert len(names)==10
print(f'PASS: 10 cloned masters, streamed OGGs, exact captions and timings; {total:.2f}s total.')
