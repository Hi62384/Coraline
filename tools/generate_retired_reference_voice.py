"""Original staged speech, with unused cached-reference modules retired."""
import hashlib,json,os,time
import generate_staged_vocoder_voice as speech
import fixed_reference_cache as cache
from retire_reference_modules import retire,SOURCES
ROOT=speech.ROOT

def main():
    import sys
    if '--require-reference-cache' not in sys.argv:raise ValueError('Verified reference cache is mandatory')
    original=cache.restore;proofs=[]
    def restore(engine,reference,*args,**kwargs):
        provenance=original(engine,reference,*args,**kwargs)
        proofs.append(retire(engine,reference,speech.flow.cached.generator.torch,speech.flow.cached.loader.trim_own_mapped_pages))
        return provenance
    cache.restore=restore
    try:speech.main()
    finally:cache.restore=original
    if len(proofs)!=1 or proofs[0]['uncached_reference_calls']:raise ValueError('Reference cache was invalidated during speech')
    directory=ROOT/'build-character-cast/reference-retirement-proofs';directory.mkdir(parents=True,exist_ok=True)
    path=directory/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
    proof=dict(status='passed',original_speech_settings=True,source_sha256={name:hashlib.sha256((ROOT/name).read_bytes()).hexdigest() for name in SOURCES},**proofs[0])
    path.write_text(json.dumps(proof,indent=2)+'\n');print('Verified unused reference retirement: '+str(path),flush=True)

if __name__=='__main__':main()
