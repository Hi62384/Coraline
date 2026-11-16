package com.coraline.feast;

import com.coraline.otherworld.DoorData;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.*;

/** Offstage Cat cleanup must not trap the player in a completed vision. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueFarewellGateGameTests {
    @GameTest(template="empty") public static void missingCatDepartureDoesNotBlockCompletedVision(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(ActivityWorld.KEY);var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"FarewellGateQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.loop.event.putInt("Serial",7);VisitData.get(p.server).scenes.put(s.owner,s);var n=RescueChapter.data(s);n.putBoolean("Started",true);n.putString("Phase","FAREWELL_C");var door=new DoorData.Door();door.id=s.door;door.owner=s.owner;door.pos=net.minecraft.core.BlockPos.ZERO;DoorData.get(p.server).doors.put(s.door,door);
        var at=new net.minecraft.core.BlockPos(140000,180,140000);var footing=at.below();var old=level.getBlockState(footing);level.getChunkAt(at);level.setBlock(footing,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);p.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(at));
        try{
            for(int i=0;i<3;i++){var eye=RescueEyes.eye(s,i);eye.putBoolean("Defeated",true);eye.putBoolean("Discovered",true);h.assertTrue(RescueEyes.claim(p,s,i),"Authentic eye fixture unavailable");}
            RescueEpilogue.tick(p,s,RescueChapter.Phase.FAREWELL_C,ShowRecordings.duration("farewell_c"),true);
            h.assertTrue(RescueEyes.ledger(s).getBoolean("Released")&&n.getBoolean("FarewellComplete")&&RescueChapter.phase(s)==RescueChapter.Phase.COMPLETE,"Offstage Cat cleanup blocked the completed vision");
            for(int i=0;i<3;i++){final int index=i;h.assertFalse(p.getInventory().items.stream().anyMatch(stack->RescueEyes.authentic(stack,s,index)),"Released eye remains usable after the farewell");}
            h.assertTrue(!p.isInvulnerable()&&!p.isNoGravity()&&n.getBoolean("VisionRestored")&&n.getBoolean("FarewellCatCleanupPending"),"Completion did not restore the player and retain pending Cat cleanup");
        }finally{level.setBlock(footing,old,2);VisitData.get(p.server).scenes.remove(s.owner);DoorData.get(p.server).doors.remove(s.door);}h.succeed();
    }
}
