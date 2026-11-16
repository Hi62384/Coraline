"""Preserve fresh native QA PNG evidence before later checkpoints reuse filenames."""
import argparse,hashlib,json,shutil
from datetime import datetime,timezone
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def archive(path):
    report=Path(path).resolve()
    if not report.is_relative_to(ROOT/'build-character-cast/prepared-native') or report.name!='report.json':raise ValueError('Expected native QA report')
    result=json.loads(report.read_text())
    if result.get('status')!='passed' or result.get('exit_code')!=0 or not result.get('clean_runtime_shutdown'):raise ValueError('Clean passed native run required')
    start=datetime.strptime(report.parent.name,'%Y%m%dT%H%M%SZ').replace(tzinfo=timezone.utc).timestamp()
    end=report.stat().st_mtime
    cwd=Path(result['cwd']).resolve()
    if cwd.parent!=ROOT or not cwd.name.startswith('run-tests-isolated-'):raise ValueError('Expected owned disposable QA directory')
    bases=[(ROOT/'artwork',Path()),(cwd/'artwork',Path('world'))]
    dest=ROOT/'build-character-cast/runtime-captures'/report.parent.name;dest.mkdir(parents=True,exist_ok=True)
    rows=[];total=0
    for base,prefix in bases:
      for folder in base.glob('*_qa'):
        if not folder.is_dir() or folder.is_symlink():continue
        for image in folder.rglob('*.png'):
            if image.is_symlink() or not image.resolve().is_relative_to(base.resolve()):raise ValueError('Capture path escaped artwork')
            stat=image.stat()
            if not start<=stat.st_mtime<=end:continue
            total+=stat.st_size
            if stat.st_size>32*1024*1024 or total>256*1024*1024:raise ValueError('Unexpected capture size')
            target=dest/prefix/image.relative_to(base);target.parent.mkdir(parents=True,exist_ok=True)
            digest=hashlib.sha256(image.read_bytes()).hexdigest()
            if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest()!=digest:raise ValueError('Existing capture evidence differs')
            if not target.exists():shutil.copy2(image,target)
            rows.append({'source':str(image.relative_to(ROOT)),'copy':str(target.relative_to(ROOT)),'sha256':digest,'bytes':stat.st_size})
    manifest=dest/'manifest.json'
    if rows:manifest.write_text(json.dumps({'native_report':str(report),'scope':'Fresh rendered captures only; inspect images independently of completion marker','captures':rows},indent=2)+'\n',encoding='utf-8')
    print(report.parent.name+': '+str(len(rows))+' fresh captures preserved',flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('reports',nargs='+');args=p.parse_args()
    for report in args.reports:archive(report)
