"""One manually resumed admission after an idle wait, not a failed-model retry.
The successful changed-exit PCM component and measured start floor are retained.
Old failures/deadlines remain immutable; this fresh human-requested wait is finite.
"""
from pathlib import Path
import json,subprocess,sys,time,hashlib
from run_worker_exit_genuine_probe import COMPONENT,completed_exit
from run_early_reference_genuine_probe import admit,measured_floor
from run_parent_audio_chunks import wait_ready,read_attempt
from voice_batch_scope import generation_state
ROOT=Path(__file__).resolve().parents[1]


def main():
    previous=ROOT/'build-character-cast/worker-exit-genuine-admission.log';text=previous.read_text(encoding='utf-8',errors='replace')
    if 'Finite audio-chain headroom deadline exhausted' not in text or 'Compact voice attempt:' in text:raise ValueError('Only the expired idle admission may resume; no failed model retry')
    component,c=admit(COMPONENT);completed_exit(component);floor=measured_floor(c)
    manifest=ROOT/'tools/incidental_mother_voice_manifest.json'
    if generation_state(ROOT,json.loads(manifest.read_text(encoding='utf-8')))['completed']!=38:raise ValueError('Expected exactly 38 preserved Mother takes')
    folder=ROOT/'build-character-cast/resumed-voice-admission'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    report=dict(status='waiting_for_headroom',human_request='Resume and finish the implementation queue',previous_idle_log=str(previous),previous_idle_sha256=hashlib.sha256(previous.read_bytes()).hexdigest(),component=str(component),minimum_start_free_gib=floor,wait_seconds=3600,automatic_retry=False)
    def save():(folder/'report.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    save();print('Resumed single-take admission: '+str(folder/'report.json'),flush=True)
    try:
        wait_ready(time.monotonic()+3600,minimum_gib=floor);admit(component);completed_exit(component)
        if generation_state(ROOT,json.loads(manifest.read_text(encoding='utf-8')))['completed']!=38:raise ValueError('Catalogue changed while idle')
        report['status']='running';save()
        command=[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue','--memory-efficient','--low-memory-attempt','--require-reference-cache','--stage-flow-model','--stage-vocoder','--cuda-async-allocator','--early-reference-retirement','--early-reference-admission',str(component),'--max-new-takes','1','--timeout-seconds','1800']
        log=folder/'genuine.log'
        with log.open('xb',buffering=0) as stream:code=subprocess.run(command,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=1860).returncode
        n,path=read_attempt(log,'tools/incidental_mother_voice_manifest.json');report.update(worker_report=str(path),worker_exit=code)
        if code or n.get('status')!='passed' or n.get('completed')!=39:raise RuntimeError('Genuine worker failed; stop without retry')
        completed_exit(path);report['status']='passed';save()
    except BaseException as error:report.update(status='failed',error=str(error));save();raise


if __name__=='__main__':main()
