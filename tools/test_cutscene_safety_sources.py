"""Source-only regression guards, not Java compilation or native acceptance."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/com/coraline'


def source(path):
    return (JAVA / path).read_text(encoding='utf-8-sig')


class CutsceneSafetySources(unittest.TestCase):
    def test_shared_audio_timeout_runs_before_blocker_return(self):
        text = source('feast/ShowPlayback.java').split('public static boolean tick(', 1)[1]
        self.assertLess(text.index('safetyBudget'), text.index('if(!n.getString("Blocker").isEmpty())return false;'))
        self.assertIn('n.getInt("Duration")+600', text)
        self.assertIn('!n.getBoolean("SafetySkipped")', text)
        self.assertIn('safetyFinish(p,s,', text)

    def test_watchdog_survives_early_story_returns_but_not_player_input(self):
        text = source('feast/StoryCutsceneWatchdog.java')
        self.assertIn('@SubscribeEvent(priority=EventPriority.LOWEST)', text)
        self.assertIn('p.tickCount%20!=0', text)
        for chapter in ('FirstVisit', 'BlackIsTraditional', 'FirstEscapeStory', 'MissingParentsTea', 'MissingParentsCat', 'ParentRescueStory', 'RescueChapter'):
            self.assertIn(chapter + '.cutsceneSafety(p,s)', text)
        escape = source('feast/FirstEscapeStory.java').split('static void cutsceneSafety(', 1)[1].split('private static boolean readyForVoice(', 1)[0]
        motions = escape.split('boolean movement=', 1)[1].split(';', 1)[0]
        for deliberate in ('APOLOGY_CHOICE', 'MASK_REMOVAL', 'STITCH_REMOVAL', 'PUSH_WARDROBE', 'OPEN_LITTLE_DOOR', 'ESCAPE_TUNNEL'):
            self.assertNotIn(deliberate, motions)
        self.assertIn('state==ESCAPE_A', escape)

    def test_locked_doors_are_server_authoritative_and_explain_the_lock(self):
        text = source('otherworld/RealmProtection.java').split('public static void authoritativeStoryDoors(', 1)[1].split('@SubscribeEvent', 1)[0]
        for guard in ('isClientSide', 'OtherWorld.isOther', 'instanceof DoorBlock', 'setUseBlock', 'setUseItem', 'setCanceled(true)', 'InteractionResult.SUCCESS'):
            self.assertIn(guard, text)
        block = source('feast/BlackIsTraditional.java').split('public static boolean block(', 1)[1].split('static boolean lockedDoor(', 1)[0]
        self.assertIn('The door is locked.', block)
        self.assertNotIn('closeDoor(p,at)', block)

    def test_parlor_lighting_is_local_and_prison_has_priority(self):
        client = source('client/FirstEscapeClient.java').split('public static boolean brightParlor()', 1)[1].split('public static float parlorLightBlend()', 1)[0]
        self.assertIn('AABB', client)
        self.assertIn('contains(mc.player.position())', client)
        lighting = source('client/OtherWorldLighting.java')
        self.assertLess(lighting.index('FirstEscapeClient.prison()'), lighting.index('FirstEscapeClient.parlorLightBlend()'))
        self.assertLess(lighting.index('FirstEscapeClient.parlorLightBlend()'), lighting.index('TraditionalClient.darkHouse()'))
        self.assertIn('Mth.lerp(parlor,', lighting)
        self.assertIn('carryParlorLight(phase(),held())', client)
        rules = source('feast/FirstEscapeVisualRules.java')
        self.assertIn('CocoaBeetlesChoreography.ramp(ticks,80,90)', rules)
        self.assertIn('roomMin(s).asLong()', source('feast/FirstEscapeStory.java'))
        self.assertIn('roomMin(VisitData.Scene s){return at(s,14,6,32);}', source('feast/FirstEscapeScene.java'))

    def test_server_only_door_sound_is_broadcast_to_the_clicker_too(self):
        doors = source('otherworld/AuthoritativeHouseDoors.java')
        self.assertIn('door.setOpen(null,level,state,at,open)', doors)
        self.assertIn('!door.type().canOpenByHand()', doors)
        self.assertIn('door instanceof com.coraline.block.LockedHouseDoorBlock', doors)
        self.assertIn('AuthoritativeHouseDoors.toggle(visitor,e.getPos())', source('otherworld/RealmProtection.java'))
        self.assertIn('AuthoritativeHouseDoors.setOpen', source('secrets/HouseCabinets.java'))

    def test_reveal_portal_is_prepared_before_outer_door_opens(self):
        text = source('feast/FirstEscapeStory.java')
        sealed = text.split('private static void prepareSealedRoom(', 1)[1].split('private static DoorData.Door', 1)[0]
        self.assertIn('openLittleDoor(p,s,true)', sealed)
        self.assertLess(sealed.index('setRoomDoors(p,s,false)'), sealed.index('openLittleDoor(p,s,true)'))
        self.assertIn('openLittleDoor(p,s,state.ordinal()<MIRROR_A.ordinal())', text)
        self.assertIn('if(next==PUSH_WARDROBE){releaseGuide(p,s);openLittleDoor(p,s,false);}', text)

    def test_chair_uses_cached_floor_route_and_only_records_confirmed_mount(self):
        text = source('feast/FirstEscapeStory.java').split('private static void capture(', 1)[1].split('private static void grab(', 1)[0]
        for guard in ('CinematicFloorRoute.nearest', 'CinematicFloorRoute.move', 'stalled>=60||time>=400', 'recoverChairCapture', 'if(p.startRiding(chair,true))'):
            self.assertIn(guard, text)
        route = source('feast/CinematicFloorRoute.java')
        for guard in ('previous.size()<512', 'state.getLong("Goal")', 'PartyFloor.at', 'getBlockCollisions', 'FirstEscapeScene.inRoom'):
            self.assertIn(guard, route)

    def test_changed_java_delimiters(self):
        paths = [
            'feast/BedroomBarricade.java', 'feast/CinematicFloorRoute.java',
            'feast/StoryCutsceneWatchdog.java', 'feast/ShowPlayback.java',
            'feast/FirstEscapeStory.java', 'feast/BlackIsTraditional.java',
            'feast/MissingParentsTea.java', 'feast/MissingParentsCat.java',
            'feast/ParentRescueStory.java', 'feast/RescueChapter.java',
            'feast/FirstVisit.java', 'otherworld/RealmProtection.java',
            'tycoon/TycoonPersonality.java', 'tycoon/TycoonDollAnalysis.java',
            'tycoon/TycoonData.java', 'tycoon/TycoonPhaseTwo.java',
            'tycoon/TycoonPhaseTwoMenus.java', 'client/TycoonWorldScreen.java',
        ]
        ignored = re.compile(r'""".*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/', re.S)
        for path in paths:
            with self.subTest(path=path):
                stack = []
                for char in ignored.sub('', source(path)):
                    if char in '({[':
                        stack.append(char)
                    elif char in ')}]':
                        self.assertTrue(stack, path)
                        self.assertEqual(stack.pop(), {')': '(', '}': '{', ']': '['}[char], path)
                self.assertEqual(stack, [], path)


if __name__ == '__main__':
    unittest.main()
