"""Enable the real deferred level without overwriting biome spawn manifests."""
from pathlib import Path
import json

RES=Path(__file__).resolve().parents[1]/'src/main/resources'
def put(path,value):
    p=RES/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
def main():
    put('data/coraline/dimension_type/infinite_other_world.json',{'ultrawarm':False,'natural':False,'coordinate_scale':1.0,'has_skylight':True,'has_ceiling':False,'ambient_light':.25,'fixed_time':18000,'piglin_safe':False,'bed_works':True,'respawn_anchor_works':False,'has_raids':False,'logical_height':384,'min_y':-64,'height':384,'infiniburn':'#minecraft:infiniburn_overworld','effects':'coraline:infinite_other_world','monster_spawn_block_light_limit':0,'monster_spawn_light_level':7})
    # A real animated block-atlas sprite, not a standalone image of an entire
    # animation strip. The epoch level itself is deliberately not pre-created.
    put('assets/coraline/blockstates/infinite_return_portal.json',{'variants':{'':{'model':'coraline:block/infinite_return_portal'}}})
    put('assets/coraline/models/block/infinite_return_portal.json',{'render_type':'minecraft:translucent','textures':{'portal':'minecraft:block/nether_portal','particle':'minecraft:block/nether_portal'},'elements':[{'from':[1,0,7.9],'to':[15,16,8.1],'faces':{f:{'texture':'#portal','uv':[0,0,16,16]} for f in ['north','south']}}]})
    path='assets/coraline/lang/en_us.json';n=json.loads((RES/path).read_text(encoding='utf-8-sig'));n['block.coraline.infinite_return_portal']='Way Home'
    words={'unavailable':'The passage cannot open safely right now.','save_paused':'The passage is waiting for a safe world save.','preparing':'The passage is opening. Stay nearby.','arrival_blocked':'Clear the arrival frame and its walking space.','return_blocked':'A safe return landing could not be found.','ending_required':'Finish the story and dispose of the key at the Magic Well first.','portal_reserved':'This portal must remain clear for safe travel.'}
    n.update({'message.coraline.infinite.'+key:value for key,value in words.items()});put(path,n)

if __name__=='__main__':main()
