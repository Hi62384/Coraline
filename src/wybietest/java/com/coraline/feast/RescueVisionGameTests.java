package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueVisionGameTests {
    private static ServerPlayer player(GameTestHelper helper){var level=helper.getLevel();var player=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"VisionQA"));player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(player.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player);var scene=new VisitData.Scene();scene.owner=player.getUUID();scene.door=java.util.UUID.randomUUID();scene.loop.event.putInt("Serial",7);VisitData.get(player.server).scenes.put(scene.owner,scene);RescueChapter.data(scene).putBoolean("Started",true);return player;}
    private static Object invoke(String name,ServerPlayer player,VisitData.Scene scene){try{var method=RescueEpilogue.class.getDeclaredMethod(name,ServerPlayer.class,VisitData.Scene.class);method.setAccessible(true);return method.invoke(null,player,scene);}catch(ReflectiveOperationException failure){throw new IllegalStateException("Production vision operation failed: "+name,failure);}}
    @GameTest(template="empty") public static void savedVisionRestoresExactPoseVitalsAndInventory(GameTestHelper helper){
        var player=player(helper);var scene=OtherMotherLoop.scene(player);var at=helper.absolutePos(new BlockPos(2,3,2));var level=helper.getLevel();
        level.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(at,Blocks.RED_CARPET.defaultBlockState(),2);level.setBlock(at.above(),Blocks.AIR.defaultBlockState(),2);
        try{player.setPos(at.getX()+.31,at.getY()+.0625,at.getZ()+.67);player.setYRot(143);player.setXRot(-22);player.setForcedPose(Pose.CROUCHING);player.setPose(Pose.CROUCHING);player.setHealth(13);player.getFoodData().setFoodLevel(9);player.getFoodData().setSaturation(3);player.getInventory().setItem(0,new ItemStack(Items.EMERALD,7));var before=player.position();var mode=player.gameMode.getGameModeForPlayer();
            invoke("saveVision",player,scene);helper.assertTrue(player.isInvulnerable()&&player.isNoGravity(),"Protection did not begin before the eyelids");
            var saved=new VisitData.Scene();saved.owner=scene.owner;saved.door=scene.door;saved.loop=OtherMotherLoopState.load(scene.loop.save());player.setPos(before.add(12,4,0));player.setForcedPose(null);player.setPose(Pose.STANDING);player.setHealth(20);player.getFoodData().setFoodLevel(20);
            helper.assertTrue((Boolean)invoke("restoreVision",player,saved),"Valid saved anchor did not restore");helper.assertTrue(player.position().distanceToSqr(before)<1e-12,"Exact sub-block return drifted to a block center");
            helper.assertTrue(player.getYRot()==143&&player.getXRot()==-22&&player.getPose()==Pose.CROUCHING&&player.getForcedPose()==Pose.CROUCHING,"Facing or forced pose was not restored");
            helper.assertTrue(!player.isInvulnerable()&&!player.isNoGravity()&&player.getHealth()==13&&player.getFoodData().getFoodLevel()==9&&player.getFoodData().getSaturationLevel()==3,"Cinematic flags/vitals leaked into ordinary play");helper.assertTrue(player.gameMode.getGameModeForPlayer()==mode&&player.getInventory().getItem(0).getCount()==7,"Vision changed game mode or unrelated inventory");
        }finally{VisitData.get(player.server).scenes.remove(scene.owner);level.setBlock(at.below(),Blocks.AIR.defaultBlockState(),2);}helper.succeed();
    }
    @GameTest(template="empty") public static void recoveryCannotApplyAnotherOwnersSavedVision(GameTestHelper helper){var first=player(helper);var second=player(helper);var scene=OtherMotherLoop.scene(first);try{var saved=new CompoundTag();saved.putString("Dimension",first.level().dimension().location().toString());saved.putDouble("X",Double.NaN);RescueChapter.data(scene).put("VisionReturn",saved);var before=second.position();RescueEpilogue.recoverVision(second,scene);helper.assertTrue(!second.isInvulnerable()&&!second.isNoGravity()&&second.position().equals(before),"Another owner's vision captured this player");helper.assertFalse((Boolean)invoke("restoreVision",first,scene),"Non-finite saved return anchor was accepted");}finally{VisitData.get(first.server).scenes.remove(first.getUUID());VisitData.get(second.server).scenes.remove(second.getUUID());}helper.succeed();}
    @GameTest(template="empty") public static void failedCheckpointTransferKeepsDimensionPositionAndForcedPose(GameTestHelper helper){var player=player(helper);var scene=OtherMotherLoop.scene(player);try{player.setForcedPose(Pose.SWIMMING);player.setPose(Pose.SWIMMING);var original=player.position();var level=player.serverLevel();helper.assertFalse(RescueChapter.transfer(player,player.server.getLevel(ActivityWorld.KEY),new BlockPos(12000,80,12000)),"A void checkpoint was accepted as safe footing");helper.assertTrue(player.serverLevel()==level&&player.position().equals(original)&&player.getForcedPose()==Pose.SWIMMING,"Failed transfer changed the player before validating its destination");helper.assertTrue(RescueChapter.data(scene).contains("Blocker"),"Failed transfer left no actionable reason");}finally{VisitData.get(player.server).scenes.remove(scene.owner);}helper.succeed();}
    @GameTest(template="empty") public static void finalCatDeparturePreservesItsUuidButDoesNotReturnItBesideThePlayer(GameTestHelper helper){
        var player=player(helper);var scene=OtherMotherLoop.scene(player);var here=helper.absolutePos(new BlockPos(2,3,2));var real=helper.getLevel();var backstage=player.server.getLevel(ActivityWorld.KEY);var site=TheatreSite.center(player,scene).south(10);backstage.getChunkAt(site);backstage.setBlock(site.below(),Blocks.STONE.defaultBlockState(),2);ShowSiteData.get(player.server).owner(scene.owner).putBoolean("TheatreBuilt",true);real.setBlock(here.below(),Blocks.STONE.defaultBlockState(),2);real.setBlock(here,Blocks.AIR.defaultBlockState(),2);real.setBlock(here.above(),Blocks.AIR.defaultBlockState(),2);player.setPos(here.getX()+200,here.getY(),here.getZ());
        var cat=ShowActors.acquire(player,scene,"cat",real,net.minecraft.world.phys.Vec3.atBottomCenterOf(here));helper.assertTrue(cat!=null,"No canonical Cat fixture");var id=cat.getUUID();
        var finished=RescueChapter.data(scene);finished.putString("Phase","COMPLETE");finished.putBoolean("FarewellComplete",true);finished.putBoolean("VisionRestored",true);finished.putBoolean("FarewellCatCleanupPending",true);
        try{helper.assertTrue(ShowActors.departHiddenCat(player,scene)&&ShowActors.departHiddenCat(player,scene),"Final offscreen departure is not idempotent");var record=scene.loop.story.getCompound("CanonicalStoryCat");helper.assertTrue(record.getUUID("Entity").equals(id)&&record.getBoolean("FarewellDeparted"),"Final departure forgot or replaced the canonical Cat");helper.assertTrue(!record.getString("Dimension").isEmpty(),"Retirement discarded the original Cat's saved dimension history");helper.assertTrue(real.getEntity(id)==null&&ShowActors.actor(player,scene,"cat")==null,"Final Cat remained at the real door or retained its cinematic lease");
        }finally{for(var world:player.server.getAllLevels()){var actor=world.getEntity(id);if(actor!=null)actor.discard();}VisitData.get(player.server).scenes.remove(scene.owner);backstage.setBlock(site.below(),Blocks.AIR.defaultBlockState(),2);}helper.succeed();
    }
    @GameTest(template="empty") public static void normalFarewellCanDepartWithoutTheatre(GameTestHelper helper){
        var player=player(helper);var scene=OtherMotherLoop.scene(player);var level=helper.getLevel();var doorAt=helper.absolutePos(new BlockPos(2,3,2));var target=doorAt.offset(24,0,24);
        var door=new com.coraline.otherworld.DoorData.Door();door.id=scene.door;door.owner=scene.owner;door.pos=doorAt;door.dimension=level.dimension().location().toString();com.coraline.otherworld.DoorData.get(player.server).doors.put(scene.door,door);
        level.getChunkAt(target);level.setBlock(target.below(),Blocks.STONE.defaultBlockState(),2);level.setBlock(target,Blocks.AIR.defaultBlockState(),2);level.setBlock(target.above(),Blocks.AIR.defaultBlockState(),2);
        player.setPos(doorAt.getX()-100,doorAt.getY(),doorAt.getZ()-100);var cat=ShowActors.acquire(player,scene,"cat",level,net.minecraft.world.phys.Vec3.atBottomCenterOf(doorAt));helper.assertTrue(cat!=null,"No canonical Cat fixture");var id=cat.getUUID();
        try{var n=RescueChapter.data(scene);helper.assertFalse(ShowActors.departHiddenCat(player,scene),"Unfinished farewell retired the Cat");n.putString("Phase","COMPLETE");n.putBoolean("FarewellComplete",true);n.putBoolean("VisionRestored",true);n.putBoolean("FarewellCatCleanupPending",true);
            helper.assertTrue(ShowActors.departHiddenCat(player,scene)&&ShowActors.departHiddenCat(player,scene),"Normal farewell without theatre did not depart idempotently");
            var record=scene.loop.story.getCompound("CanonicalStoryCat");helper.assertTrue(record.getUUID("Entity").equals(id)&&record.getBoolean("FarewellDeparted")&&ShowActors.actor(player,scene,"cat")==null,"Departure lost the canonical identity or lease");
        }finally{for(var world:player.server.getAllLevels()){var actor=world.getEntity(id);if(actor!=null)actor.discard();}com.coraline.otherworld.DoorData.get(player.server).doors.remove(scene.door);VisitData.get(player.server).scenes.remove(scene.owner);level.setBlock(target.below(),Blocks.AIR.defaultBlockState(),2);}helper.succeed();
    }
    @GameTest(template="empty") public static void loginAfterAnUncleanRestartRequiresRecoveryAndRotatesOldInputs(GameTestHelper helper){var player=player(helper);var scene=OtherMotherLoop.scene(player);try{var n=RescueChapter.data(scene);n.putString("Phase","FINAL_N");var old=java.util.UUID.randomUUID();n.putUUID("Input",old);n.putBoolean("Audio",true);n.remove("Recover");var playback=ShowPlayback.data(scene);playback.putString("Sound","finale_n");playback.putBoolean("Started",true);RescueChapter.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));helper.assertTrue(n.getBoolean("Recover")&&!n.getUUID("Input").equals(old),"Crash/reconnect reused a stale continuation input session");helper.assertTrue(playback.getString("Sound").isEmpty()&&n.getString("Phase").equals("FINAL_N"),"Reconnect skipped the current recording or retained a dead client audio lease");helper.assertFalse(n.getBoolean("BeldamBlind")||n.getBoolean("KeyRecovered"),"Reconnect fabricated finale progress");}finally{VisitData.get(player.server).scenes.remove(scene.owner);}helper.succeed();}
    @GameTest(template="empty") public static void missingDoorAndGhostsCannotHoldTheFarewellBlackout(GameTestHelper helper){
        var p=player(helper);var s=OtherMotherLoop.scene(p);var at=helper.absolutePos(new BlockPos(2,3,2));helper.getLevel().setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(at));
        try{invoke("saveVision",p,s);RescueChapter.data(s).putString("Phase","EYES_CLOSE");
            helper.assertTrue(RescueEpilogue.protectedVision(s),"Blackout did not lock player actions");
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            var breaking=new net.minecraftforge.event.level.BlockEvent.BreakEvent(helper.getLevel(),at.below(),helper.getLevel().getBlockState(at.below()),p);
            RescueVisionProtection.breakBlock(breaking);helper.assertTrue(breaking.isCanceled(),"Creative block breaking escaped the vision lock");

            RescueEpilogue.tick(p,s,RescueChapter.Phase.EYES_CLOSE,201,false);
            helper.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.CELESTIAL_OPEN,"Missing source door or ghosts stalled closed eyes");
            helper.assertTrue(p.serverLevel()==p.server.getLevel(ActivityWorld.KEY),"Vision remained in the sleeping environment");
            RescueEpilogue.tick(p,s,RescueChapter.Phase.CELESTIAL_OPEN,41,false);
            RescueEpilogue.tick(p,s,RescueChapter.Phase.ORBS,101,false);
            helper.assertTrue(RescueChapter.phase(s)==RescueChapter.Phase.ORBS&&!RescueChapter.data(s).getBoolean("DreamDialogueComplete"),"Missing original ghosts must hold the dream safely instead of fabricating their farewell");
            helper.assertTrue((Boolean)invoke("restoreVision",p,s),"Blackout recovery lost original return anchor");
        }finally{VisitData.get(p.server).scenes.remove(s.owner);}helper.succeed();
    }
    private RescueVisionGameTests(){}
}
