"""Finish a positive, expired checkpoint without retrying any failed model.

The previous finite window remains immutable. This explicitly separate final
window admits only a deadline stop after successful measured one-take workers.
All original speech, quality and emergency-memory guards remain enabled.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import time
from datetime import datetime

from finish_verified_staged_audio import receipt, pin
from finish_recovered_reference_audio import checked
from run_parent_audio_chunks import exclusive_chain, wait_ready, read_attempt
from voice_batch_scope import generation_state

ROOT = Path(__file__).resolve().parents[1]
MANIFESTS = ('tools/full_mother_voice_manifest.json',
             'tools/incidental_father_voice_manifest.json',
             'tools/incidental_mother_voice_manifest.json')


def admit(path, now=None):
    path, prior = receipt(path, 'verified-staged-audio')
    if (prior.get('status') != 'failed'
            or prior.get('error') not in (
                'Preserved completion deadline does not permit another worker',
                'Preserved completion deadline reached before worker',
                'Finite audio-chain headroom deadline exhausted')
            or prior.get('historical_speech_ram_stops') != 16
            or prior.get('worker_max_new_takes') != 1
            or prior.get('retry_failed_models') is not False
            or not prior.get('recovery_from_speech_cutoff')
            or prior.get('positive_checkpoint_from')):
        raise ValueError('Only the successful retirement-profile deadline checkpoint is admissible')
    end = datetime.fromisoformat(prior['completion_window_started_utc'].replace('Z', '+00:00')).timestamp() + prior['completion_window_seconds']
    if (time.time() if now is None else now) < end:
        raise ValueError('Previous finite completion window has not expired')
    if prior.get('completion_window_seconds') != 64800 or not prior.get('steps'):
        raise ValueError('Original positive finite window missing')
    if any(pin(Path(name)) != digest for name, digest in prior['anchors'].items()):
        raise ValueError('Original measured profile or failure history changed')
    if {name: pin(ROOT/name) for name in MANIFESTS} != prior['manifest_sha256']:
        raise ValueError('Authored dialogue changed')
    for row in prior['steps']:
        worker, n = receipt(Path(row['report']), 'compact-voice')
        if (pin(worker) != row['sha256'] or row['status'] != 'passed'
                or row['exit_code'] != 0 or n.get('status') != 'passed'
                or n.get('exit_code') != 0 or n.get('stop_free_mib') != 384
                or n.get('memory_measurement_available') is not True):
            raise ValueError('Failed or changed worker cannot be continued')
        if n.get('stage') == 'synthesis':
            checked(worker, 'synthesis')
            if n.get('max_new_takes') != 1:
                raise ValueError('Only original single-take lifetimes permitted')
        elif n.get('stage') != 'validation':
            raise ValueError('Unexpected worker stage')
    missing = sum(generation_state(ROOT, json.loads((ROOT/name).read_text(encoding='utf-8')))['expected'] - generation_state(ROOT, json.loads((ROOT/name).read_text(encoding='utf-8')))['completed'] for name in MANIFESTS)
    if not 1 <= missing <= 25:
        raise ValueError('Final positive checkpoint must have between one and 25 remaining takes')
    floor = prior.get('required_start_free_gib')
    if not isinstance(floor, (float, int)) or not 3.125 <= floor <= 4:
        raise ValueError('Measured synthesis admission floor changed')
    return path, prior, missing, floor


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--checkpoint', type=Path, required=True)
    args = parser.parse_args()
    previous, prior, missing, floor = admit(args.checkpoint)
    run_final(previous, prior, missing, floor)


def run_final(previous, prior, missing, floor, *, recovery=None, generation_profile='late', preserve_window=False, source_guards=None, component_admission=None,quality_admission=None,quality_floor=2):
    """Run only after the caller's immutable admission proof has passed."""
    if generation_profile not in ('late','early'):raise ValueError('Unknown measured generation profile')
    if generation_profile=='early' and component_admission is None:raise ValueError('Exact-PCM component admission is required for early generation')
    if not 2<=quality_floor<=4:raise ValueError('Quality admission is outside the approved range')
    window_started=prior['completion_window_started_utc'] if preserve_window else time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())
    window_seconds=prior['completion_window_seconds'] if preserve_window else 6*3600
    remaining=datetime.fromisoformat(window_started.replace('Z','+00:00')).timestamp()+window_seconds-time.time()
    if remaining<1860:raise TimeoutError('Existing finite completion window cannot admit another worker')
    folder = ROOT/'build-character-cast/verified-staged-audio'/time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    folder.mkdir(exist_ok=False)
    deadline = time.monotonic() + remaining
    state = dict(status='ready', controller_pid=os.getpid(),
                 positive_checkpoint_from=dict(report=str(previous), sha256=pin(previous), previous_window_preserved=True),
                 historical_speech_ram_stops=prior.get('historical_speech_ram_stops', 16), retry_failed_models=False,
                 worker_max_new_takes=1, release_artifact_allowed=False,
                 completion_window_seconds=window_seconds,
                 completion_window_started_utc=window_started,
                 manifest_sha256=prior['manifest_sha256'], remaining_at_admission=missing,
                 required_start_free_gib=floor,generation_profile=generation_profile,
                 component_admission=str(component_admission) if component_admission else None,
                 quality_admission=str(quality_admission) if quality_admission else None,
                 admission_anchors=source_guards or {},steps=[])
    if recovery:
        state['higher_headroom_recovery'] = recovery

    def save():
        (folder/'report.json').write_text(json.dumps(state, indent=2)+'\n', encoding='utf-8')

    def launch(name, manifest, command, minimum):
        if deadline-time.monotonic() < 1860:
            raise TimeoutError('Final finite window cannot admit another worker')
        if any(pin(ROOT/name) != digest for name, digest in prior['manifest_sha256'].items()):
            raise ValueError('Authored dialogue changed')
        if any(pin(Path(name))!=digest for name,digest in (source_guards or {}).items()):raise ValueError('Admitted source/recording proof changed')
        state.update(status='waiting_for_headroom', active_step=name, required_start_free_gib=minimum)
        save()
        wait_ready(deadline-1860, minimum_gib=minimum)
        if any(pin(Path(name))!=digest for name,digest in (source_guards or {}).items()):raise ValueError('Admitted source/recording proof changed during wait')
        state.update(status='running')
        save()
        log = folder/(name+'.log')
        with log.open('xb', buffering=0) as stream:
            code = subprocess.run(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT, timeout=1860).returncode
        n, path = read_attempt(log, manifest)
        state['steps'].append(dict(name=name, report=str(path), sha256=pin(path), status=n.get('status'), exit_code=code))
        save()
        if code or n.get('status') != 'passed' or n.get('memory_measurement_available') is not True:
            raise RuntimeError('Worker failed; no retry or later model permitted: '+name)

    save()
    print('Verified audio completion report: '+str(folder/'report.json'), flush=True)
    try:
        with exclusive_chain():
            for who, manifest in zip(('mother', 'father', 'mother'), MANIFESTS):
                scope = '--full-dialogue' if manifest == MANIFESTS[0] else '--incidental-dialogue'
                batch = 0
                while True:
                    before = generation_state(ROOT, json.loads((ROOT/manifest).read_text(encoding='utf-8')))
                    if before['generation_complete']:
                        break
                    batch += 1
                    launch(who+('_full' if scope == '--full-dialogue' else '_incidental')+'_generate_'+str(batch), manifest,
                           [sys.executable, 'tools/run_compact_voice_batch.py', '--speaker', who, scope,
                            '--memory-efficient', *([] if floor >= 4 else ['--low-memory-attempt']), '--require-reference-cache',
                            '--stage-flow-model', '--stage-vocoder', '--cuda-async-allocator',
                            '--early-reference-retirement' if generation_profile=='early' else '--retire-reference-modules',
                            *(['--early-reference-admission',str(component_admission)] if generation_profile=='early' else []),
                            '--max-new-takes', '1', '--timeout-seconds', '1800'], floor)
                    if generation_state(ROOT, json.loads((ROOT/manifest).read_text(encoding='utf-8')))['completed']-before['completed'] != 1:
                        raise ValueError('Single-take progress changed')
                launch(who+('_full' if scope == '--full-dialogue' else '_incidental')+'_quality', manifest,
                       [sys.executable, 'tools/run_compact_voice_batch.py', '--speaker', who, scope,
                        '--low-memory-attempt', '--validate', '--asr-turbo', '--resume-checks',
                        *(['--scope-quality-pages','--quality-scope-admission',str(quality_admission)] if quality_admission else []),
                        '--timeout-seconds', '1800'], quality_floor)
            state.update(status='auditing_all_masters', active_step=None)
            save()
            audit = folder/'all-master-proofs.json'
            code = subprocess.run([sys.executable, 'tools/audit_voice_master_proofs.py', '--report', str(audit)], cwd=ROOT, timeout=300).returncode
            n = json.loads(audit.read_text(encoding='utf-8'))
            if code or (n.get('status'), n.get('passed'), n.get('failed')) != ('passed', 946, 0):
                raise ValueError('All genuine speech proofs required')
            state.update(status='passed', genuine_master_proofs=946)
            save()
    except BaseException as error:
        state.update(status='failed', error=str(error))
        save()
        raise


if __name__ == '__main__':
    main()
