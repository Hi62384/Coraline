"""Editable pixel-art wardrobe, using exact vanilla classic/slim skin UVs.

Independent from create_family_assets so regenerating skins cannot overwrite blocks.
32 saved identities per role; eight garment designs and eight haircuts, not recolors.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import random

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/coraline/textures/entity/parents'
TONES = ['#edc5a4', '#ce9876', '#ab7154', '#794c39', '#eccbb4', '#c08860', '#925a43', '#553a30']
HAIR = ['#36271f', '#171b22', '#89472d', '#c19856', '#57433a', '#aaa09a', '#542e28', '#342e39']
PALETTES = [('#b33f55','#f1d296'),('#188a8c','#e9b25b'),('#3669ad','#f2d7a5'),('#d99031','#fff0d2'),
            ('#507d35','#dbb477'),('#774091','#eab388'),('#284967','#d67647'),('#bc5743','#f1c193'),
            ('#ba668f','#f3d3b8'),('#377952','#b4d5a8'),('#914238','#e8b65b'),('#4b59a3','#cbd7dd')]

def rgb(c): return tuple(bytes.fromhex(c.lstrip('#')))
def shade(c, n): return tuple(max(0,min(255,v+n)) for v in c)

def skin(mom, index):
    im=Image.new('RGBA',(64,64)); d=ImageDraw.Draw(im)
    rng=random.Random(index+int(mom)*137)
    tone=rgb(TONES[rng.randrange(8)]); hair=rgb(HAIR[rng.randrange(8)])
    cloth,accent=map(rgb,PALETTES[rng.randrange(len(PALETTES))])
    pants=rgb(['#263c5b','#614331','#304e45','#494758'][index%4])
    style=index%8; haircut=(index//4+index%4)%8
    def rect(box,c): d.rectangle(box,fill=(*c,255))
    def face(x,y,w,h,c):
        for yy in range(h):
            for xx in range(w):
                # Deliberate edge shading and sparse fabric texture, not flat fill.
                n=(-10 if xx==0 or yy==h-1 else 5 if xx==w-1 else 0)+rng.choice([-2,0,0,2])
                im.putpixel((x+xx,y+yy),(*shade(c,n),255))
    def cube(u,v,w,h,depth,c):
        face(u+depth,v,w,depth,c);face(u+depth+w,v,w,depth,shade(c,-12))
        face(u,v+depth,depth,h,shade(c,-7));face(u+depth,v+depth,w,h,c)
        face(u+depth+w,v+depth,depth,h,shade(c,-10));face(u+depth*2+w,v+depth,w,h,shade(c,-5))
    cube(0,0,8,8,8,tone)
    # Scalp, back and sides; face fringe varies in length and silhouette.
    face(8,0,8,8,hair);face(24,8,8,8,hair)
    for x in (0,16):face(x,8,8,5 if mom else 3,hair)
    face(8,8,8,1 if haircut==5 else 2,hair)
    for x in range(8):
        fringe=[2+(x//3),4-x//2,2+x%2,1+(x%3==0),2+(x>4),1,3-(x//3),2+(x<3)][haircut]
        for y in range(fringe):rect((8+x,8+y,8+x,8+y),shade(hair,8 if x%3==0 else 0))
    if mom:
        for x in ([8,15] if haircut%3==0 else [8] if haircut%3==1 else [15]):
            face(x,10,1,6,hair)
        # Raised hair layer: visible side locks, parting and back, transparent face.
        face(40,0,8,8,hair);face(56,8,8,8 if haircut<4 else 5,hair)
        face(32,8,8,7 if haircut<4 else 4,hair);face(48,8,8,7 if haircut<4 else 4,hair)
        if haircut in (2,6):rect((48,10,49,11),accent) # hair clip
    else:
        if haircut in (1,3,6):
            face(10,14,4,2,hair);rect((11,14,12,14),shade(tone,-25))
        if haircut==4:rect((10,13,13,13),hair) # moustache
        face(40,0,8,8,hair)
    eyes=rgb(['#516b75','#4d6038','#704d33','#393241'][index%4])
    for x in (9,13):
        rect((x,11,x+1,11),(235,228,211));rect((x+(x==9),11,x+(x==9),11),eyes)
        rect((x,10,x+1,10),shade(hair,12))
    rect((11,12,12,12),shade(tone,-18));rect((11,14,12,14),shade(tone,-47))
    if index%5==0: # freckles
        for x in (9,14):rect((x,12,x,12),shade(tone,-36))
    if index%7==0: # glasses on raised layer
        for x in (41,45):d.rectangle((x,10,x+2,12),outline=(*shade(hair,20),255))
        rect((44,11,44,11),shade(hair,20))
    cube(16,16,8,12,4,cloth)
    for u,v in ((0,16),(16,48)):
        cube(u,v,4,12,4,pants)
        for sx in (u,u+4,u+8,u+12):
            face(sx,v+13,4,3,shade(hair,-8));rect((sx,v+15,sx+3,v+15),(38,32,30))
    armwidth=3 if mom else 4
    for u,v in ((40,16),(32,48)):
        cube(u,v,armwidth,12,4,cloth)
        for sx,w in ((u,4),(u+4,armwidth),(u+4+armwidth,4),(u+8+armwidth,armwidth)):
            face(sx,v+12,w,4,tone);rect((sx,v+11,sx+w-1,v+11),accent)
    # Different garments, not just the same shirt in different colors.
    rect((23,20,24,21),tone)
    if style==0: # cardigan / blazer, contrasting shirt and lapels
        face(22,22,4,8,accent)
        for y in range(22,26):rect((21+(y-22)//2,y,21+(y-22)//2,y),shade(cloth,-25))
        for y in (25,28):rect((26,y,26,y),(213,180,104))
        rect((20,28,21,28),accent)
    elif style==1: # striped knit
        for y in (23,26,29):rect((20,y,27,y),accent)
        rect((22,21,25,21),shade(cloth,-25))
    elif style==2: # overalls and brass buckles
        face(21,25,6,6,pants)
        for x in (21,26):rect((x,21,x,27),pants);rect((x,24,x,24),(218,177,73))
        d.rectangle((23,26,25,28),outline=(*shade(pants,25),255))
    elif style==3: # floral blouse / patterned shirt
        for x,y in ((21,23),(25,25),(22,28),(26,30)):
            rect((x-1,y,x+1,y),accent);rect((x,y-1,x,y+1),accent);rect((x,y,x,y),(201,145,65))
    elif style==4: # waistcoat with tie
        face(21,23,6,8,shade(cloth,-28));rect((23,22,24,26),accent)
        rect((23,22,24,22),(116,48,48))
        for y in (27,29):rect((24,y,24,y),(203,180,133))
    elif style==5: # cable/argyle knit
        for x,y in ((22,24),(26,24),(24,28)):
            d.polygon([(x,y-2),(x+2,y),(x,y+2),(x-2,y)],fill=(*accent,255))
        rect((20,30,27,31),shade(cloth,-24))
    elif style==6: # checked flannel
        for x in (21,25):rect((x,22,x,31),shade(cloth,-35))
        for y in (23,27,30):rect((20,y,27,y),accent)
        for y in (24,27,30):rect((24,y,24,y),(225,213,188))
    else: # scarf and belted coat
        rect((20,21,27,22),accent);rect((25,22,26,27),accent)
        rect((20,29,27,29),hair);rect((23,29,24,29),(205,173,96))
        for x in (20,26):rect((x,27,x+1,27),shade(cloth,-25))
    # Raised cuffs and hem give clothing a layered silhouette, without opaque empty UV.
    for u,v in ((40,32),(48,48)):
        for sx,w in ((u,4),(u+4,armwidth),(u+4+armwidth,4),(u+8+armwidth,armwidth)):
            rect((sx,v+10,sx+w-1,v+11),accent)
    OUT.mkdir(parents=True,exist_ok=True)
    im.save(OUT/f'{"mom" if mom else "dad"}_{index}.png')
    return im

if __name__=='__main__':
    sheet=Image.new('RGBA',(64*8,64*8),(44,46,51,255))
    for role,mom in enumerate((True,False)):
        for i in range(32):sheet.alpha_composite(skin(mom,i),((i%8)*64,(role*4+i//8)*64))
        for i in range(32,64):skin(mom,i)
    sheet.resize((1024,1024),Image.Resampling.NEAREST).save(ROOT/'artwork/parent_skin_uv_catalog.png')
    print('Updated 64 mom and 64 dad skins; existing identity indices preserved.')
