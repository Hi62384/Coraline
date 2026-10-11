"""UV-native worn fabric textures, model JSON and translated clothing descriptions.

Icons are separately generated artwork; this script never paints over their source images.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import json, random

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/coraline'
STYLES={
 'midnight_coat':('#232c58','#bacfea','Faster movement and a small jump boost around midnight. See skylit terrain at night, not caves.'),
 'star_shirt':('#294c92','#f9d981','Casts a gentle moving light around you while worn.'),
 'raincoat':('#e8b62e','#fff1a0','Move much faster while it is raining.'),
 'comfy_pajamas':('#8c76b5','#eadfe8','Slowly restores health while worn.'),
 'garden_overalls':('#4b7150','#dcc597','Eating restores health equal to half the food\'s nutrition.'),
 'circus_jacket':('#a83440','#ebbf62','Jump higher with a ringmaster\'s flourish.'),
 'silent_slippers':('#514263','#ad93bb','Halves normal mob detection distance. Does not prevent retaliation.'),
 'feather_boots':('#ece3c7','#a9d9e4','Drift gently downward and land safely.'),
 'top_hat':('#252b38','#91404e','A very distinguished finishing touch.')}

def rgb(h):return tuple(bytes.fromhex(h[1:]))
def shade(c,n):return tuple(max(0,min(255,x+n)) for x in c)
for number,(name,(base,trim,tooltip)) in enumerate(STYLES.items()):
    base,trim=rgb(base),rgb(trim);im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im);rng=random.Random(712+number)
    def rect(box,c):d.rectangle(box,fill=(*c,255))
    def fabric(x,y,w,h,c):
        for yy in range(h):
            for xx in range(w):
                n=rng.choice([-2,0,0,2])+(-12 if xx==0 or yy==h-1 else 9 if xx==w-1 else 0)
                rect((x+xx,y+yy,x+xx,y+yy),shade(c,n))
    def cube(u,v,w,h,depth,c):
        fabric(u+depth,v,w,depth,shade(c,9));fabric(u+depth+w,v,w,depth,shade(c,-14))
        fabric(u,v+depth,depth,h,shade(c,-7));fabric(u+depth,v+depth,w,h,c)
        fabric(u+depth+w,v+depth,depth,h,shade(c,-10));fabric(u+depth*2+w,v+depth,w,h,shade(c,-4))
    def star(x,y,c):
        for dx,dy in [(0,-2),(0,-1),(-2,0),(-1,0),(0,0),(1,0),(2,0),(-1,1),(1,1),(-1,2),(1,2)]:rect((x+dx,y+dy,x+dx,y+dy),c)
    cube(16,16,8,12,4,base)
    for u,v in [(40,16),(40,40)]:
        cube(u,v,4,12,4,base)
        for x in range(u,u+16):rect((x,v+12,x,v+13),trim)
    cube(0,32,4,12,4,base)
    if name=='top_hat':
        im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
        cube(0,0,12,1,12,base);cube(0,16,9,9,9,base)
        rect((0,30,35,33),trim);rect((0,30,35,30),shade(trim,20))
        rect((36,18,43,24),(209,177,83));rect((37,20,37,21),base)
    elif name=='star_shirt':
        star(24,25,trim);rect((22,20,25,20),trim)
        for x,y in [(33,23),(37,27),(34,30)]:rect((x,y,x,y),trim)
    elif name in ('midnight_coat','circus_jacket'):
        for y in range(20,27):
            left=20+(y-20)//2;right=27-(y-20)//2
            rect((left,y,left,y),trim);rect((right,y,right,y),trim)
        for y in (24,27,30):
            rect((22,y,22,y),trim);rect((25,y,25,y),trim)
            if name=='circus_jacket':rect((22,y,25,y),trim)
        rect((20,31,27,31),trim);rect((0,40,15,40),trim)
        if name=='midnight_coat':
            for x,y in [(33,23),(37,26),(34,29)]:rect((x,y,x,y),trim)
        else:cube(40,0,4,1,4,trim)
    elif name=='raincoat':
        cube(0,0,8,4,3,base);rect((23,20,24,31),shade(base,-30))
        for y in (23,26,29):rect((23,y,24,y),trim)
        for x in (20,25):d.rectangle((x,27,x+2,29),outline=(*shade(base,-35),255))
    elif name=='comfy_pajamas':
        rect((23,20,23,31),trim);rect((20,31,27,31),trim)
        for x,y in [(21,24),(26,27),(34,24),(37,29),(5,39),(10,43)]:
            rect((x,y,x+1,y+1),trim);rect((x+1,y,x+1,y),base)
        for y in (22,25,28):rect((24,y,24,y),trim)
    elif name=='garden_overalls':
        for u,v in [(40,16),(40,40)]:cube(u,v,4,10,4,trim)
        rect((20,20,27,23),trim)
        for x in (21,26):rect((x,20,x,25),base);rect((x,24,x,24),(222,179,80))
        d.rectangle((22,25,25,28),outline=(*shade(base,28),255));rect((23,26,24,27),shade(base,-15))
        for x in (4,12):rect((x,42,x+2,44),trim)
    elif name=='silent_slippers':
        cube(0,32,4,3,5,base);rect((0,38,17,39),trim)
    elif name=='feather_boots':
        cube(0,32,4,6,5,base);rect((0,37,17,38),trim)
        cube(20,38,1,5,4,(242,244,231));cube(32,38,1,4,2,(224,234,228))
        rect((5,40,6,41),(203,166,76))
    path=A/'textures/models/armor'/f'{name}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
    (A/'models/item'/f'{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'coraline:item/{name}'}},indent=2)+'\n')

langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text())
for name,(_,_,tooltip) in STYLES.items():
    lang['item.coraline.'+name]=name.replace('_',' ').title()
    lang['tooltip.coraline.'+name]=tooltip
langpath.write_text(json.dumps(lang,indent=2)+'\n')
print('Nine clothing UV textures, item models and tooltips exported.')
