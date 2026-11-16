"""Institutional investigation, not a disposable NPC reward farm."""
import json
from write_endless_carnival_resources import DATA,ASSETS,item,pool
from write_grand_theatre_resources import write,cube
def main():
    name='abandoned_orphanage'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+k for k in ['paperwood_forest','rootbound_wilds','pale_wasteland']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';v=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in v['structures']):v['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,v)
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':[pool([item('coraline:soulglass',2,4)]),pool([item('coraline:other_iron_ingot',1,3)]),pool([item('coraline:moonstone',1,2)],.4),pool([item('coraline:silent_slippers'),item('coraline:comfy_pajamas'),item('coraline:midnight_coat')]),pool([item('coraline:button',3,7)])]})
    variants={}
    for slot in range(4):
        model='orphanage_register_'+str(slot);write(ASSETS/f'models/block/{model}.json',{'textures':{'wood':'minecraft:block/dark_oak_planks','paper':'minecraft:block/white_wool','ink':'minecraft:block/black_concrete','particle':'minecraft:block/dark_oak_planks'},'elements':[cube([0,0,0],[16,14,16],'wood'),cube([2,14,2],[14,15,14],'paper'),cube([4,15,4],[12,15.1,5],'ink'),cube([4,15,7],[12-slot*2,15.1,8],'ink')]});variants['slot='+str(slot)]={'model':'coraline:block/'+model}
    write(ASSETS/'blockstates/orphanage_register.json',{'variants':variants})
    write(ASSETS/'models/block/orphanage_binding.json',{'textures':{'bars':'minecraft:block/iron_bars','particle':'minecraft:block/iron_bars'},'elements':[cube([0,0,7],[16,16,9],'bars'),cube([7,0,0],[9,16,16],'bars')]})
    # One crossed mesh works for every native IronBars connection state.
    write(ASSETS/'blockstates/orphanage_binding.json',{'multipart':[{'apply':{'model':'coraline:block/orphanage_binding'}}]})
    for block in ['orphanage_register','orphanage_binding']:write(DATA/f'loot_tables/blocks/{block}.json',{'type':'minecraft:block','pools':[]})
    path=ASSETS/'lang/en_us.json';v=json.loads(path.read_text(encoding='utf-8-sig'));v.update({'structure.coraline.abandoned_orphanage':'Abandoned Orphanage','block.coraline.orphanage_register':'Orphanage Record','block.coraline.orphanage_binding':'Institutional Binding','entity.coraline.otherworld_resident.role_5':'Trapped Inhabitant','entity.coraline.otherworld_resident.role_5_freed':'Freed Inhabitant',
      'message.coraline.orphanage_record_0':'Admissions: Every arrival signed the same name. The west ledger lists two inhabitants transferred to the rear cells. Neither signed a release.',
      'message.coraline.orphanage_record_1':'Matron’s Orders: Keep the doors bound. The inhabitants are people, not property. Someone crossed out the word “people.” Compare the rear discharge record.',
      'message.coraline.orphanage_record_2':'Discharges: The transfers were forged. Neither inhabitant owes the institution a debt. All three records authorize the front-office release register.',
      'message.coraline.orphanage_missing':'The release is still blocked. Read the west admissions, east orders, and rear discharge records, then return here.',
      'message.coraline.orphanage_released':'The forged custody is ended. The cell bindings dissolve, the surviving inhabitants are free, and the archive is open.',
      'message.coraline.orphanage_reward_sealed':'Investigate the three orphanage records and authorize release at the front-office register.',
      'message.coraline.orphan_bound':'They keep our names in the records. Find the admissions, orders and discharges. The front register controls these bindings.',
      'message.coraline.orphan_freed':'There is a road outside. This time we can choose to follow it.'});write(path,v)
if __name__=='__main__':main()
