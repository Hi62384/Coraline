"""Serial, finite release completion after the already-running audio controller.

Never retries failed models, installs a mod, edits a user save, or claims visual
or human listening QA. All existing model/build emergency guards remain active.
"""
from pathlib import Path
import argparse,hashlib,json,os,re,subprocess,sys,time,zipfile
from datetime import datetime
from finish_verified_staged_audio import successful_deadline_checkpoint
from run_parent_audio_chunks import wait_ready

ROOT=Path(__file__).resolve().parents[1]
BASELINE=ROOT/'build-character-cast/libs/coraline-1.15.94-placeholder-audio.jar'
BASELINE_SHA='32714031ea055d007b483fb5c2ae80fa66da1973f34203958c62568c0b9e1d99'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--audio-controller',type=Path,required=True);parser.add_argument('--native-report',type=Path,default=ROOT/'build-character-cast/prepared-native/20261028T012844Z/report.json',help='Current complete 159-test source acceptance; obsolete source hashes are refused');args=parser.parse_args()
    audio=args.audio_controller.resolve(strict=True)
    if audio.name!='report.json' or audio.parent.parent!=(ROOT/'build-character-cast/verified-staged-audio').resolve():raise ValueError('Unexpected audio controller')
    native=args.native_report.resolve(strict=True)
    if native.name!='report.json' or native.parent.parent!=(ROOT/'build-character-cast/prepared-native').resolve():raise ValueError('Native receipt escaped owned test reports')
    n=json.loads(native.read_text())
    if n.get('kind')!='server' or n.get('status')!='passed' or n.get('exit_code')!=0 or n.get('expected_compiled_game_tests')!=159 or n.get('required_tests_passed')!=159:raise ValueError('Current complete 159-test acceptance required')
    compile_proof=json.loads(Path(n['isolated_compiler_report']).read_text())
    sources=compile_proof['source_sha256']
    if any(sha(ROOT/path)!=digest for path,digest in sources.items()):raise ValueError('Story source changed after the 159-test acceptance')
    if sha(BASELINE)!=BASELINE_SHA:raise ValueError('Preserved previous release changed')
    lock=ROOT/'build-character-cast/verified-release-completion.lock'
    descriptor=os.open(lock,os.O_CREAT|os.O_EXCL|os.O_WRONLY)
    os.write(descriptor,str(os.getpid()).encode());os.close(descriptor)
    folder=ROOT/'build-character-cast/verified-release-completion'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    state=dict(status='waiting_for_audio',pid=os.getpid(),audio_controller=str(audio),native_159_report=str(native),native_159_sha256=sha(native),source_sha256=sources,steps=[],visual_qa_current=False,human_listening=False)
    deadline=time.monotonic()+22*3600
    def save(): (folder/'report.json').write_text(json.dumps(state,indent=2)+'\n')
    def run(name,arguments,timeout=2100,report_prefix=None):
        if time.monotonic()+timeout>deadline:raise TimeoutError('Finite release completion window exhausted')
        if any(sha(ROOT/path)!=digest for path,digest in sources.items()):raise ValueError('Accepted story sources changed; reverify before packaging')
        state.update(status='running',active_step=name);save();log=folder/(name+'.log')
        with log.open('xb',buffering=0) as stream: code=subprocess.run([sys.executable,*arguments],cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,timeout=timeout).returncode
        row=dict(name=name,exit_code=code,log=str(log));state['steps'].append(row);save()
        if code:raise RuntimeError('Completion step failed, no automatic retry: '+name)
        if report_prefix:
            matches=[line[len(report_prefix):].strip() for line in log.read_text(errors='replace').splitlines() if line.startswith(report_prefix)]
            if len(matches)!=1:raise ValueError('Missing unique completion receipt: '+name)
            path=Path(matches[0]).resolve(strict=True)
            if not path.is_relative_to(ROOT/'build-character-cast'):raise ValueError('Receipt escaped owned output')
            receipt=json.loads(path.read_text());row.update(report=str(path),sha256=sha(path));save()
            if receipt.get('status')!='passed' or receipt.get('exit_code')!=0:raise ValueError('Completion receipt not passed: '+name)
            return path
    try:
        save();print('Verified release completion report: '+str(folder/'report.json'),flush=True)
        while True:
            if time.monotonic()>=deadline:raise TimeoutError('Finite audio wait exhausted')
            previous=json.loads(audio.read_text())
            if previous['status']=='passed':break
            if previous['status']=='failed':
                successful_deadline_checkpoint(audio)
                original_end=datetime.fromisoformat(previous['original_started_utc'].replace('Z','+00:00')).timestamp()+previous['original_deadline_seconds']
                if time.time()<original_end:time.sleep(min(15,original_end-time.time()));continue
                run('remaining_audio',['tools/finish_verified_staged_audio.py','--successful-probe','build-character-cast/compact-voice/20261027T231157Z/report.json','--component-report','build-character-cast/compact-voice/20261027T220811Z/report.json','--vocoder-component-report','build-character-cast/compact-voice/20261027T230914Z/report.json','--single-take-workers','--continue-successful-run',str(audio)],timeout=18*3600+120)
                break
            if previous['status'] not in ('ready','running','waiting_for_headroom','auditing_all_masters'):raise ValueError('Unrecognized audio controller state')
            time.sleep(15)
        run('all_master_proofs',['tools/audit_voice_master_proofs.py','--report',str(folder/'all-master-proofs.json')],timeout=300)
        audit=json.loads((folder/'all-master-proofs.json').read_text())
        if (audit.get('status'),audit.get('passed'),audit.get('failed'))!=('passed',946,0):raise ValueError('All genuine recordings must pass')
        wait_ready(min(deadline,time.monotonic()+1800),minimum_gib=2)
        run('production_compile',['tools/run_low_memory_release.py','production-compile','--genuine-audio','--minimum-start-free-mib','2048'],report_prefix='Low-memory production-compile; report: ')
        wait_ready(min(deadline,time.monotonic()+1800),minimum_gib=2)
        run('genuine_package',['tools/run_low_memory_release.py','package','--genuine-audio','--lean-package','--minimum-start-free-mib','2048'],report_prefix='Low-memory package; report: ')
        jar=ROOT/'build-character-cast/libs/coraline-1.15.95.jar'
        if not jar.is_file():raise ValueError('Expected genuine release JAR missing')
        compiled=run('incidental_compile',['tools/check_all_story_java.py','--test-classes','IncidentalDialogueGameTests.java'],timeout=400,report_prefix='Focused compiler report: ')
        run('incidental_native',['tools/run_prepared_native.py','server','--compiled-report',str(compiled),'--test-classes','IncidentalDialogueGameTests.java','--heap-mib','1024','--timeout-seconds','600','--minimum-start-free-mib','2048','--memory-wait-seconds','30'],timeout=750,report_prefix='Native runtime report: ')
        for name,arguments in (
            ('numbered_audio',['tools/verify_numbered_events_audio.py',str(jar),'--report',str(folder/'numbered-audio.json')]),
            ('visit_audio',['tools/verify_visit_events_audio.py','--jar',str(jar)]),
            ('story_continuation',['tools/verify_story_continuation_release.py','--jar',str(jar)]),
            ('first_escape',['tools/verify_first_escape_release.py','--jar',str(jar)]),
            ('mixins',['tools/verify_packaged_mixins.py',str(jar)]),
            ('release_revision',['tools/verify_event_revision_release.py',str(jar),'--previous',str(BASELINE),'--previous-sha256',BASELINE_SHA,'--report',str(folder/'release-revision.json')])):
            run(name,arguments,timeout=1800)
        with zipfile.ZipFile(jar) as archive:
            if archive.testzip() is not None:raise ValueError('Corrupt release archive')
            if any('GameTests' in name or 'VisualCheck' in name or 'AcceptedBranchPlayers' in name for name in archive.namelist()):raise ValueError('QA helper leaked into release')
            largest=[dict(name=entry.filename,bytes=entry.file_size,compressed_bytes=entry.compress_size) for entry in sorted(archive.infolist(),key=lambda e:e.compress_size,reverse=True)[:25]]
        if sha(BASELINE)!=BASELINE_SHA:raise ValueError('Previous release changed during completion')
        state.update(status='package_checks_passed',active_step=None,jar=str(jar),jar_sha256=sha(jar),jar_bytes=jar.stat().st_size,previous_bytes=BASELINE.stat().st_size,largest_entries=largest,asset_size_report=str(ROOT/'build-character-cast/reports/asset-size.json'),claim_limit='No current accepted-route visual playthrough, uninterrupted full-story replay or human listening acceptance.');save()
    except BaseException as error:state.update(status='failed',error=str(error));save();raise
    finally:
        if lock.exists() and lock.read_text()==str(os.getpid()):lock.unlink()

if __name__=='__main__':main()
