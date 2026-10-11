from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
source=Image.open(root/'artwork/dads_slime_source.png').convert('RGBA')
source=source.crop(source.getbbox());source.thumbnail((60,60),Image.Resampling.NEAREST)
out=Image.new('RGBA',(64,64));out.alpha_composite(source,((64-source.width)//2,(64-source.height)//2))
out.save(root/'src/main/resources/assets/coraline/textures/item/dads_slime.png')
