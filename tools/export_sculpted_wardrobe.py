"""Production downsampling and alpha validation; no repainting of rendered artwork."""
from pathlib import Path
import json,math
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/coraline'
ART=ROOT/'artwork/sculpted_wardrobe'
manifest=json.loads((ART/'manifest.json').read_text())
assert len(manifest)==87
references=json.loads((ART/'reference_sources.json').read_text())
import shutil
for entry in references['icons']:
    destination=ART/'icons'/(entry['id']+'.png')
    if not destination.exists() and Path(entry['path']).exists():shutil.copy2(entry['path'],destination)
    assert destination.exists(),entry['id']
for name,entry in references['fabrics'].items():
    destination=ART/(name+'_fabric_source.png')
    if Path(entry['path']).exists():shutil.copy2(entry['path'],destination)
    with Image.open(destination) as fabric:
        if name=='comfy_silk':
            # Generated swatch has rounded transparent corners. Use its opaque
            # central tile, not the cutout border, for the garment fabric.
            w,h=fabric.size;fabric=fabric.crop((w//4,h//4,3*w//4,3*h//4))
        atlas=Image.new('RGB',(512,512),'white')
        atlas.paste(fabric.convert('RGB').resize((384,512),Image.Resampling.LANCZOS),(0,0))
        atlas.save(A/'textures/models/sculpted'/(name+'.png'))
        (A/'textures/models/sculpted'/(name+'.png.mcmeta')).write_text('{"texture":{"blur":true,"clamp":false}}\n')
fabric_source=ART/'fabric_source.png'
if fabric_source.exists():
    # Small contrast range prevents fine woven grain sparkling at a distance.
    with Image.open(fabric_source) as fabric:
        fabric=fabric.convert('RGB').resize((256,256),Image.Resampling.LANCZOS)
        fabric.point(lambda value:236+round(value*19/255)).save(A/'textures/models/sculpted/fabric.png')
for item in manifest:
    name=item['id'];source=ART/'icons'/(name+'.png')
    assert source.exists(), 'Missing illustrated icon: '+name
    with Image.open(source) as image:
        assert image.mode=='RGBA' and image.getextrema()[3][0]==0 and image.getbbox(),name
        image.resize((256,256),Image.Resampling.LANCZOS).save(A/'textures/item'/(name+'.png'))
    (A/'models/item'/(name+'.json')).write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/'+name}},indent=2)+'\n')
    mesh=json.loads((A/'sculpted'/(name+'.json')).read_text())
    for part in mesh['meshes']:
        assert len(part['vertices'])==len(part['uv']),name
        assert all(math.isfinite(n) for vertex in part['vertices'] for n in vertex),name
        assert all(0<=index<len(part['vertices']) for face in part['faces'] for index in face),name
    assert item['polygons']<25000,(name,item['polygons'])
for start,end,label in ((0,40,'sets_1'),(40,76,'sets_2'),(76,87,'single_pieces')):
    entries=manifest[start:end];cols=4;tile=240
    sheet=Image.new('RGB',(cols*tile,math.ceil(len(entries)/cols)*(tile+36)),'#242633');draw=ImageDraw.Draw(sheet)
    for i,item in enumerate(entries):
        x=i%cols*tile;y=i//cols*(tile+36)
        with Image.open(A/'textures/item'/(item['id']+'.png')) as im:
            im=im.resize((tile,tile),Image.Resampling.LANCZOS);sheet.paste(im,(x,y),im)
        labeltext=item['id'].replace('_',' ')
        draw.text((x+8,y+tile+3),labeltext[:34],fill='white')
        if len(labeltext)>34:draw.text((x+8,y+tile+16),labeltext[34:],fill='white')
    sheet.save(ART/('catalog_'+label+'.jpg'),quality=92)
print('87 alpha icons exported; all wearable meshes validated.')
