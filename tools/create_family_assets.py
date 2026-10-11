"""Native Minecraft skin UV layouts and block model/texture assets."""
from pathlib import Path
from PIL import Image, ImageDraw
import random,json
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/coraline'
D=ROOT/'src/main/resources/data/coraline'
def save_json(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2)+'\n')
def png(name,im):
    path=A/'textures'/name;path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
# Eyes are filled dark buttons, mounted on the actual adult-model face, not white rings.
im=Image.new('RGBA',(32,16));d=ImageDraw.Draw(im)
for cx in (8,23):
    d.ellipse((cx-6,3,cx+6,11),fill=(12,11,17,255))
    d.point((cx-1,6),fill=(129,121,105,255));d.point((cx+1,8),fill=(129,121,105,255))
    d.point((cx+1,6),fill=(129,121,105,255));d.point((cx-1,8),fill=(129,121,105,255))
png('entity/doll_button_eyes.png',im)
# Keep the shared asset regeneration command on the current parent wardrobe.
from create_parent_skins import skin as detailed_parent_skin
for mom in (True,False):
    for i in range(32):detailed_parent_skin(mom,i)
# Decorative door: warm painted ivory panels, oak trim, brass hardware, brick backing.
im=Image.new('RGBA',(16,16),(181,158,119,255));d=ImageDraw.Draw(im)
d.rectangle((0,0,15,15),outline=(83,61,40),width=1)
d.line((1,1,14,1),fill=(235,220,183));d.line((1,1,1,14),fill=(221,202,159))
for box in [(3,3,6,7),(9,3,12,7),(3,10,12,13)]:d.rectangle(box,fill=(198,177,138),outline=(129,103,71))
d.rectangle((12,8,13,10),fill=(190,139,55));d.point((12,8),fill=(247,218,123));d.point((12,10),fill=(34,25,26))
png('block/tiny_door.png',im)
# Flat inventory/held sprite: the door's front, never the brick back of a block model.
icon=Image.new('RGBA',(16,16));icon.paste(im.resize((12,14),Image.Resampling.NEAREST),(2,1))
png('item/tiny_door.png',icon)
im=Image.new('RGBA',(16,16),(113,81,49,255));d=ImageDraw.Draw(im)
for x in range(0,16,4):
    d.line((x,0,x,15),fill=(83,57,34));d.line((x+1,0,x+1,15),fill=(150,114,71))
png('block/tiny_door_frame.png',im)
im=Image.new('RGBA',(16,16),(103,86,83,255));d=ImageDraw.Draw(im)
for row in range(4):
    offset=4*(row%2)
    for x in range(-8,17,8):
        xx=x+offset;d.rectangle((xx,row*4,xx+6,row*4+2),fill=(132+row*5,63+row*2,53))
        d.line((xx,row*4,xx+6,row*4),fill=(174,97,75))
png('block/tiny_bricks.png',im)
im=Image.new('RGBA',(16,16),(119,85,50,255));d=ImageDraw.Draw(im)
for x in range(0,16,4):d.line((x,0,x,15),fill=(88,60,35));d.line((x+1,0,x+1,15),fill=(147,109,65))
png('block/bed_wood.png',im)
im=Image.new('RGBA',(16,16),(136,54,43,255));d=ImageDraw.Draw(im)
for y in (3,11):d.line((0,y,15,y),fill=(154,66,51))
for x in (3,11):d.line((x,0,x,15),fill=(154,66,51))
png('block/bed_cloth.png',im)
im=Image.new('RGBA',(16,16),(228,215,183,255));d=ImageDraw.Draw(im)
d.rectangle((0,0,15,15),outline=(193,177,145));png('block/bed_pillow.png',im)
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.rounded_rectangle((1,1,14,12),radius=2,fill=(242,224,172),outline=(88,64,33))
d.polygon([(5,12),(5,15),(9,12)],fill=(242,224,172))
d.line([(4,6),(6,8),(11,3)],fill=(49,119,56),width=2);png('block/parents_available.png',im)
def cuboid(start,end,texture,front=None):
    return {'from':start,'to':end,'faces':{f:{'texture':'#'+(front if f=='north' and front else texture),'uv':[0,0,16,16]} for f in ['north','south','east','west','up','down']}}
def door_box(start,end,texture):
    # UV extents follow actual face dimensions: narrow edges sample narrow strips,
    # rather than squeezing an entire brick wall onto a two-pixel-wide surface.
    x,y,z=start;X,Y,Z=end
    uv={'north':[16-X,16-Y,16-x,16-y],'south':[x,16-Y,X,16-y],
        'west':[z,16-Y,Z,16-y],'east':[16-Z,16-Y,16-z,16-y],
        'up':[x,z,X,Z],'down':[x,16-Z,X,16-z]}
    return {'from':start,'to':end,'faces':{f:{'texture':'#'+texture,'uv':v} for f,v in uv.items()}}
textures={'wood':'coraline:block/tiny_door_frame','door':'coraline:block/tiny_door','bricks':'coraline:block/tiny_bricks','particle':'coraline:block/tiny_door_frame'}
frame=[door_box([0,0,14],[16,16,16],'bricks'),door_box([0,0,11],[1,16,14],'wood'),door_box([15,0,11],[16,16,14],'wood'),door_box([1,15,11],[15,16,14],'wood')]
for opened in (False,True):
    panel=door_box([1,0,0],[3,15,14],'wood') if opened else door_box([1,0,12],[15,15,14],'wood')
    for face in (('west','east') if opened else ('north','south')):
        panel['faces'][face]={'texture':'#door','uv':[0,0,16,16]}
    save_json(A/f'models/block/tiny_door_{"open" if opened else "closed"}.json',{'parent':'minecraft:block/block','textures':textures,'elements':frame+[panel]})
save_json(A/'blockstates/tiny_door.json',{'variants':{f'facing={f},open={str(o).lower()}':{'model':f'coraline:block/tiny_door_{"open" if o else "closed"}','y':angle} for f,angle in [('north',0),('east',90),('south',180),('west',270)] for o in (False,True)}})
save_json(A/'models/item/tiny_door.json',{'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/tiny_door'}})
bed_textures={'wood':'coraline:block/bed_wood','cloth':'coraline:block/bed_cloth','pillow':'coraline:block/bed_pillow','metal':'minecraft:block/iron_block','particle':'coraline:block/bed_wood'}
bed_models=[]
for part in range(4):
    head=part>=2;right=part%2==1
    parts=[door_box([0,3,0 if head else 5],[16,6,14 if head else 16],'wood')]
    parts.append(door_box([0,6,0 if head else 5],[16,10,14 if head else 16],'cloth'))
    # Oak storage trunk spans the foot of the double bed, with its own 27-slot inventory.
    if not head:
        parts += [door_box([0,3,0],[16,8,5],'wood'),door_box([0,8,0],[16,10,5],'wood'),door_box([7,5,-.05],[9,8,0],'metal')]
    else:
        parts += [door_box([0,6,14],[16,14,16],'wood'),door_box([2,10,8],[14,12,13],'pillow')]
    x=14 if right else 0
    parts.append(door_box([x,0,13 if head else 0],[x+2,3,16 if head else 3],'wood'))
    bed_models.append(parts)
    save_json(A/f'models/block/parents_bed_{part}.json',{'parent':'minecraft:block/block','textures':bed_textures,'elements':parts})
marker={'from':[4,14,13.9],'to':[12,22,13.9],'faces':{face:{'texture':'#icon','uv':[0,0,16,16]} for face in ('north','south')}}
save_json(A/'models/block/parents_bed_available.json',{'textures':{'icon':'coraline:block/parents_available'},'elements':[marker]})
multipart=[]
for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
    for part in range(4):multipart.append({'when':{'facing':facing,'part':str(part)},'apply':{'model':f'coraline:block/parents_bed_{part}','y':angle}})
    multipart.append({'when':{'facing':facing,'part':'2','available':'true'},'apply':{'model':'coraline:block/parents_bed_available','y':angle}})
save_json(A/'blockstates/parents_bed.json',{'multipart':multipart})
# One compact 3D inventory preview of the entire double bed; all faces keep their UVs.
preview=[]
for part,elements in enumerate(bed_models):
    for original in elements:
        element=json.loads(json.dumps(original))
        for key in ('from','to'):
            x,y,z=element[key];element[key]=[(x+16*(part%2))/2,y/2+3,(z+16*(part//2))/2]
        preview.append(element)
save_json(A/'models/block/parents_bed.json',{'parent':'minecraft:block/block','textures':bed_textures,'elements':preview})
save_json(A/'models/item/parents_bed.json',{'parent':'coraline:block/parents_bed'})
save_json(A/'models/item/dads_slime.json',{'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/dads_slime'}})
save_json(D/'recipes/tiny_door.json',{'type':'minecraft:crafting_shaped','pattern':['PP','PN','BB'],'key':{'P':{'tag':'minecraft:planks'},'N':{'item':'minecraft:iron_nugget'},'B':{'item':'minecraft:brick'}},'result':{'item':'coraline:tiny_door'}})
save_json(D/'loot_tables/blocks/tiny_door.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'coraline:tiny_door'}]}]})
lang=json.loads((A/'lang/en_us.json').read_text());lang.update({'block.coraline.parents_bed':"Parents Bed",'block.coraline.tiny_door':'Tiny Door','item.coraline.dads_slime':"Dad's Slime",'entity.coraline.mom':'Mom','entity.coraline.dad':'Dad','subtitles.coraline.angry_parent':'An angry parent speaks','subtitles.coraline.mom_chat':'Mom speaks','subtitles.coraline.dad_chat':'Dad speaks'});save_json(A/'lang/en_us.json',lang)
print('Created family skins, button overlay, bed and door assets, recipe, and item models.')
