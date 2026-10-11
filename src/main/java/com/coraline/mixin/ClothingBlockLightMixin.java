package com.coraline.mixin;

import com.coraline.client.clothing.ClothingLight;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class ClothingBlockLightMixin {
    @Inject(method="getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",at=@At("RETURN"),cancellable=true)
    private static void coraline$clothingLight(BlockAndTintGetter level,BlockState state,BlockPos pos,CallbackInfoReturnable<Integer> result){
        result.setReturnValue(ClothingLight.apply(result.getReturnValue(),pos));
    }
}
