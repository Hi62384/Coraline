"""Author the finite military outpost and merge its native placement entry.

Uses existing atlases, guards, durable cache receipts and shared equipment
tables. Never rewrites another family's layout, biome spawn pools or loot.
"""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/coraline'


def write(path, value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')


def stack(name,lo,hi,chance=None):
    pool={'rolls':1,'entries':[{'type':'minecraft:item','name':'coraline:'+name,'weight':1,
          'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]}
    if chance is not None:
        pool['conditions']=[{'condition':'minecraft:random_chance','chance':chance}]
    return pool


def equipment(name,chance):
    return {'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],
            'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/'+name}]}


def main():
    name='rift_soldier_outpost'
    write(DATA/f'worldgen/structure/{name}.json',{
        'type':'coraline:'+name,
        'biomes':['coraline:postgame/'+biome for biome in ['clockwork_highlands','pale_wasteland','otherworld_badlands']],
        'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json'
    placement=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in placement['structures']):
        placement['structures'].append({'structure':'coraline:'+name,'weight':2})
    write(path,placement)
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':[
        stack('button',2,6),stack('other_iron_ingot',3,8),stack('other_diamond',1,1,.12),
        equipment('factory_tools',.12),equipment('early_armor',.1)]})
    write(DATA/f'loot_tables/postgame/structures/{name}_control.json',{'type':'minecraft:chest','pools':[
        {'rolls':1,'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/structures/'+name}]},
        stack('moonstone',1,2,.35),equipment('supernatural_weapons',.08),equipment('deep_armor',.06)]})
    path=ROOT/'assets/coraline/lang/en_us.json'
    words=json.loads(path.read_text(encoding='utf-8-sig'))
    words['structure.coraline.rift_soldier_outpost']='Rift Soldier Outpost'
    write(path,words)


if __name__=='__main__':
    main()
