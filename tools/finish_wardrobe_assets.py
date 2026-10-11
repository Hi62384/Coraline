"""Export model-matched Blender icons, factual tooltips, and an original synthesized whip crack."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,math,random,wave,struct,subprocess
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/coraline'
ART=ROOT/'artwork/tailored_wardrobe'
langpath=A/'lang/en_us.json';lang=json.loads(langpath.read_text())
tips={
 'circus_jacket':'Increases jump height.', 'top_hat':'Provides 2 armor points.',
 'set.comfy_silk':'Full set: 6 armor points.', 'set.beldam_weave':'Full set: 30 armor points.',
 'set.potent_beldam_weave':'Full set: 36 armor points.', 'set.other_world_void':'Full set: 50 armor points.',
 'set.moonstone':'Full set: 20 armor points and 2 armor toughness.',
 'needle_rapier':'3 attack damage. Very fast attack speed.',
 'midnight_wand':'Fires a %s-damage magic bolt. %s-second cooldown.',
 'thread_whip':'5 attack damage, extended reach, and pulls mobs toward you. %s-second cooldown. Attack or right-click to lash.',
 'cotton_candy':'%s bites remaining. Restores hunger with each bite.',
 'popcorn':'Hold right-click to eat continuously. %s servings remaining.',
 'bottomless_candy_bag':'Never consumed. Restores hunger with very low saturation.'}
for name,tip in tips.items():lang['tooltip.coraline.'+name]=tip
lang['subtitles.coraline.thread_whip']='Whip cracks'
langpath.write_text(json.dumps(lang,indent=2)+'\n')
names=[]
for path in sorted((A/'wardrobe').glob('*.json')):
    for piece in ('helmet','chestplate','leggings','boots'):
        name=path.stem+'_'+piece;names.append(name)
        if (A/'sculpted'/(name+'.json')).exists():continue
        image=Image.open(ART/(name+'.png')).convert('RGBA')
        assert image.getbbox() and image.getextrema()[3][0]==0,name
        # Rendered models, not retouched illustrations. Alpha remains intact.
        image.resize((128,128),Image.Resampling.LANCZOS).save(A/'textures/item'/f'{name}.png')
        (A/'models/item'/f'{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/'+name}},indent=2)+'\n')
sheet=Image.new('RGB',(8*160,10*158),'#30333e');draw=ImageDraw.Draw(sheet)
for i,name in enumerate(names):
    x=i%8*160;y=i//8*158;im=Image.open(A/'textures/item'/f'{name}.png');sheet.paste(im,(x+16,y),im)
    label=name.replace('_',' ');draw.text((x+3,y+130),label[:26],fill='white');draw.text((x+3,y+142),label[26:],fill='white')
sheet.save(ART/'icon_catalog.png')
# Original short air swish -> sharp braided-cord snap -> a quiet decay, no borrowed audio.
rate=44100;rng=random.Random(1492);values=[];prev=0
for i in range(int(rate*.47)):
    t=i/rate;noise=rng.uniform(-1,1);high=noise-prev;prev=noise
    swish=.18*math.sin(math.pi*min(1,t/.3))**3*noise
    snap=0 if t<.245 else .6*math.exp(-(t-.245)*95)*high
    tail=0 if t<.25 else .11*math.exp(-(t-.25)*18)*noise
    values.append(max(-.98,min(.98,swish+snap+tail)))
wav=ART/'thread_whip_source.wav'
with wave.open(str(wav),'wb') as out:out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate);out.writeframes(b''.join(struct.pack('<h',int(v*32767)) for v in values))
subprocess.run(['ffmpeg','-v','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(A/'sounds/thread_whip.ogg')],check=True)
soundpath=A/'sounds.json';sounds=json.loads(soundpath.read_text());sounds['thread_whip']={'subtitle':'subtitles.coraline.thread_whip','sounds':['coraline:thread_whip']};soundpath.write_text(json.dumps(sounds,indent=2)+'\n')
print('76 model-matched icons; ability-only tooltips; original whip audio exported.')
