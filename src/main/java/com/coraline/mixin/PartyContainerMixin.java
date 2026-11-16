package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Covers chest/barrel/shulker/hopper/dispenser, furnaces and brewing stands at their actual inventory. */
@Mixin({RandomizableContainerBlockEntity.class,AbstractFurnaceBlockEntity.class,BrewingStandBlockEntity.class})
public abstract class PartyContainerMixin implements PartyStorageGuard.GuardedContainer {
    @Inject(method="removeItem",at=@At("HEAD"),cancellable=true) private void coraline$remove(int slot,int amount,CallbackInfoReturnable<ItemStack> ci){if(PartyStorageGuard.protects(this))ci.setReturnValue(ItemStack.EMPTY);}
    @Inject(method="removeItemNoUpdate",at=@At("HEAD"),cancellable=true) private void coraline$removeNoUpdate(int slot,CallbackInfoReturnable<ItemStack> ci){if(PartyStorageGuard.protects(this))ci.setReturnValue(ItemStack.EMPTY);}
    @Inject(method="setItem",at=@At("HEAD"),cancellable=true) private void coraline$set(int slot,ItemStack stack,CallbackInfo ci){if(PartyStorageGuard.protects(this))ci.cancel();}
    @Inject(method="getItem",at=@At("RETURN"),cancellable=true) private void coraline$read(int slot,CallbackInfoReturnable<ItemStack> ci){if(PartyStorageGuard.protects(this))ci.setReturnValue(ci.getReturnValue().copy());}
    @Inject(method="clearContent",at=@At("HEAD"),cancellable=true) private void coraline$clear(CallbackInfo ci){if(PartyStorageGuard.protects(this))ci.cancel();}
}
