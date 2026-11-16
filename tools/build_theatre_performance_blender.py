"""Run in the connected Blender. Preserve existing scenes; export only theatre assets."""
import bpy, json, math
from pathlib import Path
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'artwork/theatre_performance'
OUT.mkdir(parents=True,exist_ok=True)
SOURCE=ROOT/'tools/build_minecraft_character_cast.py'
NS={'__file__':str(SOURCE)}
exec(compile(SOURCE.read_text(encoding='utf8'),str(SOURCE),'exec'),NS)
Builder=NS['Builder']
def ring(a,name,x,y,z,r,inner,color,bone='head'):
    vertices=[]
    for depth,rad in [(z,r),(z-.16,r),(z-.16,inner),(z,inner)]:
        for i in range(12):
            t=i*math.tau/12;vertices.append((x+math.cos(t)*rad,y+math.sin(t)*rad,depth))
    faces=[]
    for band in range(4):
        for i in range(12):faces.append((band*12+i,band*12+(i+1)%12,((band+1)%4)*12+(i+1)%12,((band+1)%4)*12+i))
    a.surface(name,vertices,faces,color,bone)
def export_actor(forcible):
    original='other_miss_forcible' if forcible else 'other_miss_spink'
    target='theatre_forcible_costume' if forcible else 'theatre_spink_costume'
    a=Builder(original);scene=bpy.context.scene;scene.name=target
    skin='#E2BFA9' if forcible else '#CBA48A';gold='#E4C57F';hair='#B97036' if forcible else '#A23D31'
    a.rig(hip=13,neck=2,shoulder=6,width=7.3 if forcible else 6.4,knee=19)
    # Separate silhouettes: Forcible's broad bare midriff and fan, Spink's fitted mermaid tail.
    a.prism('rounded waist',[(5,8,5.6),(8,14.6 if forcible else 12.8,7.5),(12,15.8 if forcible else 13.2,8),(15,13,7)],skin)
    a.prism('neck',[(1,3.8,3),(5,4.5,3.5)],skin)
    a.prism('rounded cheek silhouette',[(-7,4.8,4.5),(-5,7.5,6),(-1,7.8,6),(1,5.5,4.6),(2,3.3,3.8)],skin,'head')
    a.box('nose',(0,-1.6,-3.15),(1.2,1.9,1.1),skin,'head',fabric=False)
    a.box('lipstick',(0,.3,-2.75),(1.9,.55,.25),'#944457','head',fabric=False)
    for sign in [-1,1]:
        # Thick annular button geometry with a real recessed bore plus four holes.
        ring(a,'black button rim',sign*1.85,-3.1,-3.2,1.08,.17,'#0B1018')
        for dx in [-.27,.27]:
            for dy in [-.27,.27]:ring(a,'button hole rim',sign*1.85+dx,-3.1+dy,-3.39,.16,.09,'#222B36')
        a.box('button highlight',(sign*1.85-.35,-3.55,-3.43),(.20,.15,.06),'#A8B9BD','head',fabric=False)
        a.box('arched brow',(sign*1.8,-4.65,-2.84),(1.8,.3,.18),hair,'head',fabric=False)
        for j in range(4):a.box('framing curl',(sign*(2.5+j%2*.5),-6.1+j*.8,.3),(1.6,1.55,3.7),hair,'head')
    a.box('red crown hair',(0,-6.8,.2),(6.4,1.5,4.6),hair,'head')
    if forcible:
        # Orange sunburst hair and a jewel-studded iridescent peacock shell costume.
        for i in range(15):
            t=i*math.tau/15;start=(math.cos(t)*3,-3+math.sin(t)*4,.6)
            end=(math.cos(t)*6.2,-3+math.sin(t)*7,1.1)
            a.bar('flowing copper locks',start,end,.55,.65,hair,'head')
        for sign in [-1,1]:
            a.box('jewel shell cup',(sign*3.4,6.2,-3.5),(5.9,3.8,2.3),'#B09263')
            for row in range(3):
                for col in range(4):a.box('peacock cup jewel',(sign*3.4+(col-1.5)*1.1,5.4+row*.8,-4.72),(.5,.65,.15),['#32736E','#3B517A','#73A2A1'][(row+col)%3],fabric=False)
        a.prism('peacock briefs',[(12,13,6.8),(14,10,6),(16,4,3)],'#255E67')
        # Fan stays behind the torso, never hides the face or partner.
        for i in range(13):
            t=(i/12-.5)*2.5;x=math.sin(t)*11;y=15-math.cos(t)*10
            a.bar('peacock fan feather',(0,16,2.5),(x,y,4),1.3,.25,['#2C596D','#497F83','#83A6A0'][i%3])
            a.box('fan eye',(x,y,3.8),(1.3,1.7,.15),'#D4AD6B')
    else:
        a.prism('gold mermaid bodice',[(4.5,10.8,6),(8.2,13.8,8),(9.5,12.5,7.5)],gold)
        for row in range(3):
            for col in range(8):a.box('bodice scales',((col-3.5)*1.35,5.2+row*1.2,-3.8),(.85,.7,.2),'#89945A')
        a.prism('sculpted mermaid tail',[(13,13.5,7.5),(17,12.2,7),(21,8,5),(23.8,4.8,3.5)],'#326352','tail')
        for row in range(9):
            width=12.8-row*.86
            for col in range(9):
                x=(col-4)*width/9+(row%2)*.35
                a.box('overlapping gold green scale',(x,13.6+row*1.05,-3.8+row*.2),(.75,.60,.12),['#9AA56C','#D4CF95','#547B58'][(row+col)%3],'tail')
        for sign in [-1,1]:a.bar('mermaid fluke',(0,23.1,0),(sign*7,23.2,-1.4),2.3,1,'#7F9C55','tail')
        a.box('gold hair shell',(0,-7.8,-.5),(1.5,2.4,1.3),gold,'head')
    width=7.3 if forcible else 6.4
    for side,sign in [('right',-1),('left',1)]:
        a.bar('upper arm',(sign*width,6,0),(sign*(width+.3),10.2,-.2),1.8,1.9,skin,side+'_arm')
        a.bar('forearm',(sign*(width+.3),10.2,-.2),(sign*(width+.1),14.0,-.6),1.5,1.65,skin,side+'_forearm')
        a.box('expressive hand',(sign*width,14.7,-.6),(1.8,1.8,1.6),skin,side+'_forearm')
        for i in range(4):a.box('individual finger',(sign*width+(i-1.5)*.40,15.8,-.7),(.28,1.2,.45),skin,side+'_forearm')
        if forcible:
            a.bar('upper leg',(sign*2.6,14,0),(sign*2.7,19,0),2.9,3.2,skin,side+'_leg')
            a.bar('calf',(sign*2.7,19,0),(sign*2.7,23,0),2.2,2.5,skin,side+'_shin')
            a.box('gold stage shoe',(sign*2.7,23.5,-.6),(2.6,1,4),gold,side+'_shin')
    # Native format exactly matches CharacterModel's bone-local indexed surface loader.
    bones=[]
    for name,n in a.bones.items():
        parent=a.bones[n['parent']]['absolute'] if n['parent'] else (0,0,0)
        bones.append({'name':name,'parent':n['parent'],'pivot':[n['absolute'][i]-parent[i] for i in range(3)]})
    batches={}
    for m in a.meshes:
        key=(m['bone'],m['color']);batch=batches.setdefault(key,{'bone':m['bone'],'part':'','color':m['color'],'vertices':[],'uv':[],'faces':[]})
        offset=len(batch['vertices']);batch['vertices']+=m['vertices'];batch['uv']+=m.get('uv',[(.00097656,.99609375)]*len(m['vertices']));batch['faces'] += [[v+offset for v in face] for face in m['faces']]
    clips=['idle','walk','turn','dance','push','stumble','recover','rail_grip','bow','trapeze','unzip']
    doc={'format':1,'id':target,'texture':'characters/cast_atlas.png','bones':bones,'meshes':list(batches.values()),'animations':clips}
    (ROOT/f'src/main/resources/assets/coraline/characters/{target}.json').write_text(json.dumps(doc,separators=(',',':')),encoding='utf8')
    # Real Blender armature with rigid, one-bone weights for the native articulated style.
    arm=bpy.data.armatures.new(target+'_rig');obj=bpy.data.objects.new(target+'_rig',arm);scene.collection.objects.link(obj)
    bpy.context.view_layer.objects.active=obj;obj.select_set(True);bpy.ops.object.mode_set(mode='EDIT')
    def wp(v):return Vector((v[0]/16,v[2]/16,(24-v[1])/16))
    for name,n in a.bones.items():
        b=arm.edit_bones.new(name);b.head=wp(n['absolute']);b.tail=b.head+Vector((0,0,.16))
        if n['parent']:b.parent=arm.edit_bones[n['parent']]
    bpy.ops.object.mode_set(mode='OBJECT');bpy.context.view_layer.update()
    for mesh,m in zip(a.geometry,a.meshes):
        transform=mesh.matrix_world.copy();mesh.parent=None;mesh.matrix_world=transform
        mesh.data.transform(mesh.matrix_world);mesh.matrix_world.identity();group=mesh.vertex_groups.new(name=m['bone']);group.add(list(range(len(mesh.data.vertices))),1,'REPLACE');mod=mesh.modifiers.new('Stage armature','ARMATURE');mod.object=obj;mesh.parent=obj
    for empty in a.objects.values():empty.hide_render=True;empty.hide_viewport=True
    scene.render.fps=20;scene.frame_start=1;scene.frame_end=121
    for clip in clips:
        obj.animation_data_create();action=bpy.data.actions.new(target+'_'+clip);obj.animation_data.action=action
        for frame in range(1,122,5):
            t=(frame-1)/120*math.tau
            for name,b in obj.pose.bones.items():
                b.rotation_mode='XYZ';b.rotation_euler=(0,0,0)
                if name.endswith('_arm'):
                    sign=-1 if name.startswith('right') else 1
                    b.rotation_euler.x=(-.9+.18*math.sin(t)) if clip in ('push','rail_grip') else -.35*math.sin(t+(0 if sign<0 else math.pi)) if clip in ('walk','dance') else -2.8 if clip=='trapeze' else -.15
                    b.rotation_euler.y=sign*(.35 if clip in ('stumble','recover') else .1)
                if name.endswith('_leg') and clip in ('walk','dance','stumble'):b.rotation_euler.x=.30*math.sin(t+(0 if name.startswith('right') else math.pi))
                if name=='body':b.rotation_euler.y=.12*math.sin(t) if clip in ('dance','stumble') else 0;b.rotation_euler.x=.5*math.sin(t/2) if clip=='bow' else 0
                if name=='head':b.rotation_euler.z=.25*math.sin(t) if clip in ('turn','recover') else .06*math.sin(t)
                b.keyframe_insert(data_path='rotation_euler',frame=frame)
        obj.animation_data.action=None;track=obj.animation_data.nla_tracks.new();track.name=clip;track.strips.new(clip,1,action);track.mute=True
    scene.frame_set(1)
    for b in obj.pose.bones:b.rotation_euler=(0,0,0)
    refs=[]
    for name,path in [('Spink reference','C:/Users/User1/AppData/Local/Temp/codex-clipboard-cde3b2c5-3e1f-456f-a338-c61a0c3176ae.png'),('Forcible reference','C:/Users/User1/AppData/Local/Temp/codex-clipboard-913e02f6-a3b1-4cc4-9514-03a4b6d59d98.png')]:
        ref=bpy.data.objects.new(name,None);scene.collection.objects.link(ref);ref.empty_display_type='IMAGE';ref.data=bpy.data.images.load(path,check_existing=True);ref.data.pack();ref.location=(-3 if 'Spink' in name else 3,1,1);ref.hide_render=True;refs.append(ref)
    # A saved master, separate from runtime meshes. Keep the user's original scene intact.
    bpy.data.libraries.write(str(OUT/(target+'.blend')),{scene},fake_user=True)
    print(target,'meshes',len(a.geometry),'native_batches',len(batches),'vertices',sum(len(m['vertices']) for m in a.meshes),'weighted',sum(len(o.data.vertices) for o in a.geometry),'clips',len(clips))
    return scene

def export_stage():
    a=Builder('other_miss_spink');scene=bpy.context.scene;scene.name='Theatre handcrafted sea'
    a.bone('body',(0,0,0));a.bone('boat',(0,-24,0),'body')
    a.prism('painted wooden hull',[(-22,236,86),(-16,210,73),(-9,174,55)],'#784C38','boat')
    a.box('deck',(0,-24,0),(232,3,80),'#BE9365','boat')
    for x in range(-104,105,16):a.box('deck plank seam',(x,-25.6,0),(.7,.08,78),'#5C4335','boat',fabric=False)
    for sign in [-1,1]:
        for x in range(-104,105,26):a.bar('rail post',(x,-24,sign*35),(x,-39,sign*35),2,2,'#C9AB77','boat')
        a.bar('low handrail',(-110,-39,sign*35),(110,-39,sign*35),3,3,'#DEC18B','boat')
    for x in [-104,104]:a.box('painted prow',(x,-26,0),(10,12,70),'#A56345','boat')
    a.bar('mast',(0,-24,22),(0,-126,22),3.2,3.2,'#C49D6A','boat')
    a.surface('cream theatrical sail',[(0,-123,22),(68,-82,22),(1,-79,22)],[(0,1,2)],'#F3DFB1','boat')
    a.bar('rigging port',(-104,-24,25),(0,-122,22),.7,.7,'#D6C5A0','boat')
    a.bar('rigging starboard',(104,-24,25),(0,-122,22),.7,.7,'#D6C5A0','boat')
    # Three scalloped flat wave cut-outs, not simulated water or hundreds of entities.
    for layer in range(3):
        name=f'wave_{layer}';depth=-70+layer*56;a.bone(name,(0,0,depth),'body')
        for step in range(24):
            x=(step-11.5)*18;height=10+8*(.5+.5*math.sin(step*.75+layer))
            a.box('painted rolling wave',(x,-height/2,depth),(19,height,2),['#387D9C','#4BA9B5','#77C3C7'][layer],name)
            a.box('wave foam',(x,-height,depth-1.3),(17,1.8,.8),'#CEEBDB',name,fabric=False)
    a.box('sea horizon',(0,-58,-130),(650,120,2),'#417F9A')
    a.box('sky backdrop',(0,-170,-132),(650,120,2),'#86B9BF')
    for i in range(7):a.box('painted cloud',((i-3)*75,-168+(i%2)*16,-129),(50,12,1),'#EADCC1')
    bones=[]
    for name,n in a.bones.items():
        parent=a.bones[n['parent']]['absolute'] if n['parent'] else (0,0,0)
        bones.append({'name':name,'parent':n['parent'],'pivot':[n['absolute'][i]-parent[i] for i in range(3)]})
    batches={}
    for m in a.meshes:
        b=batches.setdefault((m['bone'],m['color']),{'bone':m['bone'],'part':'','color':m['color'],'vertices':[],'uv':[],'faces':[]});offset=len(b['vertices']);b['vertices']+=m['vertices'];b['uv']+=m.get('uv',[(.00097656,.99609375)]*len(m['vertices']));b['faces'] += [[v+offset for v in f] for f in m['faces']]
    doc={'format':1,'id':'theatre_ocean_set','bones':bones,'meshes':list(batches.values())}
    (ROOT/'src/main/resources/assets/coraline/characters/theatre_ocean_set.json').write_text(json.dumps(doc,separators=(',',':')),encoding='utf8')
    boat=a.objects['boat'];boat.rotation_mode='XYZ'
    for frame in range(1,242,4):boat.rotation_euler.y=math.sin(frame*.055)*.09;boat.keyframe_insert(data_path='rotation_euler',frame=frame)
    for layer in range(3):
        wave=a.objects[f'wave_{layer}'];base=wave.location.copy()
        for frame in range(1,242,4):wave.location.x=base.x+math.sin(frame*.04+layer)*.22;wave.keyframe_insert(data_path='location',frame=frame)
    scene.frame_set(1);bpy.data.libraries.write(str(OUT/'theatre_ocean_set.blend'),{scene},fake_user=True)
    print('theatre_ocean_set',len(a.geometry),'meshes',sum(len(m['vertices']) for m in a.meshes),'vertices')
if __name__=='__main__':
    export_actor(False);export_actor(True);export_stage()
