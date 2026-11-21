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
    public static void missingTeaVisitorsRequireNativeProofOfAbsenceBeforeOriginalUuidRecovery(GameTestHelper helper) {
        for(var result:com.coraline.rebirth.LegacyActorRecovery.Result.values())
            helper.assertTrue(MissingParentsTea.teaActorMissingProof(result)
                    ==(result==com.coraline.rebirth.LegacyActorRecovery.Result.ABSENT),
                "An unloaded, present or unverifiable visitor would be incorrectly duplicated: "+result);
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
    public static void interruptedKitchenSaveCannotAdvanceWithMissingStoveOrTable(GameTestHelper helper) {
        var n=new net.minecraft.nbt.CompoundTag();
        helper.assertFalse(ParentRescueStory.kitchenCheckpointComplete(n),
                "Uninitialized kitchen was marked ready");
        n.putLong("Kitchen",new net.minecraft.core.BlockPos(30,80,30).asLong());
        helper.assertFalse(ParentRescueStory.kitchenCheckpointComplete(n),
                "Saved kitchen marker skipped stove/table placement");
        n.putLong("Stove",new net.minecraft.core.BlockPos(28,80,35).asLong());
        helper.assertFalse(ParentRescueStory.kitchenCheckpointComplete(n),
                "Missing kitchen table was treated as complete");
        n.putLong("KitchenTable",new net.minecraft.core.BlockPos(31,80,30).asLong());
        helper.assertFalse(ParentRescueStory.kitchenCheckpointComplete(n),
                "Wrong table anchor was trusted after save/reload");
        n.putLong("KitchenTable",new net.minecraft.core.BlockPos(30,80,30).asLong());
        helper.assertTrue(ParentRescueStory.kitchenCheckpointComplete(n),
                "Complete owner-authored kitchen checkpoint was rejected");
        n.putLong("Stove",new net.minecraft.core.BlockPos(80,80,30).asLong());
        helper.assertFalse(ParentRescueStory.kitchenCheckpointComplete(n),
                "Corrupt distant saved stove would load an unrelated world chunk");
        n.putLong("Stove",new net.minecraft.core.BlockPos(28,90,35).asLong());
        helper.assertFalse(ParentRescueStory.kitchenCheckpointComplete(n),
                "Wrong-floor oven was treated as the original kitchen stove");
        n.putString("Stove","invalid-stove-tag");
        helper.assertFalse(ParentRescueStory.kitchenCheckpointComplete(n),
                "Corrupt stove data reached the cooking sequence");
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
    public static void savedUnrescuedCatCannotDisappearFromCollapsePickup(GameTestHelper helper) {
        helper.assertTrue(RescueChallengeMath.restageUnrescuedCat(false,false,0,false),
                "An unplaced original Cat was incorrectly accepted as ready");
        helper.assertFalse(RescueChallengeMath.restageUnrescuedCat(true,false,64,false),
                "A Cat near its saved physical pickup anchor was moved");
        helper.assertTrue(RescueChallengeMath.restageUnrescuedCat(true,false,65,false),
                "A stranded saved Cat was not restored to its checkpoint");
        helper.assertTrue(RescueChallengeMath.restageUnrescuedCat(true,false,1,true),
                "A Cat trapped inside a changed block was left unreachable");
        helper.assertTrue(RescueChallengeMath.restageUnrescuedCat(true,false,Double.NaN,false),
                "An invalid Cat distance was mistaken for a safe checkpoint");
        helper.assertFalse(RescueChallengeMath.restageUnrescuedCat(false,true,Double.POSITIVE_INFINITY,true),
                "The original Cat was teleported after being rescued");
        helper.assertFalse(RescueChallengeMath.restageUnrescuedCat(true,true,99999,true),
                "A rescued Cat was reset by a stale pickup location");
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

    @GameTest(template="empty", batch="story_softlocks")
    public static void teaCutsceneNeverSkipsUnloadedOriginalVisitors(GameTestHelper helper) {
        helper.assertTrue(MissingParentsTea.visitorsReadyForScene(MissingParentsTea.Phase.WAITING_FOR_KNOCK,false,false),
            "Invitation must not require actors before the visit begins");
        for(var phase:new MissingParentsTea.Phase[]{MissingParentsTea.Phase.KNOCKING,
                MissingParentsTea.Phase.GREETING,MissingParentsTea.Phase.FORCIBLE_ENTRANCE,
                MissingParentsTea.Phase.STORY_H,MissingParentsTea.Phase.STORY_I,
                MissingParentsTea.Phase.WAITING_FOR_EXIT}) {
            helper.assertFalse(MissingParentsTea.visitorsReadyForScene(phase,false,true),
                "Without authentic Spink the tea scene could advance: "+phase);
            helper.assertFalse(MissingParentsTea.visitorsReadyForScene(phase,true,false),
                "Without authentic Forcible the tea scene could advance: "+phase);
            helper.assertTrue(MissingParentsTea.visitorsReadyForScene(phase,true,true),
                "Both authenticated native visitors should resume: "+phase);
        }
        helper.assertTrue(MissingParentsTea.visitorsReadyForScene(MissingParentsTea.Phase.COMPLETED,false,false),
            "Finished tea visit must not be reopened for retired actors");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void reunionAudioWaitsForOriginalParentsAtSavedStage(GameTestHelper helper) {
        helper.assertFalse(RescueEpilogue.parentsOnReunionStage(false,true,true,0,0),
            "A missing parent-placement receipt cannot be treated as staged");
        helper.assertFalse(RescueEpilogue.parentsOnReunionStage(true,false,true,0,0),
            "A real mother in another world cannot play the Overworld reunion");
        helper.assertFalse(RescueEpilogue.parentsOnReunionStage(true,true,false,0,0),
            "A real father in another world cannot play the Overworld reunion");
        helper.assertFalse(RescueEpilogue.parentsOnReunionStage(true,true,true,Double.NaN,0),
            "A corrupt mother position must not be trusted");
        helper.assertFalse(RescueEpilogue.parentsOnReunionStage(true,true,true,0,Double.POSITIVE_INFINITY),
            "An unbounded father position must not be trusted");
        helper.assertFalse(RescueEpilogue.parentsOnReunionStage(true,true,true,1297,0),
            "A stranded real parent beyond the reunion stage must be re-staged");
        helper.assertTrue(RescueEpilogue.parentsOnReunionStage(true,true,true,1296,1296),
            "Both genuine parent actors within the safe checkpoint may resume");
        helper.assertTrue(RescueEpilogue.parentsOnReunionStage(true,true,true,4,4),
            "Normal two-actor physical stage must not be reset during dialogue");
        helper.succeed();
    }
}
