package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Guards both simulation and real extraction, including capabilities cached before party activation. */
@Mixin(targets={"net.minecraftforge.items.wrapper.InvWrapper","net.minecraftforge.items.wrapper.SidedInvWrapper","net.minecraftforge.items.ItemStackHandler"},remap=false)
public abstract class PartyHandlerMixin implements PartyStorageGuard.GuardedHandler {
    @Inject(method="extractItem",at=@At("HEAD"),cancellable=true) private void coraline$extract(int slot,int amount,boolean simulate,CallbackInfoReturnable<ItemStack> ci){if(PartyStorageGuard.protects(this))ci.setReturnValue(ItemStack.EMPTY);}
    @Inject(method="getStackInSlot",at=@At("RETURN"),cancellable=true) private void coraline$read(int slot,CallbackInfoReturnable<ItemStack> ci){if(PartyStorageGuard.protects(this))ci.setReturnValue(ci.getReturnValue().copy());}
    @Inject(method="setStackInSlot",at=@At("HEAD"),cancellable=true) private void coraline$set(int slot,ItemStack stack,CallbackInfo ci){if(PartyStorageGuard.protects(this))ci.cancel();}
    @Inject(method="insertItem",at=@At("HEAD"),cancellable=true) private void coraline$insert(int slot,ItemStack stack,boolean simulate,CallbackInfoReturnable<ItemStack> ci){if(PartyStorageGuard.protects(this))ci.setReturnValue(stack);}
}
