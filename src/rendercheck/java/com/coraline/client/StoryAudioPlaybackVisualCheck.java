package com.coraline.client;

import com.coraline.client.audio.*;
import com.coraline.feast.VisitAudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.concurrent.*;

/** Disposable title-screen mixer test. Real originals, muted; not audible/story acceptance. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class StoryAudioPlaybackVisualCheck {
    private static Probe sound;private static int stage;private static long pausedAt,pauseFrame=-1,stoppedAt,stopFrame;
    private static final class Probe extends AbstractTickableSoundInstance {
        volatile ContinuousAudioStream stream;
        Probe(){super(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","reunion_a")),SoundSource.VOICE,RandomSource.create());volume=0;relative=true;attenuation=Attenuation.NONE;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){}
        @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary library,Sound first,boolean looping){
            var manager=Minecraft.getInstance().getSoundManager();var opened=new ArrayList<CompletableFuture<AudioStream>>();
            for(String name:List.of("reunion_a","reunion_b")){var event=manager.getSoundEvent(ResourceLocation.fromNamespaceAndPath("coraline",name));if(event==null)throw new AssertionError("Original event missing");var media=event.getSound(RandomSource.create());opened.add(library.getStream(media.getPath(),false).thenApply(CloseOnceAudioStream::new));}
            return CompletableFuture.allOf(opened.toArray(CompletableFuture[]::new)).thenApply(ignored->{try{stream=new ContinuousAudioStream(opened.stream().map(CompletableFuture::join).toList());return stream;}catch(java.io.IOException error){throw new CompletionException(error);}});
        }
    }
    @SubscribeEvent public static void frame(ScreenEvent.Render.Post event){
        if(!Boolean.getBoolean("coraline.storyAudioPlaybackCheck"))return;var mc=Minecraft.getInstance();if(mc.getOverlay()!=null)return;var manager=mc.getSoundManager();
        if(stage==0){mc.getMusicManager().stopPlaying();sound=new Probe();manager.play(sound);stage=1;System.out.println("STORY_AUDIO_MIXER_TEST_STARTED original reunion A/B, unity pitch, muted; no world opened");return;}
        var stream=sound.stream;if(stream==null)return;if(stream.failed())throw new AssertionError("Original decoder failed");long now=System.nanoTime();
        if(stage==1&&stream.playedSeconds()>2){manager.pause();pausedAt=now;stage=2;}
        else if(stage==2&&now-pausedAt>300_000_000L){pauseFrame=stream.playback().frames();stage=3;}
        else if(stage==3&&now-pausedAt>1_000_000_000L){if(stream.playback().frames()!=pauseFrame)throw new AssertionError("Paused mixer advanced story PCM clock");manager.resume();stage=4;System.out.println("STORY_AUDIO_MIXER_PAUSE_VERIFIED frame="+pauseFrame);}
        else if(stage==4&&stream.segmentEnded(1)){
            if(!stream.segmentEnded(0)||!stream.segmentStarted(1)||stream.playback().cancelled())throw new AssertionError("Original adjacent clip boundary lost");
            System.out.println("STORY_AUDIO_MIXER_ORIGINALS_COMPLETE frames="+stream.playback().frames()+" seconds="+stream.playedSeconds()+" end="+stream.endSeconds(1));
            sound=new Probe();manager.play(sound);stage=5;
        }else if(stage==5&&stream.playedSeconds()>1){manager.stop(sound);stoppedAt=now;stage=6;}
        else if(stage==6&&now-stoppedAt>300_000_000L){stopFrame=stream.playback().frames();stage=7;}
        else if(stage==7&&now-stoppedAt>1_000_000_000L){if(!stream.playback().cancelled()||stream.playback().frames()!=stopFrame||stream.segmentEnded(0))throw new AssertionError("Stopped source consumed unheard queued suffix");System.out.println("STORY_AUDIO_MIXER_NATIVE_COMPLETE actual OpenAL pause/end/cancel, original samples; NOT audible/scene acceptance");stage=8;mc.stop();}
    }
    private StoryAudioPlaybackVisualCheck(){}
}
