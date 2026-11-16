"""Minecraft-style cast authoring. Execute through Blender MCP in Blender 5.2.

Only cuboids, stepped prisms and rectangular pixel-art surfaces are used.
Meshes retain the same articulated joint names as the native mod renderer.
"""
import bpy, math, json, struct
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
base_path=ROOT/'tools/build_character_cast.py'
BASE={'__file__':str(base_path)}
exec(compile(base_path.read_text(encoding='utf8').split('\nfor name in CAST:')[0],str(base_path),'exec'),BASE)
IDS=BASE['CAST'];SKIN='#C39C78';DARK='#171F2A';GOLD='#D3A546'
ATLAS=bpy.data.images.load(str(ROOT/'src/main/resources/assets/coraline/characters/cast_atlas.png'),check_existing=True)
ATLAS.pack()

class Builder(BASE['Author']):
    def __init__(self,name):
        super().__init__(name);self.pixel_style=True;bpy.context.scene.name='MinecraftCast_'+name
    def surface(self,name,vertices,faces,color,bone='body',uv=None):
        super().mesh(name,vertices,faces,color,bone)
        obj=self.geometry[-1]
        for p in obj.data.polygons:p.use_smooth=False
        uv=uv or [(.00097656,.99609375)]*len(vertices)
        self.meshes[-1]['uv']=uv
        layer=obj.data.uv_layers.new(name='Pixel UV')
        for polygon in obj.data.polygons:
            for index in polygon.loop_indices:
                u,v=uv[obj.data.loops[index].vertex_index];layer.data[index].uv=(u,1-v)
        mat=self.materials[color];nodes=mat.node_tree.nodes;links=mat.node_tree.links
        shader=next(n for n in nodes if n.type=='BSDF_PRINCIPLED');shader.inputs['Roughness'].default_value=1
        if 'Specular IOR Level' in shader.inputs:shader.inputs['Specular IOR Level'].default_value=0
        if not any(n.type=='TEX_IMAGE' for n in nodes):
            image=nodes.new('ShaderNodeTexImage');image.image=ATLAS;image.interpolation='Closest'
            mix=nodes.new('ShaderNodeMixRGB');mix.blend_type='MULTIPLY';mix.inputs[0].default_value=1
            mix.inputs[1].default_value=shader.inputs['Base Color'].default_value
            links.new(image.outputs['Color'],mix.inputs[2]);links.new(mix.outputs[0],shader.inputs['Base Color'])
    def box(self,name,c,size,color,bone='body',face_tile=None,fabric=True):
        x,y,z=c;w,h,d=size;x-=w/2;y-=h/2;z-=d/2
        points=[(x,y,z),(x+w,y,z),(x+w,y+h,z),(x,y+h,z),(x,y,z+d),(x+w,y,z+d),(x+w,y+h,z+d),(x,y+h,z+d)]
        quads=[(0,3,2,1),(4,5,6,7),(0,4,7,3),(1,2,6,5),(0,1,5,4),(3,7,6,2)]
        vertices=[];faces=[];uv=[]
        for i,quad in enumerate(quads):
            start=len(vertices);vertices.extend(points[j] for j in quad);faces.append(tuple(range(start,start+4)))
            if i==0 and face_tile is not None:
                x0=face_tile*32+.01;x1=face_tile*32+31.99;y0=.01;y1=31.99
            elif fabric:x0=.5;x1=31.5;y0=32.5;y1=63.5
            else:x0=x1=.5;y0=y1=127.5
            uv.extend([(x0/512,y0/128),(x0/512,y1/128),(x1/512,y1/128),(x1/512,y0/128)] if i==0 else [(x0/512,y0/128),(x1/512,y0/128),(x1/512,y1/128),(x0/512,y1/128)])
        self.surface(name,vertices,faces,color,bone,uv)
    def plate(self,name,c,size,color,bone='body',motif=False):
        x,y,z=c;w,h=size
        vertices=[(x-w/2,y-h/2,z),(x-w/2,y+h/2,z),(x+w/2,y+h/2,z),(x+w/2,y-h/2,z)]
        uv=[(32.5/512,64.5/128),(32.5/512,95.5/128),(63.5/512,95.5/128),(63.5/512,64.5/128)] if motif else None
        self.surface(name,vertices,[(0,1,2,3)],color,bone,uv)
    def prism(self,name,sections,color,bone='body'):
        # Octagonal cross sections cut straight bevels into an otherwise box-shaped silhouette.
        vertices=[]
        for y,w,d in sections:
            q=.16
            vertices.extend([(-w/2+w*q,y,-d/2),(w/2-w*q,y,-d/2),(w/2,y,-d/2+d*q),(w/2,y,d/2-d*q),(w/2-w*q,y,d/2),(-w/2+w*q,y,d/2),(-w/2,y,d/2-d*q),(-w/2,y,-d/2+d*q)])
        faces=[tuple(reversed(range(8)))]
        for j in range(len(sections)-1):
            for i in range(8):faces.append((j*8+i,j*8+(i+1)%8,(j+1)*8+(i+1)%8,(j+1)*8+i))
        faces.append(tuple(range((len(sections)-1)*8,len(sections)*8)))
        self.surface(name,vertices,faces,color,bone)
    def bar(self,name,a,b,width,depth,color,bone='body'):
        a=Vector(a);b=Vector(b);direction=b-a;axis=direction.normalized()
        cross=axis.cross(Vector((0,0,1)))
        if cross.length<.01:cross=axis.cross(Vector((0,1,0)))
        cross.normalize();other=axis.cross(cross).normalized()
        vertices=[]
        for p in [a,b]:
            for sx,sy in [(-1,-1),(1,-1),(1,1),(-1,1)]:vertices.append(tuple(p+cross*sx*width/2+other*sy*depth/2))
        self.surface(name,vertices,[(3,2,1,0),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7),(4,5,6,7)],color,bone)
    def rig(self,hip=12,neck=0,shoulder=3,width=5,knee=18):
        self.bone('body',(0,hip,0));self.bone('head',(0,neck,0),'body')
        for side,sign in [('right',-1),('left',1)]:
            x=sign*width;elbow=(shoulder+14)/2
            self.bone(side+'_arm',(x,shoulder,0),'body');self.bone(side+'_forearm',(x,elbow,0),side+'_arm')
            self.bone(side+'_leg',(sign*2,hip,0),'body');self.bone(side+'_shin',(sign*2,knee,0),side+'_leg')
        self.bone('tail',(0,hip,2),'body')
    def head(self,y=0,size=(8,8,7),skin=SKIN,hair=None,tile=None,bone='head'):
        # Skin-colored sides; the front face is a discrete pixel-art panel.
        w,h,d=size
        self.box('block head',(0,y-h/2,0),size,skin,bone,fabric=False)
        self.box('pixel face',(0,y-h/2,-d/2-.015),(w,h,.02),'#FFFFFF',bone,IDS.index(self.name) if tile is None else tile,False)
        for sign in [-1,1]:self.box('ear',(sign*(w/2+.4),y-h*.45,0),(.8,1.8,1.5),skin,bone)
        if hair:
            self.box('hair cap',(0,y-h-.45,.1),(w+.5,.9,d+.5),hair,bone)
            self.box('back hair',(0,y-h*.55,d/2+.2),(w+.5,h*.8,.65),hair,bone)
    def limbs(self,hip=12,shoulder=3,width=5,knee=18,sleeve=DARK,trousers=DARK,skin=SKIN,boots=DARK,armw=3,legw=3,handy=15):
        elbow=(shoulder+14)/2
        for side,sign in [('right',-1),('left',1)]:
            x=sign*width
            self.box('upper sleeve',(x,(shoulder+elbow)/2,0),(armw,elbow-shoulder+.25,3),sleeve,side+'_arm')
            self.box('lower sleeve',(x,(elbow+handy-1)/2,0),(armw,handy-1-elbow+.15,3),sleeve,side+'_forearm')
            self.box('hand',(x,handy-.2,-.15),(armw*.85,2.4,2.6),skin,side+'_forearm')
            lx=sign*2
            self.box('upper leg',(lx,(hip+knee)/2,0),(legw,knee-hip,3.2),trousers,side+'_leg')
            self.box('lower leg',(lx,(knee+23)/2,0),(legw,23-knee,3.2),trousers,side+'_shin')
            self.box('boot foot',(lx,23.3,-.7),(legw+.4,1.4,4.7),boots,side+'_shin')
            self.box('boot sole',(lx,23.9,-.7),(legw+.55,.25,4.9),'#252830',side+'_shin')
    def curls(self,y,w,d,color,bone='head',tall=False):
        for i in range(5):
            for j in range(3):
                x=(i-2)*(w/4.7);z=(j-1)*(d/2.6);h=1.2+(i+j)%3*.4
                self.box('square hair curl',(x,y-h/2,z),(w/4.4,h,d/2.8),color,bone)
        for sign in [-1,1]:
            for j in range(3):self.box('stepped hair edge',(sign*(w/2+.15),y+1.4+j*.8,-d/2+1+j*1.2),(1.3,1.6,1.6),color,bone)

def legacy_wybie(a):
    other=a.name.startswith('other');hair='#36232A';jacket='#202C39'
    a.rig(hip=14,neck=0,shoulder=3,width=5,knee=19)
    a.box('rain jacket',(0,7,0),(8,14,4.4),jacket)
    a.box('coat hem',(0,13.6,0),(8.5,1.2,4.8),jacket)
    a.head(0,(8,8,7),SKIN,hair);a.curls(-8,8,7,hair)
    a.box('hood behind neck',(0,.5,2.1),(7,3,2.8),'#182330')
    a.limbs(14,3,5,19,jacket,SKIN,SKIN,'#17232B',3,2.6,15)
    for y in [4.5,9,13]:a.box('reflective band',(0,y,-2.24),(8.15,.7,.1),'#BCC2C2')
    a.box('zipper',(0,7,-2.32),(.22,12,.12),'#74818B')
    for side,sign in [('right',-1),('left',1)]:
        a.box('cropped blue trousers',(sign*2,16.6,0),(2.8,5.2,3.4),'#4E6179',side+'_leg')
        for y in [6,12]:a.box('sleeve reflective band',(sign*5,y,-1.52),(3.05,.65,.08),'#B6BBBE',side+'_arm' if y==6 else side+'_forearm')
        a.box('black glove',(sign*5,14.8,-.2),(2.65,2.5,2.7),'#111B24',side+'_forearm')
        for i in range(3):
            a.box('glove finger bone',(sign*5+(i-1)*.65,15,-1.59),(.3,1.25,.08),'#D3D6CA',side+'_forearm')
        a.box('shorts cuff',(sign*2,17.5,0),(2.8,.5,3.5),'#6A7A8C',side+'_leg')
        a.box('bare ankle',(sign*2,21,0),(2.2,3.5,2.5),SKIN,side+'_shin')
        a.box('shoe lace',(sign*2,22.7,-2.15),(1.7,.4,.12),'#6D767C',side+'_shin')

def wybie(a):
    other=a.name=='other_wybie';skin='#BB956D';hair='#3C2B2B';coat='#25343B';silver='#9CA7A7'
    a.rig(hip=14,neck=0,shoulder=3,width=4.8,knee=19)
    a.prism('long loose raincoat',[(1,6.5,4.1),(7,7.2,4.5),(13,8.2,5),(14.5,8.6,5.2)],coat)
    a.box('raised hood behind head',(0,1.3,2.1),(7,4,2.2),'#1B2B33')
    a.box('short neck',(0,.6,0),(2.1,2.3,2),skin)
    a.prism('wide youthful face',[(-8,6.3,5.1),(-6.2,8,6),(-2,7.8,6),(-.35,5.6,4.8)],skin,'head')
    for sign in [-1,1]:
        a.box('large ear',(sign*4.3,-4.2,0),(1.25,2.3,1.6),skin,'head')
        a.box('ear inset',(sign*4.3,-4.2,-.85),(.7,1.45,.12),'#9F7A56','head',fabric=False)
        x=sign*1.65
        if other:
            a.box('button eye rim',(x,-4.25,-3.065),(1.65,1.9,.17),'#514D41','head',fabric=False)
            a.box('black button eye',(x,-4.25,-3.2),(1.3,1.45,.18),'#162228','head',fabric=False)
            for dx in [-.2,.2]:
                for dy in [-.22,.22]:a.box('button thread hole',(x+dx,-4.25+dy,-3.32),(.14,.14,.04),'#9BA399','head',fabric=False)
        else:
            a.box('eye white',(x,-4.25,-3.065),(1.6,1.85,.17),'#E5E1CB','head',fabric=False)
            a.box('green hazel iris',(x-sign*.15,-4.4,-3.2),(.75,1,.12),'#748067','head',fabric=False)
            a.box('pupil',(x-sign*.15,-4.4,-3.29),(.32,.6,.06),'#24312F','head',fabric=False)
            a.box('eye glint',(x-sign*.15-.13,-4.62,-3.33),(.16,.2,.02),'#ECEAD9','head',fabric=False)
        a.bar('uneven concerned brow',(x-.7,-5.8+sign*.15,-3.08),(x+.7,-6.05-sign*.15,-3.08),.2,.12,'#493A2E','head')
    a.bar('sloping nose bridge',(-.05,-4.8,-3.1),(.1,-2.9,-3.65),.55,.7,skin,'head')
    a.box('soft nose tip',(.1,-2.85,-3.65),(1.1,.65,.7),'#C6A176','head')
    a.bar('crooked smile',(-1.55,-1.8,-3.02),(1.45,-2.05,-3.08),.15,.1,'#76503E','head')
    # Individual square spiral locks make an irregular, asymmetrical silhouette.
    for row in range(3):
        for col in range(5):
            x=(col-2)*1.55;z=(row-1)*2.05;y=-8.1-((col*3+row)%4)*.3
            a.box('chunky dark curl',(x,y,z),(1.9,1.3,1.8),hair,'head')
            a.box('curl dark center',(x,y-.7,z),(.8,.2,.8),'#281F24','head')
            a.bar('curl raised edge',(x-.65,y-.8,z-.5),(x+.65,y-.95,z-.2),.3,.4,'#503536','head')
    for i in range(4):
        a.box('uneven falling fringe',(-2.9+i*1.5,-6.9-(i%2)*.55,-2.8),(1.7,1.6,1.1),hair,'head')
    for sign in [-1,1]:
        a.bar('outward curling side lock',(sign*3.5,-7.3,.4),(sign*5.2,-7.5,.4),.7,.8,hair,'head')
        a.bar('side lock hooked tip',(sign*5.2,-7.5,.4),(sign*5.5,-8.2,.3),.45,.6,hair,'head')
    a.bar('upright unruly curl',(1.2,-8.9,0),(1.5,-10.4,.2),.5,.6,hair,'head')
    for y,w,d in [(5,7,4.4),(10,7.8,4.8),(13.8,8.5,5.2)]:
        a.box('coat reflective front band',(0,y,-d/2-.04),(w,.65,.1),silver)
        a.box('coat reflective rear band',(0,y,d/2+.04),(w,.65,.1),silver)
    a.bar('coat central zipper',(0,2,-2.12),(0,14,-2.64),.16,.12,'#77858B')
    for sign in [-1,1]:a.box('coat pocket',(sign*2.6,11.7,-2.5),(1.8,1.5,.18),'#1B2C34')
    for side,sign in [('right',-1),('left',1)]:
        arm=side+'_arm';fore=side+'_forearm';leg=side+'_leg';shin=side+'_shin'
        a.box('upper coat sleeve',(sign*4.8,5.75,0),(2.65,5.5,3.3),coat,arm)
        a.box('lower coat sleeve',(sign*4.8,11.3,0),(2.5,5.6,3.1),coat,fore)
        for y,bone in [(5.3,arm),(11.2,fore)]:
            a.box('sleeve silver band',(sign*4.8,y,0),(2.72,.65,3.42),silver,bone)
        a.box('black skeleton glove',(sign*4.8,14.6,-.2),(2.25,2.1,2.5),'#172229',fore)
        a.box('glove palm bone',(sign*4.8,14.25,-1.49),(1,.55,.08),'#CDCDB9',fore)
        for i in range(4):
            x=sign*4.8+(i-1.5)*.43
            for y in [14.9,15.45]:a.box('skeleton finger segment',(x,y,-1.51),(.2,.38,.08),'#CDCDB9',fore)
        a.box('blue cropped trouser',(sign*2,16.2,0),(2.8,4.4,3.4),'#506577',leg)
        a.box('rolled trouser cuff',(sign*2,18.2,0),(3,.65,3.6),'#6F7E88',leg)
        a.box('bare lower leg',(sign*2,20.3,0),(1.55,3.8,2),skin,shin)
        a.box('worn black shoe',(sign*2,23,-.8),(2.75,1.7,4.9),'#253136',shin)
        a.box('thick shoe sole',(sign*2,23.9,-.8),(2.9,.25,5.1),'#444C4B',shin)
        for y in [22.5,22.9]:a.box('shoe lace',(sign*2,y,-3.3),(1.55,.15,.08),'#64706F',shin)
        a.box('worn toe scuff',(sign*2,23,-3.29),(.8,.35,.08),'#998C69',shin)

def coraline(a):
    yellow='#CDAF25';hair='#142D4C';skin='#D1AD87';stripe='#8E8991'
    a.rig(hip=12,neck=0,shoulder=2,width=4,knee=18)
    a.prism('flared yellow raincoat',[(1,5.4,3.6),(5,5.7,3.8),(9,7,4.5),(12.5,8,5)],yellow)
    a.box('small neck',(0,.1,0),(1.65,2.8,1.9),skin)
    # Bevelled Minecraft head with an angular chin and individually placed face pixels.
    a.prism('angular face',[(-8.4,6.8,5.6),(-6.4,7.6,6),(-2.3,7.6,6),(-.6,5.7,4.9)],skin,'head')
    for sign in [-1,1]:
        x=sign*1.65
        a.box('eye outline',(x,-4.85,-3.045),(1.6,2.05,.12),'#AA947D','head',fabric=False)
        a.box('eye white',(x,-4.75,-3.13),(1.34,1.8,.08),'#E4DFCD','head',fabric=False)
        a.box('brown iris',(x-sign*.1,-4.86,-3.19),(.67,1.05,.07),'#69543A','head',fabric=False)
        a.box('pupil',(x-sign*.1,-4.86,-3.235),(.29,.64,.03),'#252D2D','head',fabric=False)
        a.box('eye glint',(x-sign*.1-.12,-5.14,-3.26),(.17,.22,.02),'#F2EADA','head',fabric=False)
        a.bar('inquiring eyebrow',(x-.75,-6.25+sign*.13,-3.08),(x+.65,-6.42-sign*.08,-3.08),.17,.08,'#695642','head')
        a.box('ear',(sign*3.9,-3.6,-.1),(.65,1.55,1.1),skin,'head')
        for i,(dx,dy) in enumerate([(0,0),(.5,.25),(.85,-.1),(.35,-.45)]):
            a.box('freckle',(sign*(1.8+dx),-2.8+dy,-3.055),(.12,.12,.04),'#AA805F','head',fabric=False)
    a.box('small nose bridge',(0,-3.6,-3.1),(.48,1.15,.38),skin,'head')
    a.box('small nose tip',(.1,-3.08,-3.34),(.68,.35,.34),'#C29A76','head')
    a.bar('subtle asymmetric mouth',(-.9,-1.9,-2.99),(.6,-2.03,-3.01),.12,.08,'#916057','head')
    # A deep-blue bob, parted off-centre, with stepped diagonal fringe and tapered tips.
    a.box('bob back',(0,-4.4,2.7),(8,8.2,1.25),hair,'head')
    a.box('hair crown',(0,-8.4,0),(7.6,1.25,6.2),hair,'head')
    a.box('parted hair crown',(.6,-9,0),(4.4,.45,5.5),'#193957','head')
    for sign in [-1,1]:
        a.box('bob side',(sign*3.8,-4.8,.45),(1.15,6.9,6.1),hair,'head')
        a.box('tapered bob jaw',(sign*3.55,-.8,.6),(.9,1.8,5.5),hair,'head')
        a.box('pointed bob tip',(sign*3.2,.25,.2),(.55,.65,3.9),hair,'head')
        for i in range(3):a.box('blue hair strand',(sign*(3.9+i*.12),-4.6+i*.45,-1.9+i*.8),(.1,4.5,.16),'#214665','head',fabric=False)
    for x,y,w in [(-2.7,-6.95,1.8),(-1.5,-7.4,1.8),(-.25,-7.8,1.6),(1,-8.05,1.3),(2,-7.6,1.25),(2.95,-7.1,1.1)]:
        a.box('swept fringe',(x,y,-3.04),(w,1.05,.6),hair,'head')
        a.box('fringe strand',(x,y-.2,-3.37),(w*.72,.1,.05),'#244765','head',fabric=False)
    a.bar('dragonfly clip body',(-3.4,-6.7,-3.5),(-3.1,-5.65,-3.5),.12,.12,'#BBA26B','head')
    for y in [-6.45,-6.05]:
        for sign in [-1,1]:a.bar('dragonfly wing',(-3.25,y,-3.5),(-3.25+sign*.55,y-.25,-3.5),.22,.1,'#A6AA91','head')
    # Raised hood, folded collar, seams, pockets and coat fasteners.
    a.box('folded hood back',(0,1.5,2),(5.9,2.8,2),yellow)
    for sign in [-1,1]:
        a.bar('folded collar',(sign*.65,.9,-1.95),(sign*2.75,2,-2.1),.95,.6,'#DDC33E')
        a.box('hip patch pocket',(sign*2.5,9.25,-2.4),(1.55,1.9,.23),'#C3A323')
        a.box('pocket top welt',(sign*2.5,8.35,-2.57),(1.75,.25,.18),'#DFC143')
    a.bar('coat zipper',(0,2,-2.02),(0,12,-2.53),.14,.12,'#A68D20')
    for y in [2.6,4.6,6.6,8.6]:
        a.box('coat fastening',(.45,y,-2.1-y*.035),(.32,.25,.14),'#E5C847')
    a.box('coat lower seam',(0,12.2,-2.53),(7.8,.18,.1),'#B89B20')
    a.prism('red pleated skirt',[(12,6.5,3.8),(14,7,4.1)],'#753844')
    for x in [-2.6,-1.3,0,1.3,2.6]:a.box('skirt pleat',(x,13.2,-2.08),(.2,1.5,.08),'#91505A')
    for side,sign in [('right',-1),('left',1)]:
        arm=side+'_arm';forearm=side+'_forearm';leg=side+'_leg';shin=side+'_shin'
        a.box('upper raincoat sleeve',(sign*4,5,0),(2.1,6,2.9),yellow,arm)
        a.box('lower raincoat sleeve',(sign*4,10,0),(1.85,4,2.65),yellow,forearm)
        a.box('folded sleeve cuff',(sign*4,11.7,-.05),(2.1,.7,2.85),'#DDC13A',forearm)
        a.box('hand',(sign*4,12.65,-.1),(1.4,1.5,1.8),skin,forearm)
        a.box('thumb',(sign*3.25,12.45,-.5),(.4,.9,.6),skin,forearm)
        a.box('striped tights thigh',(sign*2,15,0),(1.5,6,1.85),'#503543',leg)
        a.box('striped tights shin',(sign*2,19.2,0),(1.4,2.4,1.75),'#503543',shin)
        for y in [14.6,15.7,16.8,17.9,19]:a.box('tights horizontal stripe',(sign*2,y,0),(1.53,.42,1.9),stripe,leg if y<18 else shin)
        a.box('yellow wellington shaft',(sign*2,21.6,.05),(2.2,4.2,2.65),yellow,shin)
        a.box('wellington rolled cuff',(sign*2,19.7,.05),(2.5,.55,2.95),'#DBC04A',shin)
        a.box('wellington toe',(sign*2,23.35,-.9),(2.3,1.05,4.3),yellow,shin)
        a.box('dark rubber sole',(sign*2,23.94,-.9),(2.45,.24,4.5),'#626343',shin)
        a.box('boot seam',(sign*2,21.7,-1.32),(.12,3,.08),'#B49927',shin)
    a.bar('diagonal burgundy strap',(-2.45,1.9,-2.65),(3.5,12.4,-2.9),.7,.18,'#693044')
    a.box('satchel body',(4.3,13.6,.9),(2.2,4.6,3.4),'#673345')
    a.box('satchel front flap',(4.3,12.4,-.87),(2.35,2.1,.3),'#773B4D')
    a.box('satchel buckle',(4.3,13.15,-1.05),(.5,.6,.14),'#A98B50')

def legacy_bobinsky(a):
    other=a.name!='bobinsky';evil=a.name.startswith('evil');skin='#6F99A9' if not other else '#4E8AB9'
    a.rig(hip=9,neck=-6,shoulder=-3,width=7,knee=17)
    a.prism('barrel chest',[(-4,7.5,5),(1,12,7),(8,10.5,6)],'#181F2A' if other else '#B8AB8A')
    a.head(-7,(5,11,5),skin,tile=IDS.index(a.name))
    a.box('long neck',(0,-5,0),(1.6,5,2),skin)
    a.box('long blue nose',(0,-11.5,-3.25),(1.2,5.6,2),skin,'head')
    a.limbs(9,-3,7,17,'#181F2A' if other else skin,'#888493' if other else skin,skin,'#17232C',1.8,1.7,13)
    a.box('purple shorts',(0,9.5,0),(5,3.1,4.2),'#5A415F')
    for sign in [-1,1]:
        a.box('mustache inner',(sign*1.1,-10.2,-3.35),(2,.5,.4),'#173B6B' if other else '#5E747C','head')
        a.box('mustache outward',(sign*2.8,-10.7,-3.25),(1.8,.55,.45),'#173B6B' if other else '#5E747C','head')
        a.box('mustache upturn',(sign*3.6,-11.6,-3.1),(.5,1.7,.4),'#173B6B' if other else '#5E747C','head')
    for side,sign in [('right',-1),('left',1)]:
        a.box('tall riding boot',(sign*2,20,0),(2.6,6,3),'#17232C',side+'_shin')
        a.box('boot cuff',(sign*2,17.3,0),(2.8,.7,3.2),GOLD if other else '#363C42',side+'_shin')
    if other:
        a.box('hat brim',(0,-18.8,0),(8,.7,7),'#111721','head')
        a.box('towering top hat',(0,-24.5,0),(5.7,11,5.5),'#15202A','head')
        a.box('red top hat ribbon',(0,-19.8,0),(5.8,1.3,5.6),'#AC293D','head')
        for y in [-1,1,3,5,7]:
            a.box('gold frogging',(0,y,-3.53),(8,.35,.18),GOLD)
            for sign in [-1,1]:a.box('jacket clasp',(sign*2.9,y,-3.67),(.45,.65,.2),GOLD)
        for sign in [-1,1]:
            a.box('epaulette',(sign*6.3,-3.1,0),(3,.8,4.3),GOLD)
            for i in range(4):a.box('epaulette fringe',(sign*6.3+(i-1.5)*.6,-2.2,-2.1),(.3,1.3,.2),'#B58B35')
            a.box('split coat tail',(sign*3,15,3),(4.7,14,.55),'#302547')
            a.box('coat tail gold trim',(sign*5.1,15,2.68),(.3,14,.25),GOLD)
        a.box('red sash',(0,8.7,-2.9),(9,.8,.6),'#A93448')
    if evil:
        a.box('hollow broken face',(0,-12.8,-2.9),(4.2,7.5,.4),'#121B23','head')
        for i in range(6):
            x=(-1 if i%2==0 else 1)*(1 if i<4 else .4);y=-16+i*.8
            a.box('rat head in disguise',(x,y,-3.2),(1.5,1,1.3),'#6B6257','head')
            for sign in [-1,1]:a.box('rat square ear',(x+sign*.6,y-.6,-3.1),(.45,.45,.3),'#A98F82','head')
            a.box('red rat eye',(x-.4,y,-3.89),(.2,.2,.04),'#C34942','head')
        for side,sign in [('right',-1),('left',1)]:
            for i in range(3):a.bar('sharp gilt fingers',(sign*7+(i-1)*.6,13,-.3),(sign*7+(i-1)*.9,15,-1),.18,.18,GOLD,side+'_forearm')

def bobinsky(a):
    """Three related silhouettes: shabby acrobat, ringmaster, empty ringmaster suit."""
    other=a.name!='bobinsky';evil=a.name=='evil_other_bobinsky'
    skin='#91B4BA' if not other else '#87C4DE';cloth='#18222B';gold='#D8AF53'
    a.rig(hip=8,neck=-7,shoulder=-4,width=8,knee=17)
    if other:
        a.prism('broad tailored ringmaster jacket',[(-5,13,6),(-2,14,7),(3,10,6),(8,5.2,3.8)],cloth)
    else:
        a.prism('exposed angular belly',[(-4,7,4),(0,12,7),(5,13,8),(8,7,5)],skin)
        a.prism('cropped knitted vest',[(-4.3,8,4.6),(-1,12.5,7.3),(4.4,13.3,8.2),(5.6,11.8,7.7)],'#C8BB86')
        for x in range(-5,6):
            a.box('vest rib knit',(x,1.5,-4.14),(.18,6.5,.08),'#AFA373',fabric=False)
        for x,y in [(-3,2),(2,0),(3,3),(-1,4)]:
            a.box('worn vest stain',(x,y,-4.22),(.6,.6,.04),'#998762',fabric=False)
        a.box('belly seam',(0,6.4,-3.96),(.16,1.1,.06),'#688991',fabric=False)
    # The two non-evil faces are built specifically for Bobinsky, rather than stretching a generic face.
    if not evil:
        a.box('neck',(0,-5.7,.2),(2.1,3.7,2.5),skin)
        a.prism('long angular head',[(-18.3,3.2,3.3),(-15,4,3.8),(-10,3.5,3.5),(-7.2,2.2,2.9)],skin,'head')
        a.box('projecting long chin',(0,-8.7,-1.7),(2.5,3.6,1.2),skin,'head')
        a.bar('long sloping nose',(0,-16.7,-2.1),(0,-12,-3.5),1.05,1.2,skin,'head')
        a.box('nose tip',(0,-12,-3.5),(1.5,.8,1.2),'#A9CDD2' if other else '#829EAB','head')
        a.box('smiling mouth',(0,-10.6,-1.99),(2.35,.8,.13),'#293239','head')
        for i in range(4):a.box('small square tooth',((i-1.5)*.49,-10.85,-2.08),(.38,.28,.08),'#DDD7BC','head')
        for sign in [-1,1]:
            a.box('ear',(sign*2,-15.8,.1),(.7,1.1,1),skin,'head')
            x=sign*.99
            a.box('eye surround',(x,-16.4,-1.97),(1.2,1.35,.2),'#284A65' if other else '#DFDEBC','head')
            a.box('button eye' if other else 'iris',(x,-16.4,-2.1),(.86,.98,.14),'#111B27' if other else '#6E8271','head')
            if other:
                for dx in [-.18,.18]:
                    for dy in [-.2,.2]:a.box('button stitch hole',(x+dx,-16.4+dy,-2.19),(.12,.13,.05),'#85A9BD','head')
            else:a.box('eye pupil',(x,-16.4,-2.21),(.35,.58,.06),'#20292B','head')
            a.bar('expressive eyebrow',(x-sign*.55,-17.5,-2),(x+sign*.45,-17.8,-1.9),.32,.25,'#25485D','head')
            if other:
                points=[(sign*.6,-11.8,-3.5),(sign*2.4,-11.2,-3.7),(sign*4.1,-11.5,-3.65),(sign*5,-12.5,-3.6),(sign*4.8,-13.5,-3.5),(sign*4.1,-13.5,-3.5),(sign*4,-13,-3.5)]
                for p,q in zip(points,points[1:]):a.bar('angular curled blue moustache',p,q,.44,.4,'#235985','head')
            else:
                for i in range(3):
                    points=[(sign*.65,-11.9,-3.5),(sign*3,-12.2+i*.23,-3.4),(sign*(4.7+i*.45),-11.5+i*.25,-3.2),(sign*(5+i*.5),-10+i*.45,-3)]
                    for p,q in zip(points,points[1:]):a.bar('wiry drooping moustache',p,q,.12,.12,'#46555B','head')
        if not other:
            for x in [-.8,0,.8]:a.bar('sparse swept hair',(x,-18.2,0),(x+.5,-19,-.8),.25,.3,'#596267','head')
    # Very long thin articulated limbs and separate shorts / trouser legs.
    for side,sign in [('right',-1),('left',1)]:
        upper=side+'_arm';lower=side+'_forearm';leg=side+'_leg';shin=side+'_shin'
        a.bar('upper sleeve' if other else 'bare upper arm',(sign*7,-3.6,0),(sign*8,5,0),1.35,1.6,cloth if other else skin,upper)
        a.bar('lower sleeve' if other else 'bare forearm',(sign*8,5,0),(sign*8,13,-.35),1.15,1.4,cloth if other else skin,lower)
        hand='#E1E8DE' if evil else skin
        a.box('white glove palm' if evil else 'palm',(sign*8,13.5,-.35),(1.5,1.7,1.3),hand,lower)
        for i in range(3):a.box('glove finger' if evil else 'long finger',(sign*8+(i-1)*.48,14.9,-.5),(.32,1.8-(abs(i-1)*.3),.65),hand,lower)
        a.bar('glove thumb' if evil else 'thumb',(sign*7.35,13.3,-.4),(sign*6.8,14,-.8),.42,.5,hand,lower)
        a.box('wrist cuff',(sign*8,12.3,-.3),(1.6,.55,1.7),gold if other else '#D5D2B7',lower)
        if evil:a.box('white glove rolled cuff',(sign*8,12.8,-.3),(1.8,.5,1.8),hand,lower)
        a.box('slender thigh',(sign*2,12.5,0),(1.55,9,1.9),'#AAA9B8' if other else skin,leg)
        a.box('slender shin',(sign*2,19.7,0),(1.4,5.5,1.7),'#AAA9B8' if other else skin,shin)
        if not other:
            a.box('purple short leg',(sign*2,9.5,0),(3.1,4,3.6),'#514064',leg)
            a.box('short cuff',(sign*2,11.4,0),(3.25,.45,3.75),'#685079',leg)
        a.box('high riding boot',(sign*2,20.8,0),(2.3,5.2,2.8),'#172126',shin)
        a.box('boot flared cuff',(sign*2,18.1,0),(2.7,.8,3.2),gold if other else '#364348',shin)
        a.box('long boot toe',(sign*2,23.35,-1.2),(2.3,1.2,4.9),'#172126',shin)
        a.box('boot sole',(sign*2,23.95,-1.2),(2.5,.25,5.1),'#0F161C',shin)
        if other:a.box('gold boot strap',(sign*2,22.95,-2.5),(2.35,.25,.3),gold,shin)
    if not other:
        for y in [16.3,16.9,17.5]:a.box('wrapped knee bandage',(-2,y,-.1),(1.85,.48,2.1),'#B99376','right_leg' if y<17 else 'right_shin')
        a.box('medal ribbon',(3,-2,-3.8),(.8,1,.2),'#A34848')
        a.box('old medal',(3,-1.3,-3.9),(1.2,1.2,.2),'#88A9A7')
        return
    hatbase=-5.6 if evil else -18.7
    hatbone='body' if evil else 'head'
    a.box('hat brim resting on collar' if evil else 'hat brim',(0,hatbase,0),(7.4,.65,6),cloth,hatbone)
    # A stepped crown keeps the recognisable tall, slightly flared silhouette.
    for i in range(3):a.box('tall stepped hat crown',(i*.12,hatbase-2-i*3.4,.1),(4.5+i*.3,3.5,4.4+i*.2),cloth,hatbone)
    a.box('crimson hat band',(0,hatbase-1,0),(4.65,1.1,4.55),'#B73545',hatbone)
    a.box('red waist sash',(0,8,-.15),(5.6,1.5,4.1),'#A52F43')
    # Raised gold braid follows the tapered jacket surface.
    for y,w,z in [(-2.7,10.5,-3.42),(-.2,10,-3.55),(2.2,8.2,-3.1),(4.6,6.5,-2.65),(6.6,4.7,-2.22)]:
        a.box('gold jacket braid',(0,y,z),(w,.25,.2),gold)
        for i in range(int(w/.55)):
            a.box('braid square loop',(-w/2+.25+i*.55,y,z-.12),(.28,.48,.12),'#B78B37')
    for p,q in zip([(0,-4.5,-3.1),(0,-1,-3.65),(0,3,-3),(0,7.5,-2.1)],[(0,-1,-3.65),(0,3,-3),(0,7.5,-2.1),(0,8,-2.1)]):a.bar('center gold piping',p,q,.28,.2,gold)
    for sign in [-1,1]:
        a.box('gold shoulder epaulette',(sign*6.8,-4.65,0),(3.4,.65,4.8),gold)
        for i in range(6):a.box('shoulder tassel',(sign*6.8+(i-2.5)*.48,-3.65,-2.2),(.23,1.7,.25),'#BD943D')
        a.bar('raised collar gold rim',(sign*.3,-5,-2.5),(sign*3,-5.6,1.4),.25,.25,gold)
        a.box('long split purple coat lining',(sign*3.2,15.8,2.25),(4.1,15,.6),'#34264E')
        a.box('black coat tail back',(sign*3.2,15.8,2.65),(4.2,15,.35),cloth)
        a.box('long coat tail edging',(sign*5.2,15.8,2.2),(.25,15,.25),gold)
        a.box('coat tail hem',(sign*3.2,23.2,2.2),(4.1,.25,.25),gold)
    for depth in [0,1.1]:
        pts=[(-6,-3,-2.6),(-5,0+depth,-3.65),(-3,1.2+depth,-3.8),(-1,0+depth,-3.8),(-.5,-1.5,-3.8)]
        for p,q in zip(pts,pts[1:]):a.bar('draped gold shoulder cord',p,q,.23,.23,gold)
    a.box('medal ribbon',(4,-2.5,-3.5),(1.1,1.1,.25),'#A83248')
    a.box('medal gold frame',(4,-1.4,-3.65),(1.55,1.5,.25),gold)
    a.box('medal turquoise center',(4,-1.4,-3.81),(.95,.95,.12),'#4D9EAA')

def lady(a):
    spink='spink' in a.name;other=a.name.startswith('other');hair='#B3496A' if spink else '#B6AD91'
    if other:
        color='#78AF32' if spink else '#AC2D58';trim='#BDD453' if spink else '#E8A353'
        a.rig(hip=12,neck=0,shoulder=3,width=4.5,knee=18)
        a.prism('stage leotard',[(1,5,3.5),(6,4.6,3.5),(12,6.6,4)],color)
        a.limbs(12,3,4.5,18,SKIN,SKIN,SKIN,color,2,2.2,14)
        a.head(0,(7,7,6),SKIN,'#A23544' if spink else '#714833');a.curls(-7,7,6,'#A23544' if spink else '#714833')
        for sign in [-1,1]:a.bar('leotard accent',(sign*2.2,2,-1.84),(sign*.6,7,-1.84),.35,.1,trim)
        a.box('performance shorts',(0,12,0),(6.6,2,4),color)
        for side,sign in [('right',-1),('left',1)]:
            a.box('stocking cuff',(sign*2,19.4,0),(2.5,.6,3.3),trim,side+'_shin')
            a.box('stage boot shaft',(sign*2,21.8,0),(2.4,3,3.3),color,side+'_shin')
        if not spink:
            for sign in [-1,1]:a.surface('triangular golden cape',[(sign*.5,1,2),(sign*4.8,6,2),(sign*.8,10,2)],[(0,1,2)],GOLD)
    else:
        color='#A7A1B4' if spink else '#C9877C';trim='#B3B79B' if spink else '#31776C'
        a.rig(hip=17,neck=3,shoulder=7,width=7,knee=20)
        a.prism('wide dressing robe',[(6,9,6),(11,13,7),(18,12,7),(23,12.5,7.5)],color)
        a.head(5,(9.5 if spink else 8,8 if spink else 10,7),SKIN,hair)
        a.limbs(17,7,7,20,color,color,SKIN,trim,3.5,3,17)
        for sign in [-1,1]:a.bar('thick robe collar',(sign*2.6,6,-3.2),(sign*1.2,17,-3.6),1.1,.5,trim)
        a.box('robe sash',(0,16.5,-3.7),(12,.8,.5),trim)
        for i in range(9):a.box('lace hem scallop',((i-4)*1.3,22.5,-3.8),(1,.6,.15),trim)
        if spink:
            a.curls(-3,9.5,7,hair)
            for sign in [-1,1]:
                x=sign*2.1
                for cx,cy,w,h in [(x,-.2,2.8,.3),(x,2.6,2.8,.3),(x-1.25,1.2,.3,2.7),(x+1.25,1.2,.3,2.7)]:a.box('square spectacle frame',(cx,cy,-3.68),(w,h,.18),'#283548','head')
                a.box('walker upright',(sign*5,20,-6),(.25,8,.25),'#959FAB')
                a.box('walker side rail',(sign*5,16,-4),(.25,.25,4.5),'#B7BDC1')
            a.box('spectacle bridge',(0,1,-3.72),(1.6,.25,.25),'#283548','head')
            a.box('walker front rail',(0,16,-6),(10,.3,.3),'#B7BDC1')
        else:
            for sign in [-1,1]:a.box('silver bob',(sign*3.6,.6,.5),(1.2,9.4,7.5),hair,'head')
            a.box('rose hat',(0,-5.4,0),(5.3,1.4,4.8),'#6C3644','head')
            for i in range(3):a.box('square rose',(i-1,-6.3,-1.2),(1.4,1.4,1.4),'#A54C64','head')
            # Spectacle-on-a-stick, held by the right hand.
            a.box('lorgnette handle',(-7,13,-1.8),(.25,6,.25),trim,'right_forearm')
            for x in [-7.7,-6.2]:a.box('lorgnette rim',(x,9.9,-1.8),(1.2,1,.25),trim,'right_forearm')

def other_spink(a):
    is_forcible=a.name=='other_miss_forcible'
    skin='#D4B499' if is_forcible else '#C99E76';green='#AB4757' if is_forcible else '#67A345';light='#D7827C' if is_forcible else '#ABC66C';trim='#79334F' if is_forcible else '#397A58';hair='#AF7C3B' if is_forcible else '#AA4537'
    a.rig(hip=12,neck=-1,shoulder=3,width=3.8,knee=18)
    a.box('slender neck',(0,.2,0),(1.25,3.8,1.5),skin)
    a.prism('acrobat fitted bodice',[(2,4.9,3.3),(4,4.5,3.3),(7,1.8,1.8),(9,2.4,2.2),(12,6,3.8)],green)
    for sign in [-1,1]:
        a.bar('costume shoulder strap',(sign*1,1.1,0),(sign*2.1,2.8,-1.6),.35,.25,trim)
        points=[(sign*1.4,2.6,-1.72),(sign*.8,5,-1.5),(sign*.35,7,-.96),(sign*.65,9,-1.16),(sign*1.8,12,-1.98)]
        for p,q in zip(points,points[1:]):a.bar('lime costume stripe',p,q,.5,.08,light)
    a.prism('angular youthful face',[(-9,3.8,4.1),(-7,5.2,4.7),(-3,5.1,4.7),(-1,2.8,3.5)] if is_forcible else [(-8,4.8,4.5),(-6,6.3,5.1),(-2.5,6.2,5.1),(-1,3.8,3.9)],skin,'head')
    for sign in [-1,1]:
        x=sign*1.45
        a.box('button eye rim',(x,-4.7,-2.61),(1.65,1.75,.13),'#786449','head',fabric=False)
        a.box('black button eye',(x,-4.7,-2.73),(1.25,1.35,.17),'#172427','head',fabric=False)
        for dx in [-.19,.19]:
            for dy in [-.2,.2]:a.box('button stitch',(x+dx,-4.7+dy,-2.85),(.12,.12,.04),'#74898A','head',fabric=False)
        for i in range(2):a.bar('long upper lash',(x+sign*.6,-5.2+i*.25,-2.8),(x+sign*(.95+i*.1),-5.7+i*.2,-2.8),.1,.1,'#263331','head')
        a.bar('raised red brow',(x-.7,-6.3,-2.61),(x+.65,-6.45,-2.61),.15,.1,'#79483A','head')
        a.box('ear',(sign*3.15,-3.9,0),(.55,1.2,1),skin,'head')
    a.box('tiny nose',(0,-3.6,-2.73),(.5,.6,.5),skin,'head')
    a.box('purple smiling lips',(0,-2.35,-2.5),(1.65,.4,.15),'#854667','head',fabric=False)
    a.box('smile highlight',(0,-2.45,-2.61),(1.1,.12,.03),'#DDC9B6','head',fabric=False)
    if is_forcible:
        a.box('golden swept hair back',(0,-6.2,2.2),(5.3,6.3,1.1),hair,'head')
        for sign in [-1,1]:
            pts=[(sign*.1,-9.2,-1),(sign*1.3,-10.1,-1),(sign*2.5,-8.4,-1.4),(sign*2.8,-6,-1.4),(sign*3.2,-5.6,-1.3)]
            for p,q in zip(pts,pts[1:]):a.bar('golden swept wave',p,q,.45,.55,'#CBA05D','head')
        a.bar('long swept ponytail',(0,-5.8,2.6),(0,-1,4.5),1.7,2,hair,'head')
        a.bar('pointed ponytail end',(0,-1,4.5),(0,2,4.8),.8,1,hair,'head')
        a.box('black bow knot',(0,2.9,-1.9),(.55,.7,.4),'#22242C')
        for sign in [-1,1]:
            a.surface('black bow wing',[(0,2.9,-2.05),(sign*1.3,2.3,-2.05),(sign*1.1,3.8,-2.05)],[(0,1,2)],'#22242C')
            a.surface('gold shoulder cape',[(sign*1.5,2,1.9),(sign*4.5,5,2.8),(sign*2,8,3)],[(0,1,2)],'#B89A4A')
    else:
        a.box('slick red hair back',(0,-5.3,2.15),(6.1,5.7,1.1),hair,'head')
        a.box('red updo base',(0,-8,0),(5.7,1.4,4.6),hair,'head')
        for sign in [-1,1]:
            pts=[(sign*.5,-8.3,0),(sign*1.8,-10.2,0),(sign*2.7,-10.8,0),(sign*3.1,-9.9,0),(sign*2.4,-9.3,0),(sign*1.9,-9.8,0)]
            for p,q in zip(pts,pts[1:]):a.bar('square spiral updo',p,q,.55,.7,hair,'head')
            a.bar('tall red hair plume',(sign*.2,-8.7,.2),(sign*1.3,-12,.3),.42,.5,'#BD5942','head')
    for side,sign in [('right',-1),('left',1)]:
        arm=side+'_arm';fore=side+'_forearm';leg=side+'_leg';shin=side+'_shin'
        a.bar('slender acrobat upper arm',(sign*3.8,3,0),(sign*4.1,8.5,0),.85,1,skin,arm)
        a.bar('slender acrobat forearm',(sign*4.1,8.5,0),(sign*4.5,13,-.2),.7,.8,skin,fore)
        a.box('acrobat hand',(sign*4.5,13.5,-.2),(1,1.2,.95),skin,fore)
        for i in range(3):a.box('acrobat finger',(sign*4.5+(i-1)*.29,14.3,-.35),(.18,.8,.4),skin,fore)
        a.box('green fitted shorts',(sign*1.6,14.2,0),(2.4,4.5,3.1),green,leg)
        for x in [-.65,.55]:a.box('shorts vertical lime stripe',(sign*1.6+x,14.2,-1.57),(.43,4.45,.06),light,leg,fabric=False)
        a.box('shorts cuff',(sign*1.6,16.3,0),(2.45,.4,3.2),trim,leg)
        a.box('bare knee',(sign*1.6,17.3,0),(1.4,2,1.8),skin,leg)
        a.box('bare lower leg',(sign*1.6,19.6,0),(1.25,3.3,1.6),skin,shin)
        a.box('green acrobat boot',(sign*1.6,22.1,0),(1.6,2.6,2),green,shin)
        a.box('lime boot cuff',(sign*1.6,20.9,0),(1.9,.5,2.2),light,shin)
        a.box('pointed green shoe',(sign*1.6,23.5,-.6),(1.65,.6,3.1),trim,shin)

def spink(a):
    skin='#C7A78B';robe='#628D90';trim='#9CB8A2';hair='#B7657A'
    a.rig(hip=18,neck=5,shoulder=8,width=7,knee=21)
    a.prism('broad teal dressing gown',[(7,9,5.8),(11,13,7.5),(17,14,8),(21,11,7),(23.5,11.5,7.4)],robe)
    a.prism('rounded angular face',[(-3,6.3,4.6),(-1.5,9.5,6),(2.5,10,6.3),(5,7.3,5.4),(5.8,5.3,4.5)],skin,'head')
    a.box('soft chin',(0,3.75,-2.85),(2.7,1.6,.9),skin,'head')
    a.box('small nose',(0,1.6,-3.4),(.9,1,.8),skin,'head')
    a.box('rose lipstick',(0,2.75,-3.24),(1.35,.35,.15),'#974D61','head',fabric=False)
    a.box('beauty spot',(1.2,3.1,-3.18),(.16,.16,.05),'#695346','head',fabric=False)
    for sign in [-1,1]:
        x=sign*2.05
        a.box('blue eye shadow',(x,-.5,-3.11),(2.25,1.55,.2),'#427EAF','head',fabric=False)
        a.box('large eye white',(x,.2,-3.24),(1.9,.95,.12),'#E8E3CB','head',fabric=False)
        a.box('hazel iris',(x-sign*.1,.22,-3.34),(.65,.85,.06),'#7B7960','head',fabric=False)
        a.box('pupil',(x-sign*.1,.22,-3.38),(.3,.55,.03),'#26302E','head',fabric=False)
        a.bar('arched expressive brow',(x-.9,-1.6,-2.9),(x+.8,-1.9,-2.88),.23,.12,'#685451','head')
        a.box('rosy cheek',(sign*3.4,2,-3.12),(1.1,.85,.06),'#C68C83','head',fabric=False)
        for i in range(3):a.box('cheek speck',(sign*(3+i*.24),2.1+(i%2)*.3,-3.18),(.1,.1,.04),'#A27068','head',fabric=False)
        # Thin open spectacle frames preserve visibility of the eyes.
        for dx,dy,w,h in [(0,-.72,2.7,.1),(0,.72,2.7,.1),(-1.3,0,.1,1.5),(1.3,0,.1,1.5)]:a.box('fine spectacle rim',(x+dx,.3+dy,-3.48),(w,h,.08),'#A29170','head',fabric=False)
        for i in range(3):
            a.box('pink rolled side curl',(sign*(4.1+i*.25),-1.2+i*.65,.25),(1.6,1.6,2.8),hair,'head')
            a.box('curl inset',(sign*(4.1+i*.25),-1.2+i*.65,-1.2),(.8,.8,.18),'#96516B','head')
    a.box('spectacle bridge',(0,.1,-3.48),(1.3,.12,.1),'#A29170','head',fabric=False)
    a.box('pink hair crown',(0,-3,.5),(7,1.1,5.7),hair,'head')
    for i in range(5):a.box('swept pink fringe',((i-2)*1.2,-2.6+abs(i-2)*.22,-2.65),(1.6,.6,.45),'#C67D8B','head')
    a.box('cream blouse',(0,8.3,-3.13),(5,3.8,.25),'#D9D1AB')
    for i in range(11):
        x=(i-5)*.43
        a.box('pearl necklace',(x,6.7+.5*(1-abs(x)/2.3),-3.27),(.25,.25,.2),'#DEDAC0')
    for sign in [-1,1]:
        a.bar('ruffled collar base',(sign*3.6,6.8,-2.9),(sign*.65,14,-4.12),1.25,.6,trim)
        for i in range(9):
            t=i/8;x=sign*(3.6-2.95*t);y=6.8+7.2*t
            a.box('folded lace ruffle',(x,y,-3.25-1.1*t),(1.3,.62,.7),trim if i%2 else '#B9C9AC')
        a.box('upper robe sleeve',(sign*7,9.5,0),(3.4,3.1,4),robe,('right' if sign<0 else 'left')+'_arm')
        fore=('right' if sign<0 else 'left')+'_forearm'
        a.bar('forward sleeve',(sign*7,11,0),(sign*6,15,-3.5),3,3.2,robe,fore)
        a.box('lace cuff',(sign*6,15,-3.6),(3.5,1,3.6),trim,fore)
        a.box('hand on walker',(sign*6,15.9,-4.5),(1.6,1,2),skin,fore)
        a.box('short slipper',(sign*2.5,23.7,-1),(3,.6,4.1),'#728773',('right' if sign<0 else 'left')+'_shin')
        a.bar('walker front upright',(sign*6,15.8,-5),(sign*6.4,23.7,-5),.23,.23,'#929C9C')
        a.bar('walker rear upright',(sign*6,15.8,-2.2),(sign*6.4,23.7,-.7),.23,.23,'#929C9C')
        a.bar('walker side handle',(sign*6,15.8,-5),(sign*6,15.8,-2.2),.3,.3,'#ADB3AD')
        a.box('walker rubber foot',(sign*6.4,23.7,-5),(.5,.6,.5),'#343B39')
    a.bar('walker front rail',(-6,15.8,-5),(6,15.8,-5),.25,.25,'#B7BCB3')
    a.bar('walker lower rail',(-6.2,21,-5),(6.2,21,-5),.2,.2,'#929C9C')
    a.box('knitting basket',(0,18,-5.2),(5,3.2,2.7),'#805A43')
    for y in [16.7,17.3,17.9,18.5,19.1]:a.box('basket woven row',(0,y,-6.61),(5.05,.18,.08),'#AF8060')
    a.box('yarn bundle',(-1,16.3,-5.1),(1.7,1.4,1.5),'#C8D2C4')
    a.bar('knitting needle',(1,17,-5.3),(1.6,14.5,-5.4),.1,.1,'#A9AEAA')
    a.box('gown sash',(0,16.5,-4.1),(13.4,.85,.3),'#9EAB89')
    for i in range(10):a.box('lace hem',((i-4.5)*1.15,23.1,-3.7),(1,.65,.5),trim)

def forcible(a):
    skin='#C5B39C';pink='#B77D76';teal='#326E68';hair='#B9B9A3'
    a.rig(hip=17,neck=3,shoulder=6,width=7,knee=21)
    a.prism('full pink robe',[(5,8,5),(8,13.2,7.5),(12,14,8),(16,10.5,6),(19,8,5),(23.5,12,7)],pink)
    a.prism('wide teal waist sash',[(14.4,12.2,7),(16.9,10.5,6.2)],teal)
    a.box('neck fold',(0,4.4,0),(4.5,1.4,4),skin)
    a.prism('long pear shaped face',[(-7,4.8,4.1),(-4,6.5,5.5),(0,8.2,6),(2.5,7,5.5),(3.3,4.7,4.2)],skin,'head')
    a.box('soft projecting chin',(0,1.4,-2.9),(2.6,2.5,1),skin,'head')
    a.bar('long narrow nose',(0,-4.8,-2.8),(0,-1.5,-3.8),.65,.7,skin,'head')
    a.box('orange pursed lips',(0,-.6,-3.16),(1.2,.32,.2),'#BA624D','head',fabric=False)
    for sign in [-1,1]:
        x=sign*1.85
        a.box('half lidded eye white',(x,-3.1,-2.94),(1.8,.62,.13),'#DBDAC8','head',fabric=False)
        a.box('iris',(x,-3.13,-3.03),(.6,.55,.06),'#658071','head',fabric=False)
        a.box('pupil',(x,-3.12,-3.07),(.25,.45,.04),'#253637','head',fabric=False)
        a.box('heavy teal eye shadow',(x,-3.95,-2.95),(1.85,1.2,.18),'#448F88','head',fabric=False)
        a.bar('dark upper lashes',(x-.95,-3.3,-3.09),(x+.95,-3.18,-3.09),.13,.1,'#343E39','head')
        a.bar('outer eyelash',(x+sign*.9,-3.2,-3.09),(x+sign*1.25,-3.8,-3.1),.12,.1,'#343E39','head')
        a.bar('arched eyebrow',(x-.8,-5.4,-2.6),(x+.7,-5.65,-2.55),.2,.14,'#716B58','head')
        a.box('silver bob side',(sign*3.4,-1.8,.7),(1.45,7.1,5.3),hair,'head')
        a.box('bob outward curl',(sign*3.9,1.3,.55),(1.8,1.65,4.9),hair,'head')
        for i in range(4):a.box('silver bob strand',(sign*(3.2+i*.32),-1.8,-2.02),(.1,5.7,.08),'#DDDAC0','head',fabric=False)
        a.bar('thick teal upper collar',(sign*2.9,5.5,-3.2),(sign*2,8,-3.95),1,.55,teal)
        a.bar('thick teal lower collar',(sign*2,8,-3.95),(sign*.45,11,-4.12),1,.55,teal)
        a.bar('teal flowing robe front',(sign*.45,17,-3.22),(sign*2.2,23.4,-3.52),.85,.4,teal)
        a.box('teal hem',(sign*3.1,23.4,-3.5),(5.5,.7,.5),teal)
    a.box('silver bob back',(0,-2,2.4),(7.3,8.4,1.2),hair,'head')
    a.box('small burgundy hat',(0,-7.35,.1),(4.2,1.1,3.8),'#683A43','head')
    for x,y in [(-.7,-7.9),(.6,-7.8)]:
        a.box('hat rose',(x,y,-1.45),(1.2,1.2,.7),'#A26266','head')
        a.box('rose inset',(x,y,-1.85),(.65,.65,.12),'#693C46','head')
        a.box('rose center',(x,y,-1.94),(.25,.25,.08),'#BD8383','head')
    a.bar('hat leaf',(0,-7.7,.4),(1,-8.5,.5),.55,.3,'#6E8873','head')
    for side,sign in [('right',-1),('left',1)]:
        arm=side+'_arm';fore=side+'_forearm';shin=side+'_shin'
        a.box('robe upper sleeve',(sign*7,8,0),(3.1,4.1,4),pink,arm)
        a.box('flared lower sleeve',(sign*7,12.4,0),(3.8,4.8,4.8),pink,fore)
        a.box('deep teal sleeve cuff',(sign*7,14.5,0),(4.1,.95,5.1),teal,fore)
        a.box('small hand',(sign*7,15.55,-.4),(1.7,1.3,2),skin,fore)
        for i in range(3):a.box('slender finger',(sign*7+(i-1)*.45,16.35,-.55),(.3,1,.65),skin,fore)
        a.box('slipper',(sign*2,23.65,-1),(2.5,.65,4),'#646F55',shin)
    a.box('lorgnette handle',(-7,12,-3),(.2,7,.2),teal,'right_forearm')
    for x in [-7.75,-6.25]:
        for dx,dy,w,h in [(0,-.65,1.25,.18),(0,.65,1.25,.18),(-.62,0,.18,1.3),(.62,0,.18,1.3)]:
            a.box('open spectacle frame',(x+dx,8.3+dy,-3),(w,h,.18),teal,'right_forearm')
    a.box('spectacle bridge',(-7,8.3,-3),(.4,.18,.18),teal,'right_forearm')
    for i in range(28):
        x=((i*7)%11-5)*.8;y=7+(i%5)*1.25
        a.box('subtle robe embroidered speck',(x,y,-4.03),(.12,.18,.08),'#D2BA83',fabric=False)

def father(a):
    orange='#B56B31';light='#C8813B';green='#485E32'
    a.rig(hip=14,neck=-3,shoulder=1,width=7,knee=19)
    a.prism('heavy pumpkin torso',[(-1,4,3.5),(2,8,5.5),(5,13,8.5),(10,13,8.5),(13.6,8,6),(14.5,4,3)],orange)
    # Raised flat-sided lobes follow the swelling gourd from shoulder to base.
    for sign in [-1,1]:
        for x,z in [(1.4,-4.45),(3.5,-4),(5.1,-3)]:
            points=[(sign*x*.4,1,-2),(sign*x,5,z),(sign*x,10,z),(sign*x*.6,13.5,-2.9)]
            for p,q in zip(points,points[1:]):a.bar('pumpkin lobe ridge',p,q,1.1,.65,light)
    a.box('thin neck',(0,-1.2,0),(1.6,3.5,2),orange)
    a.prism('long sorrowful face',[(-14.4,2.7,3.2),(-12.8,4.1,3.6),(-9,3.2,3.6),(-5,2.5,3),(-3.2,2.9,2.7)],'#C78A4D','head')
    for sign in [-1,1]:
        x=sign*1.07
        a.box('ear',(sign*2,-10.7,.1),(.8,1.35,1.1),'#BC7B42','head')
        a.box('drooping eye socket',(x,-11.8,-1.88),(1.5,2,.22),'#765136','head',fabric=False)
        a.box('black button eye',(x,-12,-2.05),(1.1,1.2,.18),'#1C2425','head',fabric=False)
        for dx in [-.18,.18]:
            for dy in [-.18,.18]:a.box('button stitch',(x+dx,-12+dy,-2.17),(.13,.13,.05),'#8C8C71','head',fabric=False)
        a.bar('sad raised eyebrow',(sign*.35,-13.8,-1.9),(sign*1.75,-13.15,-1.82),.26,.2,'#6C6335','head')
        a.bar('under eye crease',(sign*.7,-10.9,-1.9),(sign*1.3,-10.1,-1.84),.14,.09,'#8B613D','head')
    a.bar('long drooping nose',(0,-12,-2),(0,-9.1,-3.1),.8,.9,'#C3813D','head')
    # Narrow inverted-U mouth and extended orange chin, matching the distressed reference.
    a.box('mouth dark center',(0,-6.15,-1.69),(1.85,3.5,.16),'#352A21','head',fabric=False)
    a.box('long hanging chin',(0,-5.3,-1.85),(1.05,3.2,.5),'#C78A4D','head')
    for sign in [-1,1]:a.bar('downturned mouth rim',(sign*.25,-8,-1.8),(sign*1.05,-4.5,-1.7),.22,.2,'#BD7D42','head')
    for side,sign in [('right',-1),('left',1)]:
        arm=side+'_arm';fore=side+'_forearm';leg=side+'_leg';shin=side+'_shin'
        a.bar('sloping pumpkin shoulder',(sign*3,1,0),(sign*7,3,0),1.65,1.8,orange,arm)
        a.bar('curved upper arm',(sign*7,3,0),(sign*8.5,7.5,0),1.1,1.2,orange,arm)
        a.bar('long lower arm',(sign*8.5,7.5,0),(sign*8,15,-.5),.85,1,orange,fore)
        a.box('green wrist',(sign*8,15,-.5),(.9,1.6,1.15),green,fore)
        a.box('green palm',(sign*8,16,-.5),(1.4,1.6,1.3),green,fore)
        for i in range(3):
            x=sign*8+(i-1)*.45
            a.bar('long green finger',(x,16.5,-.5),(x,19-abs(i-1)*.3,-.9),.3,.38,green,fore)
        a.bar('green thumb',(sign*7.4,15.6,-.5),(sign*6.9,17,-.9),.4,.5,green,fore)
        a.bar('bowed thigh',(sign*2,14,0),(sign*3,18.7,0),1.5,1.8,orange,leg)
        a.box('knobbly knee',(sign*3,18.6,-.1),(1.9,1.5,2),'#BA7638',leg)
        a.bar('bent shin',(sign*3,19,0),(sign*2.3,22.5,-.4),1.2,1.5,orange,shin)
        a.box('green ankle',(sign*2.3,22.2,-.25),(1.5,2.1,1.8),green,shin)
        a.box('rootlike foot',(sign*2.5,23.35,-1),(2.3,1.1,4),green,shin)
    for x,y,z in [(-3,5,-4.4),(2,8,-4.6),(-4,10,-4),(4,6,-3.8),(-1,11,-4.4),(1.5,3,-3.5)]:
        for dx,dy in [(0,0),(.35,.3),(-.2,.65)]:a.box('pumpkin decay fleck',(x+dx,y+dy,z-.4),(.28,.45,.07),'#73704D',fabric=False)

def beldam(a):
    bone='#A5B6B0';dark='#202A29';cloth='#484739';rim='#777357'
    a.bone('body',(0,7,0));a.bone('head',(0,-12,0),'body')
    for side,sign in [('right',-1),('left',1)]:
        a.bone(side+'_arm',(sign*5,-12,0),'body')
        a.bone(side+'_forearm',(sign*7,-3,0),side+'_arm')
        a.bone(side+'_leg',(sign*2,9,1),'body')
        a.bone(side+'_shin',(sign*10,15,2),side+'_leg')
    a.bone('tail',(0,10,2),'body')
    # Bare connecting spine between the armoured upper torso and pointed hip shell.
    a.bar('exposed narrow spine',(0,-6,.7),(0,0,.7),.55,.6,bone)
    a.prism('skeletal chest shell',[(-13,5,3.2),(-9,4,2.7),(-6,3,2.3)],cloth)
    a.prism('pointed hip bodice',[(-1,2.5,2),(1,5.8,3.6),(4,3.3,3)],cloth)
    a.prism('long ragged skirt',[(2,4,3.5),(11,4.7,4),(18,7.5,5.5),(22,12,8),(24,18,12)],'#414541')
    for sign in [-1,1]:
        # High folded collar projects behind and above the head.
        vertices=[(sign*1,-10,1.5),(sign*4,-13,1.8),(sign*8,-14,2.7),(sign*6,-18,3.2),(sign*6.5,-23,3),(sign*2,-25,2),(sign*1,-19,2)]
        a.surface('high angular wing collar',vertices,[tuple(range(7))],cloth)
        for p,q in zip(vertices,vertices[1:]+vertices[:1]):a.bar('collar stitched edge',p,q,.23,.25,rim)
        for j in range(4):
            y=-21+j*2
            a.bar('collar woven diagonal',(sign*3,y,1.9),(sign*5.7,y+1.1,2.6),.17,.12,'#8A8060')
            a.bar('collar woven crossing',(sign*3,y+1.2,1.9),(sign*5.7,y,2.6),.14,.12,'#2D342B')
        a.bar('sharp hip wing',(sign*1.2,0,-.3),(sign*5.5,3,1.5),.7,.7,cloth)
        a.bar('hip needle tip',(sign*5.5,3,1.5),(sign*6.8,6,1.8),.2,.2,rim)
    # Long black hair frame and pointed crown around a fractured pale mask.
    a.prism('porcelain angular mask',[(-23,3.4,3.8),(-21,5.6,4.8),(-17,5.3,4.8),(-13.5,3.4,4),(-12.5,2.1,3)],bone,'head')
    a.box('dark hair back',(0,-18,2.1),(5.8,11,1.1),dark,'head')
    for sign in [-1,1]:
        a.bar('black tapered hair edge',(sign*2.4,-22,-1.6),(sign*3,-15,-1.9),.65,.65,dark,'head')
        a.bar('long sharp hair lock',(sign*3,-15,-1.9),(sign*2.7,-10,-2),.35,.35,dark,'head')
    a.bar('pointed hair crown',(-2.4,-22.3,0),(0,-26.1,.3),1,.9,dark,'head')
    a.bar('pointed hair crown',(2.4,-22.3,0),(0,-26.1,.3),1,.9,dark,'head')
    for sign in [-1,1]:
        x=sign*1.25
        a.box('sunken button socket',(x,-19,-2.48),(1.7,1.8,.2),'#586F6B','head',fabric=False)
        a.box('black square button eye',(x,-19,-2.63),(1.2,1.25,.16),'#111B1B','head',fabric=False)
        for dx in [-.2,.2]:
            for dy in [-.2,.2]:a.box('button hole',(x+dx,-19+dy,-2.74),(.13,.13,.04),'#829891','head',fabric=False)
        a.bar('sharp brow',(x-sign*.65,-19.9,-2.65),(x+sign*.65,-20.4,-2.4),.24,.15,'#667B72','head')
    a.bar('long sharp nose',(0,-20,-2.5),(0,-16.8,-3.7),.75,.8,bone,'head')
    a.box('grinning mouth',(0,-15.5,-2.28),(2.55,.65,.15),'#172523','head',fabric=False)
    for i in range(7):a.box('tiny porcelain tooth',((i-3)*.32,-15.6,-2.38),(.22,.3,.06),'#D0D4BA','head',fabric=False)
    # Branching cracks placed on the flat frontal facets; restrained enough to read in game.
    for pts in [[(-1.8,-21.5),(-.8,-20.8),(-.5,-19.7)],[(.5,-22),(1,-21),(.6,-20.3)],[(2.1,-17.7),(1.5,-17),(.5,-16.8)],[(-2.1,-17.8),(-1.6,-16.8),(-1,-16.2)],[(.1,-18.7),(-.4,-17.4),(.1,-16.5)]]:
        for p,q in zip(pts,pts[1:]):a.bar('porcelain fracture',(p[0],p[1],-2.44),(q[0],q[1],-2.44),.08,.08,'#435C55','head')
    for y,w in [(-11.5,3),(-10.3,2.6),(-9.1,2.1)]:
        for sign in [-1,1]:a.bar('exposed rib',(0,y,-1.9),(sign*w/2,y-.4,-1.7),.22,.25,bone)
    # Long jointed arms ending in splayed sewing-needle fingers.
    for side,sign in [('right',-1),('left',1)]:
        arm=side+'_arm';fore=side+'_forearm';leg=side+'_leg';shin=side+'_shin'
        a.bar('upper bone arm',(sign*5,-12,0),(sign*7,-3,0),.55,.65,bone,arm)
        a.box('sharp elbow joint',(sign*7,-3,0),(.9,.8,.9),'#7F938A',fore)
        a.bar('long bone forearm',(sign*7,-3,0),(sign*6.4,4,-1),.4,.5,bone,fore)
        a.box('skeletal palm',(sign*6.4,4.6,-1),(1,.95,.45),'#74897E',fore)
        for i in range(4):
            x=sign*6.4+(i-1.5)*.38;end=x+(i-1.5)*.4
            a.bar('needle finger proximal',(x,4.8,-1),(end,7.1,-1.3),.13,.14,bone,fore)
            a.bar('needle finger hooked tip',(end,7.1,-1.3),(end-sign*.25,8.8-abs(i-1.5)*.3,-2),.09,.1,'#657D72',fore)
        a.bar('needle thumb',(sign*6,4.3,-1),(sign*4.9,5.9,-1.6),.17,.17,bone,fore)
        # Two visible spider supports per side, emerging from the skirt.
        a.bar('spider leg upper',(sign*2,9,1),(sign*10,15,2),.5,.55,bone,leg)
        a.bar('spider leg needle',(sign*10,15,2),(sign*11.7,23.8,2.5),.25,.28,'#80978A',shin)
        a.bar('rear spider strut',(sign*2,11,2),(sign*6,17,5),.35,.4,'#84988A',leg)
        a.bar('rear spider point',(sign*6,17,5),(sign*7.5,24,6),.18,.2,'#6B8275',shin)
        for j in range(3):a.bar('arm thorn',(sign*(5.4+j*.4),-10+j*1.6,.2),(sign*(6+j*.4),-9.5+j*1.6,.2),.18,.2,'#85988C',arm)
    for i in range(9):
        x=(i-4)*1.8
        a.bar('skirt vertical fold',(x*.19,10,-1.9),(x*.5,19,-3),.25,.2,'#596057' if i%2 else '#2E3833')
        a.bar('skirt flared fold',(x*.5,19,-3),(x,23.7,-5.7),.35,.22,'#596057' if i%2 else '#2E3833')
        a.box('uneven tattered hem',(x,23.7,-4.5+(i%3)*.5),(1.4,.55,3.2),'#424B42')

def legacy_mother(a):
    beldam=a.name=='beldam';skin='#C2CCCC' if beldam else '#E0BFAE';cloth='#645F52' if beldam else '#151925'
    a.rig(hip=8,neck=-10,shoulder=-4,width=4.5,knee=18)
    a.head(-9,(6,10,5),skin,'#7D7051' if beldam else '#111521',tile=12 if beldam else 11)
    a.box('long narrow neck',(0,-5.5,0),(1.3,7,1.6),skin)
    a.prism('angular corset',[(-4,6,3.5),(2,5,3.5),(8,2.5,2)],cloth)
    a.prism('waist and long skirt',[(8,2.5,2),(11,7,4),(20,4.6,3),(22,3.5,2.6)],cloth)
    a.limbs(8,-4,4.5,18,skin,skin,skin,'#7F263B',1.15,1.2,16)
    a.box('exposed collarbone',(0,-3,-1.9),(4,1.3,.2),skin)
    for sign in [-1,1]:
        a.bar('pointed shoulder collar',(sign*1.4,-4,0),(sign*5.8,-6.3,.4),1.3,1.2,cloth)
        a.box('long dark bob',(sign*2.75,-11,.8),(1.1,13,5.2),'#141724','head')
        for i in range(4):a.bar('long skeletal finger',(sign*4.5+(i-1.5)*.4,16,-.3),(sign*4.5+(i-1.5)*.65,19,-.6),.14,.17,skin,('right' if sign==-1 else 'left')+'_forearm')
    if beldam:
        a.prism('ragged floor train',[(16,4,3),(22,9,7),(24,16,12)],'#756F60')
        for i in range(8):a.box('ragged stepped hem',((i-3.5)*2,23.65,-5.3),(1.6,.7,1.6),'#827C6E')
        a.box('woven hood crown',(0,-19.6,.1),(7.3,1.5,6.3),'#80764D','head')
        for sign in [-1,1]:a.box('woven hood border',(sign*3.25,-14.1,-2.8),(1,11.2,1.3),'#80764D','head')
        a.bar('hood pointed collar',(-3,-9,-2.4),(0,-7,-3),.8,.8,'#80764D','head');a.bar('hood pointed collar',(3,-9,-2.4),(0,-7,-3),.8,.8,'#80764D','head')
    else:
        a.plate('checkered corset',(0,1,-1.83),(4.6,5.2),'#69717D',motif=True)
        a.plate('checkered dress',(0,14,-2.05),(4.5,10),'#69717D',motif=True)

def mother(a):
    skin='#D4BDA9';black='#161D25';pattern='#62666A'
    a.rig(hip=9,neck=-10,shoulder=-5,width=4.1,knee=19)
    a.prism('narrow tailored bodice',[(-6,6,3.3),(-2,5.4,3.5),(3,2.3,2),(6,2.2,2)],black)
    a.prism('flared hip pencil skirt',[(6,2.2,2),(8,7.2,4.1),(11,6.5,4),(17,3.4,2.8),(19,2.9,2.5)],black)
    a.box('long pale neck',(0,-8,0),(1.1,8.5,1.4),skin)
    a.prism('long angular face',[(-20,3.7,3.3),(-18,4.7,4),(-14,4.2,3.9),(-11.8,2.5,2.9)],skin,'head')
    a.box('black hair back',(0,-15.7,1.75),(5.4,10.2,1),black,'head')
    a.bar('pointed hair crown',(-2,-19.8,0),(.6,-24,.1),1.3,2.8,black,'head')
    a.bar('pointed hair crown',(2,-19.8,0),(.6,-24,.1),1.3,2.8,black,'head')
    a.surface('solid pointed fringe',[(-2.4,-19.8,-1.6),(.6,-24,-1.3),(2.4,-19.8,-1.6)],[(0,1,2)],black,'head')
    for sign in [-1,1]:
        a.bar('long angled bob',(sign*2.4,-19.3,0),(sign*2.3,-10.6,-.5),.8,3.8,black,'head')
        a.bar('sharp bob tip',(sign*2.3,-11,-.5),(sign*1.8,-9.7,-.8),.38,2,black,'head')
        x=sign*1.03
        a.box('button socket',(x,-16.9,-2.03),(1.55,1.8,.14),'#B09489','head',fabric=False)
        a.box('glossy black button eye',(x,-17,-2.15),(1.15,1.35,.16),'#13191F','head',fabric=False)
        for dx in [-.18,.18]:
            for dy in [-.2,.2]:a.box('button stitching',(x+dx,-17+dy,-2.25),(.1,.13,.04),'#69717B','head',fabric=False)
        a.bar('arched dark eyebrow',(x-sign*.62,-17.9,-2.12),(x+sign*.6,-18.55,-1.95),.19,.12,'#342A2E','head')
    a.bar('pointed nose',(0,-17,-2.1),(0,-14.9,-2.95),.55,.7,skin,'head')
    a.box('dark smiling lips',(0,-13.8,-1.96),(2,.55,.15),'#542C39','head',fabric=False)
    for i in range(5):a.box('smiling tooth',((i-2)*.29,-13.88,-2.06),(.23,.18,.04),'#E4D9C0','head',fabric=False)
    a.box('cheek beauty mark',(1.65,-14.7,-1.93),(.15,.18,.06),'#654548','head',fabric=False)
    # Pale open neckline is inset between the dark shoulders.
    a.surface('deep neckline',[(-1.9,-5.9,-1.76),(1.9,-5.9,-1.76),(1.1,-2.2,-1.82),(0,-1.4,-1.84),(-1.1,-2.2,-1.82)],[(0,1,2,3,4)],skin)
    for sign in [-1,1]:
        a.bar('collarbone',(0,-5.2,-1.9),(sign*1.55,-4.75,-1.88),.13,.1,'#B49A86')
        a.surface('pointed shoulder',[ (sign*2,-5,0),(sign*5.9,-7.2,.4),(sign*4.9,-4.7,1),(sign*2,-3.8,1)],[(0,1,2,3)],black)
        a.surface('pointed peplum',[(sign*1.3,6,-.7),(sign*5,8,.3),(sign*3.1,9,1.5),(sign*1.1,7,1)],[(0,1,2,3)],black)
    # Tiny woven checks follow the front and back facets rather than floating panels.
    sections=[(-6,6,3.3),(-2,5.4,3.5),(3,2.3,2),(6,2.2,2),(8,7.2,4.1),(11,6.5,4),(17,3.4,2.8),(19,2.9,2.5)]
    for row in range(31):
        y=-5.5+row*.78
        lo,hi=next((p,q) for p,q in zip(sections,sections[1:]) if p[0]<=y<=q[0])
        t=(y-lo[0])/(hi[0]-lo[0]);w=lo[1]+(hi[1]-lo[1])*t;d=lo[2]+(hi[2]-lo[2])*t
        for col in range(-5,6):
            x=col*.65+(row%2)*.3
            if abs(x)>w*.32:continue
            for sign in [-1,1]:
                if sign==-1 and y<-1.3 and abs(x)<1.9:continue
                a.box('woven dress check',(x,y,sign*(d/2+.025)),(.18,.22,.045),pattern,fabric=False)
    # Back crimson inset, visible only beneath the hip flare.
    a.box('crimson back inset',(0,10.4,2.07),(1.2,2.3,.12),'#763041')
    for side,sign in [('right',-1),('left',1)]:
        arm=side+'_arm';fore=side+'_forearm';leg=side+'_leg';shin=side+'_shin'
        elbow=4.5
        a.bar('long pale upper arm',(sign*4.1,-5,0),(sign*4.8,elbow,0),.75,.85,skin,arm)
        a.bar('long pale forearm',(sign*4.8,elbow,0),(sign*5.8,12,-.3),.58,.68,skin,fore)
        a.box('slender hand',(sign*5.8,12.5,-.3),(.85,1.3,.7),skin,fore)
        for i in range(4):
            x=sign*5.8+(i-1.5)*.25
            a.bar('long finger',(x,13,-.3),(x+(i-1.5)*.15,15-abs(i-1.5)*.25,-.8),.12,.16,skin,fore)
            a.box('dark nail',(x+(i-1.5)*.15,14.85-abs(i-1.5)*.25,-.85),(.13,.35,.13),'#3B2930',fore)
        a.box('pale lower leg',(sign*.95,20.6,0),(.85,3.4,1.1),skin,shin)
        a.box('red ankle boot',(sign*.95,22.65,0),(1.1,1.75,1.45),'#8C263A',shin)
        a.box('red pointed toe',(sign*.95,23.4,-.7),(1.15,.65,2.8),'#8C263A',shin)
        a.box('slender red heel',(sign*.95,23.6,.45),(.45,.8,.5),'#652331',shin)

def legacy_twins(a):
    a.rig(hip=12,neck=0,shoulder=6,width=6,knee=18)
    a.bone('second_head',(3,0,0),'body')
    a.prism('silken cocoon',[(7,7,5),(10,13,9),(17,12,8),(23,6,5)],'#D9D2B5')
    for bone,x,skin,hair,tile in [('head',-3.3,'#95B48B','#AEC786',10),('second_head',3.3,'#C18197','#D2918D',15)]:
        a.box('cocoon neck',(x,5.5,0),(1.6,3,1.8),skin)
        start=len(a.meshes);a.head(4,(6.5,8,6),skin,hair,tile,bone);a.curls(-4,6.5,6,hair,bone)
        for m in a.meshes[start:]:m['vertices']=[(v[0]+x,v[1],v[2]) for v in m['vertices']]
        for obj in a.geometry[start:]:obj.location.x=x/16
    for side,sign,color in [('right',-1,'#9BBB8B'),('left',1,'#C78B9C')]:
        a.bar('twisted long arm',(sign*5,7,0),(sign*9,12,-1),1.5,1.5,color,side+'_arm')
        a.bar('twisted long forearm',(sign*9,12,-1),(sign*13,16,-1),1.1,1.1,color,side+'_forearm')
        for i in range(3):a.bar('cocoon claw',(sign*13+(i-1)*.35,16,-1),(sign*14+(i-1)*.75,18,-1.5),.2,.2,color,side+'_forearm')
    for y,w in [(9,10),(12,12.9),(15,12.5),(18,11)]:a.box('cocoon silk seam',(0,y,-4.56),(w,.45,.18),'#F0EAD0')

def twins(a):
    a.rig(hip=15,neck=3,shoulder=10,width=6,knee=20)
    a.bone('second_head',(3,3,-2),'body')
    silk='#C4BFA8';shadow='#8C8D7D'
    a.prism('bulging silk cocoon',[(8,8,6),(11,14,9),(17,15,9),(21,10,7),(23,5,4)],silk)
    for sign in [-1,1]:
        # Flared, pleated silk ends give the combined creature its wrapped shape.
        for i in range(5):
            y=10+i*2.1
            a.surface('flared silk wrapper',[(sign*6,14,0),(sign*13,y-1,-.2),(sign*13,y+1,1.1),(sign*6,16,1.5)],[(0,1,2,3)],'#DAD6BE' if i%2 else '#A9AE9D')
            a.bar('silk pleat',(sign*6.5,15,-.1),(sign*12.8,y,-.15),.15,.17,shadow)
        a.box('wrapper cinch',(sign*6.7,15,.5),(.5,3.8,3.2),'#827E64')
    for i in range(7):
        y=10+i*1.6;w=12.4 if y<19 else 9
        a.bar('layered silk winding',(-w/2,y,-4.56),(w/2,y+1.3,-4.56),.4,.15,'#DDD7C0')
    for bone,x,skin,hair,top in [('head',-3.2,'#79A87C','#A9BC76',0),('second_head',3.2,'#C48695','#D79A88',-1)]:
        # Each head has its own animation pivot and its own face construction.
        a.box('exposed twin neck',(x,7,-2.2),(1.6,4,1.8),skin)
        a.box('angular twin head',(x,3+top,-2.5),(5.4,6.6,4.9),skin,bone)
        a.box('narrow twin jaw',(x,6.3+top,-2.7),(4.3,1.1,4.1),skin,bone)
        for sign in [-1,1]:
            ex=x+sign*1.25
            a.box('dark eye socket',(ex,2.4+top,-5.02),(1.8,2,.16),'#436454' if x<0 else '#865668',bone,fabric=False)
            a.box('black button',(ex,2.4+top,-5.15),(1.35,1.4,.16),'#172527',bone,fabric=False)
            for dx in [-.2,.2]:
                for dy in [-.2,.2]:a.box('button cross stitch',(ex+dx,2.4+top+dy,-5.25),(.14,.14,.04),'#B9B4A2',bone,fabric=False)
            a.bar('worried brow',(ex-sign*.5,.9+top,-5),(ex+sign*.6,.5+top,-4.98),.2,.13,'#486844' if x<0 else '#915563',bone)
        a.box('small twin nose',(x,3.7+top,-5.14),(.6,.8,.6),skin,bone)
        a.box('open frightened mouth',(x,5.35+top,-5),(3.1,1.2,.2),'#223631' if x<0 else '#5E2E43',bone,fabric=False)
        for i in range(6):a.box('crooked little tooth',(x+(i-2.5)*.44,4.99+top,-5.14),(.3,.32,.07),'#D9D6B7',bone,fabric=False)
        # Square curling tendrils replace the old small hair lumps.
        for i in range(5):
            startx=x+(i-2)*.85;height=3.8+(2-abs(i-2))*.7
            pts=[(startx,top-.25,-2.1),(startx+(i-2)*.35,top-height*.55,-2.1),(startx+(i-2)*.7,top-height,-2.1),(startx+(i-2)*.7+.65,top-height-.55,-2.1),(startx+(i-2)*.7+1,top-height,-2.1)]
            for p,q in zip(pts,pts[1:]):a.bar('angular curling hair',p,q,.48,.55,hair,bone)
            a.box('curl highlight',(pts[2][0],pts[2][1],-2.43),(.22,.4,.08),'#C1CB95' if x<0 else '#E5B39A',bone)
    for side,sign in [('right',-1),('left',1)]:
        arm=side+'_arm';fore=side+'_forearm'
        start=(sign*4.5,10,-3);elbow=(sign*8,12,-3.5);end=(sign*10,16,-4)
        for j,(p,q,bone) in enumerate([(start,elbow,arm),(elbow,end,fore)]):
            for k in range(6):
                t=k/6;u=(k+1)/6
                a.bar('intertwined green pink arm',tuple(p[i]+(q[i]-p[i])*t for i in range(3)),tuple(p[i]+(q[i]-p[i])*u for i in range(3)),.85,.9,'#88AA80' if k%2==0 else '#C28E98',bone)
        hand='#86AA7B' if sign<0 else '#C68D9A'
        a.box('twin hand',(sign*10,16.3,-4),(1.4,1.5,.85),hand,fore)
        for i in range(4):a.bar('long spread finger',(sign*10+(i-1.5)*.32,16.8,-4),(sign*10+(i-1.5)*.65,19-abs(i-1.5)*.3,-4.5),.18,.22,hand,fore)

def cat(a):
    black='#19212A';a.bone('body',(0,17,0));a.bone('head',(0,14,-5),'body');a.bone('tail',(0,16,5),'body')
    a.box('slender cat body',(0,17,0),(3,3.7,9),black)
    a.head(16.5,(5,4.7,4.4),black,tile=14)
    # Move the head geometry forward into its own rig location.
    for m in a.meshes[1:]:m['vertices']=[(v[0],v[1],v[2]-5) for v in m['vertices']]
    for obj in a.geometry[1:]:obj.location.y=-5/16
    for sign in [-1,1]:
        a.box('pointed rectangular ear',(sign*1.7,11,-5),(1.4,2.2,1.4),black,'head')
        a.box('ear tip',(sign*1.85,9.9,-5),(.8,.6,1),black,'head')
        for front in [True,False]:
            bone=('right' if sign==-1 else 'left')+('_arm' if front else '_leg');z=-3 if front else 3
            a.bone(bone,(sign*1.2,18,z),'body');a.bone(bone+'_paw',(sign*1.2,21,z),bone)
            a.box('cat leg',(sign*1.2,19.5,z),(.8,3.2,.8),black,bone)
            a.box('cat ankle',(sign*1.2,22.1,z),(.7,2.5,.8),black,bone+'_paw')
            a.box('cat paw',(sign*1.2,23.5,z-.35),(1.1,.8,1.5),black,bone+'_paw')
    for i,(p,q) in enumerate([((0,16,5),(0,11,6)),((0,11,6),(0,6,5.5)),((0,6,5.5),(1.5,4.7,4.8))]):a.bar('angular curled tail '+str(i),p,q,.6,.6,black,'tail')
    for sign in [-1,1]:
        for y in [14.4,15.3]:a.bar('straight whisker',(sign*1, y,-7.3),(sign*3.3,y-.3,-7.3),.06,.06,'#A0ADB1','head')

def create(name):
    a=Builder(name)
    if 'wybie' in name:wybie(a)
    elif 'bobinsky' in name:bobinsky(a)
    elif name=='evil_other_spink_forcible':twins(a)
    elif name in ('other_miss_spink','other_miss_forcible'):other_spink(a)
    elif name=='miss_spink':spink(a)
    elif name=='miss_forcible':forcible(a)
    elif 'spink' in name or 'forcible' in name:lady(a)
    elif name=='evil_other_father':father(a)
    elif name=='beldam':beldam(a)
    elif name=='evil_other_mother':mother(a)
    elif name=='coraline':coraline(a)
    else:cat(a)
    a.finish();repair_glb(name)
    return a

def repair_glb(name):
    # Blender's exporter follows the pixel image through MixRGB but omits its
    # constant tint. Preserve that authored material color in the portable GLB.
    path=ROOT/'artwork/character_cast'/f'{name}.glb';raw=path.read_bytes()
    size=struct.unpack_from('<I',raw,12)[0];document=json.loads(raw[20:20+size])
    for material in document.get('materials',[]):
        color=material.get('name','')[:7]
        if len(color)==7 and color.startswith('#'):
            rgb=[int(color[i:i+2],16)/255 for i in (1,3,5)]
            material.setdefault('pbrMetallicRoughness',{})['baseColorFactor']=[c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4 for c in rgb]+[1]
    encoded=json.dumps(document,separators=(',',':')).encode();encoded+=b' '*((-len(encoded))%4)
    rest=raw[20+size:]
    path.write_bytes(struct.pack('<4sII',b'glTF',2,20+len(encoded)+len(rest))+struct.pack('<I4s',len(encoded),b'JSON')+encoded+rest)
