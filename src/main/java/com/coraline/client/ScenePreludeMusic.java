package com.coraline.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Owner-local prelude loops, preserving the original recording and fading on scene exits. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ScenePreludeMusic {
    private static final Map<String,Prelude> tracks=new HashMap<>();
    public static void set(String name,boolean wanted,BlockPos anchor){
        var mc=Minecraft.getInstance();var old=tracks.get(name);
        if(old!=null&&old.level!=mc.level){mc.getSoundManager().stop(old);tracks.remove(name);old=null;}
        if(wanted&&mc.level!=null&&old==null){old=new Prelude(name);tracks.put(name,old);mc.getSoundManager().play(old);}
        if(old!=null){old.wanted=wanted;old.anchor=anchor;old.lease=120;}
    }
    public static void setGain(String name,boolean wanted,float gain){
        var previous=tracks.get(name);
        set(name,wanted,null);var track=tracks.get(name);
        if(track!=null&&wanted){
            track.levelGain=net.minecraft.util.Mth.clamp(gain,0,1);
            // Begin with the scene, already quiet beneath dialogue. Only the
            // later volume increase and exit fade interpolate.
            if(track!=previous){track.gain=1;track.currentLevel=track.levelGain;}
        }
    }
    public static boolean owns(SoundInstance sound){return sound instanceof Prelude;}
    /** Seating is a hard boundary: the entrance must not fade over act A. */
    public static void setEntrance(boolean wanted,BlockPos anchor){
        String name=com.coraline.feast.NauticalOpeningPlan.ENTRANCE;
        if(entranceSeat())wanted=false;
        if(!wanted){var old=tracks.remove(name);if(old!=null)Minecraft.getInstance().getSoundManager().stop(old);return;}
        set(name,true,anchor);
    }
    private static boolean entranceSeat(){var player=Minecraft.getInstance().player;return player!=null&&player.getVehicle() instanceof com.coraline.feast.CinematicPropEntity seat&&seat.kind()==com.coraline.feast.CinematicPropEntity.SEAT&&seat.slot()==0&&player.getUUID().equals(seat.owner);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END)tracks.values().removeIf(AbstractTickableSoundInstance::isStopped);}
    private static final class Prelude extends AbstractTickableSoundInstance {
        final net.minecraft.client.multiplayer.ClientLevel level;boolean wanted;BlockPos anchor;int lease=120;float gain,levelGain=Float.NaN,currentLevel;
        Prelude(String name){super(SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name)),SoundSource.RECORDS,RandomSource.create());level=Minecraft.getInstance().level;relative=true;attenuation=Attenuation.NONE;looping=true;delay=0;volume=0;}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){var mc=Minecraft.getInstance();if(mc.level!=level||mc.player==null||!mc.player.isAlive()||getLocation().getPath().equals(com.coraline.feast.NauticalOpeningPlan.ENTRANCE)&&entranceSeat()){stop();return;}if(mc.isPaused())return;
            if(--lease<=0)wanted=false;gain=Mth.approach(gain,wanted?1:0,1/40F);if(!wanted&&gain<=0){stop();return;}
            float distance=anchor==null?1:Mth.clamp((64-(float)Math.sqrt(mc.player.distanceToSqr(anchor.getCenter())))/40,0,1);
            float level=VisitAudioClient.voicePlaying()?.12F:.28F;
            if(!Float.isNaN(levelGain)){currentLevel=Mth.approach(currentLevel,VisitAudioClient.voicePlaying()?Math.min(.10F,levelGain):levelGain,1/60F);level=currentLevel;}
            volume=gain*distance*level*com.coraline.feast.VisitConfig.MUSIC_VOLUME.get().floatValue();
        }
    }
    private ScenePreludeMusic(){}
}
