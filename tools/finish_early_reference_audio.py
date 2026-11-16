"""Finish the last ten takes only after measured early-retirement acceptance.

Preserves the seventeenth stopped speech worker, the expired idle four-GiB wait,
all successful recordings, and the current final window's original deadline.
"""
import argparse
import json
import math
from pathlib import Path
from early_reference_retirement import verify as verify_early
from finish_verified_staged_audio import receipt,pin
from finish_positive_audio_checkpoint import MANIFESTS,run_final
from run_early_reference_genuine_probe import admit as admit_component,measured_floor
from staged_voice_flow import verify_proof as verify_flow
from staged_voice_vocoder import verify_proof as verify_vocoder
from voice_batch_scope import generation_state
from run_scoped_quality_admission import admit as admit_quality,measured_floor as measured_quality_floor
from check_scoped_quality import verify as verify_quality

ROOT=Path(__file__).resolve().parents[1]


def genuine(path):
    path,n=receipt(path,'compact-voice')
    expected=dict(status='passed',exit_code=0,stage='synthesis',manifest=MANIFESTS[2],
                  completed=34,max_new_takes=1,early_reference_retirement=True,
                  stop_free_mib=384,aggregate_job_limit_mib=12288,memory_measurement_available=True,
                  requested_cuda_allocator='cudaMallocAsync')
    if any(n.get(k)!=v for k,v in expected.items()) or n.get('minimum_start_free_gib',0)<2.75:
        raise ValueError('One successful original-profile genuine early-retirement take required')
    component,c=admit_component(Path(n['early_component_admission']))
    if pin(component)!=n['early_component_admission_sha256'] or n['minimum_start_free_gib']<measured_floor(c):raise ValueError('Genuine take changed measured component admission')
    for field,verify in (('early_reference_proof',verify_early),('flow_staging_proof',verify_flow),('vocoder_staging_proof',verify_vocoder)):
        _,digest=verify(ROOT,Path(n[field]))
        if digest!=n[field+'_sha256']:raise ValueError('Genuine take source/weight proof changed')
    return path,n


def admit(checkpoint,component,probe,quality,failed_quality):
    previous,prior=receipt(checkpoint,'verified-staged-audio')
    if (prior.get('status')!='failed' or prior.get('historical_speech_ram_stops')!=16
            or not prior.get('positive_checkpoint_from') or prior.get('higher_headroom_recovery')
            or prior.get('worker_max_new_takes')!=1 or prior.get('retry_failed_models') is not False
            or prior.get('completion_window_seconds')!=21600
            or not str(prior.get('error','')).startswith('Worker failed; no retry or later model permitted: ')):
        raise ValueError('Expected the stopped final positive checkpoint')
    parent=prior['positive_checkpoint_from'];original,old=receipt(Path(parent['report']),'verified-staged-audio')
    if pin(original)!=parent['sha256'] or any(pin(Path(name))!=digest for name,digest in old['anchors'].items()):
        raise ValueError('Original positive checkpoint/profile history changed')
    rows=prior.get('steps',[])
    if not rows:raise ValueError('No stopped worker history')
    for index,row in enumerate(rows):
        path,n=receipt(Path(row['report']),'compact-voice');expected='low_memory_cutoff' if index==len(rows)-1 else 'passed'
        if pin(path)!=row['sha256'] or row['status']!=expected or n.get('status')!=expected or n.get('stop_free_mib')!=384 or n.get('memory_measurement_available') is not True:
            raise ValueError('Worker failure history changed')
        if (expected=='passed' and (row.get('exit_code')!=0 or n.get('exit_code')!=0)) or (expected!='passed' and (row.get('exit_code')==0 or n.get('exit_code')==0)):
            raise ValueError('Worker status/exit history disagrees')
        if index==len(rows)-1 and (n.get('manifest')!=MANIFESTS[2] or n.get('stage')!='synthesis' or n.get('own_child_stopped') is not True):
            raise ValueError('Expected the seventeenth stopped incidental Mother worker')
    component,c=admit_component(component);probe,p=genuine(probe);quality,q=receipt(quality,'compact-voice')
    if Path(p['early_component_admission']).resolve()!=component.resolve():raise ValueError('Different measured component admitted the genuine take')
    if any(q.get(k)!=v for k,v in dict(status='passed',exit_code=0,stage='validation',manifest=MANIFESTS[2],stop_free_mib=384,memory_measurement_available=True).items()) or not {'--existing-only','--resume-checks'}.issubset(q.get('command',[])):
        raise ValueError('All 34 saved incidental Mother takes must pass actual ASR/speaker validation')
    if q.get('quality_own_page_scope') is not True:raise ValueError('Proven original-quality memory scope required after its cutoff')
    quality_component,qc=admit_quality(Path(q['quality_component_admission']))
    if pin(quality_component)!=q['quality_component_admission_sha256'] or q.get('minimum_start_free_gib',0)<measured_quality_floor(qc):raise ValueError('Quality component admission changed')
    qp,qsha=verify_quality(ROOT,Path(q['quality_scope_proof']))
    if qsha!=q['quality_scope_proof_sha256'] or qp.get('checks')!=34 or qp.get('partial') is not True:raise ValueError('Expected all 34 saved original-quality checks')
    failed_quality,fq=receipt(failed_quality,'compact-voice')
    if any(fq.get(k)!=v for k,v in dict(status='low_memory_cutoff',stage='validation',manifest=MANIFESTS[2],stop_free_mib=384,memory_measurement_available=True,own_child_stopped=True).items()):raise ValueError('Original stopped quality receipt must remain unchanged')
    pins={name:pin(ROOT/name) for name in MANIFESTS}
    if pins!=prior['manifest_sha256']:raise ValueError('Authored dialogue changed')
    counts=[generation_state(ROOT,json.loads((ROOT/name).read_text(encoding='utf-8'))) for name in MANIFESTS]
    if not all(n['generation_complete'] for n in counts[:2]) or counts[2]['completed']!=34:
        raise ValueError('Preserved genuine recording coverage changed')
    consumption=max(float(n['start_free_gib'])-float(n['min_free_gib']) for n in (c,p))
    floor=max(2.75,math.ceil((consumption+.75)*8)/8)
    if floor>4:raise ValueError('Measured original profile requires more headroom than the approved range')
    guards={str(path):pin(path) for path in (previous,original,component,probe,quality,failed_quality,quality_component,Path(c['early_equivalence_proof']),Path(qc['quality_equivalence_proof']),Path(qc['quality_scope_proof']),Path(q['quality_scope_proof']))}
    baseline=Path(qc['quality_equivalence_proof']).parent/'baseline-checkpoint.json';guards[str(baseline)]=pin(baseline)
    guards.update(old['anchors'])
    for row in [*old.get('steps',[]),*prior['steps']]:guards[row['report']]=row['sha256']
    for name,digest in qp['source_sha256'].items():guards[str(ROOT/name)]=digest
    for n in (c,p):
        for field in ('early_reference_proof','flow_staging_proof','vocoder_staging_proof','bounded_checkpoint_copy_proof'):
            path=Path(n[field]);guards[str(path)]=pin(path);proof=json.loads(path.read_text(encoding='utf-8'))
            for name,digest in proof.get('source_sha256',{}).items():
                source=ROOT/name
                if not source.is_file():source=ROOT/'tools'/name
                if not source.resolve().is_relative_to(ROOT) or pin(source)!=digest:raise ValueError('Changed admitted speech source')
                guards[str(source)]=digest
    for name in ('tools/check_early_reference_voice.py','tools/finish_early_reference_audio.py','tools/finish_positive_audio_checkpoint.py','tools/run_compact_voice_batch.py','tools/run_early_reference_genuine_probe.py','tools/run_scoped_quality_admission.py'):
        guards[str(ROOT/name)]=pin(ROOT/name)
    return previous,dict(prior,historical_speech_ram_stops=17),floor,guards,quality_component,measured_quality_floor(qc)


def main():
    parser=argparse.ArgumentParser()
    for name in ('checkpoint','component','probe','quality','failed-quality'):parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args();previous,prior,floor,guards,quality_component,quality_floor=admit(args.checkpoint,args.component,args.probe,args.quality,args.failed_quality)
    run_final(previous,prior,10,floor,generation_profile='early',preserve_window=True,source_guards=guards,component_admission=args.component,quality_admission=quality_component,quality_floor=quality_floor,
              recovery=dict(component=str(args.component),genuine_probe=str(args.probe),quality=str(args.quality),
                            changed_profile='Same original reference-only modules retired after their original checkpoint loads, before vocoder construction; actual exact PCM and genuine ASR/speaker acceptance',
                            emergency_stop_mib=384,minimum_start_free_gib=floor))


if __name__=='__main__':main()
