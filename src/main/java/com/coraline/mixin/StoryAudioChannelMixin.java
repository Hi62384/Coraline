package com.coraline.mixin;

import com.coraline.client.audio.ContinuousAudioStream;
import com.coraline.client.audio.StoryAudioMixerCursor;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe only the sound thread's actual OpenAL cursor/buffers; never alter playback. */
@Mixin(Channel.class)
public abstract class StoryAudioChannelMixin {
    @Shadow @Final private int source;
    @Unique private StoryAudioMixerCursor coraline$story;
    @Inject(method="attachBufferStream",at=@At("HEAD"))
    private void coraline$stream(AudioStream stream,CallbackInfo ci){coraline$story=stream instanceof ContinuousAudioStream story?new StoryAudioMixerCursor(source,story):null;}
    @Inject(method="play",at=@At("RETURN"))
    private void coraline$start(CallbackInfo ci){if(coraline$story!=null)coraline$story.start();}
    @Inject(method="stop",at=@At("HEAD"))
    private void coraline$cancel(CallbackInfo ci){if(coraline$story!=null)coraline$story.cancel();}
    @Inject(method="destroy",at=@At("HEAD"))
    private void coraline$destroy(CallbackInfo ci){if(coraline$story!=null)coraline$story.beforeDestroy();}
    @Inject(method="destroy",at=@At("RETURN"))
    private void coraline$released(CallbackInfo ci){if(coraline$story!=null)coraline$story.released();coraline$story=null;}
    @Inject(method="updateStream",at=@At("HEAD"))
    private void coraline$cursor(CallbackInfo ci){if(coraline$story!=null)coraline$story.sample();}
    @Redirect(method="removeProcessedBuffers",at=@At(value="INVOKE",target="Lorg/lwjgl/openal/AL10;alSourceUnqueueBuffers(I[I)V",remap=false))
    private void coraline$consumed(int channel,int[] buffers){
        AL10.alSourceUnqueueBuffers(channel,buffers);
        if(coraline$story!=null)coraline$story.consumed(buffers);
    }
}
