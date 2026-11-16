package com.coraline.mixin;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractContainerMenu.class)
public abstract class DeliveryContainerMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void coraline$keepSealedParcel(int slot,int button,ClickType type,Player player,CallbackInfo ci){if(com.coraline.feast.SpecialDelivery.blockedContainerClick((AbstractContainerMenu)(Object)this,slot,button,type,player))ci.cancel();}
}
