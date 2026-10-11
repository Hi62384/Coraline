"""Optimize copied build resources only; never modify source/master assets.

Requires Pillow and FFmpeg (already used by the project's asset pipeline).
Audio deduplication compares decoded PCM, not unreliable Ogg container hashes.
"""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import argparse
import hashlib
import io
import json
import subprocess
import shutil
import tempfile
import zlib
import math
from PIL import Image

VERSION = '1'


def optimize_png(data, limit, animated=False):
    if animated:
        return data  # Frame dimensions/metadata must be handled by an authored export.
    with Image.open(io.BytesIO(data)) as source:
        image = source.copy()
        if max(image.size) > limit:
            image = image.convert('RGBA')
            image.thumbnail((limit, limit), Image.Resampling.LANCZOS)
        output = io.BytesIO()
        image.save(output, format='PNG', optimize=True, compress_level=9)
        result = output.getvalue()
        return result if len(result) < len(data) else data


def pcm_key(path):
    info = json.loads(subprocess.check_output([
        'ffprobe', '-v', 'error', '-show_entries', 'stream=sample_rate,channels',
        '-of', 'json', str(path)], text=True))['streams'][0]
    pcm = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(path),
        '-map', '0:a:0', '-c:a', 'pcm_s32le', '-f', 'hash', '-hash', 'sha256', '-'], text=True).strip()
    return f"{info['sample_rate']}:{info['channels']}:{pcm}"


def sound_files(sounds, namespace):
    for event in sounds.values():
        for sound in event.get('sounds', []):
            if isinstance(sound, dict) and sound.get('type', 'file') != 'file':
                continue
            name = sound if isinstance(sound, str) else sound['name']
            domain, separator, relative = name.partition(':')
            if not separator:
                domain, relative = namespace, name
            if domain == namespace:
                yield relative


def deduplicate_sounds(assets, cache, resampled=None):
    mapping = assets / 'sounds.json'
    if not mapping.exists():
        return []
    sounds = json.loads(mapping.read_text(encoding='utf-8'))
    namespace = assets.name
    paths = sorted(set(sound_files(sounds, namespace)))
    fingerprint_file = cache / 'pcm-hashes.json'
    fingerprints = json.loads(fingerprint_file.read_text()) if fingerprint_file.exists() else {}

    def fingerprint(name):
        path = assets / 'sounds' / (name + '.ogg')
        if not path.is_file():
            raise ValueError('Missing registered sound: ' + str(path))
        content_hash = hashlib.sha256(path.read_bytes()).hexdigest()
        key = fingerprints.get(content_hash) or pcm_key(path)
        original_hash, original_key = content_hash, key
        reduced = None
        if int(key.split(':')[0]) > 48000:
            # Some imported music is 192 kHz. Keep duration/channels/gain but export
            # at standard 48 kHz, never upsample voices or recompress normal-rate audio.
            cached = cache / (content_hash + '-48k-q5.ogg')
            if not cached.exists():
                with tempfile.TemporaryDirectory(dir=cache) as temporary:
                    encoded = Path(temporary) / 'audio.ogg'
                    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(path),
                        '-map', '0:a:0', '-ar', '48000', '-c:a', 'libvorbis', '-q:a', '5', str(encoded)], check=True)
                    encoded.replace(cached)
            if cached.stat().st_size < path.stat().st_size:
                reduced = {'file': name, 'before': path.stat().st_size, 'after': cached.stat().st_size,
                           'source_rate': int(key.split(':')[0]), 'runtime_rate': 48000}
                shutil.copyfile(cached, path)
                content_hash = hashlib.sha256(path.read_bytes()).hexdigest()
                key = fingerprints.get(content_hash) or pcm_key(path)
        return name, content_hash, key, original_hash, original_key, reduced

    with ThreadPoolExecutor(max_workers=4) as workers:
        records = list(workers.map(fingerprint, paths))
    canonical, replacements = {}, {}
    for name, content_hash, key, original_hash, original_key, reduced in records:
        fingerprints[content_hash] = key
        fingerprints[original_hash] = original_key
        if reduced is not None and resampled is not None:
            resampled.append(reduced)
        if key in canonical:
            replacements[name] = canonical[key]
        else:
            canonical[key] = name
    fingerprint_file.write_text(json.dumps(fingerprints, sort_keys=True))
    for event in sounds.values():
        for i, sound in enumerate(event.get('sounds', [])):
            if isinstance(sound, dict) and sound.get('type', 'file') != 'file':
                continue
            name = sound if isinstance(sound, str) else sound['name']
            relative = name.removeprefix(namespace + ':')
            if relative in replacements:
                replacement = namespace + ':' + replacements[relative]
                if isinstance(sound, str):
                    event['sounds'][i] = replacement
                else:
                    sound['name'] = replacement
    mapping.write_text(json.dumps(sounds, separators=(',', ':'), ensure_ascii=False), encoding='utf-8')
    # All events (and their stream/volume/pitch/subtitle settings) remain intact.
    referenced = set(sound_files(sounds, namespace))
    removed = []
    for name, replacement in replacements.items():
        assert name not in referenced and replacement in referenced
        path = assets / 'sounds' / (name + '.ogg')
        removed.append({'file': name, 'shared_with': replacement, 'bytes': path.stat().st_size})
        path.unlink()  # Only duplicate files under the validated generated output directory.
    for name in referenced:
        assert (assets / 'sounds' / (name + '.ogg')).is_file(), name
    return removed


def rebuild_durations(assets, cache):
    """Derive a COMPLETE table from packaged audio, independent of skipped import tasks."""
    sounds_path = assets/'sounds.json'
    if not sounds_path.exists() or not (assets/'visit_audio_lengths.json').exists():
        return
    sounds = json.loads(sounds_path.read_text())
    cache_file = cache/'audio-durations.json'
    durations = json.loads(cache_file.read_text()) if cache_file.exists() else {}
    lengths = {}
    for event, definition in sounds.items():
        ticks = []
        for name in sound_files({event: definition}, assets.name):
            audio = assets/'sounds'/(name+'.ogg')
            key = hashlib.sha256(audio.read_bytes()).hexdigest()
            if key not in durations:
                seconds = float(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries',
                    'format=duration', '-of', 'csv=p=0', str(audio)], text=True))
                if not math.isfinite(seconds) or seconds <= 0:
                    raise ValueError('Invalid sound duration: '+str(audio))
                durations[key] = math.ceil(seconds*20)
            ticks.append(durations[key])
        if ticks:
            lengths[event] = max(ticks)
    (assets/'visit_audio_lengths.json').write_text(json.dumps(lengths, sort_keys=True, separators=(',', ':')))
    cache_file.write_text(json.dumps(durations, sort_keys=True))
    print(f'Validated {len(lengths)} packaged audio durations.')


def optimize(resources, cache, report):
    root = Path(__file__).resolve().parents[1]
    resources, cache = resources.resolve(), cache.resolve()
    # Refuse arbitrary/source directories; only standard generated build outputs are writable.
    if resources not in [root / 'build/resources/main', root / 'build-activity/resources/main']:
        raise ValueError('Expected generated build/resources/main, never a source directory')
    cache.mkdir(parents=True, exist_ok=True)
    before = sum(p.stat().st_size for p in resources.rglob('*') if p.is_file())
    textures = []
    for path in sorted(resources.glob('assets/*/textures/**/*.png')):
        relative = path.relative_to(resources).as_posix()
        limit = 256 if '/textures/item/' in relative else 512
        animated = path.with_suffix('.png.mcmeta').exists()
        data = path.read_bytes()
        key = hashlib.sha256(data + f'{VERSION}:{limit}:{animated}'.encode()).hexdigest()
        cached = cache / (key + '.png')
        if not cached.exists():
            cached.write_bytes(optimize_png(data, limit, animated))
        result = cached.read_bytes()
        if result != data:
            path.write_bytes(result)
            with Image.open(io.BytesIO(result)) as image:
                textures.append({'file': relative, 'before': len(data), 'after': len(result), 'size': image.size})
    duplicates, resampled = [], []
    for assets in sorted((resources / 'assets').iterdir()):
        if assets.is_dir():
            duplicates.extend(deduplicate_sounds(assets, cache, resampled))
            rebuild_durations(assets, cache)
    after = sum(p.stat().st_size for p in resources.rglob('*') if p.is_file())
    # Resource budget measures packaged cost, not highly compressible mesh JSON size.
    packed = sum(len(zlib.compress(p.read_bytes(), 6)) for p in resources.rglob('*') if p.is_file())
    result = {'before_bytes': before, 'after_bytes': after, 'saved_bytes': before-after,
              'estimated_packaged_bytes': packed,
              'textures': textures, 'duplicate_sounds': duplicates, 'resampled_sounds': resampled}
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps(result, indent=2))
    print(f'Runtime assets: {before/1048576:.1f} -> {after/1048576:.1f} MiB; '
          f'{len(textures)} optimized PNGs, {len(duplicates)} shared recordings, '
          f'{len(resampled)} high-rate recordings exported at 48 kHz. Originals preserved.')
    if packed > 64 * 1048576:
        print('ASSET BUDGET WARNING: packaged resources exceed 64 MiB; inspect ' + str(report))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--resources', type=Path, required=True)
    parser.add_argument('--cache', type=Path, required=True)
    parser.add_argument('--report', type=Path, required=True)
    args = parser.parse_args()
    optimize(args.resources, args.cache, args.report)
