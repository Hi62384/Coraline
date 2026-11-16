"""Author a hollow block-style porcelain cup via Blender MCP; preserve existing scenes."""
import bpy,io,contextlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ns={'__file__':str(ROOT/'tools/build_minecraft_character_cast.py')}
exec(compile(Path(ns['__file__']).read_text(encoding='utf8'),ns['__file__'],'exec'),ns)
ns['BASE']['CAST'].extend(['tea_cup','cheese_parcel','tea_glass','tea_glass_shards'])
with contextlib.redirect_stdout(io.StringIO()):
    a=ns['Builder']('tea_cup');a.bone('body',(0,0,0))
    a.box('porcelain foot',(0,.15,0),(2.2,.3,2.2),'#DDE9D8',fabric=False)
    a.box('hollow bowl base',(0,-.15,0),(2.7,.3,2.7),'#DDE9D8',fabric=False)
    for sign in [-1,1]:
        a.box('cup wall',(sign*1.35,-1,0),(.3,1.7,3),'#E9EDDA',fabric=False)
        a.box('cup wall',(0,-1,sign*1.35),(2.4,1.7,.3),'#E9EDDA',fabric=False)
        a.box('gold rim',(sign*1.35,-1.9,0),(.32,.18,3.04),'#C9AD68',fabric=False)
        a.box('gold rim',(0,-1.9,sign*1.35),(2.4,.18,.32),'#C9AD68',fabric=False)
    a.box('dark tea leaves',(0,-.36,0),(1.9,.05,1.9),'#668377',fabric=False)
    for y in [-1.65,-.35]:a.box('handle bridge',(1.95,y,0),(1.1,.3,.4),'#DDE9D8',fabric=False)
    a.box('handle outer',(2.5,-1,0),(.3,1.6,.4),'#DDE9D8',fabric=False)
    for sign in [-1,1]:
        a.box('green floral detail',(sign*.5,-1,-1.52),(.35,.35,.05),'#629583',fabric=False)
        a.box('rose floral detail',(sign*.5,-1.25,-1.55),(.2,.2,.05),'#DBA49B',fabric=False)
    a.finish();ns['repair_glb'](a.name)
    b=ns['Builder']('cheese_parcel');b.bone('body',(0,0,0))
    b.box('paper wrapped parcel',(0,-1.3,0),(5,2.6,4),'#E4D4A8',fabric=False)
    b.box('folded paper seam',(0,-2.65,0),(4.8,.1,.25),'#BBA477',fabric=False)
    for z in [-2.03,2.03]:b.box('red ribbon',(0,-1.3,z),(.6,2.65,.08),'#A84135',fabric=False)
    b.box('red ribbon top',(0,-2.68,0),(.6,.1,4.08),'#A84135',fabric=False)
    b.box('red ribbon cross',(0,-2.7,0),(5.08,.1,.6),'#A84135',fabric=False)
    b.box('cheese label',(-1.35,-1.2,-2.06),(1.4,1.2,.1),'#DDAF42',fabric=False)
    b.box('cheese label hole',(-1.5,-1.2,-2.13),(.3,.3,.05),'#94702D',fabric=False)
    b.finish();ns['repair_glb'](b.name)
    g=ns['Builder']('tea_glass');g.bone('body',(0,0,0))
    g.box('glass base',(0,-.15,0),(2.8,.3,2.8),'#C8E6E5',fabric=False)
    for sign in [-1,1]:
        g.box('glass wall',(sign*1.4,-2.3,0),(.15,4.3,2.95),'#BCD8DD',fabric=False)
        g.box('glass wall',(0,-2.3,sign*1.4),(2.7,4.3,.15),'#D5F0ED',fabric=False)
    g.finish();ns['repair_glb'](g.name)
    s=ns['Builder']('tea_glass_shards');s.bone('body',(0,0,0))
    import random,math
    rng=random.Random(532)
    for i in range(17):
        x=rng.uniform(-10,10);z=rng.uniform(-9,9);r=rng.uniform(.7,2.1);angle=rng.random()*math.tau
        points=[(x+math.sin(angle+j*2.1)*r,-.065-rng.random()*.08,z+math.cos(angle+j*2.1)*r) for j in range(3)]
        s.surface('scattered triangular fragment',points,[(0,1,2),(2,1,0)],'#C7E7E8')
    s.finish();ns['repair_glb'](s.name)
bpy.app.driver_namespace['tea_cup_author']=a
print('TEA_CUP_EXPORTED: hollow walls, gold rim, open handle, floral porcelain; preserved existing scenes')
