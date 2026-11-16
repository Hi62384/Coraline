"""Physical false palace and cage puzzle; no forged original-character receipt."""
import json
from write_endless_carnival_resources import DATA,ASSETS,item,pool
from write_grand_theatre_resources import write
def main():
    name='false_pink_palace';write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+k for k in ['forgotten_gardens','pale_wasteland','porcelain_plains']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';v=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in v['structures']):v['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,v)
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':[pool([item('coraline:button',4,8)]),pool([item('coraline:other_iron_ingot',2,4)]),pool([item('coraline:moonstone',2,4)]),pool([item('coraline:soulglass',2,4)]),pool([item('coraline:other_diamond',1,2)]),pool([{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/rare_wardrobe'}]),pool([{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/supernatural_weapons'}],.15)]})
    path=ASSETS/'lang/en_us.json';v=json.loads(path.read_text(encoding='utf-8-sig'));v.update({'structure.coraline.false_pink_palace':'False Pink Palace','message.coraline.false_palace_sealed':'The archive is bound to the Buttonroot Cage. Align the two upstairs dials and the downstairs rear dial with their illuminated floor clues, then operate the anchor beside the cage.','message.coraline.false_palace_opened':'The Buttonroot Cage anchors are disconnected. Its bindings and the upper archive are open.'});write(path,v)
if __name__=='__main__':main()
