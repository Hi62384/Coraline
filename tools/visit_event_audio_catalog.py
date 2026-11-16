"""Shared visit-event manifest list and strict resource naming. No generation is performed here."""
from pathlib import PurePosixPath
import hashlib
import json
import re

EVENT_MANIFESTS = [
    "tools/exploration_voice_manifest.json",
    "tools/dinner_mother_voice_manifest.json",
    "tools/dinner_father_voice_manifest.json",
    "tools/father_study_voice_manifest.json",
    "tools/outing_mother_voice_manifest.json",
    "tools/outing_father_voice_manifest.json",
    "tools/game_night_mother_voice_manifest.json",
    "tools/game_night_father_voice_manifest.json",
    "tools/delivery_mother_voice_manifest.json",
    "tools/house_party_mother_voice_manifest.json",
    "tools/house_party_father_voice_manifest.json",
    "tools/player_party_mother_voice_manifest.json",
    "tools/player_party_father_voice_manifest.json",
    "tools/mother_goes_out_mother_voice_manifest.json",
    "tools/mother_goes_out_father_voice_manifest.json",
    "tools/wybie_adventure_mother_voice_manifest.json",
    "tools/theatre_mother_voice_manifest.json",
    "tools/mouse_circus_mother_voice_manifest.json"
]

def event_directory(manifest):
    output=PurePosixPath(manifest["output"].replace("\\","/"))
    if len(output.parts)!=4 or output.parts[:1]!=("voicelines",) or output.parts[1] not in ("othermother","otherfather") or output.parts[2]!="events":
        raise ValueError("Unexpected event voice output directory: "+str(output))
    event=output.name
    if not re.fullmatch(r"[a-z][a-z0-9_]*",event):
        raise ValueError("Unsafe event resource directory")
    if manifest.get("event_directory",event)!=event:
        raise ValueError("Manifest event_directory disagrees with its output")
    return event

def check_reference(root,manifest):
    reference=root/manifest["reference"]
    expected=manifest.get("reference_sha256","")
    if not re.fullmatch(r"[0-9a-f]{64}",expected) or hashlib.sha256(reference.read_bytes()).hexdigest()!=expected:
        raise ValueError("Established speaker reference changed: "+str(reference))


def authored_recordings(root):
    """Stable event IDs and untouched scripts, independently of recording reuse."""
    records = {}
    for filename in EVENT_MANIFESTS:
        manifest = json.loads((root/filename).read_text(encoding='utf-8'))
        category = event_directory(manifest)
        father = 'otherfather' in manifest['reference']
        for state, profile in manifest['states'].items():
            for index, text in enumerate(profile['lines'], 1):
                name = state if father else 'other_mother_events_'+category+'_'+state.replace('/', '_')+f'_{index:02d}'
                resource = f'{category}/{state}' if father else f'{category}/{state}_{index:02d}'
                source = '/'.join((manifest['output'], profile.get('source_directory', state),
                                   profile.get('filename', f'line_{index:02d}.wav'))).replace('//', '/')
                if name in records:
                    raise ValueError('Duplicate event: '+name)
                records[name] = dict(event=name, manifest=filename, state=state, index=index,
                                     text=text, source=source, resource=resource, profile=profile,
                                     reference=manifest['reference'], reference_sha256=manifest['reference_sha256'],
                                     speaker='Other Father' if father else 'Other Mother')
    return records


def effective_recordings(root):
    """Resolve direct, same-speaker recording aliases without changing event IDs.

    Captions must use the resolved script, not claim that a reused recording says
    an alternate script. Original manifests/masters remain archival and intact.
    """
    records = authored_recordings(root)
    plan_path = root/'tools/full_event_voice_plan.json'
    if not plan_path.exists():plan_path=root/'tools/compact_event_voice_plan.json'
    if not plan_path.exists():
        return records
    plan = json.loads(plan_path.read_text(encoding='utf-8'))
    aliases = plan['aliases']
    for name, target in aliases.items():
        if name not in records or target not in records or target in aliases:
            raise ValueError('Invalid/chained compact voice alias: '+name)
        if records[name]['reference_sha256'] != records[target]['reference_sha256']:
            raise ValueError('Cross-speaker compact voice alias: '+name)
        if plan.get('preserve_authored_text') and records[name]['text']!=records[target]['text']:
            raise ValueError('Full dialogue alias changed authored words: '+name)
    for name, override in plan['new_recordings'].items():
        if name not in records or name in aliases:
            raise ValueError('Invalid compact voice master: '+name)
        for key, prefix in [('source', 'voicelines/'), ('resource', 'compact_events/')]:
            value = override[key]
            path = PurePosixPath(value)
            if not value.startswith(prefix) or path.is_absolute() or '..' in path.parts or ':' in value or '\\' in value:
                raise ValueError('Unsafe compact voice '+key)
        records[name] = dict(records[name], **override)
    for name, target in aliases.items():
        records[name] = dict(records[target], event=name, shared_recording=target,
                             original_text=records[name]['text'], original_manifest=records[name]['manifest'])
    if plan.get('preserve_authored_text'):
        original=authored_recordings(root)
        if any(record['text']!=original[name]['text'] for name,record in records.items()):raise ValueError('Full dialogue plan changed an authored script')
    return records
