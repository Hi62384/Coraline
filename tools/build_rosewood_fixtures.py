"""Compact native models plus original 32-pixel storage and brass-key textures."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/coraline'
SIDES=('north','south','east','west','up','down')
def cube(a,b,texture): return {'from':a,'to':b,'faces':{s:{'texture':'#'+texture} for s in SIDES}}
def write(folder,name,data):
    dest=ROOT/folder/(name+'.json');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(data,separators=(',',':'))+'\n',encoding='utf-8')
def model(name,elements):
    write('models/block',name,{'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'particle':'minecraft:block/spruce_planks','wood':'minecraft:block/dark_oak_planks','white':'minecraft:block/quartz_block_side','metal':'minecraft:block/iron_block','silver':'minecraft:block/light_gray_concrete','blue':'minecraft:block/light_blue_concrete','pot':'minecraft:block/terracotta','green':'minecraft:block/moss_block','gold':'minecraft:block/yellow_concrete','glass':'minecraft:block/glass','light':'minecraft:block/glowstone'},'elements':elements})
def variants(name): write('blockstates',name,{'variants':{'facing='+f:{'model':'coraline:block/'+name,'y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)]}})
def build():
    storage_assets()
    model('rosewood_counter',[cube([0,0,0],[16,12,16],'wood'),cube([0,12,0],[16,14,16],'white'),cube([1,1,-.1],[15,11,.3],'pot'),cube([7,7,-.3],[9,8,.1],'metal')]);variants('rosewood_counter')
    model('rosewood_oven',[cube([0,0,0],[16,13,16],'metal'),cube([2,2,-.1],[14,10,.2],'wood'),cube([3,3,-.2],[13,9,.1],'silver'),cube([2,10,-.4],[14,11,0],'metal'),cube([2,13,2],[7,13.3,7],'wood'),cube([9,13,9],[14,13.3,14],'wood')]);variants('rosewood_oven')
    chair=[cube([2,5,2],[14,7,14],'wood'),cube([3,7,3],[13,8,12],'pot'),cube([2,7,12],[4,16,15],'wood'),cube([12,7,12],[14,16,15],'wood'),cube([4,9,13],[12,15,15],'wood')]
    for x in (2,12):
        for z in (2,12): chair.append(cube([x,0,z],[x+2,5,z+2],'wood'))
    model('rosewood_chair',chair);variants('rosewood_chair')
    model('rosewood_sofa',[cube([0,2,0],[16,5,14],'wood'),cube([0,5,0],[16,7,13],'pot'),cube([0,7,12],[16,14,16],'pot'),cube([1,0,1],[3,2,3],'wood'),cube([13,0,12],[15,2,14],'wood')]);variants('rosewood_sofa')
    model('rosewood_coat_rack',[cube([4,0,4],[12,2,12],'wood'),cube([7,2,7],[9,23,9],'wood'),cube([3,18,7],[13,20,9],'wood'),cube([7,17,3],[9,19,13],'wood')]);variants('rosewood_coat_rack')
    model('rosewood_mirror',[cube([2,1,14],[14,15,16],'wood'),cube([3,2,13.8],[13,14,14.05],'silver'),cube([4,10,13.7],[5,13,13.9],'white'),cube([5,8,13.7],[6,10,13.9],'white')]);variants('rosewood_mirror')
    sink=[cube([2,0,3],[14,9,15],'wood'),cube([1,9,2],[15,10,16],'white'),cube([1,10,2],[3,13,16],'white'),cube([13,10,2],[15,13,16],'white'),cube([3,10,2],[13,13,4],'white'),cube([3,10,14],[13,13,16],'white'),cube([3,10,4],[13,10.4,14],'blue'),cube([7,13,14],[9,16,16],'metal'),cube([7,15,11],[9,16,15],'metal')]
    model('rosewood_sink',sink);variants('rosewood_sink')
    toilet=[cube([5,0,4],[11,6,13],'white'),cube([3,6,2],[13,8,14],'white'),cube([3,8,2],[5,10,14],'white'),cube([11,8,2],[13,10,14],'white'),cube([5,8,2],[11,10,4],'white'),cube([5,8,12],[11,10,14],'white'),cube([5,8,4],[11,8.2,12],'blue'),cube([3,8,13],[13,14,16],'white'),cube([11,11,12.7],[12.5,12,13.1],'metal')]
    model('rosewood_toilet',toilet);variants('rosewood_toilet')
    bath=[cube([0,0,0],[16,3,16],'white'),cube([0,3,0],[2,12,16],'white'),cube([14,3,0],[16,12,16],'white'),cube([2,3,0],[14,12,2],'white'),cube([2,5,2],[14,5.3,16],'blue'),cube([0,11,0],[3,13,16],'white'),cube([13,11,0],[16,13,16],'white'),cube([3,11,0],[13,13,2],'white')]
    model('rosewood_bath',bath);variants('rosewood_bath')
    for decor in ('','_vase','_lamp'):
        name='rosewood_table'+decor;parts=[cube([0,10,0],[16,12,16],'wood')]
        if decor=='_vase': parts += [cube([6,12,6],[10,16,10],'pot'),cube([7,16,7],[9,20,9],'green'),cube([6,19,6],[10,21,10],'gold')]
        if decor=='_lamp': parts += [cube([6,12,6],[10,13,10],'metal'),cube([7,13,7],[9,18,9],'metal'),cube([4,18,4],[12,22,12],'light'),cube([4,22,4],[12,23,12],'gold')]
        model(name,parts)
        multipart=[{'apply':{'model':'coraline:block/'+name}}]
        for corner,a,b,edges in [('nw',[1,0,1],[3,10,3],('north','west')),('ne',[13,0,1],[15,10,3],('north','east')),('sw',[1,0,13],[3,10,15],('south','west')),('se',[13,0,13],[15,10,15],('south','east'))]:
            leg='rosewood_table_leg_'+corner;model(leg,[cube(a,b,'wood')]);multipart.append({'when':{e:'false' for e in edges},'apply':{'model':'coraline:block/'+leg}})
        write('blockstates',name,{'multipart':multipart})
    doorstates={}
    for half in ('lower','upper'):
        for hinge in ('left','right'):
            name='rosewood_glass_door_'+half+'_'+hinge
            pieces=[cube([0,0,0],[1.5,16,2],'wood'),cube([14.5,0,0],[16,16,2],'wood'),cube([1.5,0,0],[14.5,1.5,2],'wood'),cube([1.5,14.5,0],[14.5,16,2],'wood'),cube([1.5,1.5,.6],[14.5,14.5,1.3],'glass')]
            if half=='upper': pieces.append(cube([12 if hinge=='left' else 2,3,-.7],[14 if hinge=='left' else 4,4.5,.3],'gold'))
            # Vanilla NORTH doors occupy the back three pixels of the block.
            for piece in pieces:
                piece['from'][2]+=13;piece['to'][2]+=13
            model(name,pieces)
            for facing,y in [('north',0),('east',90),('south',180),('west',270)]:
                for opened in ('false','true'):
                    rotation=(y+(0 if opened=='false' else 90 if hinge=='left' else -90))%360
                    doorstates[f'facing={facing},half={half},hinge={hinge},open={opened}']={'model':'coraline:block/'+name,'y':rotation}
    write('blockstates','rosewood_glass_door',{'variants':doorstates})
    print('Built native low tables, compact mirrors, bathroom fixtures and working glass-door models')
def storage_assets():
    from PIL import Image, ImageDraw
    for name,base in [('rosewood_drawer',(108,51,37)),('rosewood_cabinet',(56,74,67))]:
        image=Image.new('RGB',(32,32),base);draw=ImageDraw.Draw(image)
        for y in range(32):
            for x in range(32):
                shade=((x*17+y*3)%9)-4
                image.putpixel((x,y),tuple(max(0,min(255,c+shade)) for c in base))
        draw.rectangle((1,1,30,30),outline=tuple(c+28 for c in base),width=2)
        draw.rectangle((5,5,26,26),outline=tuple(c-18 for c in base))
        path=ROOT/'textures/block'/f'{name}.png';path.parent.mkdir(parents=True,exist_ok=True);image.save(path)
        shell=[cube([0,0,0],[2,16,16],'wood'),cube([14,0,0],[16,16,16],'wood'),cube([2,0,0],[14,2,16],'wood'),cube([2,14,0],[14,16,16],'wood'),cube([2,2,14],[14,14,16],'wood')]
        for opened in (False,True):
            parts=list(shell)
            if name.endswith('drawer'):
                parts += [cube([2,2,0],[14,6,14],'wood'),cube([6,4,-.5],[10,5,.1],'metal')]
                z=-7 if opened else 0
                parts += [cube([2,7,z],[14,13,z+1.5],'wood'),cube([2,7,z+1.5],[14,8,z+14],'wood'),cube([2,8,z+1.5],[3,12,z+14],'wood'),cube([13,8,z+1.5],[14,12,z+14],'wood'),cube([3,8,z+13],[13,12,z+14],'wood'),cube([6,10,z-.6],[10,11,z],'metal')]
            else:
                parts += [cube([2,7,1],[14,8,14],'wood')]
                if opened:
                    # Folded doors, leaving a real opening and two visible shelves.
                    parts += [cube([0,2,-7],[1.5,14,0],'wood'),cube([14.5,2,-7],[16,14,0],'wood')]
                else:
                    parts += [cube([2,2,0],[7.8,14,1.5],'wood'),cube([8.2,2,0],[14,14,1.5],'wood'),cube([6,7,-.6],[7,10,0],'metal'),cube([9,7,-.6],[10,10,0],'metal')]
            suffix='_open' if opened else '_closed'
            # Extended drawers/doors have coordinates outside 0..16. Implicit UVs
            # would sample neighbouring atlas sprites instead of their own wood.
            for part in parts:
                for face in part['faces'].values():face['uv']=[0,0,16,16]
            write('models/block',name+suffix,{'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'particle':'coraline:block/'+name,'wood':'coraline:block/'+name,'metal':'minecraft:block/gold_block'},'elements':parts})
        write('blockstates',name,{'variants':{f'facing={f},open={str(o).lower()}':{'model':'coraline:block/'+name+('_open' if o else '_closed'),'y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)] for o in (False,True)}})
    icon=Image.new('RGBA',(32,32));d=ImageDraw.Draw(icon)
    d.ellipse((2,2,17,17),fill='#65402b');d.ellipse((3,2,16,15),fill='#dab762');d.ellipse((6,5,13,12),fill=(0,0,0,0));d.line((13,13,27,27),fill='#76512d',width=5);d.line((13,12,28,27),fill='#e5c979',width=3);d.line((23,21,27,17),fill='#e5c979',width=3);d.line((27,25,30,22),fill='#e5c979',width=3);d.point((5,4),fill='#fff0b0')
    path=ROOT/'textures/item/borrowed_house_key.png';path.parent.mkdir(parents=True,exist_ok=True);icon.save(path)
    write('models/item','borrowed_house_key',{'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/borrowed_house_key'}})
if __name__=='__main__': build()
