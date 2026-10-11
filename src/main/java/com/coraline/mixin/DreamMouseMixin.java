package com.coraline.mixin;

import com.coraline.client.ArtifactClientControls;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keep dream look input out of the real body's rotation and movement packets. */
@Mixin(MouseHandler.class)
public abstract class DreamMouseMixin {
    @Redirect(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void coraline$dreamLook(LocalPlayer player,double yaw,double pitch){
        if(!ArtifactClientControls.turnDream(yaw,pitch))player.turn(yaw,pitch);
    }
}
