package com.coraline.mixin;

import com.coraline.clothing.RapierCombat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Player.class)
public abstract class RapierPrimaryHurtMixin {
    @Redirect(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),require=1)
    private boolean coraline$rapierPrimaryStrike(Entity target,DamageSource source,float amount) {
        return RapierCombat.primaryHurt((Player)(Object)this,target,source,amount);
    }
}
