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

/** Opt-in client render and parkour activity lifecycle check. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class DreamParkourVisualCheck {
    private static volatile int phase;
    private static int frames;
    private static VisitData.Scene scene;
    private static BlockPos center;
    private DreamParkourVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.dreamParkourVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(12);mc.options.pauseOnLostFocus=false;
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.DREAM_PARKOUR)))
            throw new IllegalStateException("Other Father dream parkour voice stopped after entry");
        if(phase==2&&++frames>140){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/dream_parkour_overhaul_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            System.out.println("CORALINE_DREAM_CHECK screenshot saved");phase=3;
            mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));
        }
        if(phase==5)mc.stop();
    }
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(32000,80,32000);
        DoorData.get(server).doors.put(door.id,door);
        var r=RealmData.get(server).realm(door);r.ready=true;r.parentGeneration=1;r.porch=r.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(r.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=r.doorPos;scene.table=r.doorPos;scene.loop.generation=1;scene.loop.portal=r.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.DREAM_PARKOUR;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        var oldCenter=DreamParkourActivity.center(p,scene);var activity=server.getLevel(ActivityWorld.KEY);
        activity.setBlock(oldCenter.below(),Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.defaultBlockState(),2);
        activity.setBlock(oldCenter.offset(17,77,0),Blocks.COBBLESTONE.defaultBlockState(),2);
        p.teleportTo(other,r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);
        if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Dream portal did not transport the player");
        center=DreamParkourActivity.center(p,scene);var level=p.serverLevel();
        if(!level.getBlockState(center.below()).is(Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA)||
           !level.isEmptyBlock(DreamParkourActivity.stage(center,0))||
           !level.isEmptyBlock(DreamParkourActivity.stage(center,191))||
           DreamParkourActivity.stage(center,191).getY()-center.getY()<160||
           !level.isEmptyBlock(center.offset(17,77,0)))
            throw new IllegalStateException("Dream course did not remain locked until Father's speech ended");
        if(level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center.south(16)).inflate(8),f->f.getPersistentData().getBoolean("CoralineParkourFather")).size()!=1)
            throw new IllegalStateException("Other Father did not arrive at dream parkour");
        if(DreamParkourActivity.exitReady(scene))throw new IllegalStateException("Return portal appeared early");
        p.teleportTo(level,center.getX()+.5,center.getY()+10,center.getZ()+28.5,180,-18);
        System.out.println("CORALINE_DREAM_CHECK course and Father spawned");phase=2;
    }
    private static void verify(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();var l=scene.loop;
        if(l.parkourEndAt!=0)throw new IllegalStateException("Three-minute timer began before Father's voice completed");
        l.parkourVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.parkourEndAt-OtherMotherLoop.now(p)!=DreamParkourActivity.DURATION)
            throw new IllegalStateException("Dream timer did not start at 180 seconds");
        if(level.isEmptyBlock(DreamParkourActivity.stage(center,0))||level.isEmptyBlock(DreamParkourActivity.stage(center,191)))
            throw new IllegalStateException("Dream pads did not appear when Father finished speaking");
        var upperSteps=new java.util.HashSet<String>();
        for(long seed=0;seed<128;seed++)DreamParkourActivity.validateCourse(seed*0x9E3779B97F4A7C15L);
        for(int i=1;i<DreamParkourActivity.STAGES;i++){
            var a=DreamParkourActivity.stage(center,i-1);var b=DreamParkourActivity.stage(center,i);
            double horizontal=Math.hypot(b.getX()-a.getX(),b.getZ()-a.getZ());
            if(b.getY()-a.getY()>1||horizontal<2||horizontal>Math.sqrt(10))
                throw new IllegalStateException("Unreachable dream jump at stage "+i);
            if(i>=96)upperSteps.add((b.getX()-a.getX())+","+(b.getZ()-a.getZ()));
        }
        if(upperSteps.size()<12)throw new IllegalStateException("Upper dream jumps repeat too much: "+upperSteps.size()+" directions");
        p.teleportTo(level,center.getX()+.5,center.getY()-6,center.getZ()+.5,180,0);
        DreamParkourActivity.tick(p,scene,center);
        if(p.getY()>center.getY()-5)throw new IllegalStateException("Falling still teleports the player to a checkpoint");
        p.teleportTo(level,center.getX()+.5,center.getY(),center.getZ()+16.5,180,0);
        l.parkourEndAt=OtherMotherLoop.now(p);
        ActivityWorld.tick(p,scene);
        if(!l.parkourRewarded||!DreamParkourActivity.exitReady(scene)||
            LoopProps.nearby(level,scene,center.south(20)).stream().noneMatch(e->e.kind()==ScenePropEntity.ACTIVITY_EXIT))
            throw new IllegalStateException("Timer expiry did not award prize and open exit");
        if(p.getInventory().items.stream().noneMatch(stack->stack.is(net.minecraft.world.item.Items.COOKIE)))
            throw new IllegalStateException("Landing at launch height did not give the current-height reward");
        DreamParkourActivity.start(p,scene,level);l.parkourVoiceUntil=OtherMotherLoop.now(p);
        DreamParkourActivity.tick(p,scene,center);
        var top=DreamParkourActivity.stage(center,DreamParkourActivity.STAGES-1);
        p.teleportTo(level,top.getX()+.5,top.getY()+1,top.getZ()+.5,180,0);p.setOnGround(true);
        DreamParkourActivity.tick(p,scene,center);
        if(!l.parkourRewarded||l.parkourEndAt<=OtherMotherLoop.now(p)||
            p.getInventory().items.stream().noneMatch(stack->stack.is(net.minecraft.world.item.Items.NETHERITE_INGOT)))
            throw new IllegalStateException("Reaching the summit did not end the timer immediately with top-tier loot");
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE||!level.isEmptyBlock(center.below()))
            throw new IllegalStateException("Dream course did not clean up and return to the Other World");
        System.out.println("CORALINE_DREAM_CHECK passed voice gate, timer, current-height reward, summit, exit, return");phase=5;
    }
}
