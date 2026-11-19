"""Read-only regressions for the post-boss reunion -> Cat -> dream -> map -> well route.

Supplementary static checks; the runnable PalaceWellResumeRulesCheck.java exercises
the pure Java reconnect rule. Neither replaces native Minecraft integration tests.
"""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'src/main/java/com/coraline'
def java(path):
    return (SRC/path).read_text(encoding='utf-8-sig')

class PostBossRouteRepairs(unittest.TestCase):
    def test_reunion_and_dream_preserve_physical_gates(self):
        finale=java('feast/RescueEpilogue.java')
        for phase in ('REUNION_A,REUNION_B','CAT_NOTICE','FAREWELL_A','FAREWELL_B',
                      'EYES_CLOSE','CELESTIAL_OPEN','ORBS','FAREWELL_C'):
            self.assertIn(phase,finale)
        self.assertIn('if(!reunionReady(s))return;',finale)
        self.assertIn('RescueEyes.release(p,s)',finale)
        self.assertIn('verifyVisionLanding(p,n)',finale)
        safety=java('feast/RescueChapter.java')
        self.assertIn('state.ordinal()<Phase.REUNION_A.ordinal()',safety)
        self.assertNotIn('if(RescueBosses.fighting(state)||state.ordinal()>=Phase.BOB_A.ordinal())return;',safety)

    def test_canonical_farewell_cat_survives_event_serial_change(self):
        scene=java('feast/ShowActors.java')
        adapter=scene.split('private static void reconcileFarewellCat(',1)[1].split('static Mob recoverCat(',1)[0]
        self.assertIn('RescueChapter.Phase.CAT_NOTICE',adapter)
        self.assertIn('lease.getUUID("Entity")',adapter)
        self.assertIn('cat.characterId().equals("cat")',adapter)
        self.assertIn('tag.putInt("ShowActorSerial",s.loop.event.getInt("Serial"))',adapter)
        self.assertIn('rememberCat(s,cat)',adapter)
        self.assertIn('reconcileFarewellCat(p,s);',scene)

    def test_map_generation_and_recovery_use_consistent_proof(self):
        journey=java('feast/PalaceJourney.java')
        self.assertIn('companyMapReady()',journey)
        self.assertIn('if(!ownsTransport(p,scene)||!ParentRescueStory.key(',journey)
        self.assertIn('PalaceWorldGeneration.requireCompany(p.server)',journey)
        world=java('palace/PalaceWorldGeneration.java')
        self.assertIn('d.continueReservedCompany()',world)
        sign=world.split('private static void signs(',1)[1].split('private static void finish(',1)[0]
        self.assertIn('LegacyActorRecovery.check(l.getServer(),id)',sign)
        self.assertIn('sign.setUUID(id)',sign)
        self.assertIn('getBoolean("CoralineWorldLandmark")',sign)

    def test_palace_visitor_does_not_pause_distant_well_generation(self):
        world=java('palace/PalaceWorldGeneration.java')
        occupied=world.split('private static boolean occupied(',1)[1].split('/** Structure bounding boxes',1)[0]
        self.assertIn('PalaceGenerationOccupancy.pauses(',occupied)
        rules=java('palace/PalaceGenerationOccupancy.java')
        self.assertIn('stage.equals("WELL")||stage.equals("WELL_VERIFY")',rules)
        self.assertIn('stage.equals("SIGNS")||stage.equals("FINISH")',rules)
        self.assertIn('WELL_PAUSE_RADIUS=56',rules)
        self.assertIn('palaceClaimed&&near(x,z,palaceX,palaceZ',rules)

    def test_well_bicycle_admission_and_reconnect_are_original_only(self):
        stage=java('feast/PalaceWellEncounter.java')
        bike=stage.split('private static WybieBicycleEntity bike(',1)[1].split('private static Vec3 entryTarget(',1)[0]
        self.assertLess(bike.index('if(!savedBikeReservation(p,s,b.getUUID()))return null;'),
                        bike.index('if(!p.serverLevel().addFreshEntity(b))return null;'))
        self.assertIn('b.resumeWellStory(s.owner,s.door,w.getUUID(),progress.session())',bike)
        self.assertIn('LegacyActorRecovery.Result.ABSENT',bike)
        save=stage.split('private static boolean savedBikeReservation(',1)[1].split('private static WybieBicycleEntity bike(',1)[0]
        self.assertIn('data/coraline_first_visits.dat',save)
        self.assertIn('stage.getUUID("Bike").equals(id)',save)
        original=java('feast/WybieBicycleEntity.java')
        self.assertIn('PalaceWellResumeRules.canRebind',original)
        self.assertIn('entityData.set(SESSION,java.util.Optional.of(nextSession))',original)

    def test_well_boss_recovery_does_not_lose_combat_completion(self):
        stage=java('feast/PalaceWellEncounter.java')
        battle=stage.split('private static void boss(',1)[1].split('public static boolean interact(',1)[0]
        self.assertLess(battle.index('if(fight.complete()){progress.defeated('),
                        battle.index('if(!fight.session().equals(progress.session()))'))
        self.assertIn('Interrupted Hand telegraph skipped safely',battle)
        self.assertIn('fight.hit(',stage)
        self.assertIn('progress.defeated(',stage)
        self.assertIn('progress.shattered(',stage)
        self.assertIn('progress.wrapped(',stage)

    def test_disposal_remains_bound_to_real_key_and_well_shaft(self):
        custody=java('feast/PalaceKeyCustody.java')
        self.assertIn('Original-key/bundle/well receipt mismatch',custody)
        self.assertIn('state.phase()!=PalaceWellProgress.Phase.KEY_BUNDLED',custody)
        self.assertIn('bundle.getY()>rim.getY()-12',custody)
        self.assertIn('state.disposed(s.owner,state.session(),tx.receipt(),true,heard)',custody)
        self.assertIn('if(!saveStory(p,s))',custody)

if __name__=='__main__':
    unittest.main()
