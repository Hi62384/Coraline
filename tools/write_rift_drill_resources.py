"""Material/tool/facility source resources; no verification-only launch.
Preserves image masters and merges the shared artwork/language manifests.
"""
from pathlib import Path
import hashlib
import json
import shutil
from apply_loot_recipe_overhaul import canonical

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
DATA=RES/'data/coraline'
ASSETS=RES/'assets/coraline'
IMAGE=Path('C:/Users/User1/.codex/generated_images/01a0e5a1-6507-7363-8a26-a56f9f230b40/exec-32db1bd1-e9ad-4fe4-8187-8a33e28ddde3.png')
PROMPT='Single isolated 2D inventory sprite for a Minecraft Coraline mod: Other Iron Drill Disruptor, a compact handheld supernatural sabotage tool. Distinctive forked pale silver iron probe at the upper right, a small contained pale blue Moonstone coil in its middle, short black wrapped grip at the lower left, tiny restrained brass fittings. A practical industrial electrical disruptor, not a gun, sword, lockpick, scissors or drill. Crisp pixel-art stepped edges and readable silhouette for 256-pixel game inventory export, restrained metal highlights, entire tool centered diagonally with clear transparent margins. One tool only, no text, no scenery, no border, no background, no cast shadow, no fake checkerboard; true transparent RGBA background. Flat game sprite, no perspective scene or 3D render.'

def fresh(path,value):
    data=(json.dumps(value,indent=2)+'\n').encode()
    if path.exists() and path.read_bytes()!=data:raise ValueError('Existing different source must be reviewed: '+str(path))
    path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)

def main():
    recipe={'type':'minecraft:crafting_shaped','category':'equipment','pattern':[' I ','IMI',' S '],
            'key':{'I':{'item':'coraline:other_iron_ingot'},'M':{'item':'coraline:moonstone'},'S':{'item':'minecraft:stick'}},
            'result':{'item':'coraline:other_iron_drill_disruptor','count':1}}
    for path in (DATA/'recipes').glob('*.json'):
        old=json.loads(path.read_text(encoding='utf-8-sig'))
        if canonical(old)==canonical(recipe) and old['result']!=recipe['result']:raise ValueError('Recipe conflict: '+str(path))
    master=ROOT/'artwork/infiltration/other_iron_drill_disruptor.png';runtime=ASSETS/'textures/item/other_iron_drill_disruptor.png';digest=hashlib.sha256(IMAGE.read_bytes()).hexdigest()
    for target in [master,runtime]:
        if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest()!=digest:raise ValueError('Different existing art: '+str(target))
        target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(IMAGE,target)
    manifest=ROOT/'artwork/infiltration/manifest.json';m=json.loads(manifest.read_text())
    if not any(a['id']=='other_iron_drill_disruptor' for a in m['assets']):m['assets'].append({'id':'other_iron_drill_disruptor','generated_source':IMAGE.as_posix(),'master':master.relative_to(ROOT).as_posix(),'runtime_source':runtime.relative_to(ROOT).as_posix(),'sha256':digest,'prompt':PROMPT,'inspection':'Built-in output inspected: forked iron probe, blue coil and wrapped grip; native game appearance untested.'})
    manifest.write_text(json.dumps(m,indent=2)+'\n')
    fresh(DATA/'recipes/other_iron_drill_disruptor.json',recipe)
    fresh(DATA/'advancements/recipes/equipment/other_iron_drill_disruptor.json',{'parent':'minecraft:recipes/root','criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':['coraline:other_iron_ingot']}]}}},'requirements':[['has_material']],'rewards':{'recipes':['coraline:other_iron_drill_disruptor']}})
    fresh(ASSETS/'models/item/other_iron_drill_disruptor.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'coraline:item/other_iron_drill_disruptor'}})
    fresh(DATA/'worldgen/structure/rift_drill_center.json',{'type':'coraline:rift_drill_center','biomes':['coraline:postgame/white_rift_wastes','coraline:postgame/pale_wasteland','coraline:postgame/otherworld_badlands'],'step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'none'})
    path=DATA/'worldgen/structure_set/infinite_sites.json';placement=json.loads(path.read_text());entry={'structure':'coraline:rift_drill_center','weight':1}
    if not any(v['structure']==entry['structure'] for v in placement['structures']):placement['structures'].append(entry)
    path.write_text(json.dumps(placement,indent=2)+'\n')
    def pool(name,lo,hi,chance=None):
        result={'rolls':1,'entries':[{'type':'minecraft:item','name':name,'weight':1,'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':lo,'max':hi}}]}]}
        if chance is not None:result['conditions']=[{'condition':'minecraft:random_chance','chance':chance}]
        return result
    fresh(DATA/'loot_tables/postgame/structures/rift_drill_center.json',{'type':'minecraft:chest','pools':[
        pool('coraline:button',3,8),pool('coraline:other_iron_ingot',5,12),pool('coraline:moonstone',1,3),pool('coraline:other_diamond',1,2,.45),pool('coraline:soulglass',1,2,.3),pool('coraline:perfect_ingot',1,1,.06),pool('coraline:midnight_crossbow',1,1,.035)]})
    faces={face:{'texture':'#metal'} for face in ['north','south','east','west','up','down']};elements=[]
    for x in [2,6,10,14]:elements.append({'from':[x-.5,0,7.5],'to':[x+.5,16,8.5],'faces':faces})
    for z in [2,6,10,14]:elements.append({'from':[7.5,0,z-.5],'to':[8.5,16,z+.5],'faces':faces})
    for y in [3,13]:elements.extend([{'from':[0,y-.5,7.5],'to':[16,y+.5,8.5],'faces':faces},{'from':[7.5,y-.5,0],'to':[8.5,y+.5,16],'faces':faces}])
    fresh(ASSETS/'models/block/rift_security_gate.json',{'textures':{'metal':'minecraft:block/iron_block','particle':'minecraft:block/iron_block'},'elements':elements})
    fresh(ASSETS/'models/block/rift_security_gate_open.json',{'textures':{'particle':'minecraft:block/iron_block'},'elements':[]})
    for block in ['rift_drill_core','rift_drill_conduit','rift_security_gate']:
        variants={}
        for active in ['false','true']:
            for opened in ['false','true']:
                model=block+('_open' if block=='rift_security_gate' and opened=='true' else '')
                variants['active='+active+',open='+opened]={'model':'coraline:block/'+model}
        fresh(ASSETS/f'blockstates/{block}.json',{'variants':variants})
        if block!='rift_security_gate':fresh(ASSETS/f'models/block/{block}.json',{'parent':'builtin/entity','textures':{'particle':'minecraft:block/polished_deepslate'}})
    lang=ASSETS/'lang/en_us.json';words=json.loads(lang.read_text(encoding='utf-8-sig'));words.update({
        'item.coraline.other_iron_drill_disruptor':'Other Iron Drill Disruptor',
        'block.coraline.rift_drill_core':'Rift Drill','block.coraline.rift_drill_conduit':'Rift Power Conduit','block.coraline.rift_security_gate':'Rift Security Gate',
        'message.coraline.rift_security_locked':'The security gate needs an Other Iron Lockpick.',
        'message.coraline.rift_disruptor_required':'Use an Other Iron Drill Disruptor on this machinery.',
        'message.coraline.rift_sabotage_sequence':'Unlock security, disable both conduits, then disrupt the drill.',
        'message.coraline.rift_drill_disabled':'The Rift Drill is permanently disabled.',
        'message.coraline.rift_conduit_disabled':'Power conduit severed.'})
    lang.write_text(json.dumps(words,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print('Rift facility, shared loot, Disruptor acquisition and exact original artwork sources written. Runtime export/gameplay untested.')

if __name__=='__main__':main()
