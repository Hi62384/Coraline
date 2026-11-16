"""Small read-only status for the long local voice jobs, never acceptance proof."""
import argparse,json,time
from pathlib import Path
import check_numbered_candidate as monitor
ROOT=Path(__file__).resolve().parents[1]

def read_json(path):
    for attempt in range(3):
        try:return json.loads(path.read_text(encoding='utf-8'))
        except json.JSONDecodeError:
            if attempt==2:return {'status':'receipt_being_updated'}
            time.sleep(.05)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('generation_run');parser.add_argument('validation_run',nargs='?');args=parser.parse_args()
    base=ROOT/'build-character-cast/compact-voice'
    directories=[]
    for name in (args.generation_run,args.validation_run):
        if name is None:continue
        path=(base/name).resolve()
        if path.parent!=base.resolve() or not path.is_dir():raise ValueError('Expected one voice-run directory name')
        directories.append(path)
    manifest=read_json(ROOT/'tools/full_mother_voice_manifest.json')
    generation=read_json(ROOT/manifest['output']/'generation_report.json')
    rows={}
    log=directories[1]/'output.log' if len(directories)>1 else None
    if log is not None and log.exists():
        for line in log.read_text(encoding='utf-8',errors='replace').splitlines():
            if not line.startswith('{'):continue
            try:row=json.loads(line)
            except json.JSONDecodeError:continue
            if 'file' in row:rows[row['file']]=row
    statuses=[read_json(path/'report.json').get('status','running') if (path/'report.json').exists() else 'running' for path in directories]
    if len(statuses)==1:statuses.append('deferred_until_generation_finishes')
    print(json.dumps({'expected':len(manifest['states']),'generated_receipts':len(generation.get('lines',{})),
        'speech_checks_emitted':len(rows),'flagged':[name for name,row in rows.items() if row.get('needs_review')],
        'generation_job':statuses[0],'validation_job':statuses[1],'available_gib':round(monitor.free_gib(),2)}))

if __name__=='__main__':main()
