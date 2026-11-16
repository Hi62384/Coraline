"""Author compact Mirror resources, merging shared language data in place."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def put(name,value):
    path=ROOT/name
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')

def main():
    put('data/coraline/recipes/magic_mirror.json',{'type':'minecraft:crafting_shaped','category':'decorations','pattern':['DGD','GPG','DGD'],'key':{'D':{'item':'coraline:other_diamond'},'G':{'item':'minecraft:glass'},'P':{'item':'minecraft:polished_deepslate'}},'result':{'item':'coraline:magic_mirror'}})
    put('data/coraline/advancements/recipes/decorations/magic_mirror.json',{'parent':'minecraft:recipes/root','criteria':{'has_diamond':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['coraline:other_diamond']}]}}},'rewards':{'recipes':['coraline:magic_mirror']}})
    put('data/coraline/loot_tables/blocks/magic_mirror.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'coraline:magic_mirror','conditions':[{'condition':'minecraft:block_state_property','block':'coraline:magic_mirror','properties':{'half':'lower'}}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    put('assets/coraline/models/block/magic_mirror.json',{'parent':'builtin/entity','textures':{'particle':'minecraft:block/dark_oak_planks'}})
    put('assets/coraline/blockstates/magic_mirror.json',{'variants':{'':{'model':'coraline:block/magic_mirror'}}})
    elements=[]
    def box(a,b,tex):
        elements.append({'from':a,'to':b,'faces':{face:{'texture':'#'+tex} for face in ['north','south','east','west','up','down']}})
    box([2,0,7],[4,16,9],'wood');box([12,0,7],[14,16,9],'wood');box([4,0,7],[12,2,9],'wood');box([4,14,7],[12,16,9],'wood')
    box([4,2,7.5],[12,14,8.5],'silver');box([3.5,2,7],[4,14,7.5],'gold');box([12,2,7],[12.5,14,7.5],'gold');box([4,13.5,7],[12,14,7.5],'gold');box([4,2,7],[12,2.5,7.5],'gold')
    put('assets/coraline/models/item/magic_mirror.json',{'parent':'minecraft:block/block','textures':{'wood':'minecraft:block/dark_oak_planks','silver':'minecraft:block/amethyst_block','gold':'minecraft:block/gold_block','particle':'minecraft:block/dark_oak_planks'},'elements':elements,'display':{'gui':{'rotation':[10,-25,0],'scale':[.9,.9,.9]},'firstperson_righthand':{'rotation':[0,-30,0],'translation':[0,0,-2],'scale':[.6,.6,.6]}}})
    path=ROOT/'assets/coraline/lang/en_us.json';words=json.loads(path.read_text(encoding='utf-8-sig'))
    words.update({'block.coraline.magic_mirror':'Magic Mirror','message.coraline.mirror.access':'You cannot use this mirror here.','gui.coraline.mirror.confirm':'Duplicate','gui.coraline.mirror.select':'Select an inventory item','gui.coraline.mirror.price':'Other Diamonds: %s','gui.coraline.mirror.moonstone':'Moonstone: 1'})
    for i,value in enumerate(['Ready to duplicate','Choose an item below','This item is restricted','Unsupported item data','Not enough currency','Make inventory space','Duplicate received','Transfer save is paused','Wait a moment','Select again to confirm']):words['gui.coraline.mirror.status.'+str(i)]=value
    put('assets/coraline/lang/en_us.json',words)

    put('data/coraline/recipes/perfect_essence.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':'coraline:perfect_ingot'}],'result':{'item':'coraline:perfect_essence','count':16}})
    put('data/coraline/advancements/recipes/misc/perfect_essence.json',{'parent':'minecraft:recipes/root','criteria':{'has_ingot':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['coraline:perfect_ingot']}]}}},'rewards':{'recipes':['coraline:perfect_essence']}})
    # A compact golden dust sprite reuses Minecraft's existing atlas asset.
    put('assets/coraline/models/item/perfect_essence.json',{'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/glowstone_dust'}})
    words.update({'item.coraline.perfect_essence':'Perfect Essence','key.coraline.perfect_barrier':'Perfect Barrier','tooltip.coraline.perfect_power':'Carry Perfect Essence to empower equipped gear. 1 Essence repairs 10 durability.','tooltip.coraline.perfect_ability':'Special ability: 1 Essence. Tools: sneak + right-click; weapons: right-click.','tooltip.coraline.perfect_armor':'Full set: press the Perfect Barrier key (default V). Costs 1 Essence.'})
    put('assets/coraline/lang/en_us.json',words)

if __name__=='__main__':main()
