package com.coraline.mixin;

import com.coraline.feast.AcceptedButtonEyesInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class AcceptedInventoryDropMixin {
    @Inject(method="drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",at=@At("HEAD"),cancellable=true)
    private void coraline$acceptedEscrow(ItemStack stack,boolean scatter,boolean trace,CallbackInfoReturnable<ItemEntity> result){
        if((Object)this instanceof ServerPlayer p&&(AcceptedButtonEyesInventory.captureOverflow(p,stack)||AcceptedButtonEyesInventory.locked(p)))result.setReturnValue(null);
    }
}
