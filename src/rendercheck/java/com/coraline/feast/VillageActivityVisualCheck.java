package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import com.coraline.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Opt-in in-game village/trading regression fixture. Excluded from release jars. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class VillageActivityVisualCheck {
    private static volatile int phase;
    private static int frames;
    private static VisitData.Scene scene;
    private static BlockPos center;
    private static final java.util.Map<java.util.UUID,net.minecraft.world.phys.Vec3> initialVillagers=new java.util.HashMap<>();
    private VillageActivityVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.villageVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=phase!=3;mc.options.renderDistance().set(10);mc.options.pauseOnLostFocus=false;
        if(phase==0){
            if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;
        }
        if(phase==2&&++frames>250){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/village_activity_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            System.out.println("CORALINE_VILLAGE_CHECK screenshot saved");
            phase=3;frames=0;mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->grant(mc.getSingleplayerServer()));
        }
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.OTHER_VILLAGE)))
            throw new IllegalStateException("Other Father activity voice was stopped after entering the village dimension");
        if(phase==3)frames++;
        if(phase==5)mc.stop();
    }
    @SubscribeEvent public static void gui(net.minecraftforge.client.event.RenderGuiEvent.Post event){
        if(!Boolean.getBoolean("coraline.villageVisualCheck")||phase!=3||frames<=100)return;
        var mc=Minecraft.getInstance();if(mc.getSingleplayerServer()==null)return;
        if(mc.player==null||mc.player.getY()>90||mc.player.getInventory().items.stream().noneMatch(stack->stack.is(ModItems.VILLAGE_TOKEN.get())))return;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/village_token_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            System.out.println("CORALINE_VILLAGE_CHECK token screenshot saved");
            phase=4;mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));
    }
    private static void setup(MinecraftServer server){
        System.out.println("CORALINE_VILLAGE_CHECK setting up");
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(24000,80,24000);
        DoorData.get(server).doors.put(door.id,door);
        var r=RealmData.get(server).realm(door);r.ready=true;r.parentGeneration=1;r.porch=r.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(r.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=r.doorPos;scene.table=r.doorPos;
        scene.loop.generation=1;scene.loop.portal=r.doorPos;scene.loop.phase=OtherMotherLoopState.Phase.READY;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);
        if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Village portal did not transport the player");
        center=VillageActivity.center(p,scene);
        var level=p.serverLevel();
        if(!level.getBlockState(center.offset(-50,-1,-50)).is(Blocks.GRASS_BLOCK)||
            !level.getBlockState(center.offset(49,-1,49)).is(Blocks.GRASS_BLOCK)||
            !level.isEmptyBlock(center.offset(50,-1,0)))throw new IllegalStateException("Village platform is not exactly 100x100");
        var villagers=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(center).inflate(48,12,48),VillageActivity::isVillageVillager);
        if(villagers.size()<40)throw new IllegalStateException("Only "+villagers.size()+" villagers spawned");
        int[] quadrants=new int[4];int outskirts=0;
        for(var v:villagers){int dx=v.blockPosition().getX()-center.getX(),dz=v.blockPosition().getZ()-center.getZ();
            quadrants[(dx>=0?1:0)+(dz>=0?2:0)]++;
            if(Math.abs(dx)>25||Math.abs(dz)>25)outskirts++;
        }
        for(int count:quadrants)if(count<6)throw new IllegalStateException("A village quadrant has only "+count+" residents");
        if(outskirts<20)throw new IllegalStateException("Villagers are still clumped near the plaza: "+outskirts+" on outskirts");
        initialVillagers.clear();for(var v:villagers)initialVillagers.put(v.getUUID(),v.position());
        var fathers=level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center).inflate(8),f->f.getPersistentData().getBoolean("CoralineVillageFather"));
        if(fathers.size()!=1)throw new IllegalStateException("Other Father did not wait beside arrival");
        if(scene.loop.villageIssued||scene.loop.villageTokensRemaining!=20||VillageActivity.exitReady(scene)||
            LoopProps.nearby(level,scene,center.south(5)).stream().anyMatch(e->e.kind()==ScenePropEntity.ACTIVITY_EXIT))
            throw new IllegalStateException("Tokens or return portal appeared before Father finished his introduction");
        p.teleportTo(level,center.getX()+.5,center.getY()+26,center.getZ()+37.5,180,42);
        p.getAbilities().flying=true;p.onUpdateAbilities();phase=2;
        System.out.println("CORALINE_VILLAGE_CHECK spawned "+villagers.size()+" villagers");
    }
    private static void grant(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();
        int moved=0;
        for(var v:level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(center).inflate(48,12,48),VillageActivity::isVillageVillager)){
            var initial=initialVillagers.get(v.getUUID());if(initial!=null&&initial.distanceToSqr(v.position())>1)moved++;
        }
        if(moved==0)System.out.println("CORALINE_VILLAGE_CHECK no villager moved a full block before the brief screenshot window");
        System.out.println("CORALINE_VILLAGE_CHECK "+moved+" villagers wandered");
        scene.loop.villageGiftAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(!scene.loop.villageIssued)throw new IllegalStateException("Father did not grant tokens when his introduction ended");
        int tokenSlot=-1;for(int i=0;i<9;i++)if(p.getInventory().getItem(i).is(ModItems.VILLAGE_TOKEN.get())){tokenSlot=i;break;}
        if(tokenSlot<0||p.getInventory().getItem(tokenSlot).getCount()!=20)throw new IllegalStateException("Father did not give exactly 20 button tokens");
        p.getInventory().selected=tokenSlot;
        var portrait=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(center).inflate(48,12,48),VillageActivity::isVillageVillager).get(0);
        portrait.setNoAi(true);portrait.getNavigation().stop();portrait.setYRot(0);portrait.setYBodyRot(0);portrait.setYHeadRot(0);
        p.teleportTo(level,portrait.getX(),portrait.getY(),portrait.getZ()+3,180,0);
        p.getAbilities().flying=false;p.onUpdateAbilities();
    }
    private static void verify(MinecraftServer server){
        System.out.println("CORALINE_VILLAGE_CHECK verifying trades");
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();
        var villagers=level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class,new AABB(center).inflate(48,12,48),VillageActivity::isVillageVillager);
        for(int i=0;i<20;i++){
            var v=villagers.get(i);p.teleportTo(level,v.getX(),v.getY(),v.getZ(),0,0);
            VillageActivity.trade(p,v,InteractionHand.MAIN_HAND);
            if(i==0){VillageActivity.trade(p,v,InteractionHand.MAIN_HAND);
                if(scene.loop.villageTokensRemaining!=19)throw new IllegalStateException("One villager accepted two tokens or no trade was accepted: remaining="+scene.loop.villageTokensRemaining+", active="+(OtherMotherLoop.active(p)!=null)+", held="+p.getMainHandItem()+", generation="+scene.loop.generation+"/"+v.getPersistentData().getInt("CoralineVillageGeneration")+", owner="+v.getPersistentData().getUUID("CoralineVillageOwner")+", distance="+p.distanceToSqr(v));}
        }
        if(scene.loop.villageTokensRemaining!=0||!VillageActivity.exitReady(scene)||
            LoopProps.nearby(level,scene,center.south(5)).stream().noneMatch(e->e.kind()==ScenePropEntity.ACTIVITY_EXIT))
            throw new IllegalStateException("Return portal did not appear after exactly 20 trades");
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE)
            throw new IllegalStateException("Village return portal did not complete the activity");
        if(!level.isEmptyBlock(center.below())||!level.isEmptyBlock(center.offset(-35,-1,-35)))
            throw new IllegalStateException("The previous village platform remained after leaving the activity");
        for(ItemStack stack:p.getInventory().items)if(stack.is(ModItems.VILLAGE_TOKEN.get()))throw new IllegalStateException("Unused activity tokens escaped the village");
        System.out.println("CORALINE_VILLAGE_CHECK passed all 20 single-use trades and return transport");phase=5;
    }
}
