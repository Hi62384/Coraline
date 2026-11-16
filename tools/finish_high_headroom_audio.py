"""Explicit final recovery after a successful, stronger four-GiB speech probe.

This never retries a failed model automatically. The failed receipt is retained,
and an actual successful single take at the stronger start floor is mandatory.
"""
import argparse
import json
from pathlib import Path

from finish_verified_staged_audio import receipt, pin
from finish_recovered_reference_audio import checked
from finish_positive_audio_checkpoint import ROOT, MANIFESTS, run_final
from voice_batch_scope import generation_state


def admit(checkpoint, probe):
    previous, prior = receipt(checkpoint, 'verified-staged-audio')
    if (prior.get('status') != 'failed'
            or not str(prior.get('error', '')).startswith('Worker failed; no retry or later model permitted: ')
            or prior.get('historical_speech_ram_stops') != 16
            or prior.get('retry_failed_models') is not False
            or not prior.get('positive_checkpoint_from') or prior.get('higher_headroom_recovery')):
        raise ValueError('Only the stopped final checkpoint is admissible')
    anchor = prior['positive_checkpoint_from']
    original, original_state = receipt(Path(anchor['report']), 'verified-staged-audio')
    if pin(original) != anchor['sha256']:
        raise ValueError('Previous positive checkpoint changed')
    if any(pin(Path(name)) != digest for name, digest in original_state['anchors'].items()):
        raise ValueError('Original profile or failure history changed')
    rows = prior.get('steps', [])
    if not rows or len(rows) > 30:
        raise ValueError('Unexpected final checkpoint history')
    for index, row in enumerate(rows):
        path, n = receipt(Path(row['report']), 'compact-voice')
        expected = 'low_memory_cutoff' if index == len(rows)-1 else 'passed'
        if (pin(path) != row['sha256'] or n.get('status') != expected
                or row['status'] != expected or n.get('stop_free_mib') != 384
                or n.get('memory_measurement_available') is not True):
            raise ValueError('Worker history changed or contains another failure')
        if index == len(rows)-1 and (n.get('own_child_stopped') is not True
                or n.get('stage') != 'synthesis'
                or n.get('manifest') != MANIFESTS[2]
                or row['exit_code'] == 0):
            raise ValueError('Expected a stopped incidental-Mother speech worker')
    successful, p = checked(probe, 'synthesis')
    if (p.get('manifest') != MANIFESTS[2] or p.get('completed') != 34
            or p.get('max_new_takes') != 1 or p.get('minimum_start_free_gib', 0) < 4
            or p.get('start_free_gib', 0) < 4 or p.get('min_free_gib', 0) < 1):
        raise ValueError('Actual stronger-headroom successful take with measured margin required')
    if {name: pin(ROOT/name) for name in MANIFESTS} != prior['manifest_sha256']:
        raise ValueError('Authored dialogue changed')
    counts = [generation_state(ROOT, json.loads((ROOT/name).read_text(encoding='utf-8'))) for name in MANIFESTS]
    if not all(n['generation_complete'] for n in counts[:2]) or counts[2]['completed'] != 34:
        raise ValueError('Preserved genuine recording coverage changed')
    return previous, prior, successful


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--checkpoint', type=Path, required=True)
    parser.add_argument('--successful-probe', type=Path, required=True)
    args = parser.parse_args()
    previous, prior, successful = admit(args.checkpoint, args.successful_probe)
    prior = dict(prior, historical_speech_ram_stops=17)
    run_final(previous, prior, 10, 4,
              recovery=dict(successful_probe=str(successful), sha256=pin(successful),
                            changed_admission='Original four-GiB wrapper preflight; original speech profile and 384-MiB emergency stop unchanged',
                            failed_checkpoint=str(previous), failed_checkpoint_sha256=pin(previous)))


if __name__ == '__main__':
    main()
