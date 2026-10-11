"""128 persistent parent identities with UV-native faces, fabric patterns and 3D hairstyles/outfits."""
from pathlib import Path
from PIL import Image,ImageDraw
import random,json
from create_parent_skins import skin,TONES,HAIR,PALETTES,rgb
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/coraline'

def build(mom,index):
    key=f'{"mom" if mom else "dad"}_{index}';rng=random.Random(index+int(mom)*137)
    tone=TONES[rng.randrange(8)];hair=HAIR[rng.randrange(8)];cloth,accent=PALETTES[rng.randrange(len(PALETTES))]
    pants=['#263c5b','#614331','#304e45','#494758'][index%4]
    palette=[hair,cloth,accent,pants,'#342b32','#e5cc94','#e3a28e',tone]
    im=skin(mom,index).resize((128,128),Image.Resampling.NEAREST);d=ImageDraw.Draw(im)
    # Tailored stitching and larger patterned garments, not a single flat shirt tint.
    for u,v in ((20,20),(32,20)):
        for y in range(v*2+4,v*2+22,3):
            for x in range(u*2+1,u*2+16,4):
                if index%4==0:d.line((x,y,x+1,y+1),fill=accent,width=1)
                elif index%4==1:d.rectangle((x,y,x+1,y+1),fill=accent)
                elif index%4==2:d.line((x,y,x+2,y),fill=accent,width=1)
        d.line((u*2,v*2+23,u*2+15,v*2+23),fill=accent,width=1)
    # Fine lips, warm cheeks, eyebrows and irises retain readable Minecraft faces.
    t=rgb(tone);shadow=tuple(max(0,c-35) for c in t)
    for x in (19,28):d.point((x,26),fill=shadow);d.point((x+1,26),fill=shadow)
    d.line((23,28,25,28+(index%3==0)),fill=shadow,width=1)
    if index%5==1:
        for x in (19,28):d.point((x,25),fill='#8a5440');d.point((x+1,27),fill='#8a5440')
    im.save(A/'textures/entity/parents'/f'{key}.png')
    boxes=[]
    def b(part,x,y,z,w,h,depth,m,r=(0,0,0)):
        boxes.append(dict(piece='all',part=part,box=[x,y,z,w,h,depth],material=m,rotation=list(r)))
    def button(x,y,m=4):
        b('body',x-.38,y-.38,-2.88,.76,.76,.25,m)
        b('body',x-.15,y-.1,-2.94,.12,.2,.1,5)
    haircut=index%10
    # Hair silhouettes: bun, braid, curly bob, long sweep, twin buns, quiff,
    # curls, ponytail, side part, and tied headscarf/flat cap.
    if mom:
        if haircut==0:b('head',-2.6,-9.4,3.4,5.2,4.4,3.2,0)
        elif haircut==1:
            for i in range(6):b('head',2.8+(i%2)*.5,-3.5+i*1.5,3.3,2,1.8,2.2,0)
            b('head',3.1,4.5,3.2,2,1,2.3,2)
        elif haircut==2:
            for side in (-1,1):
                for yy in (-6,-3,0):b('head',side*4-1,-.8+yy,-2,2.4,3.1,6.8,0,r=(0,0,side*.08))
        elif haircut==3:
            b('head',-4.6,-7.5,3.4,9.2,11,1.4,0);b('head',-4.8,-5,-2,1.5,9,6,0)
        elif haircut==4:
            for x in (-6.1,3.8):b('head',x,-8.6,.4,2.7,3.1,3.8,0)
        elif haircut==5:
            b('head',-4.6,-9.2,-4.2,5.8,1.9,8.1,0,r=(0,0,-.1));b('head',3.5,-6,-1,1.4,7,5,0)
        elif haircut==6:
            for x in (-4.7,-1.7,1.3):
                for z in (-4.6,-1.6,1.4):b('head',x,-9.9+(int(x)%2)*.4,z,3.4,2.7,3.5,0)
            for x in (-5.1,3.4):b('head',x,-7,-1,2,5,5,0)
        elif haircut==7:b('head',-1.6,-6.5,4,3.2,9,2.5,0);b('head',-1.9,-6.4,4,3.8,.8,2.7,2)
        elif haircut==8:b('head',-4.6,-8.5,-4.5,6.2,1.3,9,0);b('head',-4.7,-7,-4,1.3,6,7,0)
        else:
            b('head',-4.6,-8.8,-4.5,9.2,1,9,2);b('head',-4.7,-7.7,2.4,9.4,4,2.3,2)
            b('head',3.5,-5,3,1.8,5.3,.8,2,r=(.1,0,-.3))
        # Clip / ribbon offset away from face and button eyes.
        if haircut in (0,2,3,4,5,7,8):b('head',3.9,-5.4,-1.8,.6,1.1,2.4,2)
    else:
        if haircut==0:b('head',-4.3,-9.3,-4.4,7.3,1.8,7,0,r=(0,0,-.08))
        elif haircut==1:
            for x in (-4,-1,2):b('head',x,-10,-3,2.8,2.5,6.5,0,r=(0,0,.1))
        elif haircut==2:
            for x in (-4.5,-1.5,1.5):
                for z in (-4.5,-1.5,1.5):b('head',x,-9.7,z,3.2,2.4,3.3,0)
        elif haircut==3:b('head',-4.6,-7.5,3.4,9.2,8.5,1.4,0)
        elif haircut==4:b('head',-4.5,-8.5,-4.2,9,1.2,8.4,0);b('head',-3.5,-9.8,-4,6,1.5,6,0)
        elif haircut==5:b('head',-4.6,-8.3,-4.4,9.2,1.6,8.8,2);b('head',-4.7,-7.2,-5.8,9.4,.6,2,2)
        elif haircut==6:b('head',-2,-10,-2,4,2.7,5,0)
        elif haircut==7:b('head',-4.4,-8.8,-4.2,8.8,1.5,8.4,0);b('head',-1.6,-4,4.2,3.2,6,1.6,0)
        elif haircut==8:b('head',-4.5,-8.6,-4.4,5.2,1.2,8.8,0)
        else:
            b('head',-5,-8.2,-5,10,.7,10,1);b('head',-4,-10.5,-3.6,8,2.6,7.2,1);b('head',-4.1,-8.8,-3.7,8.2,.7,7.4,2)
    style=(index//2+index%3)%12
    # Shirt collars and cuffed sleeves on nearly all outfits.
    for sign in (-1,1):b('body',sign*1.5,-.25,-2.4,1.7,2.2,.5,2,r=(0,0,sign*.3))
    for left in (False,True):
        arm='left_arm' if left else 'right_arm';x=-1.35 if left else -3.35;w=3.7 if mom else 4.7
        b(arm,x,5.8,-2.35,w,1,4.7,2)
    if style in (0,1,9): # tailored long cardigans / flared coat tails
        for x in (-4.35,.9):b('body',x,.8,-2.55,3.5,10.8,.4,1)
        for left in (False,True):
            leg='left_leg' if left else 'right_leg';b(leg,-2.45,0,2.35,4.9,5.5,.5,1);b(leg,-2.45,0,-2.9,4.9,3,.5,1)
        for y in (4,7,10):button(.2,y)
        for x in (-3.6,1.4):b('body',x,7,-3,2.2,2.5,.4,2)
    elif style in (2,3): # apron / work overalls with projecting pockets
        b('body',-3,3,-2.55,6,9.5,.5,2 if style==2 else 3)
        for x in (-3,2):b('body',x,.3,-2.6,1,6,.3,2 if style==2 else 3)
        for x in (-2.6,.3):b('body',x,7,-3.2,2.2,2.5,.55,1)
        for x in (-2.6,2.6):button(x,3,5)
    elif style in (4,5): # patterned waistcoat and bow tie / rolled scarf
        for x in (-4.3,1):b('body',x,1,-2.6,3.3,10,.5,1)
        if style==4:
            for sign in (-1,1):b('body',sign*1.1,1,-3,1.8,1.5,.6,2,r=(0,0,sign*.3))
        else:b('body',-4.5,-.5,-2.8,9,1.7,5.6,2);b('body',2,1,-3,1.8,7,.5,2)
        for y in (4,6.5,9):button(0,y)
    elif style==6: # wide scarf and cross-body satchel
        b('body',-4.5,-.2,-2.8,9,1.5,5.6,2);b('body',-3,1,-2.9,1.6,6,.5,2)
        b('body',-3,0,-2.8,1.1,13,.45,4,r=(0,0,-.4));b('body',3.9,7,-1.9,2,4,3.8,4);b('body',4,7,-2,2,1,4,5)
    elif style==7: # ruffled blouse/sweater, layered skirt-like coat for mom
        for x in (-3,-1,1,3):b('body',x,1,-2.6,1,2.5,.8,2,r=(0,0,.15))
        if mom:
            for left in (False,True):b('left_leg' if left else 'right_leg',-2.45,0,-2.7,4.9,4.5,5.4,1)
        for y in (5,8):button(0,y)
    elif style==8: # layered practical vest and canvas pockets
        b('body',-4.4,2,-2.5,8.8,8.5,.4,3)
        for x in (-3.7,1):b('body',x,5,-3,2.8,3,.7,2)
    elif style==10: # suspenders and necktie
        for x in (-3,2):b('body',x,0,-2.6,1,11,.4,4)
        b('body',-.5,1,-2.9,1,5.5,.4,2);b('body',-4.5,10.5,-2.5,9,.8,5,4)
    else: # loose shawl / broad knit collar
        b('body',-4.8,0,2.3,9.6,9,.7,2)
        for sign in (-1,1):b('body',sign*2,-.5,-2.7,2.5,5,.5,2,r=(0,0,sign*.2))
    path=A/'wardrobe/parents'/f'{key}.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(dict(name=key,palette=palette,boxes=boxes),indent=2)+'\n')
    atlas=Image.new('RGBA',(256,128))
    for n,c in enumerate(palette):
        base=rgb(c)
        for y in range(64):
            for x in range(64):
                delta=3 if (x+y)%5==0 else -3 if x%4==0 else 0
                atlas.putpixel(((n%4)*64+x,(n//4)*64+y),tuple(max(0,min(255,v+delta)) for v in base)+(255,))
    tex=A/'textures/models/wardrobe/parents'/f'{key}.png';tex.parent.mkdir(parents=True,exist_ok=True);atlas.save(tex)

if __name__=='__main__':
    for mom in (True,False):
        for index in range(64):build(mom,index)
    print('128 faces and outfits, 10 hair silhouettes per role, 12 garment constructions. Saved IDs preserved.')
