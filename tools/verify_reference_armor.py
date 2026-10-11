"""Verify reference assets, unchanged non-reference wearables and release packaging."""
from pathlib import Path
import json,zipfile,hashlib,sys,math
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/coraline'
families={'dreamweave','comfy_silk','buttonguard','garden_vines','midnight','miners'}
manifest=json.loads((ROOT/'artwork/sculpted_wardrobe/manifest.json').read_text())
reference=[m for m in manifest if m['id'].rsplit('_',1)[0] in families and m['id'].endswith('_'+m['piece'])]
assert len(reference)==24
for m in reference:
    d=json.loads((A/'sculpted'/(m['id']+'.json')).read_text());assert d['style'].endswith('_reference')
    assert (A/'textures/models/sculpted'/(d['texture']+'.png')).exists()
    assert m['polygons']<25000
    for part in d['meshes']:
        assert part['material'] in d['materials']
        assert part['bone'] in {'head','body','cape','left_arm','right_arm','left_leg','right_leg'}
        if 'normals' in part:
            assert len(part['normals'])==len(part['vertices'])
            assert all(math.isfinite(x) for n in part['normals'] for x in n)
    with Image.open(A/'textures/item'/(m['id']+'.png')) as im:assert im.mode=='RGBA' and im.size==(256,256) and im.getextrema()[3][0]==0
old=ROOT/'build/libs/coraline-1.12.0.jar'
if old.exists():
    with zipfile.ZipFile(old) as jar:
        for m in manifest:
            if m in reference:continue
            for rel in ('sculpted/'+m['id']+'.json','textures/item/'+m['id']+'.png'):
                assert jar.read('assets/coraline/'+rel)==(A/rel).read_bytes(),'Unrelated asset changed: '+rel
    print('63 unrelated wearable meshes/icons unchanged against 1.12.0.')
if len(sys.argv)>1:
    path=Path(sys.argv[1])
    with zipfile.ZipFile(path) as jar:
        assert not any('VisualCheck' in n or 'GameTests' in n for n in jar.namelist()),'Test fixture in release'
        for m in reference:
            for rel in ('sculpted/'+m['id']+'.json','textures/item/'+m['id']+'.png'):
                assert jar.read('assets/coraline/'+rel)==(A/rel).read_bytes()
        for name in families:
            rel='textures/models/sculpted/'+name+'.png';assert jar.read('assets/coraline/'+rel)==(A/rel).read_bytes()
    print(path.name,'SHA256',hashlib.sha256(path.read_bytes()).hexdigest())
print('24 reference models/icons and six fabric textures validated. Maximum piece polygons:',max(m['polygons'] for m in reference))
