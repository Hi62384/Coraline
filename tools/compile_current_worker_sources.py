"""Compile the three pending worker edits; no test or model execution.
The last successful all-main-source output supplies dependencies only when
every other production source still matches that exact compilation receipt.
"""
from pathlib import Path
import hashlib,json
import check_missing_parents_cat_compile as compiler

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'build-character-cast/cat-focused-compile/20261031T164629Z/report.json'
SOURCES=['tycoon/TycoonWorkers.java','tycoon/TycoonWorker.java','client/TycoonAutomationScreen.java']

def main():
    before=BASE.read_bytes();report=json.loads(before)
    if report.get('status')!='passed' or report.get('exit_code')!=0 or report.get('full_source_compile') is not True:raise RuntimeError('Successful all-source dependency receipt required')
    hashes={name.replace(chr(92),'/'):digest for name,digest in report['source_sha256'].items()}
    if len(hashes)!=len(report['source_sha256']):raise RuntimeError('Ambiguous source paths in dependency receipt')
    current={p.relative_to(ROOT).as_posix():p for p in (ROOT/'src/main/java/com/coraline').rglob('*.java')}
    if set(current)!=set(hashes):raise RuntimeError('Production source membership changed; compile all sources instead')
    allowed={'src/main/java/com/coraline/'+name for name in SOURCES}
    for name,path in current.items():
        if name not in allowed and hashlib.sha256(path.read_bytes()).hexdigest()!=hashes[name]:raise RuntimeError('Another dependency source changed: '+name)
    cached=ROOT/'build-character-cast/classpath/runClient_minecraftClasspath.txt'
    if hashlib.sha256(cached.read_bytes()).hexdigest()!=report['cached_classpath_sha256']:raise RuntimeError('Native dependency list changed')
    compiler.HEAP_MIB=192;compiler.SOURCES=SOURCES;compiler.TESTS=[];compiler.EXTRA_SOURCES=[]
    compiler.COMPILED=BASE.parent/'classes'
    compiler.BASE_SOURCE_COMPILE={'report':str(BASE.relative_to(ROOT)),'sha256':hashlib.sha256(before).hexdigest(),'all_other_production_sources_unchanged':True}
    return compiler.main()

if __name__=='__main__':raise SystemExit(main())
