"""Small pixel inventory camera, following the existing native item asset workflow."""
from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/coraline'
im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
d.rectangle((1,4,14,12),fill='#26373D');d.rectangle((2,5,13,11),fill='#657675')
d.rectangle((3,2,6,4),fill='#293E44');d.rectangle((10,3,12,4),fill='#B64135')
d.rectangle((9,5,12,6),fill='#E9E4BE');d.rectangle((2,5,3,10),fill='#26373D')
d.rectangle((5,6,10,11),fill='#253238');d.rectangle((6,7,9,10),fill='#9CBBBD');d.rectangle((7,8,9,10),fill='#283F4A');d.point((7,8),fill='#E7F2DD')
im.save(A/'textures/item/camera.png')
(A/'models/item/camera.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/camera'}},indent=2)+'\n',encoding='utf8')
lang=json.loads((A/'lang/en_us.json').read_text(encoding='utf8'));lang['item.coraline.camera']='Camera';lang['entity.coraline.wybie_visitor']='Wybie'
lang['item.coraline.cheese_package']='Cheese Package';lang['entity.coraline.bobinsky_visitor']='Mr. Bobinsky'
lang['item.coraline.jasmine_tea']='Jasmine Tea';lang['item.coraline.empty_teacup']='Empty Teacup';lang['item.coraline.broken_glass']='Broken Glass';lang['entity.coraline.spink_visitor']='Miss Spink';lang['entity.coraline.forcible_visitor']='Miss Forcible'
for name,liquid in [('empty_teacup',False),('jasmine_tea',True)]:
    cup=Image.new('RGBA',(16,16));cp=ImageDraw.Draw(cup)
    cp.polygon([(2,5),(11,5),(10,10),(8,12),(5,12),(3,10)],fill='#DCE8DD',outline='#658982')
    cp.line((3,6,4,9,5,10),fill='#FBF7E6');cp.line((9,6,9,9,8,10),fill='#A5BCAB')
    cp.rectangle((11,5,14,9),outline='#E9DBA6');cp.line((12,6,13,6,13,8,11,8),fill='#76968A')
    cp.ellipse((2,3,11,6),fill='#E8DCAC',outline='#A38B52');cp.line((4,4,9,4),fill='#B17D36' if liquid else '#658579')
    cp.line((4,7,9,7),fill='#C2A258');cp.point((6,8),fill='#5E947F');cp.point((7,8),fill='#E3B1A4')
    cp.ellipse((1,12,13,14),fill='#E5E7D4',outline='#8F9D83');cp.line((3,13,11,13),fill='#C9AD68')
    if liquid:cp.line((5,1,5,2),fill='#E7EADF80');cp.point((7,0),fill='#E7EADF80')
    cup.save(A/f'textures/item/{name}.png');(A/f'models/item/{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'coraline:item/{name}'}},indent=2)+'\n',encoding='utf8')
glass=Image.new('RGBA',(16,16));gd=ImageDraw.Draw(glass);gd.polygon([(2,12),(7,3),(8,11)],fill='#BFE2E380',outline='#E4F6F3');gd.polygon([(10,13),(12,6),(14,12)],fill='#BFE2E380',outline='#D2EFED');glass.save(A/'textures/item/broken_glass.png');(A/'models/item/broken_glass.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/broken_glass'}},indent=2)+'\n',encoding='utf8')
cheese=Image.new('RGBA',(16,16));cd=ImageDraw.Draw(cheese)
cd.polygon([(2,5),(10,2),(14,5),(6,8)],fill='#F3E4B9',outline='#B99F6C')
cd.polygon([(2,5),(6,8),(6,14),(2,11)],fill='#BCA275',outline='#826F50')
cd.polygon([(6,8),(14,5),(14,11),(6,14)],fill='#DDCAA0',outline='#826F50')
cd.polygon([(6,4),(8,3),(12,6),(10,7)],fill='#B74436');cd.polygon([(9,7),(11,6),(11,12),(9,13)],fill='#A43B30')
cd.polygon([(3,7),(13,4),(14,5),(4,8)],fill='#B74436')
cd.polygon([(6,9),(8,8),(8,11),(6,12)],fill='#EAB847');cd.point((7,10),fill='#96712C')
cheese.save(A/'textures/item/cheese_package.png')
(A/'models/item/cheese_package.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/cheese_package'}},indent=2)+'\n',encoding='utf8')
(A/'lang/en_us.json').write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
# Theatre poster is authored as native pixel art, matching the game's block textures.
poster=Image.new('RGB',(64,64),'#BF7D65');pd=ImageDraw.Draw(poster)
pd.rectangle((1,1,62,62),outline='#55342D',width=2)
for x,y,hair in [(20,25,'#9D3C37'),(43,24,'#D7C7A1')]:
    pd.ellipse((x-10,y-12,x+10,y+9),fill=hair);pd.ellipse((x-7,y-6,x+7,y+9),fill='#E0B397')
    pd.arc((x-5,y-3,x,y+2),185,345,fill='#3F3430',width=1);pd.arc((x+1,y-3,x+6,y+2),185,345,fill='#3F3430',width=1)
    pd.line((x,y+1,x+2,y+5,x-1,y+5),fill='#7E4B43');pd.arc((x-4,y+3,x+5,y+8),0,165,fill='#69373A')
    pd.polygon([(x-6,y+10),(x+6,y+10),(x+13,51),(x-13,51)],fill='#40373B')
    pd.polygon([(x-6,y+10),(x,y+20),(x+6,y+10)],fill='#E0B397')
    pd.line((x-10,y+16,x-17,y+12),fill='#40373B',width=3)
font=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',8)
pd.text((7,4),'SPINK!',fill='#302F33',font=font);pd.text((4,53),'FORCIBLE!',fill='#302F33',font=font)
(A/'textures/block').mkdir(exist_ok=True);poster.save(A/'textures/block/tea_theatre_poster.png')
import random
rng=random.Random(694);wall=Image.new('RGB',(32,32),'#807C70');wd=ImageDraw.Draw(wall)
for row in range(4):
    shift=8 if row%2 else 0
    for col in range(-1,3):
        x=col*16+shift;y=row*8;v=rng.randrange(80,101)
        wd.rectangle((x+1,y+1,x+15,y+7),fill=(v+4,v+2,v-3));wd.line((x+1,y+1,x+15,y+1),fill=(v+18,v+15,v+8));wd.line((x+1,y+7,x+15,y+7),fill=(v-13,v-12,v-14))
for i in range(120):
    x=rng.randrange(32);y=rng.randrange(32);v=rng.randrange(-9,10);rgb=wall.getpixel((x,y));wd.point((x,y),fill=tuple(max(0,min(255,c+v)) for c in rgb))
wall.save(A/'textures/block/tea_weathered_bricks.png')
