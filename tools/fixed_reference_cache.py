"""Lossless, source-bound persistence of IndexTTS's five existing reference caches.

Only the original reference path computes these tensors. Speech weights/settings
are untouched; a reference-only preparation process refuses to enter speech code.
"""
import hashlib
import json
from pathlib import Path
import pickle
import random
import re
import time
import os
import sys
from types import SimpleNamespace
import zipfile

NAMES=('cache_spk_cond','cache_s2mel_style','cache_s2mel_prompt','cache_mel','cache_emo_cond')
MAX_BYTES=64*1024*1024


def digest(path):
    value=hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda:stream.read(1024*1024),b''):value.update(block)
    return value.hexdigest()


def json_digest(value):
    return hashlib.sha256(json.dumps(value,sort_keys=True,separators=(',',':')).encode('utf-8')).hexdigest()


def build_context(root,reference,model):
    root=root.resolve();reference=reference.resolve(strict=True);model=model.resolve(strict=True)
    engine=root/'.voice-tools/index-tts'
    config=model/'config.yaml';text=config.read_text(encoding='utf-8')
    paths={config,reference,root/'tools/fixed_reference_cache.py',root/'tools/low_memory_voice_loader.py',
           root/'tools/generate_other_mother_welcome.py'}
    for key in ('s2mel_checkpoint','w2v_stat'):
        match=re.findall(r'^'+key+r':\s*([^\n#]+)',text,re.M)
        if len(match)!=1:raise ValueError('Ambiguous reference model configuration: '+key)
        relative=Path(match[0].strip().strip('\'"'))
        if relative.is_absolute() or '..' in relative.parts:raise ValueError('Reference checkpoint path escaped model')
        paths.add(model/relative)
    bert=model/'hf_cache/w2v-bert-2.0'
    weights=[p for p in bert.rglob('*') if p.is_file() and p.suffix in ('.safetensors','.bin','.json')
             and not any(part.startswith('.') for part in p.relative_to(bert).parts)]
    if not any(p.suffix in ('.safetensors','.bin') for p in weights):raise ValueError('Reference encoder weights unavailable')
    paths.update(weights)
    paths.update((model/'hf_cache/maskgct/semantic_codec/model.safetensors',model/'hf_cache/campplus/campplus_cn_common.bin'))
    paths.update(p for p in (engine/'indextts').rglob('*.py') if not any(x.startswith('.') for x in p.relative_to(engine/'indextts').parts))
    metadata=engine/'.venv/Lib/site-packages'
    paths.add(metadata/'transformers/models/wav2vec2_bert/modeling_wav2vec2_bert.py')
    for package in ('torch','torchaudio','numpy','transformers','safetensors','soundfile','librosa','omegaconf'):
        matches=list(metadata.glob(package+'-*.dist-info/METADATA'))
        if len(matches)!=1:raise ValueError('Ambiguous reference dependency metadata: '+package)
        paths.add(matches[0])
    files={}
    for path in sorted(paths):
        resolved=path.resolve(strict=True)
        if not resolved.is_relative_to(root):raise ValueError('Reference cache input escaped checkout')
        files[resolved.relative_to(root).as_posix()]={'sha256':digest(resolved),'bytes':resolved.stat().st_size}
    return {'schema':2,'reference_sha256':digest(reference),'files':files,
            'contract':{'reference_limit_seconds':15,'reference_encoder':'CPU float32',
                        'same_speaker_emotion_reference':True,'quantization':False,
                        'cpu_rng':'Original unused CPU scalar layer-drop draws replayed once per model lifetime'}}


def fingerprints(arrays):
    if set(arrays)!=set(NAMES):raise ValueError('Incomplete reference conditioning')
    result={}
    for name,array in arrays.items():
        if str(array.dtype) not in ('float16','float32') or not 1<=array.ndim<=4 or not array.size or array.nbytes>MAX_BYTES:
            raise ValueError('Invalid reference tensor: '+name)
        result[name]={'dtype':str(array.dtype),'shape':list(array.shape),
                      'sha256':hashlib.sha256(array.tobytes(order='C')).hexdigest()}
    return result


def cache_base(root,context):
    return root/'.voice-tools/fixed-reference-cache'/context['reference_sha256']/json_digest(context)


def verify_artifact(proof_path,context=None):
    if proof_path.is_symlink() or proof_path.stat().st_size>1024*1024:raise ValueError('Invalid cache proof')
    proof=json.loads(proof_path.read_text(encoding='utf-8'))
    folder=proof_path.parent
    metadata=folder/'metadata.json';payload=folder/'conditioning.npz'
    if any(p.is_symlink() for p in (metadata,payload)) or metadata.stat().st_size>1024*1024 or payload.stat().st_size>MAX_BYTES:
        raise ValueError('Invalid cache payload')
    info=json.loads(metadata.read_text(encoding='utf-8'))
    expected={'status':'passed','original_reference_path':True,'speech_generated':False,
              'random_states_preserved':True,'cpu_rng_replay_verified':True,'reference_seed_independent_tensors':True,
              'reference_speech_models_unchanged':True,'exact_tensor_roundtrip':True}
    if any(proof.get(k)!=v for k,v in expected.items()):raise ValueError('Unverified reference conditioning')
    if proof.get('metadata_sha256')!=digest(metadata) or proof.get('payload_sha256')!=digest(payload):raise ValueError('Changed reference cache')
    if proof.get('context_sha256')!=json_digest(info['context']) or (context is not None and info['context']!=context):
        raise ValueError('Reference/source context changed')
    if set(info['tensors'])!=set(NAMES):raise ValueError('Incomplete cache tensor metadata')
    draws=info.get('cpu_reference_rng_draws')
    if type(draws) is not int or not 0<=draws<=512:raise ValueError('Invalid original reference RNG recipe')
    with zipfile.ZipFile(payload) as archive:
        entries=archive.infolist()
        if len(entries)!=len(NAMES) or {p.filename for p in entries}!={n+'.npy' for n in NAMES} or sum(p.file_size for p in entries)>MAX_BYTES:
            raise ValueError('Unexpected/oversized reference archive')
    return info,proof


def load(root,context,np,required=False):
    base=cache_base(root,context)
    proofs=sorted(base.glob('*/verified.json')) if base.exists() else []
    if not proofs:
        if required:raise ValueError('Prepare the exact source-bound reference cache first')
        return None
    proof_path=proofs[-1];info,proof=verify_artifact(proof_path,context)
    with np.load(proof_path.parent/'conditioning.npz',allow_pickle=False) as stored:
        arrays={name:stored[name].copy() for name in NAMES}
    if fingerprints(arrays)!=info['tensors'] or any(not np.isfinite(a).all() for a in arrays.values()):
        raise ValueError('Reference tensor checksum/values changed')
    return arrays,{'proof':str(proof_path),'proof_sha256':digest(proof_path),
                   'context_sha256':proof['context_sha256'],'payload_sha256':proof['payload_sha256'],
                   'cpu_reference_rng_draws':info['cpu_reference_rng_draws']}


class _UnusedModel:
    def to(self,*a,**k):return self
    def cpu(self):return self
    def eval(self):return self
    def half(self):return self
    def post_init_gpt2_config(self,*a,**k):return self
    def remove_weight_norm(self):return self
    def __call__(self,*a,**k):raise RuntimeError('Reference-only process attempted an unused model')
    def __getattr__(self,name):raise RuntimeError('Reference-only process attempted speech: '+name)


def reference_only_constructor(inference):
    """Omit only models not called before the original reference-ready hook."""
    original=inference.load_checkpoint
    inference.UnifiedVoice=lambda *a,**k:_UnusedModel()
    def checkpoint(model,*a,**k):
        return None if isinstance(model,_UnusedModel) else original(model,*a,**k)
    inference.load_checkpoint=checkpoint
    inference.bigvgan.BigVGAN=SimpleNamespace(from_pretrained=lambda *a,**k:_UnusedModel())


def skip_reference_encoder(inference):
    inference.Wav2Vec2BertModel=SimpleNamespace(from_pretrained=lambda *a,**k:_UnusedModel())


def restore(engine,reference,loaded,torch):
    arrays,provenance=loaded
    for name in NAMES:setattr(engine,name,torch.from_numpy(arrays[name].copy()).to(engine.device))
    engine.cache_spk_audio_prompt=str(reference);engine.cache_emo_audio_prompt=str(reference)
    engine.semantic_model=None;engine._coraline_reference_retired=True
    engine._coraline_reference_rng_pending=provenance['cpu_reference_rng_draws']
    return provenance


def replay_reference_rng(engine,torch):
    draws=getattr(engine,'_coraline_reference_rng_pending',0)
    if type(draws) is not int or not 0<=draws<=512:raise ValueError('Invalid pending reference RNG recipe')
    for _ in range(draws):torch.rand([],device='cpu',dtype=torch.float32)
    engine._coraline_reference_rng_pending=0


def _speech_reference_state(engine,torch):
    """Reference processing must not initialize/change the later speech models."""
    result=hashlib.sha256()
    for name,model in [('codec',engine.semantic_codec),('style',engine.campplus_model),('regulator',engine.s2mel.models['length_regulator'])]:
        for key,tensor in sorted(model.state_dict().items()):
            result.update((name+':'+key+':'+str(tensor.dtype)+':'+str(tuple(tensor.shape))).encode())
            raw=tensor.detach().contiguous().reshape(-1).view(torch.uint8).reshape(-1)
            for offset in range(0,raw.numel(),1024*1024):result.update(raw[offset:offset+1024*1024].cpu().numpy().tobytes())
    return result.hexdigest()


def prepare(engine,reference,root,context,profile,torch,np):
    """Stop at the original post-reference hook; never tokenize/generate speech."""
    class ReferenceReady(Exception):pass
    def rng():
        values={'python':random.getstate(),'numpy':np.random.get_state(),
                'torch_cpu':torch.get_rng_state().cpu().numpy().tobytes(),
                'torch_cuda':[s.cpu().numpy().tobytes() for s in torch.cuda.get_rng_state_all()]}
        return {name:hashlib.sha256(pickle.dumps(value,protocol=4)).hexdigest() for name,value in values.items()}
    def run(seed):
        captured={};draws=[]
        for name in NAMES:setattr(engine,name,None)
        engine.cache_spk_audio_prompt=None;engine.cache_emo_audio_prompt=None
        def progress(value,description):
            if value==.1 and description=='text processing...':
                if engine.cache_spk_audio_prompt!=str(reference) or engine.cache_emo_audio_prompt!=str(reference):
                    raise ValueError('Reference changed during conditioning')
                for name in NAMES:
                    tensor=getattr(engine,name,None)
                    if tensor is None:raise ValueError('Original reference cache incomplete')
                    captured[name]=tensor.detach().cpu().numpy().copy()
                raise ReferenceReady()
        engine._set_gr_progress=progress
        random.seed(seed);np.random.seed(seed);torch.manual_seed(seed);torch.cuda.manual_seed_all(seed)
        before=rng();cpu_before=torch.get_rng_state().clone();original_rand=torch.rand
        def traced_rand(*args,**kwargs):
            result=original_rand(*args,**kwargs)
            source=sys._getframe(1).f_code.co_filename.replace('\\','/')
            if (not source.endswith('/transformers/models/wav2vec2_bert/modeling_wav2vec2_bert.py')
                    or result.device.type!='cpu' or result.ndim!=0 or result.dtype!=torch.float32):
                raise ValueError('Unexpected reference random operation; no cache accepted: '+source)
            draws.append(1)
            if len(draws)>512:raise ValueError('Unexpected reference RNG count')
            return result
        torch.rand=traced_rand
        try:
            with torch.inference_mode():
                engine.infer(spk_audio_prompt=str(reference),text=profile['lines'][0],
                             output_path=str(root/'.voice-tools/welcome-work/reference-only-never-written.wav'),
                             emo_vector=engine.normalize_emo_vec(profile['emotion']),use_random=False,
                             temperature=profile['temperature'],top_p=.8,top_k=30,num_beams=1,
                             max_text_tokens_per_segment=120,max_mel_tokens=1500)
        except ReferenceReady:pass
        else:raise RuntimeError('Reference-only hook did not stop before speech')
        finally:torch.rand=original_rand
        after=rng();cpu_after=torch.get_rng_state().clone()
        if any(before[name]!=after[name] for name in ('python','numpy','torch_cuda')):
            raise ValueError('Unexpected non-CPU reference randomness; no cache accepted')
        torch.set_rng_state(cpu_before)
        for _ in draws:original_rand([],device='cpu',dtype=torch.float32)
        equivalent=torch.equal(torch.get_rng_state(),cpu_after)
        torch.set_rng_state(cpu_after)
        if not equivalent:raise ValueError('Original CPU reference random-state replay differs')
        return captured,len(draws)
    initial_state=_speech_reference_state(engine,torch)
    captured,draws=run(17400)
    repeated,repeated_draws=run(17401)
    if fingerprints(captured)!=fingerprints(repeated) or draws!=repeated_draws:
        raise ValueError('Reference conditioning depends on seed; no persistent cache accepted')
    if _speech_reference_state(engine,torch)!=initial_state:
        raise ValueError('Reference processing changes speech model state; no cache accepted')
    tensor_info=fingerprints(captured)
    if any(not np.isfinite(a).all() for a in captured.values()):raise ValueError('Nonfinite conditioning')
    folder=cache_base(root,context)/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid()))
    folder.mkdir(parents=True,exist_ok=False)
    payload=folder/'conditioning.npz';np.savez_compressed(payload,**captured)
    metadata=folder/'metadata.json';metadata.write_text(json.dumps({'context':context,'tensors':tensor_info,
        'cpu_reference_rng_draws':draws,'reference_model_state_sha256':initial_state},indent=2)+'\n',encoding='utf-8')
    with np.load(payload,allow_pickle=False) as stored:
        for name in NAMES:
            restored=torch.from_numpy(stored[name].copy()).to(engine.device)
            if restored.dtype!=getattr(engine,name).dtype or not torch.equal(restored,getattr(engine,name)):
                raise ValueError('Reference tensor roundtrip changed values/dtype')
    proof={'status':'passed','original_reference_path':True,'speech_generated':False,
           'random_states_preserved':True,'cpu_rng_replay_verified':True,'reference_seed_independent_tensors':True,
           'reference_speech_models_unchanged':True,'exact_tensor_roundtrip':True,'context_sha256':json_digest(context),
           'metadata_sha256':digest(metadata),'payload_sha256':digest(payload)}
    proof_path=folder/'verified.json';proof_path.write_text(json.dumps(proof,indent=2)+'\n',encoding='utf-8')
    verify_artifact(proof_path,context)
    print('Verified lossless fixed-reference cache: '+str(proof_path),flush=True)
    return proof_path
