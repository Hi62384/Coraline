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
import re
import struct
from PIL import Image, ImageChops, ImageStat

VERSION = '8'


def verified_speech_masters(root):
    from voice_recording_proof import legacy_recordings
    from visit_event_audio_catalog import effective_recordings
    from incidental_dialogue_catalog import recordings
    result = {}
    for catalogue in (legacy_recordings(root), effective_recordings(root), recordings(root)):
        for record in catalogue.values():
            source = (root / record['source']).resolve(strict=True)
            if not source.is_relative_to(root / 'voicelines') or source.suffix.lower() != '.wav':
                raise ValueError('Expected original checked speech PCM')
            resource = record['resource']
            if resource in result and result[resource] != source:
                raise ValueError('Conflicting original speech source: ' + resource)
            result[resource] = source
    return result


def export_budget_audio(assets, cache):
    """Produce the approved smaller runtime exports from untouched inputs."""
    mapping = assets / 'sounds.json'
    if not mapping.exists():
        return []
    root = Path(__file__).resolve().parents[1]
    sounds = json.loads(mapping.read_text(encoding='utf-8'))
    subtitles = assets / 'story_subtitles.json'
    captions = json.loads(subtitles.read_text(encoding='utf-8')) if subtitles.exists() else {}
    masters = verified_speech_masters(root) if assets.name == 'coraline' else {}
    usage = {}
    for event, definition in sounds.items():
        for name in sound_files({event: definition}, assets.name):
            usage.setdefault(name, []).append(event)
    changed = []
    for name, events in sorted(usage.items()):
        path = assets / 'sounds' / (name + '.ogg')
        original = root / 'src/main/resources/assets' / assets.name / 'sounds' / (name + '.ogg')
        if not original.is_file() or original.read_bytes() != path.read_bytes():
            continue
        music_words = ('music', 'ambient', 'song', 'baking', 'supper', 'exploration',
                       'coraline_garden', 'garden_fly', 'circus_race')
        speech = all(bool(captions.get(event, {}).get('cues')) and
                     not any(word in event for word in music_words) for event in events)
        master = masters.get(name) if speech else None
        source = master or original
        music = (any(any(word in event for word in music_words) for event in events)
                 and not any(bool(captions.get(event, {}).get('cues')) for event in events))
        quality = '1' if master or music else '3'
        digest = hashlib.sha256(source.read_bytes()).hexdigest()
        key = f'{digest}-budget-q{quality}-pcm-voice24' if master else f'{digest}-budget-q3-voice24' if speech else f'{digest}-budget-q{quality}-music'
        output, proof = cache / (key + '.ogg'), cache / (key + '.json')
        if not proof.exists() or not output.exists():
            info = json.loads(subprocess.check_output(['ffprobe', '-v', 'error',
                '-show_entries', 'stream=sample_rate,channels:format=duration', '-of', 'json', str(original)], text=True))
            stream = info['streams'][0]
            rate = min(int(stream['sample_rate']), 24000 if speech else 48000)
            with tempfile.TemporaryDirectory(dir=cache) as temporary:
                encoded = Path(temporary) / 'audio.ogg'
                subprocess.run(['ffmpeg', '-nostdin', '-v', 'error', '-y', '-threads', '1',
                    '-filter_threads', '1', '-i', str(source), '-map', '0:a:0', '-vn',
                    '-ar', str(rate), '-c:a', 'libvorbis', '-q:a', quality, str(encoded)], check=True)
                seconds = float(subprocess.check_output(['ffprobe', '-v', 'error',
                    '-show_entries', 'format=duration', '-of', 'csv=p=0', str(encoded)], text=True))
                if abs(seconds - float(info['format']['duration'])) > .002:
                    print('Keeping exact-timing original: ' + name, flush=True)
                    continue
                encoded.replace(output)
            proof.write_text(json.dumps({'source_sha256': digest, 'seconds': seconds,
                'rate': rate, 'channels': stream['channels'], 'speech': speech,
                'original_pcm_master': str(master.relative_to(root)) if master else None,
                'vorbis_quality': quality}), encoding='utf-8')
        if output.stat().st_size <= original.stat().st_size * .98:
            changed.append({'file': name, 'before': original.stat().st_size,
                'after': output.stat().st_size, **json.loads(proof.read_text())})
            shutil.copyfile(output, path)
    return changed


def optimize_png(data, limit, animated=False, palette_budget=False):
    if animated:
        return data  # Frame dimensions/metadata must be handled by an authored export.
    with Image.open(io.BytesIO(data)) as source:
        large_original = max(source.size) > 128
        image = source.copy()
        if max(image.size) > limit:
            image = image.convert('RGBA')
            image.thumbnail((limit, limit), Image.Resampling.LANCZOS)
        if image.mode == 'RGBA' and image.getextrema()[3] == (255, 255):
            image = image.convert('RGB')
        output = io.BytesIO()
        image.save(output, format='PNG', optimize=True, compress_level=9)
        result = output.getvalue()
        # Exact indexed palettes are lossless; do not quantize photographic art.
        rgba = image.convert('RGBA')
        if rgba.getcolors(256) is not None:
            indexed = rgba.convert('P', palette=Image.Palette.ADAPTIVE, colors=256)
            if indexed.convert('RGBA').tobytes() == rgba.tobytes():
                candidate = io.BytesIO()
                indexed.save(candidate, format='PNG', optimize=True, compress_level=9)
                if len(candidate.getvalue()) < len(result):
                    result = candidate.getvalue()
        elif palette_budget and large_original and max(image.size) > 64 and len(result) > 8192:
            # High-resolution world/gear artwork only. Keep every alpha value;
            # accept palette colour reduction only below a per-channel RMS of
            # 4/255. Faces, UI, skies, animation and small pixel art are excluded.
            rgb = rgba.convert('RGB')
            indexed = rgb.quantize(colors=256, method=Image.Quantize.MEDIANCUT,
                                   dither=Image.Dither.NONE)
            reduced = indexed.convert('RGBA')
            reduced.putalpha(rgba.getchannel('A'))
            error = ImageStat.Stat(ImageChops.difference(rgb, reduced.convert('RGB'))).rms
            if max(error) <= 4:
                candidate = io.BytesIO()
                opaque = rgba.getchannel('A').getextrema() == (255, 255)
                (indexed if opaque else reduced).save(candidate, format='PNG', optimize=True, compress_level=9)
                if len(candidate.getvalue()) < len(result):
                    result = candidate.getvalue()
        return result if len(result) < len(data) else data


def minify_json(data):
    """Remove insignificant whitespace without rewriting numeric literals."""
    text = data.decode('utf-8-sig')
    json.loads(text)
    quoted = escaped = False
    result = []
    for char in text:
        if quoted:
            result.append(char)
            if escaped:
                escaped = False
            elif char == '\\':
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
            result.append(char)
        elif char not in ' \t\r\n':
            result.append(char)
    return ''.join(result).encode('utf-8')


def shortest_mesh_floats(data):
    """Exactly preserve SculptedArmorModel's getAsFloat values, with shorter text."""
    pattern = r'"(?:[^"\\]|\\.)*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?'
    def replace(match):
        token = match.group()
        if token.startswith('"') or not any(c in token for c in '.eE'):
            return token
        value = float(token)
        if value == 0:
            return token  # Keep signed zero explicit across JSON readers too.
        bits = struct.pack('>f', value)
        narrowed = struct.unpack('>f', bits)[0]
        for digits in range(1, 10):
            candidate = format(narrowed, f'.{digits}g')
            if struct.pack('>f', float(candidate)) == bits:
                return candidate if len(candidate) < len(token) else token
        return token
    return re.sub(pattern, replace, data.decode('utf-8')).encode('utf-8')


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
    # Use a new cache namespace: old entries were based on FFprobe's rounded
    # presentation duration, which may differ from the final Vorbis granule.
    cache_file = cache/'audio-durations-vorbis-v2.json'
    durations = json.loads(cache_file.read_text()) if cache_file.exists() else {}
    lengths = {}
    for event, definition in sounds.items():
        ticks = []
        for name in sound_files({event: definition}, assets.name):
            audio = assets/'sounds'/(name+'.ogg')
            key = hashlib.sha256(audio.read_bytes()).hexdigest()
            if key not in durations:
                from import_numbered_original_audio import ogg_measure
                durations[key] = ogg_measure(audio.read_bytes())['ticks']
            ticks.append(durations[key])
        if ticks:
            lengths[event] = max(ticks)
    (assets/'visit_audio_lengths.json').write_text(json.dumps(lengths, sort_keys=True, separators=(',', ':')))
    cache_file.write_text(json.dumps(durations, sort_keys=True))
    print(f'Validated {len(lengths)} packaged audio durations.')


def prune_archived_voice_exports(assets):
    """Omit only unused exports of the archived alternate-script voice plan.

    Called on generated copies, never masters. Retained exact-script compact
    takes remain registered and therefore stay. Do not prune arbitrary sounds:
    other runtime systems may access those assets directly.
    """
    root=Path(__file__).resolve().parents[1]
    assets=Path(assets).resolve()
    if assets.is_relative_to(root/'src'):raise ValueError('Cannot prune source assets')
    if assets.name!='coraline' or not (assets/'sounds.json').is_file():return []
    referenced=set(sound_files(json.loads((assets/'sounds.json').read_text(encoding='utf8')),'coraline'))
    removed=[]
    for speaker in ('mother','father'):
        for path in sorted((assets/'sounds/compact_events'/speaker).rglob('*.ogg')):
            name=path.relative_to(assets/'sounds').with_suffix('').as_posix()
            if name in referenced:continue
            removed.append({'file':name,'bytes':path.stat().st_size})
            path.unlink()  # Archived export in the validated generated tree only.
    # Pre-rotation normal-parent exports. The current importer and ParentVoices
    # use parent_voice_groups.json and registered events, never these old names.
    for path in sorted((assets/'sounds/parents').glob('*.ogg')):
        if not re.fullmatch(r'(?:mom|dad)(?:clicked)?\d+', path.stem):
            continue
        name = path.relative_to(assets/'sounds').with_suffix('').as_posix()
        if name in referenced:
            continue
        removed.append({'file': name, 'bytes': path.stat().st_size,
                        'reason': 'obsolete pre-rotation parent export'})
        path.unlink()
    return removed


def runtime_texture_limit(relative):
    if relative == 'assets/coraline/textures/block/insect_parlor_wallpaper.png':
        return 512  # Authored wall detail required by the first-escape scene.
    if relative in ('assets/coraline/textures/entity/festival_cake_frosting_atlas.png',
                    'assets/coraline/textures/entity/festival_cake_sponge_atlas.png'):
        return 256
    return 128 if any(part in relative for part in ('/textures/item/', '/textures/block/')) else 256 if '/textures/models/sculpted/' in relative else 512


def optimize(resources, cache, report):
    root = Path(__file__).resolve().parents[1]
    resources, cache = resources.resolve(), cache.resolve()
    # Refuse arbitrary/source directories; only standard generated build outputs are writable.
    if resources not in [root / 'build/resources/main', root / 'build-activity/resources/main', root / 'build-character-cast/resources/main']:
        raise ValueError('Expected generated build/resources/main, never a source directory')
    cache.mkdir(parents=True, exist_ok=True)
    before = sum(p.stat().st_size for p in resources.rglob('*') if p.is_file())
    textures = []
    for path in sorted(resources.glob('assets/*/textures/**/*.png')):
        relative = path.relative_to(resources).as_posix()
        # Inventory icons and tileable surfaces still have eight times vanilla
        # resolution. Worn gear retains 256; faces, skies and UI retain 512.
        # All exports start from untouched masters, never an earlier thumbnail.
        limit = runtime_texture_limit(relative)
        metadata = path.with_suffix('.png.mcmeta')
        # Filtering/clamping metadata does not make a texture an animation.
        # Only actual animation strips need the frame-preserving export path.
        animated = metadata.exists() and 'animation' in json.loads(metadata.read_text(encoding='utf-8'))
        data = path.read_bytes()
        key = hashlib.sha256(data + f'{VERSION}:{limit}:{animated}'.encode()).hexdigest()
        cached = cache / (key + '.png')
        if not cached.exists():
            palette_budget = (limit <= 256 or
                              relative == 'assets/coraline/textures/block/insect_parlor_wallpaper.png')
            cached.write_bytes(optimize_png(data, limit, animated, palette_budget=palette_budget))
        result = cached.read_bytes()
        if result != data:
            path.write_bytes(result)
            with Image.open(io.BytesIO(result)) as image:
                textures.append({'file': relative, 'before': len(data), 'after': len(result), 'size': image.size})
    duplicates, resampled, archived_voice_exports, budget_audio = [], [], [], []
    baseline_entries = [{'file': p.relative_to(resources).as_posix(),
                         'compressed_bytes': len(zlib.compress(p.read_bytes(), 6))}
                        for p in resources.rglob('*') if p.is_file()]
    baseline_packed = sum(entry['compressed_bytes'] for entry in baseline_entries)
    oversized = baseline_packed > 75_000_000
    for assets in sorted((resources / 'assets').iterdir()):
        if assets.is_dir():
            if oversized:
                budget_audio.extend(export_budget_audio(assets, cache))
            duplicates.extend(deduplicate_sounds(assets, cache, resampled))
            archived_voice_exports.extend(prune_archived_voice_exports(assets))
            rebuild_durations(assets, cache)
    json_saved = 0
    for path in sorted(resources.rglob('*.json')):
        original = path.read_bytes()
        compact = minify_json(original)
        if '/sculpted/' in path.as_posix():
            compact = shortest_mesh_floats(compact)
        if len(compact) < len(original):
            path.write_bytes(compact)
            json_saved += len(original) - len(compact)
    after = sum(p.stat().st_size for p in resources.rglob('*') if p.is_file())
    # Resource budget measures packaged cost, not highly compressible mesh JSON size.
    packed = sum(len(zlib.compress(p.read_bytes(), 6)) for p in resources.rglob('*') if p.is_file())
    result = {'before_bytes': before, 'after_bytes': after, 'saved_bytes': before-after,
              'estimated_packaged_bytes': packed,
              'textures': textures, 'duplicate_sounds': duplicates, 'resampled_sounds': resampled,
              'archived_voice_exports':archived_voice_exports, 'budget_audio_exports':budget_audio,
              'json_whitespace_saved_bytes':json_saved,
              'optimizer_version': VERSION,
              'texture_policy': '128 px inventory/world tiles; 256 px worn gear; 512 px faces/skies/UI; alpha preserved; optional world/gear palette RMS <= 4/255; small pixel art and animation unchanged',
              'mesh_policy': 'SculptedArmorModel float32 values preserved bit-for-bit; no geometry removed',
              'before_budget_estimated_packaged_bytes':baseline_packed,
              'before_budget_largest_entries':sorted(baseline_entries,
                  key=lambda entry: entry['compressed_bytes'], reverse=True)[:40]}
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
