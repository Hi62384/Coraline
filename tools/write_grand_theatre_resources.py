"""A real eight-phrase stage game, surviving troupe and sealed performance cache."""
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
    name='grand_theatre'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['porcelain_plains','stitched_meadows','candy_wonderland']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,value)
    pools=[]
    for item,lo,hi,chance in [('button',3,7,1),('other_iron_ingot',2,5,1),('soulglass',1,2,.3),('other_diamond',1,1,.15)]:pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:item','name':'coraline:'+item,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]})
    pools += [{'rolls':1,'entries':[{'type':'minecraft:item','name':'coraline:circus_jacket','weight':1},{'type':'minecraft:item','name':'coraline:top_hat','weight':1}]},{'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':.15}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/rare_wardrobe'}]}]
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':pools})
    variants={}
    for color,name in enumerate(['red_concrete','blue_concrete','green_concrete','purple_concrete']):
        for lit in [False,True]:
            model=f'performance_tile_{color}_{int(lit)}';elements=[cube([0,0,0],[16,16,16],'color')]
            if lit:elements.append({'from':[3,16.01,3],'to':[13,16.01,13],'faces':{'up':{'texture':'#light'}}})
            write(ASSETS/f'models/block/{model}.json',{'textures':{'color':'minecraft:block/'+name,'light':'minecraft:block/sea_lantern','particle':'minecraft:block/'+name},'elements':elements})
            variants[f'color={color},lit={str(lit).lower()}']={'model':'coraline:block/'+model}
    write(ASSETS/'blockstates/performance_tile.json',{'variants':variants})
    variants={}
    for phase in range(3):
        model='performance_control_'+str(phase);elements=[cube([0,0,0],[16,16,16],'wood')]
        for key in range(4):elements.append(cube([2+key*3,16,3],[4+key*3,17,8],'light'))
        write(ASSETS/f'models/block/{model}.json',{'textures':{'wood':'minecraft:block/note_block','light':'minecraft:block/emerald_block' if phase==2 else 'minecraft:block/sea_lantern' if phase==1 else 'minecraft:block/polished_deepslate','particle':'minecraft:block/note_block'},'elements':elements})
        variants['phase='+str(phase)]={'model':'coraline:block/'+model}
    write(ASSETS/'blockstates/performance_control.json',{'variants':variants})
    for block in ['performance_control','performance_tile']:write(DATA/f'loot_tables/blocks/{block}.json',{'type':'minecraft:block','pools':[]})
    path=ASSETS/'lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    value.update({'structure.coraline.grand_theatre':'Grand Theatre','block.coraline.performance_control':'Stage Cue Box','block.coraline.performance_tile':'Stage Spotlight Mark','entity.coraline.otherworld_resident.role_4':'Bound Performer',
      'message.coraline.performance_started':'Take your place on the lit mark. At each second chime, jump from that mark. Complete eight phrases to break the repeating script.',
      'message.coraline.performance_active':'A performance is in progress. Follow the lit marks and jump at the second chime.',
      'message.coraline.performance_missed':'The phrase repeats. Return to the lit mark and jump at the second chime.',
      'message.coraline.performance_complete':'The repeating performance has ended. The surviving troupe is free, and the backstage cache is open.',
      'message.coraline.performance_reward_sealed':'Break this theatre’s repeating performance at its Stage Cue Box to open the backstage cache.',
      'message.coraline.performer_loop':'The same steps. The same ending. Please change the performance at the cue box.',
      'message.coraline.performer_freed':'The script finally ended. We can choose our own steps again.'})
    write(path,value)


if __name__=='__main__':main()
