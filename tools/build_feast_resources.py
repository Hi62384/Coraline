"""Export generated art without changing its design, and emit item/cake model JSON."""
from pathlib import Path
import json,shutil,subprocess
root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/coraline'
originals=root/'artwork/feast';originals.mkdir(exist_ok=True)
sources=json.loads((root/'artwork/feast_sources.json').read_text())
language=json.loads((assets/'lang/en_us.json').read_text())
for name,source in sources.items():
    copied=originals/(name+'.png')
    if not copied.exists():shutil.copy2(source,copied)
    cake=name.startswith('welcome_cake_')
    target=assets/('textures/block' if cake else 'textures/item')/(name+'.png')
    subprocess.run(['ffmpeg','-v','error','-y','-i',str(copied),'-vf','scale=256:256:flags=neighbor' if cake else 'scale=64:64:flags=neighbor',str(target)],check=True)
    if not cake:
        (assets/'models/item'/f'{name}.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/'+name}},indent=2)+'\n')
        language['item.coraline.'+name]='Pancakes with Strawberry Sauce' if name=='strawberry_pancakes' else name.replace('_',' ').title()
language['block.coraline.welcome_cake']='Welcome Home Strawberry Cake'
language['entity.coraline.scene_prop']='Other World Furnishing'
(assets/'lang/en_us.json').write_text(json.dumps(language,indent=2)+'\n')
variants={}
for bites in range(7):
    left=1+bites*2
    # Keep the icing UV clipped, not stretched, as slices are eaten.
    model={'textures':{'particle':'coraline:block/welcome_cake_top','top':'coraline:block/welcome_cake_top','side':'coraline:block/welcome_cake_side','inside':'minecraft:block/cake_inner','bottom':'minecraft:block/cake_bottom'},'elements':[{'from':[left,0,1],'to':[15,8,15],'faces':{
        'up':{'texture':'#top','uv':[(left-1)*16/14,0,16,16]},'down':{'texture':'#bottom','uv':[left,1,15,15]},
        'north':{'texture':'#side','uv':[(left-1)*16/14,0,16,16]},'south':{'texture':'#side','uv':[(left-1)*16/14,0,16,16]},
        'east':{'texture':'#side','uv':[0,0,16,16]},'west':{'texture':'#inside' if bites else '#side','uv':[0,0,16,16]}}}]}
    model['display']={'gui':{'rotation':[30,225,0],'translation':[0,1,0],'scale':[.9,.9,.9]},'ground':{'translation':[0,3,0],'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]},'firstperson_righthand':{'rotation':[0,45,0],'scale':[.55,.55,.55]}}
    (assets/'models/block'/f'welcome_cake_{bites}.json').write_text(json.dumps(model,indent=2)+'\n')
    variants[f'bites={bites}']={'model':f'coraline:block/welcome_cake_{bites}'}
(assets/'blockstates/welcome_cake.json').write_text(json.dumps({'variants':variants},indent=2)+'\n')
(assets/'models/item/welcome_cake.json').write_text(json.dumps({'parent':'coraline:block/welcome_cake_0'},indent=2)+'\n')
