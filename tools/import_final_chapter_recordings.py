"""Bounded serial import of supplied A-N/Entrance and hand A-I originals.

Never synthesize, transcribe, normalize, trim or edit captions. Existing exports
are reused only through exact source/output receipts. Shared tables are merged
after conversion, fingerprint-checked, backed up and published by the established
per-file atomic installer. Not a cross-file/power-loss transaction guarantee.
"""
from pathlib import Path
from ctypes import wintypes
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import threading
import time

import check_numbered_candidate as monitor
from inspect_final_chapter_recordings import inspect_recordings
from import_numbered_original_audio import contained, install_runtime_outputs, ogg_measure
from run_low_memory_release import job

ROOT = Path(__file__).resolve().parents[1]
ASSET_REL = Path('src/main/resources/assets/coraline')
RECEIPT = 'final_chapter_original_audio.json'
REVISION = 1


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def resource(sound):
    if sound not in inspect_names():
        raise ValueError('Unexpected supplied-recording event: ' + sound)
    return 'final_chapter_originals/' + sound


def inspect_names():
    return ({'theatre_opening_' + c for c in 'abcdefghijklmn'}
            | {'theatre_entrance'} | {'crawling_hand_' + c for c in 'abcdefghi'})


def expected_event(sound):
    return {'sounds': [{'name': 'coraline:' + resource(sound), 'stream': True}]}


def merge_tables(sounds, lengths, outputs):
    """Pure merge: refuse foreign playback settings; retain all unrelated data."""
    merged_sounds, merged_lengths = dict(sounds), dict(lengths)
    if set(outputs) != inspect_names():
        raise ValueError('Incomplete supplied originals')
    for sound, measured in outputs.items():
        event = expected_event(sound)
        if sound in sounds and sounds[sound] != event:
            raise ValueError('Existing event conflicts with supplied original: ' + sound)
        ticks = measured['ticks']
        if not isinstance(ticks, int) or isinstance(ticks, bool) or ticks <= 0:
            raise ValueError('Invalid original duration: ' + sound)
        merged_sounds[sound] = event
        merged_lengths[sound] = ticks
    return merged_sounds, merged_lengths


def guarded(command, folder, label, steps):
    if monitor.free_gib() < monitor.START_FREE_GIB:
        raise RuntimeError('Below unchanged 640 MiB start floor; no converter started')
    api, handle = job(192)
    try:
        with (folder / (label + '.log')).open('xb', buffering=0) as log:
            child = subprocess.Popen(command, cwd=ROOT, env=monitor.child_environment(),
                                     stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle, wintypes.HANDLE(int(child._handle))):
                child.kill(); child.wait()
                raise RuntimeError('Cannot bind owned audio conversion job')
            reached, errors = threading.Event(), []
            reader = threading.Thread(target=monitor.drain_bounded,
                                      args=(child.stdout, log, monitor.MAX_LOG_BYTES, reached, errors), daemon=True)
            reader.start()
            result = monitor.watch_child(child, 120, monitor.free_gib, monitor.process_memory, log_limit=reached)
            reader.join(timeout=10)
            if reader.is_alive() or errors or reached.is_set():
                result['status'] = 'capture_failed'
            steps[label] = result
            if result.get('status') != 'passed' or result.get('exit_code') != 0:
                raise RuntimeError('Owned audio child did not pass: ' + label + ': ' + str(result))
    finally:
        api.CloseHandle(handle)


def main():
    if ROOT != monitor.ROOT or os.name != 'nt':
        raise RuntimeError('Unexpected workspace/platform')
    originals = inspect_recordings()
    if set(originals) != inspect_names():
        raise RuntimeError('Unexpected supplied originals')
    ffmpeg = shutil.which('ffmpeg')
    if not ffmpeg or not Path(ffmpeg).is_absolute() or not Path(ffmpeg).is_file():
        raise RuntimeError('Use the existing ffmpeg executable; no download/install')
    assets = contained(ROOT, ASSET_REL)
    paths = [assets / 'sounds.json', assets / 'visit_audio_lengths.json', assets / RECEIPT]
    before = {p: p.read_bytes() if p.exists() else None for p in paths}
    sounds, lengths = [json.loads(before[p]) for p in paths[:2]]
    prior = json.loads(before[paths[2]]) if before[paths[2]] is not None else {}
    if prior and (prior.get('format') != 1 or set(prior.get('recordings', {})) != inspect_names()):
        raise RuntimeError('Unknown/incomplete previous import receipt; review without overwriting')
    for sound in originals:
        if sound in sounds and sounds[sound] != expected_event(sound):
            raise RuntimeError('Existing sound event conflicts; review before conversion: ' + sound)
    # Preserve old OGGs and caption bytes, not merely a count of their entries.
    old_audio = {p: digest(p) for p in (assets / 'sounds').rglob('*.ogg')}
    captions = {p: digest(p) for p in (assets / 'story_subtitles.json', assets / 'numbered_event_dialogue.json') if p.exists()}
    build = ROOT / 'build-character-cast' / 'final-chapter-recordings'
    build.mkdir(parents=True, exist_ok=True)
    folder = build / (time.strftime('%Y%m%dT%H%M%SZ', time.gmtime()) + '-' + os.urandom(4).hex())
    folder.mkdir(exist_ok=False)
    report = {'status': 'preparing', 'scope': 'Supplied-original import and decode only; no speaker/content review, synthesis, transcription, captions, native playback or release acceptance',
              'steps': {}, 'originals': originals, 'prior_ogg_count': len(old_audio)}
    try:
        with tempfile.TemporaryDirectory(prefix='staging-', dir=folder) as temporary:
            staging = Path(temporary)
            outputs, staged_outputs = {}, {}
            for sound, source in originals.items():
                target = contained(ROOT, ASSET_REL / 'sounds' / (resource(sound) + '.ogg'))
                before[target] = target.read_bytes() if target.exists() else None
                old = prior.get('recordings', {}).get(sound, {})
                reused = (old.get('source_sha256') == source['sha256'] and old.get('export_revision') == REVISION
                          and target.exists() and old.get('ogg_sha256') == digest(target))
                if target.exists() and not reused:
                    raise RuntimeError('Existing export has no matching receipt; refusing overwrite: ' + sound)
                encoded = target if reused else staging / (sound + '.ogg')
                if not reused:
                    command = [ffmpeg, '-nostdin', '-v', 'error', '-y', '-threads', '1', '-filter_threads', '1',
                               '-i', str(ROOT / source['source']), '-map', '0:a:0', '-vn', '-map_metadata', '-1',
                               '-c:a', 'libvorbis', '-q:a', '5', '-threads', '1',
                               '-fflags', '+bitexact', '-flags:a', '+bitexact']
                    if source['sample_rate'] > 48000:
                        command += ['-ar', '48000']
                    guarded(command + [str(encoded)], folder, sound + '-encode', report['steps'])
                    staged_outputs[target] = encoded
                measured = ogg_measure(encoded.read_bytes())
                if (measured['channels'] != source['channels'] or measured['sample_rate'] != min(48000, source['sample_rate'])
                        or abs(measured['seconds'] - source['seconds']) > 1 / measured['sample_rate']):
                    raise RuntimeError('Original duration/channels changed: ' + sound)
                if not reused:
                    guarded([ffmpeg, '-nostdin', '-v', 'error', '-threads', '1', '-i', str(encoded),
                             '-map', '0:a:0', '-f', 'null', '-'], folder, sound + '-decode', report['steps'])
                if reused and not old.get('decode_validated'):
                    raise RuntimeError('Existing export was never decode validated: ' + sound)
                outputs[sound] = dict(measured, source=source['source'], source_sha256=source['sha256'],
                                      ogg_sha256=digest(encoded), resource=resource(sound), export_revision=REVISION,
                                      decode_validated=True)
                print(('Reused ' if reused else 'Converted original ') + sound, flush=True)
            merged_sounds, merged_lengths = merge_tables(sounds, lengths, outputs)
            for p, identity in {**old_audio, **captions}.items():
                if digest(p) != identity:
                    raise RuntimeError('Existing audio/caption changed during serial import: ' + str(p))
            for sound, source in originals.items():
                if digest(ROOT / source['source']) != source['sha256']:
                    raise RuntimeError('Supplied master changed: ' + sound)
            tables = {paths[0]: merged_sounds, paths[1]: merged_lengths,
                      paths[2]: {'format': 1, 'recordings': outputs}}
            for p, value in tables.items():
                payload = (json.dumps(value, indent=2, ensure_ascii=False) + '\n').encode('utf8')
                if payload == before[p]:
                    continue
                prepared = staging / p.name
                prepared.write_bytes(payload)
                staged_outputs[p] = prepared
            for p, content in before.items():
                if (p.read_bytes() if p.exists() else None) != content:
                    raise RuntimeError('Concurrent runtime change refused: ' + str(p))
            if staged_outputs:
                install_runtime_outputs(ROOT, folder, staged_outputs, before)
            for p, identity in {**old_audio, **captions}.items():
                if digest(p) != identity:
                    raise RuntimeError('Pre-existing audio/caption identity changed after import')
            report.update(status='passed', recordings=outputs, exported_ogg_bytes=sum((assets / 'sounds' / (resource(s) + '.ogg')).stat().st_size for s in outputs),
                          all_prior_oggs_unchanged=True, existing_captions_unchanged=True,
                          prior_sound_definitions_preserved=all(merged_sounds[k] == v for k, v in sounds.items()),
                          prior_duration_entries_preserved=all(merged_lengths[k] == v for k, v in lengths.items() if k not in outputs))
    except BaseException as error:
        report.update(status='failed', error=str(error))
        raise
    finally:
        (folder / 'report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
        print('Import report: ' + str(folder / 'report.json'), flush=True)
    print('24 supplied originals ready; source masters, all previous OGGs and captions preserved.')


if __name__ == '__main__':
    main()
