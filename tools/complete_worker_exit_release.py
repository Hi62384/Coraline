"""Finite serial continuation of the already admitted single genuine probe.
No failed model is retried. Every later model/build has its existing original
quality, available-RAM, owned-job and emergency guards. No mod installation or
user-save modification. Future storyline drafts are deliberately not packaged.
"""
from datetime import datetime
import json
from pathlib import Path
import subprocess
import sys
import time
from run_parent_audio_chunks import wait_ready,read_attempt
from finish_worker_exit_audio import admit

ROOT=Path(__file__).resolve().parents[1]


def main():
    folder=ROOT/'build-character-cast/worker-exit-release-chain'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    state=dict(status='waiting_for_existing_genuine_probe',failed_speech_workers_preserved=18,steps=[],retries=False)
    def save(): (folder/'report.json').write_text(json.dumps(state,indent=2)+'\n',encoding='utf-8')
    def run(name,arguments,timeout):
        state.update(status='running',active_step=name);save();log=folder/(name+'.log')
        with log.open('xb',buffering=0) as stream:code=subprocess.run([sys.executable,*arguments],cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=timeout).returncode
        state['steps'].append(dict(name=name,exit_code=code,log=str(log)));save()
        if code:raise RuntimeError('Step failed; chain stopped without retry: '+name)
        return log
    checkpoint=ROOT/'build-character-cast/verified-staged-audio/20261029T012339Z/report.json'
    component=ROOT/'build-character-cast/compact-voice/20261029T014715Z/report.json'
    old=json.loads(checkpoint.read_text());end=datetime.fromisoformat(old['completion_window_started_utc'].replace('Z','+00:00')).timestamp()+old['completion_window_seconds']
    save();print('Worker-exit release chain: '+str(folder/'report.json'),flush=True)
    try:
        while time.time()<end-1860:
            content=(ROOT/'build-character-cast/worker-exit-genuine-admission.log').read_text(encoding='utf-8',errors='replace')
            if 'Traceback (most recent call last)' in content:raise RuntimeError('Existing single-probe admission failed; no replacement launched')
            paths=[Path(line.split(': ',1)[1]) for line in content.splitlines() if line.startswith('Compact voice attempt: ')]
            if len(paths)>1:raise ValueError('Ambiguous genuine probe; no later model permitted')
            if paths and paths[0].is_file():
                probe=paths[0].resolve()
                try:n=json.loads(probe.read_text())
                except json.JSONDecodeError:time.sleep(1);continue
                if n.get('status')!='passed' or n.get('exit_code')!=0:raise RuntimeError('Existing genuine probe failed; no later model permitted')
                break
            time.sleep(1)
        else:raise TimeoutError('Original finite speech window cannot admit later work')
        state.update(status='waiting_for_saved_quality_headroom',genuine_probe=str(probe));save()
        wait_ready(time.monotonic()+max(0,end-time.time()-1860),minimum_gib=2)
        log=run('saved_39_original_quality',['tools/run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue',
            '--low-memory-attempt','--validate','--asr-turbo','--existing-only','--resume-checks','--scope-quality-pages',
            '--quality-scope-admission','build-character-cast/compact-voice/20261029T011736Z/report.json','--timeout-seconds','1800'],1860)
        q,quality=read_attempt(log,'tools/incidental_mother_voice_manifest.json')
        if q.get('status')!='passed':raise ValueError('Saved-take original quality did not pass')
        admit(checkpoint,component,probe,quality)
        log=run('final_five_genuine_and_quality',['tools/finish_worker_exit_audio.py','--checkpoint',str(checkpoint),'--component',str(component),'--probe',str(probe),'--quality',str(quality)],max(1,int(end-time.time()+60)))
        controllers=[Path(line.split(': ',1)[1]) for line in log.read_text(encoding='utf-8',errors='replace').splitlines() if line.startswith('Verified audio completion report: ')]
        if len(controllers)!=1:raise ValueError('Missing unique all-audio completion receipt')
        state['audio_controller']=str(controllers[0]);save()
        log=run('genuine_release',['tools/complete_verified_release.py','--audio-controller',str(controllers[0]),'--native-report','build-character-cast/prepared-native/20261028T041551Z/report.json'],22*3600+60)
        receipts=[Path(line.split(': ',1)[1]) for line in log.read_text(encoding='utf-8',errors='replace').splitlines() if line.startswith('Verified release completion report: ')]
        if len(receipts)!=1 or json.loads(receipts[0].read_text()).get('status')!='package_checks_passed':raise ValueError('Genuine JAR release acceptance missing')
        state.update(status='package_checks_passed',release_report=str(receipts[0]));save()
    except BaseException as error:
        state.update(status='failed',error=str(error));save();raise


if __name__=='__main__':main()
