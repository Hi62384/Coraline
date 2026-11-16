"""Verify supplied legacy originals and merge their exact runtime durations.

No audio conversion or subtitle approval happens here. In particular this adds
the three early Wybie events omitted from the old shared timing table, without
discarding any duration contributed by another importer.
"""
from pathlib import Path
import hashlib
import json
from audio_manifest_io import write_json_if_changed
from import_numbered_original_audio import ogg_measure

ROOT=Path(__file__).resolve().parents[1]


def merged_lengths(existing,measured):
    return dict(existing,**{name:info['ticks'] for name,info in measured.items()})


def checked_recordings(root,records):
    root=Path(root);assets=root/'src/main/resources/assets/coraline'
    measured={}
    for name,pin in records.items():
        with (root/pin['source']).open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
        if digest!=pin['source_sha256']:raise RuntimeError('Supplied legacy master changed: '+name)
        payload=(assets/'sounds'/(pin['resource']+'.ogg')).read_bytes()
        if hashlib.sha256(payload).hexdigest()!=pin['ogg_sha256']:raise RuntimeError('Supplied legacy export changed: '+name)
        measured[name]=ogg_measure(payload)
    return measured


def main():
    records=json.loads((ROOT/'tools/legacy_original_recordings.json').read_text(encoding='utf-8'))
    measured=checked_recordings(ROOT,records)
    path=ROOT/'src/main/resources/assets/coraline/visit_audio_lengths.json'
    existing=json.loads(path.read_text(encoding='utf-8'))
    write_json_if_changed(path,merged_lengths(existing,measured))
    print(f'Verified {len(measured)} supplied legacy recordings; merged exact durations without changing audio.')


if __name__=='__main__':main()
