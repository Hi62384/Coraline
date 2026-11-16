"""Read-only exact-script generation coverage; a partial batch is never full acceptance."""
import hashlib
import json
from pathlib import Path


def effective_take_limit(requested,memory_efficient):
    """A requested maximum is an upper bound, never a promise of full coverage."""
    if requested is None:return None
    if requested not in (1,5,10,20):raise ValueError('Unsupported bounded take count')
    return min(requested,5) if memory_efficient else requested


def generation_state(root, manifest):
    root=Path(root).resolve()
    output=(root/manifest['output']).resolve()
    if not output.is_relative_to(root):raise ValueError('Voice output escaped project')
    report=output/manifest.get('generation_report','generation_report.json')
    receipt=json.loads(report.read_text(encoding='utf-8')) if report.exists() else {}
    if receipt and receipt.get('reference_sha256')!=manifest['reference_sha256']:
        raise ValueError('Generation reference proof changed')
    completed=0;expected=0;seen=set()
    for state,profile in manifest['states'].items():
        for index,text in enumerate(profile['lines'],1):
            expected+=1
            filename=profile.get('filename',f"{profile.get('prefix',manifest.get('prefix','welcome'))}_{index:02d}.wav")
            key=(state+'/'+filename).lstrip('/')
            path=(output/profile.get('source_directory',state)/filename).resolve()
            if not path.is_relative_to(output) or path in seen:raise ValueError('Invalid/duplicate generation target')
            seen.add(path);row=receipt.get('lines',{}).get(key)
            if not path.exists():
                if row is not None:raise ValueError('Previously receipted master is missing: '+key)
                continue
            digest=hashlib.sha256(path.read_bytes()).hexdigest()
            if row is None or row.get('sha256')!=digest or row.get('text')!=text:
                raise ValueError('Unreceipted or changed master preserved: '+key)
            completed+=1
    return {'expected':expected,'completed':completed,'remaining':expected-completed,
            'generation_complete':completed==expected}
