"""Opt-in prototype only: original speech with earlier unused-module retirement."""
import hashlib,json,os,time
import generate_staged_vocoder_voice as speech
import fixed_reference_cache as cache
from early_reference_retirement import EarlyRetirement,SOURCES
ROOT=speech.ROOT


def main():
    import sys
    if '--require-reference-cache' not in sys.argv:raise ValueError('Verified reference cache required')
    loader=speech.flow.cached.loader
    original_load=cache.load;original_restore=cache.restore;original_install=loader.install
    admitted=[];stages=[];engines=[]
    def load(*args,**kwargs):
        value=original_load(*args,**kwargs)
        if value is None:raise ValueError('Required fixed-reference cache missing')
        admitted.append(value[1]);return value
    def install(inference,torch,device,model_dir):
        original_install(inference,torch,device,model_dir)
        stage=EarlyRetirement(torch,loader.trim_own_mapped_pages,lambda:len(admitted)==1)
        stage.install(inference);stages.append(stage)
    def restore(engine,reference,*args,**kwargs):
        value=original_restore(engine,reference,*args,**kwargs)
        if len(stages)!=1:raise ValueError('Expected one original engine')
        stages[0].verify_engine(engine,reference);engines.append((engine,reference));return value
    cache.load=load;cache.restore=restore;loader.install=install
    try:speech.main()
    finally:cache.load=original_load;cache.restore=original_restore;loader.install=original_install
    if len(engines)!=1:raise ValueError('Expected restored original engine')
    stats=stages[0].verify_engine(*engines[0]);folder=ROOT/'build-character-cast/early-reference-proofs';folder.mkdir(parents=True,exist_ok=True)
    path=folder/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
    n=dict(status='passed',original_speech_settings=True,source_sha256={name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() for name in SOURCES},**stats)
    path.write_text(json.dumps(n,indent=2)+'\n',encoding='utf-8');print('Verified early reference retirement: '+str(path),flush=True)


if __name__=='__main__':
    from proven_voice_worker_exit import run
    run(main,__file__)
