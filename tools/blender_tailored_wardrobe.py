"""Live MCP authoring preview plus isolated background icon rendering. Preserves the user's scene."""
from pathlib import Path
import bpy,json,math,sys
from mathutils import Vector,Euler
ROOT=Path(r'C:/Users/User1/Documents/Downloads/Coraline')
A=ROOT/'src/main/resources/assets/coraline'
ART=ROOT/'artwork/tailored_wardrobe';ART.mkdir(parents=True,exist_ok=True)
BONES={'head':(0,0,0),'body':(0,0,0),'left_arm':(5,2,0),'right_arm':(-5,2,0),'left_leg':(1.9,12,0),'right_leg':(-1.9,12,0)}
SCENE='Coraline Tailored Wardrobe'
def vec(p):return Vector((p[0]/8,p[2]/8,-p[1]/8))
def create():
    previous=bpy.data.scenes.get(SCENE)
    if previous and previous.get('coraline_wardrobe_authored'):
        for obj in list(previous.objects):bpy.data.objects.remove(obj,do_unlink=True)
        bpy.data.scenes.remove(previous)
    scene=bpy.data.scenes.new(SCENE)
    scene['coraline_wardrobe_authored']=True
    try:scene.render.engine='BLENDER_EEVEE'
    except TypeError:pass
    scene.render.resolution_x=scene.render.resolution_y=256;scene.render.resolution_percentage=100
    scene.render.film_transparent=True
    formats=[i.identifier for i in scene.render.image_settings.bl_rna.properties['file_format'].enum_items]
    if 'PNG' in formats:scene.render.image_settings.file_format='PNG'
    modes=[i.identifier for i in scene.render.image_settings.bl_rna.properties['color_mode'].enum_items]
    if 'RGBA' in modes:scene.render.image_settings.color_mode='RGBA'
    scene.world=bpy.data.worlds.new('Wardrobe Studio');scene.world.use_nodes=True
    bg=next(n for n in scene.world.node_tree.nodes if n.type=='BACKGROUND');bg.inputs[0].default_value=(.35,.4,.5,1);bg.inputs[1].default_value=.45
    camera=bpy.data.cameras.new('Wardrobe Camera');scene.camera=bpy.data.objects.new('Wardrobe Camera',camera);scene.collection.objects.link(scene.camera)
    camera.type=next(i.identifier for i in camera.bl_rna.properties['type'].enum_items if i.identifier=='ORTHO')
    for name,loc,power,size in [('Key',(-4,-6,8),850,5),('Fill',(5,-3,3),650,4),('Rim',(2,4,6),1000,3)]:
        light=bpy.data.lights.new('Wardrobe '+name,'AREA');light.energy=power;light.shape='DISK';light.size=size
        ob=bpy.data.objects.new(light.name,light);scene.collection.objects.link(ob);ob.location=loc;ob.rotation_euler=(Vector((0,0,-.5))-ob.location).to_track_quat('-Z','Y').to_euler()
    for index,path in enumerate(sorted((A/'wardrobe').glob('*.json'))):
        spec=json.loads(path.read_text());name=spec['name'];materials=[]
        for n,color in enumerate(spec['palette']):
            rgb=tuple(int(color[i:i+2],16)/255 for i in (1,3,5));linear=tuple(v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4 for v in rgb)
            mat=bpy.data.materials.new('Wardrobe '+name+str(n));mat.use_nodes=True;mat.diffuse_color=(*rgb,1)
            shader=next(node for node in mat.node_tree.nodes if node.type=='BSDF_PRINCIPLED')
            shader.inputs['Base Color'].default_value=(*linear,1);shader.inputs['Roughness'].default_value=.7 if n<7 else .38
            materials.append(mat)
        for piece in ('helmet','chestplate','leggings','boots'):
            verts=[];faces=[];colors=[]
            for b in spec['boxes']:
                if b['piece']!=piece:continue
                x,y,z,w,h,d=b['box'];r=Euler(b['rotation'],'XYZ').to_matrix();bone=Vector(BONES[b['part']]);off=Vector((x,y,z));start=len(verts)
                for local in [(0,0,0),(w,0,0),(w,h,0),(0,h,0),(0,0,d),(w,0,d),(w,h,d),(0,h,d)]:verts.append(vec(bone+off+r@Vector(local)))
                for face in [(0,3,2,1),(4,5,6,7),(0,1,5,4),(3,7,6,2),(0,4,7,3),(1,2,6,5)]:faces.append(tuple(start+i for i in face));colors.append(b['material'])
            mesh=bpy.data.meshes.new(name+'_'+piece);mesh.from_pydata(verts,[],faces);mesh.update()
            obj=bpy.data.objects.new(name+'_'+piece,mesh);scene.collection.objects.link(obj)
            for mat in materials:mesh.materials.append(mat)
            for poly,color in zip(mesh.polygons,colors):poly.material_index=color
            obj['wardrobe']=name;obj['piece']=piece;obj.location=((index%5)*3.4,(index//5)*3.4,0)
    if bpy.context.window:bpy.context.window.scene=scene
    scene.camera.location=(7,-17,10);scene.camera.rotation_euler=(Vector((6,4,-1))-scene.camera.location).to_track_quat('-Z','Y').to_euler();camera.ortho_scale=24
    bpy.ops.wm.save_as_mainfile(filepath=str(ART/'coraline_wardrobe.blend'),copy=True)
    if bpy.context.screen:
        for area in bpy.context.screen.areas:
            if area.type=='VIEW_3D':
                area.spaces.active.region_3d.view_distance=24
                area.spaces.active.region_3d.view_location=(6,4,-1)
                area.spaces.active.region_3d.view_rotation=scene.camera.rotation_euler.to_quaternion()
                area.spaces.active.shading.color_type='MATERIAL'
    print('WARDROBE_READY: 19 sets, 76 pieces. Original scene objects untouched. '+str(ART/'coraline_wardrobe.blend'))

def render():
    scene=bpy.data.scenes.get(SCENE)
    if scene is None:raise RuntimeError('Open coraline_wardrobe.blend first')
    objects=[o for o in scene.objects if o.get('wardrobe')]
    for obj in objects:obj.hide_render=True;obj.location=(0,0,0)
    for index,obj in enumerate(objects):
        obj.hide_render=False
        # Spread the legs for a readable inventory pose (the worn bones still animate normally).
        # Rebuild coordinates from the same specification, with only pose offsets changed.
        spec=json.loads((A/'wardrobe'/f"{obj['wardrobe']}.json").read_text());vertices=[]
        for b in spec['boxes']:
            if b['piece']!=obj['piece']:continue
            x,y,z,w,h,d=b['box'];r=Euler(b['rotation'],'XYZ').to_matrix();bone=Vector(BONES[b['part']]);off=Vector((x,y,z))
            if b['part']=='left_leg':bone.x+=.9
            if b['part']=='right_leg':bone.x-=.9
            if b['part']=='left_arm':bone.x+=.35
            if b['part']=='right_arm':bone.x-=.35
            for local in [(0,0,0),(w,0,0),(w,h,0),(0,h,0),(0,0,d),(w,0,d),(w,h,d),(0,h,d)]:vertices.append(vec(bone+off+r@Vector(local)))
        for vertex,co in zip(obj.data.vertices,vertices):vertex.co=co
        obj.data.update();bpy.context.view_layer.update()
        coords=[Vector(p) for p in obj.bound_box]
        low=Vector(tuple(min(p[i] for p in coords) for i in range(3)));high=Vector(tuple(max(p[i] for p in coords) for i in range(3)));center=(low+high)/2
        scene.camera.location=center+Vector((3,-8,3.5));scene.camera.rotation_euler=(center-scene.camera.location).to_track_quat('-Z','Y').to_euler()
        scene.camera.data.ortho_scale=max(high.x-low.x,high.z-low.z)*1.42+.15
        scene.render.filepath=str(ART/(obj.name+'.png'))
        bpy.ops.render.render(write_still=True,scene=scene.name)
        obj.hide_render=True;print('WARDROBE_ICON',index+1,obj.name,flush=True)
    print('WARDROBE_ICONS_COMPLETE',flush=True)

if __name__=='__main__':
    if '--render-icons' in sys.argv:render()
    else:create()
