"""Prepare a small, resumable recording list; never erase/rewrite existing masters.

Only unrecorded alternates are consolidated, within one speaker/event/scene/tone.
Every authored sound ID still resolves. Warm and suspicious delivery stay separate;
specific game rules, destinations and Father conversation topics are not merged.
"""
import argparse
import json
import re
from pathlib import Path
from visit_event_audio_catalog import authored_recordings, effective_recordings

ROOT = Path(__file__).resolve().parents[1]
PLAN = ROOT/'tools/compact_event_voice_plan.json'
MOODS = {'low_low': 'warm', 'mid_mid': 'warm', 'high_low': 'warm',
         'low_high': 'stern', 'high_high': 'stern'}


def group(record):
    state = record['state']
    parts = state.split('/')
    if parts[-1] in MOODS:
        stage = '/'.join(parts[:-1])
        tone = MOODS[parts[-1]]
    else:
        stage = re.sub(r'_[12]$', '', state) if record['speaker'] == 'Other Father' else state
        tone = 'specific'
    return record['manifest'], stage, tone


def prepare(root=ROOT):
    plan_path = root/'tools/compact_event_voice_plan.json'
    if plan_path.exists():
        raise RuntimeError('Existing plan is resumable; do not silently change its recorded scripts.')
    records = authored_recordings(root)
    groups = {}
    for record in records.values():
        groups.setdefault(group(record), []).append(record)
    aliases, new_recordings = {}, {}
    manifests = {}
    before = sum(not (root/r['source']).is_file() for r in records.values())
    for entries in groups.values():
        # Prefer an already voiced take in this precise scene/tone. Never alias
        # away an existing genuine recording or regenerate its original WAV.
        canonical = next((r for r in entries if (root/r['source']).is_file()), entries[0])
        target = canonical['event']
        for record in entries:
            if record['event'] != target and not (root/record['source']).is_file():
                aliases[record['event']] = target
        if (root/canonical['source']).is_file():
            continue
        speaker = 'father' if canonical['speaker'] == 'Other Father' else 'mother'
        output = f'voicelines/other{speaker}/events/compact_v1'
        new_recordings[target] = dict(source=f'{output}/{target}/line_01.wav', resource=f'compact_events/{target}')
        manifest = manifests.setdefault(speaker, dict(reference=canonical['reference'],
            reference_sha256=canonical['reference_sha256'], output=output, prefix='line', states={}))
        profile = dict(canonical['profile'])
        # One complete, unchanged script per new take. No stitched/truncated words.
        profile.update(lines=[canonical['text']], filename='line_01.wav', source_directory=target)
        profile.pop('segments', None)
        manifest['states'][target] = profile
    plan = dict(format=1, missing_events_before=before, new_unique_recordings=len(new_recordings),
                aliases=aliases, new_recordings=new_recordings,
                note='Only missing alternates consolidated. Existing voices, original scripts, event IDs and gameplay retained.')
    for speaker, manifest in manifests.items():
        write_json(root/f'tools/compact_{speaker}_voice_manifest.json', manifest)
    write_json(plan_path, plan)
    effective = effective_recordings(root)
    assert len(effective) == len(records)
    return plan


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')


def status(root=ROOT):
    records = effective_recordings(root)
    missing = {r['source'] for r in records.values() if not (root/r['source']).is_file()}
    print(json.dumps(dict(event_ids=len(records), unique_recordings=len({r['source'] for r in records.values()}),
                          missing_unique_recordings=len(missing),
                          missing_event_mappings=sum(not (root/r['source']).is_file() for r in records.values())), indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--prepare', action='store_true')
    args = parser.parse_args()
    if args.prepare:
        print(json.dumps({k:v for k,v in prepare().items() if k not in ('aliases', 'new_recordings')}, indent=2))
    status()
