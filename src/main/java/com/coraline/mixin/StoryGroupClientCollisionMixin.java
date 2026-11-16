package com.coraline.mixin;

import com.coraline.client.StoryGroupView;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class StoryGroupClientCollisionMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void coraline$pairPush(Entity other,CallbackInfo ci){var self=(Entity)(Object)this;if(self.level().isClientSide&&self instanceof Player&&other instanceof Player&&StoryGroupView.pair(self.getUUID(),other.getUUID()))ci.cancel();}
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void coraline$pairCollision(Entity other,CallbackInfoReturnable<Boolean> ci){var self=(Entity)(Object)this;if(self.level().isClientSide&&self instanceof Player&&other instanceof Player&&StoryGroupView.pair(self.getUUID(),other.getUUID()))ci.setReturnValue(false);}
}
