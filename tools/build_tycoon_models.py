"""Deterministic code-native block/item models; vanilla materials, no raster masters."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/coraline'
def write(path,data):
    target=ROOT/path;target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(data,separators=(',',':'))+'\n',encoding='utf8')
def box(a,b,texture):
    return {'from':a,'to':b,'faces':{side:{'texture':'#'+texture} for side in ('north','south','east','west','up','down')}}
def model(elements):
    textures={'wood':'minecraft:block/dark_oak_planks','pale':'minecraft:block/quartz_block_side','metal':'minecraft:block/iron_block','violet':'minecraft:block/purple_concrete','gold':'minecraft:block/gold_block','paper':'minecraft:block/white_wool','cloth':'minecraft:block/light_blue_wool','dark':'minecraft:block/black_concrete','water':'minecraft:block/light_blue_stained_glass','green':'minecraft:block/azalea_leaves','red':'minecraft:block/red_wool'}
    return {'textures':{'particle':textures['wood'],**textures},'elements':elements}
def block(id,elements):
    write(Path('models/block')/(id+'.json'),model(elements))
    # Broken props are visibly tilted; bounded cosmetic damage, not world destruction.
    broken=model([box([2,0,2],[14,2,14],'red'),*elements]);write(Path('models/block')/(id+'_broken.json'),broken)
    variants={}
    for facing,yaw in [('north',0),('east',90),('south',180),('west',270)]:
        for lit in ('false','true'):
            for damage in ('false','true'):
                variants[f'facing={facing},lit={lit},broken={damage}']={'model':'coraline:block/'+id+('_broken' if damage=='true' else ''),'y':yaw}
    write(Path('blockstates')/(id+'.json'),{'variants':variants});write(Path('models/item')/(id+'.json'),{'parent':'coraline:block/'+id})
def item(id,elements):
    m=model(elements);m['display']={'gui':{'rotation':[25,35,0],'scale':[.8,.8,.8]},'ground':{'scale':[.4,.4,.4]},'thirdperson_righthand':{'rotation':[0,0,0],'scale':[.6,.6,.6]},'firstperson_righthand':{'rotation':[0,-35,5],'scale':[.6,.6,.6]}}
    write(Path('models/item')/(id+'.json'),m)
legs=[box([x,0,z],[x+2,11,z+2],'wood') for x in (1,13) for z in (1,13)]
block('void_foundation',[box([0,0,0],[16,16,16],'pale'),box([1,15.95,1],[15,16,1.25],'metal'),box([1,15.95,14.75],[15,16,15],'metal')])
door=[box([1,0,5],[3,16,11],'wood'),box([13,0,5],[15,16,11],'wood'),box([1,14,5],[15,16,11],'wood'),box([3,0,7],[13,14,9],'violet'),box([4,8,6.8],[7,12,7],'dark'),box([9,8,6.8],[12,12,7],'dark'),box([4,2,6.8],[12,6,7],'dark'),box([11,6.5,6],[12.2,7.7,7],'gold')]
block('special_dimension_little_door',door)
block('world_work_desk',[*legs,box([0,11,0],[16,13,16],'wood'),box([10,5,2],[14,11,14],'wood'),box([10.1,9,1.8],[14,9.5,2],'gold'),box([2,13,5],[9,13.5,12],'paper'),box([9,13,8],[14,14.5,13],'violet'),box([11,14.5,8.5],[11.5,14.8,12.5],'gold'),box([2,13,2],[4,15,4],'dark')])
block('orphan_bed',[box([1,0,0],[15,4,16],'wood'),box([0,4,0],[16,8,16],'cloth'),box([1,8,1],[15,9,5],'paper'),box([0,4,15],[16,12,16],'wood')])
block('orphan_food_table',[*legs,box([0,11,0],[16,13,16],'wood'),box([2,13,3],[7,13.5,8],'pale'),box([10,13,5],[13,15,8],'water')])
block('orphan_fridge',[box([1,0,1],[15,16,15],'pale'),box([1.3,1,0],[14.7,10,1],'paper'),box([1.3,10.5,0],[14.7,15.5,1],'paper'),box([11,5,-.5],[12,9,0],'metal'),box([11,12,-.5],[12,14,0],'metal')])
block('water_station',[box([2,0,2],[14,8,14],'wood'),box([3,8,3],[13,15,13],'water'),box([7,6,0],[9,8,3],'metal'),box([3,1,0],[13,2,3],'metal')])
block('orphan_rug',[box([0,0,0],[16,.5,16],'violet'),box([1,.5,1],[15,.7,15],'cloth'),box([7,.7,1],[9,.8,15],'gold')])
block('orphan_lamp',[box([3,0,3],[13,2,13],'wood'),box([7,2,7],[9,10,9],'gold'),box([2,10,2],[14,16,14],'paper')])
block('orphan_plant',[box([4,0,4],[12,5,12],'wood'),box([7,5,7],[9,13,9],'wood'),box([2,8,3],[14,14,13],'green')])
block('toy_box',[box([1,0,1],[15,8,15],'cloth'),box([0,8,0],[16,10,16],'wood'),box([3,10,5],[6,13,9],'red'),box([9,10,4],[13,14,8],'gold')])
block('storage_cabinet',[box([1,0,1],[15,16,15],'wood'),box([1,1,0],[7.8,15,1],'wood'),box([8.2,1,0],[15,15,1],'wood'),box([6,7,-.5],[7,10,0],'gold'),box([9,7,-.5],[10,10,0],'gold')])
item('world_weaver',[box([2,1,6],[4,15,8],'dark'),box([12,1,6],[14,15,8],'dark'),box([2,13,6],[14,15,8],'metal'),box([2,1,6],[14,3,8],'gold'),*[box([x,3,7],[x+.3,13,7.3],'violet') for x in (5,7,9,11)],box([6,5,5],[10,7,6],'gold'),box([7,0,6],[9,2,8],'metal')])
item('void_power',[box([5,2,5],[11,12,11],'dark'),box([6,3,4.8],[10,11,5.2],'violet'),box([7,12,7],[9,15,9],'violet'),box([6,1,6],[10,2,10],'gold')])
item('soul_essence',[box([4,3,4],[12,13,12],'water'),box([6,5,6],[10,11,10],'violet'),box([5,1,5],[11,3,11],'metal')])
item('orphanage_phone',[box([2,1,2],[14,4,14],'dark'),box([4,4,4],[12,6,12],'violet'),box([2,7,2],[5,11,14],'dark'),box([11,7,2],[14,11,14],'dark'),box([4,9,5],[12,11,8],'metal'),*[box([x,6,7],[x+1,6.5,8],'gold') for x in (5,7,9)],box([13,3,12],[14,8,13],'gold')])
item('orphan_play_ball',[box([3,3,3],[13,13,13],'red'),box([7,2.8,2.8],[9,13.2,13.2],'paper')])
print('Generated 12 custom furniture models and 5 modeled tycoon items; vanilla material references only.')
