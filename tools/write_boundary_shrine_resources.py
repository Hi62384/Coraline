"""Actual boundary defense encounter and its once-unlocked reward cache."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
DATA=ROOT/'data/coraline'
ASSETS=ROOT/'assets/coraline'


def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')


def cube(start,end,texture):
    return {'from':start,'to':end,'faces':{face:{'texture':'#'+texture} for face in ['north','south','east','west','up','down']}}


def main():
    name='ancient_boundary_shrine'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['pale_wasteland','porcelain_plains','forgotten_gardens']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,value)
    pools=[]
    for item,lo,hi in [('button',4,8),('other_iron_ingot',3,6),('moonstone',1,2),('other_diamond',1,2)]:pools.append({'rolls':1,'entries':[{'type':'minecraft:item','name':'coraline:'+item,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]})
    for table,chance in [('deep_armor',.2),('exploration',.3)]:pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/'+table}]})
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':pools})
    variants={}
    for stage,color in enumerate(['sea_lantern','orange_concrete','red_concrete','emerald_block']):
        model='boundary_defense_beacon_'+str(stage)
        elements=[cube([0,0,0],[16,3,16],'stone'),cube([3,3,3],[13,13,13],'light'),cube([0,13,0],[16,16,16],'stone')]
        for x,z in [(1,1),(13,1),(1,13),(13,13)]:elements.append(cube([x,3,z],[x+2,13,z+2],'stone'))
        write(ASSETS/f'models/block/{model}.json',{'textures':{'stone':'minecraft:block/smooth_quartz','light':'minecraft:block/'+color,'particle':'minecraft:block/smooth_quartz'},'elements':elements})
        variants['stage='+str(stage)]={'model':'coraline:block/'+model}
    write(ASSETS/'blockstates/boundary_defense_beacon.json',{'variants':variants})
    write(DATA/'loot_tables/blocks/boundary_defense_beacon.json',{'type':'minecraft:block','pools':[]})
    path=ASSETS/'lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    value.update({'structure.coraline.ancient_boundary_shrine':'Ancient Boundary Shrine','block.coraline.boundary_defense_beacon':'Boundary Beacon',
      'message.coraline.boundary_complete':'The boundary is secure. Its reward cache is open.',
      'message.coraline.boundary_active':'Defend the beacon. Keep close until the attackers are defeated or the boundary stabilizes.',
      'message.coraline.boundary_retry':'The beacon is recovering. Try again shortly.',
      'message.coraline.boundary_clear_space':'Clear the four inner corners of the shrine before starting its defense.',
      'message.coraline.boundary_started':'Defend the beacon! Defeat the attackers or hold the boundary for 60 seconds.',
      'message.coraline.boundary_victory':'Boundary stabilized. The shrine reward cache is now open.',
      'message.coraline.boundary_failed':'The boundary failed. Its reward stays sealed; the beacon will recover.',
      'message.coraline.boundary_status':'Hold for %s seconds. Beacon integrity: %s/32.',
      'message.coraline.boundary_reward_sealed':'Defend this shrine at its Boundary Beacon to unseal the reward.'})
    write(path,value)


if __name__=='__main__':main()
