"""Challenging finite containment arena; no main-story Heart rewards."""
import json
from write_endless_carnival_resources import DATA,ASSETS,item,pool
from write_grand_theatre_resources import write
def main():
    name='entity_heart_chamber';write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+k for k in ['white_rift_wastes','threadbound_mountains','pale_wasteland']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';v=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in v['structures']):v['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,v)
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':[pool([item('coraline:button',4,8)]),pool([item('coraline:other_iron_ingot',3,6)]),pool([item('coraline:soulglass',3,6)]),pool([item('coraline:moonstone',2,4)]),pool([item('coraline:other_diamond',1,2)]),pool([item('coraline:perfect_ingot')],.08),pool([{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/deep_armor'}],.3)]})
    path=ASSETS/'lang/en_us.json';v=json.loads(path.read_text(encoding='utf-8-sig'));v.update({'structure.coraline.entity_heart_chamber':'Entity Heart Chamber',
      'message.coraline.heart_started':'Containment begins. Defeat all eight Rift Soldiers within two minutes while protecting the beacon. Waiting alone cannot close the heart.',
      'message.coraline.heart_status':'Containment: %s seconds left. Beacon integrity: %s/64.',
      'message.coraline.heart_timeout':'The heart escaped containment. Its reward stays sealed. Clear the chamber before trying again.',
      'message.coraline.heart_victory':'The regional heart is contained. The archive is open. A powered Breach Drive receives containment credit once; an empty Drive cannot bank credit for later.'});write(path,v)
if __name__=='__main__':main()
