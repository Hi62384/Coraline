package com.coraline.qa;

import com.coraline.client.FirstEscapeMusic;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Disposable QA diagnostics only; does not cancel or change any sound action. */
@Mixin(SoundEngine.class)
public abstract class MusicLifetimeTrace {
    @Inject(method="stopAll",at=@At("HEAD"))
    private void coraline$all(CallbackInfo ci){
        if(Boolean.getBoolean("coraline.seeingStoneMusicCheck"))new Exception("QA_MUSIC_STOP_ALL").printStackTrace();
    }
    @Inject(method="stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",at=@At("HEAD"))
    private void coraline$one(SoundInstance sound,CallbackInfo ci){
        if(Boolean.getBoolean("coraline.seeingStoneMusicCheck")&&FirstEscapeMusic.owns(sound))new Exception("QA_MUSIC_STOP_TRACK").printStackTrace();
    }
}
