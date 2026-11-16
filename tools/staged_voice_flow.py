"""Keep the unchanged diffusion weights mapped on CPU outside their GPU phase.

No quantization or alternative inference: the original CFM implementation,
arguments, precision and GPU cache construction run unchanged. Every weight is
checked against its original mapped value on each activation.
"""
import gc
import hashlib,json
from pathlib import Path
from functools import wraps
from bounded_checkpoint_copy import bounded_copies


def verify_proof(root,path,component=False):
    path=Path(path).resolve(strict=True)
    directory='flow-staging-checks' if component else 'flow-staging-proofs'
    if path.suffix!='.json' or path.parent!=(root/'build-character-cast'/directory).resolve():raise ValueError('Unexpected flow proof path')
    proof=json.loads(path.read_text(encoding='utf-8'))
    required={'status':'passed','original_gpu_inference':True,'original_precision':True,'original_cache_dimensions':True,'all_values_verified':True}
    if component:required.update(exact_output=True,exact_cpu_cuda_rng=True,certifies_speech_generation=False,steps=25,cfg=.7)
    if any(proof.get(k)!=v for k,v in required.items()) or proof.get('activations',0)<(2 if component else 1) or proof.get('verified_bytes',0)<=0 or proof.get('verified_tensors',0)<=0:
        raise ValueError('Incomplete flow equivalence/copy proof')
    names=({'check_staged_flow.py','staged_voice_flow.py','bounded_checkpoint_copy.py','low_memory_voice_loader.py'} if component else
           {'staged_voice_flow.py','generate_staged_voice.py','generate_cached_voice.py','bounded_checkpoint_copy.py','low_memory_voice_loader.py'})
    if set(proof.get('source_sha256',{}))!=names:raise ValueError('Incomplete flow source proof')
    for name,sha in proof['source_sha256'].items():
        if hashlib.sha256((root/'tools'/name).read_bytes()).hexdigest()!=sha:raise ValueError('Flow proof source changed')
    return proof,hashlib.sha256(path.read_bytes()).hexdigest()


class FlowStage:
    def __init__(self,model,torch,device,trim):
        self.model=model;self.torch=torch;self.device=device;self.trim=trim
        self.sources=None;self.cache_args=None;self.active=False
        self.stats={'activations':0,'verified_tensors':0,'verified_bytes':0,'all_values_verified':True}
        self.original_caches=model.estimator.setup_caches
        model.estimator.setup_caches=self.defer_caches
        original=model.inference
        self.original_inference=original
        @wraps(original)
        def inference(*args,**kwargs):
            self.activate()
            try:return original(*args,**kwargs)
            finally:self.deactivate()
        model.inference=inference

    def defer_caches(self,*args,**kwargs):
        if self.active:raise RuntimeError('Unexpected nested flow cache construction')
        recipe=(args,kwargs)
        if self.cache_args is not None and recipe!=self.cache_args:raise ValueError('Original flow cache recipe changed')
        self.cache_args=recipe

    def prepare(self):
        if self.sources is not None:raise RuntimeError('Flow sources already captured')
        sources=[]
        for module in self.model.modules():
            for kind in ('_parameters','_buffers'):
                for name,value in getattr(module,kind).items():
                    if value is None:continue
                    if value.device.type!='cpu' or value.is_meta or not value.is_contiguous():
                        raise ValueError('Flow source must be original contiguous CPU checkpoint storage')
                    sources.append((module,kind,name,value.detach()))
        if not sources:raise ValueError('Missing original mapped flow weights')
        self.sources=sources

    def activate(self):
        if self.active or self.sources is None or self.cache_args is None:raise RuntimeError('Flow stage not ready')
        self.active=True
        try:
            with bounded_copies(self.torch,self.trim):
                for module,kind,name,source in self.sources:
                    target=self.torch.empty_like(source,device=self.device)
                    target.copy_(source)
                    # Also verify the small tensors not intercepted by the
                    # bounded-copy adapter. Large ones were checked per chunk.
                    if source.numel()*source.element_size()<=2*1024*1024 and not self.torch.equal(target.cpu(),source):
                        self.stats['all_values_verified']=False
                        raise ValueError('Flow checkpoint values changed during activation')
                    if kind=='_parameters':module._parameters[name].data=target
                    else:module._buffers[name]=target
                    self.stats['verified_tensors']+=1
                    self.stats['verified_bytes']+=source.numel()*source.element_size()
                    del target;self.trim()
            self.original_caches(*self.cache_args[0],**self.cache_args[1])
            self.stats['activations']+=1
        except BaseException:
            self.deactivate();raise

    def deactivate(self):
        if self.sources is None:return
        for module,kind,name,source in self.sources:
            if kind=='_parameters':module._parameters[name].data=source
            else:module._buffers[name]=source
        # These are derived, non-persistent tensors, recreated by the original
        # GPU setup function with its original dimensions before every pass.
        transformer=self.model.estimator.transformer
        transformer.freqs_cis=None;transformer.causal_mask=None;transformer.mask_cache=None
        transformer.max_batch_size=-1;transformer.max_seq_length=-1
        for layer in transformer.layers:
            if layer.attention.kv_cache is not None:raise RuntimeError('Unexpected flow KV cache; original setup disables it')
        self.active=False;gc.collect();self.torch.cuda.empty_cache();self.trim()


def factory(original,torch,device,trim,stages):
    def create(*args,**kwargs):
        previous=torch.get_default_dtype()
        try:
            torch.set_default_dtype(torch.float32)
            with torch.device('meta'):model=original(*args,**kwargs)
        finally:torch.set_default_dtype(previous)
        if set(model.models)!={'cfm','length_regulator','gpt_layer'}:raise ValueError('Unexpected flow model layout')
        for child in model.models.values():
            old_load=child.load_state_dict
            def load(state,*args,_load=old_load,_child=child,**kwargs):
                expected=_child.state_dict()
                if set(state)!=set(expected):raise ValueError('Mapped flow checkpoint must cover every original tensor')
                for key,value in state.items():
                    if value.shape!=expected[key].shape or value.dtype!=expected[key].dtype or value.device.type!='cpu':
                        raise ValueError('Flow checkpoint shape/precision/device changed: '+key)
                kwargs['assign']=True
                result=_load(state,*args,**kwargs)
                if any(p.is_meta for p in _child.parameters()):raise ValueError('Unloaded meta flow weight')
                return result
            child.load_state_dict=load
        stage=FlowStage(model.models['cfm'],torch,device,trim)
        def to(target,*args,**kwargs):
            if str(target)!=device or args or kwargs:raise ValueError('Unexpected staged flow transfer')
            for name in ('length_regulator','gpt_layer'):model.models[name].to(target)
            stage.prepare();stages.append(stage)
            print('Mapped original diffusion weights on CPU until their unchanged GPU inference phase.',flush=True)
            return model
        model.to=to
        return model
    return create
