package com.coraline.mixin;

import com.coraline.client.FirstEscapeClient;
import com.coraline.feast.FirstEscapeVisualRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** After vanilla gamma/ambient lifting; emissive ghost geometry does not sample this texture. */
@Mixin(LightTexture.class)
public abstract class PrisonLightmapMixin {
    @ModifyArg(method="updateLightTexture",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/platform/NativeImage;setPixelRGBA(III)V"),index=2)
    private int coraline$prisonShadow(int pixel){
        var level=Minecraft.getInstance().level;
        return level!=null&&com.coraline.otherworld.OtherWorld.isOther(level)&&FirstEscapeClient.prison()?FirstEscapeVisualRules.prisonPixel(pixel):pixel;
    }
}
