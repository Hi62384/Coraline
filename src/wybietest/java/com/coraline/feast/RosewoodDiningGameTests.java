package com.coraline.feast;
import com.coraline.otherworld.OtherWorld;
import com.coraline.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RosewoodDiningGameTests {
    @GameTest(template="empty",batch="dining",timeoutTicks=200) public static void repeatDiningChairsStayAliveAndKeepTheirIdentity(GameTestHelper h){
        var level=h.getLevel().getServer().getLevel(OtherWorld.KEY);h.assertTrue(level!=null,"Other World unavailable");
        var at=new BlockPos(1800,95,1800);level.getChunkAt(at);
        var p=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"DiningQA"));
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);p.setPos(at.getX(),at.getY(),at.getZ());
        var scene=new VisitData.Scene();scene.owner=p.getUUID();scene.door=UUID.randomUUID();scene.table=scene.piano=at;scene.stage=VisitData.Stage.DONE;scene.loop.phase=OtherMotherLoopState.Phase.GIFTS;scene.loop.generation=42;scene.loop.giftPrepared=true;
        VisitData.get(p.server).scenes.put(scene.owner,scene);LoopProps.hold(level,at);
        var chunk=new net.minecraft.world.level.ChunkPos(at);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setChunkForced(chunk.x+x,chunk.z+z,true);
        h.runAfterDelay(40,()->{LoopProps.ensure(p,scene);h.runAfterDelay(5,()->{
        var chairs=LoopProps.nearby(level,scene,at).stream().filter(e->e.kind()==ScenePropEntity.CHAIR).toList();h.assertTrue(chairs.size()==9,"Long dining table needs nine stable chairs; got "+chairs.size());
        var ids=chairs.stream().map(ScenePropEntity::getUUID).collect(java.util.stream.Collectors.toSet());
        for(var chair:chairs)h.assertTrue(LoopProps.owned(chair)&&LoopProps.valid(chair),"Repeat chair has first-visit ownership");
        var table=LoopProps.nearby(level,scene,at).stream().filter(e->e.kind()==ScenePropEntity.TABLE).findFirst().orElseThrow();h.assertTrue(table.longDining()&&Math.abs(table.getBoundingBox().getZsize()-9)<.001,"Dining table hitbox isn't extended");
        var saved=table.saveWithoutId(new net.minecraft.nbt.CompoundTag());saved.putUUID("UUID",UUID.randomUUID());var restored=ModEntities.SCENE_PROP.get().create(level);restored.load(saved);h.assertTrue(restored.longDining()&&Math.abs(restored.getBoundingBox().getZsize()-9)<.001,"Long table didn't survive serialization");restored.discard();
        h.runAfterDelay(60,()->{
            for(int i=0;i<10;i++)LoopProps.ensure(p,scene);
            var after=LoopProps.nearby(level,scene,at).stream().filter(e->e.kind()==ScenePropEntity.CHAIR).toList();
            h.assertTrue(after.size()==9&&after.stream().map(ScenePropEntity::getUUID).collect(java.util.stream.Collectors.toSet()).equals(ids),"Chairs were discarded/recreated or duplicated");
            for(var chair:after)h.assertTrue(!chair.isRemoved()&&chair.position().distanceToSqr(SupperSeating.position(at,chair.slot))<.001,"A dining chair moved unexpectedly");
            LoopProps.nearby(level,scene,at).forEach(ScenePropEntity::discard);VisitData.get(p.server).scenes.remove(scene.owner);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.setChunkForced(chunk.x+x,chunk.z+z,false);h.succeed();
        });});});
    }
}
