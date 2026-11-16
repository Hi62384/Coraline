package com.coraline.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
/** Snapshot-driven one-shot scores; changing phase fades the old score without restarting EOF. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class AcceptedSewingMusic {
    private static String selected="";private static final List<Track> tracks=new ArrayList<>();
    public static boolean owns(SoundInstance sound){return sound instanceof Track;}
    public static void update(){
        String phase=AcceptedButtonEyesClient.phase();
        boolean ghostScore=Set.of("MIRROR_IMPRISONMENT","GHOST_C","GHOST_D","GHOST_E","ACCEPTED_BUTTON_EYES_MIRROR_PRISON").contains(phase)||Set.of("ENTER_MIRROR","EMPTY_WORLD","RECONSTRUCTION","WAITING_FOR_VICTIM","INTRODUCTION","FIRST_SUPPER").contains(GhostVictimClient.phase());
        String next=ghostScore?"mysterious_supper":phase.equals("BASEMENT_INVITATION")||phase.equals("WAITING_TO_SIT")?"parents_rescue_arrival_music":
            Set.of("APPROACH_CHAIR","TAKE_BUTTONS","TAKE_NEEDLE","RAISE_NEEDLE","BLACKOUT","PROCEDURE_COMPLETE").contains(phase)?"accepted_little_me_music":"";
        if(next.equals(selected))return;selected=next;for(var track:tracks)track.fading=true;
        if(!next.isEmpty()){var track=new Track(next);tracks.add(track);Minecraft.getInstance().getSoundManager().play(track);}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(mc.getConnection()==null){for(var t:tracks)mc.getSoundManager().stop(t);tracks.clear();selected="";return;}
        if(!mc.isPaused()){update();tracks.removeIf(AbstractTickableSoundInstance::isStopped);}
    }
    private static final class Track extends AbstractTickableSoundInstance {
        boolean fading;float gain=1;
        Track(String event){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",event)),SoundSource.RECORDS,RandomSource.create());relative=true;attenuation=Attenuation.NONE;looping=event.equals("mysterious_supper");volume=.5F;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){if(fading)gain=Math.max(0,gain-.05F);volume=com.coraline.feast.VisitConfig.MUSIC_VOLUME.get().floatValue()*(VisitAudioClient.voicePlaying()?.28F:.55F)*gain;if(getLocation().getPath().equals("mysterious_supper")){float damage=GhostVictimClient.active()?Math.max(0,Math.min(1,GhostVictimClient.deterioration()/5F)):1;var level=Minecraft.getInstance().level;pitch=com.coraline.client.audio.DeterioratingMusicPitch.at(damage,level==null?0:level.getGameTime());}if(gain==0)stop();}
    }
    private AcceptedSewingMusic(){}
}
