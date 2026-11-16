"""Small native infection models; preserve all terrain and recording masters."""
import json
from write_endless_carnival_resources import DATA,ASSETS
from write_grand_theatre_resources import write,cube
def main():
    textures={'grass':'grass_block_top','soil':'dirt','planks':'oak_planks','leaves':'oak_leaves','stone':'stone','sand':'sand','gravel':'gravel','base':'obsidian'}
    for kind,texture in textures.items():
        name='infected_'+kind
        # A readable natural base with contrasting stitched seams, using the
        # original vanilla textures rather than shipping enlarged duplicates.
        elements=[cube([0,0,0],[16,16,16],'base')]
        for axis in range(3):
            low=[7,7,7];high=[9,9,9];low[axis]=-.01;high[axis]=16.01
            elements.append(cube(low,high,'seam'))
        elements.append(cube([2,15.99,2],[14,16.02,3],'seam'))
        write(ASSETS/f'models/block/{name}.json',{'textures':{'base':'minecraft:block/'+texture,'seam':'minecraft:block/purple_concrete','particle':'minecraft:block/'+texture},'elements':elements})
        write(ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':'coraline:block/'+name}}})
        write(DATA/f'loot_tables/blocks/{name}.json',{'type':'minecraft:block','pools':[]})
    write(ASSETS/'models/block/infection_heart.json',{'textures':{'shell':'minecraft:block/crying_obsidian','core':'minecraft:block/redstone_block','particle':'minecraft:block/crying_obsidian'},'elements':[cube([0,0,0],[16,16,16],'shell'),cube([5,-.01,5],[11,16.01,11],'core'),cube([-.01,5,5],[16.01,11,11],'core'),cube([5,5,-.01],[11,11,16.01],'core')]})
    write(ASSETS/'blockstates/infection_heart.json',{'variants':{'':{'model':'coraline:block/infection_heart'}}})
    write(DATA/'loot_tables/blocks/infection_heart.json',{'type':'minecraft:block','pools':[]})
    path=ASSETS/'lang/en_us.json';v=json.loads(path.read_text(encoding='utf-8-sig'))
    v['message.coraline.breach_reinforcements']='An Infection Heart is calling reinforcements nearby.'
    v['block.coraline.infection_heart']='Infection Heart'
    for kind in textures:v['block.coraline.infected_'+kind]='Infected '+{'soil':'Soil','base':'Matter'}.get(kind,kind.title())
    write(path,v)
if __name__=='__main__':main()
