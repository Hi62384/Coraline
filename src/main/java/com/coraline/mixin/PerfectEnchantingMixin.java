package com.coraline.mixin;

import com.coraline.clothing.PerfectPower;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public abstract class PerfectEnchantingMixin {
    @Inject(method="canEnchant",at=@At("HEAD"),cancellable=true)
    private void coraline$denyPerfectMending(ItemStack stack,CallbackInfoReturnable<Boolean> result){if((Object)this==Enchantments.MENDING&&PerfectPower.equipment(stack))result.setReturnValue(false);}
}
