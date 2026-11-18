package com.coraline.feast;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Previously failing recovery fixtures, isolated for focused native reruns. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostEscapeRecoveryGameTests {
    @GameTest(template="empty") public static void interceptionApproachStaysBesideTheRealDoor(GameTestHelper h){
        var level=h.getLevel();var door=h.absolutePos(new BlockPos(180,30,80));
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-2;x<=8;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=2;y++){
            var at=door.offset(x,y,z);level.getChunkAt(at);saved.put(at,level.getBlockState(at));level.setBlock(at,(y<0||x<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        }
        level.setBlock(door,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.EAST),2);
        var mother=com.coraline.registry.ModEntities.MOM.get().create(level);mother.makeOtherParent(0);mother.getPersistentData().putBoolean("CoralineDecoration",true);mother.setNoAi(true);mother.setPos(Vec3.atBottomCenterOf(door.east(6)));level.addFreshEntity(mother);
        mother.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.05,0));
        try{
            h.assertTrue(mother.onGround(),"Interception fixture did not settle on its supported floor");
            var before=mother.position();var target=GhostEscapeMother.interceptionApproach(mother,door);
            h.assertTrue(target!=null&&target.getX()>door.getX()&&target.getZ()!=door.getZ()&&target.distSqr(door)<=13,"Confrontation approach entered the tunnel or blocked the crawl line");
            h.assertTrue(mother.position().equals(before),"Choosing the confrontation position teleported Mother");
            for(int x=1;x<=3;x++)for(int z:new int[]{-2,-1,1,2})for(int y=0;y<=1;y++)level.setBlock(door.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
            h.assertTrue(GhostEscapeMother.interceptionApproach(mother,door)==null,"Obstructed confrontation invented an approach through the wall");
        }finally{mother.discard();saved.forEach((at,state)->level.setBlock(at,state,2));}h.succeed();
    }
    @GameTest(template="empty") public static void farewellTargetsTheRoomSideBeyondTheCompletionBoundary(GameTestHelper h){
        var level=h.getLevel();var door=h.absolutePos(new BlockPos(145,30,80));
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-3;x<=16;x++)for(int z=-9;z<=9;z++)for(int y=-1;y<=2;y++){
            var at=door.offset(x,y,z);level.getChunkAt(at);saved.put(at,level.getBlockState(at));
            level.setBlock(at,(y<0||x<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        }
        var child=com.coraline.tycoon.TycoonContent.NPC.get().create(level);child.setNoAi(true);child.setPos(Vec3.atBottomCenterOf(door.east(2)));level.addFreshEntity(child);
        child.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.05,0));
        try{
            h.assertTrue(child.onGround(),"Farewell fixture did not settle on its real floor");
            var before=child.position();var target=GhostEscapeRestoration.departureTarget(child,door,net.minecraft.core.Direction.EAST);
            h.assertTrue(target!=null&&target.getX()>door.getX()&&target.distSqr(door)>=64,"Farewell selected the blocked tunnel side or a target too close to finish");
            h.assertTrue(child.position().equals(before),"Selecting a departure target teleported the rescued child");
            var route=new CompoundTag();h.assertTrue(GhostVictimNavigation.walk(child,route,target,1,1,600,p->false)==GhostVictimNavigation.Result.MOVING,"Planning a farewell route fabricated departure");
        }finally{child.discard();saved.forEach((at,state)->level.setBlock(at,state,2));}h.succeed();
    }

    @GameTest(template="empty") public static void sharedEscapeMonitorExpiresThePhysicalAmbush(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());var n=session();
        n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putLong("ActiveTicks",6000);
        var child=com.coraline.tycoon.TycoonContent.NPC.get().create(p.serverLevel());
        child.owner=s.owner;child.resident=GhostVictimProgress.child(n).getUUID("Id");child.setPos(p.position());child.setNoAi(true);child.setNoGravity(true);
        n.putUUID("VictimActor",child.getUUID());var lease=new CompoundTag();lease.putUUID("Owner",s.owner);lease.putUUID("Door",s.door);lease.putUUID("Session",n.getUUID("Session"));
        child.getPersistentData().put(GhostVictimActors.LEASE,lease);p.serverLevel().addFreshEntity(child);
        var e=new CompoundTag();e.putUUID("Attempt",UUID.randomUUID());e.putUUID("Victim",child.resident);e.putUUID("Session",n.getUUID("Session"));
        e.putLong("AmbushStarted",0);e.putString("HidingSpot","test_cover");GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.WAIT_LURE,0);n.put(GhostEscapeProgress.KEY,e);
        var actual=child.position();
        try{
            GhostEscapeCovert.monitor(p,s,n);
            h.assertTrue(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.READY&&!e.contains("HidingSpot"),"Shared physical monitor left the expired ambush active");
            h.assertTrue(child.position().equals(actual)&&!e.getBoolean("KeyStolen"),"Timeout moved the child or awarded an unearned key");
            GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.WAIT_LURE,0);e.putLong("AmbushStarted",0);n.putUUID("Owner",UUID.randomUUID());
            GhostEscapeCovert.monitor(p,s,n);
            h.assertTrue(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.WAIT_LURE,"Foreign owner could advance the physical child's escape");
        }finally{child.discard();AcceptedBranchPlayers.remove(p,s);}
        h.succeed();
    }
    @GameTest(template="empty") public static void failedEscapeHandsOffWithoutReplacingAnUnloadedChild(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=VisitData.get(p.server).scenes.get(p.getUUID());var n=session();
        n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);UUID originalActor=UUID.randomUUID();n.putUUID("VictimActor",originalActor);
        AcceptedButtonEyes.data(s).put(GhostVictimProgress.KEY,n);
        var e=GhostEscapeProgress.data(n);e.putUUID("Attempt",UUID.randomUUID());e.putUUID("Victim",GhostVictimProgress.child(n).getUUID("Id"));e.putUUID("Session",n.getUUID("Session"));GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.FAILED_SEWING,0);
        var eye=RescueEyes.eye(s,0);UUID identity=eye.getUUID("Identity");eye.putUUID("GhostEscapeHolder",e.getUUID("Attempt"));
        try{
            h.assertTrue(GhostVictimActors.find(p,s,n)==null,"Fixture unexpectedly loaded the absent child");
            h.assertFalse(GhostEscapeCovert.tick(p,s,n),"Failed escape retained control instead of handing off to the sewing scene");
            h.assertTrue(e.getBoolean("FailureHandedOff")&&e.getBoolean("QuestEyesReturnedToBeldam")&&GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.VICTIM_CAPTURED,"Unloaded child prevented durable failure handoff");
            h.assertTrue(n.getUUID("VictimActor").equals(originalActor)&&GhostVictimActors.find(p,s,n)==null,"Failure recovery manufactured a replacement child");
            h.assertTrue(eye.getUUID("Identity").equals(identity)&&!eye.hasUUID("GhostEscapeHolder"),"Failure handoff lost canonical eye identity or left stale custody");
        }finally{AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }

    @GameTest(template="empty") public static void alreadyCrossedChildFinishesWithoutAnOverworldCopyOfTheOtherDoor(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);
        var s=OtherMotherLoop.scene(p);
        var level=p.serverLevel();
        var n=session();
        n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);
        AcceptedButtonEyes.data(s).put(GhostVictimProgress.KEY,n);
        var source=new com.coraline.otherworld.DoorData.Door();
        source.id=s.door;source.owner=s.owner;
        source.pos=h.absolutePos(new BlockPos(140,118,80));
        source.dimension=level.dimension().location().toString();
        var doors=com.coraline.otherworld.DoorData.get(p.server).doors;
        doors.put(s.door,source);
        var realm=new com.coraline.otherworld.RealmData.Realm();
        realm.door=s.door;realm.sourceDoor=source.pos;realm.doorPos=source.pos.offset(30,0,0);
        com.coraline.otherworld.RealmData.get(p.server).realms.put(s.door,realm);
        for(var at:BlockPos.betweenClosed(source.pos.offset(-5,-1,-2),source.pos.offset(4,2,2))){
            level.getChunkAt(at);
            level.setBlock(at,(at.getY()<source.pos.getY()?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        }
        level.setBlock(source.pos,com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState()
                .setValue(com.coraline.block.TinyDoorBlock.FACING,net.minecraft.core.Direction.EAST),2);
        var door=(com.coraline.otherworld.DoorBlockEntity)level.getBlockEntity(source.pos);
        door.doorId=s.door;
        var child=com.coraline.tycoon.TycoonContent.NPC.get().create(level);
        child.owner=s.owner;child.resident=GhostVictimProgress.child(n).getUUID("Id");
        child.setPos(Vec3.atBottomCenterOf(source.pos.east(2)));
        child.setOnGround(true);child.setNoAi(true);
        var lease=new CompoundTag();lease.putUUID("Owner",s.owner);lease.putUUID("Door",s.door);lease.putUUID("Session",n.getUUID("Session"));
        child.getPersistentData().put(GhostVictimActors.LEASE,lease);
        child.getPersistentData().putUUID("ShowTravelOwner",s.owner);
        child.getPersistentData().put("GhostEscapeChild",new CompoundTag());
        n.putUUID("VictimActor",child.getUUID());
        h.assertTrue(level.addFreshEntity(child),"Could not stage the saved Overworld child");
        var e=GhostEscapeProgress.data(n);
        e.putUUID("Attempt",UUID.randomUUID());e.putUUID("Session",n.getUUID("Session"));e.putUUID("Victim",child.resident);
        e.putBoolean("PlayerEyesHeld",true);
        GhostEscapeProgress.move(e,GhostEscapeProgress.Phase.CRAWL,0);
        GhostVictimActors.remember(n,child);
        h.succeedWhen(()->{
            h.assertTrue(GhostVictimActors.find(p,s,n)==child,"The original child is not visible in the loaded entity chunk");
            h.assertTrue(GhostEscapeHunt.tick(p,s,n),"The current crossing was not handled");
            h.assertTrue(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.SAFE&&e.contains("HomeLanding"),
                    "Already-crossed child could not commit room-side arrival: "+e.getString("Blocker"));
            h.assertTrue(level.getEntity(child.getUUID())==child,"Crossing reconciliation replaced the child");
            child.discard();com.coraline.otherworld.TunnelPassage.restore(level,door);
            com.coraline.otherworld.RealmData.get(p.server).realms.remove(s.door);
            AcceptedBranchPlayers.remove(p,s);
        });
    }

    @GameTest(template="empty") public static void authoredCabinetUsesSupportedSlotAndPreservesObstruction(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);
        var s=OtherMotherLoop.scene(p);
        var level=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        h.assertTrue(level!=null,"Other World is unavailable");
        var realms=com.coraline.otherworld.RealmData.get(p.server).realms;
        var realm=new com.coraline.otherworld.RealmData.Realm();
        realm.door=s.door;realm.house=h.absolutePos(new BlockPos(180,155,110));
        realms.put(s.door,realm);
        AcceptedButtonEyesProgress.scenery(s).putLong("House",realm.house.asLong());
        var n=session();
        n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);
        var cabinet=GhostVictimWorld.at(s,48,15,64);
        var door=GhostEscapeRooms.door(s,"storage");
        var child=com.coraline.tycoon.TycoonContent.NPC.get().create(level);
        child.owner=s.owner;child.resident=GhostVictimProgress.child(n).getUUID("Id");
        var lease=new CompoundTag();lease.putUUID("Owner",s.owner);lease.putUUID("Door",s.door);lease.putUUID("Session",n.getUUID("Session"));
        child.getPersistentData().put(GhostVictimActors.LEASE,lease);
        n.putUUID("VictimActor",child.getUUID());
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        try{
            for(var cell:BlockPos.betweenClosed(cabinet.offset(-1,-1,-1),cabinet.offset(1,2,1))){
                var at=cell.immutable();level.getChunkAt(at);saved.put(at,level.getBlockState(at));
                level.setBlock(at,(at.getY()<cabinet.getY()?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
            }
            level.getChunkAt(door);
            saved.put(door,level.getBlockState(door));
            level.setBlock(door,Blocks.OAK_DOOR.defaultBlockState(),2);
            child.setPos(Vec3.atBottomCenterOf(cabinet.east()));
            level.setBlock(cabinet,Blocks.OBSIDIAN.defaultBlockState(),2);
            h.assertFalse(GhostEscapeRooms.prepare(p,s,n,child).contains("storage"),"An obstructed slot was accepted");
            h.assertTrue(level.getBlockState(cabinet).is(Blocks.OBSIDIAN),"Preparation overwrote the obstruction");
            level.setBlock(cabinet,Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(GhostEscapeRooms.prepare(p,s,n,child).contains("storage"),"Supported original cabinet slot did not become searchable");
            h.assertTrue(cabinet.equals(GhostEscapeRooms.container(n,"storage")),"The cabinet was created outside its authored slot");
        }finally{
            child.discard();
            saved.forEach((at,state)->level.setBlock(at,state,2));
            realms.remove(s.door);
            AcceptedBranchPlayers.remove(p,s);
        }
        h.succeed();
    }

    private static CompoundTag session(){
        var n=new CompoundTag();GhostVictimProgress.begin(n,"ACCEPTED_PROPOSAL");n.putUUID("Session",UUID.randomUUID());
        GhostVictimProgress.leavePrison(n);var c=new CompoundTag();c.putString("Name","Robin");
        GhostVictimProgress.newVictim(n,UUID.randomUUID(),c);return n;
    }
}
