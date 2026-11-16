package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostVictimNavigationGameTests {
    @GameTest(template="empty",timeoutTicks=400) public static void protectedOtherWorldAdmitsOriginalChildAndRejectsUnleasedNpc(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());
        var level=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        h.assertTrue(level!=null,"Other World test dimension is missing");
        AcceptedButtonEyes.data(s).putBoolean("Started",true);
        var n=new CompoundTag();n.putBoolean("Started",true);n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putUUID("Session",java.util.UUID.randomUUID());
        GhostVictimProgress.newVictim(n,java.util.UUID.randomUUID(),new CompoundTag());
        GhostVictimProgress.move(n,GhostVictimProgress.Phase.WAITING_FOR_VICTIM);
        AcceptedButtonEyes.data(s).put(GhostVictimProgress.KEY,n);
        var at=new BlockPos(300000,200,300000);var chunk=new net.minecraft.world.level.ChunkPos(at);boolean wasForced=level.getForcedChunks().contains(chunk.toLong());level.setChunkForced(chunk.x,chunk.z,true);level.getChunkAt(at);
        int[] waited={0};boolean[] started={false};h.onEachTick(()->{if(started[0])return;if(!level.areEntitiesLoaded(chunk.toLong())){if(++waited[0]>=280){if(!wasForced)level.setChunkForced(chunk.x,chunk.z,false);AcceptedBranchPlayers.remove(p,s);h.fail("Fixture entity section did not load within its bounded setup wait");}return;}started[0]=true;com.coraline.tycoon.TycoonNpc child=null;
        var unrelated=com.coraline.tycoon.TycoonContent.NPC.get().create(level);
        try{
            unrelated.setPos(Vec3.atBottomCenterOf(at));
            h.assertFalse(level.addFreshEntity(unrelated),"Protected realm admitted an unleased NPC");
            child=GhostVictimActors.arrive(p,s,n,level,at,true);
            h.assertTrue(child!=null,"Realm spawn protection rejected the scripted child");
            h.assertTrue(level.getEntity(child.getUUID())==child&&GhostVictimActors.owns(child,s,n),"Child was not registered with its original identity and lease");
            h.assertTrue(GhostVictimActors.arrive(p,s,n,level,at,true)==child,"Arrival retry duplicated the child");
            var lease=child.getPersistentData().getCompound(GhostVictimActors.LEASE);
            lease.putUUID("Session",java.util.UUID.randomUUID());
            h.assertFalse(GhostVictimActors.allowedInProtectedWorld(child),"An expired story lease bypassed spawn protection");
        }finally{if(child!=null)child.discard();unrelated.discard();if(!wasForced)level.setChunkForced(chunk.x,chunk.z,false);AcceptedBranchPlayers.remove(p,s);}
        h.succeed();});
    }
    @GameTest(template="empty") public static void obstructedEmergenceCanRecoverWithoutDuplicatingTheChild(GameTestHelper h){
        var level=h.getLevel();var server=level.getServer();
        var p=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"EmergenceQA"));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();
        var n=new CompoundTag();n.putBoolean("Started",true);n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putUUID("Session",java.util.UUID.randomUUID());
        var identity=java.util.UUID.randomUUID();GhostVictimProgress.newVictim(n,identity,new CompoundTag());
        var at=h.absolutePos(new BlockPos(3,2,3));var old=level.getBlockState(at);level.setBlock(at,Blocks.STONE.defaultBlockState(),2);
        com.coraline.tycoon.TycoonNpc child=null;
        try{
            h.assertTrue(GhostVictimActors.arrive(p,s,n,level,at)==null,"Ordinary arrival ignored obstructed terrain");
            child=GhostVictimActors.arrive(p,s,n,level,at,true);
            h.assertTrue(child!=null&&child.getUUID().equals(identity),"Recovery did not preserve the original child identity");
            h.assertTrue(child.noPhysics&&n.getBoolean("EmergenceRecovery"),"Recovery did not enter the bounded emergence animation");
            h.assertTrue(GhostVictimActors.arrive(p,s,n,level,at,true)==child,"Retry created a second child");
        }finally{if(child!=null)child.discard();level.setBlock(at,old,2);}
        h.succeed();
    }
    @GameTest(template="empty") public static void perceptionFollowsTheHeadAndCannotSeeThroughCover(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(110,30,80));var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-5;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=2;y++){
            var at=base.offset(x,y,z);level.getChunkAt(at);saved.put(at,level.getBlockState(at));level.setBlock(at,(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        }
        var observer=EntityType.PIG.create(level);var target=EntityType.PIG.create(level);observer.setPos(Vec3.atBottomCenterOf(base));target.setPos(Vec3.atBottomCenterOf(base.west(4)));level.addFreshEntity(observer);level.addFreshEntity(target);
        try{
            observer.setYRot(0);observer.setYHeadRot(90);observer.setXRot(0);
            h.assertTrue(GhostVictimNavigation.canSee(observer,target,8,90),"Visible target in front of the actual head was ignored");
            observer.setYRot(180);h.assertTrue(GhostVictimNavigation.canSee(observer,target,8,90),"Body rotation overrode a fixed head gaze");
            observer.setYHeadRot(270);h.assertFalse(GhostVictimNavigation.canSee(observer,target,8,90),"Target behind the head was observed");
            observer.setYHeadRot(90);level.setBlock(base.west(2),Blocks.OAK_PLANKS.defaultBlockState(),2);level.setBlock(base.west(2).above(),Blocks.OAK_PLANKS.defaultBlockState(),2);
            h.assertFalse(GhostVictimNavigation.canSee(observer,target,8,360),"Cover failed to block the observation ray");
        }finally{observer.discard();target.discard();saved.forEach((at,state)->level.setBlock(at,state,2));}
        h.succeed();
    }
}
