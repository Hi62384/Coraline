"""Serial native server acceptance by domain, with exact helper dependencies.

Uses byte-identical compiled GameTests and existing native runner admission,
process/job limits, required-test counts and clean-shutdown checks. No fake
connected-client, rendered-scene, human-listening or full-story pass is inferred.
"""
import argparse,json,re,subprocess,sys,time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
GROUPS={
    'accepted_ghost_rebirth':('AcceptedButtonEyes','Ghost','StoryRebirth'),
    'rescue':('Rescue',),
    'palace':('Palace',),
    'tycoon':('Tycoon',),
    'postgame':('Infinite','BreachWorldData','PostgameHostiles','OtherWorldButtonDrops','SiteLockpick','ThreadCutter'),
    'equipment_loot':('EquipmentAcquisition','CoralineEquipmentRecipe','CabinetLootRoll'),
}
def selected(classes,prefixes):
    files=list(classes.rglob('*GameTests*.class'));families={p.stem.split('$',1)[0] for p in files if p.stem.split('$',1)[0].startswith(prefixes)}
    while True:
        required=set()
        for path in files:
            if path.stem.split('$',1)[0] in families:
                required.update(m.decode('ascii').rsplit('/',1)[-1].split('$',1)[0] for m in re.findall(rb'com/coraline/[A-Za-z0-9_/$]+GameTests',path.read_bytes()))
        if required<=families:break
        families|=required
    if not families:raise ValueError('Empty native domain')
    return sorted(name+'.java' for name in families)

def main():
    p=argparse.ArgumentParser();p.add_argument('--compiled-report',required=True);p.add_argument('--groups');p.add_argument('--prepare-only',action='store_true');args=p.parse_args()
    compiler=(ROOT/args.compiled_report).resolve()
    if not compiler.is_relative_to(ROOT/'build-character-cast/cat-focused-compile') or compiler.name!='report.json':raise ValueError('Expected isolated compile report')
    compiled=json.loads(compiler.read_text());classes=compiler.parent/'classes'
    if compiled.get('status')!='passed' or not compiled.get('full_source_compile'):raise ValueError('Successful full source compile required')
    names=args.groups.split(',') if args.groups else list(GROUPS)
    if len(set(names))!=len(names) or set(names)-GROUPS.keys():raise ValueError('Unknown or repeated domain')
    plans={name:selected(classes,GROUPS[name]) for name in names}
    if args.prepare_only:
        print(json.dumps(plans,indent=2));return 0
    folder=ROOT/'build-character-cast/native-domains'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());folder.mkdir(parents=True,exist_ok=False)
    report={'scope':'Actual native server fixtures only; client/render/listening/multiplayer acceptance tracked separately','compiled_report':str(compiler),'status':'running','groups':[]};target=folder/'report.json'
    def save():target.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    save();print('Native domain report: '+str(target),flush=True)
    for name,tests in plans.items():
        before=set((ROOT/'build-character-cast/prepared-native').glob('*/report.json'));row={'group':name,'tests':tests,'status':'running'};report['groups'].append(row);save();print('Starting '+name,flush=True)
        command=[sys.executable,str(ROOT/'tools/run_prepared_native.py'),'server','--test-classes',','.join(tests),'--compiled-report',str(compiler),'--heap-mib','1536','--minimum-start-free-mib','3072','--timeout-seconds','1800']
        with (folder/(name+'.log')).open('xb') as log:result=subprocess.run(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
        created=set((ROOT/'build-character-cast/prepared-native').glob('*/report.json'))-before;row['runner_exit_code']=result.returncode
        if len(created)==1:
            native=created.pop();evidence=json.loads(native.read_text());row.update(status=evidence.get('status','unknown'),native_report=str(native),required_tests_passed=evidence.get('required_tests_passed'),clean_runtime_shutdown=evidence.get('clean_runtime_shutdown',False))
        else:row['status']='preparation_or_admission_failed'
        save();print(name+': '+row['status'],flush=True)
        if row['status'] in ('low_memory_cutoff','preparation_or_admission_failed','output_capture_failed'):
            report['status']='resource_or_preparation_blocked';save();return 1
    report['status']='passed' if all(r['status']=='passed' and r['runner_exit_code']==0 for r in report['groups']) else 'failures_require_review';save();return 0 if report['status']=='passed' else 1

if __name__=='__main__':raise SystemExit(main())
