package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueGhostContinuityGameTests {
    @GameTest(template="empty",timeoutTicks=200) public static void realFirstEscapeCleanupPreservesOnlyItsThreeOriginalChildrenThroughReload(GameTestHelper h){
        var real=h.getLevel();var l=real.getServer().getLevel(com.coraline.otherworld.OtherWorld.KEY);var p=new ServerPlayer(real.getServer(),real,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"GhostContinuityQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",7);VisitData.get(p.server).scenes.put(s.owner,s);var old=FirstEscapeStory.data(s);old.putBoolean("Started",true);old.putString("Phase","MIRROR_E");old.putLong("House",new BlockPos(109000,100,109000).asLong());old.putLong("Prison",new BlockPos(109000,180,109000).asLong());
        var door=new com.coraline.otherworld.DoorData.Door();door.id=s.door;door.owner=s.owner;door.pos=h.absolutePos(new BlockPos(2,3,2));com.coraline.otherworld.DoorData.get(p.server).doors.put(door.id,door);LoopProps.hold(l,BlockPos.of(old.getLong("Prison")));
        h.runAfterDelay(25,()->{var originals=new java.util.ArrayList<CinematicPropEntity>();var stray=CinematicPropEntity.TYPE.get().create(l);stray.configure(s,CinematicPropEntity.GHOST_CHILD,0);stray.setPos(Vec3.atBottomCenterOf(BlockPos.of(old.getLong("Prison"))));l.addFreshEntity(stray);
            for(int i=0;i<3;i++){var ghost=FirstEscapeScene.ensure(p,s,"Ghost"+i,CinematicPropEntity.GHOST_CHILD,i,l,stray.position().add(i+1,0,0));ghost.getPersistentData().putString("OriginalMemory","Child "+i);originals.add(ghost);}
            // Wait for actual entity-manager visibility, not a guessed delay:
            // remote chunk promotion can take more than two ticks in a batch.
            h.runAfterDelay(2,()->finish(h,p,s,originals,stray,0));
        });
    }
    private static void finish(GameTestHelper h,ServerPlayer p,VisitData.Scene s,java.util.List<CinematicPropEntity> originals,CinematicPropEntity stray,int waiting){
        var l=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);LoopProps.hold(l,stray.blockPosition());
        if(l.getEntity(stray.getUUID())!=stray||originals.stream().anyMatch(e->l.getEntity(e.getUUID())!=e)){
            h.assertTrue(waiting<100,"Original/stray prison entities never became visible before testing cleanup");h.runAfterDelay(1,()->finish(h,p,s,originals,stray,waiting+1));return;
        }
        try{
                h.assertTrue(FirstEscapeStory.finishFirstEscape(p,s,false),"Real first-escape milestone refused its saved owner/door");h.assertTrue(stray.isRemoved(),"Unreceipted duplicate ghost survived ordinary scenery cleanup");s.loop.event.putInt("Serial",8);
                for(int i=0;i<3;i++){var original=originals.get(i);original.tick();h.assertTrue(!original.isRemoved()&&original.isInvisible()&&original.canonicalGhost(s),"Original child was deleted by cleanup or the next event serial");var saved=original.saveWithoutId(new CompoundTag());var loaded=CinematicPropEntity.TYPE.get().create(l);loaded.load(saved);loaded.tick();h.assertTrue(!loaded.isRemoved()&&loaded.getUUID().equals(original.getUUID())&&loaded.canonicalGhost(s)&&loaded.getPersistentData().getString("OriginalMemory").equals("Child "+i),"Child UUID/metadata did not survive save/reload");}
        }finally{for(var original:originals)original.discard();stray.discard();VisitData.get(p.server).scenes.remove(s.owner);com.coraline.otherworld.DoorData.get(p.server).doors.remove(s.door);}h.succeed();
    }
    private RescueGhostContinuityGameTests(){}
}
