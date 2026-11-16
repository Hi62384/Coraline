package com.coraline.mixin;

import com.coraline.client.DimensionStoryAudio;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.audio.Channel;
import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retire the old world's sounds while retaining the very same song channel.
 * Stopped channels are released by ChannelAccess's ordinary sound-thread tick. */
@Mixin(SoundEngine.class)
public abstract class StoryDimensionSoundMixin {
    @Shadow private boolean loaded;
    @Shadow @Final private Map<SoundInstance,ChannelAccess.ChannelHandle> instanceToChannel;
    @Shadow @Final private Map<SoundInstance,Integer> queuedSounds;
    @Shadow @Final private List<TickableSoundInstance> tickingSounds;
    @Shadow @Final private List<TickableSoundInstance> queuedTickableSounds;
    @Shadow @Final private Multimap<SoundSource,SoundInstance> instanceBySource;
    @Shadow @Final private Map<SoundInstance,Integer> soundDeleteTime;
    @Inject(method="stopAll",at=@At("HEAD"),cancellable=true)
    private void coraline$keepCompleteSongs(CallbackInfo ci){
        if(!loaded||!DimensionStoryAudio.handoff()||instanceToChannel.keySet().stream().noneMatch(DimensionStoryAudio::preserves))return;
        instanceToChannel.entrySet().removeIf(entry->{if(DimensionStoryAudio.preserves(entry.getKey()))return false;entry.getValue().execute(Channel::stop);return true;});
        queuedSounds.keySet().removeIf(sound->!DimensionStoryAudio.preserves(sound));
        tickingSounds.removeIf(sound->!DimensionStoryAudio.preserves(sound));
        queuedTickableSounds.removeIf(sound->!DimensionStoryAudio.preserves(sound));
        instanceBySource.values().removeIf(sound->!DimensionStoryAudio.preserves(sound));
        soundDeleteTime.keySet().removeIf(sound->!DimensionStoryAudio.preserves(sound));
        ci.cancel();
    }
}
