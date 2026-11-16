"""Native clue displays and authored dialogue; never fabricates voiced audio."""
import json
from write_endless_carnival_resources import DATA,ASSETS
from write_grand_theatre_resources import write,cube
def main():
    variants={}
    for clue in range(4):
        name='well_scar_clue_'+str(clue)
        # Wheel rim, spoke cluster, repair pin and a deliberately reversed
        # forged trace; model geometry shares ordinary vanilla materials.
        shapes=[cube([1,0,1],[15,5,15],'stone')]
        if clue==0:
            shapes += [cube([3,5,3],[13,7,5],'metal'),cube([3,5,11],[13,7,13],'metal'),cube([3,5,5],[5,7,11],'metal'),cube([11,5,5],[13,7,11],'metal')]
        elif clue==1:
            shapes += [cube([7,5,2],[9,7,14],'metal'),cube([2,5,7],[14,7,9],'metal')]
        elif clue==2:
            shapes += [cube([5,5,3],[7,8,13],'metal'),cube([7,5,10],[12,8,13],'metal')]
        else:
            shapes += [cube([3,5,3],[13,7,5],'fake'),cube([11,5,5],[13,7,13],'fake')]
        write(ASSETS/f'models/block/{name}.json',{'textures':{'stone':'minecraft:block/chiseled_stone_bricks','metal':'minecraft:block/iron_block','fake':'minecraft:block/gold_block','particle':'minecraft:block/chiseled_stone_bricks'},'elements':shapes})
        variants['clue='+str(clue)]={'model':'coraline:block/'+name}
    write(ASSETS/'blockstates/well_scar_clue.json',{'variants':variants})
    write(DATA/'loot_tables/blocks/well_scar_clue.json',{'type':'minecraft:block','pools':[]})
    path=ASSETS/'lang/en_us.json';v=json.loads(path.read_text(encoding='utf-8-sig'));messages={
      'begins':'Coraline: The well did not kill Wybie. Something carried him beyond it. Look for a newly charted Well Scar in front of a false copy of this house. Follow his repaired bicycle parts, not the perfect imitations.',
      'scar':'The Well Scar: white fractures surround a mechanical trail. Read the damaged wheel, the spoke cluster, then the repair pin before investigating the false house.',
      'clue_0':'The wheel is bent, but its repair has Wybie’s uneven left-handed crimp. The same scratch continues toward a cluster of spokes.',
      'clue_1':'The spokes carry the same hand-made repair. A bent pin points into the false house. The polished golden trace points the wrong way.',
      'clue_2':'The repair pin fits his bicycle. This trail is genuine. Search upstairs for the Buttonroot Cage; its anchors connect two upper rooms to the rear room below.',
      'false_trail':'This part is flawless, and its repair is reversed. It is a lure. Follow the damaged iron parts instead.',
      'trail_order':'Read the damaged wheel first, the spoke cluster second, and the repair pin third. Their repairs form one continuous trail.',
      'no_living_witness':'Coraline: I cannot promise he survived without evidence of his living fall. A recorded death is different from being lost beyond the well.',
      'loading_original':'Coraline: His last known place is still being searched. Speak to me again shortly.',
      'original_unavailable':'Coraline: I cannot reach the original Wybie safely right now. I will not replace him with an imitation.',
      'escort':'Wybie: I thought that house would keep me forever. I’ll follow you. Keep the route clear, and bring me close before using the stable return portal.',
      'clear_route':'Wybie cannot find a walking route. Come back and clear the obstacle or lead him around it; he will not teleport past it.',
      'complete':'Coraline: You brought the real Wybie home. His rescue is now preserved for this postgame cycle. He can stay here with us.',
      'status_search':'Coraline: Explore newly generated terrain for a false Pink Palace with a fractured mechanical approach.',
      'status_trail_one':'Follow the damaged wheel at the beginning of the Well Scar.',
      'status_trail_two':'Follow the matching cluster of bent bicycle spokes.',
      'status_trail_three':'Find the hand-repaired pin at the end of the genuine trail.',
      'status_cage':'Align the three physical anchors, operate the cage control, then speak to the original Wybie inside.',
      'status_escort':'Wybie is following you. Reach the stable return portal together; keep him within four blocks when you leave.',
      'status_returning':'Bring Wybie back to Coraline beside the Breach Drive at Pink Palace, then speak to her with him nearby.',
      'status_moving':'The original Wybie’s transfer is being reconciled from his saved position. No replacement will be created.',
      'status_failed':'The original Wybie has a recorded death. This rescue cannot silently resurrect him.'}
    v['block.coraline.well_scar_clue']='Well Scar Mechanical Trace'
    for key,text in messages.items():v['message.coraline.rescue_'+key]=text
    write(path,v)
if __name__=='__main__':main()
