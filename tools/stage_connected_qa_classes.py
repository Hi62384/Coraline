"""Copy every production class unchanged and only connected-host QA helpers.

Uses each class's actual SourceFile attribute, including multiple top-level
classes and anonymous/nested classes. The complete compiled cache is retained.
This does not filter production code, assets, native server tests or releases.
"""
from pathlib import Path
import hashlib,json,shutil,struct,time

def source_file(data):
    if data[:4]!=b'\xca\xfe\xba\xbe':raise ValueError('Not a Java class')
    offset=8;count=struct.unpack_from('>H',data,offset)[0];offset+=2;pool={};i=1
    while i<count:
        tag=data[offset];offset+=1
        if tag==1:
            size=struct.unpack_from('>H',data,offset)[0];offset+=2;pool[i]=data[offset:offset+size].decode('utf-8',errors='replace');offset+=size
        elif tag in (3,4,9,10,11,12,17,18):offset+=4
        elif tag in (5,6):offset+=8;i+=1
        elif tag in (7,8,16,19,20):offset+=2
        elif tag==15:offset+=3
        else:raise ValueError('Unknown Java constant tag '+str(tag))
        i+=1
    offset+=6
    interfaces=struct.unpack_from('>H',data,offset)[0];offset+=2+2*interfaces
    def attributes(at,retain=False):
        n=struct.unpack_from('>H',data,at)[0];at+=2;source=None
        for _ in range(n):
            name,size=struct.unpack_from('>HI',data,at);at+=6
            if retain and pool.get(name)=='SourceFile':
                if size!=2:raise ValueError('Invalid SourceFile attribute')
                source=pool[struct.unpack_from('>H',data,at)[0]]
            at+=size
        return at,source
    for _ in range(2):
        members=struct.unpack_from('>H',data,offset)[0];offset+=2
        for _ in range(members):offset,_=attributes(offset+6)
    offset,source=attributes(offset,True)
    if offset!=len(data) or not source or Path(source).name!=source or not source.endswith('.java'):raise ValueError('Missing/invalid Java source identity')
    return source

def stage(root,original):
    original=original.resolve(strict=True);base=(root/'build-character-cast/cat-focused-compile').resolve()
    if original.parent.parent!=base or original.name!='classes':raise ValueError('Expected isolated full-source compiler classes')
    dest=root/'build-character-cast/connected-class-staging'/time.strftime('%Y%m%dT%H%M%SZ',time.gmtime());dest.mkdir(parents=True,exist_ok=False)
    allowed={'com/coraline/multiplayer/ConnectedStoryVisualCheck.java','com/coraline/feast/ConnectedActivityVisualCheck.java'}
    production=[];qa=[];omitted=[];sources=set()
    for path in original.rglob('*'):
        if not path.is_file():continue
        if path.is_symlink() or not path.resolve().is_relative_to(original):raise ValueError('Compiled class path escaped')
        relative=path.relative_to(original)
        if path.suffix=='.class':
            source=relative.parent/source_file(path.read_bytes());main=(root/'src/main/java'/source).is_file()
            keep=main or source.as_posix() in allowed or source.parent.as_posix()=='com/coraline/qa'
            if not keep:omitted.append(str(relative));continue
            if main:sources.add(source.as_posix())
        target=dest/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,target)
        before=hashlib.file_digest(path.open('rb'),'sha256').hexdigest();after=hashlib.file_digest(target.open('rb'),'sha256').hexdigest()
        if before!=after:raise ValueError('Staged bytecode changed')
        row={'file':str(relative),'sha256':before}
        (production if path.suffix=='.class' and main else qa).append(row)
    expected={p.relative_to(root/'src/main/java').as_posix() for p in (root/'src/main/java').rglob('*.java') if p.name!='package-info.java'}
    if expected-sources:raise ValueError('Production source lost from staged classes: '+str(sorted(expected-sources)))
    report=dest/'staging-report.json';report.write_text(json.dumps({'original':str(original),'status':'all_production_classes_byte_identical','production_sources':len(sources),'production_classes':production,'qa_helpers_and_resources':qa,'omitted_unrelated_test_classes':omitted},indent=2)+'\n')
    print(f'Connected host class staging: {len(production)} byte-identical production classes; {len(omitted)} unrelated test classes omitted; report {report}',flush=True)
    return dest,str(report)
