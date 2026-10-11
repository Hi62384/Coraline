package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import com.coraline.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;

/** Opt-in archery render and 64-shot ledger regression, excluded from release JARs. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ArcheryActivityVisualCheck {
    private static volatile int phase;
    private static int frames;
    private static VisitData.Scene scene;
    private static BlockPos center;
    private ArcheryActivityVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.archeryVisualCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
        mc.options.hideGui=true;mc.options.renderDistance().set(10);mc.options.pauseOnLostFocus=false;
        if(phase==0){if(mc.getSingleplayerServer().getPlayerList().getPlayers().isEmpty())return;
            phase=1;mc.getSingleplayerServer().execute(()->setup(mc.getSingleplayerServer()));return;}
        if(phase==2&&++frames>130){
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(Path.of("../artwork/archery_activity_native.png"));}
            catch(Exception ex){throw new RuntimeException(ex);}
            System.out.println("CORALINE_ARCHERY_CHECK screenshot saved");phase=3;
            mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));
        }
        if(phase==2&&frames==30&&!com.coraline.client.VisitAudioClient.hasVoice(ActivityWorld.introSound(OtherMotherActivities.Activity.ARCHERY_CONTEST)))
            throw new IllegalStateException("Other Father archery voice stopped after entering the activity dimension");
        if(phase==5)mc.stop();
    }
    private static void setup(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=new BlockPos(26000,80,26000);
        DoorData.get(server).doors.put(door.id,door);
        var r=RealmData.get(server).realm(door);r.ready=true;r.parentGeneration=1;r.porch=r.doorPos;
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(r.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=door.id;scene.stage=VisitData.Stage.DONE;
        scene.piano=r.doorPos;scene.table=r.doorPos;scene.loop.generation=1;scene.loop.portal=r.doorPos;
        scene.loop.phase=OtherMotherLoopState.Phase.READY;scene.loop.activity=OtherMotherActivities.Activity.ARCHERY_CONTEST;
        VisitData.get(server).scenes.put(p.getUUID(),scene);VisitData.get(server).setDirty();
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()+.5,0,0);
        ActivityWorld.enter(p,scene);
        if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Archery portal did not transport the player");
        center=ArcheryActivity.center(p,scene);var level=p.serverLevel();
        level.setWeatherParameters(6000,0,false,false);
        if(!level.getBlockState(center.offset(0,-1,-20)).is(Blocks.GRASS_BLOCK)||
            !level.getBlockState(center.offset(0,-1,-7)).is(Blocks.RED_CONCRETE))
            throw new IllegalStateException("Archery platform or firing line was not built");
        var targets=level.getEntitiesOfClass(ScenePropEntity.class,new AABB(center).inflate(65,8,65),ArcheryActivity::isTarget);
        if(targets.size()!=ArcheryActivity.TARGETS)throw new IllegalStateException("Expected 28 targets, found "+targets.size());
        if(targets.stream().map(ScenePropEntity::kind).distinct().count()<6)throw new IllegalStateException("Not all six target types spawned");
        var near=targets.stream().filter(t->t.slot<7&&t.kind()==ScenePropEntity.TARGET_STATIC).findFirst().orElseThrow();
        var far=targets.stream().filter(t->t.slot>=21&&t.kind()==ScenePropEntity.TARGET_STATIC).findFirst().orElseThrow();
        if(far.targetScale()>=near.targetScale()||far.getBbWidth()>=near.getBbWidth())
            throw new IllegalStateException("Distant targets did not become smaller and harder to hit");
        if(level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center).inflate(10),f->f.getPersistentData().getBoolean("CoralineArcheryFather")).size()!=1)
            throw new IllegalStateException("Other Father is not at archery arrival");
        if(scene.loop.archeryIssued||ArcheryActivity.exitReady(scene))throw new IllegalStateException("Equipment arrived before Father's speech");
        System.out.println("CORALINE_ARCHERY_CHECK spawned 28 targets and Father");phase=2;
    }
    private static void verify(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);var level=p.serverLevel();
        var father=level.getEntitiesOfClass(com.coraline.parents.ParentEntity.class,new AABB(center).inflate(10),f->f.getPersistentData().getBoolean("CoralineArcheryFather")).get(0);
        if(!father.scriptedGaze()||Math.abs(net.minecraft.util.Mth.wrapDegrees(father.getYHeadRot()-90))>12)
            throw new IllegalStateException("Other Father did not keep looking at the arriving player");
        p.teleportTo(level,center.getX()+.5,center.getY(),center.getZ()-20.5,180,0);
        ActivityWorld.tick(p,scene);
        if(p.getZ()<center.getZ()-6)throw new IllegalStateException("Player entered the target area");
        scene.loop.archeryGiftAt=OtherMotherLoop.now(p);ActivityWorld.tick(p,scene);
        if(!scene.loop.archeryIssued)throw new IllegalStateException("Father did not issue archery equipment");
        ItemStack bow=ItemStack.EMPTY,ammo=ItemStack.EMPTY;
        for(var stack:p.getInventory().items){if(stack.is(ModItems.ARCHERY_BOW.get()))bow=stack;
            if(stack.is(ModItems.ARCHERY_ARROW.get()))ammo=stack;}
        if(bow.isEmpty()||ammo.getCount()!=64||p.getProjectile(bow).isEmpty())
            throw new IllegalStateException("Father did not give a usable bow and exactly 64 issued arrows");
        p.getInventory().offhand.set(0,new ItemStack(Items.ARROW,64));
        if(!p.getProjectile(bow).is(ModItems.ARCHERY_ARROW.get()))
            throw new IllegalStateException("Personal offhand arrows replaced Father's contest arrows");
        var targets=level.getEntitiesOfClass(ScenePropEntity.class,new AABB(center).inflate(65,8,65),ArcheryActivity::isTarget);
        var target=targets.stream().filter(t->t.kind()==ScenePropEntity.TARGET_STATIC).findFirst().orElseThrow();
        bow.getItem().releaseUsing(bow,level,p,bow.getItem().getUseDuration(bow)-20);
        var shots=level.getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
            new AABB(p.blockPosition()).inflate(4),a->a.getPersistentData().getBoolean("CoralineArcheryArrow"));
        if(shots.size()!=1||scene.loop.archeryShotsRemaining!=63||p.getInventory().offhand.get(0).getCount()!=64)
            throw new IllegalStateException("Actual bow shot used personal ammo or failed to count");
        var arrow=shots.get(0);
        int rewardsBefore=p.getInventory().items.stream().filter(stack->!stack.is(ModItems.ARCHERY_BOW.get())&&
            !stack.is(ModItems.ARCHERY_ARROW.get())&&!stack.is(Items.ARROW)).mapToInt(ItemStack::getCount).sum();
        var impact=new ProjectileImpactEvent(arrow,new EntityHitResult(target,target.position().add(0,.55,.1)));
        MinecraftForge.EVENT_BUS.post(impact);
        int rewardsAfter=p.getInventory().items.stream().filter(stack->!stack.is(ModItems.ARCHERY_BOW.get())&&
            !stack.is(ModItems.ARCHERY_ARROW.get())&&!stack.is(Items.ARROW)).mapToInt(ItemStack::getCount).sum();
        if(impact.getImpactResult()!=ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE||rewardsAfter<=rewardsBefore)
            throw new IllegalStateException("A target hit did not grant its prize");
        if(!arrow.isRemoved())throw new IllegalStateException("Target hit did not consume the arrow");
        if(ArcheryActivity.exitReady(scene))throw new IllegalStateException("Return portal appeared too early");
        for(int i=1;i<64;i++){var shot=((ArrowItem)ModItems.ARCHERY_ARROW.get()).createArrow(level,ammo,p);
            if(!level.addFreshEntity(shot))throw new IllegalStateException("Issued shot "+(i+1)+" rejected");shot.discard();}
        if(scene.loop.archeryShotsRemaining!=0||!ArcheryActivity.exitReady(scene)||
            LoopProps.nearby(level,scene,center.south(9)).stream().noneMatch(e->e.kind()==ScenePropEntity.ACTIVITY_EXIT))
            throw new IllegalStateException("Return portal did not open on the 64th issued shot");
        var loose=new ArrowLooseEvent(p,bow,level,20,true);MinecraftForge.EVENT_BUS.post(loose);
        if(!loose.isCanceled())throw new IllegalStateException("Personal arrows extended the completed contest");
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.phase!=OtherMotherLoopState.Phase.COMPLETE)
            throw new IllegalStateException("Archery return portal did not complete activity");
        if(!level.isEmptyBlock(center.below())||!level.isEmptyBlock(center.offset(0,-1,-20)))
            throw new IllegalStateException("The archery platform remained after leaving the activity");
        System.out.println("CORALINE_ARCHERY_CHECK passed 64 issued shots, target reward, boundary and return transport");phase=5;
    }
}
