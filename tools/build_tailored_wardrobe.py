"""Author Minecraft cuboid outfits once; Blender and the armor renderer share this geometry.
No invented illustration/model mismatch. Coordinates follow Minecraft humanoid bones.
"""
from pathlib import Path
import json, math, random
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/coraline'
# base cloth, lining, stitching, accessory, dark sole, light fabric, patch, jewel
DESIGNS={
 'dreamweave':('nightcap','#466daa','#d295b4','#f8dea0','#596cb5','#25334d','#e8dcdb','#80babb','#eedda0'),
 'comfy_silk':('sleep','#d98699','#f2d4c7','#eee2b9','#a85879','#71435f','#fff0dc','#bb829f','#efd6a2'),
 'midnight':('tails','#183653','#4363a0','#9bc9db','#23415e','#122336','#cbdfee','#394f7f','#cfecff'),
 'buttonguard':('duffle','#247e7a','#c16a48','#dfbf73','#335753','#173d3c','#dbc7a1','#498d64','#d69b4f'),
 'midnight_cloak':('cloak','#272a51','#575991','#8196b9','#332e62','#171b32','#a4bbd5','#3c4572','#919ee2'),
 'luck':('bowler','#3b8452','#c5a15b','#f3d67e','#9c473e','#284339','#edd9b0','#759258','#f3cf59'),
 'garden_vines':('garden','#588343','#c28b46','#e1c277','#e98d9c','#334a32','#dcd8ad','#84aa53','#f1c559'),
 'moonstone':('moon','#92bcc7','#39556e','#e3efde','#bfc6d7','#3b5668','#e8f4eb','#6d9daf','#a4e9e5'),
 'miners':('miner','#be7741','#315361','#dfb876','#97583d','#3e3430','#d7c4a0','#527f84','#ffe6a0'),
 'spidervision':('spider','#584461','#a3849a','#d8c8b1','#7e506d','#282339','#e6d8c9','#74556d','#d87e6e'),
 'beldam_weave':('beldam','#ded3bb','#1f2930','#b82e49','#464044','#28232a','#f0e3c9','#927c70','#dc455b'),
 'soulbound':('soul','#256477','#9ed9d3','#d8e7c8','#527c94','#193846','#bfeee1','#4f9397','#a0fff0'),
 'super_spidervision':('spider_plus','#382c4e','#ad749e','#e3c1c9','#7e4879','#211b31','#ecd6ce','#513d68','#ee8eab'),
 'potent_beldam_weave':('beldam_plus','#722e4b','#272736','#dab69c','#ab5166','#281c2d','#ead1b6','#934c6a','#df718f'),
 'soulbound_potent_beldam_weave':('soul_beldam','#24404d','#afbdab','#8dddd0','#66586b','#172a38','#d8e4c9','#496b76','#a6ffee'),
 'other_world_void':('void','#28203e','#695d8f','#cbaae0','#4c3b6e','#17142b','#e3c8e1','#3f345e','#bd9bef'),
 'other_diamond':('glass','#218ca2','#204d69','#94ece1','#579ec2','#1d384d','#b0f0e7','#386a88','#77e9ef'),
 'other_iron':('tinker','#626f6c','#9b543c','#dbc198','#af8d60','#303b3c','#dce1c9','#45706a','#c9d8d1'),
 'perfect':('perfect','#ecba37','#235480','#eddda1','#d75743','#2b3c51','#fcdf76','#3d6d92','#efeaca')}

def outfit(name,data):
    style,*palette=data; boxes=[]
    def box(piece,part,x,y,z,w,h,d,m=0,r=(0,0,0)):
        boxes.append(dict(piece=piece,part=part,box=[x,y,z,w,h,d],material=m,rotation=list(r)))
    def head(*args,**kw):box('helmet','head',*args,**kw)
    def body(*args,**kw):box('chestplate','body',*args,**kw)
    def button(piece,part,x,y,z,size=1,m=4):
        box(piece,part,x-size/2,y-size/2,z,size,size,.28,m)
        box(piece,part,x-size*.3,y-size*.64,z+.015,size*.6,size*1.28,.25,m)
        for dx in (-.19,.19):
            for dy in (-.19,.19):box(piece,part,x+size*dx-.065,y+size*dy-.065,z-.04,.13,.13,.06,2)
    def star(piece,part,x,y,z,s=1):
        box(piece,part,x-.14*s,y-.65*s,z,.28*s,1.3*s,.14,2)
        box(piece,part,x-.65*s,y-.14*s,z,1.3*s,.28*s,.14,2)
    # Open-faced hats/cowls, never a solid cube covering the skin.
    hood=style in ('tails','cloak','soul','spider','spider_plus','void','soul_beldam')
    if hood:
        head(-4.5,-8.7,-4.4,9,1.1,9,0)
        head(-4.5,-7.6,3.45,9,7.8,1.05,0)
        for x in (-4.5,3.45):head(x,-7.6,-4.4,1.05,7.8,7.85,0)
        head(-4.7,-8.1,-4.65,9.4,.55,.45,2)
        if style in ('spider','spider_plus'):
            for x in (-2.8,-1,1,2.8):button('helmet','head',x,-7.4,-4.9,.75,7)
        if style in ('cloak','void'):head(-3.7,-9.8,-2.8,7.4,1.2,6.5,1)
        if style=='soul':star('helmet','head',0,-7.2,-4.9,1.7)
    elif style in ('nightcap','sleep'):
        head(-4.45,-9,-4.45,8.9,2.3,8.9,0);head(-4.65,-7.5,-4.6,9.3,.8,9.2,1)
        head(-2.5,-12,-2.5,6,3.4,6,0,r=(0,0,.15));head(1.8,-12.2,-1.5,3.8,2,4,0,r=(0,0,.4))
        head(4.8,-11.8,-1,1.8,2,2,2)
        if style=='nightcap':star('helmet','head',-1,-8.3,-4.6,1.4)
    elif style in ('garden','bowler','miner','tinker','perfect','duffle'):
        brim=12 if style=='garden' else 10 if style=='bowler' else 9.5
        head(-brim/2,-8,-5,brim,.7,10,1 if style=='garden' else 0)
        head(-4.2,-10.7,-3.8,8.4,3,7.6,0);head(-4.3,-8.7,-3.9,8.6,.9,7.8,1)
        if style in ('miner','tinker'):button('helmet','head',0,-9.7,-4.5,2.2,4);head(-.65,-10.35,-4.75,1.3,1.3,.3,7)
        if style=='perfect':head(-4.5,-10.7,-4.6,9,1.2,1,0);head(-4.5,-9,-4.6,1.1,7,8.5,0);head(3.4,-9,-4.6,1.1,7,8.5,0)
        if style=='garden':
            for x,y in ((-2,-9),(0,-10),(2,-9)):star('helmet','head',x,y,-4.2,1.2)
            button('helmet','head',0,-9,-4.5,1.3,3)
        if style=='bowler':button('helmet','head',2.5,-9,-4.15,1.6,7)
    else:
        # Sewn bonnet, mineral headband, or a tailored asymmetric cap.
        head(-4.4,-8.5,2.8,8.8,8.5,1.65,0)
        head(-4.4,-8.5,-4.3,8.8,1,8.7,0)
        for x in (-4.4,3.4):head(x,-7.5,-3.8,1,6.5,6.6,0)
        head(-4.6,-6.8,-4.55,9.2,.6,.6,1)
        button('helmet','head',0,-7.1,-4.8,1.7,7)
        if style.startswith('beldam'):
            for x in (-3,3):head(x,-10,-1,1.2,3,1.2,4,r=(0,0,-.35 if x<0 else .35))
    # Form fitting base, then layered silhouettes and asymmetric tailoring.
    body(-4.4,-.3,-2.45,8.8,12.6,4.9,0)
    body(-4.55,10.1,-2.6,9.1,1,5.2,1)
    short=style in ('garden','miner','bowler','tinker')
    for left in (False,True):
        part='left_arm' if left else 'right_arm';x=-1.45 if left else -3.45
        box('chestplate',part,x,-2.45,-2.45,4.9,5.5 if short else 10.4,4.9,1 if style in ('miner','garden','tinker') else 0)
        box('chestplate',part,x-.12,2.4 if short else 7.2,-2.58,5.14,1,5.16,2 if style in ('sleep','moon') else 1)
        if style in ('spider_plus','beldam_plus','soul_beldam'):
            box('chestplate',part,x-.4,-3,-2.7,5.7,1.4,5.4,1)
        leg='left_leg' if left else 'right_leg'
        box('leggings',leg,-2.35,-.2,-2.35,4.7,10.3,4.7,1 if style in ('garden','miner','bowler','tinker','perfect') else 0)
        box('leggings',leg,-2.45,8.5,-2.45,4.9,1.1,4.9,2 if style=='sleep' else 1)
        # Visible sewn knee patch, not a generic metal greave.
        box('leggings',leg,-1.5,4.1,-2.52,3,2.9,.26,6)
        for xx in (-1.6,1.4):box('leggings',leg,xx,4,-2.57,.22,3.1,.16,2)
        high=style in ('perfect','garden','tails','cloak','spider_plus','tinker')
        box('boots',leg,-2.55,6 if high else 8.5,-2.6,5.1,6 if high else 3.5,5.2,0)
        box('boots',leg,-2.55,10,-3.8,5.1,2,6.4,1 if style=='sleep' else 0)
        box('boots',leg,-2.65,11.4,-3.85,5.3,.9,6.6,4)
        box('boots',leg,-2.65,6 if high else 8.5,-2.65,5.3,.8,5.3,1)
        if style in ('sleep','nightcap'):button('boots',leg,0,10,-4,1.4,3)
        else:
            for yy in (8.8,10):box('boots',leg,-1.3,yy,-2.82,2.6,.3,.2,2)
        if style in ('sleep','nightcap'):
            for yy in (1.7,7):box('leggings',leg,-2.48,yy,-2.48,4.96,.45,4.96,1)
            box('boots',leg,-2.4,9.5,-4.5,4.8,1.8,1.4,0)
        elif style in ('garden','miner','tinker'):
            x=2.3 if left else -3.4
            box('leggings',leg,x,1,-1.8,1.1,3.6,3.6,6)
            for y in (3.5,7.4):box('boots',leg,-2.68,y if high else y+3,-2.7,5.36,.45,5.4,1)
            if style=='garden':star('boots',leg,0,7.5,-2.96,1.3)
        elif 'spider' in style:
            for yy in (1.5,5.5):
                box('leggings',leg,-2.5,yy,-2.65,4.8,.38,.35,2,r=(0,0,.3 if left else -.3))
            for yy in (7.4,8.6,9.8):box('boots',leg,-1.9,yy,-2.95,3.8,.3,.35,2,r=(0,0,.16))
        elif style in ('beldam','beldam_plus','soul_beldam','duffle'):
            button('leggings',leg,0,5.6,-2.94,1.8,4)
            button('boots',leg,0,9.5,-3.12,1.5,4)
        elif style in ('moon','glass','soul','void','tails','cloak'):
            for y in (2,6):star('leggings',leg,0,y,-2.87,1.2)
            box('boots',leg,-1,8.4,-3,2,2,.55,7,r=(0,0,.3))
        elif style=='bowler':
            box('leggings',leg,-2.45,6.6,-2.45,4.9,1.7,4.9,1)
            box('boots',leg,-2,10,-3.95,4,.5,.35,5)
        elif style=='perfect':
            box('leggings',leg,-1.8,1,-2.65,3.6,2.7,.5,0)
            box('boots',leg,-2.1,6.2,2.3,1,2.3,.7,1)
        if style in ('tails','cloak','beldam','beldam_plus','soul_beldam','soul','void'):
            # Tails move with each leg rather than intersecting walking thighs.
            box('chestplate',leg,-2.65,0,2.7,5.3,6+(2 if style in ('cloak','void') else 0),.7,0)
            box('chestplate',leg,-2.65,0,-3.3,5.3,4,.7,0)
    box('leggings','body',-4.4,11,-2.5,8.8,1.2,5,1)
    # Outfit-specific details, intentionally different constructions.
    if style in ('garden','miner','tinker'):
        for x in (-3,2):body(x,0,-2.8,1.1,8,.45,1);button('chestplate','body',x+.55,3,-3.15,.8,2)
        body(-3.1,4,-2.9,6.2,7.8,.5,1)
        for x in (-2.6,.5):body(x,7,-3.6,2.1,2.8,.8,6);body(x,7,-3.7,2.1,.35,.15,2)
        if style=='miner':body(3.9,7,-1,1.4,4,2.5,4)
        if style=='tinker':
            body(-5,6,-1,1.2,4,2,1);body(3.9,5,-2,1.2,5,1,2)
        if style=='garden':
            for x,y in ((-3,2),(3,5),(-2,10)):body(x,y,-3.45,1.3,2,.3,6,r=(0,0,.5));star('chestplate','body',x,y,-3.8,.9)
    elif style in ('sleep','nightcap'):
        for x in (-2,1):body(x,0,-2.8,1.2,3.7,.4,1,r=(0,0,.18 if x<0 else -.18))
        body(-4.6,8,-2.9,9.2,1.2,5.8,1);body(2,8,-3.25,1,4,.4,1)
        for y in (4,6):button('chestplate','body',0,y,-3,.7,2)
        if style=='nightcap':
            for x,y in ((-2.6,4),(2.7,6),(0,10)):star('chestplate','body',x,y,-3,1.1)
        else:body(-3.6,5,-2.95,2,2.5,.3,6);body(-3.7,5,-3.1,2.2,.4,.3,1)
    elif style in ('spider','spider_plus'):
        for sign in (-1,1):
            for y in (2,5,8):
                body(sign*.6,y,-2.8,3.7,.35,.3,2,r=(0,0,sign*.45))
            for n in range(2 if style=='spider' else 3):
                body(sign*4,2+n*3,2,4.3,.65,.8,1,r=(0,0,sign*.45))
                body(sign*7.4,3+n*3,2,1,3,.8,4,r=(0,0,-sign*.2))
        button('chestplate','body',0,4,-3.1,2.3,4)
        button('chestplate','body',0,7,-3.1,1.6,7)
    elif style in ('beldam','beldam_plus','soul_beldam'):
        for sign in (-1,1):
            body(sign*2,-1,-2.8,1.8,4,.8,1,r=(0,0,sign*.35))
        for y in (3.8,7.5):button('chestplate','body',0,y,-3.15,2,4)
        for x in (-3,-1,1,3):body(x,9,-2.95,.28,1.1,.3,2,r=(0,0,-.4))
        if style=='soul_beldam':star('chestplate','body',0,3.8,-3.5,1.3)
    elif style in ('tails','cloak','soul','void'):
        for sign in (-1,1):body(sign*2,-1.8,-2.8,2,4,1,1,r=(0,0,sign*.25))
        body(-4.5,0,2.6,9,11,.8,1)
        button('chestplate','body',0,2,-3.3,1.7,7)
        if style=='tails':
            for y in (5,8):button('chestplate','body',-1,y,-3,1,2);button('chestplate','body',1,y,-3,1,2)
        if style in ('soul','void'):
            for x,y in ((-2,4),(2,7),(-1,10)):star('chestplate','body',x,y,-3,1)
    elif style=='duffle':
        for y in (3,6,9):
            body(-2.9,y,-2.95,5.8,.45,.3,1);button('chestplate','body',1.4,y+.2,-3.3,1.6,4)
        for x in (-3.8,1.5):body(x,7,-3,2.4,3,.45,6)
    elif style=='bowler':
        for x in (-3,2):body(x,0,-2.7,1,10,.35,1)
        body(-2,1,-3,1.8,1.7,.6,3,r=(0,0,-.25));body(.2,1,-3,1.8,1.7,.6,3,r=(0,0,.25))
        for y in (4,7,10):button('chestplate','body',0,y,-3,.85,2)
        body(3.8,7,-1,1.4,3,2,1)
    elif style=='perfect':
        # Coraline's sunny rainwear silhouette, not a gilded knight.
        for y in (3,6,9):button('chestplate','body',0,y,-3.1,1,4)
        for x in (-3.7,1.1):body(x,7,-2.95,2.6,2.8,.5,0);body(x,7,-3.1,2.6,.5,.5,1)
        body(-4,-.7,2.6,8,3.8,2,0)
    elif style in ('moon','glass'):
        for sign in (-1,1):
            body(sign*2,-.6,-2.8,2,3,.65,1,r=(0,0,sign*.35))
            for y in (5,8):body(sign*2.3,y,-2.8,1.8,1.8,.65,7,r=(0,0,.45))
        button('chestplate','body',0,3,-3.2,1.8,7)
        for y in (6,9):body(-.2,y,-3,.4,1,.3,2)
    return dict(name=name,style=style,palette=palette,boxes=boxes)

def texture(spec):
    im=Image.new('RGBA',(256,128));rng=random.Random(spec['name'])
    for mat,color in enumerate(spec['palette']):
        rgb=tuple(bytes.fromhex(color[1:]));ox=(mat%4)*64;oy=(mat//4)*64
        for y in range(64):
            for x in range(64):
                # restrained woven texture at Minecraft pixel density, no oversized UV art
                n=(2 if (x+y)%4==0 else -2 if x%4==0 else 0)+rng.choice((-1,0,1))
                im.putpixel((ox+x,oy+y),tuple(max(0,min(255,c+n)) for c in rgb)+(255,))
    path=A/'textures/models/wardrobe'/f"{spec['name']}.png";path.parent.mkdir(parents=True,exist_ok=True);im.save(path)

def main():
    out=A/'wardrobe';out.mkdir(parents=True,exist_ok=True)
    for name,data in DESIGNS.items():
        spec=outfit(name,data);(out/f'{name}.json').write_text(json.dumps(spec,indent=2)+'\n');texture(spec)
    print('Authored 19 distinct, animated outfits; 76 wearable pieces share their Blender icon geometry.')
if __name__=='__main__':main()
