package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import com.coraline.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Opt-in real-client arena and complete cake-contest state-machine check. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class CakeContestVisualCheck {
    private static volatile int phase;
    private static int frames;
    private static VisitData.Scene scene;
    private static BlockPos center;
    private CakeContestVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.cakeVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(10);mc.options.pauseOnLostFocus=false;
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.CAKE_MAKING_CONTEST)))
            throw new IllegalStateException("Other Father cake voice stopped after entry");
        if(phase==2&&++frames>130){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/cake_contest_overhaul_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            System.out.println("CORALINE_CAKE_CHECK screenshot saved");phase=3;
            mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));
        }
        if(phase==4&&++frames>35){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/cake_finished_overhaul_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            phase=41;mc.getSingleplayerServer().execute(()->verifyCompletedCake(mc.getSingleplayerServer()));
        }
        if(phase==5)mc.stop();
    }
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(27000,80,27000);
        DoorData.get(server).doors.put(door.id,door);
        var r=RealmData.get(server).realm(door);r.ready=true;r.parentGeneration=1;r.porch=r.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(r.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=r.doorPos;scene.table=r.doorPos;scene.loop.generation=1;scene.loop.portal=r.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.CAKE_MAKING_CONTEST;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);
        if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Cake portal did not transport the player");
        center=CakeContestActivity.center(p,scene);var level=p.serverLevel();level.setWeatherParameters(6000,0,false,false);
        if(!level.getBlockState(center.below(2)).is(Blocks.PINK_GLAZED_TERRACOTTA)||
           !level.getBlockState(center.offset(0,0,-22)).is(Blocks.SMOOTH_QUARTZ))
            throw new IllegalStateException("Cake festival or judging table was not built");
        int npc=level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(center).inflate(39,8,39),CakeContestActivity::isContestNpc).size();
        int audience=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(center).inflate(39,8,39),v->v.getPersistentData().getBoolean("CoralineCakeNPC")).size();
        if(npc<7||audience<36)throw new IllegalStateException("Expected human chefs/judges and 36 button-eyed villagers, found "+npc+" / "+audience);
        if(level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center.south(25)).inflate(8),f->f.getPersistentData().getBoolean("CoralineCakeFather")).size()!=1)
            throw new IllegalStateException("Other Father did not arrive at the cake contest");
        if(CakeContestActivity.exitReady(scene))throw new IllegalStateException("Return portal appeared early");
        System.out.println("CORALINE_CAKE_CHECK arena, contestants, audience and Father spawned");phase=2;
    }
    private static void verify(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();var l=scene.loop;
        l.cakeVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.cakeStage!=1)throw new IllegalStateException("Father's speech did not unlock the stand");
        p.teleportTo(level,center.getX()+.5,center.getY(),center.getZ()+3.5,180,0);ActivityWorld.tick(p,scene);
        if(l.cakeStage!=2)throw new IllegalStateException("Walking to the stand did not start countdown");
        l.cakeCountdownUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.cakeStage!=3||l.cakeBakeUntil-OtherMotherLoop.now(p)!=2400)throw new IllegalStateException("Two-minute bake did not start");
        for(int z:new int[]{-2,0,2}){l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center.offset(-2,0,z));}
        l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center.offset(2,0,-2));
        l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center);
        if(l.cakeStep!=1)throw new IllegalStateException("Physical pantry and mixer failed");
        l.cakeMixStartedAt=OtherMotherLoop.now(p)-26;
        l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center);
        if(l.cakeStep!=2)throw new IllegalStateException("Mixing did not complete");
        l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center.south(2));
        l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center.north(2));
        l.cakeOvenStartedAt=OtherMotherLoop.now(p)-155;
        l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center.north(2));
        for(int z:new int[]{-1,0,1,2,3}){l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center.offset(2,0,z));}
        l.cakeLastActionAt=OtherMotherLoop.now(p)-10;CakeContestActivity.interactBlock(p,center.south(2));
        if(l.cakeStage!=4||LoopProps.nearby(level,scene,center).stream().noneMatch(e->e.kind()==ScenePropEntity.CONTEST_CAKE))throw new IllegalStateException("Custom decorated cake is missing");
        p.getAbilities().flying=true;p.onUpdateAbilities();
        p.teleportTo(level,center.getX()+1.5,center.getY()+2.7,center.getZ()+2.5,155,25);
        frames=0;phase=4;
    }
    private static void verifyCompletedCake(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();var l=scene.loop;
        CakeContestActivity.interactBlock(p,center);
        if(!l.cakeTaken||p.getInventory().items.stream().noneMatch(stack->stack.is(ModItems.CONTEST_CAKE.get())))throw new IllegalStateException("Cake was not collected");
        CakeContestActivity.interactBlock(p,center.north(22));
        if(l.cakeStage!=5||!l.cakeSubmitted||p.getInventory().items.stream().anyMatch(stack->stack.is(ModItems.CONTEST_CAKE.get())))
            throw new IllegalStateException("Judging panel rejected the cake");
        var chair=LoopProps.nearby(level,scene,center.north(14)).stream().filter(e->e.kind()==ScenePropEntity.CHAIR&&e.slot==42).findFirst().orElseThrow();
        p.teleportTo(level,chair.getX(),chair.getY(),chair.getZ()+1,180,0);
        if(LoopProps.interact(p,chair)!=net.minecraft.world.InteractionResult.CONSUME||l.cakeStage!=6)
            throw new IllegalStateException("Judging chair interaction did not begin ten-second deliberation");
        l.cakeJudgeUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.cakeStage!=7)throw new IllegalStateException("Judges did not rank contestants");
        for(int i=0;i<5;i++){l.cakeAnnounceAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);}
        if(l.cakeStage!=8||l.cakeRank<1||l.cakeRank>5||!CakeContestActivity.exitReady(scene)||
            LoopProps.nearby(level,scene,center.south(30)).stream().noneMatch(e->e.kind()==ScenePropEntity.ACTIVITY_EXIT))
            throw new IllegalStateException("Ranking, prize or return portal did not complete");
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE||!level.isEmptyBlock(center.below()))
            throw new IllegalStateException("Cake contest did not clean up and return home");
        System.out.println("CORALINE_CAKE_CHECK passed physical ingredients, mixing, oven, custom cake, judging, ranked prize and return");phase=5;
    }
}
