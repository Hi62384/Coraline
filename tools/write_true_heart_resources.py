"""Author the final mechanisms with existing textures and merge their dialogue."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,n):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(n,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def cube(a,b,texture):return {'from':a,'to':b,'faces':{side:{'texture':'#'+texture} for side in ('north','south','east','west','up','down')}}
def main():
 variants={}
 for part in range(5):
  for sealed in (True,False):
   name=f'true_heart_{part}_{int(sealed)}';elements=[cube([0,0,0],[16,3,16],'stone'),cube([2,3,2],[14,5,14],'metal')]
   if part==0:
    elements += [cube([4,6,4],[12,14,12],'light'),cube([6,14,6],[10,19,10],'light')]
    if sealed:
     for x in (1,13):
      for z in (1,13):elements.append(cube([x,3,z],[x+2,24,z+2],'metal'))
     elements.append(cube([1,23,1],[15,25,15],'stone'))
   elif part==4:
    elements += [cube([1,3,5],[4,27,11],'stone'),cube([12,3,5],[15,27,11],'stone'),cube([4,24,5],[12,28,11],'light')]
   else:elements += [cube([5,5,5],[11,18,11],'stone'),cube([2,17,2],[14,20,14],'light')]
   write(ROOT/f'assets/coraline/models/block/{name}.json',{'textures':{'stone':'minecraft:block/smooth_quartz','metal':'minecraft:block/obsidian','light':'minecraft:block/purple_concrete' if sealed else 'minecraft:block/sea_lantern','particle':'minecraft:block/smooth_quartz'},'elements':elements})
   variants[f'part={part},sealed={str(sealed).lower()}']={'model':'coraline:block/'+name}
 write(ROOT/'assets/coraline/blockstates/true_heart_anchor.json',{'variants':variants})
 write(ROOT/'data/coraline/loot_tables/blocks/true_heart_anchor.json',{'type':'minecraft:block','pools':[]})
 write(ROOT/'data/coraline/loot_tables/entities/true_heart.json',{'type':'minecraft:entity','pools':[]})
 path=ROOT/'assets/coraline/lang/en_us.json';lang=json.loads(path.read_text(encoding='utf-8-sig'))
 lang.update({'entity.coraline.true_heart':'The Heart of the Other World','block.coraline.true_heart_anchor':'Reality Anchor','biome.coraline.true_heart':'Heart of the Other World'})
 messages={
 'heart_body_missing':'The manifestation could not be found after its entire native arena loaded. This attempt is paused without granting a victory or a replacement body. Leave through the return arch and speak to Wybie to begin a new attempt.',
 'heart_scope_capacity':'The ending has reached its bounded owner-recovery limit. Pending player cleanup must finish before another global ending can be committed.',
 'heart_parents_space':'The starting parent setup needs clear standing space beside your existing Parents Bed at %s, %s, %s. Clear the obstruction; the ending recovery will resume without replacing a missing original.',
 'heart_prepare':'Prepare to enter the true Heart (opens the final warning).',
 'heart_global_permission':'This ending affects other players’ Coraline progress. A server operator must initiate it after warning everyone.',
 'heart_final_warning':'FINAL ENDING — %s Coraline owner records are currently affected. Defeating the manifestation AND breaking its exposed Heart will retire this Infinite Other World and its portals, reset personal worlds, souls, workers, orphanages, adopted children and story/quest progress, and remove postgame NPCs. Ordinary Minecraft builds and inventories remain. Earned Void Hearts remain governed by their original reward records. Learning the method did not start a reset. You can leave the encounter before destroying the core. Confirm only if you accept these consequences.',
 'heart_confirm':'I understand — prepare the final encounter',
 'heart_finish_rituals':'Finish active Last Thread rituals and claim any reserved Void Heart rewards before committing this ending.',
 'heart_in_use':'Another final encounter is already in progress.',
 'heart_world_warning':'%s has chosen to enter the final Heart encounter. Breaking its exposed core after victory will reset ALL Coraline progression, including other players’ personal worlds and adopted children. Ordinary Minecraft content remains.',
 'heart_clear_space':'The reserved Heart arrival or body space is obstructed. No replacement body has been issued.',
 'heart_entered':'The Heart: three reality anchors feed its body. WEST: strike the anchor three times while its top shines white. EAST: follow the three cyan bridge marks outward, then operate the anchor. SOUTH: operate the anchor and survive twenty continuous seconds on its platform; jump when the floor flashes. NORTH: the return arch lets you leave without a reset. The manifestation becomes vulnerable only after all three anchors are disabled.',
 'heart_timing':'West anchor: %s / 3 correctly timed strikes.',
 'heart_navigation':'East bridge: %s / 3 markers reached. Operate the far anchor after the third.',
 'heart_survival':'Remain on the south platform for twenty continuous seconds. Its bright pulses strike the ground; jump to avoid them. Leaving, dying or disconnecting restarts this anchor’s survival interval.',
 'heart_jump':'The platform flashes — jump!',
 'heart_anchor_disabled':'A reality anchor has gone dark.',
 'heart_manifested':'The reality anchors fail. The central manifestation is vulnerable. Its close-range resonant warning precedes a ground strike; keep moving and jump.',
 'heart_core_exposed':'The manifestation has fallen. The central Heart is now exposed. Breaking it commits the complete ending. You may still leave through the north arch instead.',
 'heart_ending_complete':'The Heart collapses. The defeated Other World and its portals are retired. Coraline progression has been reset; ordinary Minecraft content and the earned-heart audit ledger remain. The original story can begin again.'}
 for key,text in messages.items():lang['message.coraline.'+key]=text
 write(path,lang)
if __name__=='__main__':main()
