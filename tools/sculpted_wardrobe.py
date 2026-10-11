"""Wearable costume meshes: shared source for Forge and Blender inventory portraits.

Pixels, Y down, front -Z. Surfaces attach to animated humanoid bones. No cubes.
Run with Python, then blender --background --python tools/render_sculpted_wardrobe.py.
"""
from pathlib import Path
import json, math
from build_tailored_wardrobe import DESIGNS
import sculpted_coat as geo

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'src/main/resources/assets/coraline/sculpted'
TAU=math.tau
PART='chestplate'

def surface(name,bone,fn,nu=24,nv=8,mat='velvet'):
    geo.surface(name,bone,fn,nu,nv,mat)

def line(name,bone,pts,r=.055,mat='thread'):
    geo.tube(name,bone,pts,r,mat,5)

def ring(name,bone,c,rx,rz,mat='piping',r=.065):
    line(name,bone,[[c[0]+rx*math.cos(i*TAU/32),c[1],c[2]+rz*math.sin(i*TAU/32)] for i in range(33)],r,mat)

def shell(name,bone,c,rx,rz,height,mat='velvet',flare=0,fold=.07):
    def f(u,v):
        a=u*TAU;x,z=geo.roundrect(a,rx+flare*v+.12*math.sin(v*math.pi),rz+flare*v*.55+.1*math.sin(v*math.pi),3.6)
        w=fold*math.sin(a*11+v*17)*math.sin(v*math.pi)
        return [c[0]+x,c[1]+height*v,c[2]+z+w]
    surface(name,bone,f,32,10,mat)
    return f

def seam(name,bone,fn):
    geo.stitch(name,bone,[fn(i/48) for i in range(49)])

def leaf(bone,x,y,z,size=1,mat='lining',angle=0):
    def f(u,v):
        xx=math.sin(v*math.pi)*(u*2-1)*size*.45;yy=(v-.5)*size*1.9
        return [x+xx*math.cos(angle)-yy*math.sin(angle),y+xx*math.sin(angle)+yy*math.cos(angle),z-.14*math.sin(v*math.pi)*math.sin(u*math.pi)]
    surface('Curved embroidered leaf',bone,f,4,6,mat)
    line('Leaf vein',bone,[[x-math.sin(angle)*(v-.5)*size*1.9,y+math.cos(angle)*(v-.5)*size*1.9,z-.16] for v in (0,.25,.5,.75,1)],.025,'thread')

def flower(bone,x,y,z,size=.6):
    for i in range(5):
        a=i*TAU/5
        geo.ellipse('Silk flower petal',bone,[x+math.cos(a)*size*.6,y+math.sin(a)*size*.6,z],size*.45,size*.45,.16,'accent')
    geo.ellipse('Flower seed',bone,[x,y,z-.18],size*.22,size*.22,.11,'brass')

def moon(bone,x,y,z,s=1):
    surface('Crescent brooch',bone,lambda u,v:[x-s*math.cos((v-.5)*math.pi*1.55)*(1-u*.65)+u*s*.15,y+s*math.sin((v-.5)*math.pi*1.55),z-u*.06],4,18,'silver')

def motif(style,bone,x,y,z,s=1):
    if style in ('garden','garden_overalls'):
        leaf(bone,x,y,z,s);flower(bone,x+.45*s,y-.45*s,z-.08,.45*s)
    elif style in ('spider','spider_plus','beldam','beldam_plus','soul_beldam','void'):
        for i in range(6):
            a=i*TAU/6;line('Web spokes',bone,[[x,y,z],[x+math.cos(a)*s,y+math.sin(a)*s,z]],.025)
        for r in (.4,.75,1):
            line('Web scallops',bone,[[x+math.cos(i*TAU/30)*s*r*(1-.12*math.sin(i*math.pi/5)**2),y+math.sin(i*TAU/30)*s*r*(1-.12*math.sin(i*math.pi/5)**2),z] for i in range(31)],.023)
    elif style in ('bowler','luck'):
        for dx,dy in ((-.3,0),(.3,0),(0,-.4)):
            geo.ellipse('Lucky clover',bone,[x+dx*s,y+dy*s,z],s*.37,s*.37,.08,'lining')
        line('Clover stem',bone,[[x,y,z],[x+.2*s,y+s*.7,z]],.045,'lining')
    elif style in ('miner','tinker'):
        geo.button('Riveted brass fastener',bone,x,y,z,s*.6,'brass')
    elif style in ('moon','tails','midnight_coat','cloak'):
        moon(bone,x,y,z,s)
    else:geo.star('Embroidered star',bone,x,y,z,s)

def bow(bone,x,y,z,size=1):
    for side in (-1,1):
        surface('Satin bow loop',bone,lambda u,v,side=side:[x+side*(.15+u*size),y+(v-.5)*size*(.35+u),z-.25*math.sin(u*math.pi)],8,4,'lining')
    geo.ellipse('Bow knot',bone,[x,y,z-.08],.24*size,.3*size,.22,'lining')

def torso(style):
    apron=style in ('garden','miner','tinker','garden_overalls')
    short=style in ('star_shirt','garden','miner','tinker','garden_overalls')
    long=style in ('tails','cloak','void','soul','beldam','beldam_plus','soul_beldam','midnight_coat','circus_jacket','raincoat','duffle')
    def bodice(u,v):
        a=u*TAU;rx=5.05-.35*math.sin(v*math.pi)+.05*v;rz=3.02-.18*math.sin(v*math.pi)
        x,z=geo.roundrect(a,rx,rz,3.6)
        fold=(.2*math.sin(a*13+v*3)+.08*math.sin(a*21-v*11))*math.sin(v*math.pi)
        return [x+math.cos(a)*fold,-.3+12.5*v,z+math.sin(a)*fold]
    surface('Tailored bodice','body',bodice,48,16)
    # Shoulder panels close the old hollow shell while leaving a real neck opening.
    surface('Shoulder yoke','body',lambda u,v:[(1-v)*geo.roundrect(u*TAU,5.05,3.02,3.6)[0]+v*2.15*math.cos(u*TAU),-.3-.25*math.sin(v*math.pi),(1-v)*geo.roundrect(u*TAU,5.05,3.02,3.6)[1]+v*1.65*math.sin(u*TAU)],32,4)
    shell('Standing cloth collar','body',(0,-1.2,0),2.3,1.85,1.2,'lining')
    if not apron:
        for side in (-1,1):
            def lap(u,v,s=side):
                return [s*((1-v)*(1.9+u*2.25)+v*(.05+u*.9)),v*5.7-.4+u*.3,-2.77-.7*math.sin(u*math.pi)-.22*v]
            surface('Folded lapel','body',lap,7,10,'lining' if style in ('sleep','circus_jacket','perfect') else 'velvet')
            seam('Lapel stitches','body',lambda t:lap(.96,t))
            line('Contrasting rolled lapel edge','body',[lap(1,i/32) for i in range(33)],.065,'piping')
        for i in range(3):geo.button('Sewn mismatched button','body',.45,6.1+i*2.1,-2.91,.44+(i%2)*.12)
        # The brooch sits above the rolled lapel, not partly buried inside it.
        motif(style,'body',-2,2.2,-4.25,.8)
    else:
        surface('Curved apron bib','body',lambda u,v:[(u-.5)*6,1.8+v*10,-2.82-.2*math.sin(u*math.pi)*math.sin(v*math.pi)],12,10,'lining')
        for side in (-1,1):
            surface('Overall shoulder strap','body',lambda u,v,s=side:[s*(2.2+u*.7),-.5+v*5,-2.94],3,7,'lining')
            geo.button('Strap button','body',side*2.5,3.7,-3.13,.43,'brass')
        motif(style,'body',0,5,-3.16,1)
    for side in (-1,1):
        bone='left_arm' if side>0 else 'right_arm';cx=side
        end=5.1 if short else 10.45
        def sleeve(u,v,cx=cx,end=end):
            a=u*TAU;radius=2.58+.15*math.cos(v*TAU)+.18*max(0,(v-.65)/.35)
            x,z=geo.roundrect(a,radius,radius,3.6)
            wrinkle=.18*math.sin(v*25+a*3)*math.sin(v*math.pi)
            return [cx+x,-2.4+end*v,z+wrinkle]
        surface('Gathered sleeve',bone,sleeve,32,18)
        surface('Closed sleeve shoulder',bone,lambda u,v,cx=cx:[cx+geo.roundrect(u*TAU,2.68,2.68,3.6)[0]*v,-2.4-.45*math.sin(v*math.pi),geo.roundrect(u*TAU,2.68,2.68,3.6)[1]*v],32,5)
        cuff=shell('Turned back cuff',bone,(cx,end-3.55,0),2.86,2.85,1.3,'velvet')
        seam('Cuff saddle stitch',bone,lambda t:cuff(t,.12))
        seam('Cuff saddle stitch',bone,lambda t:cuff(t,.93))
        for dx,dy in ((-.9,0),(.7,.22)):geo.star('Cuff embroidered constellation',bone,cx+dx,end-2.95+dy,-2.79,.34)
        geo.button('Cuff button',bone,cx+1.65,end-2.65,-2.62,.27)
        seam('Sleeve tailored seam',bone,lambda t:sleeve(.73,.1+t*.66))
    # Soft pocket: sewn U border, hollow lip, actual thread spool / gardener leaves.
    def pocket(u,v):return [1.45+u*2.45,8+v*2.8-.3*math.sin(u*math.pi),-2.94-.27*math.sin(u*math.pi)*math.sin(v*math.pi)]
    surface('Rounded patch pocket','body',pocket,10,8,'lining' if apron else 'velvet')
    for u in (.07,.93):seam('Pocket side stitches','body',lambda t,u=u:pocket(u,t))
    seam('Pocket hem','body',lambda t:pocket(t,.94))
    if apron:
        leaf('body',2.7,7.5,-3.1,1,'accent',.4)
    elif style not in ('star_shirt','sleep'):
        shell('Pocket thread spool','body',(2.7,7, -2.92),.37,.37,1.1,'lining')
        for y in (7,8.1):ring('Wooden spool lip','body',(2.7,y,-2.92),.46,.46,'brass',.09)
    if long:
        length=8.5 if style not in ('cloak','void','soul_beldam') else 10.1
        for side in (-1,1):
            bone='left_leg' if side>0 else 'right_leg'
            def tail(u,v,s=side):
                opening=.000001+v*.13
                a=-math.pi/2+opening+u*(math.pi-2*opening)
                if s<0:a=math.pi-a
                x,z=geo.roundrect(a,5.05+v*2.5,3.02+v*1.5,3.6)
                fold=(.5*math.sin(u*math.pi*10+v*1.3)+.18*math.sin(u*math.pi*17))*v
                return [x-s*1.9+math.cos(a)*fold,v*(length-.9*math.cos(u*TAU)),z+math.sin(a)*fold]
            surface('Sculpted split coat tail',bone,tail,24,12)
            # Interior is offset radially, never crosses the cloth at side edges.
            surface('Contrasting tail lining',bone,lambda u,v,s=side:[tail(u,v)[0]-(tail(u,v)[0]+s*1.9)*.018,tail(u,v)[1],tail(u,v)[2]*.97],24,12,'lining')
            seam('Tail hem embroidery',bone,lambda t:tail(t,.94))
            for edge in (.01,.99):line('Rolled contrast coat-tail edge',bone,[tail(edge,i/32) for i in range(33)],.06,'piping')
            for u in (.04,.96):seam('Tail edge stitching',bone,lambda t,u=u:tail(u,t))
            for u in (.13,.3,.5,.7,.87):
                x,y,z=tail(u,.82)
                if style in ('midnight_coat','tails','cloak'):geo.star('Hem embroidered constellation',bone,x,y,z+(-.1 if z<0 else .1),.46)
                else:motif(style,bone,x,y,z+(-.1 if z<0 else .1),.4)
            seam('Constellation trail',bone,lambda t:tail(t,.74+.06*math.sin(t*TAU*3)))
    if style in ('circus_jacket','duffle','perfect'):
        for side in (-1,1):
            for y in (4,6,8):
                line('Braided closure','body',[[side*.5,y,-3],[side*1.6,y-.3,-3.1],[side*3,y,-2.98]],.07,'brass')
                geo.button('Braided closure button','body',side*3,y,-3.08,.33,'brass')
    if style in ('sleep','comfy_pajamas'):bow('body',0,2,-3.05,1.5)

def trousers(style):
    for side in (-1,1):
        bone='left_leg' if side>0 else 'right_leg'
        # Inner side stops at the bone centreline: separate legs, with room for skin.
        def leg(u,v):
            x,z=geo.roundrect(u*TAU,2.43,2.43,6)
            x=max(-1.85,x) if side>0 else min(1.85,x)
            return [x, v*11.7,z+.06*math.sin(v*26+u*9)*math.sin(v*math.pi)]
        surface('Tailored articulated trousers',bone,leg,28,14)
        seam('Trouser side seam',bone,lambda v:leg(.02 if side>0 else .48,v))
        shell('Turned trouser hem',bone,(side*.16,10.25,0),2.29,2.52,1.25,'lining')
        surface('Sewn knee patch',bone,lambda u,v:[(u-.5)*2.8,4.1+v*2.5,-2.54-.12*math.sin(u*math.pi)*math.sin(v*math.pi)],8,7,'lining')
        motif(style,bone,0,5.4,-2.75,.65)
        for x in (-1.3,1.3):line('Patch stitching',bone,[[x,4.2,-2.69],[x,6.4,-2.69]],.035)
        for y in (1.3,2.3):geo.button('Hip seam button',bone,side*1.35,y,-2.56,.26)

def shoes(style):
    slipper=style in ('sleep','nightcap','silent_slippers','comfy_pajamas')
    for side in (-1,1):
        bone='left_leg' if side>0 else 'right_leg';height=2.3 if slipper else 5.1
        shell('Soft boot shaft',bone,(side*.18,12-height,0),2.37,2.45,height-.8,flare=.12)
        # Domed toe and curved sole, not rectangular shoe bricks.
        def toe(u,v):
            a=u*TAU;x,z=geo.roundrect(a,2.57,3.7,4)
            return [side*.18+x*math.sin(v*math.pi/2),12-2.2*math.cos(v*math.pi/2),-.8+z*math.sin(v*math.pi/2)]
        surface('Rounded shoe vamp',bone,toe,28,8)
        sole=shell('Layered stitched leather sole',bone,(side*.18,11.6,-.8),2.61,3.75,.52,'black',fold=0)
        seam('Sole welt stitch',bone,lambda u:sole(u,.4))
        shell('Folded boot cuff',bone,(side*.18,11.5-height,0),2.53,2.58,.8,'lining')
        if slipper:bow(bone,side*.18,10.4,-3.75,1)
        else:
            for j in range(3):
                y=8.0+j*.65
                line('Crossed shoelaces',bone,[[-1.1,y,-2.64],[1.1,y+.5,-2.68]],.06)
                line('Crossed shoelaces',bone,[[1.1,y,-2.64],[-1.1,y+.5,-2.68]],.06)
            motif(style,bone,0,10.4,-3.1,.55)
        if style in ('feather_boots','moon','soul','void'):
            for i in range(4):leaf(bone,side*(2.15+i*.37),8.3-i*.5,-.8,1.3,'silver',side*(.3+i*.1))

def headwear(style):
    bone='head'
    if style in ('midnight_goggles','unbinding_button_eyes'):
        if style=='midnight_goggles':
            shell('Elastic goggle strap',bone,(0,-5.3,0),4.4,4.42,1.25,'black',fold=0)
        for x in (-2.15,2.15):
            geo.button('Hand sewn eye button',bone,x,-4.55,-4.7,1.75,'button' if style=='unbinding_button_eyes' else 'brass')
            if style=='midnight_goggles':
                geo.ellipse('Polished blue glass lens',bone,[x,-4.55,-4.99],1.38,1.38,.22,'lining')
                line('Lens glint',bone,[[x-.75,-5.2,-5.13],[x-.2,-5.6,-5.13]],.06,'silver')
        return
    hood=style in ('tails','cloak','soul','spider','spider_plus','void','soul_beldam')
    if hood:
        # Open-face cowl wraps the sides and back, keeping the player's face visible.
        def cowl(u,v):
            a=-1.1+u*(math.pi+2.2)
            x,z=geo.roundrect(a,5.25,5.25,5)
            return [x,-8.7+v*9.1,z+v*.08]
        surface('Soft open cowl',bone,cowl,32,10)
        for edge in (0,1):
            line('Cowl rolled face edge',bone,[cowl(edge,v/24) for v in range(25)],.13,'lining')
            seam('Cowl face saddle stitches',bone,lambda v,edge=edge:cowl(edge,v))
        surface('Rounded hood crown',bone,lambda u,v:[geo.roundrect(u*TAU,5.26,5.26,5)[0]*v,-8.7-.7*math.sin(v*math.pi),geo.roundrect(u*TAU,5.26,5.26,5)[1]*v],32,8)
        line('Hood face piping',bone,[[-3.82,-8.7,-5.1],[-2,-8.9,-5.23],[0,-9,-5.25],[2,-8.9,-5.23],[3.82,-8.7,-5.1]],.095,'lining')
        motif(style,bone,0,-8.55,-5.5,.85)
        if style in ('spider','spider_plus'):
            for x in (-2.7,-.95,.95,2.7):geo.button('Spider cowl eyes',bone,x,-8.2,-5.48,.48,'accent')
    elif style in ('nightcap','sleep'):
        shell('Rolled sleeping cap rim',bone,(0,-8.5,0),4.92,4.92,1.2,'lining')
        surface('Drooping nightcap',bone,lambda u,v:[4.5*(1-v)*math.cos(u*TAU)+3.6*v*v,-8.5-4*math.sin(v*math.pi*.65),4.5*(1-v)*math.sin(u*TAU)],32,12)
        geo.ellipse('Fluffy cap tassel',bone,[3.6,-12.1,0],.8,.95,.8,'thread')
        motif(style,bone,0,-9,-4.55,.8)
    elif style in ('beldam','beldam_plus'):
        surface('Scalloped bonnet',bone,lambda u,v:[(4.6+.22*math.sin(u*TAU*12))*math.cos(u*TAU),-8.3+v*1.2,(4.6+.22*math.sin(u*TAU*12))*math.sin(u*TAU)],48,4,'lining')
        surface('Bonnet crown',bone,lambda u,v:[4.55*math.cos(u*TAU)*v,-8.35-1.8*(1-v*v),4.55*math.sin(u*TAU)*v],32,8)
        for x in (-4.5,4.5):surface('Bonnet ribbon',bone,lambda u,v,x=x:[x+(u-.5)*1,-7.8+v*8,-2.8-.3*math.sin(v*5)],4,12,'lining')
        bow(bone,0,-8.4,-4.8,1.7)
    else:
        garden=style=='garden';tall=style=='top_hat';miner=style in ('miner','tinker')
        brim=6.6 if garden else 5.4
        surface('Curved hat brim',bone,lambda u,v:[(4.15+v*(brim-4.15))*math.cos(u*TAU),-8.1+.22*math.sin(u*TAU*2)*v+.25*v*v,(4.15+v*(brim-4.15))*math.sin(u*TAU)],40,5,'lining' if garden else 'velvet')
        h=6 if tall else 2.7
        shell('Shaped felt hat crown',bone,(0,-8-h,0),4.2,4.2,h,flare=.15)
        surface('Hat crown top',bone,lambda u,v:[geo.roundrect(u*TAU,4.2,4.2,3.6)[0]*v,-8-h-.45*(1-v*v),geo.roundrect(u*TAU,4.2,4.2,3.6)[1]*v],32,6)
        shell('Satin hat band',bone,(0,-9.25,0),4.39,4.39,1.05,'lining',fold=0)
        if miner:
            geo.ellipse('Round miners lamp',bone,[0,-9.7,-4.55],1.2,1.2,.4,'brass')
            geo.ellipse('Lamp glass',bone,[0,-9.7,-4.91],.87,.87,.15,'silver')
        else:motif(style,bone,0,-9.4,-4.58,.75)
        if garden:
            for x in (-2,0,2):flower(bone,x,-9.1,-4.6,.6)
        if tall:leaf(bone,3,-11.1,-4.4,2.2,'accent',-.35)

def costume_details(style,piece):
    """Family-specific silhouettes and hand-sewn ornaments, not palette swaps."""
    chest=piece=='chestplate'
    if chest and style in ('cloak','void','soul','soul_beldam'):
        # Rounded shawl and a corrugated back drape. Attached to torso above the legs.
        surface('Layered crescent shoulder shawl','body',lambda u,v:[(4.5+v*2)*math.cos(u*math.pi),-.7+v*(3.2+.4*math.sin(u*math.pi*7)),(2.7+v*.7)*math.sin(u*math.pi)+.2],32,8,'lining')
        def cape(u,v):return [(u-.5)*(9+v*4),2+v*11,3.05+v*1.6+.45*math.sin(u*TAU*4)*v]
        surface('Pleated dream cape','body',cape,32,16)
        seam('Cape silver hem','body',lambda u:cape(u,.96))
        for u in (.13,.34,.65,.86):
            x,y,z=cape(u,.8);motif(style,'body',x,y,z+.08,.6)
        if style=='void':
            for i in range(5):
                surface('Floating layered scallop','body',lambda u,v,i=i:[(u-.5)*(7.5-i*.4),3+i*1.35+v*2,-2.94-.3*math.sin(u*math.pi)],16,5,'lining' if i%2==0 else 'velvet')
    if style in ('beldam','beldam_plus','soul_beldam','spider','spider_plus'):
        if chest:
            # Handmade lace ruff with scalloped edge and a laced corset panel.
            surface('Scalloped lace collar','body',lambda u,v:[(2.3+v*2.7)*math.cos(u*TAU),-.7+v*(1.6+.25*math.cos(u*TAU*16)),(1.9+v*1.45)*math.sin(u*TAU)],64,6,'lining')
            for j in range(5):
                y=5+j*1.15
                for side in (-1,1):
                    line('Corset crossed thread','body',[[side*1.15,y,-3.12],[-side*1.15,y+.9,-3.15]],.07,'thread')
                    geo.ellipse('Corset eyelet','body',[side*1.15,y,-3.08],.14,.14,.08,'brass')
            for side in (-1,1):
                bone='left_arm' if side>0 else 'right_arm'
                for j in range(3):
                    shell('Layered wrist lace',bone,(side,6.9+j*.35,0),2.65+j*.08,2.65+j*.08,.25,'lining',fold=0)
            if style in ('spider_plus','beldam_plus','soul_beldam'):
                for side in (-1,1):
                    bone='left_arm' if side>0 else 'right_arm'
                    for j in range(3):
                        line('Sculpted thread shoulder loops',bone,[[side, -2.8,0],[side*(3.2+j*.4),-4.3-j*.4,1],[side*(4.0+j*.3),-1.7-j*.3,1.5]],.1,'lining')
        if piece=='boots':
            for side in (-1,1):
                bone='left_leg' if side>0 else 'right_leg'
                surface('Curled pointed shoe toe',bone,lambda u,v,s=side:[s*.18+1.8*(1-v)*math.cos(u*TAU),10.6-1.4*v*v+.5*math.sin(u*TAU)*(1-v),-3.6-v*2.1],20,8,'lining')
    if style in ('garden','garden_overalls'):
        targets=[('body',-2.8,7,-3.03)] if chest else [('head',-2,-9.2,-4.64)] if piece=='helmet' else [('left_leg',.7,7,-2.7),('right_leg',-.7,7,-2.7)]
        for bone,x,y,z in targets:
            line('Winding garden vine',bone,[[x+.35*math.sin(i*.5),y-2+i*.19,z] for i in range(26)],.065,'lining')
            for j in range(4):leaf(bone,x+(-1 if j%2 else 1)*.35,y-1.5+j*1.05,z-.06,.64,'lining',(-1 if j%2 else 1)*.8)
    if style in ('nightcap','sleep') and chest:
        for side in (-1,1):
            bone='left_arm' if side>0 else 'right_arm'
            for j in range(4):
                shell('Quilted sleeve stripe',bone,(side,-.3+j*1.8,0),2.62,2.62,.35,'lining',fold=0)
        for x in (-3,-1.5,1.5,3):
            line('Pyjama pinstripe','body',[[x,2.1,-2.8],[x,10.5,-2.8]],.037,'thread')
        bow('body',0,2.2,-3.4,1.4)
    if style in ('moon','glass','perfect'):
        bone='body' if chest else 'head' if piece=='helmet' else 'left_leg'
        x,y,z=(0,4.5,-3.3) if chest else (0,-9,-4.55) if piece=='helmet' else (0,8.8,-2.8)
        # Faceted polished stone sewn into fabric; not medieval metal plating.
        for dx,dy in ((0,0),(-1.6,.6),(1.6,.6)):
            size=.9 if dx==0 else .45
            geo.mesh('Sewn faceted charm',bone,[[x+dx,y+dy-size,z],[x+dx+size*.65,y+dy,z],[x+dx,y+dy+size,z],[x+dx-size*.65,y+dy,z],[x+dx,y+dy,z-.5]],[[0,1,4],[1,2,4],[2,3,4],[3,0,4]],'silver')
        if chest:
            for side in (-1,1):line('Charm chain','body',[[side*3,2,-3],[side*2,3.6,-3.3],[0,4.6,-3.3]],.055,'brass')
    if style in ('miner','tinker') and chest:
        for side in (-1,1):
            def pouch(u,v,s=side):return [s*(3.0+u*1.1),8.6+v*2.7,-2.9-.4*math.sin(v*math.pi)]
            surface('Utility leather pouch','body',pouch,6,6,'accent')
            geo.button('Utility pouch clasp','body',side*3.5,9.2,-3.4,.32,'brass')
        line('Tool pocket needle','body',[[2.7,7.6,-3.35],[2.7,5.7,-3.35]],.11,'silver')
    if style=='duffle' and chest:
        for side in (-1,1):
            for j in range(3):geo.button('Oversized protective sewn button','body',side*2.6,3.5+j*2.4,-3.17,.75,'button')
    if style=='star_shirt' and chest:
        for x,y,s in ((-2,4,1),(2,6,.7),(-1,9,.6)):
            geo.star('Large luminous star embroidery','body',x,y,-2.98,s)
        seam('Constellation across shirt','body',lambda t:[-3+6*t,5.7+1.3*math.sin(t*TAU),-3.0])
    if style=='raincoat' and chest:
        surface('Storm capelet','body',lambda u,v:[(4.65+v*.35)*math.cos(u*math.pi),-.25+v*3.5,(2.65+v*.5)*math.sin(u*math.pi)],28,6,'lining')
        for x in (-2.1,2.1):line('Hood drawstring','body',[[x,0,-2.9],[x+.2,3.7,-3.1]],.075,'thread')

SINGLES={
 'midnight_coat':('midnight_coat','chestplate','#182c50','#167488','#d6d3b6'),
 'star_shirt':('star_shirt','chestplate','#286786','#ecb143','#fff2b5'),
 'raincoat':('raincoat','chestplate','#e5b52b','#e47842','#f9edb4'),
 'comfy_pajamas':('sleep','chestplate','#bc759c','#f0bed0','#f5e4c9'),
 'garden_overalls':('garden_overalls','chestplate','#365e83','#72934a','#ecd59a'),
 'circus_jacket':('circus_jacket','chestplate','#982f4c','#243e73','#e7bd62'),
 'silent_slippers':('silent_slippers','boots','#734578','#d5a0b8','#f2d9bf'),
 'feather_boots':('feather_boots','boots','#83b3bc','#efe0c5','#f6edd2'),
 'top_hat':('top_hat','helmet','#2b2843','#a1355b','#e2c581'),
 'midnight_goggles':('midnight_goggles','helmet','#253b56','#548cab','#d5e8e3'),
 'unbinding_button_eyes':('unbinding_button_eyes','helmet','#252130','#342e45','#e4d5ad')}

def export(item,style,piece,palette):
    family=item.rsplit('_',1)[0]
    if item=='raincoat':
        import raincoat_model
        data=raincoat_model.build()
        DEST.mkdir(parents=True,exist_ok=True)
        (DEST/'raincoat.json').write_text(json.dumps(data,separators=(',',':')))
        return dict(id=item,style=data['style'],piece=piece,polygons=sum(len(m['faces']) for m in data['meshes']))
    if family in ('luck','moonstone','miners','spidervision','beldam_weave','soulbound','super_spidervision','potent_beldam_weave','soulbound_potent_beldam_weave','other_world_void','other_diamond','other_iron','perfect') and item==family+'_'+piece:
        import concept_armor
        data=concept_armor.build(family,piece);data['id']=item
        DEST.mkdir(parents=True,exist_ok=True)
        (DEST/(item+'.json')).write_text(json.dumps(data,separators=(',',':')))
        return dict(id=item,style=data['style'],piece=piece,polygons=sum(len(m['faces']) for m in data['meshes']))
    if family in ('garden_vines','midnight','miners') and item==family+'_'+piece:
        import field_wardrobes
        data=field_wardrobes.build(family,piece);data['id']=item
        DEST.mkdir(parents=True,exist_ok=True)
        (DEST/(item+'.json')).write_text(json.dumps(data,separators=(',',':')))
        return dict(id=item,style=data['style'],piece=piece,polygons=sum(len(m['faces']) for m in data['meshes']))
    if item.startswith(('comfy_silk_','buttonguard_')):
        import reference_wardrobes
        family='comfy_silk' if item.startswith('comfy_silk_') else 'buttonguard'
        data=reference_wardrobes.build(family,piece);data['id']=item
        DEST.mkdir(parents=True,exist_ok=True)
        (DEST/(item+'.json')).write_text(json.dumps(data,separators=(',',':')))
        return dict(id=item,style=data['style'],piece=piece,polygons=sum(len(m['faces']) for m in data['meshes']))
    if item.startswith('dreamweave_'):
        import dreamweave_model
        data=dreamweave_model.build(piece);data['id']=item
        DEST.mkdir(parents=True,exist_ok=True)
        (DEST/(item+'.json')).write_text(json.dumps(data,separators=(',',':')))
        return dict(id=item,style=data['style'],piece=piece,polygons=sum(len(m['faces']) for m in data['meshes']))
    geo.meshes.clear()
    {'helmet':headwear,'chestplate':torso,'leggings':trousers,'boots':shoes}[piece](style)
    costume_details(style,piece)
    if item in ('garden_overalls','comfy_pajamas'):trousers(style)
    mats=dict(zip(('velvet','lining','thread'),palette[:3]))
    mats.update(piping=palette[1],button='#262330',black='#171723',brass='#b79252',silver='#d4e4dc',accent=palette[3] if len(palette)>3 else '#c66281')
    for m in geo.meshes:
        # Hats sit over the player's optional 0.5-pixel skin outer layer.
        if piece=='helmet' and style not in ('tails','cloak','soul','spider','spider_plus','void','soul_beldam','midnight_goggles','unbinding_button_eyes'):
            for v in m['vertices']:v[1]-=.6
        # Keep applique and buttons on top of the shaped, padded fabric shell.
        if m['bone'] in ('body','left_arm','right_arm') and all(v[2]<-1.8 for v in m['vertices']):
            for v in m['vertices']:v[2]-=.45 if m['bone']=='body' else .3
        # Parametric rings use Y-down author coordinates: reverse to outward winding.
        m['faces']=[list(reversed(f)) for f in m['faces']]
        # Flat applique is authored in the face plane, unlike rotational shells.
        # Orient those open sheets outward too (closed buttons have zero summed area).
        area_z=0
        for f in m['faces']:
            a,b,c=(m['vertices'][i] for i in f[:3])
            area_z+=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
        if (all(v[2]<-1.8 for v in m['vertices']) and area_z>.1) or (all(v[2]>1.8 for v in m['vertices']) and area_z<-.1):
            m['faces']=[list(reversed(f)) for f in m['faces']]
        m['vertices']=[[round(x,5) for x in v] for v in m['vertices']]
    data=dict(id=item,style=style,piece=piece,materials=mats,meshes=geo.meshes)
    DEST.mkdir(parents=True,exist_ok=True)
    (DEST/(item+'.json')).write_text(json.dumps(data,separators=(',',':')))
    return dict(id=item,style=style,piece=piece,polygons=sum(len(m['faces']) for m in geo.meshes))

if __name__=='__main__':
    manifest=[]
    for name,(style,*palette) in DESIGNS.items():
        for piece in ('helmet','chestplate','leggings','boots'):manifest.append(export(name+'_'+piece,style,piece,palette))
    for name,(style,piece,*palette) in SINGLES.items():manifest.append(export(name,style,piece,palette))
    (ROOT/'artwork/sculpted_wardrobe/manifest.json').write_text(json.dumps(manifest,indent=2))
    print('Authored',len(manifest),'wearables;',sum(m['polygons'] for m in manifest),'total polygons')
