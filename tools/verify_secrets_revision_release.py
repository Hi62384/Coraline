"""Release integrity checks for the journal / interruption update."""
import json
import sys
import zipfile
from pathlib import Path

new = Path(sys.argv[1])
root = Path(__file__).resolve().parents[1]
old_size = 73896194  # Verified 1.15.41 release size, before the clean build.
with zipfile.ZipFile(new) as jar:
    assert jar.testzip() is None
    names = set(jar.namelist())
    assert not any('GameTests' in n or n.endswith('/empty.nbt') for n in names)
    assert not any(n.endswith(('.wav', '.mp3')) for n in names)
    sounds = json.loads(jar.read('assets/coraline/sounds.json'))
    before = json.loads((root/'src/main/resources/assets/coraline/sounds.json').read_text())
    assert set(before) == set(sounds)
    timing = json.loads(jar.read('assets/coraline/visit_audio_lengths.json'))
    for event, entry in sounds.items():
        for sound in entry.get('sounds', []):
            if isinstance(sound, dict) and sound.get('type') == 'event':
                continue
            path = sound if isinstance(sound, str) else sound['name']
            if path.startswith('minecraft:'):
                continue
            assert 'assets/coraline/sounds/' + path.removeprefix('coraline:') + '.ogg' in names, path
            assert timing.get(event, 0) > 0, event
    charts = json.loads(jar.read('assets/coraline/festival_charts.json'))
    for chart in charts:
        assert chart['duration'] <= 3600 and not chart['score']
        assert len(chart['notes']) > 150
        assert all(b[0] - a[0] >= 5 for a, b in zip(chart['notes'], chart['notes'][1:]))
        assert all(1 <= len(n[3]) <= 3 and n[2] in n[3] for n in chart['notes'])
    assert 'assets/coraline/models/item/journal_discovery_debugger.json' in names
    for parent in ('mom', 'dad'):
        for index in range(8):
            assert f'assets/coraline/textures/entity/parents/{parent}_{index}.png' in names
    print(f'Validated {len(sounds)} sound events and {len(charts)} charts; no test classes or audio masters.')
    print(f'JAR: {new.stat().st_size:,} bytes; change from verified 1.15.41 size {new.stat().st_size-old_size:+,} bytes')
    print('Largest compressed entries:')
    for entry in sorted(jar.infolist(), key=lambda n: n.compress_size, reverse=True)[:10]:
        print(f'{entry.compress_size:>10,}  {entry.filename}')
