"""Read-only admission for unchanged-result whole-inference page management."""
import json,math
from pathlib import Path
from check_scoped_quality import verify,SOURCES,sha
from finish_verified_staged_audio import receipt,pin
ROOT=Path(__file__).resolve().parents[1]


def admit(path):
    path,n=receipt(path,'compact-voice')
    expected=dict(status='passed',exit_code=0,stage='quality_page_scope_component',
                  manifest='tools/incidental_mother_voice_manifest.json',quality_own_page_scope=True,
                  stop_free_mib=384,memory_measurement_available=True)
    if any(n.get(k)!=v for k,v in expected.items()) or n.get('minimum_start_free_gib',0)<2.5:
        raise ValueError('Actual guarded original-quality component must pass')
    proof=Path(n['quality_scope_proof']);_,digest=verify(ROOT,proof)
    if digest!=n['quality_scope_proof_sha256']:raise ValueError('Quality scope proof changed')
    exact=Path(n['quality_equivalence_proof']).resolve(strict=True)
    expected_path=(ROOT/'build-character-cast/quality-scope-checks'/path.parent.name/'equivalence.json').resolve()
    if exact!=expected_path or pin(exact)!=n['quality_equivalence_proof_sha256']:raise ValueError('Detached original quality comparison changed')
    p=json.loads(exact.read_text(encoding='utf-8'))
    if p.get('status')!='passed' or p.get('same_transcription_and_quality') is not True or p.get('speaker_cosine_tolerance',1)>1E-7 or p.get('probe_source_sha256')!=sha(ROOT/'tools/check_scoped_quality.py'):
        raise ValueError('Original transcription/speaker results were not preserved')
    if pin(exact.parent/'baseline-checkpoint.json')!=p['baseline_checkpoint_sha256']:raise ValueError('Original baseline quality evidence changed')
    for key in ('source_sha256','expected_text_sha256','transcription','word_error_rate','needs_review','speaker_review','seconds','pcm_decode'):
        if p['actual'].get(key)!=p['baseline'].get(key):raise ValueError('Quality comparison differs: '+key)
    if abs(p['actual']['reference_speaker_cosine']-p['baseline']['reference_speaker_cosine'])>1E-7:raise ValueError('Speaker result changed')
    return path,n


def measured_floor(component):
    consumption=float(component['start_free_gib'])-float(component['min_free_gib'])
    if not math.isfinite(consumption) or consumption<0:raise ValueError('Invalid measured quality consumption')
    floor=max(2,math.ceil((consumption+.75)*8)/8)
    if floor>4:raise ValueError('Quality model needs more than approved headroom')
    return floor
