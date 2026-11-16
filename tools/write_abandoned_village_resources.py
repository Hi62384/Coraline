"""Damaged settlement and its physically accessible buried archive."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/coraline'


def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')


def main():
    name='abandoned_village'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['paperwood_forest','rootbound_wilds','lanternwood']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,value)
    pools=[]
    for item,lo,hi,chance in [('button',2,5,1),('other_iron_ingot',1,3,.6)]:
        pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:item','name':'coraline:'+item,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]})
    pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':.12}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/ordinary_clothing'}]})
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':pools})
    vault=[{'rolls':1,'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/structures/'+name}]}]
    for item,chance in [('moonstone',1),('other_diamond',.4)]:vault.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:item','name':'coraline:'+item,'weight':1}]})
    for table,chance in [('rare_wardrobe',.3),('exploration',.2),('deep_armor',.1)]:vault.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/'+table}]})
    write(DATA/f'loot_tables/postgame/structures/{name}_crypt.json',{'type':'minecraft:chest','pools':vault})
    path=ROOT/'assets/coraline/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'));value['structure.coraline.'+name]='Abandoned Village';write(path,value)


if __name__=='__main__':main()
