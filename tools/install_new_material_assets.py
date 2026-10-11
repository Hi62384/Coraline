"""Install the two painted materials and five placeable textured blocks."""
from pathlib import Path
from PIL import Image
import json, shutil

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
DATA=ROOT/'src/main/resources/data/coraline'
ART=ROOT/'artwork/new_materials'
GENERATED=Path(r'C:/Users/User1/.codex/generated_images/01a0ff56-811d-75c0-98db-c628015951c7')
ITEMS={
    'perfect_ingot':'exec-0c8cb2dc-066b-4058-bb60-acbb8a405c25.png',
    'soulglass':'exec-1744a7a4-3659-4fce-a69b-3e26e2e3a66a.png',
}
BLOCKS={
    'other_diamond_block':'exec-8fcf7d6c-4dac-49f7-b21d-b3c4a49e6615.png',
    'other_iron_block':'exec-e82caa9f-a551-4b61-a06e-d005dcaa0b8f.png',
    'moonstone_block':'exec-1fd0cb75-7b9f-4695-a4e5-bfdc5a71b9cb.png',
    'perfect_block':'exec-3a887bf3-00f4-4bdb-b6f9-b0cf57df4375.png',
    'soulglass_block':'exec-73b81c99-4db2-4341-a1a3-9fa75fe37366.png',
}
MATERIALS={
    'other_diamond_block':'other_diamond',
    'other_iron_block':'other_iron_ingot',
    'moonstone_block':'moonstone',
    'perfect_block':'perfect_ingot',
    'soulglass_block':'soulglass',
}
ART.mkdir(parents=True,exist_ok=True)
for name,filename in ITEMS.items():
    original=ART/(name+'_source.png')
    if not original.exists():shutil.copy2(GENERATED/filename,original)
    with Image.open(original) as image:
        rgba=image.convert('RGBA')
        if rgba.getextrema()[3][0]!=0:raise ValueError('Expected isolated transparent icon: '+name)
        rgba.resize((256,256),Image.Resampling.LANCZOS).save(ASSETS/'textures/item'/(name+'.png'))
    (ASSETS/'models/item'/(name+'.json')).write_text(json.dumps({
        'parent':'minecraft:item/generated','textures':{'layer0':'coraline:item/'+name}},indent=2)+'\n')
    print('item',name)

for name,filename in BLOCKS.items():
    original=ART/(name+'_source.png')
    if not original.exists():shutil.copy2(GENERATED/filename,original)
    with Image.open(original) as image:
        rgba=image.convert('RGBA')
        side=min(rgba.size)
        x=(rgba.width-side)//2;y=(rgba.height-side)//2
        tile=rgba.crop((x,y,x+side,y+side)).resize((128,128),Image.Resampling.LANCZOS)
        if name=='soulglass_block':
            tile.putalpha(Image.new('L',tile.size,178))
        else:
            opaque=Image.new('RGBA',tile.size,(20,24,30,255))
            opaque.alpha_composite(tile)
            tile=opaque
        tile.save(ASSETS/'textures/block'/(name+'.png'))
    (ASSETS/'models/block'/(name+'.json')).write_text(json.dumps({
        'parent':'minecraft:block/cube_all','textures':{'all':'coraline:block/'+name}},indent=2)+'\n')
    (ASSETS/'blockstates'/(name+'.json')).write_text(json.dumps({
        'variants':{'':{'model':'coraline:block/'+name}}},indent=2)+'\n')
    (ASSETS/'models/item'/(name+'.json')).write_text(json.dumps({
        'parent':'coraline:block/'+name},indent=2)+'\n')
    loot=DATA/'loot_tables/blocks'/(name+'.json')
    loot.parent.mkdir(parents=True,exist_ok=True)
    loot.write_text(json.dumps({'type':'minecraft:block','pools':[{
        'rolls':1,'entries':[{'type':'minecraft:item','name':'coraline:'+name}],
        'conditions':[{'condition':'minecraft:survives_explosion'}]}]},indent=2)+'\n')
    recipes=DATA/'recipes';recipes.mkdir(parents=True,exist_ok=True)
    material='coraline:'+MATERIALS[name]
    (recipes/(name+'.json')).write_text(json.dumps({
        'type':'minecraft:crafting_shaped','pattern':['MMM','MMM','MMM'],
        'key':{'M':{'item':material}},'result':{'item':'coraline:'+name}},indent=2)+'\n')
    (recipes/(MATERIALS[name]+'_from_block.json')).write_text(json.dumps({
        'type':'minecraft:crafting_shapeless',
        'ingredients':[{'item':'coraline:'+name}],
        'result':{'item':material,'count':9}},indent=2)+'\n')
    print('block',name)
