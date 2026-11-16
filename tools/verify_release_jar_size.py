"""Audit the actual Forge distributable without modifying it or running tests."""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import zipfile


def audit(path):
    categories = Counter()
    duplicates = defaultdict(list)
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        required = {'META-INF/mods.toml', 'coraline.mixins.json',
                    'assets/coraline/sounds.json', 'assets/coraline/story_subtitles.json',
                    'com/coraline/CoralineMod.class'}
        if not required <= names:
            raise ValueError('Incomplete runtime archive: ' + str(required - names))
        manifest = jar.read('META-INF/MANIFEST.MF').decode('utf-8')
        if 'Audio-Build-Kind: genuine-audio' not in manifest:
            raise ValueError('Expected genuine audio release')
        forbidden = [n for n in names if n.endswith(('.blend', '.wav', '.mp3', '.py', '.jar'))
                     or 'GameTests.class' in n or 'VisualCheck.class' in n
                     or n.startswith(('build/', '.git/', 'tools/'))]
        if forbidden:
            raise ValueError('Development files in runtime: ' + str(forbidden[:20]))
        for entry in jar.infolist():
            if entry.is_dir():
                continue
            category = ('audio' if entry.filename.endswith('.ogg') else
                        'textures' if entry.filename.endswith('.png') else
                        'classes' if entry.filename.endswith('.class') else
                        'models' if '/sculpted/' in entry.filename else 'other')
            categories[category] += entry.compress_size
            duplicates[hashlib.sha256(jar.read(entry)).hexdigest()].append(entry.filename)
        sounds = json.loads(jar.read('assets/coraline/sounds.json'))
        lengths = json.loads(jar.read('assets/coraline/visit_audio_lengths.json'))
        for event, definition in sounds.items():
            for sound in definition.get('sounds', []):
                if isinstance(sound, dict) and sound.get('type', 'file') != 'file':
                    continue
                name = sound if isinstance(sound, str) else sound['name']
                namespace, sep, relative = name.partition(':')
                if not sep:
                    namespace, relative = 'coraline', name
                if namespace == 'coraline':
                    if f'assets/coraline/sounds/{relative}.ogg' not in names:
                        raise ValueError('Missing registered recording: ' + name)
                    if lengths.get(event, 0) <= 0:
                        raise ValueError('Missing event timing: ' + event)
        largest = [{'file': i.filename, 'compressed_bytes': i.compress_size,
                    'raw_bytes': i.file_size} for i in
                   sorted(jar.infolist(), key=lambda i: i.compress_size, reverse=True)[:40]]
    size = path.stat().st_size
    return {'jar': str(path.resolve()), 'bytes': size, 'decimal_mb': size / 1_000_000,
            'sha256': hashlib.file_digest(path.open('rb'), 'sha256').hexdigest(),
            'size_status': 'PRIMARY TARGET PASSED' if size < 80_000_000 else
                           'FALLBACK TARGET PASSED — PRIMARY TARGET MISSED' if size < 100_000_000 else
                           'SIZE REQUIREMENT FAILED',
            'compressed_categories': dict(categories), 'largest_entries': largest,
            'exact_duplicate_groups': [v for v in duplicates.values() if len(v) > 1],
            'registered_events': len(sounds), 'gameplay_verified': False,
            'audio_visual_qa': 'Not assessed by this archive-only audit; consult separate runtime acceptance reports'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    result = audit(args.jar)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f"{result['size_status']}: {result['bytes']:,} bytes; SHA256 {result['sha256']}")
    if result['bytes'] >= 100_000_000:
        raise SystemExit(1)
