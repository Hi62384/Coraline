"""Lossless file-backed residency for the original, unmodified GPU vocoder.

Weights are exported only after the original loader and weight-normalization
removal. Original forward, dtype and values are retained; temporary files are
owned exports, never source checkpoints or packaged assets.
"""
import gc,hashlib,json,os,time
from pathlib import Path
from functools import wraps

COMPONENT_SOURCES={'check_staged_vocoder.py','staged_voice_vocoder.py','bounded_checkpoint_copy.py','low_memory_voice_loader.py'}
SPEECH_SOURCES={'generate_staged_vocoder_voice.py','staged_voice_vocoder.py','generate_staged_voice.py','generate_cached_voice.py','staged_voice_flow.py','bounded_checkpoint_copy.py','low_memory_voice_loader.py'}
def checkpoint_pins(root):
    base=Path(root)/'.voice-tools/index-tts/checkpoints_2/hf_cache/bigvgan';result={}
    for name in ('config.json','bigvgan_generator.pt'):
        path=base/name
        if path.is_symlink() or not path.is_file():raise ValueError('Original local vocoder checkpoint missing or linked')
        with path.open('rb') as stream:result[str(path.relative_to(root)).replace('\\','/')]=hashlib.file_digest(stream,'sha256').hexdigest()
    return result
def verify_proof(root,path,component=False):
    path=Path(path).resolve(strict=True);folder='vocoder-staging-checks' if component else 'vocoder-staging-proofs'
    if path.parent!=(root/'build-character-cast'/folder).resolve() or path.suffix!='.json':raise ValueError('Unexpected vocoder proof path')
    raw=path.read_bytes();n=json.loads(raw);expected={'status':'passed','original_gpu_forward':True,'original_precision':True,'all_values_verified':True,'file_backed_cpu_weights':True}
    if component:expected.update(exact_output=True,exact_cpu_cuda_rng=True,certifies_speech_generation=False)
    if any(n.get(k)!=v for k,v in expected.items()) or n.get('activations',0)<(2 if component else 1) or n.get('verified_bytes',0)<=0 or n.get('verified_tensors',0)<=0 or n.get('mapped_weight_bytes',0)<=0:raise ValueError('Incomplete vocoder equivalence/copy proof')
    names=COMPONENT_SOURCES if component else SPEECH_SOURCES
    if set(n.get('source_sha256',{}))!=names or any(hashlib.sha256((root/'tools'/name).read_bytes()).hexdigest()!=sha for name,sha in n['source_sha256'].items()):raise ValueError('Vocoder proof source changed')
    if n.get('checkpoint_sha256')!=checkpoint_pins(root):raise ValueError('Vocoder original checkpoint changed')
    return n,hashlib.sha256(raw).hexdigest()

class VocoderStage:
    LIMIT=2*1024*1024
    def __init__(self,root,model,torch,trim):
        self.root=Path(root);self.model=model;self.torch=torch;self.trim=trim;self.rows=[];self.active=False;self.files=[]
        self.original_forward=model.forward
        self.stats={'all_values_verified':True,'activations':0,'verified_tensors':0,'verified_bytes':0,'mapped_weight_bytes':0}
        base=self.root/'build-character-cast/vocoder-mapped-storage';base.mkdir(parents=True,exist_ok=True)
        if base.is_symlink() or getattr(base.lstat(),'st_file_attributes',0)&0x400:raise ValueError('Linked vocoder export directory')
        self.folder=base/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid()));self.folder.mkdir(exist_ok=False)
        groups={}
        for module in model.modules():
            for kind in ('_parameters','_buffers'):
                for name,tensor in getattr(module,kind).items():
                    if tensor is None:continue
                    if tensor.device.type!='cuda' or not tensor.is_contiguous() or tensor.dtype not in (torch.float32,torch.float16,torch.int64,torch.int32,torch.bool):raise ValueError('Unexpected original vocoder tensor device/layout/dtype')
                    key=id(tensor);groups.setdefault(key,{'tensor':tensor,'locations':[]})['locations'].append((module,kind,name))
        with torch.no_grad():
            for index,group in enumerate(groups.values()):
                source=group.pop('tensor');flat=source.detach().view(-1);path=self.folder/(str(index)+'.bin');self.files.append(path);digest=hashlib.sha256()
                step=max(1,self.LIMIT//source.element_size())
                with path.open('xb',buffering=0) as output:
                    for start in range(0,flat.numel(),step):
                        original=flat[start:start+step];cpu=original.to('cpu');data=cpu.numpy().tobytes();digest.update(data);output.write(data)
                        if not torch.equal(original,cpu.to(original.device)):raise ValueError('Vocoder export differs from original GPU weights')
                        del cpu,data;self.trim()
                    os.fsync(output.fileno())
                mapped=torch.from_file(str(path),shared=False,size=source.numel(),dtype=source.dtype).view(source.shape)
                row={'locations':group['locations'],'source':mapped,'sha256':digest.hexdigest(),'bytes':source.numel()*source.element_size()}
                self.rows.append(row);self.stats['mapped_weight_bytes']+=row['bytes'];self.bind(row,mapped)
                del flat,source;self.trim()
        if not self.rows:raise ValueError('No original vocoder weights exported')
        gc.collect();torch.cuda.empty_cache();self.trim()
        @wraps(self.original_forward)
        def forward(*args,**kwargs):
            self.activate()
            try:return self.original_forward(*args,**kwargs)
            finally:self.deactivate()
        model.forward=forward
    @staticmethod
    def bind(row,value):
        for module,kind,name in row['locations']:
            if kind=='_parameters':module._parameters[name].data=value
            else:module._buffers[name]=value
    def activate(self):
        if self.active:raise RuntimeError('Nested vocoder activation')
        torch=self.torch;self.active=True
        try:
            with torch.no_grad():
                for row in self.rows:
                    source=row['source'];flat=source.view(-1);target=torch.empty(source.shape,device='cuda:0',dtype=source.dtype);out=target.view(-1);digest=hashlib.sha256();step=max(1,self.LIMIT//source.element_size())
                    for start in range(0,flat.numel(),step):
                        original=flat[start:start+step];digest.update(original.numpy().tobytes());out[start:start+step].copy_(original)
                        if not torch.equal(out[start:start+step],original.to(target.device)):raise ValueError('Vocoder activation copy differs')
                        self.trim()
                    if digest.hexdigest()!=row['sha256']:raise ValueError('Original mapped vocoder values changed')
                    self.bind(row,target);self.stats['verified_tensors']+=1;self.stats['verified_bytes']+=row['bytes'];del target
            self.stats['activations']+=1
        except BaseException:self.deactivate();raise
    def deactivate(self):
        for row in self.rows:self.bind(row,row['source'])
        self.active=False;gc.collect();self.torch.cuda.empty_cache();self.trim()
    def close(self):
        # Inference is over. Release only this discarded model's mappings before
        # deleting its exact temporary exports; never recurse or remove masters.
        if self.active:raise RuntimeError('Cannot dispose an active vocoder')
        self.model.forward=self.original_forward
        for row in self.rows:
            for module,kind,name in row['locations']:getattr(module,kind)[name]=None
            row.clear()
        self.rows.clear();self.original_forward=None;self.model=None;gc.collect()
        expected=(self.root/'build-character-cast/vocoder-mapped-storage').resolve()
        if self.folder.resolve().parent!=expected:raise ValueError('Vocoder cleanup escaped its owned export directory')
        for path in self.files:
            if path.is_symlink() or path.resolve().parent!=self.folder.resolve():raise ValueError('Unexpected vocoder cleanup path')
            path.unlink()
        self.folder.rmdir()
