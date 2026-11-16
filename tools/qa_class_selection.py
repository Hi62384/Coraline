"""Byte-preserving QA-only discovery filter, never a production build filter."""
from pathlib import Path
import hashlib,re,shutil

def stage_selected(source:Path,destination:Path,selected:set[str]):
    if not selected or any(not re.fullmatch(r'[A-Za-z0-9_]+GameTests\.java',name) for name in selected):
        raise RuntimeError('Invalid actual GameTest selection')
    if destination.exists() or source.is_symlink():raise RuntimeError('QA staging requires a fresh destination and original class root')
    families={name.removesuffix('.java') for name in selected}
    files=list(source.rglob('*'))
    if any(p.is_symlink() for p in files):raise RuntimeError('Linked compiler output refused')
    found={p.stem for p in files if p.is_file() and p.suffix=='.class' and p.stem in families}
    if found!=families:raise RuntimeError('Selected native tests were not all compiled')
    # Some actual test classes expose helpers to other test classes. Never
    # silently strip such a dependency and turn a product check into NoClassDef.
    required=set()
    for path in files:
        if path.is_file() and path.suffix=='.class' and path.name.split('$',1)[0].removesuffix('.class') in families:
            required.update(match.decode('ascii').rsplit('/',1)[-1].split('$',1)[0] for match in re.findall(rb'com/coraline/[A-Za-z0-9_/$]+GameTests',path.read_bytes()))
    if required-families:raise RuntimeError('Include shared GameTest helper classes in the selection: '+', '.join(sorted(required-families)))
    destination.mkdir(parents=True,exist_ok=False)
    copied=0
    for path in files:
        if not path.is_file():continue
        family=path.name.split('$',1)[0].removesuffix('.class')
        if path.suffix=='.class' and family.endswith('GameTests') and family not in families:continue
        target=destination/path.relative_to(source);target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(path,target)
        if hashlib.sha256(path.read_bytes()).digest()!=hashlib.sha256(target.read_bytes()).digest():raise RuntimeError('QA class bytes changed during staging')
        copied+=1
    return copied
