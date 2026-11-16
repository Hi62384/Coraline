"""Background Blender authoring. Masters/previews stay outside packaged resources.

Run installed Blender with --background --factory-startup --threads 1 --python.
The runtime export has rigid named parts, metres, indexed triangles and no textures.
"""
import bpy, json, math
from mathutils import Vector
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artwork/insect_furniture_v2'
EXPORT=ROOT/'src/main/resources/assets/coraline/insect_furniture'
OUT.mkdir(parents=True,exist_ok=True); EXPORT.mkdir(parents=True,exist_ok=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
MATS={}; objects=[]; current=''; part='shell'

def material(color):
    if color not in MATS:
        m=bpy.data.materials.new(color);m.diffuse_color=tuple(int(color[i:i+2],16)/255 for i in (1,3,5))+(1,)
        m.use_nodes=True;n=m.node_tree.nodes.get('Principled BSDF');n.inputs['Base Color'].default_value=m.diffuse_color
        n.inputs['Roughness'].default_value=.32;n.inputs['Metallic'].default_value=.16;MATS[color]=m
    return MATS[color]

def mesh(name,vertices,faces,color,group=None):
    # Native coordinates x/right, y/up, z/back; Blender uses z/up, -y/back.
    m=bpy.data.meshes.new(name);m.from_pydata([(x,-z,y) for x,y,z in vertices],[],faces);m.update()
    o=bpy.data.objects.new(current+' / '+name,m);bpy.context.collection.objects.link(o);o.data.materials.append(material(color))
    o['part']=group or part;objects.append(o);return o

def oval(name,c,r,color,group=None,s=16,t=8):
    v=[]
    for j in range(t+1):
        a=-math.pi/2+j*math.pi/t
        for i in range(s):
            b=i*math.tau/s;v.append((c[0]+r[0]*math.cos(a)*math.cos(b),c[1]+r[1]*math.sin(a),c[2]+r[2]*math.cos(a)*math.sin(b)))
    f=[(j*s+i,j*s+(i+1)%s,(j+1)*s+(i+1)%s,(j+1)*s+i) for j in range(t) for i in range(s)]
    return mesh(name,v,[tuple(reversed(face)) for face in f],color,group)

def tube(name,points,radii,color,group=None,s=6):
    v=[]
    for i,p in enumerate(points):
        tangent=(Vector(points[min(i+1,len(points)-1)])-Vector(points[max(0,i-1)])).normalized()
        axis=tangent.cross(Vector((0,0,1)))
        if axis.length<.01:axis=tangent.cross(Vector((0,1,0)))
        axis.normalize();other=tangent.cross(axis).normalized()
        for k in range(s):v.append(tuple(Vector(p)+(axis*math.cos(k*math.tau/s)+other*math.sin(k*math.tau/s))*radii[i]))
    f=[tuple(reversed(range(s)))]+[(i*s+k,i*s+(k+1)%s,(i+1)*s+(k+1)%s,(i+1)*s+k) for i in range(len(points)-1) for k in range(s)]+[tuple(range((len(points)-1)*s,len(points)*s))]
    return mesh(name,v,f,color,group)

def shell(name,rings,color):
    v=[];s=20
    for y,rx,rz,zc in rings:
        for i in range(s):a=i*math.tau/s;v.append((math.cos(a)*rx,y,zc+math.sin(a)*rz))
    f=[tuple(reversed(range(s)))]+[(j*s+i,j*s+(i+1)%s,(j+1)*s+(i+1)%s,(j+1)*s+i) for j in range(len(rings)-1) for i in range(s)]+[tuple(range((len(rings)-1)*s,len(rings)*s))]
    mesh(name,v,[tuple(reversed(face)) for face in f],color)

def legs(radius,height,length=1.2):
    for side in (-1,1):
        for i in range(3):
            z=(i-1)*length*.45;group=f'leg_{side}_{i}'
            a=(side*radius,height,z);k=(side*(radius+.32),height*.55,z+(i-1)*.22);toe=(side*(radius+.5),.035,z+(i-1)*.36)
            tube('jointed leg',[a,k,toe,(toe[0]+side*.12,.03,toe[2]-.12)],[.045,.032,.019,.008],'#302332',group)
            oval('ball joint',k,(.06,.06,.06),'#A66E48',group,s=10,t=6)

def face(y,width,z):
    oval('head',(0,y,0),(width,.18,.22),'#BDA044')
    for side in (-1,1):
        x=side*width*.63
        oval('button bevel',(x,y+.02,z),(.115,.13,.056),'#D0A247')
        oval('recessed button',(x,y+.02,z-.05),(.083,.096,.019),'#141521')
        for dx in (-.025,.025):
            for dy in (-.028,.028):oval('glow hole',(x+dx,y+.02+dy,z-.071),(.008,.009,.005),'#E5ED9C','glow',s=6,t=4)
        points=[(side*.08,y+.14,.01),(side*.14,y+.36,.06),(side*.28,y+.56,.09),(side*.35,y+.62,.04)]
        tube('curved feeler',points,[.018,.015,.011,.004],'#332531',f'antenna_{side}')

def chair():
    legs(.36,.42,.75)
    oval('velvet seat',(0,.47,-.05),(.51,.16,.48),'#733461')
    # One tapered abdomen with incised plates, not detached stacked spheres.
    shell('purple abdomen',[(.48,.36,.19,.29),(.76,.44,.22,.30),(1.16,.38,.23,.32),(1.48,.27,.19,.30),(1.62,.15,.13,.24)],'#783A87')
    for j in range(5):
        y=.68+j*.165;rx=.43-max(0,y-.85)*.24
        points=[(math.cos(a)*rx,y+.035*math.sin(a),.31+math.sin(a)*.23) for a in [math.pi+i*math.pi/12 for i in range(13)]]
        tube('carapace seam',points,[.012]*len(points),'#452744')
    for side in (-1,1):
        tube('curled armrest',[(side*.34,.53,-.36),(side*.49,.70,-.24),(side*.48,.73,.15)],[.055,.065,.045],'#A65C8D')
        for i in range(3):oval('stitched tuft',(side*.18,.52,-.30+i*.23),(.018,.009,.018),'#D39DAD',s=6,t=4)
    face(1.70,.29,.0)

def wardrobe():
    legs(.80,.72,1.4)
    shell('roach cabinet',[(.24,.28,.20,0),(.55,.62,.43,0),(1.15,.90,.57,0),(2.0,1.02,.63,0),(2.72,.90,.56,0),(3.08,.55,.39,0)],'#A36350')
    for j in range(5):
        y=.59+j*.49;w=.50+math.sin((y-.23)/3.0*math.pi)*.46
        tube('drawer lip',[(-w,y,-.52),(0,y-.045,-.64),(w,y,-.52)],[.018]*3,'#623940')
        for side in ((-1,1) if j>=3 else (0,)):
            oval('brass drawer rose',(side*.14,y+.16,-.635),(.061,.061,.022),'#B38C49',s=10,t=6)
            oval('black drawer knob',(side*.14,y+.16,-.667),(.037,.037,.028),'#26202C',s=10,t=6)
    tube('cupboard door seam',[(0,2.07,-.641),(0,2.7,-.56),(0,3.02,-.42)],[.013]*3,'#57323A')
    face(3.25,.46,-.20)

def table():
    legs(.64,.49,1.05)
    oval('beetle lower shell',(0,.61,0),(1.02,.19,.52),'#1B595A')
    for side in (-1,1):
        oval('elytron glass',(side*.45,.75,.02),(.52,.105,.47),'#377D66')
        for i in range(4):
            z=-.30+i*.20;tube('wing vein',[(side*.07,.85,z),(side*.45,.84,z+.025),(side*.84,.80,z+.08)],[.012]*3,'#D3B154')
    tube('central gold seam',[(0,.857,-.40),(0,.86,.43)],[.016]*2,'#D8BA59')
    oval('table beetle head',(0,.68,-.48),(.28,.15,.22),'#8B9850')
    for side in (-1,1):oval('table button',(side*.13,.73,-.65),(.057,.065,.027),'#191D25')
    for i in (-2,-1,0,1,2):oval('green lit wing window',(i*.32,.856,0),(.11,.012,.27),'#ACD768','glow',s=12,t=6)

def couch():
    legs(1.17,.45,1.0)
    oval('red velvet seat',(0,.52,-.1),(1.47,.25,.63),'#A72D45')
    for i in range(-3,4):
        x=i*.39
        oval('continuous scalloped back',(x,1.25,.42),(.30,.80,.24),'#AE2845')
        tube('piped seam',[(x-.19,.73,.19),(x-.22,1.35,.19),(x-.14,1.83,.27)],[.012]*3,'#CF5A5A')
        oval('teal crest plate',(x,1.96,.41),(.18,.14,.15),'#277A88')
        oval('seat button',(x,.746,-.16),(.025,.009,.025),'#E3A178',s=8,t=4)
    for side in (-1,1):oval('rolled velvet arm',(side*1.43,.79,-.08),(.27,.43,.59),'#932C42')
    tube('gilded front rail',[(-1.3,.46,-.58),(0,.40,-.67),(1.3,.46,-.58)],[.022]*3,'#D59B59')

def lamp():
    oval('bronze foot',(0,.07,0),(.36,.07,.28),'#2C2834')
    tube('mantis articulated stem',[(0,.1,0),(.1,1.15,0),(.55,1.93,0),(.68,1.98,-.13)],[.055,.045,.03,.02],'#216575')
    oval('mantis head',(.69,1.98,-.12),(.31,.12,.18),'#3AB5C2')
    for side in (-1,1):
        tube('mantis folded arm',[(.48,1.83,side*.08),(.30,1.25,side*.26),(.57,1.53,side*.18)],[.025,.018,.009],'#CA994D')
        oval('amber mantis eye',(.82,2.01,side*.15),(.045,.05,.04),'#C5DD6D','glow')

def chandelier():
    tube('ceiling stem',[(0,1.3,0),(0,.12,0)],[.024,.032],'#352837')
    oval('beetle canopy',(0,.20,0),(.22,.37,.24),'#7D8B44')
    for i in range(7):
        a=i*math.tau/7;r=1+i%2*.22;x=math.cos(a)*r;z=math.sin(a)*r
        tube('curving insect arm',[(0,.10,0),(x*.65,-.20,z*.65),(x,-.69,z)],[.02,.014,.008],'#3B2C3F')
        oval('green seed lantern',(x,-.83,z),(.11,.21,.11),'#A5BA40')
        oval('lantern window',(x,-.85,z),(.095,.15,.095),'#DEE990','glow',s=12,t=6)

def export(name,build):
    global current,objects
    current=name;objects=[];build();batches={}
    for o in objects:
        o.data.calc_loop_triangles();color=o.data.materials[0].name;key=(o['part'],color)
        b=batches.setdefault(key,{'part':o['part'],'color':color,'vertices':[],'faces':[]})
        offset=len(b['vertices'])
        b['vertices'] += [[round(v.co.x,5),round(v.co.z,5),round(-v.co.y,5)] for v in o.data.vertices]
        b['faces'] += [[offset+i for i in tri.vertices] for tri in o.data.loop_triangles]
    doc={'format':1,'id':name,'units':'metres','front':'negative_z','meshes':list(batches.values())}
    (EXPORT/(name+'.json')).write_text(json.dumps(doc,separators=(',',':')),encoding='utf8')
    # Layout is only for the preserved Blender studio; exported origins stay local.
    shift={'chair':(-3,0,0),'wardrobe':(0,2,0),'table':(0,-2,0),'couch':(3,1,0),'lamp':(-3,2,0),'chandelier':(3,0,3)}[name]
    for o in objects:o.location=shift
    print(f'EXPORTED {name}: {sum(len(b["faces"]) for b in batches.values())} triangles',flush=True)

for name,build in [('chair',chair),('wardrobe',wardrobe),('table',table),('couch',couch),('lamp',lamp),('chandelier',chandelier)]:export(name,build)
scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.render.resolution_x=1200;scene.render.resolution_y=800;scene.render.resolution_percentage=100
scene.display.shading.light='STUDIO';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True;scene.display.shading.show_cavity=True
scene.world.color=(.08,.07,.10)
bpy.ops.object.camera_add(location=(8,12,8));camera=bpy.context.object;camera.rotation_euler=(Vector((0,0,1.7))-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.type='ORTHO';camera.data.ortho_scale=12;scene.camera=camera
scene.render.image_settings.file_format='PNG';scene.render.filepath=str(OUT/'furniture_studio.png')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'insect_furniture_master.blend'))
bpy.ops.render.render(write_still=True)
print('INSECT_FURNITURE_EXPORTED',flush=True)
