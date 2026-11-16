"""Native physical alignment puzzle; runtime models reuse Minecraft atlases."""
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
    name='dimensional_seam'
    write(DATA/f'worldgen/structure/{name}.json',{'type':'coraline:'+name,'biomes':['coraline:postgame/'+key for key in ['white_rift_wastes','pale_wasteland','threadbound_mountains']],'spawn_overrides':{},'step':'surface_structures','terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    if not any(row['structure']=='coraline:'+name for row in value['structures']):value['structures'].append({'structure':'coraline:'+name,'weight':1})
    write(path,value)
    pools=[]
    for item,lo,hi,chance in [('button',2,6,1),('other_iron_ingot',2,5,1),('moonstone',1,2,.6),('soulglass',1,3,.5),('other_diamond',1,2,.3)]:
        pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],'entries':[{'type':'minecraft:item','name':'coraline:'+item,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]})
    pools.append({'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':.2}],'entries':[{'type':'minecraft:loot_table','name':'coraline:postgame/equipment/exploration'}]})
    write(DATA/f'loot_tables/postgame/structures/{name}.json',{'type':'minecraft:chest','pools':pools})
    for core in [False,True]:
        block='dimensional_seam_'+('core' if core else 'dial');variants={}
        for active in [False,True]:
            suffix='' if active else '_closed';model=block+suffix
            elements=[cube([1,0,1],[15,3,15],'frame')]
            if core:
                elements += [cube([2,3,6],[4,30,10],'frame'),cube([12,3,6],[14,30,10],'frame'),cube([4,28,6],[12,31,10],'frame')]
                if active:elements += [cube([5,4,7],[11,27,9],'rift'),cube([7,2,6.5],[9,29,9.5],'arrow')]
            else:
                elements += [cube([7,3,4],[9,5,12],'arrow'),cube([5,3,10],[11,5,12],'arrow'),cube([7,3,12],[9,5,15],'arrow')]
            textures={'frame':'minecraft:block/polished_deepslate','arrow':'minecraft:block/sea_lantern' if active else 'minecraft:block/gray_concrete','rift':'minecraft:block/purple_concrete','particle':'minecraft:block/polished_deepslate'}
            write(ASSETS/f'models/block/{model}.json',{'textures':textures,'elements':elements})
            for rotation in range(4):variants[f'active={str(active).lower()},rotation={rotation}']={'model':'coraline:block/'+model,'y':rotation*90}
        write(ASSETS/f'blockstates/{block}.json',{'variants':variants})
        write(DATA/f'loot_tables/blocks/{block}.json',{'type':'minecraft:block','pools':[]})
    path=ASSETS/'lang/en_us.json';value=json.loads(path.read_text(encoding='utf-8-sig'))
    value.update({'structure.coraline.dimensional_seam':'Dimensional Seam','block.coraline.dimensional_seam_core':'Dimensional Seam','block.coraline.dimensional_seam_dial':'Seam Alignment Dial',
      'message.coraline.seam_unavailable':'The seam cannot be operated safely right now.',
      'message.coraline.seam_closed':'This dimensional seam is permanently closed.',
      'message.coraline.seam_alignment':'Turn each arrow toward its illuminated floor marker, then close the seam here.',
      'message.coraline.seam_contained':'Seam closed. Breach reduced by up to 0.15.',
      'message.coraline.seam_closed_no_credit':'Seam closed. Containment was unavailable; no later fuel payment is pending.'})
    write(path,value)


if __name__=='__main__':main()
