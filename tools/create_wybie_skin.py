"""Build a UV-aligned 64x64 Minecraft skin; coordinates follow the vanilla player mesh."""
from PIL import Image, ImageDraw
from pathlib import Path
import random

out = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/coraline/textures/entity/wybie_skin.png'
im = Image.new('RGBA', (64, 64))
d = ImageDraw.Draw(im)
rng = random.Random(17)
def rect(box, color): d.rectangle(box, fill=color)
def noise(box, base, spread=6):
    for y in range(box[1], box[3]+1):
        for x in range(box[0], box[2]+1):
            n=rng.randint(-spread,spread)
            im.putpixel((x,y),tuple(max(0,min(255,c+n)) for c in base)+(255,))
# Skin/head, dark curly hair around the sides/back and upper face.
noise((0,0,31,15),(166,121,79))
noise((0,0,31,7),(42,29,24),12)
noise((0,8,7,15),(40,28,23),10)
noise((16,8,31,15),(38,26,22),10)
noise((8,8,15,15),(179,136,92),5)
for x,h in enumerate([3,2,2,1,1,2,1,2]):
    rect((8+x,8,8+x,8+h),(46+x*2,31+x,24+x))
rect((9,11,10,12),(233,222,201)); rect((13,11,14,12),(233,222,201))
rect((10,12,10,12),(51,42,34)); rect((13,12,13,12),(51,42,34))
rect((11,13,12,13),(146,99,60)); rect((11,14,13,14),(98,63,40))
# Raised irregular curls in the hat layer, leaving the face clear.
noise((32,0,63,7),(40,27,22),15)
for x in range(32,64):
    h=rng.randint(1,3) if 40<=x<=47 else rng.randint(3,6)
    for y in range(8,8+h):
        c=rng.choice([(36,24,20),(56,38,28),(70,47,32)])
        im.putpixel((x,y),c+(255,))
# Jacket torso, arms, trousers and boots.
for box in [(16,16,39,31),(40,16,55,31),(0,16,15,31),(16,48,31,63),(32,48,47,63)]: noise(box,(28,36,44),5)
rect((23,20,24,31),(91,99,99)); rect((20,20,22,21),(16,21,27)); rect((25,20,27,21),(16,21,27))
rect((21,26,22,28),(17,23,30)); rect((26,26,27,28),(17,23,30))
for x,y in [(40,20),(32,52)]:
    rect((x,y+3,x+15,y+4),(159,161,151))
    rect((x,y+8,x+15,y+11),(17,19,23))
    for finger in range(3): rect((x+4+finger,y+9,x+4+finger,y+10),(216,213,193))
    rect((x+5,y+8,x+6,y+8),(205,205,188))
for x,y in [(0,20),(16,52)]:
    rect((x,y+8,x+15,y+11),(17,21,25)); rect((x+4,y+10,x+7,y+10),(82,83,80))
# Palette swatches used by the bicycle mesh, outside all visible player faces.
rect((0,32,7,39),(18,20,23)); rect((8,32,15,39),(132,143,145))
im.save(out)
print(out)
