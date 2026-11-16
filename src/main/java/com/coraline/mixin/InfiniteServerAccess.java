package com.coraline.mixin;

import java.util.concurrent.Executor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Construct an epoch-bound level only after its independent seed is saved. */
@Mixin(MinecraftServer.class)
public interface InfiniteServerAccess {
    @Accessor("storageSource") LevelStorageSource.LevelStorageAccess coraline$infiniteStorage();
    @Accessor("executor") Executor coraline$infiniteExecutor();
}
