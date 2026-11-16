"""Detached equivalence probe for less frequent own-process page trimming.

The verified model, script, seed and inference stay unchanged. Never overwrite a
requested recording. This experiment cannot approve a catalogue or a release.
"""
import hashlib,json,os,threading,time,wave
from contextlib import contextmanager
from pathlib import Path
import generate_staged_vocoder_voice as speech

ROOT=speech.ROOT
STATE='other_mother_events_wybie_adventure_caught_normal_low_high_01'

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

@contextmanager
def inference_pages(trim,stats):
    done=threading.Event();errors=[]
    def release():
        while not done.wait(.25):
            try:trim();stats['releases']+=1
            except OSError as error:errors.append(str(error));return
    worker=threading.Thread(target=release,daemon=True);worker.start()
    try:yield
    finally:
        done.set();worker.join(timeout=2);trim()
        if worker.is_alive() or errors:raise RuntimeError('Own inference-page release failed: '+str(errors))

def pcm(path):
    with wave.open(str(path),'rb') as audio:
        return (audio.getnchannels(),audio.getsampwidth(),audio.getframerate(),audio.getnframes(),audio.readframes(audio.getnframes()))

def main():
    import sys
    if '--state' not in sys.argv or sys.argv[sys.argv.index('--state')+1]!=STATE:
        raise ValueError('Detached probe requires the exact previously generated baseline state')
    cached=speech.flow.cached;generator=cached.generator
    if os.environ.get('CORALINE_VOICE_MANIFEST')!='tools/full_mother_voice_manifest.json':raise ValueError('Wrong probe speaker/manifest')
    target=Path(os.environ['CORALINE_PAGE_SCOPE_PROBE_DIR']).resolve()
    base=(ROOT/'build-character-cast/page-scope-probes').resolve()
    if target.parent!=base or target.exists():raise ValueError('Probe needs a fresh owned output folder')
    target.mkdir(parents=True)
    profile=generator.MANIFEST['states'][STATE]
    filename=profile.get('filename','line_01.wav')
    baseline=generator.OUTPUT/profile.get('source_directory',STATE)/filename
    baseline_report=generator.OUTPUT/generator.MANIFEST.get('generation_report','generation_report.json')
    row=json.loads(baseline_report.read_text(encoding='utf-8'))['lines'][STATE+'/'+filename]
    if digest(baseline)!=row['sha256']:raise ValueError('Original baseline master changed')
    before=digest(baseline);report_before=digest(baseline_report)
    original_output=generator.OUTPUT;original_bind=cached.bound_own_inference_pages
    stats={'releases':0,'interval_ms':250}
    def bind(engine,scope):
        return original_bind(engine,lambda:inference_pages(cached.loader.trim_own_mapped_pages,stats))
    generator.OUTPUT=target;cached.bound_own_inference_pages=bind
    try:speech.main()
    finally:generator.OUTPUT=original_output;cached.bound_own_inference_pages=original_bind
    actual=target/profile.get('source_directory',STATE)/filename
    candidate=json.loads((target/generator.MANIFEST.get('generation_report','generation_report.json')).read_text(encoding='utf-8'))['lines'][STATE+'/'+filename]
    if digest(baseline)!=before or digest(baseline_report)!=report_before:raise ValueError('Probe changed original recordings or receipts')
    if pcm(actual)!=pcm(baseline):raise ValueError('Detached probe PCM differs from original same-script/seed baseline')
    for key in ('text','seed','attempt','emotion','direction','fixed_reference_cache'):
        if row.get(key)!=candidate.get(key):raise ValueError('Probe changed generation setting: '+key)
    ratio=candidate['inference_seconds']/row['inference_seconds']
    proof=dict(status='passed',baseline=str(baseline),baseline_sha256=before,baseline_receipt_sha256=report_before,candidate=str(actual),candidate_sha256=digest(actual),exact_pcm=True,original_settings=True,certifies_catalogue=False,
               baseline_inference_seconds=row['inference_seconds'],candidate_inference_seconds=candidate['inference_seconds'],time_ratio=ratio,worth_adopting=ratio<=.9,source_sha256=digest(Path(__file__)),**stats)
    path=target/'equivalence.json';path.write_text(json.dumps(proof,indent=2)+'\n')
    print('Verified detached page-scope equivalence: '+str(path),flush=True)

if __name__=='__main__':main()
