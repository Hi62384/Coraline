package com.coraline.postgame;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;

/** Registers serializers only. No premature dimension or portal is created. */
@Mod.EventBusSubscriber(modid="coraline")
public final class InfiniteWorldGeneration {
    private static final DeferredRegister<Codec<? extends BiomeSource>> BIOMES=DeferredRegister.create(Registries.BIOME_SOURCE,"coraline");
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,"coraline");
    static{BIOMES.register("infinite_climate",()->InfiniteBiomeSource.CODEC);GENERATORS.register("infinite_other_world",()->InfiniteChunkGenerator.CODEC);}
    public static void register(IEventBus bus){BIOMES.register(bus);GENERATORS.register(bus);InfiniteSiteStructures.register(bus);}
    public static void bindAvailable(MinecraftServer server){for(var level:server.getAllLevels())if(level.getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator)generator.bind(server);}
    @SubscribeEvent public static void started(ServerStartedEvent event){bindAvailable(event.getServer());}
    private InfiniteWorldGeneration(){}
}
