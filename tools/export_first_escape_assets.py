"""Native item geometry and a single runtime wallpaper export from its preserved master."""
from pathlib import Path
import json, shutil
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];ASSETS=ROOT/'src/main/resources/assets/coraline'
source=Path(r'C:/Users/User1/.codex/generated_images/01a0e5a1-6507-7363-8a26-a56f9f230b40/exec-e1d4014a-7e07-4521-a91c-3a0484ba9f89.png')
master=ROOT/'artwork/first_escape_wallpaper_master.png'
if not master.exists():shutil.copyfile(source,master)
with Image.open(master) as im:im.convert('RGB').resize((512,512),Image.Resampling.LANCZOS).save(ASSETS/'textures/block/insect_parlor_wallpaper.png',optimize=True)
def element(lo,hi,texture):return {'from':lo,'to':hi,'faces':{face:{'texture':'#'+texture} for face in ('north','south','east','west','up','down')}}
for name,parts,textures in [
 ('story_cane',[element([7,1,7],[8.1,13,8.1],'wood'),element([7,12,7],[8.3,14,8.3],'wood'),element([7,13.5,7],[11,15,8.3],'wood'),element([10,12,7],[11.5,14.5,8.3],'wood'),element([7,.5,7],[8.1,1.5,8.1],'cap')],{'wood':'minecraft:block/dark_oak_planks','cap':'minecraft:block/black_concrete'}),
 ('story_shoe',[element([3,1,2],[11,2,15],'sole'),element([3.5,2,2.5],[10.5,4.5,12],'cloth'),element([4,3,9],[10,6.5,14],'cloth'),element([4.5,5.8,10],[9.5,6,13],'sole')]+[element([5,4.6,5+i],[9,4.9,5.4+i],'lace') for i in range(4)],{'cloth':'minecraft:block/black_wool','sole':'minecraft:block/black_concrete','lace':'minecraft:block/gray_wool'})]:
    model={'textures':textures,'elements':parts,'display':{'gui':{'rotation':[25,-35,0],'scale':[.8,.8,.8]},'firstperson_righthand':{'rotation':[0,-75,10],'translation':[0,2,0],'scale':[.7,.7,.7]},'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.6,.6,.6]}}}
    (ASSETS/'models/item'/(name+'.json')).write_text(json.dumps(model,indent=2)+'\n')
print('Preserved wallpaper master; exported 512px texture and two native item models.')
