"""Final five only after independent shutdown PCM, genuine and quality gates.
No failed model retries; the original six-hour window and all 18 stopped speech
workers remain recorded. Merely having a saved WAV cannot admit this chain.
"""
import argparse
import json
import math
from pathlib import Path
from finish_verified_staged_audio import receipt,pin
from finish_positive_audio_checkpoint import MANIFESTS,run_final
from run_early_reference_genuine_probe import admit as admit_component,measured_floor
from run_worker_exit_genuine_probe import completed_exit
from run_scoped_quality_admission import admit as admit_quality,measured_floor as quality_floor
from early_reference_retirement import verify as early
from staged_voice_flow import verify_proof as flow
from staged_voice_vocoder import verify_proof as vocoder
from check_scoped_quality import verify as quality_proof
from voice_batch_scope import generation_state

ROOT=Path(__file__).resolve().parents[1]
CHANGED={'tools/generate_early_reference_voice.py','tools/check_early_reference_voice.py','tools/run_early_reference_genuine_probe.py'}


def admit(checkpoint,component,probe,quality):
    previous,prior=receipt(checkpoint,'verified-staged-audio')
    expected=dict(status='failed',historical_speech_ram_stops=17,worker_max_new_takes=1,retry_failed_models=False,completion_window_seconds=21600)
    if any(prior.get(k)!=v for k,v in expected.items()) or prior.get('completion_window_started_utc')!='2026-10-28T21:34:49Z':raise ValueError('Expected preserved eighteenth stopped speech worker/window')
    steps=prior.get('steps',[])
    if len(steps)!=6 or steps[-1].get('name')!='mother_incidental_generate_4':raise ValueError('Failed finalization history changed')
    guards={str(previous):pin(previous)}
    for index,row in enumerate(steps):
        path,n=receipt(Path(row['report']),'compact-voice');status='low_memory_cutoff' if index==len(steps)-1 else 'passed'
        if pin(path)!=row['sha256'] or n.get('status')!=status or row.get('status')!=status or n.get('stop_free_mib')!=384:raise ValueError('Historical worker receipt changed')
        if (status=='passed')!=(n.get('exit_code')==0) or (status=='passed')!=(row.get('exit_code')==0):raise ValueError('Historical exit disagrees')
        guards[str(path)]=pin(path)
    changed=[]
    for name,digest in prior.get('admission_anchors',{}).items():
        path=Path(name).resolve();actual=pin(path)
        if actual!=digest:
            relative=path.relative_to(ROOT).as_posix()
            if relative not in CHANGED:raise ValueError('Unreviewed source/evidence change: '+relative)
            changed.append(relative)
        guards[str(path)]=actual
    if set(changed)!=CHANGED:raise ValueError('Expected only the explicit proven shutdown profile changes')
    component,c=admit_component(component);completed_exit(component)
    probe,p=receipt(probe,'compact-voice');completed_exit(probe)
    expected=dict(status='passed',exit_code=0,stage='synthesis',manifest=MANIFESTS[2],completed=39,
        max_new_takes=1,early_reference_retirement=True,stop_free_mib=384,aggregate_job_limit_mib=12288,memory_measurement_available=True,requested_cuda_allocator='cudaMallocAsync')
    if any(p.get(k)!=v for k,v in expected.items()) or Path(p['early_component_admission']).resolve()!=component:raise ValueError('One genuine take under the changed proven exit profile must pass')
    if pin(component)!=p['early_component_admission_sha256'] or p.get('minimum_start_free_gib',0)<measured_floor(c):raise ValueError('Measured genuine admission changed')
    quality,q=receipt(quality,'compact-voice')
    if any(q.get(k)!=v for k,v in dict(status='passed',exit_code=0,stage='validation',manifest=MANIFESTS[2],stop_free_mib=384,quality_own_page_scope=True,memory_measurement_available=True).items()):raise ValueError('Original saved-take quality validation must pass')
    qc,qn=admit_quality(Path(q['quality_component_admission']));detail,sha=quality_proof(ROOT,Path(q['quality_scope_proof']))
    if sha!=q['quality_scope_proof_sha256'] or detail.get('checks')!=39 or detail.get('partial') is not True:raise ValueError('All 39 preserved takes require original transcription and speaker checks')
    if pin(qc)!=q['quality_component_admission_sha256'] or q.get('minimum_start_free_gib',0)<quality_floor(qn):raise ValueError('Original quality admission changed')
    if {name:pin(ROOT/name) for name in MANIFESTS}!=prior['manifest_sha256']:raise ValueError('Authored words changed')
    counts=[generation_state(ROOT,json.loads((ROOT/name).read_text(encoding='utf-8'))) for name in MANIFESTS]
    if not all(row['generation_complete'] for row in counts[:2]) or counts[2]['completed']!=39:raise ValueError('Expected full Mother/Father and exactly 39 preserved incidental Mother takes')
    floor=max(measured_floor(c),math.ceil((max(float(row['start_free_gib'])-float(row['min_free_gib']) for row in (c,p))+.75)*8)/8)
    if floor>4:raise ValueError('Measured successful profile requires more than approved guarded headroom')
    for path in (component,probe,quality,qc):guards[str(path)]=pin(path)
    for row in (c,p):
        for field,verify in (('early_reference_proof',early),('flow_staging_proof',flow),('vocoder_staging_proof',vocoder)):
            proof=Path(row[field]);_,digest=verify(ROOT,proof)
            if digest!=row[field+'_sha256']:raise ValueError('Changed admitted original model proof')
            guards[str(proof)]=digest
            for name,digest in json.loads(proof.read_text(encoding='utf-8'))['source_sha256'].items():
                source=ROOT/name
                if not source.is_file():source=ROOT/'tools'/name
                if not source.resolve().is_relative_to(ROOT) or pin(source)!=digest:raise ValueError('Changed original model source')
                guards[str(source)]=digest
    for row in (c,p):
        proof,_=completed_exit(component if row is c else probe)
        for name,digest in proof['source_sha256'].items():guards[str(ROOT/name)]=digest
        owner=component if row is c else probe
        markers=[line.split(': ',1)[1] for line in (owner.parent/'output.log').read_text(encoding='utf-8',errors='replace').splitlines() if line.startswith('Verified owned speech completion exit: ')]
        guards[markers[0]]=pin(Path(markers[0]))
    for name,digest in detail['source_sha256'].items():guards[str(ROOT/name)]=digest
    for name in ('tools/finish_worker_exit_audio.py','tools/proven_voice_worker_exit.py','tools/run_compact_voice_batch.py','tools/finish_positive_audio_checkpoint.py','tools/run_early_reference_genuine_probe.py'):
        guards[str(ROOT/name)]=pin(ROOT/name)
    return previous,dict(prior,historical_speech_ram_stops=18),floor,guards,qc,quality_floor(qn)


def main():
    parser=argparse.ArgumentParser()
    for name in ('checkpoint','component','probe','quality'):parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args();previous,prior,floor,guards,qc,qfloor=admit(args.checkpoint,args.component,args.probe,args.quality)
    run_final(previous,prior,5,floor,generation_profile='early',preserve_window=True,source_guards=guards,
        component_admission=args.component,quality_admission=qc,quality_floor=qfloor,
        recovery=dict(changed_profile='Same original speech, continuous own-page scope and durable verified worker exit before Python/CUDA finalizers',
            component=str(args.component),genuine_probe=str(args.probe),quality=str(args.quality),emergency_stop_mib=384,minimum_start_free_gib=floor))


if __name__=='__main__':main()
