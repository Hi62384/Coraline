"""Short, straight waterproof raincoat replacing the flared dress-like mesh."""
from pathlib import Path
import json, math

import sculpted_coat as g
from concept_armor import cap, hood, trim
from dreamweave_model import surface, shell, panel, cord

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'src/main/resources/assets/coraline/sculpted/raincoat.json'
MANIFEST=ROOT/'artwork/sculpted_wardrobe/manifest.json'
MATERIALS={
    'yellow':'#e8bb31','shadow':'#bb8d1f','orange':'#9b5a20',
    'brass':'#d5ad5d','lining':'#d8a445','button':'#272a32',
    'black':'#191b23','thread':'#f0d77a','gold':'#f0d77a',
}

def build():
    g.meshes.clear()
    hood(material='yellow',trim_material='shadow',pointed=False)
    # A broad brim projects over the brow and the orange inner edge frames skin.
    panel('Water shedding peaked hood brim','head',
          [(-4.8,-8.95),(0,-9.55),(4.8,-8.95),(4.25,-7.7),
           (0,-8.08),(-4.25,-7.7)],-5.45,'yellow',.13)
    for side in (-1,1):
        panel('Hood orange lining strip','head',[(side*4.75,-7.35),
              (side*4.25,-7.25),(side*4.15,-.1),(side*4.8,.1)],-5.5,'orange',.06)

    shell('Straight waterproof torso','body',(0,-.2,0),4.82,2.85,12.1,'yellow',8)
    cap('Waterproof shoulder yoke','body',(0,-.2,0),4.82,2.85,'yellow')
    panel('Broad straight storm flap','body',[(-1.05,.1),(3.8,.1),
          (3.8,11.55),(-1.05,11.55)],-3.05,'yellow',.08)
    panel('Offset closing placket','body',[(-1.15,.15),(-.4,.15),
          (-.4,11.5),(-1.15,11.5)],-3.23,'shadow',.08)
    panel('Short rain cape shoulder shield','body',[(-4.65,.1),(4.65,.1),
          (4.45,2.8),(0,3.5),(-4.45,2.8)],-3.28,'yellow',.08)
    panel('Chest storm shield dark fold','body',[(-4.1,2.55),(0,3.4),
          (4.1,2.55),(4.1,3.1),(0,3.85),(-4.1,3.1)],-3.36,'shadow',.06)
    for y in (4.5,6.8,9.1):g.button('Black storm flap button','body',-.72,y,-3.52,.38,'button')
    for side in (-1,1):
        x=side*2.7
        panel('Square utility rain pocket','body',[(x-1.05,7.3),(x+1.05,7.3),
              (x+1.05,10.7),(x-1.05,10.7)],-3.3,'yellow',.12)
        panel('Pocket rain-cover flap','body',[(x-1.2,7.15),(x+1.2,7.15),
              (x+1.1,8.0),(x-1.1,8.0)],-3.42,'shadow',.06)
        g.button('Pocket snap','body',x,7.65,-3.58,.19,'button')
        bone='left_arm' if side>0 else 'right_arm'
        shell('Full straight rain sleeve',bone,(side,-2.15,0),2.38,2.4,10.25,'yellow',8)
        cap('Sealed shoulder seam',bone,(side,-2.15,0),2.38,2.4,'yellow')
        shell('Wide waterproof cuff',bone,(side,6.5,0),2.55,2.56,1.45,'shadow',8)
        trim(bone,[[side+2.56*math.cos(i*math.tau/24),7.92,
                    2.57*math.sin(i*math.tau/24)] for i in range(25)],'orange',.06)
        g.button('Cuff snap',bone,side,7.2,-2.71,.21,'button')
        cord('Rain hood drawstring','body',[[side*1.7,.2,-3.1],
             [side*1.8,3.5,-3.12]],.065,'thread')
    # Separate narrow thigh flaps: they follow the legs without forming a skirt.
    for side in (-1,1):
        bone='left_leg' if side>0 else 'right_leg'
        panel('Short straight coat hem',bone,[(-1.8,0),(1.8,0),
              (1.78,4.35),(-1.78,4.35)],-2.67,'yellow',.09)
        trim(bone,[[-1.75,4.36,-2.73],[0,4.36,-2.73],
                   [1.75,4.36,-2.73]],'orange',.06)
    meshes=g.meshes
    for mesh in meshes:
        mesh['faces']=[list(reversed(face)) for face in mesh['faces']]
        mesh['vertices']=[[round(value,5) for value in vertex] for vertex in mesh['vertices']]
        mesh['uv']=[[.5,.5] for _ in mesh['vertices']]
    return {'id':'raincoat','style':'raincoat_utility','piece':'chestplate',
            'texture':'fabric','materials':MATERIALS,'meshes':meshes}

if __name__=='__main__':
    model=build()
    DEST.write_text(json.dumps(model,separators=(',',':')))
    manifest=json.loads(MANIFEST.read_text())
    for entry in manifest:
        if entry['id']=='raincoat':
            entry.update(style=model['style'],polygons=sum(len(m['faces']) for m in model['meshes']))
            break
    MANIFEST.write_text(json.dumps(manifest,indent=2)+'\n')
    print('raincoat',entry['polygons'],'polygons')
