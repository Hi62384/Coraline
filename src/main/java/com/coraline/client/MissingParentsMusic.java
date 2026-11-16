package com.coraline.client;

import com.coraline.feast.MissingParentsCat;
import com.coraline.feast.VisitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The complete song survives Cat arrival and subsequent dialogue until natural EOF. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class MissingParentsMusic {
    private static Track track;private static boolean heard;private static int waiting;
    static void play(){if(track!=null)return;track=new Track();heard=false;waiting=0;var mc=Minecraft.getInstance();mc.getMusicManager().stopPlaying();mc.getSoundManager().play(track);}
    public static boolean owns(SoundInstance sound){return sound instanceof Track;}
    public static boolean active(){return track!=null;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||track==null)return;var mc=Minecraft.getInstance();
        if(mc.getConnection()==null){mc.getSoundManager().stop(track);track=null;return;}
        if(mc.isPaused())return;boolean active=mc.getSoundManager().isActive(track);
        ShowPlaybackClient.observed(MissingParentsCat.INTRO_MUSIC,active);
        if(active)heard=true;else if(heard||++waiting>200)track=null;
    }
    private static final class Track extends AbstractTickableSoundInstance {
        Track(){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",MissingParentsCat.INTRO_MUSIC)),SoundSource.RECORDS,RandomSource.create());relative=true;attenuation=Attenuation.NONE;looping=false;delay=0;volume=VisitConfig.MUSIC_VOLUME.get().floatValue()*.65F;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){volume=VisitConfig.MUSIC_VOLUME.get().floatValue()*(VisitAudioClient.voicePlaying()?.38F:.65F);}
    }
    private MissingParentsMusic(){}
}
