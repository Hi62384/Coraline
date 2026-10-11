package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import com.mojang.authlib.GameProfile;

/** Opt-in full-client frozen arena, voice gate, checkpoint, rival and winner/loser verification. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class IceBoatRaceVisualCheck {
    private static volatile int phase;private static int frames;
    private static VisitData.Scene scene;private static BlockPos center;
    private IceBoatRaceVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.iceBoatRaceVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(12);mc.options.pauseOnLostFocus=false;
        if(phase==2){mc.player.setYRot(0);mc.player.setXRot(86);}
        if(phase==4){mc.player.setYRot(-90);mc.player.setXRot(0);}
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.ICE_BOAT_RACE)))
            throw new IllegalStateException("Ice Boat Race Father voice missing during entry");
        if(phase==2&&++frames>200){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/ice_boat_race_arena_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            phase=3;mc.getSingleplayerServer().execute(()->verifyLaunch(mc.getSingleplayerServer()));
        }
        if(phase==4&&++frames>55){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/ice_boat_race_start_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            phase=5;mc.getSingleplayerServer().execute(()->verifyResults(mc.getSingleplayerServer()));
        }
        if(phase==6)mc.stop();
    }
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(43000,80,43000);
        DoorData.get(server).doors.put(door.id,door);
        var realm=RealmData.get(server).realm(door);realm.ready=true;realm.parentGeneration=1;realm.porch=realm.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(realm.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=realm.doorPos;scene.table=realm.doorPos;scene.loop.generation=1;scene.loop.portal=realm.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.ICE_BOAT_RACE;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,realm.doorPos.getX()+.5,realm.doorPos.getY(),realm.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Ice race portal failed");
        center=IceBoatRaceActivity.center(p,scene);var level=p.serverLevel();
        if(!level.getBlockState(center.below(5)).is(Blocks.LIME_GLAZED_TERRACOTTA))throw new IllegalStateException("Frozen festival was not built");
        if(level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center.south(46)).inflate(6),
            e->e.getPersistentData().getBoolean("CoralineIceFather")).size()!=1)throw new IllegalStateException("Ice race Father missing");
        int watchers=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(center).inflate(55,10,55),
            e->e.getPersistentData().hasUUID("CoralineIceRaceOwner")).size();
        if(watchers<14)throw new IllegalStateException("Frozen spectator stands missing: "+watchers);
        if(scene.loop.iceStage!=0||ActivityWorld.exitReady(scene)||!level.getEntitiesOfClass(Boat.class,new AABB(center).inflate(55)).isEmpty())
            throw new IllegalStateException("Race started before Father finished speaking");
        p.getAbilities().flying=true;p.onUpdateAbilities();
        p.teleportTo(level,center.getX()+.5,center.getY()+90,center.getZ()+.5,0,86);
        phase=2;
    }
    private static void verifyLaunch(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var l=scene.loop;var level=p.serverLevel();
        ActivityWorld.tick(p,scene);if(l.iceStage!=0)throw new IllegalStateException("Father voice gate was bypassed");
        l.iceVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.iceStage!=1||!(p.getVehicle() instanceof Boat playerBoat)||playerBoat.isNoGravity())
            throw new IllegalStateException("Player was not placed in a normally drivable boat after voice");
        var boats=level.getEntitiesOfClass(Boat.class,new AABB(center).inflate(55,10,55));
        if(boats.size()!=7||boats.stream().filter(b->!b.getPassengers().isEmpty()).count()!=7)
            throw new IllegalStateException("Race grid needs seven occupied boats, found "+boats.size());
        l.iceCountdownUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.iceStage!=2||l.iceLap!=0||l.iceCheckpoint!=0)throw new IllegalStateException("Countdown did not begin the race");
        var helper=net.minecraftforge.common.util.FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"IceRaceHelper"));
        IceBoatRaceActivity.tick(helper,scene,center);
        if(l.iceCheckpoint!=0||l.iceLap!=0||l.iceRank!=0)throw new IllegalStateException("A multiplayer helper changed the owner's race state");
        frames=0;phase=4;
    }
    private static void gates(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var l=scene.loop;Boat boat=(Boat)p.getVehicle();
        if(boat==null)throw new IllegalStateException("Player left race boat");
        // The finish cannot be reached until all fourteen ordered gates have been visited.
        var finish=IceBoatRaceActivity.checkpointCenter(center,14);boat.setPos(finish.x,finish.y,finish.z);
        ActivityWorld.tick(p,scene);if(l.iceCheckpoint!=0||l.iceLap!=0)throw new IllegalStateException("Finish line accepted a skipped course");
        for(int lap=0;lap<3;lap++)for(int gate=1;gate<=14;gate++){
            var at=IceBoatRaceActivity.checkpointCenter(center,gate);boat.setPos(at.x,at.y,at.z);
            ActivityWorld.tick(p,scene);
            if(lap<2||gate<14){int expected=gate==14?0:gate;
                if(l.iceCheckpoint!=expected)throw new IllegalStateException("Checkpoint "+gate+" was not counted, got "+l.iceCheckpoint);
            }
        }
    }
    private static void exitThroughPortal(net.minecraft.server.level.ServerPlayer p){
        var level=p.serverLevel();var exits=level.getEntitiesOfClass(ScenePropEntity.class,new AABB(center.south(51)).inflate(3),
            e->e.kind()==ScenePropEntity.ACTIVITY_EXIT&&p.getUUID().equals(e.owner));
        if(exits.size()!=1)throw new IllegalStateException("Race exit portal was not physically created");
        var exit=exits.get(0);p.teleportTo(level,exit.getX(),exit.getY(),exit.getZ(),0,0);
        LoopProps.interact(p,exit);
    }
    private static void verifyResults(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var l=scene.loop;var level=p.serverLevel();
        gates(server);
        if(l.iceStage!=3||l.iceRank!=1||!l.iceRewarded||p.getInventory().countItem(Items.DIAMOND)<6)
            throw new IllegalStateException("First-place reward/result failed");
        if(ActivityWorld.exitReady(scene))throw new IllegalStateException("Exit appeared before result announcement pause");
        l.iceResultAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(!ActivityWorld.exitReady(scene))throw new IllegalStateException("Winner exit did not open");
        exitThroughPortal(p);
        if(!OtherWorld.isOther(p.level()))throw new IllegalStateException("Winner could not leave race");
        int diamonds=p.getInventory().countItem(Items.DIAMOND);
        OtherMotherLoop.phase(p,scene,OtherMotherLoopState.Phase.READY);scene.loop.activity=OtherMotherActivities.Activity.ICE_BOAT_RACE;
        ActivityWorld.enter(p,scene);if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Second race could not start");
        l.iceVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        l.iceCountdownUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        for(int i=0;i<2300&&Arrays.stream(l.iceRivalFinish).anyMatch(n->n==0);i++)
            IceBoatRaceActivity.moveRivals(p,scene,center,OtherMotherLoop.now(p)+i);
        if(Arrays.stream(l.iceRivalFinish).anyMatch(n->n==0))throw new IllegalStateException("A rival failed to complete the winding course: "+Arrays.toString(l.iceRivalFinish));
        gates(server);
        if(l.iceRank!=7||l.iceRewarded||p.getInventory().countItem(Items.DIAMOND)!=diamonds)
            throw new IllegalStateException("Losing improperly granted winner loot");
        l.iceResultAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(!ActivityWorld.exitReady(scene))throw new IllegalStateException("Loser exit did not open");
        exitThroughPortal(p);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE)
            throw new IllegalStateException("Loser did not return to the Other World");
        System.out.println("CORALINE_ICE_BOAT_RACE_CHECK passed voice gate, grid, ordered checkpoints, six completed rivals, win/lose payout and exits");
        phase=6;
    }
}
