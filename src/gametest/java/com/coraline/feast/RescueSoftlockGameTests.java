package com.coraline.feast;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class RescueSoftlockGameTests {
    @GameTest(template="empty", batch="story_softlocks")
    public static void ratEyeCannotBeClaimedBeforeReturnFromCircus(GameTestHelper helper) {
        helper.assertFalse(RescueEncounters.canClaimRatEye(0), "A rat still in the circus cannot award the final eye");
        helper.assertFalse(RescueEncounters.canClaimRatEye(1), "A rat whose owner has not returned to the attic cannot award the final eye");
        helper.assertTrue(RescueEncounters.canClaimRatEye(2), "The committed post-return chase leg must remain playable");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void unarmedWeakPointStrikesRemainPossibleButFair(GameTestHelper helper) {
        helper.assertFalse(RescueBosses.validStrike(true, 1.0, .79F), "Unarmed strikes still obey the normal swing cooldown");
        helper.assertTrue(RescueBosses.validStrike(true, 1.0, .8F), "A correctly timed unarmed weakpoint strike is valid");
        helper.assertFalse(RescueBosses.validStrike(false, 1.0, 1.0F), "An ineffective held item is not treated as an unarmed strike");
        helper.assertTrue(Math.abs(2.0F-RescueBosses.strikeDamage(true, 1.0, RescueChapter.Phase.MANTIS_FIGHT))<.001F, "Unarmed damage stays deliberately low");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void hiddenArrivalUsesStricterRadiusThanConcealment(GameTestHelper helper) {
        var target=new net.minecraft.core.BlockPos(0,0,0);
        helper.assertFalse(GhostVictimNavigation.arrived(new net.minecraft.world.phys.Vec3(1.16,0,.5),target), "Navigation must not stop outside the .65 concealment radius");
        helper.assertTrue(GhostVictimNavigation.arrived(new net.minecraft.world.phys.Vec3(.5,0,.5),target), "The center of the validated hiding position is a successful arrival");
        helper.assertTrue(PalaceTransport.MOVER!=CinematicPropEntity.TAFFY_BOWL && !RescueProps.kind(PalaceTransport.MOVER), "Moving Company workers must not share a kind with tea or rescue props");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void invisibleRescueEyesCannotBlockTheVisiblePickTarget(GameTestHelper helper) {
        var scene=new VisitData.Scene();
        scene.owner=java.util.UUID.randomUUID();
        scene.door=java.util.UUID.randomUUID();
        scene.loop.event.putInt("Serial",42);
        var eye=CinematicPropEntity.TYPE.get().create(helper.getLevel());
        helper.assertTrue(eye!=null,"Rescue eye entity type did not register");
        eye.configure(scene,RescueProps.EYE,0);
        helper.assertTrue(eye.isPickable(),"A visible authentic eye must be targetable");
        eye.setInvisible(true);
        helper.assertFalse(eye.isPickable(),"A hidden eye must not steal clicks from the visible boss or reward");
        eye.setInvisible(false);
        helper.assertTrue(eye.isPickable(),"The offered eye must become targetable when revealed");
        var ball=CinematicPropEntity.TYPE.get().create(helper.getLevel());
        var rat=com.coraline.registry.ModEntities.MOUSE.get().create(helper.getLevel());
        helper.assertTrue(ball!=null&&rat!=null,"Ratmaster original entity types did not register");
        ball.configure(scene,RescueProps.RAT_BALL,0);
        helper.assertTrue(ball.isPickable(),"The circus ball must accept the player's right-click");
        helper.assertTrue(ball.getPassengersRidingOffset()+rat.getBbHeight()<1.35,
            "The original rat is not concealed inside the ball's 1.35-block pick box");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void canonicalKitchenSmokerCanStartParentRescueDinner(GameTestHelper helper) {
        for(String name:new String[]{"smoker","furnace","blast_furnace","oven","oak_stove"})
            helper.assertTrue(ParentRescueStory.kitchenStoveId(name),
                "Known kitchen appliance should be accepted: "+name);
        for(String name:new String[]{"air","stone","oak_log","bookshelf"})
            helper.assertFalse(ParentRescueStory.kitchenStoveId(name),
                "Non-appliance incorrectly accepted as kitchen stove: "+name);
        helper.assertFalse(ParentRescueStory.kitchenStoveId(null),
                "Unregistered/null block name should not crash the kitchen search");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void parentRescueKitchenServingWaitsBeforeRecoveringOriginalActor(GameTestHelper helper) {
        helper.assertFalse(ParentRescueStory.serveRecoveryDue(0),
            "The normal physical kitchen serving approach should get its turn");
        helper.assertFalse(ParentRescueStory.serveRecoveryDue(359),
            "The original Other Mother was moved before navigation grace elapsed");
        helper.assertTrue(ParentRescueStory.serveRecoveryDue(360),
            "A permanently blocked serving approach would never become recoverable");
        helper.assertTrue(ParentRescueStory.serveRecoveryDue(1000),
            "Old or restarted serving checkpoints should stay recoverable");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void finaleOpenedFloorRecoversOnlyAfterRecordedFallCueAndGrace(GameTestHelper helper) {
        int duration=ShowRecordings.duration("finale_o");
        helper.assertTrue(duration>0,"Original final floor-collapse recording is missing");
        int recovery=Math.max(120,duration+80);
        helper.assertFalse(BeldamFinale.openedFloorFallRecoveryDue(0,duration),
            "No opening exists at start of the final collapse");
        helper.assertFalse(BeldamFinale.openedFloorFallRecoveryDue(recovery-1,duration),
            "Recovery would teleport the player before the original fall sequence finishes");
        helper.assertTrue(BeldamFinale.openedFloorFallRecoveryDue(recovery,duration),
            "Standing on the surviving rim could stall the final fall permanently");
        helper.assertFalse(BeldamFinale.openedFloorFallRecoveryDue(100,1000),
            "A much longer valid recording would be truncated by fallback timing");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void rebirthResetsTheTeaModuleOwnedKey(GameTestHelper helper) {
        helper.assertTrue(MainStoryRebirth.STORY_KEYS.contains(MissingParentsTea.KEY),
            "A new run must retire the tea module's actual saved story state");
        helper.assertTrue(MainStoryRebirth.STORY_KEYS.contains(AcceptedButtonEyesProgress.KEY)
                && MainStoryRebirth.STORY_KEYS.contains("AlternativeEndingComplete"),
            "A new run must retire the completed alternative route's active state");
        helper.succeed();
    }
}
