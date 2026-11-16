"""Serial completion of the current full-wording and incidental parent voices.

Waits for one explicitly named successful guarded synthesis, checks all of it,
then generates/checks each supplemental speaker separately. Stops on any failed
step. No packaging, application closure, retries, or lowered quality thresholds.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
from await_full_mother_validation import wait_for_generation

ROOT=Path(__file__).resolve().parents[1]


def steps(low_memory=False):
    common=[sys.executable,'tools/run_compact_voice_batch.py','--timeout-seconds','28800']
    if low_memory:common.append('--low-memory-attempt')
    result=[('full_mother_quality',common+['--speaker','mother','--full-dialogue',
                                        '--validate','--asr-turbo','--resume-checks'])]
    for who in ('father','mother'):
        base=common+['--speaker',who,'--incidental-dialogue']
        result.extend([(who+'_incidental_generation',base+['--memory-efficient']),
                       (who+'_incidental_quality',base+['--validate','--asr-turbo','--resume-checks'])])
    return result


def run_steps(planned,result,save,environment,*,run=subprocess.run):
    for name,command in planned:
        result.update(status='running',active_step=name);save()
        completed=run(command,cwd=ROOT,env=environment,timeout=28920)
        result['steps'].append({'name':name,'command':command,'exit_code':completed.returncode});save()
        if completed.returncode:raise RuntimeError(name+' failed; later models and packaging remain prohibited')


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('generation_run')
    parser.add_argument('--low-memory-attempt',action='store_true')
    args=parser.parse_args()
    if not re.fullmatch(r'\d{8}T\d{6}Z',args.generation_run):raise ValueError('Expected an existing guarded synthesis timestamp')
    base=(ROOT/'build-character-cast/compact-voice').resolve()
    generation=(base/args.generation_run).resolve(strict=True)
    if generation.parent!=base:raise ValueError('Unexpected synthesis path')
    folder=ROOT/'build-character-cast/parent-audio-chain'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())
    folder.mkdir(parents=True,exist_ok=False)
    report=folder/'report.json'
    result={'status':'waiting_for_generation','generation_run':args.generation_run,
            'low_memory_attempt':args.low_memory_attempt,'steps':[],
            'packaging':False,'human_listening':False}
    def save():
        staged=report.with_suffix('.tmp');staged.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');staged.replace(report)
    save()
    print('Parent audio chain report: '+str(report),flush=True)
    try:
        wait_for_generation(generation/'report.json',28800)
        environment=dict(os.environ)
        environment['PATH']=str(ROOT/'.voice-tools/index-tts/.venv/Scripts')+';C:/ffmpeg;'+environment['PATH']
        run_steps(steps(args.low_memory_attempt),result,save,environment)
        result.update(status='auditing_all_masters',active_step='all_master_proofs');save()
        audit=folder/'all-master-proofs.json'
        checked=subprocess.run([sys.executable,'tools/audit_voice_master_proofs.py','--report',str(audit)],
                               cwd=ROOT,env=environment,timeout=180)
        proof=json.loads(audit.read_text(encoding='utf-8')) if audit.exists() else {}
        if (checked.returncode or proof.get('status')!='passed' or proof.get('passed')!=946 or proof.get('failed')!=0
                or (proof.get('event_dialogue'),proof.get('legacy_dialogue'),proof.get('incidental_dialogue'))!=(583,285,78)):
            raise RuntimeError('Expected all946source/script/reference/quality-matched master proofs')
        result.update(status='passed',master_proofs=946,active_step=None)
        print('All946parent speech proofs pass. Normal genuine packaging and final packaged audits remain separate.',flush=True)
    except BaseException as error:
        result.update(status='failed',error=str(error));save();raise
    save()


if __name__=='__main__':main()
