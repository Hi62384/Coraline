"""Render QA portraits directly from wearable meshes, NEVER inventory icons. Isolated scene only.
Run in background Blender; -- midnight_coat limits a preview to one item.
"""
from pathlib import Path
import bpy,json,sys,math
from mathutils import Vector
ROOT=Path(r'C:/Users/User1/Documents/Downloads/Coraline')
ART=ROOT/'artwork/sculpted_wardrobe'
ASSETS=ROOT/'src/main/resources/assets/coraline'
OUTPUT=ART/'renders';OUTPUT.mkdir(parents=True,exist_ok=True)
selected=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []
inspect_only='--inspect' in selected
sets_only='--sets' in selected
selected=[s for s in selected if s not in ('--inspect','--sets')]
manifest=json.loads((ART/'manifest.json').read_text())
if sets_only:manifest=[{'id':name} for name in selected]
scene=bpy.data.scenes.new('Coraline Sculpted Wardrobe Studio')
if bpy.context.window:bpy.context.window.scene=scene
try:scene.render.engine='BLENDER_EEVEE'
except TypeError:pass
scene.render.resolution_x=scene.render.resolution_y=512
scene.render.resolution_percentage=100;scene.render.film_transparent=True
for prop,value in [('file_format','PNG'),('color_mode','RGBA')]:
    if value in [i.identifier for i in scene.render.image_settings.bl_rna.properties[prop].enum_items]:setattr(scene.render.image_settings,prop,value)
scene.world=bpy.data.worlds.new('Costume studio atmosphere');scene.world.use_nodes=True
bg=next(n for n in scene.world.node_tree.nodes if n.type=='BACKGROUND')
bg.inputs['Color'].default_value=(.38,.43,.55,1);bg.inputs['Strength'].default_value=.45
for name,pos,power,size in [('Key',(-4,-5,6),850,4),('Fill',(4,-3,2),550,4),('Rim',(2,4,3),1100,3)]:
    light=bpy.data.lights.new('Wardrobe '+name,'AREA');light.energy=power;light.size=size
    obj=bpy.data.objects.new(light.name,light);scene.collection.objects.link(obj);obj.location=pos;obj.rotation_euler=(Vector((0,0,-.5))-obj.location).to_track_quat('-Z','Y').to_euler()
cam=bpy.data.cameras.new('Costume portrait');camera=bpy.data.objects.new(cam.name,cam);scene.collection.objects.link(camera);scene.camera=camera
if 'ORTHO' in [i.identifier for i in cam.bl_rna.properties['type'].enum_items]:cam.type='ORTHO'
cloth=bpy.data.images.load(str(ASSETS/'textures/models/sculpted/fabric.png'),check_existing=True)
bones={'head':(0,0,0),'body':(0,0,0),'cape':(0,0,0),'left_arm':(5,2,0),'right_arm':(-5,2,0),'left_leg':(1.9,12,0),'right_leg':(-1.9,12,0)}
for item in manifest:
    if selected and item['id'] not in selected:continue
    if sets_only:
        parts=[json.loads((ASSETS/'sculpted'/(item['id']+'_'+piece+'.json')).read_text()) for piece in ('helmet','chestplate','leggings','boots')]
        data=dict(parts[0]);data['meshes']=[mesh for part in parts for mesh in part['meshes']]
    else:data=json.loads((ASSETS/'sculpted'/(item['id']+'.json')).read_text())
    atlas=bpy.data.images.load(str(ASSETS/'textures/models/sculpted'/(data.get('texture','fabric')+'.png')),check_existing=True)
    materials={};objects=[]
    for name,color in data['materials'].items():
        mat=bpy.data.materials.new(item['id']+' '+name);mat.use_nodes=True
        rgb=[int(color[i:i+2],16)/255 for i in (1,3,5)];linear=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in rgb]
        mat.diffuse_color=(*rgb,1);shader=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
        shader.inputs['Base Color'].default_value=(*linear,1)
        tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=atlas
        mix=mat.node_tree.nodes.new('ShaderNodeMixRGB')
        if 'MULTIPLY' in [i.identifier for i in mix.bl_rna.properties['blend_type'].enum_items]:mix.blend_type='MULTIPLY'
        mix.inputs[0].default_value=1;mix.inputs[2].default_value=(*linear,1)
        mat.node_tree.links.new(tex.outputs['Color'],mix.inputs[1]);mat.node_tree.links.new(mix.outputs['Color'],shader.inputs['Base Color'])
        shader.inputs['Roughness'].default_value=.79 if name=='velvet' else .36 if name=='lining' else .44
        if name in ('silver','brass'):shader.inputs['Metallic'].default_value=.6
        if name in ('velvet','lining'):
            shader.inputs['Sheen Weight'].default_value=.12
            tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=cloth
            bump=mat.node_tree.nodes.new('ShaderNodeBump');bump.inputs['Strength'].default_value=.27;bump.inputs['Distance'].default_value=.009
            mat.node_tree.links.new(tex.outputs['Color'],bump.inputs['Height']);mat.node_tree.links.new(bump.outputs['Normal'],shader.inputs['Normal'])
        materials[name]=mat
    # Merge by bone/material. Hundreds of seams should not be hundreds of Blender objects.
    groups={}
    for part in data['meshes']:
        key=(part['bone'],part['material']);group=groups.setdefault(key,dict(verts=[],faces=[],uv=[]));base=len(group['verts'])
        origin=bones[part['bone']]
        group['verts'] += [((p[0]+origin[0])/8,(p[2]+origin[2])/8,-(p[1]+origin[1])/8) for p in part['vertices']]
        group['faces'] += [[base+i for i in f] for f in part['faces']];group['uv']+=part['uv']
    for (bone,material),group in groups.items():
        mesh=bpy.data.meshes.new(item['id']+' '+bone+' '+material);mesh.from_pydata(group['verts'],[],group['faces']);mesh.update()
        obj=bpy.data.objects.new(mesh.name,mesh);scene.collection.objects.link(obj);obj['minecraft_bone']=bone;objects.append(obj)
        mesh.materials.append(materials[material])
        for face in mesh.polygons:face.use_smooth=True
        uv=mesh.uv_layers.new(name='UVMap')
        for face in mesh.polygons:
            for loop in face.loop_indices:
                u,v=group['uv'][mesh.loops[loop].vertex_index];uv.data[loop].uv=(u,1-v)
    corners=[Vector(v) for obj in objects for v in obj.bound_box]
    low=Vector(tuple(min(v[i] for v in corners) for i in range(3)));high=Vector(tuple(max(v[i] for v in corners) for i in range(3)));center=(low+high)/2
    camera.location=center+Vector((2.5,-9,3.0));camera.rotation_euler=(center-camera.location).to_track_quat('-Z','Y').to_euler()
    cam.ortho_scale=max(high.x-low.x,high.z-low.z,high.y-low.y)*1.27
    scene.render.filepath=str(OUTPUT/(item['id']+'.png'))
    if not inspect_only:bpy.ops.render.render(write_still=True,scene=scene.name)
    if selected:
        if bpy.context.screen:
            for area in bpy.context.screen.areas:
                if area.type=='VIEW_3D':
                    area.spaces.active.region_3d.view_location=center;area.spaces.active.region_3d.view_distance=cam.ortho_scale*1.3
                    area.spaces.active.region_3d.view_rotation=camera.rotation_euler.to_quaternion()
        bpy.ops.wm.save_as_mainfile(filepath=str(ART/(item['id']+'_sculpted.blend')),copy=True)
    if not inspect_only:
        for obj in objects:
            mesh=obj.data;bpy.data.objects.remove(obj,do_unlink=True);bpy.data.meshes.remove(mesh)
        for mat in materials.values():bpy.data.materials.remove(mat)
    print('SCULPTED_ICON_DONE',item['id'],flush=True)
