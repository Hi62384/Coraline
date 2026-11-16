package com.coraline.mixin;

import com.coraline.multiplayer.StoryCoop;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class StoryGroupCollisionMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void coraline$pairPush(Entity other,CallbackInfo ci){if((Object)this instanceof ServerPlayer a&&other instanceof ServerPlayer b&&StoryCoop.pair(a,b))ci.cancel();}
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void coraline$pairCollision(Entity other,CallbackInfoReturnable<Boolean> ci){if((Object)this instanceof ServerPlayer a&&other instanceof ServerPlayer b&&StoryCoop.pair(a,b))ci.setReturnValue(false);}
}
