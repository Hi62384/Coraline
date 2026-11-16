"""Author the remaining ordinary-equivalent postgame ores and processing.
Uses code-native vein geometry and existing stone/mineral atlases; no new
bitmap, master conversion, or replacement of another resource's tag entries.
"""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
ROWS={
    'coal':('coal','coal',1,1,'porcelain_stone','minecraft:block/coal_block',None),
    'copper':('raw_copper','copper_ingot',2,5,'threadbound_rock','minecraft:block/copper_block','stone'),
    'gold':('raw_gold','gold_ingot',1,1,'clockwork_stone','minecraft:block/gold_block','iron'),
    'redstone':('redstone','redstone',4,5,'clockwork_stone','minecraft:block/redstone_block','iron'),
    'lapis':('lapis_lazuli','lapis_lazuli',4,9,'mirrorstone','minecraft:block/lapis_block','stone'),
    'emerald':('emerald','emerald',1,1,'pale_rift_stone','minecraft:block/emerald_block','iron'),
    'quartz':('quartz','quartz',1,1,'voidstone','minecraft:block/quartz_block_top',None),
}
def put(path,value):
    p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def merge_tag(path,values):
    p=ROOT/path;n=json.loads(p.read_text(encoding='utf-8-sig')) if p.exists() else {'replace':False,'values':[]}
    for value in values:
        if value not in n['values']:n['values'].append(value)
    put(path,n)
def main():
    words=json.loads((ROOT/'assets/coraline/lang/en_us.json').read_text(encoding='utf-8-sig'))
    for i,(name,row) in enumerate(ROWS.items()):
        drop,processed,lo,hi,stone,mineral,tier=row;ore='other_'+name+'_ore';identifier='coraline:'+ore
        words['block.coraline.'+ore]='Other '+name.title()+' Ore'
        # A solid supernatural rock with narrow, angular stitched veins on
        # every face. No overlay holes or missing back faces at chunk borders.
        faces=['north','south','east','west','up','down']
        elements=[{'from':[0,0,0],'to':[16,16,16],'faces':{f:{'texture':'#stone','cullface':f} for f in faces}}]
        for face in faces:
            for branch in range(4):
                u=2+((branch*3+i)%11);v=2+((branch*5+i*2)%11);du=1 if branch%2 else 3;dv=4 if branch%2 else 1
                if face in ['north','south']:a=[u,v,-.01 if face=='north' else 16.01];b=[u+du,v+dv,a[2]]
                elif face in ['east','west']:a=[-.01 if face=='west' else 16.01,v,u];b=[a[0],v+dv,u+du]
                else:a=[u,-.01 if face=='down' else 16.01,v];b=[u+du,a[1],v+dv]
                elements.append({'from':a,'to':b,'faces':{face:{'texture':'#vein','uv':[2,2,14,14],'cullface':face}}})
        put('assets/coraline/models/block/'+ore+'.json',{'parent':'minecraft:block/block','textures':{'particle':'coraline:block/postgame/'+stone,'stone':'coraline:block/postgame/'+stone,'vein':mineral},'elements':elements})
        put('assets/coraline/models/item/'+ore+'.json',{'parent':'coraline:block/'+ore})
        put('assets/coraline/blockstates/'+ore+'.json',{'variants':{'':{'model':'coraline:block/'+ore}}})
        functions=[]
        if hi>1:functions.append({'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}})
        functions.append({'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:uniform_bonus_count' if name=='redstone' else 'minecraft:ore_drops','parameters':{'bonusMultiplier':1}} if name=='redstone' else {'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:ore_drops'})
        functions.append({'function':'minecraft:explosion_decay'})
        put('data/coraline/loot_tables/blocks/'+ore+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[{'type':'minecraft:item','name':identifier,'conditions':[{'condition':'minecraft:match_tool','predicate':{'enchantments':[{'enchantment':'minecraft:silk_touch','levels':{'min':1}}]}}]},{'type':'minecraft:item','name':'minecraft:'+drop,'functions':functions}]}]}]})
        for kind,duration in [('smelting',200),('blasting',100)]:
            recipe=ore+'_from_'+kind
            put('data/coraline/recipes/'+recipe+'.json',{'type':'minecraft:'+kind,'category':'misc','ingredient':{'item':identifier},'result':'minecraft:'+processed,'experience':.7 if name in ['gold','redstone','emerald'] else .2,'cookingtime':duration})
            put('data/coraline/advancements/recipes/misc/'+recipe+'.json',{'parent':'minecraft:recipes/root','criteria':{'has_ore':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':[identifier]}]}}},'rewards':{'recipes':['coraline:'+recipe]}})
        merge_tag('data/minecraft/tags/blocks/mineable/pickaxe.json',[identifier])
        if tier:merge_tag('data/minecraft/tags/blocks/needs_'+tier+'_tool.json',[identifier])
        for category in ['blocks','items']:
            merge_tag('data/forge/tags/'+category+'/ores/'+name+'.json',[identifier])
            merge_tag('data/forge/tags/'+category+'/ores.json',['#forge:ores/'+name])
    put('assets/coraline/lang/en_us.json',words)

if __name__=='__main__':main()
