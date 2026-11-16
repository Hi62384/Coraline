package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FamilyOutingBoatGameTests {
    @GameTest(template="empty",batch="outing-boat-recovery",timeoutTicks=120)
    public static void missingBoatRepairDoesNotRemountVoluntaryDismount(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);
        var center=new BlockPos(45200,130,45200);var shore=center.offset(0,1,-11);
        var forced=new HashSet<Long>();
        for(int cx=(center.getX()-12)>>4;cx<=(center.getX()+12)>>4;cx++)for(int cz=(center.getZ()-12)>>4;cz<=(center.getZ()+12)>>4;cz++){
            long key=ChunkPos.asLong(cx,cz);if(!level.getForcedChunks().contains(key)){level.setChunkForced(cx,cz,true);forced.add(key);}
        }
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BoatRepairQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        p.setPos(Vec3.atBottomCenterOf(shore));
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.table=shore;s.piano=shore.east(20);s.loop.visits=2;s.loop.secrets.visit=2;
        VisitData.get(p.server).scenes.put(s.owner,s);p.getPersistentData().putUUID("CoralineVisitDoor",s.door);
        var r=new RealmData.Realm();r.door=s.door;r.house=shore;r.porch=shore;r.table=shore;r.piano=s.piano;r.doorPos=shore;r.sourceDoor=center;r.ready=true;r.radius=250;r.islandComplete=true;
        RealmData.get(p.server).realms.put(s.door,r);
        var water=new ArrayList<Vec3>();
        for(int i=0;i<24;i++){double a=i*Math.PI*2/24;water.add(new Vec3(center.getX()+.5+Math.cos(a)*6,center.getY()+.85,center.getZ()+.5+Math.sin(a)*4));}
        var site=new OutingSites.Site(shore,List.of(shore),List.of(),List.copyOf(water));
        s.loop.event.putString("Type",VisitEvents.Type.FAMILY_OUTING.name());s.loop.event.putInt("Serial",1);s.loop.event.putString("OutingSubtype",FamilyOuting.Subtype.BOAT_RIDE.name());s.loop.event.put("OutingSite",site.save());s.loop.event.putInt("OutingElapsed",1800);s.loop.event.putBoolean("OutingPending",true);s.loop.phase=OtherMotherLoopState.Phase.OUTING_ACTIVE;
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){level.setBlock(center.offset(x,-2,z),Blocks.STONE.defaultBlockState(),2);for(int y=-1;y<=0;y++)level.setBlock(center.offset(x,y,z),Blocks.WATER.defaultBlockState(),2);for(int y=1;y<=4;y++)level.setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        for(int x=-3;x<=3;x++)for(int z=-1;z<=1;z++)level.setBlock(shore.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        h.runAtTickTime(40,()->{
            var mom=ModEntities.MOM.get().create(level);var dad=ModEntities.DAD.get().create(level);
            for(var actor:List.of(mom,dad)){actor.makeOtherParent(0);actor.getPersistentData().putUUID("RealmDoor",s.door);actor.setPos(Vec3.atBottomCenterOf(shore));level.addFreshEntity(actor);}
            mom.getPersistentData().putUUID("LoopOwner",p.getUUID());s.mom=mom.getUUID();s.dad=dad.getUUID();
            // The fixture is remote from the GameTest player. Let the forced
            // entity chunks publish the actual parent UUIDs before boarding.
            h.runAtTickTime(80,()->{
            h.assertTrue(level.getEntity(s.mom)==mom&&level.getEntity(s.dad)==dad,"Forced parent entity chunks not ready");
            FamilyOuting.board(p,s);
            h.assertTrue(FamilyOuting.boats(p,s).size()==2&&p.getVehicle() instanceof Boat,"Initial join did not create and board two real boats");
            p.stopRiding();p.setPos(Vec3.atBottomCenterOf(shore));
            var missing=FamilyOuting.boats(p,s).stream().filter(b->b.getPersistentData().getInt("OutingBoat")==0).findFirst().orElseThrow();missing.discard();
            FamilyOuting.board(p,s,false);
            h.assertTrue(FamilyOuting.boats(p,s).size()==2,"Missing boat was not repaired");
            h.assertTrue(p.getVehicle()==null,"Boat recovery forcibly remounted an owner who chose to dismount");
            for(var boat:FamilyOuting.boats(p,s)){int slot=boat.getPersistentData().getInt("OutingBoat");h.assertTrue(boat.position().distanceToSqr(FamilyOuting.boatTarget(site,slot,1800,FamilyOuting.definition(s).duration()))<.0001,"Repair reset boat route progress");}
            s.loop.event.putInt("BoatAway",200);FamilyOuting.sail(p,s,1801);
            h.assertTrue(s.loop.event.getBoolean("OutingFailed"),"Abandoned boat ride did not record cancellation");
            h.assertTrue(s.loop.event.getInt("OutingElapsed")==1800&&!s.loop.event.getBoolean("CompletionAwarded"),"Boat failure fabricated elapsed completion or rewards");
            h.assertTrue(FamilyOuting.boats(p,s).isEmpty(),"Failed boat ride leaked boats");
            FamilyOuting.cleanup(p,s);mom.discard();dad.discard();RealmData.get(p.server).realms.remove(s.door);VisitData.get(p.server).scenes.remove(s.owner);
            for(long key:forced){var chunk=new ChunkPos(key);level.setChunkForced(chunk.x,chunk.z,false);}
            h.succeed();
            });
        });
    }
    private FamilyOutingBoatGameTests(){}
}
