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

/** Opt-in full feast voice, serving-wave, basket and return-portals check. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class GrandFeastVisualCheck {
    private static volatile int phase;
    private static int frames;
    private static VisitData.Scene scene;
    private static BlockPos center;
    private GrandFeastVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.grandFeastVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(12);mc.options.pauseOnLostFocus=false;
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.GRAND_FEAST)))
            throw new IllegalStateException("Feast Father voice stopped after entry");
        if(phase==2&&++frames>130){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/grand_feast_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            phase=3;mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));
        }
        if(phase==4&&++frames>24){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/grand_feast_frenzy_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            phase=5;mc.getSingleplayerServer().execute(()->verifyFinish(mc.getSingleplayerServer()));
        }
        if(phase==6)mc.stop();
    }
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(40000,80,40000);
        DoorData.get(server).doors.put(door.id,door);
        var realm=RealmData.get(server).realm(door);realm.ready=true;realm.parentGeneration=1;realm.porch=realm.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(realm.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=realm.doorPos;scene.table=realm.doorPos;scene.loop.generation=1;scene.loop.portal=realm.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.GRAND_FEAST;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,realm.doorPos.getX()+.5,realm.doorPos.getY(),realm.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Feast portal failed");
        center=GrandFeastActivity.center(p,scene);var level=p.serverLevel();
        if(!level.getBlockState(center.below(3)).is(Blocks.CYAN_GLAZED_TERRACOTTA))throw new IllegalStateException("Updated feast town was not built");
        if(level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center.south(36)).inflate(8),
            e->e.getPersistentData().getBoolean("CoralineFeastFather")).size()!=1)throw new IllegalStateException("Feast Father missing");
        int guests=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(center).inflate(25,8,25),
            e->e.getPersistentData().getBoolean("CoralineFeastVillager")).size();
        if(guests<18)throw new IllegalStateException("Feast guests missing: "+guests);
        int displays=level.getEntitiesOfClass(ScenePropEntity.class,new AABB(center).inflate(20,8,20),
            e->e.kind()==ScenePropEntity.PLATE).size();
        if(displays<16)throw new IllegalStateException("Feast plate displays missing: "+displays);
        p.getAbilities().flying=true;p.onUpdateAbilities();
        p.teleportTo(level,center.getX()+.5,center.getY()+18,center.getZ()+25.5,180,30);
        phase=2;
    }
    private static void verify(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var l=scene.loop;var level=p.serverLevel();
        if(l.feastEndAt!=0)throw new IllegalStateException("Feast timer started during Father voice");
        l.feastVoiceUntil=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.feastEndAt-OtherMotherLoop.now(p)!=3600)throw new IllegalStateException("Feast timer is not three minutes");
        l.feastNextWaveAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(l.feastWave<1||java.util.Arrays.stream(l.feastPlateFood).allMatch(id->id==0))throw new IllegalStateException("Serving wave produced no food");
        int slot=0;while(slot<16&&l.feastPlateFood[slot]==0)slot++;
        l.feastPlateRefillAt[slot]=OtherMotherLoop.now(p)-1;
        GrandFeastActivity.prepare(p,scene,level,center);
        final int target=slot;
        var plate=level.getEntitiesOfClass(ScenePropEntity.class,new AABB(center).inflate(20,8,20),
            e->e.kind()==ScenePropEntity.PLATE&&e.slot==target).get(0);
        p.teleportTo(level,plate.getX()+1,center.getY(),plate.getZ()+1,180,0);
        LoopProps.interact(p,plate);
        if(l.feastBasketCount==0||l.feastPlateFood[slot]!=0)throw new IllegalStateException("Feast basket did not collect serving");
        int collected=l.feastBasketCount;LoopProps.interact(p,plate);
        if(l.feastBasketCount!=collected)throw new IllegalStateException("Feast plate could be claimed twice");
        l.feastEndAt=OtherMotherLoop.now(p)+200;l.feastNextWaveAt=OtherMotherLoop.now(p);
        ActivityWorld.tick(p,scene);
        if(l.feastWave<2||java.util.Arrays.stream(l.feastPlateFood).noneMatch(id->id>=12))
            throw new IllegalStateException("Final frenzy did not deliver rare food");
        p.getAbilities().flying=true;p.onUpdateAbilities();
        p.teleportTo(level,center.getX()+.5,center.getY()+12,center.getZ()+19.5,180,36);
        frames=0;phase=4;
    }
    private static void verifyFinish(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var l=scene.loop;var level=p.serverLevel();
        l.feastEndAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(!l.feastFinished||!GrandFeastActivity.exitReady(scene)||l.feastBasketCount!=0||
            p.getInventory().items.stream().allMatch(net.minecraft.world.item.ItemStack::isEmpty))
            throw new IllegalStateException("Feast payout or return portal failed");
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE)
            throw new IllegalStateException("Feast did not return to the Other World");
        System.out.println("CORALINE_GRAND_FEAST_CHECK passed Father voice, three-minute timer, wave, basket payout and exit");phase=6;
    }
}
