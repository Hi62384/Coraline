package com.coraline.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The fitted button layer replaces, rather than overlaps, vanilla glowing eyes. */
@Mixin(EyesLayer.class)
public abstract class ActivityMobEyesMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void coraline$buttonEyes(PoseStack pose,MultiBufferSource buffers,int light,Entity mob,float limb,float amount,float partial,float age,float yaw,float pitch,CallbackInfo ci){
        if(mob.getCustomName()!=null&&mob.getCustomName().getString().startsWith("Button-Eyed "))ci.cancel();
    }
}
