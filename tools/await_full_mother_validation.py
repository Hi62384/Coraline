"""Serial handoff from one guarded full Mother synthesis to its real quality gate.

Only watches the explicitly named existing run; never restarts a failed job,
changes its safety thresholds, accepts partial generation, or starts a second
model alongside synthesis. Packaging and human review remain separate.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

ROOT=Path(__file__).resolve().parents[1]
MANIFEST='tools/full_mother_voice_manifest.json'


def require_completed_generation(report):
    command=report.get('command',[])
    if (report.get('manifest')!=MANIFEST or report.get('stage')!='synthesis'
            or len(command)!=4 or command[1:]!=['-u','tools/generate_other_mother_welcome.py','--memory-efficient']):
        raise ValueError('Not the expected guarded full Mother synthesis job')
    if (report.get('status')!='passed' or report.get('exit_code')!=0
            or report.get('memory_measurement_available') is not True):
        raise ValueError('Generation did not finish successfully; no validation model will start')


def wait_for_generation(path,seconds,*,clock=time.monotonic,sleep=time.sleep):
    deadline=clock()+seconds
    while True:
        try:
            report=json.loads(path.read_text(encoding='utf-8'))
        except FileNotFoundError:
            report=None
        except json.JSONDecodeError:
            # The existing launcher writes its small final report at exit.
            # A partial write is not success and is never used as a receipt.
            report=None
        if report is not None:
            require_completed_generation(report)
            return report
        if clock()>=deadline:
            raise TimeoutError('No complete successful generation report within the finite handoff deadline')
        sleep(min(1,max(0,deadline-clock())))


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('generation_run')
    parser.add_argument('--wait-seconds',type=int,choices=[1800,3600,7200,28800],default=28800)
    parser.add_argument('--low-memory-attempt',action='store_true',help='Pass the already-approved guarded low-memory mode to validation; the emergency stop remains unchanged.')
    args=parser.parse_args()
    if not re.fullmatch(r'\d{8}T\d{6}Z',args.generation_run):raise ValueError('Expected one existing voice-run timestamp')
    base=(ROOT/'build-character-cast/compact-voice').resolve()
    directory=(base/args.generation_run).resolve(strict=True)
    if directory.parent!=base:raise ValueError('Generation directory escaped the voice-run root')
    report_path=directory/'handoff-report.json'
    if report_path.exists():raise ValueError('Existing handoff receipt preserved; inspect before another attempt')
    result={'generation_run':args.generation_run,'status':'waiting','low_memory_attempt':args.low_memory_attempt,
            'packaging':False,'human_listening':False}
    def save():
        staged=report_path.with_suffix('.tmp')
        staged.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
        staged.replace(report_path)
    save()
    try:
        print('Waiting for this exact guarded synthesis to exit successfully: '+args.generation_run,flush=True)
        wait_for_generation(directory/'report.json',args.wait_seconds)
        result['status']='validating';save()
        environment=dict(os.environ)
        environment['PATH']=str(ROOT/'.voice-tools/index-tts/.venv/Scripts')+';C:/ffmpeg;'+environment['PATH']
        command=[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--full-dialogue',
                 '--validate','--asr-turbo','--resume-checks','--timeout-seconds','28800']
        if args.low_memory_attempt:command.append('--low-memory-attempt')
        # This wrapper owns the existing Windows job/memory guard. The handoff
        # launches only after the synthesis wrapper's final exit report exists.
        print('Generation exited successfully. Starting the full guarded ASR/speaker/decode check.',flush=True)
        completed=subprocess.run(command,cwd=ROOT,env=environment,timeout=28920)
        result['validation_exit_code']=completed.returncode
        if completed.returncode:raise RuntimeError('Full Mother quality check failed; preserve masters and inspect flags before packaging')
        result['status']='auditing_all_masters';save()
        audit=directory/'all-master-proofs.json'
        checked=subprocess.run([sys.executable,'tools/audit_voice_master_proofs.py','--report',str(audit)],
                               cwd=ROOT,env=environment,timeout=180)
        if checked.returncode:raise RuntimeError('Complete master-proof audit failed; packaging remains prohibited')
        proof=json.loads(audit.read_text(encoding='utf-8'))
        if proof.get('status')!='passed' or proof.get('passed')!=868 or proof.get('failed')!=0:
            raise RuntimeError('Expected all868scripted M/F master proofs')
        result.update(status='passed',master_proofs=868)
        print('All868master proofs pass. Genuine packaging and final packaged audits remain separate.',flush=True)
    except BaseException as error:
        result.update(status='failed',error=str(error));save()
        raise
    save()


if __name__=='__main__':main()
