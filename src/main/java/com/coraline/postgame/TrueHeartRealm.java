package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;

/** A generation-owned empty native level, separate from procedural chambers
 * and ordinary Minecraft terrain. Old level folders are never deleted. */
final class TrueHeartRealm {
    static final ResourceLocation TYPE=ResourceLocation.fromNamespaceAndPath("coraline","true_heart");
    private static final String PREFIX="true_heart/";
    static ResourceKey<Level> key(UUID generation){return ResourceKey.create(Registries.DIMENSION,ResourceLocation.fromNamespaceAndPath("coraline",PREFIX+generation));}
    static boolean isHeart(Level level){var id=level.dimension().location();return id.getNamespace().equals("coraline")&&id.getPath().startsWith(PREFIX);}
    static ServerLevel ensure(MinecraftServer server,UUID generation){
        if(!server.isSameThread()||!InfiniteWorldData.get(server).matchesGeneration(generation))return null;var existing=server.getLevel(key(generation));if(existing!=null)return existing;
        ServerLevel created=null;try{var ops=RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,server.registryAccess());net.minecraft.world.level.chunk.ChunkGenerator generator;
            try(var reader=server.getResourceManager().getResourceOrThrow(ResourceLocation.fromNamespaceAndPath("coraline","postgame/heart_terrain.json")).openAsReader()){generator=net.minecraft.world.level.chunk.ChunkGenerator.CODEC.parse(ops,com.google.gson.JsonParser.parseReader(reader)).getOrThrow(false,message->{});}
            var type=server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DIMENSION_TYPE,TYPE));var access=(com.coraline.mixin.InfiniteServerAccess)server;
            var progress=new net.minecraft.server.level.progress.ChunkProgressListener(){public void updateSpawnPos(net.minecraft.world.level.ChunkPos pos){}public void onStatusChange(net.minecraft.world.level.ChunkPos pos,net.minecraft.world.level.chunk.ChunkStatus status){}public void start(){}public void stop(){}};
            created=new ServerLevel(server,access.coraline$infiniteExecutor(),access.coraline$infiniteStorage(),new DerivedLevelData(server.getWorldData(),(net.minecraft.world.level.storage.ServerLevelData)server.overworld().getLevelData()),key(generation),new LevelStem(type,generator),progress,false,InfiniteWorldData.get(server).state().seed().orElseThrow(),List.of(),false,null);
            server.forgeGetWorldMap().put(key(generation),created);server.markWorldsDirty();net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.LevelEvent.Load(created));return created;
        }catch(Exception failed){if(created!=null){server.forgeGetWorldMap().remove(key(generation),created);server.markWorldsDirty();try{created.close();}catch(java.io.IOException ignored){}}com.mojang.logging.LogUtils.getLogger().error("The true Heart could not be opened",failed);return null;}
    }
    static boolean leave(ServerPlayer player){var palace=InfiniteRealm.palaceDoor(player.server).orElse(null);if(palace==null)palace=player.server.overworld().getSharedSpawnPos();com.coraline.feast.LoopProps.hold(player.server.overworld(),palace);var at=com.coraline.tycoon.TycoonWorld.landing(player.server.overworld(),palace.east(4));if(at==null)return false;player.stopRiding();player.stopUsingItem();player.setCamera(player);player.closeContainer();player.teleportTo(player.server.overworld(),at.getX()+.5,at.getY(),at.getZ()+.5,player.getYRot(),player.getXRot());player.fallDistance=0;return player.level()==player.server.overworld();}
    private TrueHeartRealm(){}
}
