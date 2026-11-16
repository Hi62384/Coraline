"""Prototype: retire the original cached-only CPU Wav2Vec encoder after eval.
All original constructors, checkpoint loads, RNG and speech operations remain.
Detached PCM equivalence and original quality checks are separate admissions.
"""
import functools,gc,hashlib,json,os,sys,time
from pathlib import Path
from low_memory_voice_loader import release_own_loading_pages
ROOT=Path(__file__).resolve().parents[1]
SOURCES=('tools/semantic_reference_retirement.py','tools/generate_semantic_reference_voice.py','tools/check_semantic_reference_voice.py')


def install(base):
    class SemanticRetirement(base):
        def __init__(self,*args,**kwargs):
            super().__init__(*args,**kwargs);self.semantic_bytes=0;self.semantic_calls=0;self.semantic=None
        def forbidden_semantic(self,*args,**kwargs):
            self.semantic_calls+=1;raise RuntimeError('Verified original conditioning cache missing; retired reference encoder cannot run')
        def install(self,inference):
            super().install(inference);factory=inference.Wav2Vec2BertModel;original=factory.from_pretrained
            @functools.wraps(original)
            def load(*args,**kwargs):
                model=original(*args,**kwargs);evaluate=model.eval
                def retire(*args,**kwargs):
                    value=evaluate(*args,**kwargs);self.admit()
                    if self.semantic is not None:raise ValueError('Duplicate semantic encoder retirement')
                    tensors=list(model.parameters())+list(model.buffers())
                    if any(tensor.device.type!='cpu' for tensor in tensors):raise ValueError('Expected the unchanged original CPU reference profile')
                    self.semantic_bytes=sum(t.numel()*t.element_size() for t in tensors)
                    if self.semantic_bytes<1_000_000_000:raise ValueError('Unexpected original semantic reference encoder size')
                    model._modules.clear();model._parameters.clear();model._buffers.clear();model.forward=self.forbidden_semantic
                    model.eval=evaluate;self.semantic=model;del tensors;gc.collect();self.release();return value
                model.eval=retire;return model
            factory.from_pretrained=staticmethod(load)
        def verify_engine(self,engine,reference):
            result=super().verify_engine(engine,reference)
            if self.semantic is None or self.semantic_bytes<1_000_000_000 or self.semantic_calls:raise ValueError('Early semantic retirement was not proven')
            result.update(semantic_reference_bytes_retired=self.semantic_bytes,semantic_reference_calls=self.semantic_calls)
            return result
    return SemanticRetirement


def finish(callback,entry):
    entry=Path(entry).resolve();allowed={ROOT/'tools/generate_semantic_reference_voice.py',ROOT/'tools/check_semantic_reference_voice.py'}
    if entry not in allowed or os.name!='nt':raise ValueError('Owned prototype worker required')
    with release_own_loading_pages():
        callback()
        folder=ROOT/'build-character-cast/semantic-reference-proofs';folder.mkdir(parents=True,exist_ok=True)
        path=folder/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
        proof=dict(status='passed',original_model_settings=True,callback_completed=True,own_process_exit=True,
                   source_sha256={name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() for name in SOURCES})
        with path.open('x',encoding='utf-8') as stream:stream.write(json.dumps(proof,indent=2)+'\n');stream.flush();os.fsync(stream.fileno())
        print('Verified early semantic reference retirement: '+str(path),flush=True);sys.stdout.flush();sys.stderr.flush();os._exit(0)


def verify(path):
    path=Path(path).resolve(strict=True)
    if path.parent!=(ROOT/'build-character-cast/semantic-reference-proofs').resolve():raise ValueError('Unexpected semantic prototype receipt')
    proof=json.loads(path.read_text(encoding='utf-8'))
    if any(proof.get(k)!=v for k,v in dict(status='passed',original_model_settings=True,callback_completed=True,own_process_exit=True).items()):raise ValueError('Incomplete semantic prototype proof')
    if set(proof.get('source_sha256',{}))!=set(SOURCES) or any(hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=digest for name,digest in proof['source_sha256'].items()):raise ValueError('Semantic prototype source changed')
    return proof,hashlib.sha256(path.read_bytes()).hexdigest()
