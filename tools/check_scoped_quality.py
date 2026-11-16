"""Original word/speaker validation with own-page trimming through inference.

Does not modify the original validator, ASR loader, reference, model precision,
inputs, thresholds or saved checks. The detached admission compares an existing
source-matched quality row; full acceptance remains the original validator's.
"""
import hashlib,json,os,sys,time
from contextlib import nullcontext
from pathlib import Path
import check_welcome_audio as validator
from low_memory_voice_loader import release_own_loading_pages

ROOT=validator.ROOT
SOURCES=('tools/check_scoped_quality.py','tools/check_welcome_audio.py','tools/local_turbo_asr.py','tools/low_memory_voice_loader.py')
STATE='mother_53b786200560bb87'


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def verify(root,path):
    root=Path(root).resolve();path=Path(path).resolve(strict=True)
    if path.parent!=(root/'build-character-cast/quality-scope-proofs').resolve():raise ValueError('Unexpected quality page-scope receipt')
    n=json.loads(path.read_text(encoding='utf-8'))
    if n.get('status')!='passed' or n.get('original_validator') is not True or n.get('original_models_and_thresholds') is not True or n.get('whole_validation_own_pages') is not True:raise ValueError('Incomplete quality memory-scope proof')
    if set(n.get('source_sha256',{}))!=set(SOURCES) or any(sha(root/name)!=digest for name,digest in n['source_sha256'].items()):raise ValueError('Quality source changed')
    return n,sha(path)


def main():
    required={'--transcribe','--speaker-check','--asr-turbo','--memory-efficient-loading'}
    if not required.issubset(sys.argv):raise ValueError('Original pinned Turbo/speaker quality path required')
    output=os.environ.get('CORALINE_QUALITY_SCOPE_PROBE_DIR');baseline=None;probe=None
    if output:
        if ('--state' not in sys.argv or sys.argv[sys.argv.index('--state')+1]!=STATE
                or '--resume-checks' in sys.argv or '--existing-only' in sys.argv
                or os.environ.get('CORALINE_VOICE_MANIFEST')!='tools/incidental_mother_voice_manifest.json'):
            raise ValueError('Detached quality comparison requires the known original Mother state without reusing checks')
        probe=Path(output).resolve()
        if probe.parent!=(ROOT/'build-character-cast/quality-scope-checks').resolve() or probe.exists():raise ValueError('Fresh owned quality comparison required')
        checkpoint=validator.SOURCE/'validation_checkpoint.json';raw=checkpoint.read_bytes();cached=json.loads(raw)
        context=cached['context'];manifest_sha=hashlib.sha256(json.dumps(validator.MANIFEST,sort_keys=True,ensure_ascii=False).encode('utf-8')).hexdigest()
        if (context['manifest_sha256']!=manifest_sha or context['reference_sha256']!=validator.MANIFEST['reference_sha256']
                or context['validator_sha256']!=sha(ROOT/'tools/check_welcome_audio.py')
                or context['asr_loader_sha256']!=sha(ROOT/'tools/local_turbo_asr.py')
                or context['memory_adapter_sha256']!=sha(ROOT/'tools/low_memory_voice_loader.py')):
            raise ValueError('Original quality checkpoint context changed')
        baseline=cached['checks'][STATE+'/line_01.wav']
        profile=validator.MANIFEST['states'][STATE]
        master=validator.SOURCE/profile.get('source_directory',STATE)/'line_01.wav'
        if baseline.get('needs_review') is not False or baseline['source_sha256']!=sha(master):raise ValueError('Known original quality row unavailable')
        probe.mkdir(parents=True);(probe/'baseline-checkpoint.json').write_bytes(raw)
    original_scope=validator.release_own_loading_pages
    # The original validator's narrower loading context becomes a no-op only
    # inside the SAME original helper spanning loading and all inference below.
    validator.release_own_loading_pages=nullcontext
    try:
        with release_own_loading_pages():validator.main()
    finally:validator.release_own_loading_pages=original_scope
    report_path=validator.SOURCE/validator.MANIFEST.get('validation_report','validation_report.json')
    actual=json.loads(report_path.read_text(encoding='utf-8'))
    if any(row.get('needs_review') for row in actual['checks']):raise ValueError('Original speech quality check failed')
    folder=ROOT/'build-character-cast/quality-scope-proofs';folder.mkdir(parents=True,exist_ok=True)
    path=folder/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
    proof=dict(status='passed',original_validator=True,original_models_and_thresholds=True,whole_validation_own_pages=True,
               source_sha256={name:sha(ROOT/name) for name in SOURCES},checks=len(actual['checks']),partial=actual['partial'])
    path.write_text(json.dumps(proof,indent=2)+'\n',encoding='utf-8');print('Verified scoped original quality: '+str(path),flush=True)
    if probe:
        if len(actual['checks'])!=1:raise ValueError('Detached quality comparison validated unexpected rows')
        row=actual['checks'][0]
        for key in ('source_sha256','expected_text_sha256','transcription','word_error_rate','needs_review','speaker_review','seconds','pcm_decode'):
            if row.get(key)!=baseline.get(key):raise ValueError('Original quality result changed: '+key)
        if abs(row['reference_speaker_cosine']-baseline['reference_speaker_cosine'])>1E-7:raise ValueError('Original speaker result changed')
        equivalent=dict(status='passed',same_transcription_and_quality=True,speaker_cosine_tolerance=1E-7,
                        baseline=baseline,actual=row,baseline_checkpoint_sha256=sha(probe/'baseline-checkpoint.json'),
                        probe_source_sha256=sha(Path(__file__)))
        exact=probe/'equivalence.json';exact.write_text(json.dumps(equivalent,indent=2)+'\n',encoding='utf-8')
        print('Verified original quality equivalence: '+str(exact),flush=True)


if __name__=='__main__':main()
