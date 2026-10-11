package com.coraline.feast;

import com.coraline.otherworld.DoorBlockEntity;
import com.coraline.otherworld.DoorData;
import com.coraline.otherworld.OtherWorld;
import com.coraline.otherworld.RealmData;
import com.coraline.parents.FamilyData;
import com.coraline.parents.ParentEntity;
import com.coraline.registry.ModBlocks;
import com.coraline.registry.ModEntities;
import com.coraline.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Disposable in-game regression for Creative debug completion and a real Little Door return. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class DebugVisitVisualCheck {
    private static boolean begun,done;
    private DebugVisitVisualCheck(){}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(!Boolean.getBoolean("coraline.debugVisitCheck")||event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.getSingleplayerServer()==null)return;
        if(!begun){begun=true;mc.getSingleplayerServer().execute(()->verify(mc.getSingleplayerServer()));}
        else if(done)mc.stop();
    }
    private static void verify(MinecraftServer server){
        var p=server.getPlayerList().getPlayers().get(0);p.setGameMode(GameType.CREATIVE);
        var home=server.overworld();var spawn=OtherWorld.safePlayer(home,home.getSharedSpawnPos());
        p.teleportTo(home,spawn.getX()+.5,spawn.getY(),spawn.getZ()+.5,0,0);
        p.getPersistentData().remove("CoralineVisitDoor");p.getPersistentData().remove("CoralineInTunnel");
        p.getPersistentData().remove("CoralineReturning");p.getPersistentData().remove("CoralinePortalCooldown");
        var family=com.coraline.parents.ParentAccess.family(server,p.getUUID());
        if(family==null){family=new FamilyData.Family();family.owner=p.getUUID();FamilyData.get(server).families.put(family.id,family);FamilyData.get(server).setDirty();}
        var door=new DoorData.Door();door.owner=p.getUUID();door.pos=p.blockPosition();DoorData.get(server).doors.put(door.id,door);DoorData.get(server).setDirty();
        var r=RealmData.get(server).realm(door);r.ready=true;r.parentGeneration=1;r.porch=r.doorPos;
        var debugScene=new VisitData.Scene();debugScene.owner=p.getUUID();debugScene.family=family.id;debugScene.door=door.id;
        debugScene.table=debugScene.piano=r.doorPos;VisitData.get(server).scenes.put(p.getUUID(),debugScene);VisitData.get(server).setDirty();
        var other=server.getLevel(OtherWorld.KEY);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)other.setBlock(r.doorPos.offset(x,-1,z),Blocks.CHERRY_PLANKS.defaultBlockState(),2);
        other.setBlock(r.doorPos,ModBlocks.TINY_DOOR.get().defaultBlockState()
            .setValue(com.coraline.block.TinyDoorBlock.PORTAL,true).setValue(com.coraline.block.TinyDoorBlock.OPEN,true),3);
        var block=(DoorBlockEntity)other.getBlockEntity(r.doorPos);block.doorId=door.id;block.setChanged();
        String result=FirstVisit.debugComplete(p);
        var scene=VisitData.get(server).scenes.get(p.getUUID());
        if(scene==null||scene.stage!=VisitData.Stage.DONE||scene.loop.active())throw new IllegalStateException("Home debug completion failed: "+result);
        p.getInventory().setItem(0,new ItemStack(ModItems.TRUST_UP_DEBUG.get()));p.getInventory().selected=0;
        ModItems.TRUST_UP_DEBUG.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        ModItems.TRUST_UP_DEBUG.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        p.getInventory().setItem(0,new ItemStack(ModItems.SUSPICION_UP_DEBUG.get()));
        ModItems.SUSPICION_UP_DEBUG.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        ModItems.SUSPICION_UP_DEBUG.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        if(scene.loop.trust!=.5F||scene.loop.suspicion!=.5F||scene.welcomeState!=OtherMotherWelcome.State.MID_TRUST_MID_SUSPICION)
            throw new IllegalStateException("Relationship controls did not set the mid-state voice category");
        var mom=ModEntities.MOM.get().create(other);mom.makeOtherParent(0);
        mom.getPersistentData().putUUID("RealmDoor",door.id);mom.getPersistentData().putInt("RealmGeneration",r.parentGeneration);
        mom.setPos(r.doorPos.getX()+2.5,r.doorPos.getY(),r.doorPos.getZ()+.5);other.addFreshEntity(mom);
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()+2.5,0,0);
        boolean prepared=OtherMotherLoop.prepare(p,r),entered=OtherMotherLoop.entered(p,r);
        if(!prepared||!entered||scene.loop.phase!=OtherMotherLoopState.Phase.WELCOME)
            throw new IllegalStateException("Main loop did not start on entry after debug completion: prepared="+prepared+", entered="+entered+", phase="+scene.loop.phase+", mom="+scene.mom+", door="+scene.door+", realm="+r.door);
        FirstVisit.debugComplete(p);
        if(OtherMotherLoop.locked(p)||!p.getPersistentData().getBoolean("CoralineDebugVisitCompleted"))
            throw new IllegalStateException("The debug shortcut did not release the Little Door");
        p.getPersistentData().remove("CoralinePortalCooldown");OtherWorld.enter(p,block);
        if(!p.getPersistentData().getBoolean("CoralineInTunnel")||!p.getPersistentData().getBoolean("CoralineReturning"))
            throw new IllegalStateException("The Little Door failed to start a return tunnel");
        OtherWorld.leave(p,false);
        if(OtherWorld.isOther(p.level())||p.getPersistentData().getBoolean("CoralineDebugVisitCompleted"))
            throw new IllegalStateException("Return home did not clear debug visit state");
        p.getPersistentData().putUUID("CoralineVisitDoor",door.id);
        p.teleportTo(other,r.doorPos.getX()+.5,r.doorPos.getY(),r.doorPos.getZ()+2.5,0,0);
        if(!OtherMotherLoop.prepare(p,r)||!OtherMotherLoop.entered(p,r)||scene.loop.phase!=OtherMotherLoopState.Phase.WELCOME)
            throw new IllegalStateException("The repeat loop did not start after leaving and re-entering");
        FirstVisit.debugComplete(p);
        String trip=ActivityWorld.enterDebug(p);
        if(!ActivityWorld.isActivity(p.level()))throw new IllegalStateException("Creative Village shortcut failed: "+trip);
        scene.loop.villageIssued=true;scene.loop.villageTokensRemaining=0;
        ActivityWorld.exit(p,scene);
        if(!OtherWorld.isOther(p.level())||scene.loop.active())throw new IllegalStateException("Creative Village shortcut did not return to its starting point");
        System.out.println("CORALINE_DEBUG_VISIT_CHECK passed debug completion, repeat entry, and Creative Village shortcut");
        done=true;
    }
}
