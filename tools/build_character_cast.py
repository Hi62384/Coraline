"""Author the reference cast, articulated Blender masters and compact native meshes.

Run with Blender 3.5 --background --python tools/build_character_cast.py.
No external add-ons or image downloads. All generated artwork is kept outside
runtime resources except the indexed meshes and one shared white texel.
"""
import bpy, json, math, random
from pathlib import Path
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'artwork/character_cast'
RUNTIME = ROOT / 'src/main/resources/assets/coraline/characters'
OUT.mkdir(parents=True, exist_ok=True)
RUNTIME.mkdir(parents=True, exist_ok=True)
SKIN='#C99C75'; BLACK='#151923'; GOLD='#D8AC49'; BLUE='#789BAA'
CAST=['wybie','other_wybie','bobinsky','other_bobinsky','miss_spink','miss_forcible',
      'other_miss_spink','other_miss_forcible','evil_other_father','evil_other_bobinsky',
      'evil_other_spink_forcible','evil_other_mother','beldam','coraline','cat']

def world(v): return (v[0]/16, v[2]/16, (24-v[1])/16)

class Author:
    def __init__(self, name):
        scene=bpy.data.scenes.new('CharacterCast_'+name)
        scene.world=bpy.data.worlds.new('Character studio '+name)
        bpy.context.window.scene=scene
        self.name=name; self.bones={}; self.meshes=[]; self.geometry=[]; self.materials={}; self.objects={}
        self.rng=random.Random(214+CAST.index(name))
    def bone(self, name, pos, parent=None):
        obj=bpy.data.objects.new(name,None); bpy.context.collection.objects.link(obj)
        obj.empty_display_type='SPHERE';obj.empty_display_size=.035
        obj.location=world(pos)
        if parent:
            obj.parent=self.objects[parent]
            # Explicit local translation avoids reliance on deferred depsgraph updates.
            p=self.bones[parent]['absolute']; obj.location=((pos[0]-p[0])/16,(pos[2]-p[2])/16,-(pos[1]-p[1])/16)
        self.bones[name]={'absolute':pos,'parent':parent}
        self.objects[name]=obj
    def mesh(self, name, verts, faces, color, bone='body'):
        if color not in self.materials:
            mat=bpy.data.materials.new(color);mat.diffuse_color=tuple(int(color[i:i+2],16)/255 for i in (1,3,5))+(1,)
            mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
            rgb=mat.diffuse_color[:3];bs.inputs['Base Color'].default_value=tuple(c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4 for c in rgb)+(1,);bs.inputs['Roughness'].default_value=.72
            self.materials[color]=mat
        p=self.bones[bone]['absolute']
        local=[tuple(round(v[i]-p[i],4) for i in range(3)) for v in verts]
        mesh=bpy.data.meshes.new(name);mesh.from_pydata([(v[0]/16,v[2]/16,-v[1]/16) for v in local],[],faces);mesh.update()
        obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj);obj.parent=self.objects[bone];obj.data.materials.append(self.materials[color])
        self.geometry.append(obj)
        for f in mesh.polygons:f.use_smooth=True
        self.meshes.append({'name':name,'bone':bone,'color':color,'vertices':local,'faces':faces})
    def oval(self,name,c,r,color,bone='body',s=12,t=8):
        verts=[]
        for j in range(t+1):
            lat=math.pi*j/t
            for i in range(s):
                lon=2*math.pi*i/s
                verts.append((c[0]+r[0]*math.sin(lat)*math.cos(lon),c[1]+r[1]*math.cos(lat),c[2]+r[2]*math.sin(lat)*math.sin(lon)))
        faces=[]
        for j in range(t):
            for i in range(s):faces.append((j*s+i,j*s+(i+1)%s,(j+1)*s+(i+1)%s,(j+1)*s+i))
        self.mesh(name,verts,faces,color,bone)
    def tube(self,name,points,radii,color,bone='body',s=8):
        verts=[]
        for i,c in enumerate(points):
            tangent=Vector(points[min(i+1,len(points)-1)])-Vector(points[max(0,i-1)])
            tangent.normalize();axis=tangent.cross(Vector((0,0,1)))
            if axis.length<.01:axis=tangent.cross(Vector((0,1,0)))
            axis.normalize();other=tangent.cross(axis).normalized()
            for k in range(s):
                v=Vector(c)+(axis*math.cos(k*2*math.pi/s)+other*math.sin(k*2*math.pi/s))*radii[i]
                verts.append(tuple(v))
        faces=[tuple(reversed(range(s)))]
        for i in range(len(points)-1):
            for k in range(s):faces.append((i*s+k,i*s+(k+1)%s,(i+1)*s+(k+1)%s,(i+1)*s+k))
        faces.append(tuple(range((len(points)-1)*s,len(points)*s)))
        self.mesh(name,verts,faces,color,bone)
    def skirt(self,name,top,bottom,r1,r2,color,bone='body'):
        points=[(0,top,0),(0,bottom,0)]; self.tube(name,points,[r1,r2],color,bone,20)
    def face(self,y,rx=3.5,ry=4,rz=2.8,skin=SKIN,button=False,eyes='#545944',mouth='#743F40',head='head'):
        self.oval('sculpted face',(0,y,0),(rx,ry,rz),skin,head,16,10)
        self.oval('nose',(0,y+.3,-rz),(rx*.19,.7,.85),skin,head)
        for sign in [-1,1]:
            x=sign*rx*.43;z=-rz*.87
            if button:self.button((x,y-.4,z-.27),rx*.25,head)
            else:
                self.oval('eye white',(x,y-.4,z),(.85,1.05,.34),'#F1EBDA',head)
                self.oval('iris',(x,y-.35,z-.32),(.43,.55,.14),eyes,head)
                self.oval('pupil',(x,y-.35,z-.43),(.22,.32,.08),BLACK,head)
                self.oval('eye glint',(x-.1,y-.58,z-.49),(.09,.13,.03),'#FFFFFF',head,6,4)
            self.tube('eyebrow',[(x-.7,y-1.65,z-.1),(x,y-1.85,z-.15),(x+.65,y-1.65,z-.1)],[.13]*3,'#34282B',head)
            self.oval('ear',(sign*(rx+.1),y+.1,.1),(.55,.95,.42),skin,head)
        self.tube('mouth',[(-rx*.45,y+ry*.45,-rz*.91),(0,y+ry*.57,-rz-.12),(rx*.43,y+ry*.42,-rz*.92)],[.1,.13,.1],mouth,head)
    def button(self,c,r,bone):
        self.oval('black button rim',c,(r,r,.23),'#090E16',bone,12,6)
        self.oval('button center',(c[0],c[1],c[2]-.2),(r*.74,r*.74,.09),'#252832',bone,10,4)
        for dx,dy in [(-1,-1),(1,-1),(1,1),(-1,1)]:
            self.oval('button hole',(c[0]+dx*r*.22,c[1]+dy*r*.22,c[2]-.29),(r*.08,r*.08,.035),'#020509',bone,6,4)
        for sign in [-1,1]:self.tube('sewn thread',[(c[0]-r*.25,c[1]+sign*r*.25,c[2]-.34),(c[0]+r*.25,c[1]-sign*r*.25,c[2]-.34)],[.045,.045],'#998F79',bone,5)
    def skeleton(self,hip=11,head=1,shoulder=4,width=4,knee=18):
        self.bone('body',(0,hip,0));self.bone('head',(0,head,0),'body')
        for sign,side in [(-1,'right'),(1,'left')]:
            self.bone(side+'_arm',(sign*width,shoulder,0),'body');self.bone(side+'_forearm',(sign*(width+.5),(shoulder+hip+3)/2,0),side+'_arm')
            self.bone(side+'_leg',(sign*width*.43,hip,0),'body');self.bone(side+'_shin',(sign*width*.43,knee,0),side+'_leg')
        self.bone('tail',(0,hip,2),'body')
    def limbs(self,hip,shoulder,width,knee,skin,coat,boots=BLACK,thin=.65,armend=15,legcolor=None):
        elbow=(shoulder+hip+3)/2
        for sign,side in [(-1,'right'),(1,'left')]:
            x=sign*width
            self.oval('rounded shoulder',(x,shoulder,0),(thin*1.2,thin*1.2,thin*1.2),coat,side+'_arm',8,5)
            self.tube('upper sleeve',[(x,shoulder,0),(x+sign*.5,elbow,0)],[thin*1.15,thin],coat,side+'_arm')
            self.tube('forearm',[(x+sign*.5,elbow,0),(x+sign*.7,armend,-.2)],[thin,thin*.7],coat,side+'_forearm')
            self.oval('hand',(x+sign*.7,armend+.5,-.2),(thin*.8,1,thin*.55),skin,side+'_forearm')
            lx=sign*width*.43
            self.tube('thigh',[(lx,hip,0),(lx,knee,0)],[thin,thin*.77],legcolor or coat,side+'_leg')
            self.tube('shin',[(lx,knee,0),(lx,23,.15)],[thin*.77,thin*.58],legcolor or skin,side+'_shin')
            self.oval('boot',(lx,23.2,-.7),(thin*1.12,.8,1.6),boots,side+'_shin')
    def curls(self,y,rx,ry,rz,color,head='head',count=20):
        for i in range(count):
            angle=i*2.399963;h=self.rng.uniform(-ry,-ry*.15)
            x=rx*math.cos(angle)*self.rng.uniform(.65,1);z=rz*math.sin(angle)
            self.oval('hair curl',(x,y+h,z),(.75,.8,.7),color,head,8,5)
    def hat(self,y,color=BLACK):
        self.oval('top hat brim',(0,y,0),(3.3,.3,2.8),color,'head')
        self.tube('top hat',[(0,y,0),(0,y-7,0)],[2.2,2.5],color,'head',16)
        self.tube('red hat band',[(0,y-.2,0),(0,y-1,0)],[2.24,2.28],'#AA303E','head',16)
    def finish(self):
        bones=[]
        for n,b in self.bones.items():
            parent=self.bones[b['parent']]['absolute'] if b['parent'] else (0,0,0)
            bones.append({'name':n,'parent':b['parent'],'pivot':[round(b['absolute'][i]-parent[i],4) for i in range(3)]})
        # Merge surfaces by bone/color for cheap transforms at runtime.
        batches={}
        for m in self.meshes:
            part=m['name'] if (self.name in ('wybie','other_wybie') and m['name'] in ('eye white','green hazel iris','pupil','eye glint','uneven concerned brow','crooked smile')) or (self.name=='bobinsky' and m['name'] in ('smiling mouth','small square tooth','expressive eyebrow','eye pupil')) else ''
            key=(m['bone'],m['color'],part);b=batches.setdefault(key,{'bone':m['bone'],'part':part,'color':m['color'],'vertices':[],'uv':[],'faces':[]})
            off=len(b['vertices']);b['vertices'].extend(m['vertices']);b['uv'].extend(m.get('uv',[(.5,.5)]*len(m['vertices'])));b['faces'].extend([[i+off for i in f] for f in m['faces']])
        doc={'format':1,'id':self.name,'texture':'characters/cast_atlas.png' if getattr(self,'pixel_style',False) else 'characters/white.png','bones':bones,'meshes':list(batches.values()),'animations':['idle','walk','run','look','wave']}
        (RUNTIME/(self.name+'.json')).write_text(json.dumps(doc,separators=(',',':')),encoding='utf8')
        scene=bpy.context.scene;scene.render.fps=24;scene.frame_start=1;scene.frame_end=49
        # Each animation is an NLA track shared by all bones; exported GLBs retain clips.
        for clip in doc['animations']:
            for n,obj in self.objects.items():
                obj.animation_data_create();action=bpy.data.actions.new(clip+'_'+n);obj.animation_data.action=action
                for frame in range(1,50,4):
                    t=(frame-1)/48*math.tau;rx=ry=rz=0
                    if clip=='idle':
                        if n=='body':rx=.018*math.sin(t)
                        if n.endswith('arm'):rz=(.035 if n.startswith('left') else -.035)+.018*math.sin(t)
                    if clip in ('walk','run'):
                        speed=1 if clip=='walk' else 2;phase=t*speed+(math.pi if n.startswith('left') else 0)
                        if n.endswith('_leg'):rx=.48*math.sin(phase)
                        if n.endswith('_arm'):rx=-.38*math.sin(phase)
                        if n.endswith('_shin'):rx=max(0,-math.sin(phase))*.6
                        if n=='tail':ry=.2*math.sin(t)
                    if clip=='look' and n=='head':ry=.55*math.sin(t);rx=.16*math.cos(t)
                    if clip=='wave' and n=='right_arm':rz=-1.7;rx=.15*math.sin(t*2)
                    if clip=='wave' and n=='right_forearm':rz=-.45+.3*math.sin(t*3)
                    obj.rotation_euler=(rx,rz,-ry);obj.keyframe_insert(data_path='rotation_euler',frame=frame)
                # Close loop exactly at frame 49.
                curves=action.fcurves if hasattr(action,'fcurves') else action.layers[0].strips[0].channelbag(obj.animation_data.action_slot).fcurves
                for fcurve in curves:
                    for kp in fcurve.keyframe_points:kp.interpolation='BEZIER'
                obj.animation_data.action=None
                track=obj.animation_data.nla_tracks.new();track.name=clip;strip=track.strips.new(clip,1,action);track.mute=True
        scene.frame_set(1)
        for obj in self.objects.values():obj.rotation_euler=(0,0,0)
        scene.world.color=(.08,.09,.12)
        ys=[v[1]+self.bones[m['bone']]['absolute'][1] for m in self.meshes for v in m['vertices']] if False else []
        geometry=[obj for obj in bpy.context.scene.objects if obj.type=='MESH']
        bpy.context.view_layer.update()
        pts=[obj.matrix_world@Vector(c) for obj in geometry for c in obj.bound_box]
        top=max(p.z for p in pts);bottom=min(p.z for p in pts);height=top-bottom
        target=Vector((0,0,(top+bottom)/2))
        bpy.ops.object.camera_add(location=(3,-7,(top+bottom)/2+1.4))
        cam=bpy.context.object;cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=max(height*1.2,2.6);scene.camera=cam
        for pos,power in [((3,-4,6),850),((-3,-2,4),550),((0,3,5),700)]:
            bpy.ops.object.light_add(type='AREA',location=pos);lamp=bpy.context.object;lamp.data.energy=power;lamp.data.size=5;lamp.rotation_euler=(target-lamp.location).to_track_quat('-Z','Y').to_euler()
        scene.render.engine='BLENDER_EEVEE';scene.render.resolution_x=560;scene.render.resolution_y=700;scene.render.resolution_percentage=100
        scene.view_settings.view_transform='Standard';scene.render.film_transparent=True
        scene.render.filepath=str(OUT/(self.name+'.png'))
        bpy.data.libraries.write(str(OUT/(self.name+'.blend')),{scene},fake_user=True,compress=True)
        export_args={'filepath':str(OUT/(self.name+'.glb')),'export_format':'GLB','export_animations':True,'export_nla_strips':True,'export_cameras':False,'export_lights':False,'use_active_scene':True}
        if 'export_animation_mode' in bpy.ops.export_scene.gltf.get_rna_type().properties:
            export_args['export_animation_mode']='NLA_TRACKS'
        bpy.ops.export_scene.gltf(**export_args)
        for name,obj in self.objects.items():
            for track in obj.animation_data.nla_tracks:
                track.mute=True
                # glTF export can leave muted strips with zero influence.
                # Restore it so unmuting a clip in the editable master plays it.
                for strip in track.strips:strip.influence=1.0
            pos=self.bones[name]['absolute'];parent=self.bones[name]['parent']
            if parent:
                p=self.bones[parent]['absolute'];obj.location=((pos[0]-p[0])/16,(pos[2]-p[2])/16,-(pos[1]-p[1])/16)
            else:obj.location=world(pos)
            obj.rotation_euler=(0,0,0)
            obj.matrix_parent_inverse.identity()
        scene.frame_set(1);bpy.context.view_layer.update()
        bpy.data.libraries.write(str(OUT/(self.name+'.blend')),{scene},fake_user=True,compress=True)
        bpy.ops.render.render(write_still=True)
        print('CHARACTER_DONE',self.name,sum(len(m['vertices']) for m in doc['meshes']))

for name in CAST:
    raise RuntimeError('Create the Minecraft cast through Blender MCP using tools/build_minecraft_character_cast.py')
