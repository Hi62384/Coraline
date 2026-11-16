"""Serial completion after a successful changed-profile speech probe.

No failed model is retried. Preserve the exhausted workflow's history and its
original deadline; each worker keeps the existing RAM, job, source and quality
guards. This helper never packages a JAR or approves its own audio.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,sys,time
from datetime import datetime
from run_parent_audio_chunks import exclusive_chain,wait_ready,read_attempt,flow_staging_profile
from staged_voice_flow import verify_proof
from staged_voice_vocoder import verify_proof as verify_vocoder
from voice_batch_scope import generation_state
ROOT=Path(__file__).resolve().parents[1]
def pin(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def receipt(path,folder):
    path=path.resolve(strict=True)
    if path.name!='report.json' or path.parent.parent!=(ROOT/'build-character-cast'/folder).resolve():raise ValueError('Unexpected anchored report')
    return path,json.loads(path.read_text(encoding='utf-8'))

def successful_deadline_checkpoint(path):
    """A separate finite completion window may continue positive progress only.

    Never restart a model failure, clear retry history, or extend a live window.
    The previous receipt and every completed worker remain immutable anchors.
    """
    path,prior=receipt(path,'verified-staged-audio')
    if (prior.get('status')!='failed' or prior.get('error') not in (
            'Original deadline does not permit another bounded worker',
            'Original deadline reached before next worker',
            'Finite audio-chain headroom deadline exhausted')
            or prior.get('continuation_from') or prior.get('historical_speech_ram_stops')!=15
            or prior.get('retry_failed_models') is not False
            or prior.get('worker_max_new_takes')!=1):
        raise ValueError('Only a completed, positive-progress first window may continue')
    if not prior.get('steps'):raise ValueError('No successful progress to continue')
    for row in prior['steps']:
        worker,n=receipt(Path(row['report']),'compact-voice')
        if (row.get('status')!='passed' or row.get('exit_code')!=0
                or pin(worker)!=row.get('sha256') or n.get('status')!='passed'
                or n.get('exit_code')!=0 or n.get('stop_free_mib')!=384
                or n.get('memory_measurement_available') is not True):
            raise ValueError('Failed or changed worker cannot be continued')
    for name,digest in prior.get('manifest_sha256',{}).items():
        if pin(ROOT/name)!=digest:raise ValueError('Continuation manifest changed')
    if set(prior.get('manifest_sha256',{}))!={'tools/full_mother_voice_manifest.json','tools/incidental_father_voice_manifest.json','tools/incidental_mother_voice_manifest.json'}:
        raise ValueError('Incomplete continuation manifest anchors')
    return path,prior
def approved_probe(path):
    path,n=receipt(path,'compact-voice')
    expected={'status':'passed','exit_code':0,'stage':'synthesis','manifest':'tools/full_mother_voice_manifest.json','stop_free_mib':384,'aggregate_job_limit_mib':12288,'memory_measurement_available':True,'requested_cuda_allocator':'cudaMallocAsync'}
    if any(n.get(k)!=v for k,v in expected.items()) or 'tools/generate_staged_vocoder_voice.py' not in n.get('command',[]):raise ValueError('Successful guarded staged speech is required')
    _,vocoder_digest=verify_vocoder(ROOT,Path(n['vocoder_staging_proof']),False)
    if vocoder_digest!=n['vocoder_staging_proof_sha256']:raise ValueError('Changed vocoder staging proof')
    proof=Path(n['flow_staging_proof']).resolve(strict=True);_,digest=verify_proof(ROOT,proof,False)
    if digest!=n['flow_staging_proof_sha256']:raise ValueError('Changed speech staging proof')
    copied=Path(n['bounded_checkpoint_copy_proof']).resolve(strict=True)
    if copied.parent!=(ROOT/'build-character-cast/checkpoint-copy-proofs').resolve() or pin(copied)!=n['bounded_checkpoint_copy_proof_sha256']:raise ValueError('Changed checkpoint copy proof')
    detail=json.loads(copied.read_text());sources=detail.get('source_sha256',{})
    if set(sources)!={'bounded_checkpoint_copy.py','generate_cached_voice.py','low_memory_voice_loader.py'} or any(detail.get(k) is not True for k in ('original_load_state_dict','weights_precision_unchanged','all_values_verified')):raise ValueError('Incomplete original-weight copy proof')
    if any(pin(ROOT/'tools'/name)!=digest for name,digest in sources.items()):raise ValueError('Loading source changed')
    return path,n
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--successful-probe',type=Path,required=True);parser.add_argument('--component-report',type=Path,required=True);parser.add_argument('--vocoder-component-report',type=Path);parser.add_argument('--single-take-workers',action='store_true');parser.add_argument('--continue-successful-run',type=Path,help='One separate, finite completion window after the first window expires with every worker passed');args=parser.parse_args()
    probe,proof=approved_probe(args.successful_probe);component=flow_staging_profile(ROOT,args.component_report)
    if not args.single_take_workers:raise ValueError('The failed multi-take lifetime cannot be retried; require single-take workers')
    component_path,component_receipt=receipt(args.component_report,'compact-voice')
    if component_receipt.get('requested_cuda_allocator')!='cudaMallocAsync':raise ValueError('Matching allocator component proof required')
    if args.vocoder_component_report is None:raise ValueError('Exact original vocoder component check required')
    vocoder_path,vocoder_receipt=receipt(args.vocoder_component_report,'compact-voice')
    if any(vocoder_receipt.get(k)!=v for k,v in {'status':'passed','exit_code':0,'stage':'vocoder_staging_component','requested_cuda_allocator':'cudaMallocAsync','certifies_speech_generation':False,'stop_free_mib':384,'memory_measurement_available':True}.items()):raise ValueError('Successful guarded vocoder component check required')
    _,vocoder_digest=verify_vocoder(ROOT,Path(vocoder_receipt['vocoder_staging_proof']),True)
    if vocoder_digest!=vocoder_receipt['vocoder_staging_proof_sha256']:raise ValueError('Changed vocoder component proof')
    original_path,original=receipt(ROOT/'build-character-cast/parent-audio-chunks/20261027T194933Z/report.json','parent-audio-chunks')
    paused_path,paused=receipt(ROOT/'build-character-cast/parent-audio-chunks/20261027T201716Z/report.json','parent-audio-chunks')
    failure_path,failure=receipt(ROOT/'build-character-cast/compact-voice/20261027T203737Z/report.json','compact-voice')
    last_failure_path,last_failure=receipt(ROOT/'build-character-cast/compact-voice/20261027T221655Z/report.json','compact-voice')
    multi_failure_path,multi_failure=receipt(ROOT/'build-character-cast/compact-voice/20261027T231836Z/report.json','compact-voice')
    if original.get('ram_stops')!=12 or original.get('maximum_ram_stops')!=12 or original.get('status')!='failed' or len([s for s in original['steps'] if s.get('status')=='low_memory_cutoff'])!=12:raise ValueError('Original exhausted history changed')
    if paused.get('started_utc')!=original['started_utc'] or paused.get('ram_stops')!=12 or paused['steps'][:len(original['steps'])]!=original['steps']:raise ValueError('Intermediate recovery history changed')
    if failure.get('status')!='low_memory_cutoff' or failure.get('stage')!='synthesis' or failure.get('stop_free_mib')!=384:raise ValueError('Manual thirteenth speech RAM stop is required in history')
    if last_failure.get('status')!='low_memory_cutoff' or last_failure.get('stage')!='synthesis' or last_failure.get('stop_free_mib')!=384:raise ValueError('Fourteenth speech RAM stop is required in history')
    if multi_failure.get('status')!='low_memory_cutoff' or multi_failure.get('stage')!='synthesis' or multi_failure.get('stop_free_mib')!=384 or multi_failure.get('command',[])[-1:]!=['5']:raise ValueError('Fifteenth speech RAM stop from the longer-lived five-take worker is required in history')
    if proof.get('max_new_takes')!=1:raise ValueError('The proven single-take lifetime is required')
    began=datetime.fromisoformat(original['started_utc'].replace('Z','+00:00')).timestamp()
    continuation=successful_deadline_checkpoint(args.continue_successful_run) if args.continue_successful_run else None
    if continuation and time.time()-began<43200:raise ValueError('The original finite window must expire before a separate completion window starts')
    window_began=time.time() if continuation else began
    window_seconds=64800 if continuation else 43200
    remaining=window_seconds-(time.time()-window_began)
    if not 1800<remaining<=window_seconds:raise ValueError('Original finite deadline expired or insufficient for another bounded worker')
    generation_floor=max(640/1024,float(proof['start_free_gib']))
    if not generation_floor<=4:raise ValueError('Successful probe start floor is outside the guarded range')
    manifests=['tools/full_mother_voice_manifest.json','tools/incidental_father_voice_manifest.json','tools/incidental_mother_voice_manifest.json']
    source_pins={name:pin(ROOT/name) for name in manifests}
    coverage=generation_state(ROOT,json.loads((ROOT/manifests[0]).read_text()))
    if not continuation and not 1<=coverage['completed']-290<=5:raise ValueError('Probe did not save a bounded new exact-script batch beyond the preserved 290 takes')
    folder=ROOT/'build-character-cast/verified-staged-audio'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    state={'status':'ready','original_started_utc':original['started_utc'],'original_deadline_seconds':43200,'historical_speech_ram_stops':15,'retry_failed_models':False,'release_artifact_allowed':False,'worker_max_new_takes':1,'worker_lifetime_basis':'Single-take probe passed; multi-take worker accumulated memory and stopped on its third take. Never repeat that longer lifetime.',
        'anchors':{str(p.relative_to(ROOT)):pin(p) for p in (original_path,paused_path,failure_path,last_failure_path,multi_failure_path,probe,component_path,vocoder_path)},'component':component,'manifest_sha256':source_pins,'steps':[]}
    state.update(controller_pid=os.getpid(),completion_window_started_utc=datetime.fromtimestamp(window_began,__import__('datetime').timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'),completion_window_seconds=window_seconds)
    if continuation:
        previous,prior=continuation
        state['continuation_from']={'report':str(previous),'sha256':pin(previous),'passed_workers':len(prior['steps']),'original_deadline_preserved':True}
    def save(): (folder/'report.json').write_text(json.dumps(state,indent=2)+'\n')
    save();print('Verified audio completion report: '+str(folder/'report.json'),flush=True)
    def launch(name,manifest,command,minimum_gib=2.0):
        if any(pin(ROOT/path)!=digest for path,digest in source_pins.items()):raise ValueError('Authored manifest changed during completion')
        deadline=time.monotonic()+max(0,window_seconds-(time.time()-window_began))
        if deadline-time.monotonic()<1800:raise TimeoutError('Original deadline does not permit another bounded worker')
        state.update(status='waiting_for_headroom',active_step=name,required_start_free_gib=minimum_gib);save();wait_ready(deadline,minimum_gib=minimum_gib)
        if deadline-time.monotonic()<1800:raise TimeoutError('Original deadline reached before next worker')
        state.update(status='running',active_step=name);save();log=folder/(name+'.log')
        with log.open('xb',buffering=0) as output:code=subprocess.run(command,cwd=ROOT,stdout=output,stderr=subprocess.STDOUT,timeout=1860).returncode
        result,path=read_attempt(log,manifest);state['steps'].append({'name':name,'report':str(path),'sha256':pin(path),'status':result.get('status'),'exit_code':code});save()
        if code or result.get('status')!='passed' or result.get('memory_measurement_available') is not True:raise RuntimeError('Worker failed; no automatic retry or later model is permitted: '+name)
        return result
    try:
        with exclusive_chain():
            for who,manifest in zip(('mother','father','mother'),manifests):
                scope='--full-dialogue' if manifest==manifests[0] else '--incidental-dialogue';batch=0
                while True:
                    before=generation_state(ROOT,json.loads((ROOT/manifest).read_text()))
                    if before['completed']==before['expected']:break
                    batch+=1;cmd=[sys.executable,'tools/run_compact_voice_batch.py','--speaker',who,scope,'--memory-efficient','--low-memory-attempt','--require-reference-cache','--stage-flow-model','--stage-vocoder','--cuda-async-allocator','--max-new-takes','1','--timeout-seconds','1800']
                    launch(who+('_full' if scope=='--full-dialogue' else '_incidental')+'_generate_'+str(batch),manifest,cmd,generation_floor)
                    after=generation_state(ROOT,json.loads((ROOT/manifest).read_text()))
                    if after['completed']-before['completed']!=1:raise RuntimeError('Single-take worker did not save exactly one take; further workers stopped')
                launch(who+('_full' if scope=='--full-dialogue' else '_incidental')+'_quality',manifest,[sys.executable,'tools/run_compact_voice_batch.py','--speaker',who,scope,'--low-memory-attempt','--validate','--asr-turbo','--resume-checks','--timeout-seconds','1800'])
            state.update(status='auditing_all_masters',active_step=None);save();audit=folder/'all-master-proofs.json'
            code=subprocess.run([sys.executable,'tools/audit_voice_master_proofs.py','--report',str(audit)],cwd=ROOT,timeout=300).returncode
            result=json.loads(audit.read_text())
            if code or (result.get('status'),result.get('passed'),result.get('failed'))!=('passed',946,0):raise RuntimeError('All 946 genuine voice proofs are required')
            state.update(status='passed',genuine_master_proofs=946);save()
    except BaseException as error:state.update(status='failed',error=str(error));save();raise
    print('All genuine parent voice proofs passed. Native/client checks and the normal genuine resource/JAR pipeline remain separate.',flush=True)
if __name__=='__main__':main()
