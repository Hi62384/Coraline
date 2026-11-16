"""Actual original-vocoder equivalence across two different mel lengths."""
import hashlib,json,os,sys,time
from pathlib import Path
import low_memory_voice_loader as loader
ROOT=Path(__file__).resolve().parents[1];ENGINE=ROOT/'.voice-tools/index-tts'
os.environ['HF_HUB_OFFLINE']='1';os.environ['TRANSFORMERS_OFFLINE']='1';sys.path.insert(0,str(ENGINE))
def main():
    stage=None
    try:
        with loader.release_own_loading_pages():
            import torch
            from indextts import infer_v2 as inference
            from staged_voice_vocoder import VocoderStage,COMPONENT_SOURCES,checkpoint_pins
            allocator=torch.cuda.memory.get_allocator_backend()
            if allocator!='cudaMallocAsync':raise ValueError('Exact allocator component check requires cudaMallocAsync')
            print('Actual CUDA allocator: '+allocator,flush=True);model_dir=ENGINE/'checkpoints_2';loader.install(inference,torch,'cuda:0',model_dir)
            model=inference.bigvgan.BigVGAN.from_pretrained(str(model_dir/'hf_cache/bigvgan'),use_cuda_kernel=False).to('cuda:0');model.remove_weight_norm();model.eval()
            samples=[];baselines=[];states=[]
            with torch.inference_mode():
                for length in (64,197):
                    mel=torch.sin(torch.arange(80*length,device='cuda:0',dtype=torch.float32).reshape(1,80,length)/300)*.4;samples.append(mel)
                    torch.manual_seed(17400);torch.cuda.manual_seed_all(17400);baselines.append(model(mel).clone());states.append((torch.get_rng_state().clone(),torch.cuda.get_rng_state().clone()))
                stage=VocoderStage(ROOT,model,torch,loader.trim_own_mapped_pages)
                hashes=[]
                for mel,baseline,(cpu,cuda) in zip(samples,baselines,states):
                    torch.manual_seed(17400);torch.cuda.manual_seed_all(17400);actual=model(mel)
                    if not torch.equal(actual,baseline) or actual.dtype!=baseline.dtype or not torch.equal(cpu,torch.get_rng_state()) or not torch.equal(cuda,torch.cuda.get_rng_state()):raise ValueError('Original/staged vocoder output or RNG differs')
                    hashes.append(hashlib.sha256(actual.cpu().numpy().tobytes()).hexdigest())
                if stage.active or any(p.device.type!='cpu' for p in model.parameters()):raise ValueError('Vocoder did not return to file-backed CPU weights')
            folder=ROOT/'build-character-cast/vocoder-staging-checks';folder.mkdir(parents=True,exist_ok=True);path=folder/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
            proof=dict(status='passed',certifies_speech_generation=False,original_gpu_forward=True,original_precision=True,file_backed_cpu_weights=True,exact_output=True,exact_cpu_cuda_rng=True,allocator_backend=allocator,mel_lengths=[64,197],output_sha256=hashes,checkpoint_sha256=checkpoint_pins(ROOT),source_sha256={name:hashlib.sha256((ROOT/'tools'/name).read_bytes()).hexdigest() for name in COMPONENT_SOURCES},**stage.stats)
            path.write_text(json.dumps(proof,indent=2)+'\n',encoding='utf-8');stage.close();stage=None
            print('Verified staged vocoder equivalence: '+str(path),flush=True)
    finally:
        if stage is not None and not stage.active:stage.close()
if __name__=='__main__':main()
