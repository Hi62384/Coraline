"""Copy original imagegen artwork and downsample for the item atlas; preserve transparency."""
from pathlib import Path
import json, shutil, subprocess
ROOT=Path(__file__).resolve().parents[1]
sources=json.loads((ROOT/'artwork/clothing_sources.json').read_text())
out=ROOT/'artwork/clothing';out.mkdir(exist_ok=True)
for name,source in sources.items():
    original=out/(name+'.png')
    if not original.exists():shutil.copy2(source,original)
    target=ROOT/'src/main/resources/assets/coraline/textures/item'/original.name
    subprocess.run(['ffmpeg','-v','error','-y','-i',str(original),'-vf','scale=128:128:flags=neighbor',str(target)],check=True)
print('Nine transparent 128px icons exported; originals preserved in artwork/clothing.')
