"""Legacy vanilla armor UV generator. Superseded by build_tailored_wardrobe.py.
Do not run for the current wardrobe: it is retained only to reproduce archived 1.10 artwork.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import json,random

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/coraline'
SETS={
 'dreamweave':('#789ebb','#e6ecf7','Each piece increases movement speed by 10%.'),
 'comfy_silk':('#eee3d2','#a56971','Fine embroidered silk, tailored for comfort.'),
 'midnight':('#263857','#b7d7ed','Full set: leap very high and fall slowly at night.'),
 'buttonguard':('#356565','#c09859','Each piece reflects 10% of damage taken back to the attacker.'),
 'midnight_cloak':('#282c40','#5b648a','Full set: greatly reduces normal mob detection at night. Retaliation still applies.'),
 'luck':('#348452','#e7c76c','Full set: adds two Looting levels to eligible mob drops.'),
 'garden_vines':('#4a704b','#be9673','Full set: 50% chance to poison and slow an attacker for 4 seconds.'),
 'moonstone':('#a9c9d7','#eaf2ed','Moonstone protection: diamond armor and toughness.'),
 'miners':('#826246','#c59159','Full set: doubles ordinary mined block drops. Containers and tagged items are excluded.'),
 'spidervision':('#342c46','#b5abbf','Full set: move quickly through webs. Enemy attacks may create temporary cobwebs.'),
 'beldam_weave':('#dbd2bf','#8b784c','A complete weave grants 30 armor points.'),
 'soulbound':('#255863','#a0e5df','Full set: restore half a heart on mob hits and three hearts on kills.'),
 'super_spidervision':('#252133','#c4b6ce','Full set: web speed, defensive webs and slow wall climbing. Hold jump to climb webs or grip a ceiling; sneak to descend or release.'),
 'potent_beldam_weave':('#502337','#c6a468','A complete potent weave grants 36 armor points.'),
 'soulbound_potent_beldam_weave':('#292e40','#80d6d8','43 total armor. Full set: restore half a heart on mob hits and three hearts on kills.'),
 'other_world_void':('#202034','#998cc7','A complete void set grants 50 armor points.'),
 'other_diamond':('#378fbc','#b6eff5','22 total armor, improved toughness and durability over diamond.'),
 'other_iron':('#7f8e9f','#d3dfeb','18 total armor with improved durability over iron.'),
 'perfect':('#e2e5df','#d9b657','28 total armor, toughness 3 and exceptional durability.')}
langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text())
def rgb(s):return tuple(bytes.fromhex(s[1:]))
def shade(c,n):return tuple(max(0,min(255,v+n)) for v in c)+(255,)
for index,(name,(base,trim,tip)) in enumerate(SETS.items()):
    base,trim=rgb(base),rgb(trim)
    for layer in (1,2):
        im=Image.new('RGBA',(64,32));d=ImageDraw.Draw(im);rng=random.Random(1720+index)
        def rect(box,c):d.rectangle(box,fill=c if len(c)==4 else c+(255,))
        def cloth(x,y,w,h,c):
            for yy in range(h):
                for xx in range(w):rect((x+xx,y+yy,x+xx,y+yy),shade(c,rng.choice([-2,0,0,1])+(-16 if xx==0 else 12 if xx==w-1 else 0)))
        def cube(u,v,w,h,z,c):
            cloth(u+z,v,w,z,c);cloth(u+z+w,v,w,z,c)
            for x,width,offset in [(u,z,-8),(u+z,w,0),(u+z+w,z,-12),(u+2*z+w,w,-4)]:cloth(x,v+z,width,h,tuple(max(0,k+offset) for k in c))
        cube(0,0,8,8,8,base);cube(16,16,8,12,4,base);cube(40,16,4,12,4,base);cube(0,16,4,12,4,base)
        # Open helmet face and polished rim; never hide the player's eyes behind an opaque square.
        rect((9,11,14,15),(0,0,0,0));rect((8,9,15,10),trim)
        rect((8,11,8,15),trim);rect((15,11,15,15),trim)
        for y in (20,30):rect((20,y,27,y),trim)
        rect((23,21,24,29),shade(base,-22))
        for y in (22,25,28):rect((24,y,24,y),trim)
        for u in (0,40):
            rect((u,29,u+15,30),trim)
            rect((u+4,21,u+4,28),shade(base,24))
        if layer==2:
            rect((16,20,39,22),shade(base,-23));rect((22,20,25,22),trim)
            for x in (5,13):rect((x,25,x+1,27),shade(base,-18))
        if name in ('dreamweave','midnight','moonstone'):
            for x,y in [(21,24),(26,26),(34,24),(37,28),(46,24),(6,24)]:
                rect((x,y-1,x,y+1),trim);rect((x-1,y,x+1,y),trim)
            if name=='moonstone':
                for x,y in [(20,21),(25,25),(32,23),(45,21),(5,22)]:
                    d.polygon([(x,y),(x+2,y+1),(x+1,y+5)],fill=shade(trim,-4))
        if name=='comfy_silk':
            for x in range(20,28):rect((x,23+(x%2),x,23+(x%2)),trim)
            for x in (21,26):rect((x,26,x,29),trim)
            rect((22,29,25,29),trim)
        if name=='buttonguard':
            for x,y in [(21,23),(25,23),(21,27),(25,27),(34,24)]:
                rect((x-1,y-1,x+1,y+1),(25,29,34));rect((x,y,x,y),trim)
        if name=='luck':
            for x,y in [(23,25),(35,25),(46,25)]:
                for dx,dy in [(-1,-1),(1,-1),(-1,1),(1,1)]:rect((x+dx,y+dy,x+dx,y+dy),trim)
                rect((x,y,x,y+2),trim)
        if name=='garden_vines':
            for x in (21,26,34,46,6):
                for y in range(21,30):
                    xx=x+y%2;rect((xx,y,xx,y),(110,148,69))
                    if y%3==0:rect((xx-1,y,xx-1,y),(159,180,91))
                rect((x,25,x+1,25),(230,154,173))
        if name=='miners':
            for x in (20,26):rect((x,21,x+1,29),trim)
            rect((21,27,26,29),shade(base,-30));rect((23,27,24,28),trim)
            rect((11,8,12,9),(248,224,140))
        if name=='midnight_cloak':
            for x in (21,26,33,37):rect((x,21,x,29),shade(base,18))
        if 'spidervision' in name:
            for origin in (23,35):
                for dx,dy in [(-3,-3),(3,-3),(-3,3),(3,3),(0,-4),(0,4)]:d.line((origin,25,origin+dx,25+dy),fill=trim+(255,))
                d.rectangle((origin-2,23,origin+2,27),outline=trim+(255,))
            rect((11,9,12,9),(190,60,73))
        if 'beldam' in name:
            for y in (23,27):
                rect((22,y-1,25,y+1),(24,25,31));rect((23,y,24,y),trim)
            for x in range(20,28):rect((x,29+x%2,x,29+x%2),trim)
        if 'soulbound' in name:
            d.polygon([(24,22),(26,25),(24,28),(22,25)],fill=trim+(255,))
            rect((24,24,24,25),(224,255,246))
        if name=='other_world_void':
            for x,y in [(21,23),(26,28),(35,24),(38,29),(46,23),(6,27),(10,9)]:rect((x,y,x,y),(212,205,242))
            d.line((20,21,23,25,21,28),fill=trim+(255,))
        path=A/'textures/models/armor'/f'{name}_{layer}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
    for part in ('helmet','chestplate','leggings','boots'):
        item=f'{name}_{part}'
        (A/'models/item'/f'{item}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'coraline:item/{item}'}},indent=2)+'\n')
        lang['item.coraline.'+item]=item.replace('_',' ').title()
    lang['tooltip.coraline.set.'+name]=tip

# The goggles' head strap and raised lenses use their own compact UV layout.
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im)
d.rectangle((0,0,35,11),fill='#283450')
for x in (0,12):
    d.rectangle((x,16,x+9,21),fill='#c0d1da')
    d.rectangle((x+2,18,x+4,20),fill='#83c9e3')
    d.point((x+2,18),fill='#edfaff')
d.rectangle((24,16,29,19),fill='#9baabb')
im.save(A/'textures/models/armor/midnight_goggles.png')
(A/'models/item/midnight_goggles.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/midnight_goggles'}},indent=2)+'\n')
lang['item.coraline.midnight_goggles']='Midnight Goggles'
lang['tooltip.coraline.midnight_goggles']='Night vision while worn, including underground.'
im=Image.new('RGBA',(64,64));d=ImageDraw.Draw(im);d.rectangle((0,0,14,15),fill='#171820');d.rectangle((16,0,20,4),fill='#d9d0ad')
im.save(A/'textures/models/armor/unbinding_button_eyes.png')
(A/'models/item/unbinding_button_eyes.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/unbinding_button_eyes'}},indent=2)+'\n')
lang['item.coraline.unbinding_button_eyes']='Unbinding Button Eyes'
lang['tooltip.coraline.unbinding_button_eyes']='Reveals nearby loaded mobs through walls while worn.'
langpath.write_text(json.dumps(lang,indent=2)+'\n')
print(f'Exported {len(SETS)*2} native armor layers, headwear textures and {len(SETS)*4+2} item definitions.')
