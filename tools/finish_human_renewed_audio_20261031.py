"""A separately approved, single-use six-hour window after an idle expiry.

Never changes previous receipts or retries a stopped/failed speech worker.
Uses the original successful genuine-39 and quality-39 admission unchanged.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import sys
import time
from datetime import datetime,timezone
from finish_worker_exit_audio import admit
from finish_positive_audio_checkpoint import MANIFESTS
from run_parent_audio_chunks import exclusive_chain,wait_ready,read_attempt
from voice_batch_scope import generation_state

ROOT=Path(__file__).resolve().parents[1]
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--approval',type=Path,required=True);args=parser.parse_args()
    approval=args.approval.resolve(strict=True)
    if not approval.is_relative_to(ROOT/'build-character-cast/voice-window-approvals'):raise ValueError('Unexpected human approval location')
    authorization=json.loads(approval.read_text(encoding='utf-8'))
    if authorization.get('human_answer')!='Yes, renew the guarded voice window' or authorization.get('window_seconds')!=21600 or authorization.get('consumed') is not False:raise ValueError('An unused, explicit six-hour human renewal is required')
    began=datetime.fromisoformat(authorization['opened_utc'].replace('Z','+00:00')).timestamp();remaining=began+21600-time.time()
    if not 1860<remaining<=21600:raise TimeoutError('The approved finite window cannot admit another worker')
    previous=ROOT/'build-character-cast/verified-staged-audio/20261029T160452Z/report.json';idle=json.loads(previous.read_text())
    if sha(previous)!=authorization['previous_sha256'] or idle.get('status')!='failed' or idle.get('error')!='Finite audio-chain headroom deadline exhausted' or idle.get('steps')!=[] or idle.get('active_step')!='mother_incidental_40':raise ValueError('Only the unchanged idle headroom expiry may resume')
    base,prior,floor,guards,qc,qfloor=admit(Path(idle['previous_report']),ROOT/'build-character-cast/compact-voice/20261029T014715Z/report.json',Path(idle['genuine_probe']),Path(idle['quality_39']))
    if floor<3.875 or floor!=idle['required_start_free_gib']:raise ValueError('The proven speech admission floor changed')
    folder=ROOT/'build-character-cast/verified-staged-audio'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    deadline=time.monotonic()+remaining
    report=dict(status='admitted',controller_pid=os.getpid(),human_renewal=authorization['human_answer'],approval=str(approval),previous_report=str(previous),previous_sha256=sha(previous),completion_window_started_utc=authorization['opened_utc'],completion_window_seconds=21600,historical_speech_ram_stops=18,retry_failed_models=False,worker_max_new_takes=1,release_artifact_allowed=False,required_start_free_gib=floor,quality_start_free_gib=qfloor,manifest_sha256=prior['manifest_sha256'],steps=[])
    authorization.update(consumed=True,controller_report=str(folder/'report.json'));approval.write_text(json.dumps(authorization,indent=2)+'\n',encoding='utf-8')
    guards[str(previous)]=sha(previous);guards[str(approval)]=sha(approval);guards[str(Path(__file__).resolve())]=sha(__file__)
    report['admission_anchors']=guards
    def save():(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    def verify():
        if any(sha(name)!=digest for name,digest in guards.items()):raise ValueError('An admitted source/proof changed')
        if any(sha(ROOT/name)!=digest for name,digest in prior['manifest_sha256'].items()):raise ValueError('Authored dialogue changed')
    def launch(name,command,minimum):
        verify();report.update(status='waiting_for_headroom',active_step=name);save();wait_ready(deadline-1860,minimum_gib=minimum);verify()
        if time.monotonic()+1860>deadline:raise TimeoutError('Finite renewed window cannot admit this worker')
        report.update(status='running');save();log=folder/(name+'.log')
        with log.open('xb',buffering=0) as stream:code=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=1860).returncode
        worker,path=read_attempt(log,MANIFESTS[2]);report['steps'].append(dict(name=name,report=str(path),sha256=sha(path),status=worker.get('status'),exit_code=code));save()
        if code or worker.get('status')!='passed' or worker.get('memory_measurement_available') is not True:raise RuntimeError('Worker failed; no retry or later model is permitted')
        return worker
    save();print('Human-renewed final-five audio report: '+str(folder/'report.json'),flush=True)
    try:
        with exclusive_chain():
            for expected in range(40,45):
                before=generation_state(ROOT,json.loads((ROOT/MANIFESTS[2]).read_text(encoding='utf-8')))
                if before['completed']!=expected-1:raise ValueError('Unexpected preserved take count')
                worker=launch('mother_incidental_'+str(expected),[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue','--memory-efficient','--low-memory-attempt','--require-reference-cache','--stage-flow-model','--stage-vocoder','--cuda-async-allocator','--early-reference-retirement','--early-reference-admission',str(ROOT/'build-character-cast/compact-voice/20261029T014715Z/report.json'),'--max-new-takes','1','--timeout-seconds','1800'],floor)
                if worker.get('completed')!=expected:raise ValueError('Worker did not produce exactly one original take')
            launch('mother_incidental_44_quality',[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue','--low-memory-attempt','--validate','--asr-turbo','--existing-only','--resume-checks','--scope-quality-pages','--quality-scope-admission',str(qc),'--timeout-seconds','1800'],qfloor)
            verify();report.update(status='auditing_all_masters',active_step=None);save();audit=folder/'all-master-proofs.json'
            code=subprocess.run([sys.executable,'tools/audit_voice_master_proofs.py','--report',str(audit)],cwd=ROOT,timeout=300).returncode;result=json.loads(audit.read_text())
            if code or (result.get('status'),result.get('passed'),result.get('failed'))!=('passed',946,0):raise RuntimeError('All 946 genuine speech proofs are required')
            report.update(status='passed',genuine_master_proofs=946);save()
    except BaseException as error:report.update(status='failed',error=str(error));save();raise
if __name__=='__main__':main()
