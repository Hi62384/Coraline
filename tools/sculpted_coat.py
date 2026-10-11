"""Midnight Coat mesh prototype. Real curved surfaces, not ModelPart cuboids.

Author coordinates are Minecraft pixels (front = -Z, down = +Y).
Every mesh is assigned to a humanoid bone for the eventual wearable renderer.
This first sculpt is deliberately kept outside release resources until visual approval.
"""
from pathlib import Path
import json, math

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artwork/sculpted_wardrobe'
TAU=math.tau
meshes=[]

def mesh(name,bone,verts,faces,mat,uv=None):
    meshes.append(dict(name=name,bone=bone,vertices=verts,faces=faces,material=mat,uv=uv or [[0,0]]*len(verts)))

def surface(name,bone,fn,nu,nv,mat,close=False):
    verts=[];uv=[];faces=[]
    for j in range(nv+1):
        for i in range(nu+1):
            u=i/nu;v=j/nv;verts.append(fn(u,v));uv.append([u,v])
    for j in range(nv):
        for i in range(nu):
            a=j*(nu+1)+i;faces.append([a,a+1,a+nu+2,a+nu+1])
    mesh(name,bone,verts,faces,mat,uv)

def tube(name,bone,points,radius,mat,sides=6):
    verts=[];faces=[]
    for j,p in enumerate(points):
        a=points[max(0,j-1)];b=points[min(len(points)-1,j+1)]
        tangent=[b[k]-a[k] for k in range(3)];l=math.sqrt(sum(t*t for t in tangent)) or 1;tangent=[t/l for t in tangent]
        ref=[0,0,1] if abs(tangent[2])<.9 else [0,1,0]
        cross=lambda a,b:[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
        n=cross(tangent,ref);l=math.sqrt(sum(t*t for t in n));n=[t/l for t in n];b=cross(tangent,n)
        for i in range(sides):
            angle=TAU*i/sides;verts.append([p[k]+radius*(n[k]*math.cos(angle)+b[k]*math.sin(angle)) for k in range(3)])
    for j in range(len(points)-1):
        for i in range(sides):a=j*sides+i;b=j*sides+(i+1)%sides;faces.append([a,b,b+sides,a+sides])
    mesh(name,bone,verts,faces,mat)

def ellipse(name,bone,center,rx,ry,depth,mat):
    x,y,z=center
    surface(name,bone,lambda u,v:[x+rx*math.cos(u*TAU)*math.sin(v*math.pi),y+ry*math.sin(u*TAU)*math.sin(v*math.pi),z+depth*math.cos(v*math.pi)],16,6,mat)

def button(name,bone,x,y,z,size,mat='button'):
    ellipse(name,bone,[x,y,z],size,size,.15,mat)
    tube(name+' rim',bone,[[x+size*.85*math.cos(i*TAU/32),y+size*.85*math.sin(i*TAU/32),z-.13] for i in range(33)],.055,'brass',6)
    for dx in (-.25,.25):
        for dy in (-.25,.25):ellipse(name+' hole',bone,[x+dx*size,y+dy*size,z-.157],size*.105,size*.105,.025,'black')
    for s in (-1,1):tube(name+' thread',bone,[[x-size*.25,y-s*size*.25,z-.195],[x,y,z-.24],[x+size*.25,y+s*size*.25,z-.195]],.045,'lining')

def stitch(name,bone,points,mat='thread'):
    for i in range(0,len(points)-1,2):tube(name+str(i),bone,points[i:i+2],.027,mat,4)

def star(name,bone,x,y,z,size):
    for angle in (0,math.pi/2,math.pi/4,math.pi*3/4):
        r=size if angle in (0,math.pi/2) else size*.6
        tube(name,bone,[[x-math.cos(angle)*r,y-math.sin(angle)*r,z],[x,y,z-.06],[x+math.cos(angle)*r,y+math.sin(angle)*r,z]],.034,'thread',4)

def roundrect(angle,rx,rz,power=4):
    c=math.cos(angle);s=math.sin(angle)
    return math.copysign(abs(c)**(2/power),c)*rx,math.copysign(abs(s)**(2/power),s)*rz

def build():
    meshes.clear()
    # Shaped velvet shell; sculpted vertical wrinkles and tailored waist.
    def torso(u,v,inner=False):
        a=u*TAU;rx=4.65-.35*math.sin(v*math.pi)+.22*v;rz=2.7-.17*math.sin(v*math.pi)
        x,z=roundrect(a,rx,rz);fold=.08*math.sin(a*11+v*4)+.035*math.sin(v*19+a*5)
        return [x*(1+fold*.16),-.4+12.4*v,z*(1+fold*.3)+(0.11 if inner and z<0 else -.11 if inner else 0)]
    surface('Velvet tailored body','body',torso,48,16,'velvet')
    # Broad turned-back lapels, modelled as curved cloth panels, with contrasting piping.
    for side in (-1,1):
        def lapel(u,v,s=side):
            x=s*((.4+u*3.8)*(1-v*.55));y=-.75+v*6.9+u*.65
            z=-2.96-.38*math.sin(u*math.pi)-.35*math.sin(v*math.pi)
            return [x,y,z]
        surface('Rolled lapel','body',lapel,10,12,'velvet')
        stitch('Lapel saddle stitches','body',[lapel(.94,i/40) for i in range(41)])
        tube('Lapel piping','body',[lapel(1,i/24) for i in range(25)],.067,'piping')
    # Raised soft collar rather than rigid shoulder armor.
    surface('Standing curved collar','body',lambda u,v:[3.1*math.cos(u*math.pi),-1.4-1.8*v,2.35*math.sin(u*math.pi)+.12+v*.5],32,5,'velvet')
    surface('Teal collar lining','body',lambda u,v:[3.02*math.cos(u*math.pi),-1.4-1.8*v,2.29*math.sin(u*math.pi)+.10+v*.5],32,5,'lining')
    for k,(x,y,r) in enumerate(((.5,6.2,.7),(.4,8.5,.55),(.2,10.5,.59))):button('Mismatched button '+str(k),'body',x,y,-2.99,r)
    # Silver crescent brooch with crescent silhouette rather than a circle decal.
    a=[];b=[]
    for i in range(25):
        t=-math.pi*.68+math.pi*1.36*i/24
        a.append([-2.1-.7*math.cos(t),2.3+.8*math.sin(t),-3.48]);b.append([-2.1-.32*math.cos(t)+.07,2.3+.8*math.sin(t),-3.56])
    mesh('Crescent silver brooch','body',a+b,[[i,i+1,i+26,i+25] for i in range(24)],'silver')
    # Arms fit around animated Minecraft limbs; rounded-square rings contain the skin corners.
    for left in (False,True):
        bone='left_arm' if left else 'right_arm';cx=1 if left else -1
        def sleeve(u,v):
            angle=u*TAU;radius=2.55+.2*math.cos(v*TAU)+.18*max(0,(v-.65)/.35)
            x,z=roundrect(angle,radius,radius,4.8);wrinkle=.1*math.sin(v*25+angle*3)*math.sin(v*math.pi)
            return [cx+x,-2.6+10.6*v,z+wrinkle]
        surface('Fitted velvet sleeve',bone,sleeve,32,18,'velvet')
        def cuff(u,v):
            p=sleeve(u,.81+v*.2);p[0]=cx+(p[0]-cx)*1.045;p[2]*=1.045;return p
        surface('Embroidered rolled cuff',bone,cuff,32,5,'velvet')
        for v in (.07,.93):stitch('Cuff hem stitches',bone,[cuff(i/80,v) for i in range(81)])
        for x,y,r in ((cx-.9,6.3,.43),(cx+.8,7,.32)):star('Cuff star',bone,x,y,-2.95,r)
        tube('Cuff constellation',bone,[[cx-1.6+i*.15,6.8+.32*math.sin(i*.3),-2.98] for i in range(22)],.027,'thread',4)
    # Split tail panels anchored independently to the two leg bones.
    for side in (-1,1):
        bone='left_leg' if side>0 else 'right_leg';origin=side*1.9
        def skirt(u,v,inner=False):
            angle=(-math.pi/2+.09)+u*(math.pi-.18)
            if side<0:angle=math.pi-angle
            rx=4.8+v*2.05;rz=2.75+v*1.1
            x,z=roundrect(angle,rx,rz,3.8)
            fold=(math.sin(u*math.pi*8+v*1.1)*.23+math.sin(u*math.pi*14)*.08)*v
            y=v*(7.1+1.2*math.sin(u*math.pi)-.75*math.cos(u*TAU))
            return [x-origin+math.cos(angle)*fold,y,z+math.sin(angle)*fold+(.085 if inner and z<0 else -.085 if inner else 0)]
        surface('Flowing velvet coat tail',bone,skirt,32,15,'velvet')
        surface('Teal satin tail lining',bone,lambda u,v:skirt(u,v,True),32,15,'lining')
        for u in (.01,.99):
            tube('Tail piping',bone,[skirt(u,i/32) for i in range(33)],.045,'piping')
            stitch('Tail edge stitches',bone,[skirt(u,i/64) for i in range(65)])
        stitch('Tail hem stitches',bone,[skirt(i/100,.94) for i in range(101)])
        for u in (.1,.22,.34,.48,.63,.77,.89):
            p=skirt(u,.86);p[2]-=.07 if p[2]<0 else -.07
            star('Hem constellation stars',bone,*p,.28)
        tube('Curving hem embroidery',bone,[skirt(i/90,.78+.045*math.sin(i*.3)) for i in range(91)],.026,'thread',4)
    # Soft pocket with visibly rounded bottom, stitching and real thread spool.
    def pocket(u,v):return [1.7+u*2.25,8+v*2.7-.3*math.sin(u*math.pi),-3.05-.35*math.sin(u*math.pi)*math.sin(v*math.pi)]
    surface('Sewn rounded pocket','body',pocket,12,10,'velvet')
    for u in (.05,.95):stitch('Pocket stitches','body',[pocket(u,i/36) for i in range(37)])
    stitch('Pocket bottom stitches','body',[pocket(i/32,.95) for i in range(33)])
    surface('Blue thread spool','body',lambda u,v:[3+.38*math.cos(u*TAU),6.7+v*1.5,-2.95+.38*math.sin(u*TAU)],24,8,'lining')
    for y in (6.7,8.2):surface('Wood spool end','body',lambda u,v,y=y:[3+(.49+.04*math.sin(v*math.pi))*math.cos(u*TAU),y+v*.15,-2.95+(.49+.04*math.sin(v*math.pi))*math.sin(u*TAU)],24,2,'brass')
    for j in range(10):tube('Wound spool thread','body',[[3+.39*math.cos(i*TAU/24),6.8+j*.13,-2.95+.39*math.sin(i*TAU/24)] for i in range(25)],.018,'piping',4)
    OUT.mkdir(parents=True,exist_ok=True)
    data={'name':'midnight_coat','materials':{'velvet':'#142644','lining':'#126a7d','thread':'#d5d1b8','piping':'#345371','button':'#242b32','black':'#090c12','brass':'#8b7351','silver':'#c6cbd0'},'meshes':meshes}
    (OUT/'midnight_coat_mesh.json').write_text(json.dumps(data,separators=(',',':')))
    print(len(meshes),'meshes;',sum(len(m['faces']) for m in meshes),'polygons')

if __name__=='__main__':build()
