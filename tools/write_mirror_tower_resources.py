"""Author a real navigation tower without altering existing biome resources."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/coraline'


def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')


def stack(name,lo,hi,chance=None):
    pool={'rolls':1,'entries':[{'type':'minecraft:item','name':'coraline:'+name,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]}
    if chance is not None:pool['conditions']=[{'condition':'minecraft:random_chance','chance':chance}]
    return pool


def main():
    write(DATA/'worldgen/structure/mirror_tower.json',{'type':'coraline:mirror_tower','biomes':['coraline:postgame/'+name for name in ['porcelain_plains','stitched_coast','threadbound_mountains']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json'
    source=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:mirror_tower' for row in source['structures']):source['structures'].append({'structure':'coraline:mirror_tower','weight':1})
    write(path,source)
    pools=[stack('button',3,7),stack('other_iron_ingot',2,5),stack('moonstone',2,4),stack('other_diamond',1,2,.4)]
    for name,chance in [('exploration',.4),('rare_wardrobe',.25),('supernatural_weapons',.12)]:
        pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/'+name}]})
    write(DATA/'loot_tables/postgame/structures/mirror_tower.json',{'type':'minecraft:chest','pools':pools})
    assets=ROOT/'assets/coraline'
    write(assets/'blockstates/illusion_mirror.json',{'variants':{'':{'model':'coraline:block/illusion_mirror'}}})
    write(assets/'models/block/illusion_mirror.json',{'parent':'minecraft:block/cube_all','textures':{'all':'minecraft:block/tinted_glass'}})
    write(DATA/'loot_tables/blocks/illusion_mirror.json',{'type':'minecraft:block','pools':[]})
    path=assets/'lang/en_us.json';words=json.loads(path.read_text(encoding='utf-8-sig'))
    words.update({'block.coraline.illusion_mirror':'False Mirror','structure.coraline.mirror_tower':'Mirror Tower'})
    write(path,words)


if __name__=='__main__':main()
