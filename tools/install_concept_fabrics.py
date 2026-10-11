"""Install painted cloth/opal swatches as in-game armor material atlases.

The rightmost quarter is white so raised metal, eyes and stitching keep their
independent vertex colors. The source paintings are retained under artwork.
"""
from pathlib import Path
from PIL import Image, ImageEnhance
import shutil

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'artwork/sculpted_wardrobe'
GAME=ROOT/'src/main/resources/assets/coraline/textures/models/sculpted'
GENERATED=Path(r'C:/Users/User1/.codex/generated_images/01a0ff56-811d-75c0-98db-c628015951c7')
SWATCHES={
    'luck':'exec-c46fbd27-a02a-4aa2-938a-fe0b50128f65.png',
    'moonstone':'exec-f78ee222-6258-4616-bf60-e8c14510b981.png',
    'spidervision':'exec-ed3676a0-358d-45be-9e48-b48b9be9e45c.png',
    'beldam_weave':'exec-015581b4-09ed-4d96-bb11-82c6beb837b3.png',
    'soulbound':'exec-e88bdff7-c3cf-4aea-a6a2-b88a59c07b80.png',
    'super_spidervision':'exec-8b4f8be0-3d9f-4d12-9dba-6e939eb6c785.png',
    'other_world_void':'exec-69d7c9ee-305e-45d9-812d-b9669ab0eecb.png',
    'other_diamond':'exec-acc5dbc0-9dde-4ff2-a9b0-33228c6e33ea.png',
    'other_iron':'exec-c25ba4ce-b4a7-44e3-9f02-ce61fcc7670f.png',
    'perfect':'exec-1d4c7d02-a8ae-46a4-ac87-a2b82be17865.png',
}

for style,filename in SWATCHES.items():
    source=ART/(style+'_fabric_source.png')
    if not source.exists():shutil.copy2(GENERATED/filename,source)

for style in (*SWATCHES,'potent_beldam_weave','soulbound_potent_beldam_weave'):
    origin=('beldam_weave' if style.endswith('potent_beldam_weave') else style)
    with Image.open(ART/(origin+'_fabric_source.png')) as source:
        rgba=source.convert('RGBA')
        side=int(min(rgba.size)*(.57 if style=='moonstone' else .68))
        x=(rgba.width-side)//2;y=(rgba.height-side)//2
        patch=rgba.crop((x,y,x+side,y+side))
        cloth=Image.new('RGBA',patch.size,(22,25,30,255))
        cloth.alpha_composite(patch)
        cloth=cloth.convert('RGB')
        if style=='potent_beldam_weave':
            cloth=ImageEnhance.Brightness(cloth).enhance(.83)
        elif style=='soulbound_potent_beldam_weave':
            cloth=ImageEnhance.Brightness(cloth).enhance(.88)
            cyan=Image.new('RGB',cloth.size,(10,46,48))
            cloth=Image.blend(cloth,cyan,.17)
        atlas=Image.new('RGB',(512,512),'white')
        atlas.paste(cloth.resize((384,512),Image.Resampling.LANCZOS),(0,0))
        atlas.save(GAME/(style+'.png'))
    (GAME/(style+'.png.mcmeta')).write_text('{"texture":{"blur":true,"clamp":false}}\n')
    print(style)
