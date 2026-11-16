"""Complete parent audio with small, serial, checkpointed model lifetimes.

Only RAM-cutoff attempts may resume automatically, after measured headroom
returns. All failed reports remain failed. Quality errors, orphan/changed
masters, unexpected exits and exhausted finite budgets stop the chain.
No application shutdown, packaging, changed scripts/seeds/models/precision,
or relaxed speech/emergency-memory thresholds.
"""
import argparse
import contextlib
import hashlib
import json
import math
import os
from pathlib import Path
import subprocess
import sys
import time
from datetime import datetime
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job
from voice_batch_scope import generation_state

ROOT=Path(__file__).resolve().parents[1]


def wait_ready(deadline,not_before=0,*,minimum_gib=2.0,available=monitor.free_gib,clock=time.monotonic,sleep=time.sleep):
    if not 640/1024<=minimum_gib<=4.0:raise ValueError('Start floor outside approved guarded range')
    stable=None
    while True:
        now=clock()
        if now>=deadline:raise TimeoutError('Finite audio-chain headroom deadline exhausted')
        if available()>=minimum_gib:
            if stable is None:stable=now
            if now-stable>=15 and now>=not_before:return
        else:stable=None
        sleep(min(1,deadline-now))


def resume_budget(root,path,now=None):
    """Carry the original finite deadline/retry usage across an idle restart."""
    path=path.resolve(strict=True)
    base=(root/'build-character-cast/parent-audio-chunks').resolve()
    if path.name!='report.json' or path.parent.parent!=base:raise ValueError('Unexpected previous controller receipt')
    raw=path.read_bytes();prior=json.loads(raw)
    if prior.get('status') not in ('waiting_for_headroom','waiting_for_quality_headroom'):
        raise ValueError('Only an idle controller can be resumed')
    maximum=prior.get('maximum_ram_stops')
    if prior.get('deadline_seconds')!=43200 or maximum not in (12,13):
        raise ValueError('Original finite safety budgets changed')
    if maximum==13:
        anchor=prior.get('recovery_from_exhausted',{})
        original=Path(anchor.get('receipt','')).resolve(strict=True)
        probe=Path(anchor.get('successful_probe','')).resolve(strict=True)
        if (original.name!='report.json' or original.parent.parent!=base or probe.name!='report.json'
                or probe.parent.parent!=(root/'build-character-cast/compact-voice').resolve()
                or hashlib.sha256(original.read_bytes()).hexdigest()!=anchor.get('sha256')
                or hashlib.sha256(probe.read_bytes()).hexdigest()!=anchor.get('sha256_probe')
                or anchor.get('additional_ram_stop_limit')!=1 or anchor.get('original_deadline_retained') is not True):
            raise ValueError('Recovery budget anchor changed')
        old=json.loads(original.read_text());passed=json.loads(probe.read_text())
        if (old.get('maximum_ram_stops')!=12 or old.get('ram_stops')!=12 or old.get('status')!='failed'
                or old.get('started_utc')!=prior.get('started_utc') or passed.get('status')!='passed' or passed.get('exit_code')!=0
                or prior.get('steps',[])[:len(old['steps'])]!=old['steps']):raise ValueError('Recovery history/deadline changed')
    stops=prior.get('ram_stops')
    if type(stops) is not int or not 0<=stops<maximum or not isinstance(prior.get('steps'),list):
        raise ValueError('Invalid/exhausted previous retry budget')
    if sum(isinstance(step,dict) and step.get('status')=='low_memory_cutoff' for step in prior['steps'])!=stops:
        raise ValueError('Previous retry count differs from its preserved attempts')
    began=datetime.fromisoformat(prior['started_utc'].replace('Z','+00:00')).timestamp()
    elapsed=(time.time() if now is None else now)-began
    if not 0<=elapsed<43200:raise ValueError('Original controller deadline expired or starts in the future')
    inherited={'started_utc':prior['started_utc'],'ram_stops':stops,'steps':prior['steps'],
               'resume_receipt':str(path),'resume_receipt_sha256':hashlib.sha256(raw).hexdigest()}
    if maximum==13:inherited.update(maximum_ram_stops=13,recovery_from_exhausted=prior['recovery_from_exhausted'])
    if 'required_generation_start_free_gib' in prior:
        floor=prior['required_generation_start_free_gib']
        if not isinstance(floor,(int,float)) or not 640/1024<=floor<=4:raise ValueError('Invalid previous measured start floor')
        inherited['required_generation_start_free_gib']=floor
    if 'reference_cache_profile' in prior:
        if not isinstance(prior['reference_cache_profile'],dict) or len(prior['reference_cache_profile'])!=2:
            raise ValueError('Invalid previous fixed-reference profile')
        inherited['reference_cache_profile']=prior['reference_cache_profile']
    if 'inference_page_release_mode' in prior:
        if prior['inference_page_release_mode'] is not True:raise ValueError('Invalid inference page-release mode')
        inherited['inference_page_release_mode']=True
    if 'flow_staging_profile' in prior:inherited['flow_staging_profile']=prior['flow_staging_profile']
    return inherited,43200-elapsed


def recover_after_success(root,path,probe,now=None):
    """One explicit recovery after a changed loader actually saves a batch.

    This is never an automatic retry of the exhausted run. Preserve its complete
    history and deadline, require a successful guarded speech/copy proof against
    current loading sources, and permit only one further RAM stop, then halt.
    """
    path=path.resolve(strict=True);probe=probe.resolve(strict=True)
    if path.name!='report.json' or path.parent.parent!=(root/'build-character-cast/parent-audio-chunks').resolve():
        raise ValueError('Unexpected exhausted receipt')
    raw=path.read_bytes();prior=json.loads(raw)
    if (prior.get('status')!='failed' or prior.get('maximum_ram_stops')!=12 or prior.get('ram_stops')!=12
            or prior.get('error')!='Finite RAM-stop budget exhausted; no further model started'
            or prior.get('deadline_seconds')!=43200 or prior.get('active_scope')!='full_mother'
            or prior.get('recovery_from_exhausted')):
        raise ValueError('Only the original exhausted speech chain can be recovered once')
    if sum(step.get('status')=='low_memory_cutoff' for step in prior['steps'])!=12:
        raise ValueError('Exhausted history differs from its retry count')
    began=datetime.fromisoformat(prior['started_utc'].replace('Z','+00:00')).timestamp()
    elapsed=(time.time() if now is None else now)-began
    if not 0<=elapsed<43200:raise ValueError('Original recovery deadline expired')
    if probe.name!='report.json' or probe.parent.parent!=(root/'build-character-cast/compact-voice').resolve():
        raise ValueError('Unexpected successful recovery probe')
    report=json.loads(probe.read_text(encoding='utf-8'))
    expected={'status':'passed','exit_code':0,'memory_measurement_available':True,'stop_free_mib':384,
              'aggregate_job_limit_mib':12288,'stage':'synthesis','manifest':'tools/full_mother_voice_manifest.json'}
    if any(report.get(k)!=v for k,v in expected.items()) or '--require-reference-cache' not in report.get('command',[]):
        raise ValueError('Recovery needs an actual successful guarded cached speech batch')
    proof=Path(report['bounded_checkpoint_copy_proof']).resolve(strict=True)
    if proof.parent!=(root/'build-character-cast/checkpoint-copy-proofs').resolve():raise ValueError('Unexpected loading proof')
    proof_raw=proof.read_bytes();detail=json.loads(proof_raw)
    if hashlib.sha256(proof_raw).hexdigest()!=report.get('bounded_checkpoint_copy_proof_sha256'):
        raise ValueError('Changed recovery loading proof')
    expected_proof={'status':'passed','original_load_state_dict':True,'weights_precision_unchanged':True,'all_values_verified':True}
    sources=detail.get('source_sha256',{})
    if any(detail.get(k)!=v for k,v in expected_proof.items()) or set(sources)!={'bounded_checkpoint_copy.py','generate_cached_voice.py','low_memory_voice_loader.py'}:
        raise ValueError('Incomplete recovery loading proof')
    for name,sha in sources.items():
        if hashlib.sha256((root/'tools'/name).read_bytes()).hexdigest()!=sha:raise ValueError('Recovery loading source changed')
    manifest=json.loads((root/'tools/full_mother_voice_manifest.json').read_text(encoding='utf-8'))
    current=generation_state(root,manifest);before=prior['coverage']['completed']
    if not 1<=current['completed']-before<=5:raise ValueError('Recovery probe did not preserve a bounded new speech batch')
    steps=list(prior['steps'])+[{'phase':'successful_recovery_probe','wrapper_report':str(probe),'status':'passed','exit_code':0,
                                'before':prior['coverage'],'after':current}]
    inherited={key:prior[key] for key in ('started_utc','reference_cache_profile','inference_page_release_mode')}
    inherited.update(steps=steps,ram_stops=12,maximum_ram_stops=13,
        required_generation_start_free_gib=max(2.75,float(report['start_free_gib'])),
        recovery_from_exhausted={'receipt':str(path),'sha256':hashlib.sha256(raw).hexdigest(),
                                'successful_probe':str(probe),'sha256_probe':hashlib.sha256(probe.read_bytes()).hexdigest(),
                                'additional_ram_stop_limit':1,'original_deadline_retained':True})
    return inherited,43200-elapsed


def cached_reference_profiles(root,paths):
    """Require both actual guarded preparation receipts before changing profile."""
    from fixed_reference_cache import build_context,cache_base,verify_artifact,digest
    allowed={'tools/full_mother_voice_manifest.json','tools/incidental_father_voice_manifest.json'}
    seen=set();profiles={}
    for path in paths:
        path=path.resolve(strict=True)
        if path.name!='report.json' or path.parent.parent!=(root/'build-character-cast/compact-voice').resolve():
            raise ValueError('Unexpected reference preparation receipt')
        report=json.loads(path.read_text(encoding='utf-8'));manifest=report.get('manifest')
        if manifest not in allowed or manifest in seen:raise ValueError('Missing/duplicate reference speaker preparation')
        expected={'status':'passed','exit_code':0,'stage':'reference_conditioning','reference_conditioning_only':True,
                  'certifies_speech_generation':False,'memory_measurement_available':True,'stop_free_mib':384}
        if any(report.get(k)!=v for k,v in expected.items()):raise ValueError('Original guarded reference preparation did not pass')
        data=json.loads((root/manifest).read_text(encoding='utf-8'))
        context=build_context(root,root/data['reference'],root/'.voice-tools/index-tts/checkpoints_2')
        if context['reference_sha256']!=data['reference_sha256']:raise ValueError('Manifest reference changed')
        proof_path=Path(report['reference_cache_proof']).resolve(strict=True)
        if not proof_path.is_relative_to(cache_base(root,context).resolve()):raise ValueError('Reference cache proof escaped its source context')
        _,proof=verify_artifact(proof_path,context)
        if digest(proof_path)!=report.get('reference_cache_proof_sha256') or proof['context_sha256']!=report.get('reference_cache_context_sha256'):
            raise ValueError('Reference preparation proof changed')
        profiles[context['reference_sha256']]={'context_sha256':proof['context_sha256'],'payload_sha256':proof['payload_sha256'],
             'proof':str(proof_path),'proof_sha256':digest(proof_path),'preparation_report':str(path),'preparation_report_sha256':digest(path)}
        seen.add(manifest)
    if seen!=allowed or len(profiles)!=2:raise ValueError('Both fixed parent references must be prepared')
    return profiles


def generation_start_floor(configured,inherited,verified_profile,inference_pages=False,flow_profile=None):
    previous=max(configured,inherited.get('required_generation_start_free_gib',0))
    if inherited.get('inference_page_release_mode') and not inference_pages:
        raise ValueError('Inference page-release continuation cannot revert its memory mode')
    if inherited.get('flow_staging_profile') and not flow_profile:raise ValueError('Staged flow continuation cannot revert to resident flow weights')
    if flow_profile:
        if not verified_profile:raise ValueError('Staged flow requires its verified reference')
        if not inherited.get('flow_staging_profile'):return configured
    if inference_pages:
        if not verified_profile:raise ValueError('Inference page-release requires verified cached speech')
        # Explicit one-time trial of the own-process inference paging scope.
        # The original finite deadline/retry count and emergency cutoff remain.
        # Restarting or changing code again cannot repeat this floor transition.
        if not inherited.get('inference_page_release_mode'):return configured
    # This one-time transition removes the full reference encoder from speech
    # worker construction. Re-preparing a cache/restarting never resets its floor.
    return configured if verified_profile and not inherited.get('reference_cache_profile') else previous


def flow_staging_profile(root,path):
    from staged_voice_flow import verify_proof
    path=path.resolve(strict=True)
    if path.name!='report.json' or path.parent.parent!=(root/'build-character-cast/compact-voice').resolve():raise ValueError('Unexpected flow component receipt')
    report=json.loads(path.read_text(encoding='utf-8'))
    expected={'status':'passed','exit_code':0,'stage':'flow_staging_component','certifies_speech_generation':False,
              'memory_measurement_available':True,'stop_free_mib':384,'aggregate_job_limit_mib':12288}
    if any(report.get(k)!=v for k,v in expected.items()):raise ValueError('Original/staged flow equivalence did not pass its guard')
    proof=Path(report['flow_staging_proof']);_,sha=verify_proof(root,proof,True)
    if sha!=report.get('flow_staging_proof_sha256'):raise ValueError('Flow component proof changed')
    return {'component_receipt':str(path),'component_receipt_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
            'proof':str(proof),'proof_sha256':sha,'mode':'original mapped CPU weights; unchanged verified GPU phase'}


def adopt_cached_attempts(root,inherited,paths):
    """Include manual guarded probes without resetting/omitting RAM-stop usage."""
    result=dict(inherited);steps=list(inherited.get('steps',[]));stops=inherited.get('ram_stops',0)
    seen={str(Path(step['wrapper_report']).resolve()) for step in steps if 'wrapper_report' in step}
    for path in paths:
        path=path.resolve(strict=True)
        if path.name!='report.json' or path.parent.parent!=(root/'build-character-cast/compact-voice').resolve():
            raise ValueError('Unexpected manual cached probe receipt')
        if str(path) in seen:raise ValueError('Manual attempt already counted')
        report=json.loads(path.read_text(encoding='utf-8'))
        if (report.get('stage')!='synthesis' or '--require-reference-cache' not in report.get('command',[])
                or report.get('manifest') not in ('tools/full_mother_voice_manifest.json','tools/incidental_father_voice_manifest.json','tools/incidental_mother_voice_manifest.json')
                or report.get('memory_measurement_available') is not True or report.get('stop_free_mib')!=384
                or report.get('aggregate_job_limit_mib')!=12288):
            raise ValueError('Manual probe does not retain original guards/reference mode')
        if may_resume_ram(report):stops+=1
        elif report.get('status')=='passed' and report.get('exit_code')==0 and report.get('bounded_checkpoint_copy_proof'):
            import fixed_reference_cache as cache
            proof=Path(report['bounded_checkpoint_copy_proof']).resolve(strict=True)
            if proof.parent!=(root/'build-character-cast/checkpoint-copy-proofs').resolve() or cache.digest(proof)!=report.get('bounded_checkpoint_copy_proof_sha256'):
                raise ValueError('Manual checkpoint-copy proof changed')
            if any(str(value).endswith('generate_staged_voice.py') for value in report['command']):
                from staged_voice_flow import verify_proof
                _,sha=verify_proof(root,Path(report['flow_staging_proof']))
                if sha!=report.get('flow_staging_proof_sha256'):raise ValueError('Manual staged flow proof changed')
            generation_state(root,json.loads((root/report['manifest']).read_text(encoding='utf-8')))
        else:raise ValueError('Manual non-RAM/unverified failure cannot be retried automatically')
        steps.append({'phase':'manual_cached_speech_probe','wrapper_report':str(path),'status':report['status'],'exit_code':report['exit_code']})
        seen.add(str(path))
    if stops>=inherited.get('maximum_ram_stops',12):raise ValueError('Original finite RAM-stop budget exhausted')
    result.update(steps=steps,ram_stops=stops)
    return result


def recovery_headroom(current,attempt,made_progress):
    """Every RAM stop requires more headroom, even if a take was preserved."""
    start=float(attempt['start_free_gib'])
    if not math.isfinite(start) or start<640/1024:raise ValueError('Invalid measured worker start memory')
    return min(4.0,max(current,start)+.5)


def may_resume_ram(report):
    return (report.get('status')=='low_memory_cutoff' and report.get('memory_measurement_available') is True
            and report.get('exit_code')!=0 and report.get('stop_free_mib')==384)


def read_attempt(log,manifest):
    if log.stat().st_size>1024*1024:raise ValueError('Unexpected wrapper log size')
    first=log.read_text(encoding='utf-8',errors='replace').splitlines()[0]
    prefix='Compact voice attempt: '
    if not first.startswith(prefix):raise ValueError('Missing owned wrapper report; no automatic retry')
    path=Path(first[len(prefix):]).resolve(strict=True)
    if path.name!='report.json' or path.parent.parent!=(ROOT/'build-character-cast/compact-voice').resolve():
        raise ValueError('Unexpected owned wrapper report path')
    report=json.loads(path.read_text(encoding='utf-8'))
    if report.get('manifest')!=manifest:raise ValueError('Voice attempt manifest changed')
    return report,path


@contextlib.contextmanager
def exclusive_chain():
    import msvcrt
    lock=ROOT/'build-character-cast/parent-audio-chunks.lock'
    fd=os.open(lock,os.O_CREAT|os.O_RDWR)
    try:
        if os.fstat(fd).st_size==0:os.write(fd,b'0')
        os.lseek(fd,0,os.SEEK_SET)
        msvcrt.locking(fd,msvcrt.LK_NBLCK,1)
        yield
    finally:os.close(fd) # Windows releases this exact byte lock even after a crash.


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--low-memory-attempt',action='store_true',required=True)
    parser.add_argument('--start-free-mib',type=int,choices=[640,1024,1536,2048,2560,3072,4096],default=2048)
    parser.add_argument('--batch-new-takes',type=int,choices=[1,5,10],default=10)
    parser.add_argument('--resume-chain',type=Path)
    parser.add_argument('--reference-cache-reports',nargs=2,type=Path)
    parser.add_argument('--additional-attempt-reports',nargs='+',type=Path)
    parser.add_argument('--inference-page-release',action='store_true')
    parser.add_argument('--recover-exhausted-chain',type=Path)
    parser.add_argument('--successful-recovery-probe',type=Path)
    parser.add_argument('--flow-staging-report',type=Path)
    args=parser.parse_args()
    if bool(args.recover_exhausted_chain)!=bool(args.successful_recovery_probe) or args.recover_exhausted_chain and (args.resume_chain or args.additional_attempt_reports):
        raise ValueError('Explicit recovery requires its passed probe and cannot reset another continuation')
    inherited,remaining=(recover_after_success(ROOT,args.recover_exhausted_chain,args.successful_recovery_probe)
                         if args.recover_exhausted_chain else resume_budget(ROOT,args.resume_chain) if args.resume_chain else ({},43200))
    deadline=time.monotonic()+remaining
    if args.additional_attempt_reports:
        if not args.resume_chain:raise ValueError('Manual probes require the original controller checkpoint')
        inherited=adopt_cached_attempts(ROOT,inherited,args.additional_attempt_reports)
    profiles=cached_reference_profiles(ROOT,args.reference_cache_reports) if args.reference_cache_reports else None
    flow_profile=flow_staging_profile(ROOT,args.flow_staging_report) if args.flow_staging_report else None
    if inherited.get('reference_cache_profile') and not profiles:raise ValueError('Cached-reference continuation cannot silently fall back to cold loading')
    folder=ROOT/'build-character-cast/parent-audio-chunks'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())
    folder.mkdir(parents=True,exist_ok=False)
    result={'status':'starting','controller_pid':os.getpid(),'started_utc':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),'steps':[],'batch_new_takes':args.batch_new_takes,'start_free_gib':args.start_free_mib/1024,
            'stable_headroom_seconds':15,'minimum_between_models_seconds':30,
            'deadline_seconds':43200,'maximum_ram_stops':12,'ram_stops':0,
            'quality_start_free_gib':2.0,'packaging':False,'human_listening':False,
            'controller_started_utc':time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime()),**inherited}
    generation_floor=generation_start_floor(args.start_free_mib/1024,inherited,profiles,args.inference_page_release,flow_profile)
    if flow_profile:
        result['flow_staging_profile']=flow_profile
        if not inherited.get('flow_staging_profile'):
            result['previous_resident_flow_required_start_free_gib']=inherited.get('required_generation_start_free_gib')
    if args.inference_page_release:
        result['inference_page_release_mode']=True
        if not inherited.get('inference_page_release_mode'):
            result['previous_inference_profile_required_generation_start_free_gib']=inherited.get('required_generation_start_free_gib')
            result['inference_profile_change']='Explicit low-memory trial: release only own clean pages throughout unchanged inference; memory benefit not yet certified'
    if profiles:
        result['reference_cache_profile']=profiles
        if not inherited.get('reference_cache_profile'):
            result['previous_loading_profile_required_generation_start_free_gib']=inherited.get('required_generation_start_free_gib')
            result['loading_profile_change']='Verified original fixed-reference tensors; full reference encoder omitted from speech worker'
    result['required_generation_start_free_gib']=generation_floor
    report_path=folder/'report.json'
    def save():
        stage=report_path.with_suffix('.tmp');stage.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');stage.replace(report_path)
    save();print('Chunked parent audio report: '+str(report_path),flush=True)
    environment=dict(os.environ)
    environment['PATH']=str(ROOT/'.voice-tools/index-tts/.venv/Scripts')+';C:/ffmpeg;'+environment['PATH']
    api,handle=job(12288) # KILL_ON_CLOSE for this controller's wrappers/descendants only.
    def launch(name,command):
        log=folder/(f"{len(result['steps']):03d}_"+name+'.log')
        with log.open('xb') as stream:
            child=subprocess.Popen(command,cwd=ROOT,env=environment,stdout=stream,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):
                child.kill();child.wait();raise RuntimeError('Cannot bind exact owned audio wrapper')
            code=child.wait(timeout=max(1,min(28920,deadline-time.monotonic())))
        return code,log
    cooldown=time.monotonic()+30 if args.resume_chain else 0
    try:
        with exclusive_chain():
            for who,scope,manifest_name in [('mother','full','tools/full_mother_voice_manifest.json'),
                                            ('father','incidental','tools/incidental_father_voice_manifest.json'),
                                            ('mother','incidental','tools/incidental_mother_voice_manifest.json')]:
                manifest=json.loads((ROOT/manifest_name).read_text(encoding='utf-8'))
                common=[sys.executable,'tools/run_compact_voice_batch.py','--speaker',who,
                        '--full-dialogue' if scope=='full' else '--incidental-dialogue',
                        '--low-memory-attempt','--timeout-seconds','28800']
                while True:
                    before=generation_state(ROOT,manifest)
                    if before['generation_complete']:break
                    result.update(status='waiting_for_headroom',active_scope=scope+'_'+who,coverage=before);save()
                    wait_ready(deadline,cooldown,minimum_gib=generation_floor)
                    result['status']='generating';save()
                    code,log=launch(scope+'_'+who+'_generation',common+['--memory-efficient','--max-new-takes',str(args.batch_new_takes)]+(['--require-reference-cache'] if profiles else [])+(['--stage-flow-model'] if flow_profile else []))
                    attempt,path=read_attempt(log,manifest_name)
                    after=generation_state(ROOT,manifest) # Refuse any orphan/changed master, even after RAM stop.
                    result['steps'].append({'phase':'generation','scope':scope,'speaker':who,'wrapper_report':str(path),
                                            'exit_code':code,'before':before,'after':after,'status':attempt.get('status')});save()
                    cooldown=time.monotonic()+30
                    if code==0 and attempt.get('status')=='passed' and attempt.get('memory_measurement_available') is True:
                        if not 0<after['completed']-before['completed']<=args.batch_new_takes:raise ValueError('Successful bounded batch made unexpected progress')
                    elif may_resume_ram(attempt):
                        result['ram_stops']+=1;save()
                        if result['ram_stops']>=result['maximum_ram_stops']:raise RuntimeError('Finite RAM-stop budget exhausted; no further model started')
                        progressed=after['completed']>before['completed']
                        if not progressed and generation_floor>=4:raise RuntimeError('No progress at the normal4GiB floor; no identical retry')
                        generation_floor=recovery_headroom(generation_floor,attempt,progressed)
                        result['required_generation_start_free_gib']=generation_floor;save()
                    else:raise RuntimeError('Non-RAM generation failure; no automatic retry')
                    print(f"{scope} {who}: {after['completed']}/{after['expected']} source-matched masters; model released.",flush=True)
                while True:
                    result.update(status='waiting_for_quality_headroom',active_scope=scope+'_'+who);save()
                    wait_ready(deadline,cooldown)
                    result['status']='validating_full_scope';save()
                    code,log=launch(scope+'_'+who+'_quality',common+['--validate','--asr-turbo','--resume-checks'])
                    attempt,path=read_attempt(log,manifest_name)
                    result['steps'].append({'phase':'quality','scope':scope,'speaker':who,'wrapper_report':str(path),
                                            'exit_code':code,'status':attempt.get('status')});save()
                    cooldown=time.monotonic()+30
                    if code==0 and attempt.get('status')=='passed' and attempt.get('memory_measurement_available') is True:break
                    if not may_resume_ram(attempt):raise RuntimeError('Quality/non-RAM failure; recordings preserved for review, no automatic retry')
                    result['ram_stops']+=1;save()
                    if result['ram_stops']>=result['maximum_ram_stops']:raise RuntimeError('Finite RAM-stop budget exhausted')
            result.update(status='auditing_all_masters',active_scope=None);save()
            audit=folder/'all-master-proofs.json'
            code,log=launch('all_master_proofs',[sys.executable,'tools/audit_voice_master_proofs.py','--report',str(audit)])
            proof=json.loads(audit.read_text(encoding='utf-8')) if audit.exists() else {}
            if code or (proof.get('status'),proof.get('passed'),proof.get('failed'))!=('passed',946,0):
                raise RuntimeError('All946exact-script/reference/quality proofs are required')
            result.update(status='passed',master_proofs=946);save()
            print('All946parent voice proofs pass. Packaging and final audits still remain; no JAR built by this helper.',flush=True)
    except BaseException as error:
        result.update(status='failed',error=str(error));save();raise
    finally:api.CloseHandle(handle)


if __name__=='__main__':main()
