"""Merge six real underground biome holders without replacing surface pools."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
PROFILES={
 'porcelain':('Porcelain Caverns',0xc8c5bc,[('button_eyed_wanderer',8),('threadcrawler',5),('porcelain_sentinel',2)]),
 'soul_glass':('Soul Glass Grottoes',0x789fac,[('paper_wraith',7),('void_stalker',3),('porcelain_sentinel',2)]),
 'threadroot':('Threadroot Caves',0x394b39,[('threadcrawler',8),('thread_spinner',5),('rootbound_horror',2)]),
 'clockwork':('Clockwork Depths',0x897e6c,[('clockwork_watcher',8),('rift_soldier',5),('threadcrawler',3)]),
 'mirror':('Mirror Caves',0x7e96af,[('paper_wraith',7),('false_villager',4),('thread_spinner',4)]),
 'white_rift':('White Rift Caverns',0xe0e2e6,[('void_stalker',8),('rift_soldier',6),('rift_guardian',1)])}
def write(path,n):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(n,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
 terrain_path=ROOT/'data/coraline/postgame/terrain.json';terrain=json.loads(terrain_path.read_text(encoding='utf-8-sig'))
 lang_path=ROOT/'assets/coraline/lang/en_us.json';lang=json.loads(lang_path.read_text(encoding='utf-8-sig'))
 for name,(title,fog,mobs) in PROFILES.items():
  write(ROOT/f'data/coraline/worldgen/biome/postgame/cave_{name}.json',{'has_precipitation':False,'temperature':.5,'downfall':0.,'effects':{'fog_color':fog,'sky_color':fog,'water_color':0x638ab3,'water_fog_color':0x34476d,'mood_sound':{'sound':'minecraft:ambient.cave','tick_delay':6000,'block_search_extent':8,'offset':2.}},'spawners':{'monster':[{'type':'coraline:'+mob,'weight':weight,'minCount':1,'maxCount':1 if mob in ('rift_guardian','porcelain_sentinel','rootbound_horror') else 2} for mob,weight in mobs]},'spawn_costs':{},'carvers':{},'features':[]})
  terrain['biome_source']['biomes']['cave_'+name]='coraline:postgame/cave_'+name
  lang['biome.coraline.postgame.cave_'+name]=title
 write(terrain_path,terrain);write(lang_path,lang)
if __name__=='__main__':main()
