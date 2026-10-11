package com.coraline.mixin;

import com.coraline.clothing.ArtifactCrossbowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public abstract class ArtifactCrossbowChargeMixin {
    @Inject(method="getChargeDuration",at=@At("HEAD"),cancellable=true)
    private static void coraline$chargeDuration(ItemStack stack,CallbackInfoReturnable<Integer> returnValue){
        int ticks=ArtifactCrossbowItem.chargeTicks(stack);
        if(ticks>0)returnValue.setReturnValue(ticks);
    }
}
