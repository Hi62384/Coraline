"""Scolding release-specific checks in addition to verify_optimized_jar.py."""
from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parents[1]
jar_path = root/'build-activity/libs/coraline-1.15.41.jar'
previous = root/'build-activity/libs/coraline-1.15.40.jar'
manifest = json.loads((root/'tools/other_mother_scolding.json').read_text())
with zipfile.ZipFile(jar_path) as jar, zipfile.ZipFile(previous) as old:
    names = jar.namelist()
    assert 'version="1.15.41"' in jar.read('META-INF/mods.toml').decode()
    assert not any('GameTests' in n or any(k in n for k in ('ScoldingAudioCheck','SecretsAudioCheck','SecretsVisualCheck','DollRenderCheck')) for n in names)
    assert not any(n.endswith(('.wav','.mp3')) or n.endswith('/empty.nbt') for n in names)
    events = json.loads(jar.read('assets/coraline/sounds.json'))
    old_events = json.loads(old.read('assets/coraline/sounds.json'))
    assert set(old_events) <= set(events), 'An existing sound event disappeared'
    captions = json.loads(jar.read('assets/coraline/story_subtitles.json'))
    durations = json.loads(jar.read('assets/coraline/visit_audio_lengths.json'))
    for p in manifest['states'].values():
        name = p['sound_name']
        assert durations[name] > 500
        assert ' '.join(c['text'] for c in captions[name]['cues']) == p['lines'][0]
        for sound in events[name]['sounds']:
            assert sound['stream']
            path='assets/coraline/sounds/'+sound['name'].removeprefix('coraline:')+'.ogg'
            assert path in names and len(jar.read(path)) > 10000
    largest = [{'path':i.filename,'compressed_bytes':i.compress_size} for i in sorted(jar.infolist(),key=lambda i:i.compress_size,reverse=True)[:10]]
result = {'jar':str(jar_path),'bytes':jar_path.stat().st_size,'previous_bytes':previous.stat().st_size,
          'sha256':hashlib.sha256(jar_path.read_bytes()).hexdigest(),'largest_entries':largest,
          'new_scolds':10,'all_previous_sounds_preserved':True,'qa_code_and_source_audio_excluded':True}
(root/'build-activity/reports/scolding-release.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
