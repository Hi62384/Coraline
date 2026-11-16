package com.coraline.mixin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemEntity.class)
public abstract class ParentsBedLossMixin {
    @Inject(method="hurt",at=@At("RETURN")) private void coraline$lostBed(DamageSource source,float amount,CallbackInfoReturnable<Boolean> result){var item=(ItemEntity)(Object)this;if(result.getReturnValueZ()&&item.isRemoved())com.coraline.parents.ParentsBedSafety.destroyed(item);}
}
