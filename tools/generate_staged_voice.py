"""Original cached voice generation with verified just-in-time CFM residency."""
import hashlib,json,os,time
import generate_cached_voice as cached
from staged_voice_flow import factory

ROOT=cached.generator.ROOT
SOURCES=('staged_voice_flow.py','generate_staged_voice.py','generate_cached_voice.py','bounded_checkpoint_copy.py','low_memory_voice_loader.py')


def main():
    stages=[];original_install=cached.loader.install
    allocator=cached.generator.torch.cuda.memory.get_allocator_backend()
    if 'cudaMallocAsync' in os.environ.get('PYTORCH_CUDA_ALLOC_CONF','') and allocator!='cudaMallocAsync':raise ValueError('Requested CUDA allocator is unavailable')
    print('Actual CUDA allocator: '+allocator,flush=True)
    def install(inference,torch,device,model_dir):
        original_factory=inference.MyModel
        original_install(inference,torch,device,model_dir)
        inference.MyModel=factory(original_factory,torch,device,cached.loader.trim_own_mapped_pages,stages)
    cached.loader.install=install
    try:cached.main()
    finally:cached.loader.install=original_install
    if len(stages)!=1 or stages[0].active or stages[0].stats['activations']<=0 or not stages[0].stats['all_values_verified']:
        raise ValueError('Incomplete staged flow verification')
    folder=ROOT/'build-character-cast/flow-staging-proofs';folder.mkdir(parents=True,exist_ok=True)
    path=folder/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
    proof=dict(status='passed',original_gpu_inference=True,original_precision=True,original_cache_dimensions=True,
               allocator_backend=allocator,
               source_sha256={name:hashlib.sha256((ROOT/'tools'/name).read_bytes()).hexdigest() for name in SOURCES},**stages[0].stats)
    path.write_text(json.dumps(proof,indent=2)+'\n',encoding='utf-8')
    print('Verified staged flow weights: '+str(path),flush=True)


if __name__=='__main__':main()
