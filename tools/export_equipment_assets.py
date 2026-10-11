"""Export saved imagegen assets without repainting them; build models and a review contact sheet."""
from pathlib import Path
import json,shutil,subprocess
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/coraline'
all_sources={}
for group in ('armor','advanced_armor','foods','materials','cotton_stages','weapons'):
    manifest=ROOT/'artwork'/f'{group}_sources.json'
    if not manifest.exists():continue
    sources=json.loads(manifest.read_text());all_sources.update(sources)
    originals=ROOT/'artwork'/group;originals.mkdir(exist_ok=True)
    for name,source in sources.items():
        if (A/'sculpted'/(name+'.json')).exists():continue
        # Armor now has model-matched Blender artwork. Never restore retired knight illustrations.
        if (ROOT/'artwork/tailored_wardrobe'/f'{name}.png').exists():continue
        original=originals/f'{name}.png'
        if not original.exists():shutil.copy2(source,original)
        target=A/'textures/item'/original.name
        if not target.exists() or original.stat().st_mtime>target.stat().st_mtime:
            subprocess.run(['ffmpeg','-v','error','-y','-i',str(original),'-vf','scale=128:128:flags=neighbor',str(target)],check=True)
        with Image.open(target) as im:
            assert im.mode=='RGBA' and im.getextrema()[3][0]==0,f'{name} lacks transparent pixels'
        tool=name.endswith(('_sword','_pickaxe','_axe','_shovel','_hoe')) or name in ('needle_rapier','moonblade','vine_sword','button_mace','midnight_wand','spidervision_sword','thread_whip')
        model={'parent':'minecraft:item/handheld' if tool else 'minecraft:item/generated','textures':{'layer0':f'coraline:item/{name}'}}
        if name=='cotton_candy':model['overrides']=[{'predicate':{'coraline:bites':i},'model':f'coraline:item/cotton_candy_{i}'} for i in (1,2)]
        (A/'models/item'/f'{name}.json').write_text(json.dumps(model,indent=2)+'\n')
langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text())
for name in all_sources:
    if name not in ('cotton_candy_1','cotton_candy_2'):lang['item.coraline.'+name]=name.replace('_',' ').title()
lang['tooltip.coraline.cotton_candy']='%s bites remaining. Restores hunger with each bite.'
lang['tooltip.coraline.popcorn']='Hold right-click to nibble. %s servings remaining.'
lang['tooltip.coraline.bottomless_candy_bag']='Never consumed. Restores hunger with very low saturation.'
for name,tip in {'needle_rapier':'3 attack damage. Very fast attack speed.', 'moonblade':'6 attack damage by day; 12 at night.', 'vine_sword':'Poisons and slows enemies for four seconds.', 'button_mace':'Very slow, heavy strikes. Fully charged hits splash nearby enemies.', 'midnight_wand':'Fires a %s-damage magic bolt. %s-second cooldown.', 'spidervision_sword':'Successful mob hits create temporary cobwebs in suitable empty space.', 'thread_whip':'5 attack damage, extended reach, and pulls mobs toward you. %s-second cooldown. Attack or right-click to lash.'}.items():lang['tooltip.coraline.'+name]=tip
langpath.write_text(json.dumps(lang,indent=2)+'\n')

# Standard recipes for the explicit crafting materials; no ore/world-generation edits.
recipes=ROOT/'src/main/resources/data/coraline/recipes'
patterns={'helmet':['MMM','M M'],'chestplate':['M M','MMM','MMM'],'leggings':['MMM','M M','M M'],'boots':['M M','M M'],'sword':['M','M','S'],'pickaxe':['MMM',' S ',' S '],'axe':['MM','MS',' S'],'shovel':['M','S','S'],'hoe':['MM',' S',' S']}
for material,item in [('other_diamond','other_diamond'),('other_iron','other_iron_ingot'),('moonstone','moonstone')]:
    for part,pattern in patterns.items():
        if material=='moonstone' and part not in ('helmet','chestplate','leggings','boots'):continue
        key={'M':{'item':'coraline:'+item}}
        if any('S' in row for row in pattern):key['S']={'item':'minecraft:stick'}
        recipe={'type':'minecraft:crafting_shaped','category':'equipment','pattern':pattern,'key':key,'result':{'item':f'coraline:{material}_{part}'}}
        (recipes/f'{material}_{part}.json').write_text(json.dumps(recipe,indent=2)+'\n')

# Contact sheet is QA-only, never used as an item texture.
names=list(all_sources);columns=8;width=200;height=170
sheet=Image.new('RGB',(columns*width,((len(names)+columns-1)//columns)*height),'#343841');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    x=(i%columns)*width;y=(i//columns)*height
    with Image.open(A/'textures/item'/f'{name}.png') as im:sheet.paste(im,(x+36,y+3),im)
    label=name.replace('_',' ')
    while len(label)>28:
        cut=label.rfind(' ',0,28);cut=cut if cut>0 else 28;draw.text((x+5,y+133),label[:cut],fill='white');label=label[cut:].strip();y+=12
    draw.text((x+5,y+133),label,fill='white')
sheet.save(ROOT/'artwork/equipment_contact_sheet.png')
print(f'Exported {len(names)} transparent icons, models, translations and crafting recipes.')
