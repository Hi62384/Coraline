"""Editable Blender preview of the Minecraft cuboid rider and bicycle dimensions."""
import bpy, math
from mathutils import Vector
from pathlib import Path
root=Path(__file__).resolve().parents[1]
bpy.ops.object.select_all(action='SELECT')
bpy.ops.object.delete(use_global=False)
skin=bpy.data.materials.new('Wybie Minecraft skin'); skin.use_nodes=True
tex=skin.node_tree.nodes.new('ShaderNodeTexImage')
tex.image=bpy.data.images.load(str(root/'src/main/resources/assets/coraline/textures/entity/wybie_skin.png'))
tex.interpolation='Closest'
bs=skin.node_tree.nodes.get('Principled BSDF')
skin.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
skin.node_tree.links.new(tex.outputs['Alpha'],bs.inputs['Alpha'])
bs.inputs['Roughness'].default_value=.9
def material(name,color):
    m=bpy.data.materials.new(name);m.use_nodes=True
    shader=m.node_tree.nodes.get('Principled BSDF')
    shader.inputs['Base Color'].default_value=(*color,1)
    shader.inputs['Roughness'].default_value=.7
    return m
rubber=material('Tires and saddle',(.025,.03,.04))
metal=material('Steel frame',(.37,.44,.48))
def cube(name,origin,size,pivot=(0,0,0),rx=0,uv=None,mat=skin):
    x,y,z=origin; w,h,d=size
    verts=[(x,y,z),(x+w,y,z),(x+w,y+h,z),(x,y+h,z),(x,y,z+d),(x+w,y,z+d),(x+w,y+h,z+d),(x,y+h,z+d)]
    def transform(v):
        a,b,c=v; b,c=b*math.cos(rx)-c*math.sin(rx),b*math.sin(rx)+c*math.cos(rx)
        return ((a+pivot[0])/16,(c+pivot[2])/16,(24-b-pivot[1])/16)
    faces=[(1,0,3,2),(4,5,6,7),(0,4,7,3),(5,1,2,6),(0,1,5,4),(3,7,6,2)]
    mesh=bpy.data.meshes.new(name);mesh.from_pydata([transform(v) for v in verts],[],faces);mesh.update()
    obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj);obj.data.materials.append(mat)
    if uv is not None:
        u,v=uv
        boxes=[(u+d,v+d,w,h),(u+2*d+w,v+d,w,h),(u,v+d,d,h),(u+d+w,v+d,d,h),(u+d,v,w,d),(u+d+w,v,w,d)]
        layer=mesh.uv_layers.new()
        for face,box in zip(mesh.polygons,boxes):
            a,b,sw,sh=box
            coords=[(a+sw,b),(a,b),(a,b+sh),(a+sw,b+sh)]
            for li,(tu,tv) in zip(face.loop_indices,coords):layer.data[li].uv=(tu/64,1-tv/64)
    return obj
cube('Head',(-4,-8,-4),(8,8,8),(0,-5,0),uv=(0,0))
for x in [-4,-1,2]:
    for z in [-4,-1,2]:cube('Hair curl',(x,-9.5-(.5 if (x+z)%2==0 else 0),z),(3,2,3),(0,-5,0),uv=(0,0))
cube('Curly hair outer layer',(-4.5,-8.5,-4.5),(9,9,9),(0,-5,0),uv=(32,0))
cube('Jacket',(-4,0,-2),(8,12,4),(0,-5,0),uv=(16,16))
for side in [-1,1]:
    cube('Arm '+str(side),(-2,-2,-2),(4,12,4),(side*5,-3,0),-.9,uv=(40,16) if side<0 else (32,48))
    cube('Thigh '+str(side),(-2,0,-2),(4,6,4),(side*1.9,7,0),-1,uv=(0,16) if side<0 else (16,48))
    cube('Shin '+str(side),(-2,0,-2),(4,10,4),(side*1.9,7+6*math.cos(-1),6*math.sin(-1)),.5,uv=(0,18) if side<0 else (16,50))
def bar(name,x,y,z,rx,w,h,d,color=1):
    cube(name,(-w/2,-h/2,-d/2),(w,h,d),(x,y,z),rx,mat=metal if color else rubber)
bar('Top tube',0,11,-1,0,1.5,1.5,14)
bar('Seat tube',0,12,5,-.3,1.5,10,1.5)
bar('Down tube',0,15,-1,-.75,1.5,1.5,17)
bar('Chain stay',0,20,7,0,1.5,1.5,12)
bar('Rear stay',0,15,9,.8,1.5,13,1.5)
for side in [-1,1]:bar('Fork',side*1.8,13,-10,-.15,1,15,1)
bar('Handlebars',0,5,-9,0,13,1.5,1.5)
bar('Saddle',0,8,4,0,6,1.5,6,0)
bar('Crank',0,19,1,0,8,1,1)
for side in [-1,1]:bar('Pedal',side*5,19,1,0,3,1,3,0)
for z in [-11,13]:
    for i in range(16):
        a=i*math.pi/8
        bar('Tire segment',0,18+math.cos(a)*5,z+math.sin(a)*5,a,2,1.4,2.2,0)
    for i in range(6):bar('Spoke',0,18,z,i*math.pi/6,.5,10,.5)
    bar('Hub',0,18,z,0,4,1.5,1.5)
bpy.ops.object.camera_add(location=(3,-5,3))
cam=bpy.context.object;cam.rotation_euler=(Vector((0,0,1.15))-cam.location).to_track_quat('-Z','Y').to_euler()
cam.data.type='ORTHO';cam.data.ortho_scale=3.1;bpy.context.scene.camera=cam
for pos,power,size in [((3,-4,6),700,5),((-3,-1,3),400,4)]:
    bpy.ops.object.light_add(type='AREA',location=pos);bpy.context.object.data.energy=power;bpy.context.object.data.shape='DISK';bpy.context.object.data.size=size
    bpy.context.object.rotation_euler=(Vector((0,0,1))-bpy.context.object.location).to_track_quat('-Z','Y').to_euler()
scene=bpy.context.scene;scene.render.engine='BLENDER_EEVEE';scene.render.resolution_x=800;scene.render.resolution_y=900;scene.render.resolution_percentage=100
scene.render.film_transparent=True;scene.view_settings.view_transform='Standard'
tex.image.pack()
scene.render.filepath=str(root/'artwork/wybie_3d_preview.png')
bpy.ops.wm.save_as_mainfile(filepath=str(root/'artwork/wybie_3d.blend'))
bpy.ops.render.render(write_still=True)
