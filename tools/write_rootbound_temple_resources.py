"""Overgrown temple, finite defenders and a separate high-tier sanctum cache."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/coraline'


def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')


def stack(item,lo,hi,chance=1):
    return {'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:item','name':'coraline:'+item,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]}


def main():
    name='rootbound_temple'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['rootbound_wilds','lanternwood','buttonwood_forest']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,value)
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':[stack('button',2,5),stack('other_iron_ingot',1,3),stack('soulglass',1,2,.15),stack('garden_overalls',1,1,.2)]})
    pools=[{'rolls':1,'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/structures/'+name}]},stack('soulglass',2,4),stack('moonstone',1,2,.6),stack('perfect_ingot',1,1,.01)]
    for table,chance in [('exploration',.3),('rare_wardrobe',.2),('deep_armor',.1)]:pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/'+table}]})
    write(DATA/f'loot_tables/postgame/structures/{name}_sanctum.json',{'type':'minecraft:chest','pools':pools})
    path=ROOT/'assets/coraline/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'));value['structure.coraline.'+name]='Rootbound Temple';write(path,value)


if __name__=='__main__':main()
