"""Read-only, complete event-master audit. Missing work is a failed audit."""
import argparse,collections,json
from pathlib import Path
from visit_event_audio_catalog import effective_recordings
from voice_recording_proof import validate,legacy_recordings
from incidental_dialogue_catalog import recordings as incidental_recordings
ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--report',type=Path);args=parser.parse_args()
    passed=[];failed=[];cache={}
    event_records=effective_recordings(ROOT);legacy=legacy_recordings(ROOT);incidental=incidental_recordings(ROOT)
    all_records={**event_records,**legacy,**incidental}
    if len(all_records)!=len(event_records)+len(legacy)+len(incidental):raise ValueError('Voice catalogue IDs overlap')
    for name,record in all_records.items():
        try:passed.append(validate(ROOT,record,cache))
        except (OSError,ValueError,KeyError) as error:failed.append({'event':name,'source':record['source'],'error':str(error)})
    result={'status':'failed' if failed else 'passed','passed':len(passed),'failed':len(failed),
        'event_dialogue':len(event_records),'legacy_dialogue':len(legacy),'incidental_dialogue':len(incidental),'checks':passed,'failures':failed}
    if args.report:
        path=args.report.resolve()
        if not path.is_relative_to(ROOT/'build-character-cast'):raise ValueError('Audit reports belong in build-character-cast')
        path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'status':result['status'],'passed':len(passed),'failed':len(failed),
        'failure_groups':collections.Counter(item['error'] for item in failed)},indent=2))
    return bool(failed)

if __name__=='__main__':raise SystemExit(main())
