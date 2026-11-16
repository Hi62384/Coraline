"""Atomic per-recording quality progress; never a release acceptance report."""
import json
import math


def load(path,context):
    if not path.exists():return {'context':context,'checks':{}}
    cached=json.loads(path.read_text(encoding='utf-8'))
    if cached.get('context')!=context:
        raise RuntimeError('Quality-check context changed; preserve/retire the old checkpoint before a fresh check')
    return cached


def reusable(cached,key,source_sha,text_sha):
    row=cached.get('checks',{}).get(key)
    if not row or row.get('source_sha256')!=source_sha or row.get('expected_text_sha256')!=text_sha:return None
    if row.get('file')!=key or row.get('pcm_decode')!='pass':return None
    wer=row.get('word_error_rate');speaker=row.get('reference_speaker_cosine')
    if not isinstance(wer,(float,int)) or not isinstance(speaker,(float,int)) or not math.isfinite(wer) or not math.isfinite(speaker) or wer<0 or not -1<=speaker<=1:return None
    if row.get('speaker_review')!=(speaker<.35) or row.get('needs_review')!=(wer>.20 or speaker<.35):return None
    return dict(row)


def save(path,cached,row):
    if reusable({'checks':{row['file']:row}},row['file'],row['source_sha256'],row['expected_text_sha256']) is None:
        raise RuntimeError('Incomplete or inconsistent quality row cannot be checkpointed')
    cached['checks'][row['file']]=dict(row)
    staged=path.with_suffix('.tmp')
    staged.write_text(json.dumps(cached,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    staged.replace(path)
