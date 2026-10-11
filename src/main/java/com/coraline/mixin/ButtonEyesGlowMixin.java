package com.coraline.mixin;

import com.coraline.clothing.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class ButtonEyesGlowMixin {
    @Inject(method="shouldEntityAppearGlowing",at=@At("RETURN"),cancellable=true)
    private void coraline$outline(Entity entity,CallbackInfoReturnable<Boolean> ci){
        var player=Minecraft.getInstance().player;
        if(player!=null&&entity instanceof Mob&&entity.isAlive()&&ClothingEffects.wearing(player,ClothingItem.Style.UNBINDING_BUTTON_EYES)&&(ClothingConfig.HIGHLIGHT_RANGE.get()==0||entity.distanceToSqr(player)<=Math.pow(ClothingConfig.HIGHLIGHT_RANGE.get(),2)))ci.setReturnValue(true);
    }
}
