package com.coraline.mixin;

import com.coraline.client.clothing.ClothingLight;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class ClothingEntityLightMixin {
    @Inject(method="getPackedLightCoords",at=@At("RETURN"),cancellable=true)
    private void coraline$clothingLight(Entity entity,float partial,CallbackInfoReturnable<Integer> result){
        result.setReturnValue(ClothingLight.apply(result.getReturnValue(),entity.blockPosition().above()));
    }
}
