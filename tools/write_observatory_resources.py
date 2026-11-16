"""Physical constellation research and eight-way code-authored instrument models."""
from pathlib import Path
import json
import math

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/coraline'
ASSETS=ROOT/'assets/coraline'


def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')


def cube(start,end,texture):
    return {'from':start,'to':end,'faces':{face:{'texture':'#'+texture} for face in ['north','south','east','west','up','down']}}


def main():
    name='forgotten_observatory'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['clockwork_highlands','threadbound_mountains','porcelain_plains']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,value)
    pools=[]
    for item,lo,hi,chance in [('button',3,7,1),('other_iron_ingot',2,4,1),('moonstone',1,3,1),('other_diamond',1,1,.4)]:pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:item','name':'coraline:'+item,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]})
    for table,chance in [('exploration',.4),('rare_wardrobe',.25)]:pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/'+table}]})
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':pools})
    stars={}
    for color,texture in enumerate(['ochre_froglight_side','pearlescent_froglight_side','verdant_froglight_side','sea_lantern']):
        model='observatory_star_'+str(color);write(ASSETS/f'models/block/{model}.json',{'parent':'minecraft:block/cube_all','textures':{'all':'minecraft:block/'+texture}});stars['color='+str(color)]={'model':'coraline:block/'+model}
    write(ASSETS/'blockstates/observatory_star.json',{'variants':stars})
    variants={}
    for observed in range(5):
        for rotation in range(8):
            model=f'observatory_telescope_{observed}_{rotation}'
            elements=[cube([1,0,1],[15,3,15],'metal'),cube([6,3,6],[10,8,10],'dark')]
            angle=rotation*math.pi/4
            for segment in range(6):
                x=8-math.sin(angle)*segment*1.2;z=8+math.cos(angle)*segment*1.2;y=6+segment*1.5
                elements.append(cube([round(x-1.5,4),round(y-1.5,4),round(z-1.5,4)],[round(x+1.5,4),round(y+1.5,4),round(z+1.5,4)],'lens' if segment==5 else 'metal'))
            for dot in range(4):elements.append(cube([3+dot*3,3,1],[5+dot*3,4,3],'lens' if dot<observed else 'dark'))
            write(ASSETS/f'models/block/{model}.json',{'textures':{'metal':'minecraft:block/iron_block','dark':'minecraft:block/polished_deepslate','lens':'minecraft:block/sea_lantern','particle':'minecraft:block/iron_block'},'elements':elements})
            variants[f'observed={observed},rotation={rotation}']={'model':'coraline:block/'+model}
    write(ASSETS/'blockstates/observatory_telescope.json',{'variants':variants})
    for block in ['observatory_star','observatory_telescope']:write(DATA/f'loot_tables/blocks/{block}.json',{'type':'minecraft:block','pools':[]})
    path=ASSETS/'lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    value.update({'structure.coraline.forgotten_observatory':'Forgotten Observatory','block.coraline.observatory_telescope':'Constellation Instrument','block.coraline.observatory_star':'Constellation Light',
      'message.coraline.observatory_hint':'Stand by the instrument and use a spyglass. Hold your view on each of the four constellation lights for a moment.',
      'message.coraline.observatory_observed':'Observation recorded: %s/4.',
      'message.coraline.observatory_complete':'The constellation is charted. The research archive is open.',
      'message.coraline.observatory_reward_sealed':'Observe the four constellation lights through a spyglass beside this observatory’s instrument.'})
    write(path,value)


if __name__=='__main__':main()
