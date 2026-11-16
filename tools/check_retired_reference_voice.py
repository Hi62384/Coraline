"""Detached exact-PCM test; never overwrites a requested catalogue recording."""
import hashlib,json,os,wave
from pathlib import Path
import generate_retired_reference_voice as candidate
ROOT=candidate.ROOT
STATE='other_mother_events_wybie_adventure_caught_normal_low_high_01'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def pcm(path):
    with wave.open(str(path),'rb') as source:return source.getparams()[:4],source.readframes(source.getnframes())
def main():
    import sys
    if '--state' not in sys.argv or sys.argv[sys.argv.index('--state')+1]!=STATE or os.environ.get('CORALINE_VOICE_MANIFEST')!='tools/full_mother_voice_manifest.json':raise ValueError('Exact existing Mother probe state required')
    generator=candidate.speech.flow.cached.generator
    output=Path(os.environ['CORALINE_REFERENCE_RETIREMENT_PROBE_DIR']).resolve()
    if output.parent!=(ROOT/'build-character-cast/reference-retirement-checks').resolve() or output.exists():raise ValueError('Fresh owned detached probe output required')
    output.mkdir(parents=True);profile=generator.MANIFEST['states'][STATE];filename=profile.get('filename','line_01.wav')
    original_output=generator.OUTPUT;baseline=original_output/profile.get('source_directory',STATE)/filename;receipt=original_output/generator.MANIFEST.get('generation_report','generation_report.json')
    row=json.loads(receipt.read_text(encoding='utf-8'))['lines'][STATE+'/'+filename];before=sha(baseline);receipt_before=sha(receipt)
    if before!=row['sha256']:raise ValueError('Existing baseline master changed')
    generator.OUTPUT=output
    try:candidate.main()
    finally:generator.OUTPUT=original_output
    actual=output/profile.get('source_directory',STATE)/filename;other=json.loads((output/generator.MANIFEST.get('generation_report','generation_report.json')).read_text(encoding='utf-8'))['lines'][STATE+'/'+filename]
    if sha(baseline)!=before or sha(receipt)!=receipt_before or pcm(baseline)!=pcm(actual):raise ValueError('Original masters changed or detached PCM differs')
    for key in ('text','seed','attempt','emotion','direction','fixed_reference_cache'):
        if row.get(key)!=other.get(key):raise ValueError('Speech settings changed: '+key)
    proof=dict(status='passed',exact_pcm=True,original_settings=True,certifies_catalogue=False,baseline=str(baseline),baseline_sha256=before,baseline_receipt_sha256=receipt_before,candidate=str(actual),candidate_sha256=sha(actual),probe_source_sha256=sha(Path(__file__)))
    path=output/'equivalence.json';path.write_text(json.dumps(proof,indent=2)+'\n');print('Verified reference-retirement equivalence: '+str(path),flush=True)
if __name__=='__main__':main()
