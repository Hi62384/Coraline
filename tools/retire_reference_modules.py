"""Release only modules unreachable after a verified fixed-reference restore."""
import gc,hashlib,json
from pathlib import Path

SOURCES=('tools/retire_reference_modules.py','tools/generate_retired_reference_voice.py','tools/fixed_reference_cache.py','.voice-tools/index-tts/indextts/infer_v2.py')

def quantizer_hash(model):
    digest=hashlib.sha256();size=0
    for name,tensor in sorted(model.state_dict().items()):
        if tensor.device.type!='cuda':raise ValueError('Original speech quantizer left the GPU')
        raw=tensor.detach().cpu().contiguous().numpy().tobytes()
        digest.update(name.encode());digest.update(str(tensor.dtype).encode());digest.update(str(tuple(tensor.shape)).encode());digest.update(raw);size+=len(raw)
    if not size:raise ValueError('Missing original speech quantizer')
    return digest.hexdigest(),size

def retire(engine,reference,torch,trim):
    if (engine.cache_spk_audio_prompt!=str(reference) or engine.cache_emo_audio_prompt!=str(reference)
            or any(getattr(engine,key,None) is None for key in ('cache_spk_cond','cache_s2mel_style','cache_s2mel_prompt','cache_mel','cache_emo_cond'))):
        raise ValueError('Complete restored fixed-reference conditioning required')
    codec=engine.semantic_codec
    if type(codec).__name__!='RepCodec' or type(engine.campplus_model).__name__!='CAMPPlus':raise ValueError('Unexpected reference-only module architecture')
    modules=[codec.encoder,codec.decoder,engine.campplus_model]
    if any(module is None for module in modules):raise ValueError('Reference-only modules already missing')
    speech_ids={id(value) for value in list(codec.quantizer.parameters())+list(codec.quantizer.buffers())}
    unused=[value for module in modules for value in list(module.parameters())+list(module.buffers())]
    if any(id(value) in speech_ids for value in unused):raise ValueError('Reference modules alias speech quantizer storage')
    retired=sum(value.numel()*value.element_size() for value in unused)
    if retired<176_000_000:raise ValueError('Unexpected original reference-only module size')
    before,size=quantizer_hash(codec.quantizer)
    stats=dict(retired_bytes=retired,retired_modules=['semantic_codec.encoder','semantic_codec.decoder','campplus_model'],uncached_reference_calls=0,quantizer_sha256=before,quantizer_bytes=size)
    def forbidden(*args,**kwargs):
        stats['uncached_reference_calls']+=1
        raise RuntimeError('Fixed-reference conditioning changed; retired reference encoding cannot run')
    codec.quantize=forbidden
    codec.encoder=None;codec.decoder=None;engine.campplus_model=None
    del modules,unused;gc.collect();torch.cuda.empty_cache();trim()
    after,_=quantizer_hash(codec.quantizer)
    if before!=after:raise ValueError('Original GPU speech quantizer changed')
    stats['original_gpu_quantizer_unchanged']=True
    return stats

def verify(root,path):
    root=Path(root).resolve();path=Path(path).resolve(strict=True)
    if path.suffix!='.json' or path.parent!=(root/'build-character-cast/reference-retirement-proofs').resolve():raise ValueError('Unexpected retirement proof')
    proof=json.loads(path.read_text())
    if (proof.get('status')!='passed' or proof.get('original_gpu_quantizer_unchanged') is not True
            or proof.get('original_speech_settings') is not True or proof.get('uncached_reference_calls')!=0
            or proof.get('retired_bytes',0)<176_000_000 or proof.get('retired_modules')!=['semantic_codec.encoder','semantic_codec.decoder','campplus_model']):raise ValueError('Incomplete reference retirement proof')
    if set(proof.get('source_sha256',{}))!=set(SOURCES):raise ValueError('Incomplete retirement source pins')
    if any(hashlib.sha256((root/name).read_bytes()).hexdigest()!=digest for name,digest in proof['source_sha256'].items()):raise ValueError('Retirement source changed')
    return proof,hashlib.sha256(path.read_bytes()).hexdigest()
