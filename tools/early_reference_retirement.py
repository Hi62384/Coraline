"""Prototype: retire verified-cache-only modules immediately after loading them.

Every original constructor and checkpoint load still runs. Retirement precedes
the vocoder construction peak rather than waiting for the entire engine. No
speech setting, precision, original checkpoint or original generator is edited.
Activation requires a separate exact-PCM comparison and successful real take.
"""
import functools
import gc
import hashlib
import json
from pathlib import Path
from retire_reference_modules import quantizer_hash

SOURCES=('tools/early_reference_retirement.py','tools/generate_early_reference_voice.py',
         'tools/fixed_reference_cache.py','.voice-tools/index-tts/indextts/infer_v2.py')


class EarlyRetirement:
    def __init__(self,torch,trim,cache_admitted):
        self.torch=torch;self.trim=trim;self.cache_admitted=cache_admitted
        self.codec=None;self.camp=None;self.quantizer=None;self.retired=0
        self.calls=0;self.retirements=[];self.vocoder_started=False

    def forbidden(self,*args,**kwargs):
        self.calls+=1
        raise RuntimeError('Verified fixed-reference cache changed; retired reference encoder cannot run')

    def release(self):
        gc.collect();self.torch.cuda.empty_cache();self.trim()

    def admit(self):
        if not self.cache_admitted():raise ValueError('Verified fixed-reference cache must precede retirement')
        if self.vocoder_started:raise ValueError('Reference retirement happened after vocoder construction began')

    def retire_codec(self,codec):
        self.admit()
        if self.codec is not None or type(codec).__name__!='RepCodec':raise ValueError('Unexpected/duplicate original codec')
        if codec.encoder is None or codec.decoder is None:raise ValueError('Missing original reference modules')
        quantizer_ids={id(v) for v in list(codec.quantizer.parameters())+list(codec.quantizer.buffers())}
        unused=[v for m in (codec.encoder,codec.decoder) for v in list(m.parameters())+list(m.buffers())]
        if any(id(v) in quantizer_ids for v in unused):raise ValueError('Reference modules alias speech quantizer')
        size=sum(v.numel()*v.element_size() for v in unused)
        if size<176_000_000:raise ValueError('Unexpected reference encoder/decoder size')
        before,quantizer_bytes=quantizer_hash(codec.quantizer)
        codec.encoder=None;codec.decoder=None;codec.quantize=self.forbidden
        self.codec=codec;self.quantizer=before;self.quantizer_bytes=quantizer_bytes
        self.retired+=size;self.retirements.extend(('semantic_codec.encoder','semantic_codec.decoder'))
        del unused;self.release()
        if quantizer_hash(codec.quantizer)[0]!=before:raise ValueError('Original CUDA quantizer changed')

    def retire_camp(self,camp):
        self.admit()
        if self.codec is None or self.camp is not None or type(camp).__name__!='CAMPPlus':raise ValueError('Unexpected reference speaker encoder/order')
        speech_ids={id(v) for v in list(self.codec.quantizer.parameters())+list(self.codec.quantizer.buffers())}
        unused=list(camp.parameters())+list(camp.buffers())
        if any(id(v) in speech_ids for v in unused):raise ValueError('Speaker encoder aliases speech quantizer')
        size=sum(v.numel()*v.element_size() for v in unused)
        if size<20_000_000:raise ValueError('Unexpected original speaker encoder size')
        # Keep the original object/constructor/RNG history, but release its
        # unreachable weights before BigVGAN starts allocating/loading.
        camp._modules.clear();camp._parameters.clear();camp._buffers.clear();camp.forward=self.forbidden
        self.camp=camp;self.retired+=size;self.retirements.append('campplus_model')
        del unused;self.release()

    def install(self,inference):
        for attribute,retire in (('build_semantic_codec',self.retire_codec),('CAMPPlus',self.retire_camp)):
            original=getattr(inference,attribute)
            def factory(*args,_original=original,_retire=retire,**kwargs):
                model=_original(*args,**kwargs);evaluate=model.eval
                def after_load_eval(*a,**k):
                    result=evaluate(*a,**k);_retire(model);model.eval=evaluate;return result
                model.eval=after_load_eval
                return model
            setattr(inference,attribute,functools.wraps(original)(factory))
        vocoder=inference.bigvgan.BigVGAN;original=vocoder.from_pretrained
        @functools.wraps(original)
        def vocoder_load(*args,**kwargs):
            if self.vocoder_started or self.retirements!=['semantic_codec.encoder','semantic_codec.decoder','campplus_model']:raise ValueError('Vocoder started before both reference-only retirements')
            self.vocoder_started=True
            return original(*args,**kwargs)
        vocoder.from_pretrained=staticmethod(vocoder_load)

    def verify_engine(self,engine,reference):
        if (engine.semantic_codec is not self.codec or engine.campplus_model is not self.camp
                or self.retirements!=['semantic_codec.encoder','semantic_codec.decoder','campplus_model']
                or self.retired<196_000_000 or self.calls or not self.vocoder_started):raise ValueError('Incomplete early reference retirement')
        if (engine.cache_spk_audio_prompt!=str(reference) or engine.cache_emo_audio_prompt!=str(reference)
                or any(getattr(engine,k,None) is None for k in ('cache_spk_cond','cache_s2mel_style','cache_s2mel_prompt','cache_mel','cache_emo_cond'))):raise ValueError('Conditioning cache was not restored')
        if quantizer_hash(engine.semantic_codec.quantizer)[0]!=self.quantizer:raise ValueError('Original CUDA speech quantizer changed')
        return dict(retired_bytes=self.retired,retired_modules=self.retirements,uncached_reference_calls=self.calls,
                    original_gpu_quantizer_unchanged=True,quantizer_sha256=self.quantizer,
                    quantizer_bytes=self.quantizer_bytes,before_vocoder_construction=True)


def verify(root,path):
    root=Path(root).resolve();path=Path(path).resolve(strict=True)
    if path.parent!=(root/'build-character-cast/early-reference-proofs').resolve():raise ValueError('Unexpected early retirement proof')
    n=json.loads(path.read_text(encoding='utf-8'))
    if (n.get('status')!='passed' or n.get('original_speech_settings') is not True
            or n.get('original_gpu_quantizer_unchanged') is not True
            or n.get('before_vocoder_construction') is not True or n.get('uncached_reference_calls')!=0
            or n.get('retired_bytes',0)<196_000_000
            or n.get('retired_modules')!=['semantic_codec.encoder','semantic_codec.decoder','campplus_model']):raise ValueError('Incomplete early retirement proof')
    if set(n.get('source_sha256',{}))!=set(SOURCES) or any(hashlib.sha256((root/name).read_bytes()).hexdigest()!=digest for name,digest in n['source_sha256'].items()):raise ValueError('Early retirement sources changed')
    return n,hashlib.sha256(path.read_bytes()).hexdigest()
