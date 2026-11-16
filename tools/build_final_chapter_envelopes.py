"""Merge 20 Hz mouth-animation RMS from supplied well OGGs only.

Waveform analysis, NOT voice generation, transcription, subtitles or re-encoding.
Old envelope entries, recordings, original masters and captions are preserved.
"""
from pathlib import Path
import array,hashlib,json,math,shutil,sys,time
from import_final_chapter_recordings import ROOT,ASSET_REL,guarded,digest
from import_numbered_original_audio import install_runtime_outputs

def envelope(pcm):
    samples=array.array('h');samples.frombytes(pcm)
    if sys.byteorder!='little':samples.byteswap()
    rms=[math.sqrt(sum(float(v)*v for v in samples[i:i+800])/len(samples[i:i+800]))/32768 for i in range(0,len(samples),800)]
    peak=max(rms,default=0);gate=max(.004,peak*.035)
    return [round(min(1,max(0,(v-gate)/max(.01,peak-gate)))**.65,4) if v>gate else 0 for v in rms]

def merge(prior,added):
    names={'crawling_hand_'+c for c in 'abcdefghi'}
    if set(added)!=names:raise ValueError('Exactly nine original well envelopes required')
    for name,values in added.items():
        if not values or any(not isinstance(v,(int,float)) or isinstance(v,bool) or not math.isfinite(v) or not 0<=v<=1 for v in values):raise ValueError('Invalid RMS envelope: '+name)
    return {**prior,**added}

def waveform_hash(values):
    return hashlib.sha256(json.dumps(values,separators=(',',':')).encode()).hexdigest()

def cache_matches(proof,identities,values,algorithm):
    if proof.get('status')!='passed' or proof.get('algorithm_sha256')!=algorithm:return False
    try:merge({},values)
    except (ValueError,TypeError):return False
    recorded=proof.get('recordings',{})
    return set(recorded)==set(identities)==set(values) and all(
        recorded[name].get('ogg_sha256')==row['ogg_sha256']
        and recorded[name].get('source_sha256')==row['source_sha256']
        and recorded[name].get('frames_20hz')==len(values[name])
        and recorded[name].get('envelope_sha256')==waveform_hash(values[name])
        for name,row in identities.items())

def main():
    ffmpeg=shutil.which('ffmpeg')
    if not ffmpeg or not Path(ffmpeg).is_absolute():raise RuntimeError('Existing FFmpeg is required for waveform analysis')
    assets=ROOT/ASSET_REL;target=assets/'visit_voice_envelopes.json';before=target.read_bytes();prior=json.loads(before)
    receipts=json.loads((assets/'final_chapter_original_audio.json').read_text())['recordings'];lengths=json.loads((assets/'visit_audio_lengths.json').read_text())
    algorithm=digest(Path(__file__));current={};values={}
    for letter in 'abcdefghi':
        name='crawling_hand_'+letter;receipt=receipts[name];source=assets/'sounds'/(receipt['resource']+'.ogg')
        current[name]={'ogg_sha256':digest(source),'source_sha256':digest(ROOT/receipt['source'])}
        if current[name]['ogg_sha256']!=receipt['ogg_sha256'] or current[name]['source_sha256']!=receipt['source_sha256']:raise RuntimeError('Original recording/master receipt changed: '+name)
        values[name]=prior.get(name,[])
    # A normal task still validates all original inputs and every output value.
    # Only the unchanged, proven PCM conversion may be reused; a missing or
    # altered proof always takes the original guarded analysis path below.
    candidates=sorted((ROOT/'build-character-cast/final-chapter-envelopes').glob('*/report.json'),reverse=True)
    for cached in candidates[:20]:
        try:proof=json.loads(cached.read_text())
        except (OSError,ValueError):continue
        if cache_matches(proof,current,values,algorithm) and all(abs(len(values[k])-lengths[k])<=1 and max(values[k],default=0)>0 for k in values):
            print('Revalidated nine unchanged original waveform envelopes; proof: '+str(cached));return
    folder=ROOT/'build-character-cast/final-chapter-envelopes'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    added={};steps={};identities={}
    for letter in 'abcdefghi':
        name='crawling_hand_'+letter;receipt=receipts[name];source=assets/'sounds'/(receipt['resource']+'.ogg');identity=digest(source)
        if identity!=receipt['ogg_sha256']:raise RuntimeError('Original export receipt changed: '+name)
        if digest(ROOT/receipt['source'])!=receipt['source_sha256']:raise RuntimeError('Supplied master changed: '+name)
        pcm=folder/(name+'.pcm')
        guarded([ffmpeg,'-nostdin','-v','error','-threads','1','-filter_threads','1','-i',str(source),'-f','s16le','-ar','16000','-ac','1',str(pcm)],folder,name,steps)
        added[name]=envelope(pcm.read_bytes())
        if abs(len(added[name])-lengths[name])>1 or max(added[name],default=0)<=0:raise RuntimeError('Missing/truncated original waveform envelope: '+name)
        if digest(source)!=identity:raise RuntimeError('Audio changed during waveform analysis')
        identities[name]={**current[name],'frames_20hz':len(added[name]),'envelope_sha256':waveform_hash(added[name])}
    merged=merge(prior,added)
    if target.read_bytes()!=before:raise RuntimeError('Concurrent envelope change refused')
    if any(merged[k]!=v for k,v in prior.items() if k not in added):raise RuntimeError('Unrelated lip animation changed')
    prepared=folder/target.name;prepared.write_text(json.dumps(merged,separators=(',',':'))+'\n',encoding='utf8')
    if prepared.read_bytes()!=before:install_runtime_outputs(ROOT,folder,{target:prepared},{target:before})
    report={'status':'passed','algorithm_sha256':algorithm,'recordings':identities,'steps':steps,'unrelated_entries_preserved':True,'voice_generation':False,'transcription':False,'subtitles':False,'scope':'20 Hz waveform RMS only; actual native lip/contact synchronization still requires visual review'}
    (folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf8')
    print('Validated nine full-length original well RMS envelopes; report: '+str(folder/'report.json'))

if __name__=='__main__':main()
