"""Reference-led Dreamweave: patchwork hood, layered guards and dream-silk cape.

Independent construction, not the shared coat/pajama template. Author coordinates
are Minecraft pixels, Y down, front -Z. Atlas left 3/4 is cloth; right is solid.
"""
import math
import sculpted_coat as g
from reference_mesh_finish import finish

T=math.tau
MATERIALS={'cloth':'#ffffff','violet':'#b5a2e8','under':'#303046',
           'gold':'#d6bb7c','thread':'#d6bb7c','brass':'#bba26e',
           'button':'#c9b695','black':'#242033','lining':'#332b50'}

def surface(name,bone,fn,nu=16,nv=8,mat='cloth'):
    g.surface(name,bone,fn,nu,nv,mat)

def cord(name,bone,points,r=.045,mat='gold'):
    g.tube(name,bone,points,r,mat,5)

def star(bone,x,y,z,r=.55):
    outline=[]
    for i in range(10):
        a=-math.pi/2+i*math.pi/5;s=r if i%2==0 else r*.44
        outline.append([x+math.cos(a)*s,y+math.sin(a)*s,z])
    verts=[[x,y,z-.1]]+outline
    g.mesh('Five pointed sewn star',bone,verts,[[0,i+1,(i+1)%10+1] for i in range(10)],'gold')

def crescent(bone,x,y,z,r=1.3):
    surface('Raised crescent moon',bone,lambda u,v:[x-r*math.cos((v-.5)*math.pi*1.65)*(1-u*.63)+u*r*.2,y+r*math.sin((v-.5)*math.pi*1.65),z-.08*u],5,24,'gold')

def panel(name,bone,outline,z,mat='cloth',depth=.22):
    # Slightly domed polygon with thickness and a stitched perimeter.
    cx=sum(p[0] for p in outline)/len(outline);cy=sum(p[1] for p in outline)/len(outline)
    verts=[[cx,cy,z-.1]]+[[x,y,z] for x,y in outline]
    n=len(outline);faces=[[0,i+1,(i+1)%n+1] for i in range(n)]
    uv=[[.5,.5]]+[[.5+(x-cx)/12,.5+(y-cy)/12] for x,y in outline]
    g.mesh(name,bone,verts,faces,mat,uv)
    for i in range(n):
        a=outline[i];b=outline[(i+1)%n]
        g.mesh(name+' folded edge',bone,[[*a,z],[*b,z],[*b,z+depth],[*a,z+depth]],[[0,1,2,3]],'lining')
    pts=[]
    for i in range(n):
        a=outline[i];b=outline[(i+1)%n]
        for j in range(8):
            t=j/8;pts.append([cx+(a[0]*(1-t)+b[0]*t-cx)*.93,cy+(a[1]*(1-t)+b[1]*t-cy)*.93,z-.055])
    pts.append(pts[0]);g.stitch(name+' saddle stitch',bone,pts)

def shell(name,bone,c,rx,rz,h,mat='under',power=6):
    surface(name,bone,lambda u,v:[c[0]+g.roundrect(u*T,rx,rz,power)[0],c[1]+h*v,c[2]+g.roundrect(u*T,rx,rz,power)[1]],24,6,mat)

def strap(bone,c,rx,rz,h=.42):
    shell('Woven binding strap',bone,c,rx,rz,h,'lining')
    for y in (c[1],c[1]+h):
        cord('Strap edge',bone,[[c[0]+g.roundrect(i*T/32,rx,rz,6)[0],y,c[2]+g.roundrect(i*T/32,rx,rz,6)[1]] for i in range(33)],.045)

def hood():
    # Side walls and crown fit the player's skin outer layer; face stays open.
    def side(u,v):
        a=-1.02+u*(math.pi+2.04)
        x,z=g.roundrect(a,5.18,5.18,7)
        return [x,-8.9+9.2*v,z+.10*math.sin(v*math.pi)]
    surface('Patchwork open hood','head',side,30,12)
    surface('Quilted hood crown','head',lambda u,v:[g.roundrect(u*T,5.2,5.2,7)[0]*v,-8.9-.55*math.sin(v*math.pi),g.roundrect(u*T,5.2,5.2,7)[1]*v],28,8)
    for edge in (0,1):cord('Violet hood face binding','head',[side(edge,i/32) for i in range(33)],.16,'violet')
    panel('Pointed forehead overlap','head',[(-4.1,-8.75),(0,-9.55),(4.1,-8.75),(2.9,-7.9),(0,-8.45),(-3,-7.85)],-5.23,'violet')
    # Sew the actual dimensional button onto the side of the hood.
    start=len(g.meshes);g.button('Oversized temple button','head',0,-5.6,-5.5,1.35)
    for mesh in g.meshes[start:]:
        for p in mesh['vertices']:p[0],p[2]=-p[2],p[0]-.8
    cord('Temple charm chain','head',[[5.53,-4.5,-.8],[5.55,-3.25,-.8]],.06)
    start=len(g.meshes);star('head',0,-2.9,-5.58,.56)
    for mesh in g.meshes[start:]:
        for p in mesh['vertices']:p[0],p[2]=-p[2],p[0]-.8

def chest():
    shell('Quilted sleeveless base','body',(0,0,0),4.62,2.55,11.6,'under')
    surface('Closed shoulder yoke','body',lambda u,v:[g.roundrect(u*T,4.62,2.55,6)[0]*v,-.1,g.roundrect(u*T,4.62,2.55,6)[1]*v],24,4,'under')
    panel('Crescent chest panel','body',[(-3.9,.25),(-2.4,-.1),(0,.6),(2.4,-.1),(3.9,.25),(3.6,8.9),(0,10.25),(-3.6,8.9)],-2.87)
    crescent('body',0,4.4,-3.17,1.65);star('body',0,7.25,-3.2,.58)
    cord('Moon pendant thread','body',[[0,6.0,-3.2],[0,6.55,-3.2]],.04)
    for side in (-1,1):
        bone='left_arm' if side>0 else 'right_arm';c=side
        shell('Dark joint undersleeve',bone,(c,-1.65,0),2.22,2.22,9.6,'under')
        # Broad angular shoulder cap, layered front/side rather than a coat sleeve.
        surface('Angled quilt shoulder cap',bone,lambda u,v,c=c:[c+(u-.5)*6.1,-2.75+v*3.25+.65*abs(u-.5),-3.02+v*.2],10,5,'violet')
        surface('Shoulder crown',bone,lambda u,v,c=c:[c+(u-.5)*6.1,-2.75-.18*math.sin(v*math.pi),-3.02+v*6.04],10,6)
        shell('Shoulder side wrap',bone,(c,-2.5,0),2.99,2.96,2.35,'cloth')
        panel('Pointed shoulder facing',bone,[(c-2.95,-2.6),(c+2.95,-2.6),(c+2.7,.2),(c,.95),(c-2.7,.2)],-3.14,'violet')
        panel('Forearm woven guard',bone,[(c-2.42,3.5),(c+2.42,3.5),(c+2.35,7.8),(c,8.3),(c-2.35,7.8)],-2.63)
        for y in (2.15,7.45):strap(bone,(c,y,0),2.35,2.36,.45)
        star(bone,c,5.4,-2.9,.65)
        for x in (-1.45,1.45):star(bone,c+x,-1.05,-3.34,.35)
    strap('body',(0,9.6,0),4.72,2.76,.85)
    panel('Belt brass clasp','body',[(-.85,9.55),(.85,9.55),(.85,10.55),(-.85,10.55)],-3.08,'brass',.12)
    panel('Belt clasp inset','body',[(-.55,9.75),(.55,9.75),(.55,10.35),(-.55,10.35)],-3.23,'lining',.04)
    # Double-sided scalloped silk cape; outward offset keeps it clear of leggings.
    def cape(u,v):
        return [(u-.5)*(8.8+v*5.8),1.4+v*(18.2+1.3*math.cos((u-.5)*math.pi)),3.1+v*3.1+.38*math.sin(u*T*5)*v]
    surface('Star threaded dream cape','cape',cape,30,18,'violet')
    surface('Dream cape reverse','cape',lambda u,v:[cape(1-u,v)[0],cape(1-u,v)[1],cape(1-u,v)[2]-.075],30,18,'cloth')
    for edge in (0,1):cord('Cape edge binding','cape',[cape(edge,j/36) for j in range(37)],.065)
    cord('Scalloped cape hem','cape',[cape(j/48,1) for j in range(49)],.065)
    for u in (.08,.27,.5,.73,.92):
        x,y,z=cape(u,.94);star('cape',x,y,z+.18,.48)
    x,y,z=cape(.5,.35);crescent('cape',x,y,z+.2,1.4)

def legs():
    for side in (-1,1):
        bone='left_leg' if side>0 else 'right_leg'
        # Inner seam stays within the 3.8-pixel hip separation.
        surface('Fitted quilt leggings',bone,lambda u,v:[max(-1.84,min(1.84,g.roundrect(u*T,2.18,2.2,6)[0])),v*11.65,g.roundrect(u*T,2.18,2.2,6)[1]],24,10,'under')
        x=side*.16
        panel('Split hip tasset',bone,[(x-1.85,.0),(x+1.85,.0),(x+2.18,4.7),(x+.3,5.6),(x-1.9,4.8)],-2.65,'violet')
        panel('Pointed knee shield',bone,[(x-1.8,5.3),(x+1.8,5.3),(x+1.65,8.3),(x,9.1),(x-1.65,8.3)],-2.7)
        star(bone,x,6.8,-2.93,.8)
        for y in (5.25,8.4):strap(bone,(0,y,0),1.86,2.3,.37)
        # Off-center side sash, attached to the leg so walking does not pin both legs.
        panel('Outer hip hanging ribbon',bone,[(side*1.55,.2),(side*2.6,.3),(side*3.0,5.6),(side*2.1,5.9)],-1.35,'cloth')

def boots():
    for side in (-1,1):
        bone='left_leg' if side>0 else 'right_leg';x=side*.2
        shell('Structured boot cuff',bone,(x,8.05,0),2.12,2.45,3.8,'cloth')
        surface('Squared soft boot toe',bone,lambda u,v:[x+g.roundrect(u*T,2.14,3.02,6)[0],10.4+v*1.65,g.roundrect(u*T,2.14,3.02,6)[1]-.55],24,6,'under')
        surface('Boot vamp',bone,lambda u,v:[x+g.roundrect(u*T,2.14,3.02,6)[0]*v,10.4-.35*math.sin(v*math.pi),g.roundrect(u*T,2.14,3.02,6)[1]*v-.55],24,6)
        strap(bone,(x,11.7,-.55),2.19,3.08,.34)
        panel('Moon boot front',bone,[(x-1.95,8.25),(x+1.95,8.25),(x+2,10.55),(x-2,10.55)],-2.66,'violet')
        crescent(bone,x,9.4,-2.9,.78)
        strap(bone,(x,8.1,0),2.17,2.49,.32)

def build(piece):
    g.meshes.clear();{'helmet':hood,'chestplate':chest,'leggings':legs,'boots':boots}[piece]()
    for mesh in g.meshes:
        mesh['faces']=[list(reversed(face)) for face in mesh['faces']]
        if mesh['material'] in ('cloth','violet'):
            mesh['uv']=[[.02+.69*max(0,min(1,u)),.02+.96*max(0,min(1,v))] for u,v in mesh['uv']]
        else:mesh['uv']=[[.9,.5] for _ in mesh['vertices']]
        mesh['vertices']=[[round(n,5) for n in p] for p in mesh['vertices']]
        finish(mesh,mesh['material'] in ('cloth','violet'))
    return dict(style='dreamweave_reference',piece=piece,texture='dreamweave',materials=MATERIALS,meshes=g.meshes)
