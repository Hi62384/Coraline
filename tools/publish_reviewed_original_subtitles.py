"""Publish the explicitly reviewed, pinned draft set; never approves new ASR.

Uncertain/nonverbal passages have explicit labels, not fabricated dialogue.
Unedited captions remain machine-assisted and are not human listening acceptance.
"""
from pathlib import Path
import hashlib,json,subprocess
from original_story_audio_catalog import originals

ROOT=Path(__file__).resolve().parents[1]
review_path=ROOT/'tools/original_subtitle_review.json'
review=json.loads(review_path.read_text(encoding='utf-8'))
report_path=(ROOT/review['asr_report']).resolve()
if not report_path.is_relative_to(ROOT/'build-character-cast/original-transcription'):raise RuntimeError('Invalid draft report path')
if hashlib.sha256(report_path.read_bytes()).hexdigest()!=review['asr_report_sha256']:raise RuntimeError('Drafts changed after review; no approval published')
report=json.loads(report_path.read_text(encoding='utf-8'));catalog={name:entry for name,entry in originals(ROOT).items() if not entry['music'] and not entry.get('derived')}
if set(report)!=set(catalog) or len(report)!=review['reviewed_track_count']:raise RuntimeError('Reviewed speech coverage changed')
cache_path=ROOT/'tools/story_transcripts.json';proof_path=ROOT/'tools/numbered_original_transcript_sources.json'
cache=json.loads(cache_path.read_text(encoding='utf-8'));proofs=json.loads(proof_path.read_text(encoding='utf-8'))
outputs={};labels=[]
for name,record in report.items():
    source=ROOT/catalog[name]['source']
    with source.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
    if digest!=record['source_sha256'] or digest!=catalog[name]['source_sha256']:raise RuntimeError('Reviewed master changed: '+name)
    segments=[{'start':float(s['start']),'end':float(s['end']),'text':s['text'].strip()} for s in record['segments']]
    changes=[]
    if name in review['whole_track_captions']:
        text=review['whole_track_captions'][name];duration=catalog[name].get('seconds',catalog[name].get('source_seconds'))
        if duration is None:duration=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(source)],text=True))
        segments=[{'start':0.0,'end':float(duration),'text':text}];changes.append('Explicit editorial caption; see pinned review')
        if '[' in text:labels.append(name)
    for old,new in review['replacements'].get(name,[]):
        count=0
        for segment in segments:
            count+=segment['text'].count(old);segment['text']=segment['text'].replace(old,new)
        if not count:raise RuntimeError('Reviewed correction no longer matches draft: '+name+' / '+old)
        changes.append(old+' -> '+new)
    if not segments or any(s['end']<=s['start'] or not s['text'] for s in segments):raise RuntimeError('Invalid reviewed cue: '+name)
    raw_warnings=['segment_'+str(i) for i,s in enumerate(record['segments']) if s.get('avg_logprob',0)<-1 or s.get('no_speech_prob',0)>.6 or s.get('compression_ratio',0)>2.4]
    if raw_warnings and name not in review['whole_track_captions']:raise RuntimeError('Unresolved decoder warning: '+name)
    outputs[name]=(segments,{'source_sha256':digest,'transcript_sha256':hashlib.sha256(json.dumps(segments,sort_keys=True,ensure_ascii=False).encode('utf-8')).hexdigest(),
        'engine':record['engine'],'review':review['review'],'review_status':'approved','quality_warnings':[],
        'source_decoder_warnings':raw_warnings,'editorial_changes':changes,'review_manifest_sha256':hashlib.sha256(review_path.read_bytes()).hexdigest(),
        'uncertain_caption_labelled':any('[unclear' in s['text'] for s in segments)})
for name,(segments,proof) in outputs.items():cache[name]=segments;proofs[name]=proof
cache_path.write_text(json.dumps(cache,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
proof_path.write_text(json.dumps(proofs,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print(f'Published {len(outputs)} reviewed machine-assisted speech tracks; {len(labels)} explicitly labelled nonverbal/unclear tracks. No human listening claimed.')
