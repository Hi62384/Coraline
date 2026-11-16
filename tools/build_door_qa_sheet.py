"""Read-only screenshots, composed for QA; never a runtime texture export."""
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'artwork/seamless_tunnel_qa'
DEST=ROOT/'build-character-cast/reports/door-native-contact.png'
sheet=Image.new('RGB',(6*427,6*260),'#202020');draw=ImageDraw.Draw(sheet)
for index in range(36):
    with Image.open(SOURCE/f'view_{index:02d}.png') as original:
        original.thumbnail((427,239),Image.Resampling.LANCZOS)
        x=index%6*427;y=index//6*260;sheet.paste(original,(x,y))
        draw.text((x+5,y+242),f'Actual native view {index:02d}',fill='white')
DEST.parent.mkdir(parents=True,exist_ok=True);sheet.save(DEST)
print(DEST)
