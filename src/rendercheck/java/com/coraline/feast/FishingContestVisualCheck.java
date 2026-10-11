package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import com.coraline.registry.ModItems;
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

/** Opt-in real-client check of the fishing contest's complete lifecycle. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class FishingContestVisualCheck {
    private static volatile int phase;
    private static int frames;
    private static VisitData.Scene scene;
    private static BlockPos center;
    private FishingContestVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.fishingVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(12);mc.options.pauseOnLostFocus=false;
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.FISHING_CONTEST)))
            throw new IllegalStateException("Other Father fishing voice stopped after entry");
        if(phase==2&&++frames>130){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/fishing_contest_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            System.out.println("CORALINE_FISHING_CHECK screenshot saved");phase=3;
            mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));
        }
        if(phase==5)mc.stop();
    }
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(34000,80,34000);
        DoorData.get(server).doors.put(door.id,door);
        var realm=RealmData.get(server).realm(door);realm.ready=true;realm.parentGeneration=1;realm.porch=realm.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(realm.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=realm.doorPos;scene.table=realm.doorPos;scene.loop.generation=1;scene.loop.portal=realm.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.FISHING_CONTEST;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,realm.doorPos.getX()+.5,realm.doorPos.getY(),realm.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);
        if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Fishing portal did not transport the player");
        center=FishingContestActivity.center(p,scene);var level=p.serverLevel();
        if(!level.getBlockState(center.below(8)).is(Blocks.BLUE_GLAZED_TERRACOTTA)||
           !level.getBlockState(center.below()).is(Blocks.WATER)||
           !level.getBlockState(center.offset(0,0,32)).is(Blocks.SPRUCE_PLANKS))
            throw new IllegalStateException("Fishing lake or judging pavilion was not built");
        for(int x=-43;x<=43;x+=3)for(int z=-43;z<=43;z+=3)
            if(level.isEmptyBlock(center.offset(x,-8,z)))throw new IllegalStateException("Fishing terrain has a void floor at "+x+","+z);
        int contestants=level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(center).inflate(48,9,48),npc->!npc.judge()).size();
        int judges=level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(center).inflate(48,9,48),FishingContestNpcEntity::judge).size();
        if(contestants!=4||judges!=3)throw new IllegalStateException("Expected four contestants and three judges, found "+contestants+"/"+judges);
        for(var judge:level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(center).inflate(48,9,48),FishingContestNpcEntity::judge))
            if(Math.abs(net.minecraft.util.Mth.wrapDegrees(judge.yBodyRot-180))>1||Math.abs(net.minecraft.util.Mth.wrapDegrees(judge.getYHeadRot()-180))>1)
                throw new IllegalStateException("Judge is facing away from the judging panel");
        if(level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center.south(20)).inflate(8),f->f.getPersistentData().getBoolean("CoralineFishingFather")).size()!=1)
            throw new IllegalStateException("Other Father did not arrive at the fishing contest");
        if(FishingContestActivity.exitReady(scene))throw new IllegalStateException("Return portal appeared early");
        System.out.println("CORALINE_FISHING_CHECK lake, seven NPCs and Father spawned");phase=2;
    }
    private static void verify(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();var loop=scene.loop;
        if(loop.fishingStage!=0)throw new IllegalStateException("Fishing timer began before Father's voice completed");
        loop.fishingVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(loop.fishingStage!=1||loop.fishingEndAt-OtherMotherLoop.now(p)!=FishingContestActivity.DURATION||
           p.getInventory().items.stream().noneMatch(stack->stack.is(ModItems.FISHING_CONTEST_ROD.get())))
            throw new IllegalStateException("Voice gate did not grant a rod and exactly 180 seconds");
        if(level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(center).inflate(48,9,48),npc->!npc.judge()&&npc.fishing()).size()!=4)
            throw new IllegalStateException("Contestants did not begin animated fishing");
        var rod=p.getInventory().items.stream().filter(stack->stack.is(ModItems.FISHING_CONTEST_ROD.get())).findFirst().orElseThrow();
        if(net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.FISHING_SPEED,rod)<4)
            throw new IllegalStateException("Contest rod did not shorten bite waits");
        if(!FishingContestActivity.canFish(p,rod))throw new IllegalStateException("Issued rod cannot be used in the contest");
        p.getInventory().selected=p.getInventory().items.indexOf(rod);
        rod.getItem().use(level,p,net.minecraft.world.InteractionHand.MAIN_HAND);
        if(p.fishing==null)throw new IllegalStateException("The issued rod failed to create a fishing hook");
        rod.getItem().use(level,p,net.minecraft.world.InteractionHand.MAIN_HAND);
        loop.fishingScore=64;loop.fishingCatches=7;loop.fishingRivalScores[0]=22;loop.fishingRivalScores[1]=68;
        loop.fishingRivalScores[2]=41;loop.fishingRivalScores[3]=36;
        loop.fishingEndAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(loop.fishingStage!=2||p.getInventory().items.stream().anyMatch(stack->stack.is(ModItems.FISHING_CONTEST_ROD.get())))
            throw new IllegalStateException("Timer expiry did not end fishing and remove Father's rod");
        p.teleportTo(level,center.getX()+.5,center.getY()+1,center.getZ()+29.5,180,0);ActivityWorld.tick(p,scene);
        if(loop.fishingStage!=3||loop.fishingJudgeUntil-OtherMotherLoop.now(p)!=200)
            throw new IllegalStateException("Judging pavilion did not start ten-second deliberation");
        for(var npc:level.getEntitiesOfClass(FishingContestNpcEntity.class,new AABB(center).inflate(48,9,48),n->!n.judge()))
            if(npc.fishing()||Math.abs(net.minecraft.util.Mth.wrapDegrees(npc.yBodyRot))>1||Math.abs(net.minecraft.util.Mth.wrapDegrees(npc.getYHeadRot()))>1)
                throw new IllegalStateException("Contestant kept fishing or faced away during judging");
        loop.fishingJudgeUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(loop.fishingStage!=4||loop.fishingRank!=2)throw new IllegalStateException("Judges did not rank catch values correctly");
        for(int i=0;i<5;i++){loop.fishingAnnounceAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);}
        if(loop.fishingStage!=5||!FishingContestActivity.exitReady(scene)||
            LoopProps.nearby(level,scene,center.south(41)).stream().noneMatch(e->e.kind()==ScenePropEntity.ACTIVITY_EXIT))
            throw new IllegalStateException("Ranked prize or return portal did not complete");
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE||!level.isEmptyBlock(center.below(8)))
            throw new IllegalStateException("Fishing contest did not clean up and return to the Other World");
        System.out.println("CORALINE_FISHING_CHECK passed voice, 180s, rod, judging, prize, portal, return");phase=5;
    }
}
