"""Serial disposable client acceptance, using the existing native RAM guards.

Does not infer human listening, original-save or connected-multiplayer acceptance.
No release/voice generation and no existing player world is opened.
"""
import argparse, json, subprocess, sys, time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
CASES=[
    ('post_escape_tea',['--missing-parents-tea'],600),
    ('tycoon_care',['--tycoon-phase-one'],600),
    ('tycoon_dolls_world',['--tycoon-phase-two'],600),
    ('tycoon_food',['--tycoon-phase-two','--tycoon-food'],600),
    ('tycoon_attractions_souls_workers',['--tycoon-phase-two','--tycoon-expansion'],1800),
    ('story_rebirth',['--story-rebirth'],600),
    ('web_hearing_collision',['--rescue-web'],600),
    ('family_game_night',['--family-game-night'],1800),
    ('nautical_opening',['--nautical-opening'],1800),
    ('theatre_aerial',['--nautical-opening','--theatre-aerial'],1800),
    ('attic_furnace',['--first-escape','--insect-parlor','--rescue-attic'],1800),
    ('rescue_finale',['--first-escape','--insect-parlor','--rescue-finale'],1800),
]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--compiled-report',required=True)
    parser.add_argument('--cases',help='Comma-separated exact case names; default all')
    args=parser.parse_args()
    compiler=(ROOT/args.compiled_report).resolve()
    if not compiler.is_relative_to(ROOT/'build-character-cast/cat-focused-compile') or compiler.name!='report.json':raise ValueError('Expected isolated compiler report')
    selected=set(args.cases.split(',')) if args.cases else {c[0] for c in CASES}
    if not selected or selected-{c[0] for c in CASES}:raise ValueError('Unknown checkpoint')
    folder=ROOT/'build-character-cast/serial-runtime'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    report={'scope':'Serial disposable real client checkpoints, not full story, human listening or multiplayer acceptance','status':'running','compiled_report':str(compiler),'cases':[]}
    target=folder/'report.json'
    def save():target.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    save();print('Serial acceptance report: '+str(target),flush=True)
    for name,flags,seconds in CASES:
        if name not in selected:continue
        before=set((ROOT/'build-character-cast/prepared-native').glob('*/report.json'))
        row={'case':name,'status':'running'};report['cases'].append(row);save();print('Starting '+name,flush=True)
        command=[sys.executable,str(ROOT/'tools/run_prepared_native.py'),'client',*flags,'--compiled-report',str(compiler),'--heap-mib','1024','--minimum-start-free-mib','3072','--timeout-seconds',str(seconds)]
        with (folder/(name+'.log')).open('xb') as log:
            result=subprocess.run(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
        created=set((ROOT/'build-character-cast/prepared-native').glob('*/report.json'))-before
        row['runner_exit_code']=result.returncode
        if len(created)==1:
            native=created.pop();evidence=json.loads(native.read_text());row.update(native_report=str(native),status=evidence.get('status','unknown'),clean_runtime_shutdown=evidence.get('clean_runtime_shutdown',False))
        else:row['status']='preparation_or_admission_failed'
        save();print(name+': '+row['status'],flush=True)
        # A resource/admission failure applies to the next heavy client too.
        # Stop rather than repeatedly pressuring RAM or treating skipped work as passed.
        if row['status'] in ('low_memory_cutoff','preparation_or_admission_failed','output_capture_failed'):
            report['status']='resource_or_preparation_blocked';save();return 1
    report['status']='passed' if all(r['status']=='passed' and r['runner_exit_code']==0 for r in report['cases']) else 'failures_require_review';save()
    return 0 if report['status']=='passed' else 1

if __name__=='__main__':raise SystemExit(main())
