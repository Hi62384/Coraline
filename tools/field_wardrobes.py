"""Three independently shaped reference outfits: vines, midnight and miner gear."""
import math
import sculpted_coat as g
from dreamweave_model import surface,cord,panel,shell,crescent,star,strap,T

def cap(name,bone,c,rx,rz,mat):
    surface(name,bone,lambda u,v:[c[0]+g.roundrect(u*T,rx,rz,8)[0]*v,c[1],c[2]+g.roundrect(u*T,rx,rz,8)[1]*v],24,4,mat)

def leaf(bone,x,y,z,size=1,angle=0,mat='leaf'):
    pts=[]
    for px,py in [(0,-1),(.6,-.2),(.43,.55),(0,1),(-.43,.55),(-.6,-.2)]:
        pts.append((x+size*(px*math.cos(angle)-py*math.sin(angle)),y+size*(px*math.sin(angle)+py*math.cos(angle))))
    panel('Raised pointed leaf',bone,pts,z,mat,.08)
    cord('Leaf central vein',bone,[[x-size*math.sin(angle)*t,y+size*math.cos(angle)*t,z-.15] for t in (-.7,0,.8)],.035,'vein')

def flower(bone,x,y,z,r=.75):
    for i in range(5):
        a=i*T/5;g.ellipse('Ivory flower petal',bone,[x+math.sin(a)*r*.6,y+math.cos(a)*r*.6,z],r*.45,r*.45,.15,'cream')
    g.ellipse('Flower gold heart',bone,[x,y,z-.19],r*.25,r*.25,.13,'gold')

def vine(bone,c,rx,rz,height,turns=1.3):
    pts=[]
    for i in range(49):
        a=i/48*T*turns;px,pz=g.roundrect(a,rx,rz,5)
        pts.append([c[0]+px,c[1]+height*i/48,c[2]+pz])
    cord('Thick climbing vine',bone,pts,.19,'vine')
    cord('Vine raised twist',bone,[[p[0]+.1*math.sin(i),p[1],p[2]+.1*math.cos(i)] for i,p in enumerate(pts)],.045,'vein')

def fitted_legs():
    for s in (-1,1):
        bone='left_leg' if s>0 else 'right_leg'
        surface('Fitted joint trousers',bone,lambda u,v:[max(-1.83,min(1.83,g.roundrect(u*T,2.2,2.3,7)[0])),v*11.7,g.roundrect(u*T,2.2,2.3,7)[1]],24,8,'under')

def garden(piece):
    if piece=='helmet':
        shell('Moss patchwork guardian shell','head',(0,-9,0),5.12,5.12,9.3,'cloth',8);cap('Moss crown','head',(0,-9,0),5.12,5.12,'cloth')
        panel('Shadowed guardian face','head',[(-4.5,-6.4),(4.5,-6.4),(4.5,.25),(-4.5,.25)],-5.25,'black')
        for s in (-1,1):
            panel('Turquoise eye inset','head',[(s*2.6-.32,-5.5),(s*2.6+.32,-5.5),(s*2.6+.32,-3.3),(s*2.6-.32,-3.3)],-5.42,'glow')
            panel('Layered moss cheek','head',[(s*4.7,-6.8),(s*3.65,-6.8),(s*3.6,-1.5),(s*2.7,.1),(s*4.7,.1)],-5.52,'moss')
            cord('Curling crown shoot','head',[[s*3.8,-8.5,-.6],[s*4.15,-10,-.6]]+[[s*(4.0+.7*math.cos(i*.18)),-10.55-.7*math.sin(i*.18),-.6] for i in range(27)],.18,'vine')
            leaf('head',s*4.3,-10.7,-.9,.75,s*.6)
        vine('head',(0,-8.4,0),5.3,5.3,1.7,.95)
        for i in range(7):leaf('head',(i-3)*1.25,-8.0+(i%2)*.35,-5.62,1.05,(i-3)*.33)
        for x,y in ((-4.4,-7.8),(4.4,-2.0)):flower('head',x,y,-5.83,.76)
        g.button('Crown sewn wooden button','head',0,-7.6,-6.0,.7)
        for s in (-1,1):
            for i in range(3):
                start=len(g.meshes);leaf('head',0,-6+i*1.8,-5.43,1,(i-1)*.45)
                for m in g.meshes[start:]:
                    for p in m['vertices']:p[0],p[2]=-s*p[2],s*p[0]+.8
    elif piece=='chestplate':
        shell('Weathered garden vest','body',(0,0,0),4.6,2.65,11.4,'cloth');cap('Vest closed yoke','body',(0,0,0),4.6,2.65,'cloth')
        panel('Ivory stitched garden bib','body',[(-2.6,.5),(2.6,.5),(2.8,9.7),(0,11.3),(-2.8,9.7)],-2.96,'cream')
        panel('Turquoise pendant','body',[(0,3.4),(.65,4.5),(0,5.7),(-.65,4.5)],-3.28,'glow')
        g.button('Bib wood clasp','body',0,2.8,-3.45,.62)
        for s in (-1,1):
            bone='left_arm' if s>0 else 'right_arm';x=s
            shell('Garden under sleeve',bone,(x,-2,0),2.23,2.23,10,'under')
            shell('Broad layered moss shoulder',bone,(x,-2.65,0),3.05,2.95,2.9,'moss',8);cap('Moss shoulder crown',bone,(x,-2.65,0),3.05,2.95,'moss')
            panel('Weathered shoulder facing',bone,[(x-3,-2.5),(x+3,-2.5),(x+2.7,.65),(x-2.7,.65)],-3.13,'cloth')
            panel('Garden forearm guard',bone,[(x-2.45,3.6),(x+2.45,3.6),(x+2.4,8),(x-2.4,8)],-2.67,'cloth')
            vine(bone,(x,-2.1,0),3.18,3.19,2.7,.8);vine(bone,(x,3.7,0),2.53,2.91,4.1,1.1)
            for xx,yy,a in ((x-1.4,-1.1,-.65),(x+1.4,-1,.6),(x+1,6,.5)):leaf(bone,xx,yy,-3.4,.9,a)
            flower(bone,x,-1.5,-3.75,.8);leaf(bone,x,5.5,-3.02,.7,0,'glow')
            for i in range(4):
                leaf(bone,x-2.2+i*1.45,.15+(i%2)*.65,-3.5,.85,(i-1.5)*.45)
            for i in range(3):
                start=len(g.meshes);leaf(bone,0,-1+i*1.6,-3.25,.9,i*.3)
                for m in g.meshes[start:]:
                    for p in m['vertices']:p[0],p[2]=x-s*p[2],s*p[0]
        vine('body',(0,8.8,0),4.75,2.96,1.6,1.7)
        for x in (-3.7,-2,2,3.7):leaf('body',x,9.8,-3.21,.8,x*.2)
        g.button('Vine belt wooden clasp','body',0,9.65,-3.65,.85)
        flower('body',3.5,7.9,-3.26,.6)
        vine('body',(0,1,0),4.7,2.85,6.7,.8)
    elif piece=='leggings':
        fitted_legs()
        for s in (-1,1):
            b='left_leg' if s>0 else 'right_leg'
            panel('Moss layered hip flap',b,[(-1.8,0),(1.8,0),(2,3.5),(-1.8,4.5)],-2.6,'moss')
            panel('Stitched ivory hanging cloth',b,[(-1.6,.1),(1.4,.1),(1.35,5.5),(0,6.3),(-1.6,5.6)],-2.85,'cream')
            vine(b,(0,5,0),1.86,2.6,5.5,1.3)
            leaf(b,0,7,-2.9,.8,0,'glow');leaf(b,s*1.45,4,-3,.8,s*.5)
            for i in range(3):leaf(b,s*1.2,1+i*1.2,-3.1,.7,s*.7)
    else:
        for s in (-1,1):
            b='left_leg' if s>0 else 'right_leg';x=s*.22
            shell('Moss heavy boot',b,(x,8.3,-.5),2.04,2.95,3.65,'moss',8);cap('Moss boot vamp',b,(x,8.3,-.5),2.04,2.95,'cloth')
            vine(b,(x,8.35,-.5),2.13,3.05,3.3,1.15)
            for xx,a in ((x-1,-.5),(x+1,.5)):leaf(b,xx,10.9,-3.67,.75,a)
            flower(b,x,9.3,-3.64,.65);leaf(b,x,11.2,-3.85,.55,0,'glow')

def fourstar(b,x,y,z,r=.65):
    panel('Four point embroidered star',b,[(x,y-r),(x+.18,y-.18),(x+r*.65,y),(x+.18,y+.18),(x,y+r),(x-.18,y+.18),(x-r*.65,y),(x-.18,y-.18)],z,'gold',.04)

def midnight(piece):
    if piece=='helmet':
        surface('Squared open midnight helmet','head',lambda u,v:[g.roundrect(-1.02+u*(math.pi+2.04),5.1,5.1,10)[0],-9+9.25*v,g.roundrect(-1.02+u*(math.pi+2.04),5.1,5.1,10)[1]],30,8,'cloth')
        cap('Squared midnight crown','head',(0,-9,0),5.1,5.1,'cloth')
        panel('Crescent forehead crest','head',[(-4.9,-8.9),(4.9,-8.9),(4.9,-6.7),(1.4,-6.7),(0,-5.75),(-1.4,-6.7),(-4.9,-6.7)],-5.22,'cloth')
        crescent('head',0,-7.75,-5.49,1.0)
        for s in (-1,1):
            start=len(g.meshes);g.button('Blue temple button','head',0,-5.6,-5.38,.78)
            for m in g.meshes[start:]:
                for p in m['vertices']:p[0],p[2]=-s*p[2],s*p[0]
    elif piece=='chestplate':
        shell('Midnight fitted torso','body',(0,0,0),4.6,2.6,11.5,'under');cap('Midnight shoulder yoke','body',(0,0,0),4.6,2.6,'cloth')
        panel('Pointed silver edged chest','body',[(-3.9,.25),(3.9,.25),(3.8,9.5),(0,11),(-3.8,9.5)],-2.92,'cloth')
        g.button('Ivory collar button','body',0,1.2,-3.25,.48)
        for y in (4.4,7):fourstar('body',0,y,-3.14,.75)
        cord('Pendant star thread','body',[[0,2,-3.12],[0,7.8,-3.12]],.028)
        for s in (-1,1):
            b='left_arm' if s>0 else 'right_arm';x=s
            shell('Fitted midnight sleeve',b,(x,-2,0),2.22,2.22,10,'under')
            shell('Squared crescent shoulder',b,(x,-2.7,0),2.98,2.93,3.5,'cloth',10);cap('Flat crescent shoulder top',b,(x,-2.7,0),2.98,2.93,'cloth')
            panel('Squared shoulder face',b,[(x-2.9,-2.55),(x+2.9,-2.55),(x+2.9,.7),(x-2.9,.7)],-3.03,'cloth')
            crescent(b,x,-1,-3.25,1.1);fourstar(b,x,3.5,-2.5,.63)
            for y in (5.7,7.5):shell('Silver bound cuff',b,(x,y,0),2.35,2.35,.36,'thread')
        def cape(u,v):return [(u-.5)*7.1,.6+v*(18.8-3*abs(2*u-1)),3.12+v*2.9]
        surface('Pointed moon mantle','cape',cape,16,14,'cloth');surface('Mantle inside','cape',lambda u,v:[cape(1-u,v)[0],cape(1-u,v)[1],cape(1-u,v)[2]-.07],16,14,'under')
        for edge in (0,1):cord('Mantle silver binding','cape',[cape(edge,i/24) for i in range(25)],.065,'thread')
        cord('Pointed mantle hem','cape',[cape(i/24,1) for i in range(25)],.065,'thread')
        crescent('cape',0,7,4.4,1.9);fourstar('cape',0,11,4.95,.75)
    elif piece=='leggings':
        fitted_legs()
        for s in (-1,1):
            b='left_leg' if s>0 else 'right_leg'
            panel('Pointed midnight thigh cloth',b,[(-1.8,0),(1.8,0),(1.8,5.5),(0,6.9),(-1.8,5.5)],-2.6,'cloth')
            crescent(b,0,3.4,-2.89,.75);fourstar(b,0,7.7,-2.5,.48)
    else:
        for s in (-1,1):
            b='left_leg' if s>0 else 'right_leg';x=s*.2
            shell('Midnight squared boot',b,(x,8.2,-.4),2.05,2.85,3.75,'cloth',10);cap('Midnight boot top',b,(x,8.2,-.4),2.05,2.85,'cloth')
            for y in (8.3,11.3):shell('Silver boot binding',b,(x,y,-.4),2.09,2.9,.36,'thread',10)
            fourstar(b,x,10,-3.44,.6)

def lantern(b,x,y,z,scale=1,blue=False):
    panel('Lantern brass housing',b,[(x-scale,y-scale*1.4),(x+scale,y-scale*1.4),(x+scale,y+scale*1.4),(x-scale,y+scale*1.4)],z,'brass',.55)
    panel('Lantern luminous glass',b,[(x-scale*.65,y-scale),(x+scale*.65,y-scale),(x+scale*.65,y+scale),(x-scale*.65,y+scale)],z-.2,'blue' if blue else 'amber',.1)
    panel('Lantern bright core',b,[(x,y-scale*.8),(x+scale*.32,y),(x,y+scale*.8),(x-scale*.32,y)],z-.33,'glow',.1)
    for sx in (-1,1):cord('Lantern vertical cage',b,[[x+sx*scale*.65,y-scale,z-.36],[x+sx*scale*.65,y+scale,z-.36]],.06,'brass')
    for sx in (-1,1):
        for sy in (-1,1):g.ellipse('Lantern rivet',b,[x+sx*scale*.82,y+sy*scale*1.2,z-.18],.07,.07,.07,'gold')
    cord('Lantern handle',b,[[x-scale*.45,y-scale*1.4,z],[x-scale*.45,y-scale*1.85,z],[x+scale*.45,y-scale*1.85,z],[x+scale*.45,y-scale*1.4,z]],.085,'brass')

def pouch(b,x,y,z):
    panel('Leather tool pouch',b,[(x-.7,y),(x+.7,y),(x+.72,y+1.8),(x-.72,y+1.8)],z,'leather',.6)
    panel('Pouch folded flap',b,[(x-.75,y),(x+.75,y),(x+.65,y+.7),(x,y+1),(x-.65,y+.7)],z-.2,'leather')
    g.button('Pouch sewn clasp',b,x,y+.5,z-.45,.27)

def miners(piece):
    if piece=='helmet':
        shell('Miner reinforced cap','head',(0,-9.1,0),5.03,5.03,3.5,'cloth',9);cap('Miner cap crown','head',(0,-9.1,0),5.03,5.03,'cloth')
        shell('Wide leather head band','head',(0,-6.8,0),5.18,5.18,.85,'leather',9)
        cap('Projecting miner brim','head',(0,-5.85,-.55),5.65,5.45,'cloth')
        lantern('head',0,-7.65,-5.68,1.18)
        for x in (-4,-2.5,2.5,4):g.ellipse('Band brass rivet','head',[x,-6.4,-5.32],.09,.09,.07,'brass')
        g.button('Miner dangling cap button','head',-4.55,-4.95,-3.8,.6)
    elif piece=='chestplate':
        shell('Miner work tunic','body',(0,0,0),4.55,2.6,11.5,'cloth');cap('Work tunic shoulders','body',(0,0,0),4.55,2.6,'cloth')
        panel('Ragged crescent bib','body',[(-2.9,0),(3.3,.5),(3,10.9),(1.8,10.3),(1,11.3),(-.4,10.5),(-1.5,11),(-3,10)],-2.93,'cloth')
        crescent('body',1.0,4.5,-3.18,1.25)
        panel('Diagonal leather tool harness','body',[(-4,.2),(-3.1,-.1),(3.7,8.9),(2.8,9.5)],-3.35,'leather')
        g.button('Harness button','body',-.5,4.7,-3.63,.47)
        for s in (-1,1):
            b='left_arm' if s>0 else 'right_arm';x=s
            shell('Miner jointed sleeve',b,(x,-1.9,0),2.22,2.22,10,'under')
            shell('Miner strapped shoulder',b,(x,-2.5,0),2.85,2.8,2.7,'cloth',8);cap('Miner shoulder top',b,(x,-2.5,0),2.85,2.8,'cloth')
            for y in (-2.3,-.2,4.0,7.3):shell('Leather arm binding',b,(x,y,0),2.43 if y>0 else 2.94,2.43 if y>0 else 2.89,.55,'leather')
            panel('Miner forearm pad',b,[(x-2.15,3.8),(x+2.15,3.8),(x+2.15,7.7),(x-2.15,7.7)],-2.6,'cloth')
            g.button('Shoulder brass button',b,x,-.9,-3.05,.6)
        shell('Leather utility belt','body',(0,9.1,0),4.7,2.8,.85,'leather')
        panel('Miner wrapped neck scarf','body',[(-3.6,.1),(3.6,.1),(2.3,1.3),(0,1.8),(-2.3,1.3)],-3.15,'cloth')
        pouch('body',-3.4,8.7,-3.2);lantern('body',4,10.9,-3.05,.58,True)
        # Small worn pick on the belt, decorative only.
        cord('Tool wooden handle','body',[[2.8,8.3,-3.5],[1.6,11.4,-3.5]],.13,'leather')
        cord('Curved pick head','body',[[1.5,8.5,-3.6],[2.7,8,-3.6],[3.7,8.45,-3.6]],.18,'steel')
        def cape(u,v):return [(u-.5)*7.3,1.2+v*(15.4+.35*math.cos(u*T*9)),3.1+v*2.7]
        surface('Miner ragged back cloth','cape',cape,24,12,'cloth');surface('Miner cloth inner','cape',lambda u,v:[cape(1-u,v)[0],cape(1-u,v)[1],cape(1-u,v)[2]-.05],24,12,'under')
        crescent('cape',0,6,4.25,1.5)
    elif piece=='leggings':
        fitted_legs()
        for s in (-1,1):
            b='left_leg' if s>0 else 'right_leg'
            panel('Miner split ragged tabard',b,[(-1.75,0),(1.75,0),(1.7,4.8),(.8,4.5),(0,5.3),(-.7,4.7),(-1.75,5)],-2.68,'cloth')
            for slope in (-1,1):cord('Crossed pick embroidery',b,[[-.65,1.6+(slope+1)*.6,-2.93],[.65,1.6+(1-slope)*.6,-2.93]],.09,'thread')
            pouch(b,s*1.9,.2,-2.1)
            for i in range(3):panel('Pouch blue crystal',b,[(s*1.9-.45+i*.4,-.1),(s*1.9-.2+i*.4,-.85-(i%2)*.3),(s*1.9+i*.4,-.1)],-2.4,'blue',.15)
            for y in (5.5,8):shell('Miner knee strap',b,(0,y,0),1.86,2.43,.5,'leather')
            panel('Brass reinforced knee',b,[(-1.7,5.45),(1.7,5.45),(1.7,8),(-1.7,8)],-2.62,'leather')
            g.button('Knee brass fastening',b,0,6.7,-2.95,.62)
    else:
        for s in (-1,1):
            b='left_leg' if s>0 else 'right_leg';x=s*.2
            shell('Heavy work boot',b,(x,8.25,-.5),2.06,3.02,3.75,'cloth',10);cap('Squared work boot vamp',b,(x,8.25,-.5),2.06,3.02,'cloth')
            for y in (8.4,11.35):shell('Work boot leather strap',b,(x,y,-.5),2.13,3.1,.55,'leather',10)
            panel('Brass toe reinforcement',b,[(x-2.12,11),(x+2.12,11),(x+2.12,11.75),(x-2.12,11.75)],-3.7,'brass')

def build(style,piece):
    g.meshes.clear();{'garden_vines':garden,'midnight':midnight,'miners':miners}[style](piece)
    common={'gold':'#e4d5a7','thread':'#b5bcae','brass':'#a48346','button':'#bda16c','black':'#101d20','lining':'#283438'}
    palettes={
        'garden_vines':dict(cloth='#405e54',under='#263c38',moss='#697648',cream='#c9c7a2',leaf='#668139',vine='#485827',vein='#8f9b54',glow='#8de8d9'),
        'midnight':dict(cloth='#293a59',under='#18253b',button='#73839c',brass='#8e9db2',thread='#bfc3b0'),
        'miners':dict(cloth='#354158',under='#333239',leather='#6f5032',steel='#9fa4aa',amber='#ffbe45',blue='#628bf7',glow='#effbff')}
    mats=common|palettes[style]
    mats['cloth']='#ffffff'
    for m in g.meshes:
        if m['material'] in ('glow','amber','blue'):m['emissive']=True
        m['faces']=[list(reversed(f)) for f in m['faces']]
        # Correct front/back appliques without turning closed shells inside out.
        area=0
        for f in m['faces']:
            a,b,c=(m['vertices'][i] for i in f[:3]);area+=(b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
        if (all(v[2]<-1.8 for v in m['vertices']) and area>.1) or (all(v[2]>1.8 for v in m['vertices']) and area<-.1):m['faces']=[list(reversed(f)) for f in m['faces']]
        m['vertices']=[[round(n,5) for n in p] for p in m['vertices']]
        if m['material']=='cloth':m['uv']=[[.02+.69*max(0,min(1,u)),.02+.96*max(0,min(1,v))] for u,v in m['uv']]
        else:m['uv']=[[.9,.5] for _ in m['vertices']]
    from reference_mesh_finish import finish
    for mesh in g.meshes:finish(mesh,mesh['material']=='cloth')
    return dict(style=style+'_reference',piece=piece,texture=style,materials=mats,meshes=g.meshes)
