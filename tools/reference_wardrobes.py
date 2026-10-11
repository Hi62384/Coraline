"""Separate reference constructions for Comfy Silk and Buttonguard."""
import math
import sculpted_coat as g
from dreamweave_model import surface,cord,panel,star,crescent,shell,strap
T=math.tau

def silk(piece):
    navy='cloth';cream='cream'
    if piece=='helmet':
        # Padded open sleep hood, not Dreamweave's armored cowl.
        def hood(u,v):
            a=-1.04+u*(math.pi+2.08);x,z=g.roundrect(a,5.23,5.23,7)
            return [x,-8.8+v*9,z]
        surface('Padded sleep hood','head',hood,28,10,navy)
        surface('Drooping quilt cap','head',lambda u,v:[g.roundrect(u*T,5.23*(1-v),5.23*(1-v),7)[0]+5.8*v*v,-8.8-3.3*math.sin(v*math.pi),g.roundrect(u*T,5.23*(1-v),5.23*(1-v),7)[1]],28,14,navy)
        # Ivory padded brow and two soft cheek bindings leave the face readable.
        for s in (-1,1):
            panel('Ivory cheek binding','head',[(s*3.65,-7.9),(s*4.75,-7.9),(s*4.75,-.25),(s*3.8,-.25)],-5.32,cream)
        panel('Wide ivory brow cuff','head',[(-4.8,-8.65),(0,-9.05),(4.8,-8.65),(4.8,-7.3),(0,-7.75),(-4.8,-7.3)],-5.32,cream)
        g.button('Nightcap dangling button','head',5.8,-8.2,-.4,.78)
        crescent('head',0,-10.0,-4.7,.8)
        for x in (-2.3,2.3):star('head',x,-9.3,-4.9,.38)
    elif piece=='chestplate':
        shell('Soft ivory quilt jacket','body',(0,-.1,0),4.68,2.72,11.8,cream)
        surface('Quilt shoulder yoke','body',lambda u,v:[g.roundrect(u*T,4.68,2.72,6)[0]*v,-.1,g.roundrect(u*T,4.68,2.72,6)[1]*v],24,4,navy)
        panel('Navy half jacket','body',[(.2,.6),(4.45,.6),(4.45,11.3),(.2,11.3)],-2.91,navy)
        panel('Ivory button placket','body',[(-.6,.4),(.6,.4),(.6,11.6),(-.6,11.6)],-3.14,cream)
        # Rounded Peter Pan collar, deliberately without the old V lapels.
        for s in (-1,1):
            panel('Rounded silk collar','body',[(s*.1,-.1),(s*2.5,-.5),(s*3.55,.5),(s*2.9,1.65),(s*1.8,2.25),(s*.2,1.0)],-3.15,cream)
            panel('Rounded patch pocket','body',[(s*1.3,7.6),(s*3.9,7.6),(s*3.9,10.25),(s*2.5,10.55),(s*1.3,10.25)],-3.19,navy)
            star('body',s*2.6,8.8,-3.45,.48)
        for y in (2.65,5.1,7.55,10):g.button('Silk jacket sewn button','body',0,y,-3.43,.43)
        crescent('body',2.65,4.1,-3.22,.9)
        for s in (-1,1):
            bone='left_arm' if s>0 else 'right_arm'
            shell('Padded ivory sleeve',bone,(s,-2.2,0),2.45,2.45,10.3,cream)
            surface('Sleeve closed shoulder',bone,lambda u,v,s=s:[s+g.roundrect(u*T,2.47,2.47,6)[0]*v,-2.2-.18*math.sin(v*math.pi),g.roundrect(u*T,2.47,2.47,6)[1]*v],24,5,navy)
            shell('Navy short shoulder yoke',bone,(s,-2.22,0),2.55,2.55,2.5,navy)
            shell('Soft navy sleeve cuff',bone,(s,6.8,0),2.57,2.57,1.15,navy)
            star(bone,s,4.3,-2.64,.6)
        panel('Moon back panel','body',[(-4.25,.7),(4.25,.7),(4.25,10.6),(-4.25,10.6)],3.02,navy)
        crescent('body',0,4.4,3.2,1.4)
        # Back bow is actual loops and trailing ribbons, not a painted buckle.
        for s in (-1,1):
            surface('Silk back bow loop','body',lambda u,v,s=s:[s*(.15+u*1.5),8.4+(v-.5)*(1+u),3.35+.23*math.sin(u*math.pi)],8,6,navy)
            panel('Trailing silk bow ribbon','body',[(s*.15,8.6),(s*.8,8.6),(s*1.3,11.7),(s*.4,11.4)],3.42,navy)
    elif piece=='leggings':
        for s in (-1,1):
            bone='left_leg' if s>0 else 'right_leg'
            surface('Soft silk trousers',bone,lambda u,v:[max(-1.84,min(1.84,g.roundrect(u*T,2.18,2.28,6)[0])),v*11.7,g.roundrect(u*T,2.18,2.28,6)[1]],24,12,navy)
            shell('Ivory turned ankle cuff',bone,(0,9.4,0),1.86,2.39,2.2,cream)
            for x in (-1.2,.9):cord('Vertical silk trouser stitch',bone,[[x,1,-2.35],[x,9.35,-2.35]],.035)
            for x,y in ((-.8,2.8),(.7,5.7),(-.7,8)):star(bone,x,y,-2.48,.42)
            cord('Waist drawstring',''+bone,[[s*-.7,.3,-2.47],[s*-.55,2.1,-2.58]],.065,cream)
            star(bone,s*-.55,2.4,-2.62,.32)
    else:
        for s in (-1,1):
            bone='left_leg' if s>0 else 'right_leg';x=s*.2
            surface('Cushioned slipper boot',bone,lambda u,v:[x+g.roundrect(u*T,2.15,2.95,4)[0]*math.sin(v*math.pi/2),11.85-2.3*math.cos(v*math.pi/2),g.roundrect(u*T,2.15,2.95,4)[1]*math.sin(v*math.pi/2)-.45],24,10,navy)
            shell('Ivory padded boot cuff',bone,(x,8.35,0),2.15,2.37,1.75,cream)
            panel('Ivory boot front stripe',bone,[(x-.6,9.9),(x+.6,9.9),(x+.6,11.8),(x-.6,11.8)],-3.13,cream)
            g.button('Slipper cuff button',bone,x,9.1,-2.65,.56)
            star(bone,x-1.3,10.9,-3.05,.35)

def button_guard(piece):
    if piece=='helmet':
        shell('Enclosed button helmet','head',(0,-8.9,0),5.12,5.12,9.25,'cloth',8)
        surface('Closed helmet crown','head',lambda u,v:[g.roundrect(u*T,5.12,5.12,8)[0]*v,-8.9,g.roundrect(u*T,5.12,5.12,8)[1]*v],24,4,'cloth')
        panel('Black face mask','head',[(-4.7,-7.6),(4.7,-7.6),(4.7,.2),(-4.7,.2)],-5.21,'black')
        panel('Stepped forehead frame','head',[(-5.2,-8.8),(-3,-8.8),(-2.5,-9.3),(-.9,-9.3),(-.9,-10.1),(.9,-10.1),(.9,-9.3),(2.5,-9.3),(3,-8.8),(5.2,-8.8),(5.2,-7.5),(2.5,-7.5),(1.7,-6.85),(-1.7,-6.85),(-2.5,-7.5),(-5.2,-7.5)],-5.45,'frame')
        for x in (-4.85,0,4.85):panel('Helmet upright frame','head',[(x-.25,-7.7),(x+.25,-7.7),(x+.25,.45),(x-.25,.45)],-5.5,'frame')
        panel('Helmet lower frame','head',[(-5.15,-.15),(5.15,-.15),(5.15,.5),(-5.15,.5)],-5.45,'frame')
        for x in (-2.45,2.45):g.button('Helmet four-hole button eye','head',x,-4.1,-5.59,1.1)
    elif piece=='chestplate':
        shell('Dark protective tunic','body',(0,-.1,0),4.7,2.72,11.65,'cloth')
        surface('Closed tunic yoke','body',lambda u,v:[g.roundrect(u*T,4.7,2.72,6)[0]*v,-.1,g.roundrect(u*T,4.7,2.72,6)[1]*v],24,4,'cloth')
        panel('Cross stitched chest guard','body',[(-4,.6),(4,.6),(3.8,8.8),(0,9.6),(-3.8,8.8)],-3.02,'cloth')
        panel('Violet button scarf','body',[(-4.7,-.1),(0,1.0),(4.7,-.1),(3.8,2.15),(0,3.3),(-3.8,2.15)],-3.3,'violet')
        g.button('Scarf oversized fastening','body',0,2.65,-3.7,1.04)
        for s in (-1,1):cord('Hanging scarf thread','body',[[s*.3,3.45,-3.55],[s*.4,5.5,-3.5]],.085,'thread')
        for s in (-1,1):
            bone='left_arm' if s>0 else 'right_arm';x=s
            shell('Jointed dark sleeve',bone,(x,-2,0),2.25,2.25,10.2,'under')
            for y,w in ((-2.6,3.2),(-.7,2.9)):
                panel('Layered shoulder frame',bone,[(x-w,y),(x+w,y),(x+w,y+2.1),(x,y+2.8),(x-w,y+2.1)],-3.0,'frame')
                panel('Inset shoulder cloth',bone,[(x-w+.35,y+.3),(x+w-.35,y+.3),(x+w-.35,y+1.8),(x,y+2.35),(x-w+.35,y+1.8)],-3.2,'cloth')
            surface('Shoulder armor top',bone,lambda u,v,x=x:[x+(u-.5)*6.3,-2.65,-3+v*5.7],8,6,'frame')
            panel('Boxed bracer frame',bone,[(x-2.5,3.75),(x+2.5,3.75),(x+2.5,8.0),(x-2.5,8)],-2.65,'frame')
            panel('Bracer fabric inset',bone,[(x-2.1,4.1),(x+2.1,4.1),(x+2.1,7.65),(x-2.1,7.65)],-2.9,'cloth')
            for y in (-.6,5.8):g.button('Guard protective button',bone,x,y,-3.43 if y<0 else -3.13,.65)
        strap('body',(0,9.5,0),4.8,2.85,.8);g.button('Waist central button','body',0,9.9,-3.15,.65)
        def cape(u,v):return [(u-.5)*(9.2+v*3),1.5+v*(18.6-1.6*abs(2*u-1)+.65*math.cos(u*T*7)),3.2+v*2.8+.23*math.sin(u*T*7)*v]
        surface('Ragged violet back cloth','cape',cape,30,18,'violet')
        surface('Back cloth inner','cape',lambda u,v:[cape(1-u,v)[0],cape(1-u,v)[1],cape(1-u,v)[2]-.06],30,18,'lining')
        cord('Back cloth sewn spine','cape',[[0,3+i*.65,3.5+i*.10] for i in range(25)],.04,'thread')
        # Embroidered spider on the outside of the back cloth.
        g.ellipse('Spider stitched abdomen','cape',[0,12.8,5.6],.65,1.0,.1,'thread')
        g.ellipse('Spider stitched head','cape',[0,11.65,5.48],.4,.45,.1,'thread')
        for s in (-1,1):
            for i in range(4):cord('Spider embroidered leg','cape',[[s*.4,12.1+i*.3,5.7],[s*(1.3+i*.12),11.1+i*.8,5.7],[s*(1.6+i*.1),10.5+i*1.15,5.7]],.065,'thread')
    elif piece=='leggings':
        for s in (-1,1):
            bone='left_leg' if s>0 else 'right_leg'
            surface('Dark jointed leggings',bone,lambda u,v:[max(-1.84,min(1.84,g.roundrect(u*T,2.2,2.3,6)[0])),11.7*v,g.roundrect(u*T,2.2,2.3,6)[1]],24,10,'under')
            panel('Angled split hip panel',bone,[(-1.85,0),(1.85,0),(2.05,4.6),(-1.8,5.5)],-2.68,'cloth')
            panel('Knee protective frame',bone,[(-1.85,5.4),(1.85,5.4),(1.85,8.45),(-1.85,8.45)],-2.66,'frame')
            panel('Knee navy inset',bone,[(-1.5,5.7),(1.5,5.7),(1.5,8.15),(-1.5,8.15)],-2.9,'cloth')
            g.button('Knee fastening button',bone,0,6.9,-3.12,.7)
    else:
        for s in (-1,1):
            bone='left_leg' if s>0 else 'right_leg';x=s*.18
            shell('Dark framed boot',bone,(x,8.2,-.4),2.1,2.95,3.7,'cloth',8)
            surface('Boot squared top',bone,lambda u,v:[x+g.roundrect(u*T,2.1,2.95,8)[0]*v,8.2,g.roundrect(u*T,2.1,2.95,8)[1]*v-.4],24,5,'cloth')
            for y in (8.3,11.4):shell('Heavy boot frame band',bone,(x,y,-.4),2.16,3.0,.55,'frame',8)
            panel('Framed square toe',''+bone,[(x-2.05,10.5),(x+2.05,10.5),(x+2.05,11.8),(x-2.05,11.8)],-3.51,'frame')
            g.button('Boot button clasp',bone,x,9.7,-3.5,.65)

def build(style,piece):
    g.meshes.clear()
    if style=='comfy_silk':
        silk(piece)
        mats={'cloth':'#25334d','cream':'#fff7e7','lining':'#273148','gold':'#ddbd83','thread':'#baa476','brass':'#bfa47a','button':'#d4bd98','black':'#263049'}
        textured=('cloth','cream');texture='comfy_silk'
    else:
        button_guard(piece)
        mats={'cloth':'#ffffff','under':'#1b1e29','frame':'#5b5654','violet':'#463751','lining':'#242335','gold':'#ac9475','thread':'#b5a08f','brass':'#b7a281','button':'#c8b896','black':'#151620'}
        textured=('cloth',);texture='buttonguard'
    for mesh in g.meshes:
        mesh['faces']=[list(reversed(face)) for face in mesh['faces']]
        if mesh['material'] in textured:mesh['uv']=[[.02+.69*max(0,min(1,u)),.02+.96*max(0,min(1,v))] for u,v in mesh['uv']]
        else:mesh['uv']=[[.9,.5] for _ in mesh['vertices']]
        mesh['vertices']=[[round(x,5) for x in p] for p in mesh['vertices']]
    from reference_mesh_finish import finish
    for mesh in g.meshes:finish(mesh,mesh['material'] in textured)
    return dict(style=style+'_reference',piece=piece,texture=texture,materials=mats,meshes=g.meshes)
