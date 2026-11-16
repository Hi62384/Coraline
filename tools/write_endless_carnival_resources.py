"""Runtime resources for the finite shooting gallery; merge shared manifests."""
from pathlib import Path
import json
from write_grand_theatre_resources import write,cube
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/coraline';ASSETS=ROOT/'assets/coraline'
def item(name,lo=1,hi=1,weight=1):
    return {'type':'minecraft:item','name':name,'weight':weight,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}
def pool(entries,chance=1):
    return {'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':entries}
def main():
    name='endless_carnival'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['candy_wonderland','stitched_meadows','paperwood_forest']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,value)
    write(DATA/f'loot_tables/postgame/structures/{name}_supplies.json',{'type':'minecraft:chest','pools':[pool([item('minecraft:bow')]),pool([item('minecraft:arrow',12,12)]),pool([item('coraline:button',2,4)]),pool([item('coraline:other_iron_ingot')])]})
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':[pool([item('coraline:circus_jacket'),item('coraline:top_hat')]),pool([item('coraline:button',4,8)]),pool([item('coraline:other_iron_ingot',2,4)]),pool([item('coraline:other_diamond')],.2)]})
    variants={}
    for phase in range(3):
        model='carnival_control_'+str(phase)
        write(ASSETS/f'models/block/{model}.json',{'textures':{'wood':'minecraft:block/note_block','light':'minecraft:block/emerald_block' if phase==2 else 'minecraft:block/sea_lantern' if phase==1 else 'minecraft:block/gold_block','particle':'minecraft:block/note_block'},'elements':[cube([0,0,0],[16,16,16],'wood'),cube([3,16,3],[13,17,13],'light')]})
        variants['phase='+str(phase)]={'model':'coraline:block/'+model}
    write(ASSETS/'blockstates/carnival_control.json',{'variants':variants})
    write(DATA/'loot_tables/blocks/carnival_control.json',{'type':'minecraft:block','pools':[]})
    path=ASSETS/'lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    value.update({'structure.coraline.endless_carnival':'Endless Carnival','block.coraline.carnival_control':'Carnival Gallery Bell','entity.coraline.carnival_target':'Carnival Button Target','entity.coraline.otherworld_resident.role_4_freed':'Freed Performer',
      'message.coraline.carnival_started':'Stand behind the yellow firing line and shoot the glowing targets. Eight correct hits in sixty seconds will end the routine. Supplies are in the side booth.',
      'message.coraline.carnival_active':'The gallery is running. Shoot the glowing target from the yellow firing lane.',
      'message.coraline.carnival_retry':'The gallery is resetting. Ring the bell again in a few seconds.',
      'message.coraline.carnival_timeout':'Time is up. The prize stays sealed; the bell will allow another attempt shortly.',
      'message.coraline.carnival_complete':'The carnival routine is broken. The surviving performers are free, and the prize cache is open.',
      'message.coraline.carnival_score':'Correct target: %s / 8',
      'message.coraline.carnival_reward_sealed':'Break this carnival’s routine at the Gallery Bell to open its prize cache.',
      'message.coraline.carnival_performer_loop':'The prizes never change and the targets never stop. Ring the gallery bell. Please end this routine.'})
    write(path,value)
if __name__=='__main__':main()
