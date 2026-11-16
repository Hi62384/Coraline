"""Read-only release audit: references, original story masters, timings and size."""
from pathlib import Path
import argparse, hashlib, json, math, zipfile

ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / 'build-character-cast/libs/coraline-1.15.94-placeholder-audio.jar'
BASELINE_SHA256 = '32714031ea055d007b483fb5c2ae80fa66da1973f34203958c62568c0b9e1d99'


def digest(path):
    value = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            value.update(chunk)
    return value.hexdigest()


def size_comparison(jar, previous, expected_sha256):
    jar = Path(jar).resolve(strict=True)
    previous = Path(previous).resolve(strict=True)
    if jar.samefile(previous):
        raise ValueError('Release and baseline must be different files')
    actual = digest(previous)
    if actual != expected_sha256:
        raise ValueError('Baseline checksum changed; size comparison refused')
    before, after = previous.stat().st_size, jar.stat().st_size
    if before <= 0:
        raise ValueError('Baseline is empty')
    return {'previous_jar': str(previous), 'previous_sha256': actual,
            'previous_bytes': before, 'bytes': after, 'change_bytes': after - before,
            'change_percent': (after - before) * 100 / before}


def verify(jar, previous=BASELINE, previous_sha256=BASELINE_SHA256):
    jar = Path(jar).resolve(strict=True)
    comparison = size_comparison(jar, previous, previous_sha256)
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None, 'Corrupt archive'
        names = set(z.namelist())
        assert not any('GameTests' in n or 'VisualCheck' in n or n.endswith(('.wav', '.blend')) for n in names), 'QA classes/master assets packaged'
        assets = 'assets/coraline/'
        sounds = json.loads(z.read(assets + 'sounds.json'))
        lengths = json.loads(z.read(assets + 'visit_audio_lengths.json'))
        refs = 0
        for event, definition in sounds.items():
            for entry in definition.get('sounds', []):
                name = entry if isinstance(entry, str) else entry['name']
                kind = 'file' if isinstance(entry, str) else entry.get('type', 'file')
                namespace, resource = name.split(':', 1) if ':' in name else ('coraline', name)
                if namespace != 'coraline':
                    continue
                assert resource in sounds if kind == 'event' else f'{assets}sounds/{resource}.ogg' in names, (event, name)
                refs += 1
        for event, ticks in lengths.items():
            assert event in sounds and ticks > 0, ('Invalid timing', event)
        story = json.loads(z.read(assets + 'black_is_traditional_audio.json'))
        for letter in 'ABCDEFGHIJKLMNOPQRS':
            event = 'black_is_traditional_' + letter.lower()
            entry = story[letter]
            master = ROOT / 'voicelines/story/BlackIsTraditional' / (letter + '.wav')
            assert digest(master) == entry['source_sha256'], ('Master changed', letter)
            sound = sounds[event]['sounds'][0]['name'].removeprefix('coraline:')
            assert hashlib.sha256(z.read(assets + 'sounds/' + sound + '.ogg')).hexdigest() == entry['ogg_sha256'], ('Original export changed', letter)
            assert abs(lengths[event] - math.ceil(entry['seconds'] * 20)) <= 1, ('Timing mismatch', event)
        largest = [{'path': i.filename, 'compressed_bytes': i.compress_size} for i in sorted(z.infolist(), key=lambda i: i.compress_size, reverse=True)[:12]]
        asset_compressed = sum(i.compress_size for i in z.infolist() if i.filename.startswith('assets/'))
    return {'passed': True, 'jar': str(jar), **comparison, 'sha256': digest(jar),
            'coraline_sound_references': refs, 'timed_events': len(lengths),
            'original_story_recordings': len(story), 'masters_unchanged': True,
            'compressed_assets_bytes': asset_compressed,
            'size_review_warning': asset_compressed > 64 * 1024 * 1024,
            'largest_entries': largest}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('jar', type=Path)
    parser.add_argument('--previous', type=Path, default=BASELINE)
    parser.add_argument('--previous-sha256', default=BASELINE_SHA256)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    result = verify(args.jar, args.previous, args.previous_sha256)
    if args.report:
        args.report.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2))
