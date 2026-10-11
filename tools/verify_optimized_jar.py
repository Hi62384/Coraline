"""Check a release against its asset masters and a previous unoptimized release."""
from pathlib import Path
import io
import json
import sys
import zipfile
import subprocess
from PIL import Image
from optimize_runtime_assets import sound_files, pcm_key

root = Path(__file__).resolve().parents[1]
jar_path, baseline = map(Path, sys.argv[1:3])
build = jar_path.resolve().parent.parent
report = json.loads((build/'reports/asset-size.json').read_text())
source = root/'src/main/resources/assets/coraline'
runtime = build/'resources/main/assets/coraline'
aliases = {x['file']: x['shared_with'] for x in report['duplicate_sounds']}
with zipfile.ZipFile(jar_path) as jar, zipfile.ZipFile(baseline) as old:
    assert jar.testzip() is None
    names = set(jar.namelist())
    removed = set(old.namelist()) - names
    # Reviewed retirement: held lighting no longer places invisible world blocks.
    # Keep the asset-removal check strict; only this obsolete bookkeeping class
    # may additionally disappear relative to releases using physical held lights.
    obsolete_classes = {'com/coraline/clothing/UtilityEffects$Glow.class'}
    assert removed-obsolete_classes == {'assets/coraline/sounds/'+name+'.ogg' for name in aliases}, removed
    assert not any(name.endswith('GameTests.class') for name in names)
    originals = json.loads((source/'sounds.json').read_text())
    events = json.loads(jar.read('assets/coraline/sounds.json'))
    timings = json.loads(jar.read('assets/coraline/visit_audio_lengths.json'))
    assert set(originals) == set(events), 'Lost sound events'
    for event, value in originals.items():
        if list(sound_files({event: value}, 'coraline')):
            assert timings.get(event, 0) > 0, 'Missing timing: '+event
        for i, sound in enumerate(value.get('sounds', [])):
            if isinstance(sound, dict) and sound.get('type', 'file') != 'file':
                continue
            name = sound if isinstance(sound, str) else sound['name']
            relative = name.removeprefix('coraline:')
            if relative in aliases:
                if isinstance(sound, str):
                    value['sounds'][i] = 'coraline:'+aliases[relative]
                else:
                    sound['name'] = 'coraline:'+aliases[relative]
        assert value == events[event], 'Changed playback settings: '+event
    for name in sound_files(events, 'coraline'):
        assert 'assets/coraline/sounds/'+name+'.ogg' in names, 'Missing sound: '+name
    count = 0
    for name in names:
        if name.endswith('.json'):
            json.loads(jar.read(name))
        if '/textures/' not in name or not name.endswith('.png'):
            continue
        original = root/'src/main/resources'/name
        if not original.exists():
            continue
        with Image.open(original) as a, Image.open(io.BytesIO(jar.read(name))) as b:
            limit = 256 if '/textures/item/' in name else 512
            if max(a.size) <= limit or original.with_suffix('.png.mcmeta').exists():
                assert a.size == b.size and a.convert('RGBA').tobytes() == b.convert('RGBA').tobytes(), name
            else:
                expected = a.convert('RGBA'); expected.thumbnail((limit, limit), Image.Resampling.LANCZOS)
                assert b.size == expected.size and b.convert('RGBA').tobytes() == expected.tobytes(), name
            count += 1

def probe(path):
    return json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries',
        'stream=sample_rate,channels,duration', '-of', 'json', str(path)], text=True))['streams'][0]

for item in report['resampled_sounds']:
    name = item['file']
    original = probe(source/'sounds'/(name+'.ogg'))
    packaged = probe(runtime/'sounds'/(aliases.get(name, name)+'.ogg'))
    assert int(packaged['sample_rate']) == 48000
    assert original['channels'] == packaged['channels']
    assert abs(float(original['duration'])-float(packaged['duration'])) < .03, name
for name, canonical in aliases.items():
    assert pcm_key(source/'sounds'/(name+'.ogg')) == pcm_key(source/'sounds'/(canonical+'.ogg')), name
print(f'Validated {count} textures, {len(events)} sound events, JSON and ZIP integrity; only verified duplicate audio and retired held-light bookkeeping removed.')
print(f'Full duration/channels preserved for {len(report["resampled_sounds"])} resampled tracks.')
print(f'JAR: {baseline.stat().st_size/1e6:.2f} MB -> {jar_path.stat().st_size/1e6:.2f} MB '
      f'({(1-jar_path.stat().st_size/baseline.stat().st_size)*100:.1f}% smaller)')
