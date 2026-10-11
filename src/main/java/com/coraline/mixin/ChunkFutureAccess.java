package com.coraline.mixin;

import com.mojang.datafixers.util.Either;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerChunkCache.class)
public interface ChunkFutureAccess {
    /** Main-thread-only request, unlike getChunkFuture which managed-blocks on that thread. */
    @Invoker("getChunkFutureMainThread")
    CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>> coraline$requestChunk(int x,int z,ChunkStatus status,boolean create);
}
