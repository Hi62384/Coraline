"""Original speech with both verified inactive modules off the GPU."""
import hashlib,json,os,time
import generate_staged_voice as flow
from staged_voice_vocoder import VocoderStage,SPEECH_SOURCES,checkpoint_pins
ROOT=flow.ROOT
def main():
    stages=[];original_install=flow.cached.loader.install
    def install(inference,torch,device,model_dir):
        original_install(inference,torch,device,model_dir);constructor=inference.IndexTTS2
        def construct(*args,**kwargs):
            engine=constructor(*args,**kwargs);stages.append(VocoderStage(ROOT,engine.bigvgan,torch,flow.cached.loader.trim_own_mapped_pages));return engine
        inference.IndexTTS2=construct
    flow.cached.loader.install=install
    try:
        flow.main()
        if len(stages)!=1 or stages[0].active or stages[0].stats['activations']<=0 or not stages[0].stats['all_values_verified']:raise ValueError('Incomplete original vocoder verification')
        folder=ROOT/'build-character-cast/vocoder-staging-proofs';folder.mkdir(parents=True,exist_ok=True);path=folder/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
        proof=dict(status='passed',original_gpu_forward=True,original_precision=True,file_backed_cpu_weights=True,allocator_backend=flow.cached.generator.torch.cuda.memory.get_allocator_backend(),checkpoint_sha256=checkpoint_pins(ROOT),source_sha256={name:hashlib.sha256((ROOT/'tools'/name).read_bytes()).hexdigest() for name in SPEECH_SOURCES},**stages[0].stats)
        path.write_text(json.dumps(proof,indent=2)+'\n',encoding='utf-8')
        for stage in stages:stage.close()
        stages.clear();print('Verified staged vocoder weights: '+str(path),flush=True)
    finally:
        flow.cached.loader.install=original_install
        for stage in stages:
            if not stage.active:stage.close()
if __name__=='__main__':main()
