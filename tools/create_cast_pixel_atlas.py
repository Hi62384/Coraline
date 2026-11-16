"""Small, hand-authored pixel motifs for the block-built reference characters."""
from pathlib import Path
from PIL import Image, ImageDraw
import random
ROOT=Path(__file__).resolve().parents[1]
IDS=['wybie','other_wybie','bobinsky','other_bobinsky','miss_spink','miss_forcible','other_miss_spink','other_miss_forcible','evil_other_father','evil_other_bobinsky','evil_other_spink_forcible','evil_other_mother','beldam','coraline','cat']
SKINS=['#BD916D','#BD916D','#8AA8B1','#559AC1','#D4A587','#D6B59B','#D5AA81','#C99A7D','#B87635','#446F86','#93B581','#E4C9BA','#B9C5C6','#CBA57E','#1C222A']
HAIR=['#32242C','#32242C','#879192','#174679','#A54E74','#BCB49D','#A13443','#6D4833','#855928','#132433','#B9CC94','#13151F','#807443','#1D2E62','#1C222A']

def shift(hex_color,k):
    rgb=[int(hex_color[i:i+2],16) for i in (1,3,5)]
    return tuple(max(0,min(255,v+k)) for v in rgb)+(255,)

def face(name,index):
    im=Image.new('RGBA',(16,16),SKINS[index]);d=ImageDraw.Draw(im)
    shade=shift(SKINS[index],-12);light=shift(SKINS[index],9)
    d.rectangle((0,0,1,15),fill=shade);d.rectangle((14,0,15,15),fill=shade)
    d.rectangle((2,14,13,15),fill=shade);d.line((3,3,12,3),fill=light)
    if name in ('wybie','other_wybie','coraline'):
        d.rectangle((0,0,15,2),fill=HAIR[index]);d.rectangle((0,2,2,7),fill=HAIR[index]);d.rectangle((13,2,15,7),fill=HAIR[index])
        for x,y in [(2,3),(3,3),(4,2),(7,3),(9,2),(12,3)]:d.rectangle((x,y,x+1,y+1),fill=HAIR[index])
    elif 'spink' in name or 'forcible' in name:
        d.rectangle((0,0,15,1),fill=HAIR[index]);d.rectangle((0,2,1,5),fill=HAIR[index]);d.rectangle((14,2,15,5),fill=HAIR[index])
    elif name in ('evil_other_mother','beldam'):
        d.rectangle((0,0,15,1),fill=HAIR[index]);d.rectangle((0,2,1,12),fill=HAIR[index]);d.rectangle((14,2,15,12),fill=HAIR[index])
    button=name.startswith('other_') or name.startswith('evil_') or name=='beldam'
    for left in [3,10]:
        if button:
            x=left-1;y=6
            d.rectangle((x+1,y,x+3,y+4),fill='#070B12');d.rectangle((x,y+1,x+4,y+3),fill='#070B12')
            d.line((x+1,y+1,x+3,y+1),fill='#404652')
            for xx,yy in [(x+1,y+2),(x+3,y+2),(x+1,y+3),(x+3,y+3)]:d.point((xx,yy),fill='#998E75')
        else:
            d.rectangle((left,6,left+2,8),fill='#F0E9D9')
            iris='#50624C' if 'wybie' in name else '#786040' if name=='coraline' else '#3C7D84'
            d.rectangle((left+1,6,left+2,8),fill=iris);d.line((left+2,6,left+2,8),fill='#22242B')
            d.point((left+1,6),fill='#D6D8B9')
        d.line((left,4,left+2,4),fill=HAIR[index])
    d.rectangle((7,9,8,10),fill=shade);d.point((7,9),fill=light)
    lips='#924844' if ('spink' in name or 'forcible' in name) else '#644435'
    d.line((5,12,10,12),fill=lips);d.point((4,11),fill=lips);d.point((11,11),fill=lips)
    if name.startswith('evil_') or name=='beldam':
        d.rectangle((5,11,10,13),fill='#2A2029');d.line((6,11,9,11),fill='#D1C0A1')
        d.point((5,11),fill='#674A4D');d.point((10,13),fill='#674A4D')
    if name=='evil_other_father':
        d.line((5,13,10,13),fill='#46342D');d.line((6,12,9,12),fill=SKINS[index])
    if 'bobinsky' in name:
        d.line((2,10,6,11),fill='#20537D' if button else '#56656C');d.line((9,11,13,10),fill='#20537D' if button else '#56656C')
        d.point((1,9),fill='#20537D');d.point((14,9),fill='#20537D')
    if name=='beldam':
        for points in [[(5,1),(6,4),(5,6)],[(11,2),(10,5),(11,8)],[(7,10),(6,14)]]:d.line(points,fill='#566975',width=1)
    if name=='cat':
        d.rectangle((0,0,15,15),fill=SKINS[index])
        for x in [3,10]:
            d.rectangle((x,5,x+3,8),fill='#80BAC8');d.line((x+2,5,x+2,8),fill='#111A23');d.point((x,5),fill='#ABD8DB')
        d.rectangle((7,9,8,9),fill='#AC788A');d.line((6,11,9,11),fill='#707B83')
        for yy in [9,11]:d.line((0,yy,4,yy+1),fill='#9CAEB2');d.line((11,yy+1,15,yy),fill='#9CAEB2')
    return im.resize((32,32),Image.Resampling.NEAREST)

def generate():
    atlas=Image.new('RGBA',(512,128),(255,255,255,255))
    for i,name in enumerate(IDS):atlas.paste(face(name,i),(i*32,0))
    # A restrained repeating fabric pattern; never blurred at export or runtime.
    rng=random.Random(1948);draw=ImageDraw.Draw(atlas)
    for y in range(32,64):
        for x in range(32):
            n=rng.choice([231,240,247,255,255,255]);draw.point((x,y),fill=(n,n,n,255))
    # Checker motifs for the Other Mother's angular dress.
    for y in range(64,96):
        for x in range(32,64):
            n=235 if ((x//3+y//3)%2==0 and (x%3==1 or y%3==1)) else 54
            draw.point((x,y),fill=(n,n,n,255))
    # An alternate green/pink face for the double-headed cocoon creature.
    twin=face('evil_other_spink_forcible',10);pixels=twin.load()
    for y in range(32):
        for x in range(32):
            r,g,b,a=pixels[x,y]
            if g>r and g>b:pixels[x,y]=(min(255,g+8),max(0,r-5),min(255,b+35),a)
    atlas.paste(twin,(15*32,0))
    path=ROOT/'src/main/resources/assets/coraline/characters/cast_atlas.png';path.parent.mkdir(parents=True,exist_ok=True);atlas.save(path,optimize=True)
    print('Authored 512x128 nearest-neighbor pixel atlas:',path.stat().st_size,'bytes')

if __name__=='__main__':generate()
