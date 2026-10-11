package com.coraline.client;
import com.coraline.feast.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class VisitAudioClient {
    private static SoundInstance voice,music,farewell;
    private static final java.util.ArrayDeque<VisitAudio.Cue> gardenQueue=new java.util.ArrayDeque<>();
    private static int ticks,voiceStarted,musicStarted;
    private static boolean garden(SoundInstance sound){if(sound==null)return false;String id=sound.getLocation().getPath();return id.equals("circus_race")||id.startsWith("magic_garden_")||id.equals("coraline_garden")||id.equals("other_father_activity_magic_garden_intro");}
    private static boolean playing(SoundInstance sound,boolean song){return sound!=null&&(ticks-(song?musicStarted:voiceStarted)<20||Minecraft.getInstance().getSoundManager().isActive(sound));}
    public static boolean hasVoice(String name){return voice!=null&&voice.getLocation().getPath().equals(name);}
    public static boolean hasMusic(String name){return music!=null&&music.getLocation().getPath().equals(name);}
    public static void accept(VisitAudio.Cue cue){
        var manager=Minecraft.getInstance().getSoundManager();
        if(cue.stop()){
            if(!cue.name().isEmpty()){
                gardenQueue.removeIf(queued->queued.name().equals(cue.name()));
                if(cue.music()&&music!=null&&music.getLocation().getPath().equals(cue.name())){manager.stop(music);music=null;}
                if(!cue.music()&&voice!=null&&voice.getLocation().getPath().equals(cue.name())){manager.stop(voice);voice=null;StorySubtitles.stop(cue.name());}
                return;
            }
            gardenQueue.removeIf(queued->queued.name().startsWith("other_mother_"));
            if(!garden(voice)){if(voice!=null)manager.stop(voice);voice=null;}
            if(cue.music()&&!garden(music)){if(music!=null)manager.stop(music);music=null;}
            return;
        }
        var current=cue.music()?music:voice;
        if(cue.name().startsWith("ambient_")&&current!=null&&playing(current,true))return;
        // Streaming can start a little after the server's duration clock.
        // Keep consecutive Other Mother lines intact, including offer/outcome dialogue.
        boolean motherTransition=!cue.music()&&current!=null&&current.getLocation().getPath().startsWith("other_mother_")&&cue.name().startsWith("other_mother_");
        if((garden(current)||motherTransition)&&playing(current,cue.music())){
            if(!current.getLocation().getPath().equals(cue.name())&&!gardenQueue.contains(cue))gardenQueue.add(cue);
            return;
        }
        if(cue.name().equals("see_you_soon")){
            if(voice!=null)manager.stop(voice);voice=null;if(farewell!=null)manager.stop(farewell);
            farewell=new Recording(cue);manager.play(farewell);return;
        }
        var old=cue.music()?music:voice;if(old!=null)manager.stop(old);
        var sound=new Recording(cue);manager.play(sound);if(cue.music()){music=sound;musicStarted=ticks;}else{voice=sound;voiceStarted=ticks;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END){
        ticks++;
        var mc=Minecraft.getInstance();
        if(mc.player==null||!mc.player.isAlive()||mc.level==null){
            for(var sound:new SoundInstance[]{voice,music,farewell})if(sound!=null)mc.getSoundManager().stop(sound);
            voice=null;music=null;farewell=null;gardenQueue.clear();return;
        }
        if(voice!=null&&!playing(voice,false)){String done=voice.getLocation().getPath();if(done.startsWith("other_mother_"))VisitAudio.voiceFinished(done);voice=null;}
        if(music!=null&&!playing(music,true))music=null;
        var ready=new java.util.ArrayList<VisitAudio.Cue>();
        for(var cue:gardenQueue)if((cue.music()?music:voice)==null){ready.add(cue);break;}
        for(var cue:ready){gardenQueue.remove(cue);accept(cue);}
        if(!ActivityWorld.protectedWorld(mc.level))accept(new VisitAudio.Cue("",true,true));
    }}
    private static class Recording extends AbstractTickableSoundInstance {
        private final boolean background;
        private final float nominal;
        @Override public void tick(){if(background)volume=nominal*(voice!=null?.42F:1F);}
        // Scripted songs are performances, not vanilla background music. Keep them audible
        // when players disable the Music slider; Jukebox/Note Blocks controls their level.
        Recording(VisitAudio.Cue cue){super(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",cue.name())),cue.music()?SoundSource.RECORDS:SoundSource.VOICE,RandomSource.create());relative=true;attenuation=Attenuation.NONE;
            background=cue.name().startsWith("ambient_")||cue.name().equals("cake_baking");
            float base=(cue.music()?VisitConfig.MUSIC_VOLUME:VisitConfig.VOICE_VOLUME).get().floatValue();
            // The two garden interjections are quiet masters. Their Oggs are peak-safe;
            // keep the flight music beneath them without restarting it mid-flight.
            volume=base*(background?.28F:cue.name().equals("circus_race")?.6F:cue.name().equals("magic_garden_fly")?.45F:
                cue.name().equals("magic_garden_hop_on")||cue.name().equals("magic_garden_mother_said")?1.35F:1F);
            // The flight asset covers the whole scripted flight once. Looping
            // it restarted the opening motif during descent.
            // Garden recordings play once, in full; queued tracks start afterward.
            nominal=volume;if(background)looping=true;
        }
    }
}
