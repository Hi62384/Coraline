package com.coraline.postgame;

import java.util.*;
import net.minecraft.server.MinecraftServer;

/** Immutable generator binding obtained once after durable seed readback.
 * It must not be swapped on an existing dimension during complete reset. */
public final class InfiniteGeneration {
    private final UUID id;
    private final long seed;
    private final InfiniteTerrain terrain;
    InfiniteGeneration(UUID id,long seed){this.id=Objects.requireNonNull(id);this.seed=seed;this.terrain=new InfiniteTerrain(seed);}
    public UUID id(){return id;}
    public long seed(){return seed;}
    public InfiniteTerrain terrain(){return terrain;}
    public static Optional<InfiniteGeneration> checkpoint(MinecraftServer server){
        var data=InfiniteWorldData.get(server);var seed=data.checkpointSeed(server);var state=data.state();
        if(seed.isEmpty()||state.generation().isEmpty()||state.seed().isEmpty()||state.seed().getAsLong()!=seed.getAsLong())return Optional.empty();
        return Optional.of(new InfiniteGeneration(state.generation().orElseThrow(),seed.getAsLong()));
    }
}
