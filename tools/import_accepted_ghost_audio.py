"""Branch-only lossless edit of the supplied E master, with inherited cue provenance.

The original refusal-route performance remains byte-for-byte unchanged. Remove
only its incompatible opening statement; retain all following ghost dialogue.
"""
from pathlib import Path
import hashlib,json,math,subprocess,wave
from audio_manifest_io import write_json_if_changed
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
EVENT='mirror_prison_e_accepted'
OFFSET=11.6
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def transcript_sha(rows):return hashlib.sha256(json.dumps(rows,sort_keys=True,ensure_ascii=False).encode()).hexdigest()
def verify(root):
    root=Path(root);assets=root/'src/main/resources/assets/coraline'
    receipt=json.loads((assets/'accepted_button_eyes_audio.json').read_text(encoding='utf-8'))[EVENT]
    if receipt.get('derived_from')!='voicelines/story/MirrorPrison/E.wav' or receipt.get('source')!='voicelines/story/MirrorPrison/accepted_E.wav' or receipt.get('offset_seconds')!=OFFSET:raise ValueError('Unexpected accepted ghost source or edit')
    source=root/receipt['derived_from'];master=root/receipt['source']
    if sha(source)!=receipt['parent_source_sha256'] or sha(master)!=receipt['source_sha256'] or sha(assets/'sounds/mirror_prison/e_accepted.ogg')!=receipt['ogg_sha256']:raise ValueError('Accepted ghost audio identity changed')
    with wave.open(str(source),'rb') as reader:
        params=reader.getparams();frame=round(OFFSET*params.framerate);reader.setpos(frame);pcm=reader.readframes(params.nframes-frame)
    with wave.open(str(master),'rb') as reader:
        if (reader.getnchannels(),reader.getsampwidth(),reader.getframerate())!=(params.nchannels,params.sampwidth,params.framerate) or reader.readframes(reader.getnframes())!=pcm:raise ValueError('Accepted ghost PCM is not the exact original suffix')
    if (receipt['source_frame_start'],receipt['source_frame_end'],receipt['source_rate'],receipt['pcm_range_sha256'])!=(frame,params.nframes,params.framerate,hashlib.sha256(pcm).hexdigest()):raise ValueError('Accepted ghost PCM provenance changed')
    cache=json.loads((root/'tools/story_transcripts.json').read_text(encoding='utf-8'));proofs=json.loads((root/'tools/numbered_original_transcript_sources.json').read_text(encoding='utf-8'))
    expected=[dict(start=round(row['start']-OFFSET,6),end=round(row['end']-OFFSET,6),text=row['text']) for row in cache['mirror_prison_e'] if row['start']>=OFFSET]
    parent=proofs['mirror_prison_e'];proof=proofs[EVENT]
    if len(expected)!=5 or cache[EVENT]!=expected or parent['source_sha256']!=sha(source) or parent['transcript_sha256']!=transcript_sha(cache['mirror_prison_e']) or parent['review_status']!='approved' or parent.get('quality_warnings'):raise ValueError('Accepted ghost inherited cues changed')
    if proof['transcript_sha256']!=transcript_sha(expected) or proof['source_sha256']!=sha(master) or proof['parent_transcript_sha256']!=parent['transcript_sha256'] or proof['offset_seconds']!=OFFSET or proof['review_status']!='approved' or proof.get('quality_warnings'):raise ValueError('Accepted ghost caption provenance changed')
    return receipt
def main():
    source=ROOT/'voicelines/story/MirrorPrison/E.wav';master=ROOT/'voicelines/story/MirrorPrison/accepted_E.wav'
    cache_path=ROOT/'tools/story_transcripts.json';proof_path=ROOT/'tools/numbered_original_transcript_sources.json'
    cache=json.loads(cache_path.read_text(encoding='utf-8'));proofs=json.loads(proof_path.read_text(encoding='utf-8'));parent=proofs['mirror_prison_e']
    if parent['source_sha256']!=sha(source) or parent['transcript_sha256']!=transcript_sha(cache['mirror_prison_e']) or parent['review_status']!='approved' or parent.get('quality_warnings'):raise ValueError('Original E provenance changed')
    with wave.open(str(source),'rb') as reader:
        params=reader.getparams();frame=round(OFFSET*params.framerate)
        if frame/params.framerate!=OFFSET or params.comptype!='NONE':raise ValueError('Expected exact PCM cut')
        reader.setpos(frame);pcm=reader.readframes(params.nframes-frame)
    if not master.exists():
        with wave.open(str(master),'wb') as writer:writer.setparams(params);writer.writeframes(pcm)
    with wave.open(str(master),'rb') as reader:
        if reader.readframes(reader.getnframes())!=pcm:raise ValueError('Lossless range copy mismatch')
    cues=[]
    for cue in cache['mirror_prison_e']:
        if cue['end']<=OFFSET:continue
        if cue['start']<OFFSET:raise ValueError('Edit cuts through a reviewed cue')
        cues.append(dict(start=round(cue['start']-OFFSET,6),end=round(cue['end']-OFFSET,6),text=cue['text']))
    if len(cues)!=5:raise ValueError('Expected exactly the five retained dialogue cues')
    target=ASSETS/'sounds/mirror_prison/e_accepted.ogg';target.parent.mkdir(parents=True,exist_ok=True)
    prior_path=ASSETS/'accepted_button_eyes_audio.json';prior=json.loads(prior_path.read_text(encoding='utf-8')).get(EVENT,{}) if prior_path.exists() else {}
    if not(target.exists() and prior.get('source_sha256')==sha(master) and prior.get('parent_source_sha256')==sha(source) and prior.get('offset_seconds')==OFFSET and prior.get('ogg_sha256')==sha(target)):
        subprocess.run(['ffmpeg','-nostdin','-v','error','-y','-threads','1','-filter_threads','1','-i',str(master),'-vn','-ac','1','-ar','24000','-c:a','libvorbis','-q:a','5',str(target)],check=True)
    seconds=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(target)],text=True))
    subprocess.run(['ffmpeg','-nostdin','-v','error','-threads','1','-i',str(target),'-f','null','-'],check=True)
    receipt={'source':str(master.relative_to(ROOT)).replace('\\','/'),'source_sha256':sha(master),'ogg_sha256':sha(target),'seconds':seconds,'ticks':math.ceil(seconds*20),
        'derived_from':str(source.relative_to(ROOT)).replace('\\','/'),'parent_source_sha256':sha(source),'source_frame_start':frame,'source_frame_end':params.nframes,'source_rate':params.framerate,'pcm_range_sha256':hashlib.sha256(pcm).hexdigest(),'offset_seconds':OFFSET,'transform':'exact PCM suffix; no generated speech'}
    cache[EVENT]=cues;proofs[EVENT]={**parent,'source_sha256':sha(master),'transcript_sha256':transcript_sha(cues),'derived_from':'mirror_prison_e','parent_transcript_sha256':parent['transcript_sha256'],'offset_seconds':OFFSET,'editorial_changes':['Branch-only removal of the incompatible opening cue; five following reviewed cues retained verbatim.']}
    sounds=json.loads((ASSETS/'sounds.json').read_text());lengths=json.loads((ASSETS/'visit_audio_lengths.json').read_text())
    sounds[EVENT]={'sounds':[{'name':'coraline:mirror_prison/e_accepted','stream':True}]};lengths[EVENT]=receipt['ticks']
    for path,data in [(ASSETS/'sounds.json',sounds),(ASSETS/'visit_audio_lengths.json',lengths),(ASSETS/'accepted_button_eyes_audio.json',{EVENT:receipt}),(cache_path,cache),(proof_path,proofs)]:write_json_if_changed(path,data)
    verify(ROOT)
    print('Accepted-route E derivative decoded; exact original PCM suffix and five inherited reviewed cues pinned. Original performance preserved.')
if __name__=='__main__':main()
