"""Native six-house settlement, merchants and ordinary exploration supplies."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/coraline'


def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')


def main():
    name='other_village'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['stitched_meadows','buttonwood_forest','forgotten_gardens']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':2})
    write(path,value)
    pools=[]
    for item,lo,hi,chance in [('button',1,4,1),('other_iron_ingot',1,2,.35)]:
        pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:item','name':'coraline:'+item,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]})
    pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':.1}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/ordinary_clothing'}]})
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':pools})
    write(DATA/'loot_tables/entities/otherworld_resident.json',{'type':'minecraft:entity','pools':[]})
    path=ROOT/'assets/coraline/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    value.update({'structure.coraline.other_village':'Other Village','entity.coraline.otherworld_resident':'Otherworld Inhabitant','entity.coraline.otherworld_resident.role_0':'Otherworld Provisioner','entity.coraline.otherworld_resident.role_1':'Otherworld Tinker','entity.coraline.otherworld_resident.role_2':'Otherworld Tailor'})
    write(path,value)


if __name__=='__main__':main()
