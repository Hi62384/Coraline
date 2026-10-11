"""Export generated transparent bucket masters as compact 128px runtime icons.

Built-in image_gen prompt (one per liquid): magical bottomless water/lava bucket,
chunky Minecraft pixel art, gray iron silhouette, blue water/orange lava,
cyan/gold infinity symbol, transparent background, no text or glow haze.
Masters are preserved under artwork/bottomless_buckets, never packaged.
"""
from pathlib import Path
from PIL import Image
import shutil

ROOT = Path(__file__).resolve().parents[1]
GENERATED = Path(r'C:/Users/User1/.codex/generated_images/01a0e5a1-6507-7363-8a26-a56f9f230b40')
SOURCES = {'water': 'exec-525115d3-b882-47e6-9e54-65c0a9a958ec.png',
           'lava': 'exec-fb5e53f7-6fd3-4e46-bf76-37cb9769f313.png'}
for liquid, source in SOURCES.items():
    master = ROOT / 'artwork/bottomless_buckets' / (liquid + '.png')
    master.parent.mkdir(parents=True, exist_ok=True)
    if not master.exists():
        shutil.copy2(GENERATED / source, master)
    with Image.open(master) as image:
        image = image.convert('RGBA')
        assert image.getextrema()[3][0] == 0, 'Icon must preserve transparent background'
        image.resize((128,128), Image.Resampling.NEAREST).save(
            ROOT / 'src/main/resources/assets/coraline/textures/item' / f'bottomless_{liquid}_bucket.png',
            optimize=True)
