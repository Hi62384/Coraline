"""The unchanged voice generator with verified, bounded constructor copies."""
import hashlib
import json
import os
from pathlib import Path
import time
import generate_other_mother_welcome as generator
import low_memory_voice_loader as loader
from bounded_checkpoint_copy import bounded_copies,bound_own_inference_pages


def main():
    if '--require-reference-cache' not in __import__('sys').argv:
        raise ValueError('Bounded cached speech requires its verified fixed reference')
    original_install=loader.install
    def install(inference,torch,device,model_dir):
        original_install(inference,torch,device,model_dir)
        original_constructor=inference.IndexTTS2
        def construct(*args,**kwargs):
            # Model constructors themselves fault DLLs and initialized checkpoint
            # pages, before torch.load's narrower release scope is entered.
            # Keep only this worker's clean mappings bounded throughout loading.
            with loader.release_own_loading_pages(), bounded_copies(torch,loader.trim_own_mapped_pages) as stats:
                engine=original_constructor(*args,**kwargs)
            bound_own_inference_pages(engine,loader.release_own_loading_pages)
            if not stats['copies'] or not stats['chunks'] or not stats['verified_target_bytes'] or not stats['all_values_verified']:
                raise ValueError('No verified bounded checkpoint copies observed; speech not started')
            directory=generator.ROOT/'build-character-cast/checkpoint-copy-proofs'
            directory.mkdir(parents=True,exist_ok=True)
            path=directory/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
            report={'status':'passed','original_load_state_dict':True,'weights_precision_unchanged':True,**stats,
                    'source_sha256':{name:hashlib.sha256((generator.ROOT/'tools'/name).read_bytes()).hexdigest()
                                     for name in ('bounded_checkpoint_copy.py','generate_cached_voice.py','low_memory_voice_loader.py')}}
            path.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
            print('Verified bounded checkpoint copies: '+str(path),flush=True)
            return engine
        inference.IndexTTS2=construct
    loader.install=install
    try:generator.main()
    finally:loader.install=original_install;generator.check_reference()


if __name__=='__main__':main()
