"""Continue only after a changed-profile exact-audio AND genuine-take proof.

Keeps the failed controller's original completion deadline and every failure.
No failed worker is retried automatically. Stops on the first new failure.
"""
import argparse,hashlib,json,math,os,subprocess,sys,time
from datetime import datetime
from pathlib import Path
from finish_verified_staged_audio import receipt,pin
from retire_reference_modules import verify as verify_retirement
from staged_voice_flow import verify_proof as verify_flow
from staged_voice_vocoder import verify_proof as verify_vocoder
from run_parent_audio_chunks import exclusive_chain,wait_ready,read_attempt
from voice_batch_scope import generation_state
ROOT=Path(__file__).resolve().parents[1]

def checked(path,stage):
    path,n=receipt(path,'compact-voice')
    if any(n.get(k)!=v for k,v in dict(status='passed',exit_code=0,stage=stage,reference_only_retirement=True,stop_free_mib=384,aggregate_job_limit_mib=12288,memory_measurement_available=True,requested_cuda_allocator='cudaMallocAsync').items()):raise ValueError('Successful measured retirement profile proof required')
    if n.get('minimum_start_free_gib',0)<2.75:raise ValueError('Retirement start floor changed')
    for field,verify in (('reference_retirement_proof',verify_retirement),('flow_staging_proof',verify_flow),('vocoder_staging_proof',verify_vocoder)):
        _,digest=verify(ROOT,Path(n[field]))
        if digest!=n[field+'_sha256']:raise ValueError('Changed retirement speech proof')
    return path,n

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--failed-controller',type=Path,required=True);parser.add_argument('--equivalence-probe',type=Path,required=True);parser.add_argument('--successful-probe',type=Path,required=True);parser.add_argument('--quality-report',type=Path,required=True);args=parser.parse_args()
    component,c=checked(args.equivalence_probe,'reference_retirement_component');probe,p=checked(args.successful_probe,'synthesis')
    exact=Path(c['reference_equivalence_proof']).resolve(strict=True);equivalence=json.loads(exact.read_text())
    if (not exact.is_relative_to(ROOT/'build-character-cast/reference-retirement-checks') or pin(exact)!=c['reference_equivalence_proof_sha256']
            or equivalence.get('status')!='passed' or equivalence.get('exact_pcm') is not True or equivalence.get('original_settings') is not True
            or equivalence.get('probe_source_sha256')!=pin(ROOT/'tools/check_retired_reference_voice.py')):raise ValueError('Changed or incomplete exact-audio comparison')
    if any(pin(Path(equivalence[name]))!=equivalence[name+'_sha256'] for name in ('baseline','candidate')):raise ValueError('Equivalence recordings changed')
    if p.get('max_new_takes')!=1 or p.get('manifest')!='tools/full_mother_voice_manifest.json' or p.get('completed')!=333:raise ValueError('One newly verified genuine take beyond preserved 332 is required')
    quality,q=receipt(args.quality_report,'compact-voice')
    if any(q.get(k)!=v for k,v in dict(status='passed',exit_code=0,stage='validation',manifest='tools/full_mother_voice_manifest.json',stop_free_mib=384,memory_measurement_available=True).items()):raise ValueError('Saved Mother ASR/speaker validation must pass')
    if '--existing-only' not in q.get('command',[]) or '--resume-checks' not in q.get('command',[]):raise ValueError('Complete existing-master quality checkpoint required')
    failed,old=receipt(args.failed_controller,'verified-staged-audio')
    if (old.get('status')!='failed' or old.get('historical_speech_ram_stops')!=15 or old.get('worker_max_new_takes')!=1
            or old.get('completion_window_seconds')!=64800 or not old.get('continuation_from') or old.get('recovery_from_speech_cutoff')):raise ValueError('Unexpected exhausted recovery history')
    if len(old.get('steps',[]))!=14:raise ValueError('Expected thirteen passed workers and the sixteenth historical RAM stop')
    for index,row in enumerate(old['steps']):
        worker,n=receipt(Path(row['report']),'compact-voice')
        expected='low_memory_cutoff' if index==13 else 'passed'
        if pin(worker)!=row['sha256'] or n.get('status')!=expected or row.get('status')!=expected or n.get('stop_free_mib')!=384:raise ValueError('Worker failure history changed')
        if index==12 and n.get('completed')!=332:raise ValueError('Preserved raw coverage changed')
    began=datetime.fromisoformat(old['completion_window_started_utc'].replace('Z','+00:00')).timestamp();end=began+old['completion_window_seconds']
    if end-time.time()<1800:raise ValueError('Existing completion window exhausted; no reset permitted')
    manifests=['tools/full_mother_voice_manifest.json','tools/incidental_father_voice_manifest.json','tools/incidental_mother_voice_manifest.json']
    source_pins={name:pin(ROOT/name) for name in manifests}
    if source_pins!=old.get('manifest_sha256'):raise ValueError('Authored voice manifests changed')
    if generation_state(ROOT,json.loads((ROOT/manifests[0]).read_text()))['completed']!=333:raise ValueError('Unverified changed-profile progress')
    # Target at least 1 GiB remaining at measured peak; never reduce the new
    # profile's 2.75 GiB floor or the old 384 MiB emergency stop.
    consumption=max(float(n['start_free_gib'])-float(n['min_free_gib']) for n in (c,p))
    floor=max(2.75,math.ceil((consumption+1)*8)/8)
    if floor>4:raise ValueError('Measured profile needs more than approved guarded headroom')
    folder=ROOT/'build-character-cast/verified-staged-audio'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(exist_ok=False)
    state=dict(status='ready',controller_pid=os.getpid(),original_started_utc=old['original_started_utc'],original_deadline_seconds=old['original_deadline_seconds'],completion_window_started_utc=old['completion_window_started_utc'],completion_window_seconds=old['completion_window_seconds'],historical_speech_ram_stops=16,retry_failed_models=False,release_artifact_allowed=False,worker_max_new_takes=1,recovery_from_speech_cutoff=dict(report=str(failed),sha256=pin(failed),changed_profile='Retire only unreachable fixed-reference modules; verified exact PCM; increased measured start headroom'),manifest_sha256=source_pins,anchors={str(path):pin(path) for path in (failed,component,probe,quality,exact)},steps=[],required_start_free_gib=floor)
    def save(): (folder/'report.json').write_text(json.dumps(state,indent=2)+'\n')
    def launch(name,manifest,command,minimum):
        if end-time.time()<1800:raise TimeoutError('Preserved completion deadline does not permit another worker')
        if any(pin(ROOT/name)!=digest for name,digest in source_pins.items()):raise ValueError('Authored manifest changed')
        state.update(status='waiting_for_headroom',active_step=name,required_start_free_gib=minimum);save();wait_ready(time.monotonic()+end-time.time(),minimum_gib=minimum)
        if end-time.time()<1800:raise TimeoutError('Preserved completion deadline reached before worker')
        state.update(status='running');save();log=folder/(name+'.log')
        with log.open('xb',buffering=0) as stream:code=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=1860).returncode
        result,path=read_attempt(log,manifest);state['steps'].append(dict(name=name,report=str(path),sha256=pin(path),status=result.get('status'),exit_code=code));save()
        if code or result.get('status')!='passed' or result.get('memory_measurement_available') is not True:raise RuntimeError('Worker failed; no automatic retry or later model permitted: '+name)
    save();print('Verified audio completion report: '+str(folder/'report.json'),flush=True)
    try:
        with exclusive_chain():
            for who,manifest in zip(('mother','father','mother'),manifests):
                scope='--full-dialogue' if manifest==manifests[0] else '--incidental-dialogue';batch=0
                while True:
                    before=generation_state(ROOT,json.loads((ROOT/manifest).read_text()))
                    if before['generation_complete']:break
                    batch+=1
                    launch(who+('_full' if scope=='--full-dialogue' else '_incidental')+'_generate_'+str(batch),manifest,[sys.executable,'tools/run_compact_voice_batch.py','--speaker',who,scope,'--memory-efficient','--low-memory-attempt','--require-reference-cache','--stage-flow-model','--stage-vocoder','--cuda-async-allocator','--retire-reference-modules','--max-new-takes','1','--timeout-seconds','1800'],floor)
                    if generation_state(ROOT,json.loads((ROOT/manifest).read_text()))['completed']-before['completed']!=1:raise ValueError('Single-take progress changed')
                launch(who+('_full' if scope=='--full-dialogue' else '_incidental')+'_quality',manifest,[sys.executable,'tools/run_compact_voice_batch.py','--speaker',who,scope,'--low-memory-attempt','--validate','--asr-turbo','--resume-checks','--timeout-seconds','1800'],2)
            state.update(status='auditing_all_masters',active_step=None);save();audit=folder/'all-master-proofs.json';code=subprocess.run([sys.executable,'tools/audit_voice_master_proofs.py','--report',str(audit)],cwd=ROOT,timeout=300).returncode
            n=json.loads(audit.read_text())
            if code or (n.get('status'),n.get('passed'),n.get('failed'))!=('passed',946,0):raise ValueError('All genuine voice proofs required')
            state.update(status='passed',genuine_master_proofs=946);save()
    except BaseException as error:state.update(status='failed',error=str(error));save();raise
if __name__=='__main__':main()
