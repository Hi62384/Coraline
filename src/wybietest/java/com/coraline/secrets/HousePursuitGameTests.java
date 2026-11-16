package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class HousePursuitGameTests {
    private static ServerPlayer fixture(ServerLevel level,BlockPos origin){
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PursuitQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=UUID.randomUUID();scene.stage=VisitData.Stage.DONE;scene.table=scene.piano=origin;scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;scene.loop.secrets.visit=1;
        VisitData.get(p.server).scenes.put(p.getUUID(),scene);p.getPersistentData().putUUID("CoralineVisitDoor",scene.door);
        var realm=new RealmData.Realm();realm.door=scene.door;realm.doorPos=realm.sourceDoor=origin;realm.house=origin;realm.porch=origin;realm.ready=true;RealmData.get(p.server).realms.put(scene.door,realm);return p;
    }
    private static void cleanup(ServerPlayer p){var scene=OtherMotherLoop.scene(p);RealmData.get(p.server).realms.remove(scene.door);VisitData.get(p.server).scenes.remove(p.getUUID());}
    @GameTest(template="empty",batch="pursuit-physics",timeoutTicks=240)
    public static void pursuitClimbsFullBlocksAndTurnsAroundWall(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var origin=new BlockPos(18000,120,18000);var chunk=new net.minecraft.world.level.ChunkPos(origin);
        for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}
        for(int x=-3;x<=12;x++)for(int z=-3;z<=12;z++)for(int y=-1;y<=7;y++)level.setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        for(int x=-3;x<=0;x++)for(int z=-2;z<=2;z++)level.setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
        for(int x=1;x<=3;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<x;y++)level.setBlock(origin.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
        for(int x=3;x<=11;x++)for(int z=-2;z<=11;z++)level.setBlock(origin.offset(x,2,z),Blocks.STONE.defaultBlockState(),2);
        for(int z=-2;z<=5;z++)for(int y=3;y<=6;y++)level.setBlock(origin.offset(6,y,z),Blocks.STONE.defaultBlockState(),2);
        var p=fixture(level,origin);p.setPos(Vec3.atBottomCenterOf(origin.offset(30,0,30)));var scene=OtherMotherLoop.scene(p);var goal=origin.offset(8,3,8);
        var mother=ModEntities.MOM.get().create(level);mother.makeOtherParent(0);mother.setPos(Vec3.atBottomCenterOf(origin));mother.getPersistentData().putLong("OtherHome",goal.asLong());
        h.runAtTickTime(40,()->{level.addFreshEntity(mother);scene.mom=mother.getUUID();scene.loop.secrets.search=SecretsState.Search.INVESTIGATING_NOISE;scene.loop.secrets.target=goal;scene.loop.secrets.world.putLong("SpyReportOrigin",goal.asLong());});
        final boolean[] reached={false};
        for(int tick=41;tick<=190;tick++)h.runAtTickTime(tick,()->{MotherSearch.control(mother,p);if(mother.getY()>=origin.getY()+2.9&&mother.distanceToSqr(Vec3.atBottomCenterOf(goal))<6)reached[0]=true;});
        h.runAtTickTime(191,()->{h.assertTrue(reached[0],"Pursuit stalled on full-block steps or the L-turn: "+mother.position());mother.discard();cleanup(p);for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++)level.setChunkForced(x,z,false);h.succeed();});
    }
    @GameTest(template="empty",batch="pursuit-rules",timeoutTicks=100)
    public static void forbiddenSightingChasesBeforeContactAndDollFacesForward(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var origin=new BlockPos(18500,120,18500);var p=fixture(level,origin);var scene=OtherMotherLoop.scene(p);
        for(int x=-2;x<=10;x++)for(int z=-2;z<=2;z++){level.setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<=3;y++)level.setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        var mother=ModEntities.MOM.get().create(level);mother.makeOtherParent(0);mother.setNoAi(true);mother.setNoGravity(true);mother.setPos(Vec3.atBottomCenterOf(origin));mother.setYHeadRot(-90);p.setPos(Vec3.atBottomCenterOf(origin.east(8)));
        var chunk=new net.minecraft.world.level.ChunkPos(origin);for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}
        h.runAtTickTime(40,()->{level.addFreshEntity(mother);scene.mom=mother.getUUID();});
        h.runAtTickTime(45,()->{
        h.assertTrue(MotherSearch.mother(p)==mother,"Native test entity didn't become available");
        MotherSearch.forbiddenAction(p,SecretDialogue.Event.RESTRICTED_ITEM_CAUGHT);
        h.assertTrue(scene.loop.secrets.search==SecretsState.Search.DIRECT_CHASE&&!Reconciliation.active(p)&&scene.loop.secrets.dialogue.getString("Queue").isEmpty(),"Detection scolded before a physical catch");
        MotherSearch.catchPlayer(p);h.assertFalse(Reconciliation.active(p),"Catch accepted from across the room");
        h.assertTrue(scene.loop.secrets.world.getString("ChaseOffense").equals("RESTRICTED_ITEM_CAUGHT"),"Witnessed offense wasn't remembered");
        MotherSearch.noise(p,origin,100,"cabinet noise");h.assertTrue(scene.loop.secrets.search==SecretsState.Search.DIRECT_CHASE,"A sound replaced the witnessed chase");
        var doll=net.minecraft.world.entity.EntityType.ITEM_DISPLAY.create(level);doll.setPos(Vec3.atCenterOf(origin));
        for(float yaw:new float[]{0,90,180,270}){doll.setYRot(yaw);var front=doll.position().subtract(doll.getLookAngle().scale(4));h.assertTrue(SecretSpies.inDollView(doll,front)&&!SecretSpies.inDollView(doll,doll.position().add(doll.getLookAngle().scale(4))),"Doll sees through its back at yaw "+yaw);}
        p.setPos(mother.position());MotherSearch.catchPlayer(p);h.assertTrue(scene.loop.secrets.world.getBoolean("CaughtEncounter")&&!scene.loop.secrets.world.contains("ChaseOffense"),"Physical catch didn't consume the remembered offense");mother.discard();cleanup(p);for(int x=chunk.x-1;x<=chunk.x+1;x++)for(int z=chunk.z-1;z<=chunk.z+1;z++)level.setChunkForced(x,z,false);h.succeed();});
    }
}
