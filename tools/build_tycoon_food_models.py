"""Distinct code-native 3D food/furniture exports, using existing vanilla materials.
No bitmap masters, recording changes or runtime high-resolution textures.
"""
from pathlib import Path
from build_tycoon_models import box, model, write, block, item

def plate():
    return [box([1,1,1],[15,1.7,15],'pale'),box([2,1.7,2],[14,2,14],'gold')]

item('button_biscuits',[*plate(),*[box([x,2,z],[x+4,3,z+4],'wood') for x,z in [(3,3),(8,4),(5,9)]],*[box([x,3,z],[x+.55,3.2,z+.55],'dark') for x,z in [(4,4),(5.5,4),(4,5.5),(5.5,5.5),(9,5),(10.5,5),(9,6.5),(10.5,6.5),(6,10),(7.5,10),(6,11.5),(7.5,11.5)]]])
item('moonberry_jam_toast',[*plate(),box([3,2,3],[12,3.3,13],'wood'),box([3.6,3.3,3.6],[11.4,3.6,12.4],'paper'),box([4,3.6,4],[11,3.9,12],'violet'),*[box([x,3.9,z],[x+1,4.4,z+1],'water') for x,z in [(5,5),(9,6),(6,10)]],box([12,2,3],[13,2.5,14],'metal')])
item('other_fathers_vegetable_soup',[box([3,1,3],[13,2,13],'pale'),box([2,2,2],[14,4,14],'pale'),box([1.5,4,1.5],[14.5,5,14.5],'gold'),box([3,4,3],[13,4.5,13],'red'),*[box([x,4.5,z],[x+2,4.9,z+2],'green') for x,z in [(4,4),(9,8),(5,10)]],box([8,4.6,4],[10,5,6],'gold'),box([13,4,6],[16,5,7],'metal')])
item('golden_garden_pie',[*plate(),box([3,2,3],[13,5,13],'wood'),box([2.5,5,2.5],[13.5,5.7,13.5],'gold'),box([3.5,5.7,3.5],[12.5,6,12.5],'red'),*[box([x,6,3],[x+.7,6.4,13],'gold') for x in [4,7,10]],*[box([3,6.5,z],[13,6.9,z+.7],'gold') for z in [4,7,10]],box([7,7,7],[9,7.4,9],'green')])
item('little_door_macarons',[*plate(),*[part for x,z,t in [(3,3,'violet'),(9,4,'cloth'),(6,10,'red')] for part in [box([x,2,z],[x+4,3,z+3],t),box([x,3,z],[x+4,3.7,z+3],'paper'),box([x,3.7,z],[x+4,4.7,z+3],t),box([x+1,4.7,z+1],[x+1.5,4.9,z+1.5],'gold'),box([x+2.5,4.7,z+1],[x+3,4.9,z+1.5],'gold')]]])
item('other_mothers_chocolate_truffles',[box([1,1,1],[15,2,15],'gold'),box([2,2,2],[14,2.4,14],'violet'),*[part for x,z in [(3,3),(9,3),(3,9),(9,9)] for part in [box([x,2.4,z],[x+4,3,z+4],'paper'),box([x+.3,3,z+.3],[x+3.7,5,z+3.7],'wood'),box([x+1,5,z+1],[x+3,5.5,z+3],'dark'),box([x+1.8,5.5,z+1],[x+2.3,5.8,z+3],'gold')]]])
item('grand_other_world_supper',[*plate(),box([2,2,3],[8,4,9],'wood'),box([3,4,4],[7,4.5,8],'gold'),*[box([x,2,z],[x+2,4,z+2],'green') for x,z in [(9,3),(11,5),(9,7)]],box([3,2,10],[7,3.5,13],'paper'),box([8,2,10],[13,3,13],'gold'),box([0,2,2],[1,3,14],'metal'),box([15,2,2],[16,3,14],'metal')])
cake=[*plate(),box([2,2,2],[14,5,14],'wood'),box([1.5,5,1.5],[14.5,5.8,14.5],'paper'),box([3.5,5.8,3.5],[12.5,8.5,12.5],'violet'),box([3,8.5,3],[13,9.3,13],'paper'),box([5,9.3,5],[11,12,11],'red'),box([4.5,12,4.5],[11.5,12.7,11.5],'paper'),box([7.5,12.7,7.5],[8.5,16,8.5],'gold'),box([6.5,15,7],[9.5,17,9],'violet')]
for x,z in [(3,3),(11,3),(3,11),(11,11),(5,5),(10,10)]:
    cake += [box([x,5.8,z],[x+1.6,6.2,z+1.6],'gold'),box([x+.5,6.2,z+.5],[x+.8,6.4,z+.8],'dark')]
item('welcome_home_cake',cake)

legs=[box([x,0,z],[x+2,11,z+2],'wood') for x in [1,13] for z in [1,13]]
block('other_world_supper_table',[*legs,box([0,10.8,0],[16,12,16],'gold'),box([0,12,0],[16,13,16],'wood'),box([1,13,1],[15,13.3,15],'violet'),box([5,13.3,1],[11,13.5,15],'paper'),box([3,13.5,3],[7,14,7],'pale'),box([9,13.5,9],[13,14,13],'pale'),box([11,13.5,3],[13,15,5],'water')])
base=[box([1,0,2],[15,16,15],'cloth'),box([2,1,1],[14,15,2],'dark'),box([3,5,1],[13,5.5,3],'pale'),box([3,10,1],[13,10.5,3],'pale'),box([1,0,1],[3,16,2],'gold'),box([13,0,1],[15,16,2],'gold')]
door=[box([1,1,0],[15,15,1],'paper'),box([2,2,-.1],[14,14,0],'cloth'),box([11,5,-1],[12,11,0],'gold'),box([3,11,-.2],[5,13,0],'violet')]
variants={}
for phase in range(4):
    panels=[]
    for element in door:
        e=dict(element)
        if phase:e['rotation']={'origin':[1,8,1],'axis':'y','angle':[-22.5,-45,-45][phase-1]}
        panels.append(e)
    name='other_world_fridge_open_'+str(phase)
    write(Path('models/block')/(name+'.json'),model([*base,*panels]))
    for facing,yaw in [('north',0),('east',90),('south',180),('west',270)]:
        for lit in ['false','true']:
            for broken in ['false','true']:variants[f'facing={facing},lit={lit},broken={broken},open={phase}']={'model':'coraline:block/'+name,'y':yaw}
write(Path('blockstates/other_world_fridge.json'),{'variants':variants})
write(Path('models/item/other_world_fridge.json'),{'parent':'coraline:block/other_world_fridge_open_0'})
print('Generated eight distinct 3D foods, supper furniture and animated fridge frames; no raster/audio assets.')
