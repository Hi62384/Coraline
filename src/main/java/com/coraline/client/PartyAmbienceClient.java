package com.coraline.client;

import com.coraline.feast.VisitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** An owner-status lease, separate from the story voice/music channel. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class PartyAmbienceClient {
    private static Track track;private static int age=100;
    public static boolean active(){return track!=null&&!track.isStopped()&&age<=60;}
    static void receive(CompoundTag packet){
        age=0;var mc=Minecraft.getInstance();
        if(!packet.getBoolean("PartyMusic")||mc.level==null||mc.player==null){stop();return;}
        if(track!=null&&(track.level!=mc.level||track.isStopped()))stop();
        if(track==null){track=new Track(mc.level);mc.getMusicManager().stopPlaying();mc.getSoundManager().stop(null,SoundSource.MUSIC);mc.getSoundManager().play(track);}
    }
    private static void stop(){if(track!=null)Minecraft.getInstance().getSoundManager().stop(track);track=null;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.isPaused())return;age++;if(age>60||mc.player==null||!mc.player.isAlive()||track!=null&&track.level!=mc.level)stop();}
    private static final class Track extends AbstractTickableSoundInstance {
        final net.minecraft.client.multiplayer.ClientLevel level;
        Track(net.minecraft.client.multiplayer.ClientLevel level){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","party_exploration")),SoundSource.RECORDS,RandomSource.create());this.level=level;relative=true;attenuation=Attenuation.NONE;looping=true;delay=0;volume=0;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){if(age>60||Minecraft.getInstance().level!=level){stop();return;}float target=(VisitAudioClient.voicePlaying()||VisitEventClient.dialogueOpen()?.045F:.13F)*VisitConfig.MUSIC_VOLUME.get().floatValue();volume=net.minecraft.util.Mth.lerp(.08F,volume,target);}
    }
    private PartyAmbienceClient(){}
}
