package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class LiveBugRegressionGameTests {
    @GameTest(template="empty") public static void outingAndDeliveryRoutesCanWalkOffThePorchStairs(GameTestHelper h){
        var l=h.getLevel();var at=h.absolutePos(new BlockPos(1,2,1));l.setBlock(at.below(),Blocks.STONE_BRICK_STAIRS.defaultBlockState(),2);h.assertTrue(OutingSites.feet(l,at),"Stair exit is still excluded by full-square support test");
        l.setBlock(at.below(),Blocks.OAK_PLANKS.defaultBlockState(),2);l.setBlock(at,Blocks.BLUE_CARPET.defaultBlockState(),2);h.assertTrue(OutingSites.feet(l,at),"Thin carpet still disconnects the approach route");l.setBlock(at,Blocks.STONE.defaultBlockState(),2);h.assertFalse(OutingSites.feet(l,at),"Route accepts a blocked body cell");h.succeed();
    }
    @GameTest(template="empty") public static void toastPromptSwitchesFromMotherToFatherAndThenPlate(GameTestHelper h){
        var s=new VisitData.Scene();var n=new CompoundTag();n.putInt("Task",5);s.loop.event.put("DinnerActivities",n);
        h.assertTrue(DinnerActivities.hint(s).contains("Other Mother"),"Initial toast target is wrong");n.putInt("Toasts",2);
        h.assertTrue(DinnerActivities.hint(s).contains("Other Father"),"Mother toast asks for Mother again");n.putInt("Task",6);
        h.assertTrue(DinnerActivities.hint(s).contains("plate"),"Completed toast pair has no next task");h.succeed();
    }
    @GameTest(template="empty") public static void eligibleOrdinaryEventsCannotBeStarvedByRepeatedDraws(GameTestHelper h){
        var values=List.of("activity","dinner","delivery","away","outing");var last=new HashMap<String,Long>();String previous=null;var seen=new HashSet<String>();
        for(long sequence=1;sequence<=12;sequence++){var next=VisitEventSelection.chooseFair(values,x->true,previous,"activity",x->last.getOrDefault(x,0L),bound->0);h.assertFalse(next.equals(previous),"Event repeated consecutively");last.put(next,sequence);seen.add(next);previous=next;}
        h.assertTrue(seen.containsAll(values),"Adversarial draws starved eligible delivery/away/outing");h.succeed();
    }
    @GameTest(template="empty") public static void proposalParentsSitOnOppositeLongSidesBesideTheGift(GameTestHelper h){
        var s=new VisitData.Scene();s.table=BlockPos.ZERO;var mother=BlackIsTraditional.seat(s,1);var father=BlackIsTraditional.seat(s,2);
        h.assertTrue(mother.x<0&&father.x>0&&mother.z<0&&father.z<0,"Parents remain behind the gift lid at the opposite table end");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=240) public static void realGrabApproachesFromBesideTheCapturedChair(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(OtherWorld.KEY);var p=new ServerPlayer(l.getServer(),l,new GameProfile(UUID.randomUUID(),"GrabRegression"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",38);s.loop.phase=OtherMotherLoopState.Phase.BLACK_IS_TRADITIONAL;
        var n=FirstEscapeStory.data(s);n.putBoolean("Started",true);n.putString("Phase","COCOA_G");n.putLong("House",new BlockPos(109120,180,109120).asLong());VisitData.get(p.server).scenes.put(s.owner,s);
        var center=FirstEscapeScene.room(s);var chunks=new HashSet<net.minecraft.world.level.ChunkPos>();
        var realm=new RealmData.Realm();realm.door=s.door;realm.house=BlockPos.of(n.getLong("House"));realm.ready=true;RealmData.get(p.server).realms.put(s.door,realm);
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++){var at=center.offset(x,0,z);var chunk=new net.minecraft.world.level.ChunkPos(at);if(chunks.add(chunk))l.setChunkForced(chunk.x,chunk.z,true);l.setBlock(at.below(),Blocks.OAK_PLANKS.defaultBlockState(),2);l.setBlock(at,Blocks.BLUE_CARPET.defaultBlockState(),2);}
        var m=ModEntities.MOM.get().create(l);m.makeOtherParent(0);m.getPersistentData().putUUID("RealmDoor",s.door);m.getPersistentData().putUUID("LoopOwner",s.owner);m.setPos(Vec3.atBottomCenterOf(center.offset(-4,0,-2)).add(0,.0625,0));m.setNoAi(true);h.assertTrue(l.addFreshEntity(m),"Fixture mother rejected by realm generation protection");s.mom=m.getUUID();
        var chair=FirstEscapeScene.ensure(p,s,"BugChair",CinematicPropEntity.BUG_CHAIR,0,l,Vec3.atBottomCenterOf(center).add(0,.0625,0));chair.setYRot(90);p.setPos(chair.position());p.startRiding(chair,true);
        p.setYRot(73);p.setXRot(12);
        h.startSequence().thenExecuteFor(160,()->{FirstEscapeStory.drag(p,s);h.assertTrue(Math.abs(p.getXRot()-12)<.001&&Math.abs(p.getYRot()-73)<.001,"Carry forced the player's view instead of preserving free look");}).thenExecute(()->{
            try{h.assertTrue(n.getBoolean("Grabbed"),"Mother walked in place instead of grabbing: "+n.getString("Blocker")+" mother="+m.position()+" owner="+p.position()+" routeNode="+n.getInt("CarryNode"));h.assertTrue(p.getVehicle() instanceof CinematicPropEntity carrier&&carrier.kind()==CinematicPropEntity.GRAB_CARRIER,"Grab never transferred the real owner to the carrier");}
            finally{n.putBoolean("InternalTransfer",true);p.stopRiding();m.discard();for(String key:List.of("BugChair","Carrier")){var prop=FirstEscapeScene.prop(p,s,key);if(prop!=null)prop.discard();}VisitData.get(p.server).scenes.remove(s.owner);RealmData.get(p.server).realms.remove(s.door);for(var c:chunks)l.setChunkForced(c.x,c.z,false);}
        }).thenSucceed();
    }
}
