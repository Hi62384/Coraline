package com.coraline.secrets;

import com.coraline.feast.*;
import com.coraline.otherworld.*;
import com.coraline.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class HouseSpyGameTests {
    @GameTest(template="empty",batch="spy-report",timeoutTicks=160)
    public static void ratReportsThreeSecondSightingAfterFleeing(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);var origin=new BlockPos(14000,120,14000);
        for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++){level.setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);for(int y=0;y<4;y++)level.setBlock(origin.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);}
        var chunk=new net.minecraft.world.level.ChunkPos(origin);level.getChunkSource().addRegionTicket(TicketType.PORTAL,chunk,3,origin);
        var p=new ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SpyQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=UUID.randomUUID();scene.stage=VisitData.Stage.DONE;scene.loop.phase=OtherMotherLoopState.Phase.EXPLORING;scene.loop.secrets.visit=1;VisitData.get(p.server).scenes.put(p.getUUID(),scene);p.getPersistentData().putUUID("CoralineVisitDoor",scene.door);
        var realm=new RealmData.Realm();realm.door=scene.door;realm.doorPos=realm.sourceDoor=origin.north(200);realm.house=origin.north(200);realm.ready=true;RealmData.get(p.server).realms.put(scene.door,realm);
        var mother=ModEntities.MOM.get().create(level);mother.makeOtherParent(0);mother.setPos(origin.getX()+14.5,origin.getY(),origin.getZ()+.5);mother.setNoAi(true);level.addFreshEntity(mother);scene.mom=mother.getUUID();
        p.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+4.5);var sighting=p.blockPosition();SecretSpies.rat(p,origin);
        var rat=(MouseEntity)level.getEntity(net.minecraft.nbt.NbtUtils.loadUUID(scene.loop.secrets.world.getList("Rats",11).get(0)));
        for(int tick=1;tick<=59;tick++)h.runAtTickTime(tick,()->{rat.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);rat.setYRot(0);SecretSpies.mouse(rat,p);h.assertFalse(rat.getPersistentData().getBoolean("Alerted"),"Rat reported before three seconds");});
        h.runAtTickTime(60,()->{rat.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);rat.setYRot(0);SecretSpies.mouse(rat,p);h.assertTrue(rat.getPersistentData().getBoolean("Alerted"),"Rat didn't alert at three seconds");p.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+18.5);});
        for(int tick=61;tick<80;tick++)h.runAtTickTime(tick,()->{SecretSpies.mouse(rat,p);h.assertFalse(rat.isRemoved(),"Rat didn't get a fleeing interval");});
        h.runAtTickTime(80,()->{SecretSpies.mouse(rat,p);h.assertTrue(rat.isRemoved(),"Rat didn't disappear after fleeing");h.assertTrue(scene.loop.secrets.target.equals(sighting)&&scene.loop.secrets.search==SecretsState.Search.INVESTIGATING_NOISE,"Report follows current player position instead of the sighting");h.assertTrue(mother.distanceToSqr(p)>36,"Report teleported Mother beside player");p.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+204.5);MotherSearch.control(mother,p);h.assertTrue(MotherSearch.inSearchRadius(sighting,scene.loop.secrets.target),"Investigation followed the player after leaving the reported area");mother.discard();VisitData.get(p.server).scenes.remove(p.getUUID());RealmData.get(p.server).realms.remove(scene.door);h.succeed();});
    }
}
