package com.coraline.mixin;

import com.coraline.client.ArtifactClientControls;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remote inputs must never also swap, drop, use or attack with the player's body. */
@Mixin(Minecraft.class)
public abstract class RemoteBodyInputMixin {
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void coraline$noBodyMining(boolean held,CallbackInfo ci){
        if(ArtifactClientControls.remoteView()||com.coraline.client.FestivalClient.active())ci.cancel();
    }
    @Inject(method="handleKeybinds",at=@At("HEAD"),cancellable=true)
    private void coraline$remoteInputs(CallbackInfo ci){
        if(ArtifactClientControls.remoteView()||com.coraline.client.FestivalClient.active()){
            ArtifactClientControls.consumeBodyActions();
            ci.cancel();
        }
    }
}
