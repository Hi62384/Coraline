package com.coraline.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Silences only background music, never the MUSIC-category story recordings themselves. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class StoryMusicGuard {
    private static final Set<String> CUES=Set.of("mice_intro","mice_chase","door_portal_open","tunnel_enter",
            "other_mother_intro","other_father_enter_scene","other_father_intro","other_father_song",
            "other_father_dinners_ready","other_father_golden_chicken_breast","other_parents_waiting",
            "other_mother_visit_again","mysterious_supper","other_mother_supper_0","other_mother_supper_1","other_mother_supper_2","see_you_soon","circus_race");
    private static final Map<SoundInstance,Long> playing=new IdentityHashMap<>();
    private static long ticks;
    @SubscribeEvent(priority=EventPriority.LOW) public static void onSound(PlaySoundEvent event){
        var sound=event.getSound();var mc=Minecraft.getInstance();if(sound==null||mc.level==null)return;
        String cue=sound.getLocation().getPath();
        boolean story="coraline".equals(sound.getLocation().getNamespace())&&(CUES.contains(cue)||cue.startsWith("ambient_")||cue.startsWith("magic_garden_")||cue.equals("coraline_garden")||cue.equals("cake_baking")||cue.startsWith("other_father_activity_"));
        if(story){
            // Server-originated scene cues arrive with a fixed world position. Keep the soundtrack
            // centered on the listener instead; ordinary entity sounds remain positional.
            if(!sound.isRelative()||sound.getAttenuation()!=SoundInstance.Attenuation.NONE){
                sound=new ListenerRecording(sound);
                event.setSound(sound);
            }
            // SoundEngine starts streams asynchronously; give newly queued recordings a short grace period.
            playing.put(sound,ticks+40);mc.getMusicManager().stopPlaying();
        }else if((!playing.isEmpty()||FestivalClient.active())&&sound.getSource()==SoundSource.MUSIC&&"minecraft".equals(sound.getLocation().getNamespace()))event.setSound(null);
    }
    private static final class ListenerRecording extends net.minecraft.client.resources.sounds.AbstractSoundInstance {
        private final SoundInstance original;
        ListenerRecording(SoundInstance original){
            super(original.getLocation(),original.getSource(),net.minecraft.util.RandomSource.create());
            this.original=original;relative=true;attenuation=Attenuation.NONE;
            looping=original.isLooping();delay=original.getDelay();
        }
        @Override public net.minecraft.client.sounds.WeighedSoundEvents resolve(net.minecraft.client.sounds.SoundManager manager){return original.resolve(manager);}
        @Override public net.minecraft.client.resources.sounds.Sound getSound(){return original.getSound();}
        @Override public float getVolume(){return original.getVolume();}
        @Override public float getPitch(){return original.getPitch();}
        @Override public boolean canPlaySound(){return original.canPlaySound();}
        @Override public boolean canStartSilent(){return original.canStartSilent();}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.START)return;ticks++;
        var mc=Minecraft.getInstance();if(mc.level==null){playing.clear();return;}
        playing.entrySet().removeIf(entry->ticks>entry.getValue()&&!mc.getSoundManager().isActive(entry.getKey()));
        // New vanilla tracks are blocked in onSound. Repeated stopPlaying() would keep adding to its timer.
    }
}
