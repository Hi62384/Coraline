package com.coraline.feast;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class RescueSoftlockGameTests {
    @GameTest(template="empty", batch="story_softlocks")
    public static void finalCatSwatRetainsACompleteFallbackWhenAudioIsMissing(GameTestHelper helper) {
        int fallback=RescueWebLayout.catSwatDuration(0);
        helper.assertTrue(fallback>=120,"Missing original audio shortened the physical Cat-swat window");
        helper.assertFalse(RescueWebLayout.swatAt(fallback-41,fallback),"Cat swat began before the fallback cue");
        helper.assertTrue(RescueWebLayout.swatAt(105,fallback),"Cat swat never starts during the audio-failure recovery");
        helper.assertTrue((105-(fallback-40))/25.0>=1,"Cat cannot complete its saved swat after dialogue fallback");
        helper.assertTrue(RescueWebLayout.catSwatDuration(341)==341,"Healthy packaged Cat audio timing was altered");
        helper.succeed();
    }
    @GameTest(template="empty", batch="story_softlocks")
    public static void finalWebDoorActionsRejectWrongRealmAndDistantPlayer(GameTestHelper helper) {
        for(var state:new RescueChapter.Phase[]{RescueChapter.Phase.DOOR_KICK,
                RescueChapter.Phase.DOOR_STRUGGLE,RescueChapter.Phase.HAND_FALL}){
            helper.assertTrue(BeldamFinale.webDoorStageNeedsRecovery(state,false,0),
                    "Final web action could advance in another dimension: "+state);
            helper.assertTrue(BeldamFinale.webDoorStageNeedsRecovery(state,true,225),
                    "Final web action could advance far from its door: "+state);
            helper.assertTrue(BeldamFinale.webDoorStageNeedsRecovery(state,true,Double.NaN),
                    "Invalid saved coordinates bypassed the final web stage: "+state);
            helper.assertFalse(BeldamFinale.webDoorStageNeedsRecovery(state,true,4),
                    "A player at the authentic web door was rejected: "+state);
        }
        helper.assertFalse(BeldamFinale.webDoorStageNeedsRecovery(RescueChapter.Phase.CAT_THROW,false,999),
                "The web-only gate changed the earlier Beldam room scene");
        helper.succeed();
    }
    @GameTest(template="empty", batch="story_softlocks")
    public static void staleDoorOwnerWithoutOriginalSceneCannotCrashOrReopenPassage(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        var missing=java.util.UUID.randomUUID();
        helper.assertFalse(VisitData.get(server).scenes.containsKey(missing),
            "Missing-scene fixture unexpectedly reused a live owner");
        helper.assertTrue(StoryTunnel.sealed(server,missing),
            "An orphaned Little Door must fail closed instead of dereferencing a deleted story scene");
        helper.assertFalse(StoryTunnel.permanent(server,missing),
            "An orphaned Little Door must not expose a return tunnel");
        helper.assertTrue(StoryTunnel.sealed(null,missing),
            "A missing server cannot authenticate an original Little Door");
        helper.assertFalse(StoryTunnel.permanent(null,missing),
            "A missing server must not advertise an active story passage");
        helper.succeed();
    }
    @GameTest(template="empty", batch="story_softlocks")
    public static void dreamCutsceneCannotAdvanceFromWrongRealmOrWithoutRealArrival(GameTestHelper helper) {
        var server=helper.getLevel().getServer();var stage=server.getLevel(ActivityWorld.KEY);
        helper.assertTrue(stage!=null,"The authentic dream Activity World must exist");
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DreamGateQA");
        var p=new net.minecraftforge.common.util.FakePlayer(stage,profile);
        var s=new VisitData.Scene();s.owner=profile.getId();s.door=java.util.UUID.randomUUID();
        var saved=RescueChapter.data(s);
        var pos=new net.minecraft.core.BlockPos(20,100,20);
        p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));
        helper.assertFalse(RescueEpilogue.visionStageReady(p,s,RescueChapter.Phase.CELESTIAL_OPEN),
            "A celestial scene without its recorded physical arrival was allowed to advance");
        saved.putLong("VisionAt",pos.asLong());
        helper.assertTrue(RescueEpilogue.visionStageReady(p,s,RescueChapter.Phase.CELESTIAL_OPEN),
            "A physically reached saved dream checkpoint was rejected");
        p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos.offset(9,0,0)));
        helper.assertFalse(RescueEpilogue.visionStageReady(p,s,RescueChapter.Phase.CELESTIAL_OPEN),
            "The dream watchdog could run while the player was outside the authentic scene");
        var outside=new net.minecraftforge.common.util.FakePlayer(server.overworld(),
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"DreamWrongQA"));
        outside.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));
        helper.assertFalse(RescueEpilogue.visionStageReady(outside,s,RescueChapter.Phase.FAREWELL_C),
            "Farewell dialogue could finish in the real world rather than the dream");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void theIndependentCutsceneStageGateSeesActualServerRealm(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        var activity=server.getLevel(ActivityWorld.KEY);
        var other=server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        helper.assertTrue(activity!=null&&other!=null,"Both original stage dimensions must be registered");
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"RealmGateQA");
        var onStage=new net.minecraftforge.common.util.FakePlayer(activity,profile);
        var scene=new VisitData.Scene();scene.owner=profile.getId();scene.door=java.util.UUID.randomUUID();
        helper.assertFalse(RescueEncounters.stageDimensionReady(onStage,scene,RescueChapter.Phase.GARDEN_SEARCH),
            "Garden dialogue could advance in the Activity World");
        helper.assertTrue(RescueEncounters.stageDimensionReady(onStage,scene,RescueChapter.Phase.THEATRE_SEARCH),
            "The original theatre stage was rejected");
        RescueChapter.data(scene).putInt("RatLeg",2);
        helper.assertFalse(RescueEncounters.stageDimensionReady(onStage,scene,RescueChapter.Phase.RAT_CHASE),
            "Rat chase after its attic return could still tick in the old circus");
        RescueChapter.data(scene).putInt("RatLeg",1);
        helper.assertTrue(RescueEncounters.stageDimensionReady(onStage,scene,RescueChapter.Phase.RAT_CHASE),
            "The rat handoff did not allow the original circus");
        var inHouse=new net.minecraftforge.common.util.FakePlayer(other,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"HouseStageQA"));
        helper.assertTrue(RescueEncounters.stageDimensionReady(inHouse,scene,RescueChapter.Phase.GARDEN_SEARCH),
            "The correct garden world was rejected");
        helper.assertTrue(RescueEncounters.stageDimensionReady(inHouse,scene,RescueChapter.Phase.RAT_CHASE),
            "The rat return was blocked while the two-world handoff was in progress");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void noncombatStageRecoveryDoesNotUseBossOrWrongDimension(GameTestHelper helper) {
        for(var phase:java.util.List.of(RescueChapter.Phase.GARDEN_SEARCH,
                RescueChapter.Phase.MANTIS_TAKE,RescueChapter.Phase.ATTIC_SEARCH,
                RescueChapter.Phase.HOUSE_SAFE,RescueChapter.Phase.ROOM_LOCKED)) {
            helper.assertFalse(RescueEncounters.activityCheckpoint(phase,0),
                "Other World checkpoint would recover into the activity stage: "+phase);
            helper.assertFalse(RescueBosses.fighting(phase),
                "Noncombat checkpoint was incorrectly classified as a boss: "+phase);
        }
        for(var phase:java.util.List.of(RescueChapter.Phase.THEATRE_SEARCH,
                RescueChapter.Phase.THEATRE_EYE,RescueChapter.Phase.BOB_E,
                RescueChapter.Phase.SWARM_TRANSFORM)) {
            helper.assertTrue(RescueEncounters.activityCheckpoint(phase,0),
                "Theatre/circus checkpoint must recover into its original activity stage: "+phase);
            helper.assertFalse(RescueBosses.fighting(phase),
                "Noncombat activity checkpoint was treated as a boss: "+phase);
        }
        helper.assertTrue(RescueEncounters.activityCheckpoint(RescueChapter.Phase.RAT_CHASE,0),
            "The first rat segment is still in the circus");
        helper.assertFalse(RescueEncounters.activityCheckpoint(RescueChapter.Phase.RAT_CHASE,2),
            "The returned rat must not reset to the original circus");
        helper.assertTrue(RescueBosses.fighting(RescueChapter.Phase.MANTIS_FIGHT)
                &&RescueBosses.fighting(RescueChapter.Phase.THEATRE_FIGHT)
                &&RescueBosses.fighting(RescueChapter.Phase.SWARM_FIGHT),
            "Actual boss retries must keep their own combat checkpoints");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void invalidEscapeRoomCleanupJournalCannotCrashOrEraseUnownedBlocks(GameTestHelper helper) {
        var level=helper.getLevel();
        helper.assertTrue(level.getServer().getLevel(com.coraline.otherworld.OtherWorld.KEY)!=null,
            "The original Other World must exist for the escape cleanup test");
        var player=new net.minecraftforge.common.util.FakePlayer(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EscapeRoomQA"));
        var scene=new VisitData.Scene();
        scene.owner=player.getUUID();
        scene.door=java.util.UUID.randomUUID();
        var saved=FirstEscapeStory.data(scene);
        var journal=new net.minecraft.nbt.CompoundTag();
        journal.put("malformed-escaped-room-position",new net.minecraft.nbt.CompoundTag());
        journal.put(Long.toString(new net.minecraft.core.BlockPos(0,-512,0).asLong()),
            new net.minecraft.nbt.CompoundTag());
        saved.put("Environment",journal);
        FirstEscapeScene.restore(player,scene);
        helper.assertTrue(journal.size()==2,
            "Malformed or unowned cleanup records must remain unresolved, not be deleted");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void malformedCollapseCheckpointRestartsTheRealExteriorObjective(GameTestHelper helper) {
        var saved=new net.minecraft.nbt.CompoundTag();
        helper.assertFalse(RescueEncounters.collapseCheckpointValid(saved),
            "A saved phase marker without physical collapse data cannot retry");
        saved.putLong("CollapseStart",new net.minecraft.core.BlockPos(30,74,40).asLong());
        helper.assertFalse(RescueEncounters.collapseCheckpointValid(saved),
            "A collapse position alone cannot substitute for its clock and radius");
        saved.putInt("CollapseDuration",1500);
        helper.assertFalse(RescueEncounters.collapseCheckpointValid(saved),
            "A missing initial collapse radius must not cause an instant defeat loop");
        saved.putDouble("CollapseInitialRadius",Double.NaN);
        helper.assertFalse(RescueEncounters.collapseCheckpointValid(saved),
            "An invalid saved radius cannot reach the collapse retry");
        saved.putDouble("CollapseInitialRadius",180);
        helper.assertTrue(RescueEncounters.collapseCheckpointValid(saved),
            "The original fully committed exterior checkpoint must remain valid");
        saved.putInt("CollapseDuration",0);
        helper.assertFalse(RescueEncounters.collapseCheckpointValid(saved),
            "A zero-duration collapse must roll back to the real exterior gate");
        saved.putInt("CollapseDuration",1500);
        saved.putLong("CollapseStart",0);
        helper.assertFalse(RescueEncounters.collapseCheckpointValid(saved),
            "An absent/default origin must not teleport the owner to (0,0,0)");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void interruptedWybieShoeMissKeepsSameActorAndNeverAutoCompletes(GameTestHelper helper) {
        helper.assertTrue(FirstEscapeTimeline.after(FirstEscapeTimeline.Phase.THROW_SHOE)==null,
            "The shoe scene must never time-advance after an ordinary miss");
        var level=helper.getLevel();
        var player=new net.minecraftforge.common.util.FakePlayer(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ShoeMissQA"));
        var scene=new VisitData.Scene();
        scene.owner=player.getUUID();
        scene.door=java.util.UUID.randomUUID();
        scene.loop.event.putInt("Serial",731);
        var saved=FirstEscapeStory.data(scene);
        saved.putBoolean("Started",true);
        saved.putString("Phase",FirstEscapeTimeline.Phase.THROW_SHOE.name());
        var shoe=CinematicPropEntity.TYPE.get().create(level);
        helper.assertTrue(shoe!=null,"The original shoe entity type is missing");
        shoe.configure(scene,CinematicPropEntity.STORY_SHOE,0);
        shoe.setPos(helper.absolutePos(new net.minecraft.core.BlockPos(1,2,1)).getCenter());
        shoe.frame(1,0,0); // Physical miss survived; retry flag did not.
        helper.assertTrue(level.addFreshEntity(shoe),"Could not create original saved shoe");
        saved.putUUID("ShoeFlight",shoe.getUUID());
        saved.putLong("ShoeFlightPosition",shoe.blockPosition().asLong());
        VisitData.get(level.getServer()).scenes.put(scene.owner,scene);
        FirstEscapeStory.shoe(player,scene);
        helper.assertTrue(saved.getBoolean("ShoeMissed"),
            "A partially saved missed shoe must re-enable the player's Use retry");
        helper.assertTrue(FirstEscapeStory.phase(scene)==FirstEscapeTimeline.Phase.THROW_SHOE,
            "A missed shoe incorrectly advanced Wybie's scene");
        helper.assertTrue(shoe.isAlive()&&shoe.cue()==1&&shoe.getUUID().equals(saved.getUUID("ShoeFlight")),
            "Retry recovery must retain the same canonical projectile and identity");
        shoe.discard();
        VisitData.get(level.getServer()).scenes.remove(scene.owner);
        helper.succeed();
    }

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
        helper.assertFalse(RescueEpilogue.reunionViewerNear(false,0),
            "Post-boss reunion must not play with Coraline stranded in another dimension");
        helper.assertFalse(RescueEpilogue.reunionViewerNear(true,4097),
            "Offstage player should get a return hint, not silent voiced progression");
        helper.assertFalse(RescueEpilogue.reunionViewerNear(true,Double.NaN),
            "A malformed saved viewpoint must not count as present");
        helper.assertTrue(RescueEpilogue.reunionViewerNear(true,4096),
            "Player within the original door checkpoint can view the reunion");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void postBossScenesMustPreserveOriginalWorldAndCast(GameTestHelper helper) {
        for(var phase:new RescueChapter.Phase[]{RescueChapter.Phase.REUNION_A,
                RescueChapter.Phase.REUNION_B,RescueChapter.Phase.CAT_NOTICE,
                RescueChapter.Phase.FAREWELL_A,RescueChapter.Phase.FAREWELL_B})
            helper.assertTrue(RescueEpilogue.requiresEpilogueStage(phase),
                "Post-boss stage or original Cat readiness was skipped: "+phase);
        for(var phase:new RescueChapter.Phase[]{RescueChapter.Phase.EYES_CLOSE,
                RescueChapter.Phase.CELESTIAL_OPEN,RescueChapter.Phase.ORBS,
                RescueChapter.Phase.FAREWELL_C,RescueChapter.Phase.COMPLETE})
            helper.assertFalse(RescueEpilogue.requiresEpilogueStage(phase),
                "Dream recovery was incorrectly gated on the Overworld stage: "+phase);
        helper.assertTrue(RescueEncounters.restoreHouseEntry(true,false,true),
            "Reloaded HOUSE_SAFE checkpoint would leave its first door shut");
        helper.assertTrue(RescueEncounters.restoreHouseEntry(true,true,false),
            "Reloaded HOUSE_SAFE checkpoint would leave its second door shut");
        helper.assertTrue(RescueEncounters.restoreHouseEntry(false,true,true),
            "Initial HOUSE_SAFE preparation must open the real entry doors");
        helper.assertFalse(RescueEncounters.restoreHouseEntry(true,true,true),
            "Already-open original doors should not be rewritten every tick");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void savedEscapeCrawlRechecksEveryNewAttempt(GameTestHelper helper) {
        helper.assertTrue(RescuePassage.entryRecheckNeeded(false,false),
            "An old saved PassageVerified flag must not skip physical readback");
        helper.assertFalse(RescuePassage.entryRecheckNeeded(false,true),
            "A bounded physical readback must not restart every tick");
        helper.assertFalse(RescuePassage.entryRecheckNeeded(true,false),
            "An already entered and verified crawl must keep running");
        helper.assertFalse(RescuePassage.entryRecheckNeeded(true,true),
            "The already-entered state must not invalidate the live passage");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void finalDoorAndBangingCannotResumeInTheWebWorld(GameTestHelper helper) {
        helper.assertTrue(BeldamFinale.requiresOriginalExit(RescueChapter.Phase.LOCK_DOOR),
            "Saved exit must prove actual Overworld transfer before allowing key interaction");
        helper.assertTrue(BeldamFinale.requiresOriginalExit(RescueChapter.Phase.BANGING),
            "Banging reunion handoff must stay at the original physical door");
        for(var phase:new RescueChapter.Phase[]{RescueChapter.Phase.HAND_FALL,
                RescueChapter.Phase.TUNNEL_CHASE,RescueChapter.Phase.REUNION_A,
                RescueChapter.Phase.CAT_NOTICE,RescueChapter.Phase.FAREWELL_C})
            helper.assertFalse(BeldamFinale.requiresOriginalExit(phase),
                "A non-door chapter would be transferred away from its real scene: "+phase);
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void firstEscapeOriginalCastGatesEveryCheckpoint(GameTestHelper helper) {
        for(var state:FirstEscapeTimeline.Phase.values()){
            boolean mother=state.ordinal()>=FirstEscapeTimeline.Phase.COCOA_A.ordinal()
                    &&state.ordinal()<=FirstEscapeTimeline.Phase.COCOA_G.ordinal();
            boolean otherWybie=state.ordinal()>=FirstEscapeTimeline.Phase.ESCAPE_A.ordinal()
                    &&state.ordinal()<=FirstEscapeTimeline.Phase.ESCAPE_G.ordinal();
            boolean realWybie=state.ordinal()>=FirstEscapeTimeline.Phase.MISSING_B.ordinal()
                    &&state.ordinal()<=FirstEscapeTimeline.Phase.BICYCLE_DEPARTURE.ordinal();
            helper.assertTrue(FirstEscapeTimeline.needsOtherMother(state)==mother,
                "Other Mother became optional at "+state);
            helper.assertTrue(FirstEscapeTimeline.needsOtherWybie(state)==otherWybie,
                "Masked Wybie became optional at "+state);
            helper.assertTrue(FirstEscapeTimeline.needsRealWybie(state)==realWybie,
                "Original Wybie became optional at "+state);
        }
        helper.assertTrue(FirstEscapeTimeline.needsRealWybie(FirstEscapeTimeline.Phase.THROW_SHOE),
            "A missed original shoe should never time-advance without its canonical target");
        helper.assertFalse(FirstEscapeTimeline.needsRealWybie(FirstEscapeTimeline.Phase.ESCAPE_TUNNEL),
            "The original rescue tunnel cannot depend on an Overworld Wybie");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void catWatchdogRequiresOriginalActorThroughoutMirrorAndDolls(GameTestHelper helper) {
        for(var state:MissingParentsCat.Phase.values()){
            for(boolean started:new boolean[]{false,true})for(boolean present:new boolean[]{false,true}){
                boolean expected=!started||state.ordinal()<MissingParentsCat.Phase.NOTICE.ordinal()
                        ||state==MissingParentsCat.Phase.COMPLETE||present;
                helper.assertTrue(MissingParentsCat.catWatchdogReady(started,state,present)==expected,
                    "Original Cat omitted or needlessly required at "+state+" started="+started);
            }
        }
        helper.assertFalse(MissingParentsCat.catWatchdogReady(true,MissingParentsCat.Phase.E,false),
            "The Cat's doll retrieval cannot be awarded while the canonical Cat is unloaded");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void parentRescueWatchdogNeverFinishesMissingMother(GameTestHelper helper) {
        for(var state:ParentRescueStory.Phase.values()){
            for(boolean present:new boolean[]{false,true}){
                boolean expected=state.ordinal()<ParentRescueStory.Phase.B.ordinal()
                    ||state.ordinal()>=ParentRescueStory.Phase.PARENT_RESCUE_KITCHEN_MOTHER_DISAPPEARED.ordinal()
                    ||present;
                helper.assertTrue(ParentRescueStory.rescueMotherReadyForWatchdog(state,present)==expected,
                    "Saved Other Mother omitted from parent-rescue voice "+state);
            }
        }
        helper.assertFalse(ParentRescueStory.rescueMotherReadyForWatchdog(ParentRescueStory.Phase.N,false),
            "A missing original Other Mother must not auto-finish the kitchen and start three boss quests");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void webReadyCannotSkipItsPhysicalReceipts(GameTestHelper helper) {
        for(boolean built:new boolean[]{false,true})
            for(boolean verified:new boolean[]{false,true})
                for(int receipts:new int[]{0,1,999}){
                    boolean expected=built&&(!verified||receipts<=0);
                    helper.assertTrue(RescueWebSite.webVerificationRequired(built,verified,receipts)==expected,
                        "Unsafe web ready/verified combination: built="+built+" verified="+verified+" receipts="+receipts);
                }
        helper.assertTrue(RescueWebSite.webVerificationRequired(true,true,0),
            "A missing web journal must never allow entry even when its flag says verified");
        helper.assertTrue(RescueWebSite.webVerificationRequired(true,false,999),
            "A new web retry must recheck its physically placed blocks");
        helper.assertFalse(RescueWebSite.webVerificationRequired(true,true,999),
            "Verified intact web receipts should not be rebuilt every tick");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void littleDoorReentryUsesTheRealFinaleDimensionAndFreshReceipts(GameTestHelper helper) {
        for(var state:RescueChapter.Phase.values()) {
            boolean web=state==RescueChapter.Phase.FINAL_P||state==RescueChapter.Phase.FINAL_Q
                    ||state.ordinal()>=RescueChapter.Phase.WEB_STEALTH.ordinal()
                    &&state.ordinal()<=RescueChapter.Phase.HAND_FALL.ordinal();
            helper.assertTrue(StoryTunnel.webReturnPhase(state)==web,
                "Return through original Little Door chose wrong world at "+state);
        }
        helper.assertTrue(StoryTunnel.webReturnPhase(RescueChapter.Phase.DOOR_STRUGGLE),
                "Interrupted door struggle must not bypass the verified web");
        helper.assertFalse(StoryTunnel.webReturnPhase(RescueChapter.Phase.TUNNEL_CHASE),
                "Final 52-block crawl must use its own verification journal");
        helper.assertFalse(StoryTunnel.webReturnPhase(RescueChapter.Phase.FINAL_O),
                "The stage-floor collapse still starts in the original Other World");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void postBossDreamCannotRestoreDeathOrInvalidVitals(GameTestHelper helper) {
        helper.assertTrue(RescueVisionRules.restoredHealth(true,13,20,20)==13,
            "Valid saved health was changed on wake-up");
        helper.assertTrue(RescueVisionRules.restoredHealth(true,0,12,20)==12,
            "Corrupted zero health would kill the player during the farewell");
        helper.assertTrue(RescueVisionRules.restoredHealth(true,Float.NaN,8,20)==8,
            "NaN health would corrupt player data on dream return");
        helper.assertTrue(RescueVisionRules.restoredHealth(false,0,0,20)==1,
            "Missing saved health and invalid current health must not cause death");
        helper.assertTrue(RescueVisionRules.restoredHealth(true,300,4,20)==20,
            "Excess saved health must respect the player's maximum");
        helper.assertTrue(RescueVisionRules.restoredFood(true,9,20)==9,
            "Normal original hunger did not survive the dream");
        helper.assertTrue(RescueVisionRules.restoredFood(false,0,14)==14,
            "Missing saved hunger should preserve current survival state");
        helper.assertTrue(RescueVisionRules.restoredFood(true,Double.NaN,14)==14,
            "Non-finite saved hunger must not be read as zero");
        helper.assertTrue(RescueVisionRules.restoredFood(true,9.5,14)==14,
            "Fractional saved hunger must not be truncated into a fake value");
        helper.assertTrue(RescueVisionRules.restoredSaturation(true,3,8,9)==3,
            "Normal original saturation did not survive the dream");
        helper.assertTrue(RescueVisionRules.restoredSaturation(true,Float.NaN,3,9)==3,
            "Corrupt saved saturation must not reach player data");
        helper.assertTrue(RescueVisionRules.restoredSaturation(true,50,3,9)==9,
            "Saved saturation must not exceed food capacity");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void entireOriginalStoryRouteHasPlayableMeasuredVoiceRecordings(GameTestHelper helper) {
        var required=new java.util.LinkedHashSet<String>();
        for(var phase:FirstEscapeTimeline.Phase.values()) {
            String sound=FirstEscapeTimeline.sound(phase);
            if(!sound.isEmpty())required.add(sound);
        }
        for(var phase:MissingParentsTea.Phase.values()) {
            String sound=MissingParentsTea.sound(phase);
            if(!sound.isEmpty())required.add(sound);
        }
        for(var phase:MissingParentsCat.Phase.values()) {
            String sound=MissingParentsCat.voice(phase);
            if(!sound.isEmpty())required.add(sound);
        }
        for(char c='a';c<='n';c++)required.add("parent_rescue_"+c);
        for(var phase:RescueChapter.Phase.values()) {
            String sound=RescueChapter.sound(phase);
            if(!sound.isEmpty())required.add(sound);
        }
        helper.assertTrue(required.size()==88,
            "The story-route voice set changed without updating full-path acceptance: "+required.size());
        for(String sound:required)
            helper.assertTrue(ShowRecordings.duration(sound)>0,
                "Original route voice unavailable or invalid at runtime: "+sound);
        helper.succeed();
    }
}
