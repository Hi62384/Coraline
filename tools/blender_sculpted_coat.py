"""Import the curved coat into an isolated Blender scene for visual review.
Leaves the user's other scenes, assets, and old wardrobe untouched.
"""
from pathlib import Path
import bpy,json,math
from mathutils import Vector
ROOT=Path(r'C:/Users/User1/Documents/Downloads/Coraline')
ART=ROOT/'artwork/sculpted_wardrobe'
data=json.loads((ART/'midnight_coat_mesh.json').read_text())
scene=bpy.data.scenes.new('Coraline Sculpted Midnight Coat')
if bpy.context.window:bpy.context.window.scene=scene
try:scene.render.engine='BLENDER_EEVEE'
except TypeError:pass
scene.render.resolution_x=scene.render.resolution_y=1024
scene.render.resolution_percentage=100;scene.render.film_transparent=True
for prop,value in [('file_format','PNG'),('color_mode','RGBA')]:
    if value in [i.identifier for i in scene.render.image_settings.bl_rna.properties[prop].enum_items]:setattr(scene.render.image_settings,prop,value)
materials={}
for name,color in data['materials'].items():
    mat=bpy.data.materials.new('Sculpted Coat '+name);mat.use_nodes=True
    rgb=[int(color[i:i+2],16)/255 for i in (1,3,5)];linear=[v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in rgb]
    mat.diffuse_color=(*rgb,1);shader=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
    shader.inputs['Base Color'].default_value=(*linear,1)
    shader.inputs['Roughness'].default_value=.78 if name=='velvet' else .32 if name=='lining' else .43
    if name in ('silver','brass'):shader.inputs['Metallic'].default_value=.65
    if name in ('velvet','lining'):
        if 'Sheen Weight' in shader.inputs:shader.inputs['Sheen Weight'].default_value=.4
        noise=mat.node_tree.nodes.new('ShaderNodeTexNoise');noise.inputs['Scale'].default_value=190;noise.inputs['Detail'].default_value=2
        bump=mat.node_tree.nodes.new('ShaderNodeBump');bump.inputs['Strength'].default_value=.19;bump.inputs['Distance'].default_value=.012
        mat.node_tree.links.new(noise.outputs['Fac'],bump.inputs['Height']);mat.node_tree.links.new(bump.outputs['Normal'],shader.inputs['Normal'])
    materials[name]=mat
bones={'body':(0,0,0),'left_arm':(5.5,2,0),'right_arm':(-5.5,2,0),'left_leg':(1.9,12,0),'right_leg':(-1.9,12,0)}
for part in data['meshes']:
    origin=bones[part['bone']]
    verts=[((p[0]+origin[0])/8,(p[2]+origin[2])/8,-(p[1]+origin[1])/8) for p in part['vertices']]
    mesh=bpy.data.meshes.new(part['name']);mesh.from_pydata(verts,[],part['faces']);mesh.update()
    obj=bpy.data.objects.new(part['name'],mesh);scene.collection.objects.link(obj);obj['minecraft_bone']=part['bone']
    mesh.materials.append(materials[part['material']])
    for face in mesh.polygons:face.use_smooth=True
    uv=mesh.uv_layers.new(name='UVMap')
    for face in mesh.polygons:
        for loop in face.loop_indices:uv.data[loop].uv=part['uv'][mesh.loops[loop].vertex_index]
scene.world=bpy.data.worlds.new('Sculpted Coat Studio');scene.world.use_nodes=True
bg=next(n for n in scene.world.node_tree.nodes if n.type=='BACKGROUND');bg.inputs[0].default_value=(.25,.3,.4,1);bg.inputs[1].default_value=.5
center=Vector((0,0,-1.1))
for name,pos,power,size in [('Key',(-4,-5,5),700,4),('Fill',(5,-4,1),450,4),('Rim',(2,3,4),1100,3)]:
    light=bpy.data.lights.new('Sculpted '+name,'AREA');light.energy=power;light.size=size
    obj=bpy.data.objects.new(light.name,light);scene.collection.objects.link(obj);obj.location=pos;obj.rotation_euler=(center-obj.location).to_track_quat('-Z','Y').to_euler()
cam=bpy.data.cameras.new('Sculpted Coat Camera');obj=bpy.data.objects.new(cam.name,cam);scene.collection.objects.link(obj);scene.camera=obj
if 'ORTHO' in [i.identifier for i in cam.bl_rna.properties['type'].enum_items]:cam.type='ORTHO'
cam.ortho_scale=3.5;obj.location=(3,-8,2);obj.rotation_euler=(center-obj.location).to_track_quat('-Z','Y').to_euler()
if bpy.context.screen:
    for area in bpy.context.screen.areas:
        if area.type=='VIEW_3D':
            area.spaces.active.region_3d.view_location=center;area.spaces.active.region_3d.view_distance=4
            area.spaces.active.region_3d.view_rotation=obj.rotation_euler.to_quaternion()
            area.spaces.active.shading.color_type='MATERIAL'
scene.render.filepath=str(ART/'midnight_coat_model_preview.png')
bpy.ops.wm.save_as_mainfile(filepath=str(ART/'midnight_coat_prototype.blend'),copy=True)
print('Curved coat prototype imported with',len(data['meshes']),'mesh details. Original scenes preserved.')
