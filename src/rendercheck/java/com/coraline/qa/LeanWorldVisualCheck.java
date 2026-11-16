package com.coraline.qa;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** QA sources only: do not preload 441 unrelated spawn chunks before a scene test. */
@Mixin(MinecraftServer.class)
public abstract class LeanWorldVisualCheck {
    @Inject(method="prepareLevels",at=@At("HEAD"),cancellable=true)
    private void coraline$boundedQaStartup(ChunkProgressListener progress,CallbackInfo callback){
        if(!Boolean.getBoolean("coraline.leanVisualWorld"))return;
        var server=(MinecraftServer)(Object)this;
        progress.updateSpawnPos(new ChunkPos(server.overworld().getSharedSpawnPos()));
        progress.stop();
        com.mojang.logging.LogUtils.getLogger().info("LEAN_QA_STARTUP: spawn preload omitted; player chunks and all scene logic remain active");
        callback.cancel();
    }
}
