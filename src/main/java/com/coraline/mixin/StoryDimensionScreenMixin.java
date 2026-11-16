package com.coraline.mixin;

import com.coraline.client.DimensionStoryAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class StoryDimensionScreenMixin {
    @Shadow private void updateScreenAndTick(Screen screen){throw new AssertionError();}
    @Redirect(method="setLevel",at=@At(value="INVOKE",target="Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void coraline$handoff(Minecraft minecraft,Screen screen){
        if(minecraft.level!=null&&minecraft.getConnection()!=null)DimensionStoryAudio.duringHandoff(()->updateScreenAndTick(screen));
        else updateScreenAndTick(screen);
    }
}
