"""Attempt local sequential synthesis with a bounded, monitored Windows job.

The user may request an attempt below the usual 4 GiB preflight. That does not
disable the 384 MiB emergency stop, finite timeout or per-job commit cap. Only
this invocation's child process tree can be stopped; other applications stay open.
"""
import argparse
import ctypes
import hashlib
import json
import os
import re
import sys
import subprocess
import threading
import time
from ctypes import wintypes
from pathlib import Path
import check_numbered_candidate as monitor
from run_low_memory_release import job
from visit_event_audio_catalog import EVENT_MANIFESTS
from owned_job_memory import measure as measure_owned_job
if hasattr(sys.stdout,'reconfigure'):sys.stdout.reconfigure(encoding='utf-8',errors='backslashreplace')

ROOT = Path(__file__).resolve().parents[1]


def proof_markers(log,prefix):
    found=[]
    with log.open(encoding='utf-8',errors='replace') as stream:
        for line in stream:
            if line.startswith(prefix):found.append(line[len(prefix):].strip())
            if len(found)>1:raise ValueError('Duplicate proof marker')
    return found


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--speaker', choices=['mother', 'father'], required=True)
    parser.add_argument('--state', help='One exact compact-manifest state, for a bounded first take.')
    parser.add_argument('--validate', action='store_true')
    parser.add_argument('--low-memory-attempt', action='store_true')
    parser.add_argument('--memory-efficient', action='store_true')
    parser.add_argument('--prepare-reference-cache',action='store_true')
    parser.add_argument('--require-reference-cache',action='store_true')
    parser.add_argument('--stage-flow-model',action='store_true')
    parser.add_argument('--flow-staging-check',action='store_true')
    parser.add_argument('--stage-vocoder',action='store_true')
    parser.add_argument('--vocoder-staging-check',action='store_true')
    parser.add_argument('--cuda-async-allocator',action='store_true')
    parser.add_argument('--page-scope-probe',action='store_true',help='Detached same-seed own-page-trimming equivalence; never changes catalogue masters')
    parser.add_argument('--retire-reference-modules',action='store_true',help='After verified reference restore, release unused codec encoder/decoder and reference speaker encoder; original GPU speech quantizer retained')
    parser.add_argument('--reference-retirement-probe',action='store_true',help='Detached exact-audio check of reference-only retirement, separate from catalogue acceptance')
    parser.add_argument('--early-reference-retirement',action='store_true',help='Prototype: release unused reference modules after their original checkpoint loads, before vocoder construction; requires separate exact-audio and genuine-take acceptance')
    parser.add_argument('--early-reference-probe',action='store_true',help='Detached unchanged-settings PCM comparison for earlier reference retirement')
    parser.add_argument('--early-reference-admission',type=Path,help='Actual passed exact-PCM component receipt for measured genuine early-retirement admission')
    parser.add_argument('--scope-quality-pages',action='store_true',help='Keep only own-process page management active during original ASR/speaker inference; no model/quality changes')
    parser.add_argument('--quality-page-scope-probe',action='store_true',help='Detached original transcription/speaker equivalence check before full scoped validation')
    parser.add_argument('--quality-scope-admission',type=Path,help='Actual passed original-quality comparison for full scoped validation')
    parser.add_argument('--full-dialogue',action='store_true',help='Preserve all authored words rather than the archived compact alternate-line plan.')
    parser.add_argument('--incidental-dialogue',action='store_true',help='Use the separate exact-script incidental parent catalogue; never changes an active full-dialogue manifest.')
    parser.add_argument('--max-new-takes',type=int,choices=[1,5,10,20],help='Generation-only upper bound; memory-efficient bounded workers retire after at most five. Partial reports cannot certify the full manifest.')
    parser.add_argument('--manifest',choices=EVENT_MANIFESTS,help='Validate a retained authored manifest; never synthesizes alternate duplicate masters.')
    parser.add_argument('--asr-turbo',action='store_true')
    parser.add_argument('--existing-only',action='store_true')
    parser.add_argument('--resume-checks',action='store_true')
    parser.add_argument('--wait-for-files',action='store_true',help='Validate each checksum-confirmed take as the sequential generator publishes it.')
    parser.add_argument('--timeout-seconds',type=int,choices=[1800,3600,7200,28800],default=1800,
                        help='Finite batch deadline; does not change RAM, commit or log safety limits.')
    args = parser.parse_args()
    if args.scope_quality_pages and (not args.validate or not args.asr_turbo or not args.low_memory_attempt or args.wait_for_files):raise RuntimeError('Quality page management requires the original low-memory pinned validation path')
    if args.quality_page_scope_probe and (not args.scope_quality_pages or not args.incidental_dialogue or args.speaker!='mother' or args.state!='mother_53b786200560bb87' or args.resume_checks or args.existing_only):raise RuntimeError('Quality component requires one known original state without reusing results')
    if args.quality_scope_admission and (not args.scope_quality_pages or args.quality_page_scope_probe):raise RuntimeError('Quality scope admission is full-validation only')
    if args.scope_quality_pages and not args.quality_page_scope_probe and not args.quality_scope_admission:raise RuntimeError('Full scoped quality requires its actual unchanged-result component')
    if args.early_reference_retirement and (not args.require_reference_cache or not args.stage_flow_model or not args.stage_vocoder or not args.cuda_async_allocator or not args.memory_efficient or args.validate or args.prepare_reference_cache or args.retire_reference_modules or args.page_scope_probe or args.reference_retirement_probe):raise RuntimeError('Early reference retirement requires its separate original staged cached speech profile')
    if args.early_reference_probe and (not args.early_reference_retirement or not args.full_dialogue or args.speaker!='mother' or args.state!='other_mother_events_wybie_adventure_caught_normal_low_high_01' or args.max_new_takes):raise RuntimeError('Early reference component requires its exact detached original Mother baseline')
    if args.early_reference_admission and (not args.early_reference_retirement or args.early_reference_probe):raise RuntimeError('Measured early admission is genuine-generation only')
    if args.early_reference_retirement and not args.early_reference_probe and not args.early_reference_admission:raise RuntimeError('Genuine early retirement requires its actual exact-PCM component admission')
    if args.retire_reference_modules and (not args.require_reference_cache or not args.stage_vocoder or not args.cuda_async_allocator or args.validate or args.prepare_reference_cache or args.page_scope_probe):raise RuntimeError('Reference retirement requires the original staged cached-reference speech profile')
    if args.reference_retirement_probe and (not args.retire_reference_modules or not args.full_dialogue or args.speaker!='mother' or args.state!='other_mother_events_wybie_adventure_caught_normal_low_high_01' or args.max_new_takes):raise RuntimeError('Reference-retirement component requires its exact detached baseline state')
    if args.page_scope_probe and (not args.stage_vocoder or not args.full_dialogue or args.speaker!='mother' or args.state!='other_mother_events_wybie_adventure_caught_normal_low_high_01' or args.max_new_takes or args.validate):raise RuntimeError('Detached page-scope probe requires its exact full Mother baseline and staged speech')
    if args.stage_vocoder and (not args.stage_flow_model or args.vocoder_staging_check):raise RuntimeError('Vocoder staging requires verified staged flow speech')
    if args.vocoder_staging_check and (not args.memory_efficient or not args.cuda_async_allocator or args.stage_flow_model or args.flow_staging_check or args.validate or args.prepare_reference_cache or args.require_reference_cache or args.max_new_takes or args.state):raise RuntimeError('Vocoder component check is separate from speech/reference preparation')
    if args.stage_flow_model and (not args.require_reference_cache or args.validate or args.prepare_reference_cache or args.flow_staging_check):
        raise RuntimeError('Staged flow speech requires verified cached-reference generation')
    if args.flow_staging_check and (not args.memory_efficient or args.validate or args.prepare_reference_cache or args.require_reference_cache or args.max_new_takes or args.state):
        raise RuntimeError('Flow component check is separate from speech/reference preparation')
    if args.cuda_async_allocator and not (args.stage_flow_model or args.flow_staging_check or args.vocoder_staging_check):raise RuntimeError('Async allocator trial requires verified staged component mode')
    if (args.prepare_reference_cache or args.require_reference_cache) and (args.validate or not args.memory_efficient):
        raise RuntimeError('Reference caching is separate from quality checking and requires memory-efficient loading')
    if args.prepare_reference_cache and (args.require_reference_cache or args.max_new_takes or args.state):
        raise RuntimeError('Reference preparation does not generate/select speech takes')
    if args.max_new_takes and (args.validate or args.state or not (args.full_dialogue or args.incidental_dialogue)):
        raise RuntimeError('Bounded generation requires a complete full/incidentals manifest without state/validation selection')
    if args.incidental_dialogue and (args.full_dialogue or args.manifest):raise RuntimeError('Incidental dialogue has its own manifest, separate from event/retained batches')
    if args.resume_checks and not (args.validate and args.asr_turbo):raise RuntimeError('Resumable quality checks require pinned Turbo validation')
    if args.manifest and (not args.validate or args.full_dialogue):raise RuntimeError('Retained manifests are validation-only, separate from the full batch')
    if (args.asr_turbo or args.existing_only or args.wait_for_files) and not args.validate:raise RuntimeError('ASR/retained-master options are validation-only')
    if args.asr_turbo and args.wait_for_files:raise RuntimeError('Streaming validation uses CPU ASR; the synthesis GPU remains exclusive')
    free = monitor.free_gib()
    floor = monitor.START_FREE_GIB if args.low_memory_attempt else 4.0
    if args.retire_reference_modules:floor=max(floor,2.75)
    early_admission=None
    quality_admission=None
    if args.scope_quality_pages:
        if args.quality_page_scope_probe:floor=max(floor,2.5)
        else:
            from run_scoped_quality_admission import admit as admit_quality,measured_floor as quality_floor
            quality_admission,component=admit_quality(args.quality_scope_admission)
            floor=max(floor,quality_floor(component))
    if args.early_reference_retirement:
        if args.early_reference_probe:floor=max(floor,3.25)
        else:
            from run_early_reference_genuine_probe import admit,measured_floor
            early_admission,component=admit(args.early_reference_admission)
            floor=max(floor,measured_floor(component))
    free=monitor.free_gib()
    if free < floor:
        raise RuntimeError(f'Only {free:.2f} GiB free; below {floor:.2f} GiB start floor. No model started.')
    manifest_path = args.manifest or f'tools/{"incidental" if args.incidental_dialogue else "full" if args.full_dialogue else "compact"}_{args.speaker}_voice_manifest.json'
    manifest = json.loads((ROOT/manifest_path).read_text(encoding='utf-8'))
    if ('otherfather' in manifest['reference'])!=(args.speaker=='father'):raise RuntimeError('Manifest speaker does not match requested reference')
    if args.state and args.state not in manifest['states']:
        raise ValueError('Unknown compact state')
    command = [str(ROOT/'.voice-tools/index-tts/.venv/Scripts/python.exe'), '-u']
    command += (['tools/check_staged_vocoder.py'] if args.vocoder_staging_check else ['tools/check_staged_flow.py'] if args.flow_staging_check else
                ['tools/check_scoped_quality.py' if args.scope_quality_pages else 'tools/check_welcome_audio.py', '--transcribe', '--speaker-check'] if args.validate else
                ['tools/check_early_reference_voice.py' if args.early_reference_probe else 'tools/generate_early_reference_voice.py' if args.early_reference_retirement else 'tools/check_retired_reference_voice.py' if args.reference_retirement_probe else 'tools/generate_retired_reference_voice.py' if args.retire_reference_modules else 'tools/generate_page_scope_probe.py' if args.page_scope_probe else 'tools/generate_staged_vocoder_voice.py' if args.stage_vocoder else 'tools/generate_staged_voice.py' if args.stage_flow_model else 'tools/generate_cached_voice.py' if args.require_reference_cache else 'tools/generate_other_mother_welcome.py'])
    if args.asr_turbo:command.append('--asr-turbo')
    if args.asr_turbo and args.low_memory_attempt:command.append('--memory-efficient-loading')
    if args.existing_only:command.append('--existing-only')
    if args.resume_checks:command.append('--resume-checks')
    if args.wait_for_files:command.append('--wait-for-files')
    if args.memory_efficient and not args.validate:
        command += ['--memory-efficient']
    if args.prepare_reference_cache:command.append('--prepare-reference-cache')
    if args.require_reference_cache:command.append('--require-reference-cache')
    if args.max_new_takes:
        from voice_batch_scope import effective_take_limit
        command+=['--max-new-takes',str(effective_take_limit(args.max_new_takes,args.memory_efficient))]
    if args.state:
        command += ['--state', args.state]
    environment = dict(os.environ, CORALINE_VOICE_MANIFEST=manifest_path,
                       HF_HUB_OFFLINE='1', TRANSFORMERS_OFFLINE='1', TOKENIZERS_PARALLELISM='false')
    if args.cuda_async_allocator:environment['PYTORCH_CUDA_ALLOC_CONF']='backend:cudaMallocAsync'
    environment['PYTHONFAULTHANDLER']='1'
    environment['PATH'] = str(ROOT/'.voice-tools/index-tts/.venv/Scripts')+';C:/ffmpeg;'+environment['PATH']
    directory = ROOT/'build-character-cast/compact-voice'/time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    directory.mkdir(parents=True, exist_ok=False)
    if args.page_scope_probe:
        environment['CORALINE_PAGE_SCOPE_PROBE_DIR']=str(ROOT/'build-character-cast/page-scope-probes'/directory.name)
    if args.reference_retirement_probe:environment['CORALINE_REFERENCE_RETIREMENT_PROBE_DIR']=str(ROOT/'build-character-cast/reference-retirement-checks'/directory.name)
    if args.early_reference_probe:environment['CORALINE_EARLY_REFERENCE_PROBE_DIR']=str(ROOT/'build-character-cast/early-reference-checks'/directory.name)
    if args.quality_page_scope_probe:environment['CORALINE_QUALITY_SCOPE_PROBE_DIR']=str(ROOT/'build-character-cast/quality-scope-checks'/directory.name)
    log = directory/'output.log'
    # CUDA allocations also reserve Windows process commit. This is a commit
    # ceiling, not permission to consume that much physical RAM; the unchanged
    # 384 MiB available-physical-memory emergency stop still applies.
    commit_limit=8192 if args.prepare_reference_cache else 12288 if args.memory_efficient else 8192 if args.asr_turbo else 6144
    report = dict(command=command, manifest=manifest_path, start_free_gib=free, timeout_seconds=args.timeout_seconds,
                  aggregate_job_limit_mib=commit_limit, stop_free_mib=384,
                  stage='vocoder_staging_component' if args.vocoder_staging_check else 'flow_staging_component' if args.flow_staging_check else 'reference_conditioning' if args.prepare_reference_cache else 'validation' if args.validate else 'synthesis')
    if args.prepare_reference_cache:report.update(reference_conditioning_only=True,certifies_speech_generation=False)
    if args.flow_staging_check:report.update(certifies_speech_generation=False)
    if args.vocoder_staging_check:report.update(certifies_speech_generation=False)
    if args.page_scope_probe:report.update(stage='page_scope_probe',certifies_speech_generation=False,certifies_catalogue=False)
    if args.retire_reference_modules:report.update(minimum_start_free_gib=floor,reference_only_retirement=True)
    if args.reference_retirement_probe:report.update(stage='reference_retirement_component',certifies_speech_generation=False,certifies_catalogue=False)
    if args.early_reference_retirement:report.update(minimum_start_free_gib=floor,early_reference_retirement=True)
    if early_admission is not None:report.update(early_component_admission=str(early_admission),early_component_admission_sha256=hashlib.sha256(early_admission.read_bytes()).hexdigest())
    if args.early_reference_probe:report.update(stage='early_reference_component',certifies_speech_generation=False,certifies_catalogue=False)
    if args.scope_quality_pages:report.update(quality_own_page_scope=True,minimum_start_free_gib=floor)
    if args.quality_page_scope_probe:report.update(stage='quality_page_scope_component',certifies_complete_quality=False)
    if quality_admission is not None:report.update(quality_component_admission=str(quality_admission),quality_component_admission_sha256=hashlib.sha256(quality_admission.read_bytes()).hexdigest())
    if args.cuda_async_allocator:report.update(requested_cuda_allocator='cudaMallocAsync',allocator_scope='owned_child_environment_only')
    api, handle = job(commit_limit)
    print('Compact voice attempt: '+str(directory/'report.json'), flush=True)
    try:
        from voice_verification_pause import wait_if_requested
        pause_proof=wait_if_requested(ROOT)
        if pause_proof is not None:
            report['verification_pause_proof']=str(pause_proof)
            floor=max(floor,json.loads(pause_proof.read_text())['minimum_resume_free_gib'])
            free=monitor.free_gib()
            if free<floor:raise RuntimeError('Memory below unchanged start floor after verification; no model started')
            report['start_free_gib']=free
        with log.open('xb') as output:
            child = subprocess.Popen(command, cwd=ROOT, env=environment, stdout=subprocess.PIPE,
                                     stderr=subprocess.STDOUT, bufsize=0)
            if not api.AssignProcessToJobObject(handle, wintypes.HANDLE(int(child._handle))):
                error = ctypes.get_last_error()
                child.kill(); child.wait()
                raise ctypes.WinError(error)
            reached, errors = threading.Event(), []
            reader = threading.Thread(target=monitor.drain_bounded,
                args=(child.stdout, output, monitor.MAX_LOG_BYTES, reached, errors), daemon=True)
            reader.start()
            sampled=[];began=time.monotonic()
            def owned_memory(_child):
                memory=measure_owned_job(api,handle,monitor.process_memory)
                if not sampled or time.monotonic()-began-sampled[-1]['elapsed_seconds']>=15:
                    if len(sampled)<2000:
                        sampled.append(dict(elapsed_seconds=round(time.monotonic()-began,2),
                            available_mib=round(monitor.free_gib()*1024,1),**memory))
                return memory
            report.update(monitor.watch_child(child, args.timeout_seconds, monitor.free_gib, owned_memory, log_limit=reached))
            report.update(memory_measurement_scope='aggregate_owned_job_members',owned_job_memory_samples=sampled,
                          peak_working_set_note='Sum of per-member peaks; an upper bound, not a simultaneous peak.')
            api.CloseHandle(handle); handle = None
            reader.join(timeout=10)
            child.stdout.close()
            if reader.is_alive() or errors or reached.is_set():
                report.update(status='output_capture_failed', errors=errors)
            if (args.stage_vocoder or args.vocoder_staging_check) and report.get('status')=='passed':
                try:
                    from staged_voice_vocoder import verify_proof as verify_vocoder
                    prefix='Verified staged vocoder equivalence: ' if args.vocoder_staging_check else 'Verified staged vocoder weights: '
                    found=proof_markers(log,prefix)
                    if len(found)!=1:raise ValueError('Missing unique staged vocoder proof')
                    path=Path(found[0]);detail,sha=verify_vocoder(ROOT,path,args.vocoder_staging_check)
                    if args.cuda_async_allocator and detail.get('allocator_backend')!='cudaMallocAsync':raise ValueError('Requested async allocator was not active')
                    report.update(vocoder_staging_proof=str(path),vocoder_staging_proof_sha256=sha)
                except (OSError,ValueError,KeyError) as error:report.update(status='vocoder_staging_postflight_failed',error=str(error))
            if (args.stage_flow_model or args.flow_staging_check) and report.get('status')=='passed':
                try:
                    from staged_voice_flow import verify_proof
                    prefix='Verified staged flow equivalence: ' if args.flow_staging_check else 'Verified staged flow weights: '
                    found=proof_markers(log,prefix)
                    if len(found)!=1:raise ValueError('Missing unique staged flow proof')
                    path=Path(found[0].strip())
                    detail,sha=verify_proof(ROOT,path,args.flow_staging_check)
                    if args.cuda_async_allocator and detail.get('allocator_backend')!='cudaMallocAsync':raise ValueError('Requested async allocator was not active')
                    report.update(flow_staging_proof=str(path),flow_staging_proof_sha256=sha)
                except (OSError,ValueError,KeyError) as error:report.update(status='flow_staging_postflight_failed',error=str(error))
            if args.require_reference_cache and report.get('status')=='passed':
                try:
                    found=proof_markers(log,'Verified bounded checkpoint copies: ')
                    if len(found)!=1:raise ValueError('Missing unique checkpoint copy proof')
                    proof_path=Path(found[0].strip()).resolve(strict=True)
                    if proof_path.suffix!='.json' or proof_path.parent!=(ROOT/'build-character-cast/checkpoint-copy-proofs').resolve():
                        raise ValueError('Checkpoint copy proof escaped owned reports')
                    proof=json.loads(proof_path.read_text(encoding='utf-8'))
                    if (proof.get('status')!='passed' or proof.get('original_load_state_dict') is not True
                            or proof.get('weights_precision_unchanged') is not True or proof.get('all_values_verified') is not True
                            or proof.get('chunks',0)<=0 or proof.get('verified_target_bytes',0)<=0 or proof.get('chunk_limit_bytes')!=2*1024*1024):
                        raise ValueError('Checkpoint copy was not verified')
                    if set(proof.get('source_sha256',{}))!={'bounded_checkpoint_copy.py','generate_cached_voice.py','low_memory_voice_loader.py'}:
                        raise ValueError('Incomplete checkpoint copy source proof')
                    for name,sha in proof['source_sha256'].items():
                        if name not in ('bounded_checkpoint_copy.py','generate_cached_voice.py','low_memory_voice_loader.py'):
                            raise ValueError('Unknown checkpoint-copy source')
                        if hashlib.sha256((ROOT/'tools'/name).read_bytes()).hexdigest()!=sha:raise ValueError('Checkpoint copy source changed')
                    report.update(bounded_checkpoint_copy_proof=str(proof_path),bounded_checkpoint_copy_proof_sha256=hashlib.sha256(proof_path.read_bytes()).hexdigest())
                except (OSError,ValueError,KeyError) as error:report.update(status='checkpoint_copy_postflight_failed',error=str(error))
            if args.retire_reference_modules and report.get('status')=='passed':
                try:
                    from retire_reference_modules import verify as verify_retirement
                    markers=proof_markers(log,'Verified unused reference retirement: ')
                    if len(markers)!=1:raise ValueError('Missing unique reference retirement proof')
                    path=Path(markers[0]);detail,sha=verify_retirement(ROOT,path)
                    report.update(reference_retirement_proof=str(path),reference_retirement_proof_sha256=sha)
                    if args.reference_retirement_probe:
                        markers=proof_markers(log,'Verified reference-retirement equivalence: ')
                        if len(markers)!=1:raise ValueError('Missing detached reference equivalence proof')
                        path=Path(markers[0]).resolve(strict=True)
                        if path!=(ROOT/'build-character-cast/reference-retirement-checks'/directory.name/'equivalence.json').resolve():raise ValueError('Detached reference equivalence escaped owned output')
                        detail=json.loads(path.read_text())
                        if detail.get('status')!='passed' or detail.get('exact_pcm') is not True or detail.get('original_settings') is not True or detail.get('probe_source_sha256')!=hashlib.sha256((ROOT/'tools/check_retired_reference_voice.py').read_bytes()).hexdigest():raise ValueError('Detached reference equivalence not proven')
                        if any(hashlib.sha256(Path(detail[name]).read_bytes()).hexdigest()!=detail[name+'_sha256'] for name in ('baseline','candidate')):raise ValueError('Detached reference audio changed')
                        report.update(reference_equivalence_proof=str(path),reference_equivalence_proof_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
                except (OSError,ValueError,KeyError) as error:report.update(status='reference_retirement_postflight_failed',error=str(error))
            if args.early_reference_retirement and report.get('status')=='passed':
                try:
                    from early_reference_retirement import verify as verify_early
                    markers=proof_markers(log,'Verified early reference retirement: ')
                    if len(markers)!=1:raise ValueError('Missing unique early retirement proof')
                    path=Path(markers[0]);detail,sha=verify_early(ROOT,path)
                    report.update(early_reference_proof=str(path),early_reference_proof_sha256=sha)
                    if args.early_reference_probe:
                        markers=proof_markers(log,'Verified early reference equivalence: ')
                        if len(markers)!=1:raise ValueError('Missing detached early equivalence proof')
                        path=Path(markers[0]).resolve(strict=True)
                        if path!=(ROOT/'build-character-cast/early-reference-checks'/directory.name/'equivalence.json').resolve():raise ValueError('Early equivalence escaped owned output')
                        detail=json.loads(path.read_text(encoding='utf-8'))
                        if detail.get('status')!='passed' or detail.get('exact_pcm') is not True or detail.get('original_settings') is not True or detail.get('probe_source_sha256')!=hashlib.sha256((ROOT/'tools/check_early_reference_voice.py').read_bytes()).hexdigest():raise ValueError('Early original-settings equivalence not proven')
                        if any(hashlib.sha256(Path(detail[name]).read_bytes()).hexdigest()!=detail[name+'_sha256'] for name in ('baseline','candidate')):raise ValueError('Early comparison audio changed')
                        report.update(early_equivalence_proof=str(path),early_equivalence_proof_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
                except (OSError,ValueError,KeyError) as error:report.update(status='early_reference_postflight_failed',error=str(error))
            if args.scope_quality_pages and report.get('status')=='passed':
                try:
                    from check_scoped_quality import verify as verify_quality
                    markers=proof_markers(log,'Verified scoped original quality: ')
                    if len(markers)!=1:raise ValueError('Missing original-quality page-scope proof')
                    path=Path(markers[0]);detail,sha=verify_quality(ROOT,path)
                    report.update(quality_scope_proof=str(path),quality_scope_proof_sha256=sha)
                    if args.quality_page_scope_probe:
                        markers=proof_markers(log,'Verified original quality equivalence: ')
                        if len(markers)!=1:raise ValueError('Missing detached original-quality comparison')
                        path=Path(markers[0]).resolve(strict=True)
                        if path!=(ROOT/'build-character-cast/quality-scope-checks'/directory.name/'equivalence.json').resolve():raise ValueError('Quality comparison escaped owned output')
                        p=json.loads(path.read_text(encoding='utf-8'))
                        if p.get('status')!='passed' or p.get('same_transcription_and_quality') is not True or p.get('probe_source_sha256')!=hashlib.sha256((ROOT/'tools/check_scoped_quality.py').read_bytes()).hexdigest():raise ValueError('Original quality comparison not proven')
                        report.update(quality_equivalence_proof=str(path),quality_equivalence_proof_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
                except (OSError,ValueError,KeyError) as error:report.update(status='quality_scope_postflight_failed',error=str(error))
            if args.page_scope_probe and report.get('status')=='passed':
                try:
                    markers=proof_markers(log,'Verified detached page-scope equivalence: ')
                    if len(markers)!=1:raise ValueError('Missing detached page-scope proof')
                    path=Path(markers[0]).resolve(strict=True)
                    expected=(ROOT/'build-character-cast/page-scope-probes'/directory.name/'equivalence.json').resolve()
                    if path!=expected:raise ValueError('Detached probe escaped owned output')
                    detail=json.loads(path.read_text())
                    if detail.get('status')!='passed' or detail.get('exact_pcm') is not True or detail.get('original_settings') is not True:raise ValueError('Detached equivalence not proven')
                    report.update(page_scope_proof=str(path),page_scope_proof_sha256=hashlib.sha256(path.read_bytes()).hexdigest())
                except (OSError,ValueError,KeyError) as error:report.update(status='page_scope_postflight_failed',error=str(error))
            if args.prepare_reference_cache and report.get('status')=='passed':
                from fixed_reference_cache import build_context,verify_artifact,digest
                try:
                    found=proof_markers(log,'Verified lossless fixed-reference cache: ')
                    if len(found)!=1:raise ValueError('Missing unique original-reference cache proof')
                    proof_path=Path(found[0].strip()).resolve(strict=True)
                    if proof_path.name!='verified.json' or not proof_path.is_relative_to((ROOT/'.voice-tools/fixed-reference-cache').resolve()):
                        raise ValueError('Reference cache proof escaped owned cache')
                    context=build_context(ROOT,ROOT/manifest['reference'],ROOT/'.voice-tools/index-tts/checkpoints_2')
                    _,proof=verify_artifact(proof_path,context)
                    report.update(reference_cache_proof=str(proof_path),reference_cache_proof_sha256=digest(proof_path),
                                  reference_cache_context_sha256=proof['context_sha256'])
                except (OSError,ValueError,KeyError) as error:
                    report.update(status='reference_cache_postflight_failed',error=str(error))
            if args.max_new_takes and report.get('status')=='passed':
                from voice_batch_scope import generation_state
                try:
                    report.update(generation_state(ROOT,manifest))
                    report['partial']=not report['generation_complete']
                    report['requested_max_new_takes']=args.max_new_takes
                    report['max_new_takes']=effective_take_limit(args.max_new_takes,args.memory_efficient)
                except (OSError,ValueError,KeyError) as error:
                    report.update(status='generation_postflight_failed',error=str(error))
    finally:
        if handle:
            api.CloseHandle(handle)
        (directory/'report.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(monitor.tail_bytes(log, 4000), flush=True)
    summary=dict(report)
    summary.pop('owned_job_memory_samples',None)
    summary['owned_job_memory_sample_count']=len(report.get('owned_job_memory_samples',[]))
    print(json.dumps(summary, indent=2), flush=True)
    return 0 if report.get('status') == 'passed' and report.get('exit_code') == 0 else 1


if __name__ == '__main__':
    raise SystemExit(main())
