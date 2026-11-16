package com.coraline.mixin;

import com.coraline.clothing.PerfectPower;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Neutralize legacy/command-added Mending before XP can repair it. */
@Mixin(EnchantmentHelper.class)
public abstract class PerfectMendingMixin {
    @Inject(method="getItemEnchantmentLevel",at=@At("HEAD"),cancellable=true)
    private static void coraline$noPerfectMending(Enchantment enchant,ItemStack stack,CallbackInfoReturnable<Integer> result){if(enchant==Enchantments.MENDING&&PerfectPower.equipment(stack))result.setReturnValue(0);}
}
