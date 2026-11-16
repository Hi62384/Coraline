"""One genuine take only after actual unchanged-settings PCM equivalence."""
import argparse
from datetime import datetime
import hashlib
import json
import math
from pathlib import Path
import subprocess
import sys
import time
from early_reference_retirement import verify as verify_early
from finish_verified_staged_audio import receipt,pin
from staged_voice_flow import verify_proof as verify_flow
from staged_voice_vocoder import verify_proof as verify_vocoder
from run_parent_audio_chunks import wait_ready
from voice_batch_scope import generation_state

ROOT=Path(__file__).resolve().parents[1]


def admit(path):
    path,n=receipt(path,'compact-voice')
    if any(n.get(k)!=v for k,v in dict(status='passed',exit_code=0,stage='early_reference_component',
            early_reference_retirement=True,stop_free_mib=384,aggregate_job_limit_mib=12288,
            memory_measurement_available=True,requested_cuda_allocator='cudaMallocAsync').items()):
        raise ValueError('Actual successful guarded early-retirement component required')
    if n.get('minimum_start_free_gib',0)<3.25:raise ValueError('Component admission floor changed')
    for field,verify in (('early_reference_proof',verify_early),('flow_staging_proof',verify_flow),('vocoder_staging_proof',verify_vocoder)):
        _,digest=verify(ROOT,Path(n[field]))
        if digest!=n[field+'_sha256']:raise ValueError('Component source/weight proof changed')
    exact=Path(n['early_equivalence_proof']).resolve(strict=True)
    if exact!=(ROOT/'build-character-cast/early-reference-checks'/path.parent.name/'equivalence.json').resolve() or pin(exact)!=n['early_equivalence_proof_sha256']:
        raise ValueError('Detached comparison receipt changed')
    p=json.loads(exact.read_text(encoding='utf-8'))
    if p.get('status')!='passed' or p.get('exact_pcm') is not True or p.get('original_settings') is not True or p.get('probe_source_sha256')!=pin(ROOT/'tools/check_early_reference_voice.py'):
        raise ValueError('Actual original-settings exact PCM comparison required')
    if any(pin(Path(p[name]))!=p[name+'_sha256'] for name in ('baseline','candidate')):
        raise ValueError('Original comparison recordings changed')
    return path,n


def measured_floor(component):
    consumption=float(component['start_free_gib'])-float(component['min_free_gib'])
    if not math.isfinite(consumption) or consumption<0:raise ValueError('Invalid measured component consumption')
    # The separately proven worker-exit profile keeps own-page management alive
    # until OS teardown. Admit it only from its actual durable exit receipt, not
    # from an argument claiming the finalizer spike has been fixed.
    exact=component.get('early_equivalence_proof')
    if exact:
        directory=Path(exact).resolve().parent.name
        log=ROOT/'build-character-cast/compact-voice'/directory/'output.log'
        if log.is_file():
            markers=[line.split(': ',1)[1] for line in log.read_text(encoding='utf-8',errors='replace').splitlines() if line.startswith('Verified owned speech completion exit: ')]
            if markers:
                if len(markers)!=1:raise ValueError('Duplicate worker completion receipt')
                from proven_voice_worker_exit import verify
                verify(ROOT,Path(markers[0]))
                floor=max(3.25,math.ceil((consumption+.75)*8)/8)
                if floor>4:raise ValueError('Completed-exit profile requires more than approved guarded headroom')
                return floor
    floor=max(2.75,math.ceil((consumption+1)*8)/8)
    if floor>4:raise ValueError('Component requires more than the approved guarded headroom range')
    return floor


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--component',type=Path,required=True);parser.add_argument('--deadline-utc');args=parser.parse_args()
    path,n=admit(args.component);component_sha=pin(path)
    floor=measured_floor(n)
    remaining=datetime.fromisoformat(args.deadline_utc.replace('Z','+00:00')).timestamp()-time.time() if args.deadline_utc else 3600
    if not 0<remaining<=3601:raise ValueError('Original finite genuine-probe admission window expired or changed')
    manifest=ROOT/'tools/incidental_mother_voice_manifest.json';manifest_sha=pin(manifest)
    if generation_state(ROOT,json.loads(manifest.read_text(encoding='utf-8')))['completed']!=33:
        raise ValueError('Expected preserved 33 incidental Mother recordings')
    print('Single genuine early-retirement probe after exact-PCM component: '+str(path)+'; measured consumption plus 1 GiB margin; start floor '+str(floor)+' GiB',flush=True)
    wait_ready(time.monotonic()+remaining,minimum_gib=floor)
    admit(path)
    if pin(path)!=component_sha or pin(manifest)!=manifest_sha:raise ValueError('Admitted component or dialogue changed during wait')
    command=[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue',
             '--memory-efficient','--low-memory-attempt','--require-reference-cache','--stage-flow-model',
             '--stage-vocoder','--cuda-async-allocator','--early-reference-retirement',
             '--early-reference-admission',str(path),
             '--max-new-takes','1','--timeout-seconds','1800']
    raise SystemExit(subprocess.run(command,cwd=ROOT,timeout=1860).returncode)


if __name__=='__main__':main()
