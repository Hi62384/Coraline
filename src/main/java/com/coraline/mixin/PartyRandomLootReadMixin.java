package com.coraline.mixin;
import com.coraline.feast.PartyStorageGuard;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Do not trigger lazy loot generation while setItem is protected: that could consume a loot table into rejected writes. */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class PartyRandomLootReadMixin {
    @Shadow protected abstract NonNullList<ItemStack> getItems();
    @Inject(method="getItem",at=@At("HEAD"),cancellable=true) private void coraline$peek(int slot,CallbackInfoReturnable<ItemStack> ci){if(PartyStorageGuard.protects(this))ci.setReturnValue(getItems().get(slot).copy());}
}
