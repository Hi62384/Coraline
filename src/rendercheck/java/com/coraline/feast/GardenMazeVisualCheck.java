package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;
import java.util.ArrayDeque;

/** Opt-in actual-client maze geometry, voice, timer, prize and exit check. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class GardenMazeVisualCheck {
    private static volatile int phase;private static int frames;
    private static VisitData.Scene scene;private static BlockPos center;
    private GardenMazeVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.gardenMazeVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(12);mc.options.pauseOnLostFocus=false;
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.GARDEN_MAZE)))
            throw new IllegalStateException("Garden Father voice stopped after entry");
        if(phase==2&&++frames>130){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/garden_maze_overhaul_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            phase=3;mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));
        }
        if(phase==4&&++frames>50){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/garden_maze_finish_overhaul_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            phase=5;mc.getSingleplayerServer().execute(()->verifySuccess(mc.getSingleplayerServer()));
        }
        if(phase==6)mc.stop();
    }
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(37000,80,37000);
        DoorData.get(server).doors.put(door.id,door);
        var realm=RealmData.get(server).realm(door);realm.ready=true;realm.parentGeneration=1;realm.porch=realm.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(realm.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=realm.doorPos;scene.table=realm.doorPos;scene.loop.generation=1;scene.loop.portal=realm.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.GARDEN_MAZE;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,realm.doorPos.getX()+.5,realm.doorPos.getY(),realm.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Maze portal failed");
        center=GardenMazeActivity.center(p,scene);var level=p.serverLevel();
        if(!level.getBlockState(center.below(2)).is(Blocks.GREEN_GLAZED_TERRACOTTA))throw new IllegalStateException("Maze was not built");
        if(level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center.south(34)).inflate(8),f->f.getPersistentData().getBoolean("CoralineGardenFather")).size()!=1)
            throw new IllegalStateException("Garden Father missing");
        for(int slot=0;slot<4;slot++)if(prizeDisplay(level,slot)==null)
            throw new IllegalStateException("Garden prize display "+slot+" missing");
        if(ActivityWorld.exitReady(scene))throw new IllegalStateException("Maze exit appeared early");
        verifyRoute(p.serverLevel());phase=2;
    }
    private static void verifyRoute(net.minecraft.server.level.ServerLevel level){
        boolean[][] seen=new boolean[59][59];var q=new ArrayDeque<int[]>();q.add(new int[]{29,57,0});seen[57][29]=true;int count=0,goal=-1,branches=0;
        while(!q.isEmpty()){var v=q.remove();count++;if(v[0]==29&&v[1]==1)goal=v[2];
            int exits=0;for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=v[0]+step[0],z=v[1]+step[1];
                if(x>=0&&z>=0&&x<59&&z<59&&level.isEmptyBlock(center.offset(x-29,0,z-29)))exits++;}
            if(exits>=3)branches++;
            for(int[] step:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int x=v[0]+step[0],z=v[1]+step[1];
                if(x<0||z<0||x>=59||z>=59||seen[z][x])continue;
                if(!level.isEmptyBlock(center.offset(x-29,0,z-29)))continue;seen[z][x]=true;q.add(new int[]{x,z,v[2]+1});}
        }
        if(goal<0||count<600||branches<80)throw new IllegalStateException("Hedge maze lacks connected choices: reached "+count+" cells, "+branches+" branches");
        System.out.println("CORALINE_GARDEN_MAZE_ROUTE shortest path "+goal+" blocks, "+count+" reachable cells, "+branches+" branches");
    }
    private static void verify(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var state=scene.loop;var level=p.serverLevel();
        ActivityWorld.tick(p,scene);if(state.gardenEndAt!=0)throw new IllegalStateException("Maze timer began during Father speech");
        state.gardenVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(state.gardenEndAt-OtherMotherLoop.now(p)!=2400)throw new IllegalStateException("Maze two-minute timer did not start");
        LoopProps.interact(p,prizeDisplay(level,0));
        if(state.gardenClaimMask!=0)throw new IllegalStateException("Claimed prize before reaching end");
        state.gardenEndAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(!state.gardenFinished||state.gardenEligible||!ActivityWorld.exitReady(scene)||Math.abs(p.getZ()-(center.getZ()-41.5))>1)
            throw new IllegalStateException("Timed-out player was not shown the missed prizes");
        p.teleportTo(level,center.getX()-8.5,center.getY(),center.getZ()-41.5,180,0);
        LoopProps.interact(p,prizeDisplay(level,0));
        if(state.gardenClaimMask!=0)throw new IllegalStateException("Claimed prize after missing deadline");
        p.getAbilities().flying=true;p.onUpdateAbilities();
        p.teleportTo(level,center.getX()+.5,center.getY()+5,center.getZ()-35.5,180,20);
        frames=0;phase=4;
    }
    private static void verifySuccess(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var state=scene.loop;var level=p.serverLevel();
        state.gardenFinished=false;state.gardenEligible=false;state.gardenClaimMask=0;state.gardenEndAt=OtherMotherLoop.now(p)+100;
        p.teleportTo(level,center.getX()+.5,center.getY(),center.getZ()-30.5,180,0);ActivityWorld.tick(p,scene);
        if(!state.gardenFinished||!state.gardenEligible)throw new IllegalStateException("Successful maze finish not detected");
        for(int slot=0;slot<4;slot++){int x=new int[]{-9,-3,3,9}[slot];p.teleportTo(level,center.getX()+x+.5,center.getY(),center.getZ()-41.5,180,0);
            LoopProps.interact(p,prizeDisplay(level,slot));}
        if(state.gardenClaimMask!=15)throw new IllegalStateException("Winning prizes not all claimable");
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE||!level.isEmptyBlock(center.below(2)))
            throw new IllegalStateException("Garden exit failed to return and clean up");
        System.out.println("CORALINE_GARDEN_MAZE_CHECK passed route, Father gate, 2-minute timer, missed and earned prizes, return");phase=6;
    }
    private static ScenePropEntity prizeDisplay(net.minecraft.server.level.ServerLevel level,int slot){
        int x=new int[]{-9,-3,3,9}[slot];var at=center.offset(x,0,-42);
        return level.getEntitiesOfClass(ScenePropEntity.class,new AABB(at).inflate(1),e->e.kind()==ScenePropEntity.PRIZE_PLATE&&e.slot==slot).stream().findFirst().orElse(null);
    }
}
