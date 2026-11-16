"""Localized physical missions and the explicit optional-ending warning."""

import json

from write_endless_carnival_resources import ASSETS

from write_grand_theatre_resources import write

def main():

    path=ASSETS/'lang/en_us.json';v=json.loads(path.read_text(encoding='utf-8-sig'))

    names={'factory':'The Artificial People', 'carnival':'The Endless Carnival','orphanage':'The Forgotten Orphanage','theatre':'The Last Performance','sanctuary':'The Forgotten Sanctuary','seam':'The Broken Signal','drill':'The Drill Beneath Reality','boundary':'The Ancient Boundary','observatory':'The Broken Observatory','heart_location':'The Heart’s Echo','heart':'Unmaking the Anchor','new_mysteries':'New Mysteries'}

    for key,name in names.items():v['quest.coraline.'+key]=name

    messages={

      'explore':'There is more to investigate, but I need a real location for %s. Chart new structures in the Infinite Other World, then come back.',

      'world_quiet':'Coraline: The boundary is holding for now. There are still people and places worth finding beyond it.',
      'world_raids':'Coraline: The breach is open wide enough for raids and infection. Watch the settlements as well as the roads.',
      'world_spreading':'Coraline: It is spreading faster now. Closing real seams and machinery matters, but keep the Breach Drive supplied.',
      'world_avatar':'Coraline: The boundary has reached its limit. The White Void Avatar crisis must be resolved before ordinary containment can help.',
      'rewards':'Field reward: %s.',
      'destination':'%s — your charted location is %s, %s, %s in the Infinite Other World.',

      'goal_factory':'Investigate the clockwork factory. Stop the actual production press: disengage the red workroom clutch, blue hall clutch and green control-room clutch in that order, then switch off the central press. Open its secured archive with an Other Iron Lockpick and read the production orders. Then return to Coraline.',
      'goal_carnival':'Break the gallery’s repeating routine: ring its bell and hit all eight glowing targets from the firing lane. Return to Coraline afterward.',

      'goal_orphanage':'Read the admissions, orders and discharge records. Use the front-office register to free the trapped inhabitants, then return to Coraline.',

      'goal_theatre':'Help the surviving troupe by ending all eight timed stage phrases. Return to Coraline after the performance is broken.',

      'goal_sanctuary':'Start the sanctuary beacon’s defense. Protect it through the actual encounter, then tell Coraline what happened.',

      'goal_seam':'Investigate the physical seam. Match all three dials to their floor clues and close its core, then return to Wybie.',

      'goal_drill':'Infiltrate the drill center. Open security, disrupt both conduits, and disable the real drill core. Return to Wybie.',

      'goal_boundary':'Activate the ancient boundary beacon and finish its actual defense. Wybie needs the surviving instrument’s behavior, not a count of ordinary monster kills.',

      'goal_observatory':'Use a real spyglass beside the instrument to study all four constellation lights. Return to Wybie when the observatory’s research is complete.',

      'goal_heart_location':'Revisit this charted regional Heart Chamber and examine the actual structure. This is a research site, separate from the true central Heart.',

      'goal_heart':'Contain this regional Heart: defeat all eight admitted attackers while protecting its beacon before time expires. Then return to Wybie.',

      'completed':'%s is complete. Your field reward is recorded. Containment credit follows the current Breach Drive and Avatar rules.',

      'abandoned':'This assignment is set aside. Its world objective is unchanged. Speak again when you want another field assignment.',

      'finished_elsewhere':'That objective was completed outside this assignment. I will not count it as your field result. Speak again for another charted location.',

      'find_mine':'For the next research step, chart a real Soul Glass Mine. We need a genuine supply site before we assemble the components.',

      'components':'Bring four ordinary Soul Glass and two ordinary Other Diamonds. I need the real materials for the instrument; keep special named or bound items.',

      'components_received':'The components are assembled. Our next field step is stabilizing a charted observatory.',

      'reward_space':'Make room for the field reward, then speak to me again. Your completed objective is still recorded.',

      'save_paused':'Your assignment is safe, but the world’s save could not be confirmed. Reconnect after the save problem is resolved.',

      'warning':'Wybie: I think I found a way to destroy the entire Other World entity. But its portals will stop working. Your personal Other World, Soul Farms, workers and orphanage will be reset. Adopted children will leave the Coraline system. Coraline and I will not remain here in the same way, and you will have to start the story again. Learning how is optional and does not reset anything. Are you sure you want the instructions?',

      'how':'Tell Me How',

      'later':'I’m Not Ready',

      'keep_exploring':'Nothing has been reset. Keep exploring for as long as you want. You can return to this choice later; sneak-interact for another field assignment.',

      'instructions':'Wybie: At the central Heart, disable its reality anchors, survive the shifting world, and expose its living core. Entering is your choice. These instructions are preserved, and nothing has been reset. Sneak-interact if you want another ordinary field assignment instead.'}

    for key,text in messages.items():v['message.coraline.quest_'+key]=text

    v['message.coraline.factory_orders']='The control archive records batches of artificial workers: bodies assembled to order, assigned to production lines, and replaced when their seams fail. You have uncovered the factory’s purpose.'
    v['quest.coraline.tower']='Navigate the Mirror Tower'
    v['message.coraline.quest_goal_tower']='Climb the real spiral stair, find a route through the false mirror walls, and stand on the summit landing. Then return to Coraline.'
    write(path,v)

if __name__=='__main__':main()
