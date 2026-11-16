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
    private static int voiceAudibleStarted=-1;
    private static com.google.gson.JsonObject envelopes;
    private static String visitMusic="";
    private static String voiceName(){return voice instanceof StoryRun run?run.selected():voice==null?"":voice.getLocation().getPath();}
    public static boolean visitActive(){return !visitMusic.isEmpty();}
    public static float voiceLevel(String prefix){
        if(voice==null||!ShowSeekClient.actuallyStarted(voice)||!voiceName().startsWith(prefix)||!Minecraft.getInstance().getSoundManager().isActive(voice))return 0;
        if(envelopes==null)try(var stream=Minecraft.getInstance().getResourceManager().open(ResourceLocation.fromNamespaceAndPath("coraline","visit_voice_envelopes.json"));var reader=new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){envelopes=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();}catch(java.io.IOException e){return 0;}
        var values=envelopes.getAsJsonArray(voiceName());int index=storyTicks(voiceName());
        if(values==null||index<0||index>=values.size())return 0;
        // Lip articulation follows the recording, not the listener's volume slider.
        // Multiplying all mixer gains made the thin Bobinsky smile appear motionless.
        float level=values.get(index).getAsFloat();return level<.035F?0:Math.min(1,level*1.25F);
    }
    private static boolean garden(SoundInstance sound){if(sound==null)return false;String id=sound.getLocation().getPath();return id.equals("circus_race")||id.startsWith("magic_garden_")||id.equals("coraline_garden")||id.equals("other_father_activity_magic_garden_intro");}
    private static boolean playing(SoundInstance sound,boolean song){return sound!=null&&(ShowSeekClient.pending(sound)||ticks-(song?musicStarted:voiceStarted)<20||Minecraft.getInstance().getSoundManager().isActive(sound));}
    public static boolean hasVoice(String name){return voice!=null&&voiceName().equals(name);}
    /** Preserve only the currently bound story stream during a level handoff. */
    static boolean handoffVoice(SoundInstance sound){return sound!=null&&sound==voice&&sound instanceof StoryRun;}
    public static int storyTicks(String name){if(voice instanceof StoryRun run)return run.position(name);return hasVoice(name)&&voiceAudibleStarted>=0?Math.max(0,ticks-voiceAudibleStarted):-1;}
    public static boolean hasMusic(String name){return music instanceof StoryRun run?run.selected().equals(name):music!=null&&music.getLocation().getPath().equals(name);}
    public static boolean voicePlaying(){return playing(voice,false);}
    /** Retire house-only chatter at venue entry, not the actual performance mix. */
    static void quietAudience(){
        gardenQueue.removeIf(cue->cue.name().startsWith("other_mother_"));
        if(voice!=null&&voiceName().startsWith("other_mother_")){Minecraft.getInstance().getSoundManager().stop(voice);voice=null;}
    }
    @SubscribeEvent public static void theatreSound(net.minecraftforge.client.event.sound.PlaySoundEvent event){
        var sound=event.getSound();if(sound!=null&&VisitEventClient.quietTheatre()&&sound.getLocation().getNamespace().equals("coraline")&&
            (sound.getLocation().getPath().startsWith("other_mother_")||sound.getLocation().getPath().equals("mouse_squeak")))event.setSound(null);
    }
    public static void accept(VisitAudio.Cue cue){
        if(cue.music()&&cue.name().equals(OtherMotherGameProposalMusic.NAME)){if(cue.stop())OtherMotherGameProposalMusic.stop();else{ParentsRescueArrivalMusic.stop();OtherMotherGameProposalMusic.play();}return;}
        if(cue.music()&&cue.name().equals(ParentsRescueArrivalMusic.NAME)){if(cue.stop())ParentsRescueArrivalMusic.stop();else ParentsRescueArrivalMusic.play();return;}
        if(cue.music()&&cue.name().equals(FirstEscapeMusic.NAME)){if(cue.stop())FirstEscapeMusic.stop();else FirstEscapeMusic.play();return;}
        if(cue.music()&&cue.name().equals(MissingParentsCat.INTRO_MUSIC)){if(!cue.stop())MissingParentsMusic.play();return;}
        var manager=Minecraft.getInstance().getSoundManager();
        if(cue.stop()){
            ShowSeekClient.stopping(cue.name(),cue.music());
            if(cue.name().isEmpty())ShowSeekClient.stopping("",false);
            if(cue.music()&&(cue.name().isEmpty()||cue.name().equals(visitMusic)))visitMusic="";
            if(!cue.name().isEmpty()){
                gardenQueue.removeIf(queued->queued.name().equals(cue.name()));
                if(cue.music()&&hasMusic(cue.name())){manager.stop(music);music=null;}
                if(!cue.music()&&voice!=null&&(voiceName().equals(cue.name())||voice.getLocation().getPath().equals(cue.name()))){manager.stop(voice);voice=null;StorySubtitles.stop(cue.name());}
                return;
            }
            gardenQueue.removeIf(queued->queued.name().startsWith("other_mother_"));
            if(!garden(voice)){if(voice!=null)manager.stop(voice);voice=null;}
            if(cue.music()&&!garden(music)){if(music!=null)manager.stop(music);music=null;}
            return;
        }
        if(VisitEventClient.quietTheatre()&&cue.name().startsWith("other_mother_"))return;
        if((cue.music()?music:voice) instanceof StoryRun run&&run.select(cue.name()))return;
        if(cue.music()&&(cue.name().equals("wybie_visit_music")||cue.name().equals("bobinsky_visit_music")||cue.name().equals("tea_visit_music"))){visitMusic=cue.name();Minecraft.getInstance().getMusicManager().stopPlaying();manager.stop(null,SoundSource.MUSIC);}
        var current=cue.music()?music:voice;
        if(cue.music()&&cue.name().equals(AcceptedButtonEyes.HOUSE_AMBIENCE)&&hasMusic(cue.name())&&playing(current,true))return;
        if(cue.name().startsWith("ambient_")&&current!=null&&playing(current,true))return;
        // Streaming can start a little after the server's duration clock.
        // Keep consecutive Other Mother lines intact, including offer/outcome dialogue.
        boolean motherTransition=!cue.music()&&current!=null&&current.getLocation().getPath().startsWith("other_mother_")&&cue.name().startsWith("other_mother_");
        if((garden(current)||motherTransition)&&playing(current,cue.music())){
            if(!current.getLocation().getPath().equals(cue.name())&&!gardenQueue.contains(cue))gardenQueue.add(cue);
            return;
        }
        if(cue.name().equals("see_you_soon")){
            ShowSeekClient.stopping("",false);
            if(voice!=null)manager.stop(voice);voice=null;if(farewell!=null)manager.stop(farewell);
            farewell=new Recording(cue);manager.play(farewell);return;
        }
        var old=cue.music()?music:voice;if(old!=null)manager.stop(old);ShowSeekClient.stopping("",cue.music());
        var bound=ShowPlaybackClient.seekSnapshot();
        var sound=bound!=null&&bound.sound().equals(cue.name())?new StoryRun(cue):new Recording(cue);
        manager.play(sound);if(cue.music()){music=sound;musicStarted=ticks;}else{voice=sound;voiceStarted=ticks;voiceAudibleStarted=-1;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase==TickEvent.Phase.END){
        var mc=Minecraft.getInstance();
        if(mc.isPaused())return;ticks++;
        if(mc.player==null||!mc.player.isAlive()||mc.level==null){
            for(var sound:new SoundInstance[]{voice,music,farewell})if(sound!=null)mc.getSoundManager().stop(sound);
            voice=null;music=null;farewell=null;visitMusic="";gardenQueue.clear();ShowPlaybackClient.clear();return;
        }
        if(voice!=null){boolean active=mc.getSoundManager().isActive(voice);if(active&&voiceAudibleStarted<0)voiceAudibleStarted=ticks;if(voice instanceof StoryRun run)run.observe(active);else ShowPlaybackClient.observed(voiceName(),active);ShowSeekClient.observed(voice,active);}
        if(music!=null){boolean active=mc.getSoundManager().isActive(music);if(music instanceof StoryRun run)run.observe(active);else ShowPlaybackClient.observed(music.getLocation().getPath(),active);ShowSeekClient.observed(music,active);}
        if(voice!=null&&!playing(voice,false)&&!(voice instanceof StoryRun run&&run.retainSource())){String done=voice.getLocation().getPath();if(done.startsWith("cat_encounter_")||done.startsWith("other_mother_")||done.startsWith("father_game_night_")||done.startsWith("father_outing_")||done.startsWith("father_dinner_")||done.startsWith("father_pass_")||done.startsWith("wybie_visit_")||done.startsWith("bobinsky_visit_")||done.startsWith("tea_visit_"))VisitAudio.voiceFinished(done);voice=null;}
        if(music!=null&&!playing(music,true)&&!(music instanceof StoryRun run&&run.retainSource()))music=null;
        var ready=new java.util.ArrayList<VisitAudio.Cue>();
        for(var cue:gardenQueue)if((cue.music()?music:voice)==null){ready.add(cue);break;}
        for(var cue:ready){gardenQueue.remove(cue);accept(cue);}
        if(!ActivityWorld.protectedWorld(mc.level)&&!visitActive()&&!hasMusic("wybie_visit_music")&&!hasMusic("bobinsky_visit_music")&&!hasMusic("tea_visit_music")&&(voice==null||!VisitAudio.overworldStoryVoice(voice.getLocation().getPath())))accept(new VisitAudio.Cue("",true,true));
    }}
    /** Only the server-bound offset path calls this; ordinary queues, mix and captions stay unchanged. */
    static SoundInstance playSeek(ShowPlaybackNetwork.SeekStart command,ShowSeekClient.Token token){
        var manager=Minecraft.getInstance().getSoundManager();
        var old=command.music()?music:voice;if(old!=null)manager.stop(old);
        // Full-line captions must not falsely restart at the middle of a recording.
        // Aligned offset subtitles are a separate native enablement gate.
        if(!command.music())StorySubtitles.stop(command.sound());
        var sound=new SeekRecording(new VisitAudio.Cue(command.sound(),command.music(),false),token);
        if(command.music()){music=sound;musicStarted=ticks;}else{voice=sound;voiceStarted=ticks;}
        manager.play(sound);return sound;
    }
    static void seekStarted(SoundInstance sound,int offset){if(voice==sound)voiceStarted=ticks-offset;else if(music==sound)musicStarted=ticks-offset;}
    /** One decoder sequence on one mixer source. Following cues only bind the next checkpoint. */
    private static final class StoryRun extends Recording {
        final java.util.List<String> names;final boolean musicChannel;int segment;java.util.UUID observationNonce;
        volatile com.coraline.client.audio.ContinuousAudioStream stream;
        volatile boolean openFailed;
        StoryRun(VisitAudio.Cue cue){super(cue);musicChannel=cue.music();names=StoryAudioContinuity.run(cue.name());var binding=ShowPlaybackClient.seekSnapshot();observationNonce=binding!=null&&binding.sound().equals(cue.name())?binding.nonce():null;}
        String selected(){return names.get(segment);}
        double seconds(){return stream==null?-1:stream.playedSeconds();}
        int position(String name){int i=names.indexOf(name);if(i<0||stream==null||seconds()<0)return -1;double start=stream.startSeconds(i);return start<0?-1:Math.max(0,(int)((seconds()-start)*20));}
        // The sound thread publishes its final consumed buffers before release.
        // Do not drop that receipt on an earlier SoundManager removal, nor a
        // genuinely completed adjacent clip before its server binding arrives.
        boolean retainSource(){var decoded=stream;return !openFailed&&(decoded==null||!decoded.failed()&&!decoded.playback().cancelled()&&(!decoded.playback().released()||segment+1<names.size()&&decoded.segmentEnded(segment)));}
        void observe(boolean active){var decoded=stream;boolean complete=decoded!=null&&decoded.segmentEnded(segment);boolean failed=openFailed||decoded!=null&&(decoded.failed()||decoded.playback().cancelled()||decoded.playback().released()&&!complete);ShowPlaybackClient.observedFrames(observationNonce,selected(),decoded!=null&&decoded.segmentStarted(segment),complete,failed);}
        boolean select(String name){var bound=ShowPlaybackClient.seekSnapshot();if(bound==null||bound.music()!=musicChannel||!bound.sound().equals(name)||segment+1>=names.size()||!names.get(segment+1).equals(name))return false;segment++;observationNonce=bound.nonce();if(musicChannel)musicStarted=ticks;else{voiceStarted=ticks;voiceAudibleStarted=ticks-Math.max(0,position(name));StorySubtitles.continuation(this,name,Math.max(0,position(name)));}return true;}
        @Override public java.util.concurrent.CompletableFuture<net.minecraft.client.sounds.AudioStream> getStream(net.minecraft.client.sounds.SoundBufferLibrary library,Sound first,boolean looping){
            var manager=Minecraft.getInstance().getSoundManager();var opened=new java.util.ArrayList<java.util.concurrent.CompletableFuture<net.minecraft.client.sounds.AudioStream>>();
            var failed=new java.util.concurrent.atomic.AtomicBoolean();
            try{
                for(String name:names){var event=manager.getSoundEvent(ResourceLocation.fromNamespaceAndPath("coraline",name));if(event==null)throw new java.io.IOException("Missing adjacent story recording: "+name);var sound=event.getSound(RandomSource.create());if(!sound.shouldStream())throw new java.io.IOException("Story continuity requires original streamed recordings");
                    var future=library.getStream(sound.getPath(),false).thenApply(raw->(net.minecraft.client.sounds.AudioStream)new com.coraline.client.audio.CloseOnceAudioStream(raw));
                    future.whenComplete((source,error)->{if(error!=null)failed.set(true);if(source!=null&&failed.get())try{source.close();}catch(java.io.IOException ignored){}});opened.add(future);
                }
            }catch(Exception error){openFailed=true;failed.set(true);for(var future:opened)future.thenAccept(source->{try{source.close();}catch(java.io.IOException ignored){}});return java.util.concurrent.CompletableFuture.failedFuture(error);}
            var result=java.util.concurrent.CompletableFuture.allOf(opened.toArray(java.util.concurrent.CompletableFuture[]::new)).thenApply(ignored->{try{stream=new com.coraline.client.audio.ContinuousAudioStream(opened.stream().map(java.util.concurrent.CompletableFuture::join).toList());return (net.minecraft.client.sounds.AudioStream)stream;}catch(java.io.IOException error){throw new java.util.concurrent.CompletionException(error);}});
            result.whenComplete((ready,error)->{if(error!=null){openFailed=true;failed.set(true);for(var future:opened)future.thenAccept(source->{try{source.close();}catch(java.io.IOException ignored){}});}});return result;
        }
    }
    private static final class SeekRecording extends Recording implements ShowSeekClient.Streamed {
        private final ShowSeekClient.Token token;
        SeekRecording(VisitAudio.Cue cue,ShowSeekClient.Token token){super(cue);this.token=token;looping=false;}
        @Override public ShowSeekClient.Token seekToken(){return token;}
        @Override public net.minecraft.client.sounds.WeighedSoundEvents resolve(net.minecraft.client.sounds.SoundManager manager){
            var resolved=super.resolve(manager);
            if(resolved==null||!ShowSeekClient.accepts(token,getSound())){ShowSeekClient.rejected(token);return null;}
            return resolved;
        }
        @Override public java.util.concurrent.CompletableFuture<net.minecraft.client.sounds.AudioStream> getStream(net.minecraft.client.sounds.SoundBufferLibrary buffers,Sound sound,boolean looping){return ShowSeekClient.open(token,buffers,sound,looping);}
    }
    private static class Recording extends AbstractTickableSoundInstance {
        // Muting the mix must not suppress the stream's start/end acknowledgements.
        // The real recording still runs silently; no guessed replacement timer is used.
        @Override public boolean canStartSilent(){return true;}
        private final boolean background;
        private final float nominal;
        @Override public void tick(){if(background){
            volume=nominal*(voice!=null?.42F:1F)*AcceptedButtonEyesClient.musicGain();
            if(getLocation().getPath().equals(AcceptedButtonEyes.HOUSE_AMBIENCE)){
                var level=Minecraft.getInstance().level;
                pitch=AcceptedButtonEyesClient.house()&&level!=null
                    ?com.coraline.client.audio.DeterioratingMusicPitch.at(AcceptedButtonEyesClient.stage()/5F,level.getGameTime()):1;
            }
        }}
        // Scripted songs are performances, not vanilla background music. Keep them audible
        // when players disable the Music slider; Jukebox/Note Blocks controls their level.
        Recording(VisitAudio.Cue cue){super(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",cue.name())),cue.music()?SoundSource.RECORDS:SoundSource.VOICE,RandomSource.create());relative=true;attenuation=Attenuation.NONE;
            background=cue.name().startsWith("ambient_")||cue.name().equals("cake_baking")||cue.name().equals(AcceptedButtonEyes.HOUSE_AMBIENCE)||cue.name().equals("wybie_visit_music")||cue.name().equals("bobinsky_visit_music")||cue.name().equals("tea_visit_music");
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
