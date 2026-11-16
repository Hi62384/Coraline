"""Preserve selected original imagegen masters and copy resource sources intact.

No image editing/re-encoding or release optimization. Normal processResources
still optimizes generated runtime copies; final broad size work remains last.
"""
from pathlib import Path
import hashlib,json
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
MANIFEST=ROOT/'artwork/postgame/materials/manifest.json'

def destination(relative,allowed):
    candidate=(ROOT/relative).resolve()
    if not candidate.is_relative_to((ROOT/allowed).resolve()):raise ValueError('Unexpected asset destination: '+relative)
    return candidate

def copy_intact(source,target):
    raw=source.read_bytes()
    if len(raw)>16*1024*1024:raise ValueError('Oversized material master')
    digest=hashlib.sha256(raw).hexdigest();target.parent.mkdir(parents=True,exist_ok=True)
    if target.exists():
        if hashlib.sha256(target.read_bytes()).hexdigest()!=digest:raise ValueError('Existing asset differs; no overwrite: '+str(target))
    else:
        with target.open('xb') as stream:stream.write(raw)
    return digest,len(raw)

def main():
    manifest=json.loads(MANIFEST.read_text(encoding='utf-8'));audit=[];seen=set()
    for row in manifest['records']:
        if row['name'] in seen:raise ValueError('Duplicate material name')
        seen.add(row['name']);source=Path(row['generated_source']).resolve(strict=True)
        with Image.open(source) as image:
            if image.width!=image.height or image.width>4096:raise ValueError('Expected bounded square material')
            item={'name':row['name'],'size':list(image.size),'mode':image.mode,'runtime_limit_applied':False}
            if row['transparent_background']:
                if 'A' not in image.getbands():raise ValueError('Missing cutout alpha: '+row['name'])
                alpha=image.getchannel('A');extrema=alpha.getextrema()
                if extrema!=(0,255):raise ValueError('Cutout lacks fully clear/opaque pixels: '+row['name']+' '+str(extrema))
                item['alpha_extrema']=list(extrema);item['transparent_fraction']=alpha.histogram()[0]/(image.width*image.height)
        digest,size=copy_intact(source,destination(row['master'],'artwork/postgame/materials'))
        copy_intact(source,destination(row['runtime_source'],'src/main/resources/assets/coraline/textures/block/postgame'))
        item.update(sha256=digest,bytes=size)
        if 'edit_reference' in row:
            ref=row['edit_reference'];copy_intact(Path(ref['generated_source']).resolve(strict=True),destination(ref['master'],'artwork/postgame/materials'))
        audit.append(item)
    (MANIFEST.parent/'source-audit.json').write_text(json.dumps(audit,indent=2)+'\n',encoding='utf-8')
    print('Preserved/copied '+str(len(audit))+' original materials with exact hashes; alpha verified. Runtime export, tiling and in-game QA remain pending.')
    return 0

if __name__=='__main__':raise SystemExit(main())
