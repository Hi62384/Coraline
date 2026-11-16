"""Import thirteen pinned ORIGINAL recordings; never synthesize, replace or edit the masters.

All conversions are measured before runtime assets are changed. The optional decode
pass is omitted by --convert-only, and provenance records that it was not run. This
task must run after every other sound-table importer. Transcription remains a separate
operation through build_story_subtitles.py --transcribe.
"""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import math
import os
import re
import shutil
import struct
import subprocess
import tempfile
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
ASSET_REL = Path('src/main/resources/assets/coraline')
MANIFEST_REL = Path('tools/numbered_original_recordings.json')
EXPECTED_IDS = {'wybie_adventure_intro', 'theatre_music', 'theatre_a', 'theatre_b', 'theatre_c',
                'theatre_d', 'theatre_e', 'theatre_f', 'theatre_meet_parents', 'mouse_circus_invite',
                'mouse_circus_intro', 'mouse_circus_music', 'mouse_circus_end'}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def contained(root, relative):
    rel = PurePosixPath(str(relative).replace('\\', '/'))
    if rel.is_absolute() or '..' in rel.parts or ':' in str(rel):
        raise ValueError('Unsafe relative path: ' + str(relative))
    target = (root / str(rel)).resolve()
    if not target.is_relative_to(root.resolve()):
        raise ValueError('Path leaves project root: ' + str(relative))
    return target


def _ogg_crc_table():
    table = []
    for value in range(256):
        crc = value << 24
        for _ in range(8):
            crc = ((crc << 1) ^ (0x04c11db7 if crc & 0x80000000 else 0)) & 0xffffffff
        table.append(crc)
    return tuple(table)


_OGG_CRC_TABLE = _ogg_crc_table()


def ogg_page_crc(page):
    """Ogg's non-reflected CRC-32; the stored checksum field is treated as zero."""
    crc = 0
    for index, byte in enumerate(page):
        if 22 <= index < 26:
            byte = 0
        crc = ((crc << 8) & 0xffffffff) ^ _OGG_CRC_TABLE[((crc >> 24) ^ byte) & 255]
    return crc


def ogg_measure(data):
    """Read genuine single-stream Vorbis pages, including continued identification packets."""
    offset = 0
    serial = None
    last_sequence = -1
    rate = channels = 0
    samples = -1
    packet = bytearray()
    ended = False
    while offset < len(data):
        if offset + 27 > len(data) or data[offset:offset+4] != b'OggS' or data[offset+4] != 0:
            raise ValueError('Malformed OGG page')
        flags = data[offset+5]
        if flags & ~7 or bool(flags & 1) != bool(packet):
            raise ValueError('Invalid OGG flags or packet continuation')
        if serial is not None and flags & 2:
            raise ValueError('Repeated OGG beginning-of-stream')
        granule, stream, sequence = struct.unpack_from('<QII', data, offset+6)
        if serial is None:
            serial = stream
            if not flags & 2:
                raise ValueError('OGG lacks beginning-of-stream')
        if stream != serial or sequence != last_sequence + 1 or ended:
            raise ValueError('Chained/multiple/disordered OGG streams are not accepted')
        last_sequence = sequence
        count = data[offset+26]
        start = offset + 27 + count
        if start > len(data):
            raise ValueError('Truncated segment table')
        sizes = data[offset+27:start]
        end = start + sum(sizes)
        if end > len(data):
            raise ValueError('Truncated OGG body')
        if ogg_page_crc(data[offset:end]) != struct.unpack_from('<I', data, offset+22)[0]:
            raise ValueError('OGG page checksum mismatch')
        cursor = start
        for size in sizes:
            packet.extend(data[cursor:cursor+size])
            cursor += size
            if size < 255:
                if not rate:
                    if len(packet) < 30 or packet[:7] != b'\x01vorbis':
                        raise ValueError('Required Vorbis identification packet is missing')
                    channels = packet[11]
                    rate = struct.unpack_from('<I', packet, 12)[0]
                    if channels not in (1, 2) or not 8000 <= rate <= 192000:
                        raise ValueError('Unsupported Vorbis channel/rate metadata')
                packet.clear()
        if granule != 0xffffffffffffffff:
            if granule < samples:
                raise ValueError('OGG granule position moved backwards')
            samples = granule
        ended = bool(flags & 4)
        offset = end
    if not ended or not rate or samples <= 0 or packet:
        raise ValueError('Incomplete/empty Vorbis recording')
    return {'sample_rate': rate, 'channels': channels, 'samples': samples,
            'seconds': samples/rate, 'ticks': math.ceil(samples*20/rate)}


def load_manifest(root):
    document = json.loads(contained(root, MANIFEST_REL).read_text(encoding='utf-8'))
    recordings = document.get('recordings', {})
    if document.get('format') != 1 or set(recordings) != EXPECTED_IDS:
        raise ValueError('Original-recording manifest does not match the thirteen required identities')
    resources = set()
    for key, entry in recordings.items():
        if not re.fullmatch(r'[0-9a-f]{64}', entry.get('source_sha256', '')):
            raise ValueError('Unpinned original: ' + key)
        resource = entry.get('resource', '')
        if not re.fullmatch(r'numbered_originals/[a-z0-9_]+', resource) or resource in resources:
            raise ValueError('Invalid or duplicated resource: ' + key)
        resources.add(resource)
        if not isinstance(entry.get('music'), bool):
            raise ValueError('Missing recording type: ' + key)
        if entry['music'] == bool(entry.get('speaker', '')):
            raise ValueError('Spoken recordings require a speaker; music has none: ' + key)
    return recordings


def install_runtime_outputs(root, build, staged, before):
    """All conversion finishes first; preserve old bytes and atomically install each file."""
    backup = build / ('backup-' + time.strftime('%Y%m%dT%H%M%SZ', time.gmtime()) + '-' + uuid.uuid4().hex[:8])
    backup.mkdir(exist_ok=False)
    rows = []
    audit = {'status': 'preparing', 'decode_validation_claim': False, 'files': rows,
             'limits': 'Atomic replacement per file, not a cross-file transaction. No automatic rollback or power-loss durability claim.'}
    report = backup / 'recovery-manifest.json'
    def save():
        temp = backup / 'manifest.tmp'
        with temp.open('wb') as stream:
            stream.write((json.dumps(audit, indent=2) + '\n').encode('utf-8'))
            stream.flush(); os.fsync(stream.fileno())
        os.replace(temp, report)
    def current(path):
        return path.read_bytes() if path.exists() else None
    for index, (path, prepared) in enumerate(staged.items()):
        original = before[path]
        if current(path) != original:
            raise RuntimeError('Runtime target changed before backup: ' + str(path))
        stored = backup / ('original-' + str(index) + '.bin') if original is not None else None
        if stored is not None:
            with stored.open('xb') as stream:
                stream.write(original); stream.flush(); os.fsync(stream.fileno())
            if stored.read_bytes() != original:
                raise RuntimeError('Runtime backup readback failed: ' + str(path))
        rows.append({'path': str(path.relative_to(root)), 'original_sha256': sha(original) if original is not None else None,
                     'backup': str(stored) if stored is not None else None, 'candidate_sha256': sha(prepared.read_bytes()),
                     'write_started': False, 'applied': False})
    audit['status'] = 'prepared'; save()
    try:
        for path in staged:
            if current(path) != before[path]:
                raise RuntimeError('Runtime target changed before installation: ' + str(path))
        for (path, prepared), row in zip(staged.items(), rows):
            if current(path) != before[path]:
                raise RuntimeError('Runtime target changed immediately before installation: ' + str(path))
            if row['backup'] is not None and sha(Path(row['backup']).read_bytes()) != row['original_sha256']:
                raise RuntimeError('Runtime backup changed before installation')
            path.parent.mkdir(parents=True, exist_ok=True)
            row['write_started'] = True; save()
            if before[path] is None:
                if os.name != 'nt':
                    raise RuntimeError('Exclusive atomic creation is supported only on the Windows production target')
                os.rename(prepared, path)  # Windows refuses an existing destination.
            else:
                os.replace(prepared, path)
            if sha(path.read_bytes()) != row['candidate_sha256']:
                raise RuntimeError('Runtime output readback differs: ' + str(path))
            row['applied'] = True; save()
        for row in rows:
            if sha((root / row['path']).read_bytes()) != row['candidate_sha256']:
                raise RuntimeError('Runtime output changed before final readback')
            if row['backup'] is not None and sha(Path(row['backup']).read_bytes()) != row['original_sha256']:
                raise RuntimeError('Runtime backup changed before completion')
        audit['status'] = 'converted_and_imported'; save()
    except BaseException as error:
        audit['status'] = 'stopped_without_rollback'; audit['error'] = str(error); save()
        raise
    print('Original-audio recovery manifest: ' + str(report), flush=True)
    return report


def import_recordings(root=ROOT, *, decode_output=True, ffmpeg_path=None):
    root = root.resolve()
    recordings = load_manifest(root)
    ffmpeg = str(Path(ffmpeg_path)) if ffmpeg_path is not None else shutil.which('ffmpeg')
    if ffmpeg_path is not None and (not Path(ffmpeg).is_absolute() or not Path(ffmpeg).is_file()):
        raise RuntimeError('The explicitly supplied existing ffmpeg executable must be an absolute file path')
    if not ffmpeg:
        raise RuntimeError('Use the project\'s existing ffmpeg installation; no software is downloaded')
    paths = [contained(root, ASSET_REL/'sounds.json'), contained(root, ASSET_REL/'visit_audio_lengths.json')]
    before = {path: path.read_bytes() for path in paths}
    sounds, lengths = [json.loads(before[path]) for path in paths]
    destinations = {key: contained(root, ASSET_REL/'sounds'/(entry['resource']+'.ogg')) for key, entry in recordings.items()}
    provenance = contained(root, ASSET_REL/'numbered_original_audio.json')
    for path in [*destinations.values(), provenance]:
        before[path] = path.read_bytes() if path.exists() else None
    source_fingerprints = {}
    for key, entry in recordings.items():
        source = contained(root, entry['source'])
        data = source.read_bytes()
        if len(data) != entry['source_bytes'] or sha(data) != entry['source_sha256']:
            raise RuntimeError('Original source identity changed; review rather than substitute: ' + str(source))
        source_fingerprints[source] = entry['source_sha256']
        registered = sounds.get(key)
        expected = {'sounds': [{'name': 'coraline:'+entry['resource'], 'stream': True}]}
        if registered is not None and registered != expected:
            raise RuntimeError('Existing sound identity conflicts; reconcile before import: ' + key)
    output = {}
    build = root/'build-character-cast'/'numbered-original-audio'
    build.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='convert-', dir=build) as temporary:
        temporary = Path(temporary)
        for key, entry in recordings.items():
            source = contained(root, entry['source'])
            target = temporary/(key+'.ogg')
            command = [ffmpeg, '-nostdin', '-y', '-v', 'error', '-threads', '1', '-i', str(source),
                       '-map', '0:a:0', '-vn', '-map_metadata', '-1', '-c:a', 'libvorbis', '-q:a', '4',
                       '-fflags', '+bitexact', '-flags:a', '+bitexact', '-threads', '1']
            if not entry['music']:
                command += ['-ac', '1']
            subprocess.run(command+[str(target)], check=True, timeout=180)
            # Optional standalone decode check. Conversion-only requests omit this pass.
            if decode_output:
                subprocess.run([ffmpeg, '-nostdin', '-v', 'error', '-threads', '1', '-i', str(target),
                                '-f', 'null', '-'], check=True, timeout=180)
            payload = target.read_bytes()
            measured = ogg_measure(payload)
            if measured['seconds'] > (3600 if entry['music'] else 600):
                raise RuntimeError('Implausible imported duration: ' + key)
            if not entry['music'] and measured['channels'] != 1:
                raise RuntimeError('Spoken original is not mono: ' + key)
            if 'source_seconds' in entry and abs(measured['seconds']-entry['source_seconds']) > .05:
                raise RuntimeError('Conversion truncated or extended original: ' + key)
            output[key] = dict(measured, source_sha256=entry['source_sha256'], ogg_sha256=sha(payload),
                               resource=entry['resource'], original_source=entry['source'],
                               decode_validated=bool(decode_output))
            sounds[key] = {'sounds': [{'name': 'coraline:'+entry['resource'], 'stream': True}]}
            lengths[key] = measured['ticks']
        # No mixed-source export or lost updates to tables changed by another importer.
        for source, fingerprint in source_fingerprints.items():
            if sha(source.read_bytes()) != fingerprint:
                raise RuntimeError('Original changed during conversion: ' + str(source))
        for path, original in before.items():
            if (path.read_bytes() if path.exists() else None) != original:
                raise RuntimeError('Runtime target changed during conversion; rerun the serialized import')
        staged_outputs = {destinations[key]: temporary/(key+'.ogg') for key in recordings}
        tables = [(paths[0], sounds), (paths[1], lengths), (provenance, {'format': 1, 'recordings': output})]
        for path, value in tables:
            staged = temporary/path.name
            with staged.open('wb') as stream:
                stream.write((json.dumps(value, indent=2, ensure_ascii=False)+'\n').encode('utf-8'))
                stream.flush(); os.fsync(stream.fileno())
            staged_outputs[path] = staged
        install_runtime_outputs(root, build, staged_outputs, before)
    print('Imported 13 pinned original recordings; source masters unchanged; separate decode validation: ' + ('completed' if decode_output else 'not run (conversion-only)') + '.')
    return output


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--convert-only', action='store_true', help='Convert/import without the separate decode-validation pass; provenance records this explicitly.')
    parser.add_argument('--ffmpeg', type=Path, help='Absolute path to the existing installed ffmpeg executable; no installation or download.')
    args = parser.parse_args()
    import_recordings(decode_output=not args.convert_only, ffmpeg_path=args.ffmpeg)
