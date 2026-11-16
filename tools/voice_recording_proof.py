"""Fail closed on unverified or script-mismatched event voice masters.

Older retained validation receipts predate per-check hashes. Their generation
receipts still pin the source, reference and exact script; the release audit
reports this distinction instead of claiming those checks had newer metadata.
"""
import hashlib
import json
from pathlib import Path


def validate(root, record, cache=None):
    root=Path(root);cache={} if cache is None else cache
    def read(path):
        if path not in cache:cache[path]=json.loads(path.read_text(encoding='utf-8'))
        return cache[path]
    reference_key=('reference',root/record['reference'])
    if reference_key not in cache:cache[reference_key]=hashlib.sha256(reference_key[1].read_bytes()).hexdigest()
    if cache[reference_key]!=record['reference_sha256']:
        raise ValueError('Established voice reference changed: '+record['event'])
    source=root/record['source'];canonical=record.get('shared_recording',record['event'])
    if record['resource'].startswith('compact_events/'):
        directory=source.parent.parent;key=canonical+'/line_01.wav';manifest={}
    else:
        manifest=read(root/record['manifest']);directory=root/manifest.get('output','voicelines/othermother/welcome')
        key=(record['state']+'/'+record['profile'].get('filename',f"line_{record['index']:02d}.wav")).lstrip('/')
    generation_path=directory/manifest.get('generation_report','generation_report.json')
    validation_path=directory/manifest.get('validation_report','validation_report.json')
    generated=read(generation_path)
    checked=read(validation_path)
    digest=hashlib.sha256(source.read_bytes()).hexdigest()
    receipt=generated.get('lines',{}).get(key,{})
    if (generated.get('reference_sha256')!=record['reference_sha256'] or
            receipt.get('sha256')!=digest or receipt.get('text')!=record['text']):
        raise ValueError('Voice generation/reference/script proof mismatch: '+record['event'])
    rows=[row for row in checked.get('checks',[]) if row.get('file')==key]
    if checked.get('reference_unchanged') is not True or len(rows)!=1:
        raise ValueError('Missing or ambiguous voice quality check: '+record['event'])
    row=rows[0]
    if (row.get('pcm_decode')!='pass' or row.get('needs_review') is not False or
            row.get('speaker_review') is not False or row.get('word_error_rate',1)>.20 or
            row.get('reference_speaker_cosine',0)<.35):
        raise ValueError('Voice quality review remains unresolved: '+record['event'])
    if 'source_sha256' in row and row['source_sha256']!=digest:
        raise ValueError('Voice changed since quality check: '+record['event'])
    if 'expected_text_sha256' in row and row['expected_text_sha256']!=hashlib.sha256(record['text'].encode('utf-8')).hexdigest():
        raise ValueError('Voice script changed since quality check: '+record['event'])
    return {'event':record['event'],'source_sha256':digest,'reference_sha256':record['reference_sha256'],
            'generation_report':str(generation_path.relative_to(root)),
            'validation_report':str(validation_path.relative_to(root)),
            'validation_source_hash_present':'source_sha256' in row}


def legacy_recordings(root, manifests=None):
    """Catalogue the other285already-voiced welcome/activity/etc. lines too.

    Same naming and source conventions as import_welcome_audio.py and the
    subtitle builder, including manifests with custom shared receipt names.
    This is read-only; it never synthesizes or replaces an existing master.
    """
    root=Path(root);records={}
    paths=[root/name for name in manifests] if manifests is not None else sorted((root/'tools').glob('other_mother_*.json'))
    father=root/'tools/other_father_activity_intros.json'
    if manifests is None and father.exists():paths.append(father)
    for path in paths:
        manifest=json.loads(path.read_text(encoding='utf-8'))
        prefix=manifest.get('prefix','welcome')
        output=manifest.get('output','voicelines/othermother/welcome')
        folder=manifest.get('resource_folder','welcome')
        for state,profile in manifest['states'].items():
            for index,text in enumerate(profile['lines'],1):
                filename=profile.get('filename',f'{profile.get("prefix",prefix)}_{index:02d}.wav')
                name=(f'other_mother_welcome_{state}_{index:02d}' if prefix=='welcome'
                      else f'other_mother_{state.replace("/","_")}_{index:02d}')
                if 'sound_name' in profile:name=profile['sound_name']
                if 'sound_prefix' in manifest:name=f'{manifest["sound_prefix"]}_{index:02d}'
                resource=profile.get('resource_path','/'.join(part for part in (state,Path(filename).stem) if part))
                if name in records:raise ValueError('Duplicate legacy voice event: '+name)
                records[name]={'event':name,'manifest':path.relative_to(root).as_posix(),
                    'state':state,'index':index,'profile':dict(profile,filename=filename),'text':text,
                    'source':(Path(output)/profile.get('source_directory',state)/filename).as_posix(),
                    'resource':'visit/'+'/'.join(part for part in (folder,resource) if part),
                    'reference':manifest['reference'],'reference_sha256':manifest['reference_sha256']}
    return records
