"""Actual original CFM equivalence after lossless CPU/GPU weight residency swaps."""
import hashlib,json,os,sys,time
from pathlib import Path
import low_memory_voice_loader as loader

ROOT=Path(__file__).resolve().parents[1]
ENGINE=ROOT/'.voice-tools/index-tts'
os.environ['HF_HUB_OFFLINE']='1';os.environ['TRANSFORMERS_OFFLINE']='1'
sys.path.insert(0,str(ENGINE))


def main():
    with loader.release_own_loading_pages():
        import torch
        allocator=torch.cuda.memory.get_allocator_backend()
        if 'cudaMallocAsync' in os.environ.get('PYTORCH_CUDA_ALLOC_CONF','') and allocator!='cudaMallocAsync':raise ValueError('Requested CUDA allocator is unavailable')
        print('Actual CUDA allocator: '+allocator,flush=True)
        from omegaconf import OmegaConf
        from indextts import infer_v2 as inference
        from staged_voice_flow import factory
        raw=inference.MyModel;model_dir=ENGINE/'checkpoints_2';device='cuda:0'
        loader.install(inference,torch,device,model_dir)
        stages=[];cfg=OmegaConf.load(model_dir/'config.yaml')
        model=factory(raw,torch,device,loader.trim_own_mapped_pages,stages)(cfg.s2mel,use_gpt_latent=True)
        model,_,_,_=inference.load_checkpoint2(model,None,str(model_dir/cfg.s2mel_checkpoint),load_only_params=True,ignore_modules=[],is_distributed=False)
        model=model.to(device);model.eval();flow=model.models['cfm'];stage=stages[0]
        flow.estimator.setup_caches(max_batch_size=1,max_seq_length=8192)
        with torch.inference_mode():
            width=int(cfg.s2mel.DiT.content_dim)
            mu=torch.sin(torch.arange(96*width,device=device,dtype=torch.float32).reshape(1,96,width)/100)
            lengths=torch.tensor([96],device=device);prompt=torch.zeros((1,80,16),device=device)
            style=torch.linspace(-.1,.1,192,device=device).reshape(1,192)
            def seed():torch.manual_seed(17400);torch.cuda.manual_seed_all(17400)
            stage.activate();seed()
            baseline=stage.original_inference(mu.clone(),lengths,prompt,style,None,25,inference_cfg_rate=.7)
            after_cpu=torch.get_rng_state().clone();after_cuda=torch.cuda.get_rng_state().clone()
            stage.deactivate();seed()
            actual=flow.inference(mu.clone(),lengths,prompt,style,None,25,inference_cfg_rate=.7)
            if not torch.equal(actual,baseline) or actual.dtype!=baseline.dtype or not torch.equal(after_cpu,torch.get_rng_state()) or not torch.equal(after_cuda,torch.cuda.get_rng_state()):
                raise ValueError('Original/staged diffusion output or RNG differs')
            if stage.active or any(p.device.type!='cpu' for p in flow.parameters()):raise ValueError('Flow weights did not return to mapped CPU storage')
        folder=ROOT/'build-character-cast/flow-staging-checks';folder.mkdir(parents=True,exist_ok=True)
        path=folder/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
        files=('check_staged_flow.py','staged_voice_flow.py','bounded_checkpoint_copy.py','low_memory_voice_loader.py')
        report=dict(status='passed',certifies_speech_generation=False,original_gpu_inference=True,exact_output=True,exact_cpu_cuda_rng=True,
                    original_precision=True,original_cache_dimensions=True,steps=25,cfg=.7,output_shape=list(actual.shape),**stage.stats,
                    allocator_backend=allocator,output_sha256=hashlib.sha256(actual.cpu().numpy().tobytes()).hexdigest(),
                    cpu_rng_sha256=hashlib.sha256(after_cpu.cpu().numpy().tobytes()).hexdigest(),cuda_rng_sha256=hashlib.sha256(after_cuda.cpu().numpy().tobytes()).hexdigest(),
                    source_sha256={name:hashlib.sha256((ROOT/'tools'/name).read_bytes()).hexdigest() for name in files})
        path.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
        print('Verified staged flow equivalence: '+str(path),flush=True)


if __name__=='__main__':main()
