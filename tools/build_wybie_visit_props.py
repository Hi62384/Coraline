"""Run inside Blender MCP; author the visit's block-style slug and bicycle."""
import bpy, math, io, contextlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ns={'__file__':str(ROOT/'tools/build_minecraft_character_cast.py')}
exec(compile(Path(ns['__file__']).read_text(encoding='utf8'),ns['__file__'],'exec'),ns)
Builder=ns['Builder']
ns['BASE']['CAST'].extend(['wybie_slug','wybie_bicycle'])
def slug():
    a=Builder('wybie_slug');a.bone('body',(0,0,0))
    for i in range(7):
        x=(i-3)*.62;size=1.1-abs(i-3)*.17
        a.box('olive slug segment',(x,0,0),(.72,size,.85),'#769343')
        a.box('mottled slug skin',(x,-size/2-.02,-.15),(.24,.09,.27),'#4D6731')
    for sign in [-1,1]:
        a.bar('slug eye stalk',(-1.9,-.3,sign*.27),(-2.2,-1.1,sign*.4),.13,.13,'#688638')
        a.box('slug eye',(-2.2,-1.1,sign*.4),(.23,.23,.23),'#182319')
    a.finish();ns['repair_glb'](a.name);return a
def bicycle():
    a=Builder('wybie_bicycle');a.bone('body',(0,0,0))
    for z in [-8,8]:
        for i in range(16):
            t=i*math.tau/16;u=(i+1)*math.tau/16
            p=(0,-4+math.sin(t)*4,z+math.cos(t)*4);q=(0,-4+math.sin(u)*4,z+math.cos(u)*4)
            a.bar('faceted tire',p,q,.7,.7,'#19252B')
            a.bar('silver wheel spoke',(0,-4,z),p,.12,.12,'#A0ACAA')
        a.box('wheel axle',(0,-4,z),(2,.5,.5),'#718380')
    for p,q in [((0,-4,8),(0,-5,0)),((0,-5,0),(0,-11,2)),((0,-11,2),(0,-4,8)),((0,-11,2),(0,-12,-6)),((0,-12,-6),(0,-5,0)),((0,-12,-6),(0,-4,-8)),((0,-12,-6),(0,-15,-7))]:
        a.bar('red bicycle frame',p,q,.45,.45,'#7C3D31')
    a.box('black saddle',(0,-11.5,2),(3,.6,3),'#283334')
    a.bar('silver handlebar',(-3.5,-15,-8),(3.5,-15,-8),.35,.35,'#A2AEAC')
    for sign in [-1,1]:
        a.box('handlebar grip',(sign*3.3,-15,-8),(1.2,.55,.55),'#1B2A2D')
        a.box('pedal',(sign*1.4,-5,0),(1.1,.3,1.2),'#1B282A')
    a.box('rear mudguard',(0,-8.3,8),(1.1,.3,5),'#708B89')
    a.box('front bicycle lamp',(0,-12,-8.2),(1.4,1.2,.8),'#D7D8B3')
    a.finish();ns['repair_glb'](a.name);return a
with contextlib.redirect_stdout(io.StringIO()):
    props={'slug':slug(),'bicycle':bicycle()}
    actors={name:ns['create'](name) for name in ['wybie','other_wybie']}
bpy.app.driver_namespace['wybie_visit_props']={'props':props,'actors':actors,'ns':ns}
print('Exported slug, bicycle, and expression-ready Wybie meshes')
