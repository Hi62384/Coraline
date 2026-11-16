"""Human-resumed final five after successful genuine-39 and original quality-39.
A new finite window is explicit; expired windows and all 18 RAM stops remain
immutable. One take per worker, no failed-worker retry, no release approval.
"""
from pathlib import Path
import argparse,hashlib,json,subprocess,sys,time
from finish_worker_exit_audio import admit
from finish_positive_audio_checkpoint import MANIFESTS
from run_parent_audio_chunks import exclusive_chain,wait_ready,read_attempt
from voice_batch_scope import generation_state
ROOT=Path(__file__).resolve().parents[1]
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def main():
    parser=argparse.ArgumentParser()
    for name in ('checkpoint','component','probe','quality'):parser.add_argument('--'+name,type=Path,required=True)
    args=parser.parse_args();previous,prior,floor,guards,qc,qfloor=admit(args.checkpoint,args.component,args.probe,args.quality)
    folder=ROOT/'build-character-cast/verified-staged-audio'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    start=time.time();deadline=time.monotonic()+21600
    report=dict(status='admitted',controller_pid=__import__('os').getpid(),human_renewal='WORK ON IT THEN!!!; continue and finish the outstanding work on October 29',
        previous_report=str(previous),previous_sha256=sha(previous),previous_window_preserved=False,
        expired_previous_window=dict(started_utc=prior['completion_window_started_utc'],seconds=prior['completion_window_seconds']),
        completion_window_started_utc=time.strftime('%Y-%m-%dT%H:%M:%SZ',time.gmtime(start)),completion_window_seconds=21600,
        historical_speech_ram_stops=18,retry_failed_models=False,worker_max_new_takes=1,release_artifact_allowed=False,
        genuine_probe=str(args.probe),quality_39=str(args.quality),required_start_free_gib=floor,manifest_sha256=prior['manifest_sha256'],
        admission_anchors=guards,steps=[])
    guards[str(Path(__file__).resolve())]=sha(__file__)
    def save():(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    def verify():
        if any(sha(name)!=digest for name,digest in guards.items()):raise ValueError('Admitted source/proof changed')
        if any(sha(ROOT/name)!=digest for name,digest in prior['manifest_sha256'].items()):raise ValueError('Authored dialogue changed')
    def launch(name,command,minimum):
        verify()
        if time.monotonic()+1860>deadline:raise TimeoutError('Finite renewed window cannot admit another worker')
        report.update(status='waiting_for_headroom',active_step=name);save();wait_ready(deadline-1860,minimum_gib=minimum);verify()
        report.update(status='running');save();log=folder/(name+'.log')
        with log.open('xb',buffering=0) as stream:code=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=1860).returncode
        worker,path=read_attempt(log,MANIFESTS[2]);report['steps'].append(dict(name=name,report=str(path),sha256=sha(path),status=worker.get('status'),exit_code=code));save()
        if code or worker.get('status')!='passed' or worker.get('memory_measurement_available') is not True:raise RuntimeError('Worker failed; no retry or later model permitted')
        return worker
    save();print('Renewed final-five audio report: '+str(folder/'report.json'),flush=True)
    try:
        with exclusive_chain():
            for expected in range(40,45):
                before=generation_state(ROOT,json.loads((ROOT/MANIFESTS[2]).read_text(encoding='utf-8')))
                if before['completed']!=expected-1:raise ValueError('Unexpected preserved take count')
                worker=launch('mother_incidental_'+str(expected),[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue','--memory-efficient','--low-memory-attempt','--require-reference-cache','--stage-flow-model','--stage-vocoder','--cuda-async-allocator','--early-reference-retirement','--early-reference-admission',str(args.component),'--max-new-takes','1','--timeout-seconds','1800'],floor)
                if worker.get('completed')!=expected:raise ValueError('Worker did not produce exactly one original take')
            launch('mother_incidental_44_quality',[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue','--low-memory-attempt','--validate','--asr-turbo','--existing-only','--resume-checks','--scope-quality-pages','--quality-scope-admission',str(qc),'--timeout-seconds','1800'],qfloor)
            verify();report.update(status='auditing_all_masters',active_step=None);save();audit=folder/'all-master-proofs.json'
            code=subprocess.run([sys.executable,'tools/audit_voice_master_proofs.py','--report',str(audit)],cwd=ROOT,timeout=300).returncode
            result=json.loads(audit.read_text(encoding='utf-8'))
            if code or (result.get('status'),result.get('passed'),result.get('failed'))!=('passed',946,0):raise RuntimeError('All 946 genuine speech proofs required')
            report.update(status='passed',genuine_master_proofs=946);save()
    except BaseException as error:report.update(status='failed',error=str(error));save();raise
if __name__=='__main__':main()
