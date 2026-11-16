"""Populated allied sanctuary, real defender and once-only defense reward."""
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
    name='resistance_sanctuary'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['forgotten_gardens','lanternwood','rootbound_wilds']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,value)
    # The residence table does not expose the gated defense rewards.
    write(DATA/f'loot_tables/postgame/structures/{name}_supplies.json',{'type':'minecraft:chest','pools':[stack('button',1,3),stack('other_iron_ingot',1,2,.25)]})
    pools=[stack('button',4,8),stack('other_iron_ingot',3,6),stack('moonstone',1,2),stack('other_diamond',1,1)]
    for table,chance in [('exploration',.3),('deep_armor',.2)]:pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/'+table}]})
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':pools})
    path=ROOT/'assets/coraline/lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    value.update({'structure.coraline.resistance_sanctuary':'Resistance Sanctuary','entity.coraline.otherworld_resident.role_3':'Resistance Lookout','message.coraline.resistance_lookout':"I'll keep watch. Use the Boundary Beacon when you're ready to defend this sanctuary."})
    write(path,value)


if __name__=='__main__':main()
