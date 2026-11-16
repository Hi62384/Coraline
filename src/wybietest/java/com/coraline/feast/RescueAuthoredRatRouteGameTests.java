package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Real authored attic, exterior flights, main landing and basement; no substitute ramps. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueAuthoredRatRouteGameTests {
    @GameTest(template="empty",timeoutTicks=3600) public static void sameMountedRatTraversesTheAuthoredAtticToBasementRoute(GameTestHelper h){
        var l=h.getLevel().getServer().getLevel(com.coraline.otherworld.OtherWorld.KEY);var p=new ServerPlayer(l.getServer(),l,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"AuthoredRatQA"));p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(p.server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
        var s=new VisitData.Scene();s.owner=p.getUUID();s.door=java.util.UUID.randomUUID();s.stage=VisitData.Stage.DONE;s.loop.event.putInt("Serial",5);VisitData.get(p.server).scenes.put(s.owner,s);var parent=ParentRescueStory.data(s);parent.putBoolean("Started",true);var scenery=new net.minecraft.nbt.CompoundTag();scenery.putLong("House",new BlockPos(108000,180,108000).asLong());parent.put("Scenery",scenery);RescueChapter.data(s).putBoolean("Started",true);
        var changed=new java.util.ArrayList<BlockPos>();var forced=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        try(var raw=RescueAuthoredRatRouteGameTests.class.getResourceAsStream("/assets/coraline/structures/rosewood_house.json.gz");var zipped=new java.util.zip.GZIPInputStream(java.util.Objects.requireNonNull(raw));var reader=new java.io.InputStreamReader(zipped,java.nio.charset.StandardCharsets.UTF_8)){
            var source=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();var palette=source.getAsJsonArray("palette");
            for(var element:source.getAsJsonArray("blocks")){var cell=element.getAsJsonArray();int x=cell.get(0).getAsInt(),y=cell.get(1).getAsInt(),z=cell.get(2).getAsInt();if(x<4||x>35||y< -2||y>28||z< -2||z>68)continue;var entry=palette.get(cell.get(3).getAsInt()).getAsJsonObject();var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation(entry.get("id").getAsString()));var state=java.util.Objects.requireNonNull(block).defaultBlockState();for(var property:entry.getAsJsonObject("properties").entrySet())state=property(state,property.getKey(),property.getValue().getAsString());var at=FirstEscapeScene.at(s,x,y,z);l.getChunkAt(at);l.setBlock(at,state,2);changed.add(at);var chunk=new net.minecraft.world.level.ChunkPos(at);if(!l.getForcedChunks().contains(chunk.toLong()))forced.add(chunk);}
        }catch(java.io.IOException error){throw new IllegalStateException(error);}
        for(var chunk:forced)l.setChunkForced(chunk.x,chunk.z,true);
        // These are exactly the production chase waypoints, not inferred safe destinations.
        BlockPos[] route={FirstEscapeScene.at(s,7,24,60),FirstEscapeScene.at(s,7,15,52),FirstEscapeScene.at(s,7,15,18),FirstEscapeScene.at(s,20,15,10),FirstEscapeScene.at(s,26,6,10),FirstEscapeScene.at(s,26,4,8),FirstEscapeScene.at(s,26,2,4),FirstEscapeScene.at(s,26,0,0),FirstEscapeScene.at(s,26,-1,11)};
        FirstEscapeStory.setDoor(p,FirstEscapeScene.at(s,9,24,65),true);FirstEscapeStory.setDoor(p,FirstEscapeScene.at(s,26,-1,12),true);FirstEscapeStory.setDoor(p,FirstEscapeScene.at(s,27,-1,12),true);
        var start=FirstEscapeScene.at(s,32,24,23);var rat=com.coraline.registry.ModEntities.MOUSE.get().create(l);rat.doorId=s.door;rat.circusStyle(6);rat.spying(true);rat.setInvulnerable(true);rat.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE).setBaseValue(80);rat.getPersistentData().putBoolean("CoralineDecoration",true);rat.getPersistentData().putUUID("RescueOwner",s.owner);rat.refreshDimensions();rat.setNoAi(true);rat.setPos(Vec3.atBottomCenterOf(start));l.addFreshEntity(rat);var identity=rat.getUUID();int[] ticks={0},node={0};
        Runnable cleanup=()->{RescueProps.retire(p,s,"RatBall2");rat.discard();VisitData.get(p.server).scenes.remove(s.owner);for(var at:changed)l.removeBlock(at,false);for(var chunk:forced)l.setChunkForced(chunk.x,chunk.z,false);};
        h.onEachTick(()->{if(++ticks[0]<25)return;var target=route[node[0]];p.setPos(rat.position().add(0,0,3));RescueEncounters.rollRat(p,s,rat,target,ticks[0],2);var ball=RescueProps.find(p,s,"RatBall2");if(ball!=null){h.assertTrue(identity.equals(rat.getUUID())&&rat.getVehicle()==ball,"Chase replaced the rat or detached its ball");h.assertFalse(l.getBlockCollisions(ball,ball.getBoundingBox().deflate(.001)).iterator().hasNext(),"Mounted assembly entered authored stairs/walls at node "+node[0]+": "+ball.position());if(rat.distanceToSqr(target.getCenter())<5){if(++node[0]==route.length){cleanup.run();h.succeed();return;}}}
            if(!RescueChapter.data(s).getString("Blocker").isEmpty()||ticks[0]>3450){var n=RescueChapter.data(s);var path=n.getLongArray("RatPath");int index=n.getInt("RatPathNode");var next=index<path.length?BlockPos.of(path[index]):target;String detail="node="+node[0]+" target="+target+" rat="+rat.position()+" ball="+(ball==null?"missing":ball.position()+" velocity="+ball.getDeltaMovement()+" grounded="+ball.onGround())+" path="+index+" next="+next+" blocks="+l.getBlockState(next.below())+" / "+l.getBlockState(next)+" / "+l.getBlockState(next.above())+" / "+n.getString("Blocker");cleanup.run();h.fail("Actual attic/basement chase stalled: "+detail);}
        });
    }
    private static <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState property(net.minecraft.world.level.block.state.BlockState state,String key,String value){@SuppressWarnings("unchecked") var property=(net.minecraft.world.level.block.state.properties.Property<T>)state.getBlock().getStateDefinition().getProperty(key);return property==null?state:property.getValue(value).map(parsed->state.setValue(property,parsed)).orElse(state);}
    private RescueAuthoredRatRouteGameTests(){}
}
