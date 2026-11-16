package com.coraline.feast;
import com.coraline.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.network.*;
import java.util.*;
public final class VisitAudio {
    public static final Map<String,net.minecraftforge.registries.RegistryObject<SoundEvent>> SOUNDS=new LinkedHashMap<>();
    private static final net.minecraftforge.network.simple.SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath("coraline","visit_audio"),()->"2","2"::equals,"2"::equals);
    public record Cue(String name,boolean music,boolean stop){}
    private static void storySend(ServerPlayer owner,Object packet){
        for(var recipient:com.coraline.multiplayer.StorySessionData.get(owner.server).audience(owner))CHANNEL.send(PacketDistributor.PLAYER.with(()->recipient),packet);
    }

    public static boolean overworldStoryVoice(String name){return name.startsWith("wybie_visit_")||name.startsWith("bobinsky_visit_")||name.startsWith("tea_visit_")||name.startsWith("missing_parents_")||name.startsWith("reunion_")||name.startsWith("farewell_")||name.startsWith("crawling_hand_");}
    public record Caption(String speaker,String text,int ticks){}
    public record IntroOffer(String sound,int generation,int ticks){}
    public record SkipIntro(String sound,int generation){}
    public record LeaveActivity(){}
    public static void leaveActivity(){CHANNEL.sendToServer(new LeaveActivity());}
    public record VoiceFinished(String sound){}
    public record Chatter(int actor,boolean start){}
    public static void chatter(net.minecraft.world.entity.Mob actor,boolean start){if(actor.getPersistentData().getBoolean("PartyChatterActive")==start&&actor.tickCount%40!=0)return;actor.getPersistentData().putBoolean("PartyChatterActive",start);CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(()->actor),new Chatter(actor.getId(),start));}
    public static void voiceFinished(String sound){CHANNEL.sendToServer(new VoiceFinished(sound));}
    static void introOffer(ServerPlayer p,IntroOffer offer){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),offer);}
    public static void skipIntro(String sound,int generation){CHANNEL.sendToServer(new SkipIntro(sound,generation));}
    public static void caption(ServerPlayer player,String speaker,String text,int ticks){if(!IncidentalDialogue.say(player,speaker,text))storySend(player,new Caption(speaker,text,ticks));}
    private static com.google.gson.JsonObject lengths;
    public static void register(){
        for(String name:new String[]{"garden_ghost_eye_music","theatre_ghost_eye_music","circus_ghost_eye_music"})SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        SOUNDS.put("ghost_victim_talk",ModSounds.SOUNDS.register("ghost_victim_talk",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","ghost_victim_talk"))));
        for(String name:IncidentalDialogue.soundIds())SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        SOUNDS.put("other_mother_game_proposal_music",ModSounds.SOUNDS.register("other_mother_game_proposal_music",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","other_mother_game_proposal_music"))));
        SOUNDS.put("accepted_little_me_music",ModSounds.SOUNDS.register("accepted_little_me_music",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","accepted_little_me_music"))));
        SOUNDS.put("parents_rescue_arrival_music",ModSounds.SOUNDS.register("parents_rescue_arrival_music",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","parents_rescue_arrival_music"))));
        SOUNDS.put("spink_and_forcible_boss_music",ModSounds.SOUNDS.register("spink_and_forcible_boss_music",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","spink_and_forcible_boss_music"))));
        SOUNDS.put("the_cat_music",ModSounds.SOUNDS.register("the_cat_music",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","the_cat_music"))));
        SOUNDS.put(LongFaceMusicPlan.SOUND,ModSounds.SOUNDS.register(LongFaceMusicPlan.SOUND,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",LongFaceMusicPlan.SOUND))));
        SOUNDS.put("first_escape_music",ModSounds.SOUNDS.register("first_escape_music",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","first_escape_music"))));
        SOUNDS.put(MissingParentsCat.INTRO_MUSIC,ModSounds.SOUNDS.register(MissingParentsCat.INTRO_MUSIC,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",MissingParentsCat.INTRO_MUSIC))));
        for(String name:FinalChapterRecordings.SOURCES.keySet())SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(String name:RescueRecordings.names())SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(char c='a';c<='n';c++){String name="parent_rescue_"+c;SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));}
        for(char c='a';c<='f';c++){String name="missing_parents_cat_"+c;SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));}
        for(char c='a';c<='i';c++){String name="missing_parents_tea_"+c;SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));}
        for(var part:java.util.Map.of("cocoa_beetles","abcdefg","mirror_prison","abcde","first_escape","abcdefg","missing_parents","abcdefg").entrySet())for(char c:part.getValue().toCharArray()){String name=part.getKey()+"_"+c;SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));}
        SOUNDS.put("mirror_prison_e_accepted",ModSounds.SOUNDS.register("mirror_prison_e_accepted",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","mirror_prison_e_accepted"))));
        for(char letter='a';letter<='s';letter++){String name="black_is_traditional_"+letter;SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));}
        for(char letter='a';letter<='i';letter++){String name="cat_encounter_"+letter;SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));}
        CHANNEL.messageBuilder(Chatter.class,6,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{b.writeInt(v.actor);b.writeBoolean(v.start);}).decoder(b->new Chatter(b.readInt(),b.readBoolean())).consumerMainThread((v,c)->com.coraline.client.PartyChatterClient.accept(v.actor,v.start)).add();
        for(String name:new String[]{"wybie_adventure_intro","theatre_music","theatre_a","theatre_b","theatre_c","theatre_d","theatre_e","theatre_f","theatre_meet_parents","mouse_circus_invite","mouse_circus_intro","mouse_circus_music","mouse_circus_end"})if(!SOUNDS.containsKey(name))SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(String name:FamilyGameNight.fatherVoices())SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(String name:FamilyOuting.fatherVoices())SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(String name:FamilyDinner.FATHER_VOICES)SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(String name:VisitEvents.fatherStudyVoices())SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(String letter:new String[]{"a","b","c","d","e","f","g","h","i","j","music"}){String name="tea_visit_"+letter;SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));}
        for(String letter:new String[]{"a","b","c","d","e","f","g","h","i","j","k","music"}){String name="bobinsky_visit_"+letter;SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));}
        for(String letter:new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","music"}){
            String name="wybie_visit_"+letter;
            SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        }
        SOUNDS.put("circus_race",ModSounds.SOUNDS.register("circus_race",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","circus_race"))));
        for(String name:new String[]{"ambient_carnival","ambient_theatre","ambient_circus","ambient_race","ambient_archery","ambient_garden","ambient_feast","ambient_dream","ambient_village","party_exploration","theatre_arrival_music","traditional_music"})
            SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        SOUNDS.put("cake_baking",ModSounds.SOUNDS.register("cake_baking",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","cake_baking"))));
        SOUNDS.put("accepted_ghost_children_music",ModSounds.SOUNDS.register("accepted_ghost_children_music",()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline","accepted_ghost_children_music"))));
        for(String name:new String[]{"other_mother_intro","other_father_enter_scene","other_father_intro","other_father_song","other_father_dinners_ready","other_father_golden_chicken_breast","other_parents_waiting","other_mother_visit_again","mysterious_supper","magic_garden_fly","coraline_garden","magic_garden_hop_on","magic_garden_mother_said","other_mother_supper_0","other_mother_supper_1","other_mother_supper_2","see_you_soon"})SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(var state:OtherMotherWelcome.State.values())for(int i=0;i<3;i++){
            String name=state.sound(i);
            SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        }
        for(var pool:OtherMotherDialogue.pools())for(int i=0;i<pool.size();i++){
            String name=pool.sound(i);
            SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        }
        for(String name:NumberedEventDialogue.soundIds())if(!SOUNDS.containsKey(name))SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        for(var activity:OtherMotherActivities.Activity.values()){
            String name=ActivityWorld.introSound(activity);
            SOUNDS.put(name,ModSounds.SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("coraline",name))));
        }
        CHANNEL.messageBuilder(Cue.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{b.writeUtf(v.name);b.writeBoolean(v.music);b.writeBoolean(v.stop);}).decoder(b->new Cue(b.readUtf(),b.readBoolean(),b.readBoolean())).consumerMainThread((v,c)->com.coraline.client.VisitAudioClient.accept(v)).add();
        CHANNEL.messageBuilder(Caption.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{b.writeUtf(v.speaker);b.writeUtf(v.text);b.writeInt(v.ticks);}).decoder(b->new Caption(b.readUtf(),b.readUtf(),b.readInt())).consumerMainThread((v,c)->com.coraline.client.StorySubtitles.caption(v.speaker,v.text,v.ticks)).add();
        CHANNEL.messageBuilder(IntroOffer.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder((v,b)->{b.writeUtf(v.sound);b.writeInt(v.generation);b.writeInt(v.ticks);}).decoder(b->new IntroOffer(b.readUtf(128),b.readInt(),b.readInt())).consumerMainThread((v,c)->com.coraline.client.ActivityIntroClient.offer(v)).add();
        CHANNEL.messageBuilder(VoiceFinished.class,4,NetworkDirection.PLAY_TO_SERVER).encoder((v,b)->b.writeUtf(v.sound,128)).decoder(b->new VoiceFinished(b.readUtf(128))).consumerMainThread((v,c)->{var p=c.get().getSender();if(p!=null&&!com.coraline.multiplayer.StorySessionData.get(p.server).guest(p.getUUID())){VisitEvents.voiceFinished(p,v.sound);com.coraline.secrets.SecretDialogue.finished(p,v.sound);com.coraline.wybie.WybieVisitNetwork.finished(p,v.sound);com.coraline.bobinsky.BobinskyVisit.voiceFinished(p,v.sound);com.coraline.tea.TeaVisit.voiceFinished(p,v.sound);}}).add();
        CHANNEL.messageBuilder(SkipIntro.class,3,NetworkDirection.PLAY_TO_SERVER).encoder((v,b)->{b.writeUtf(v.sound);b.writeInt(v.generation);}).decoder(b->new SkipIntro(b.readUtf(128),b.readInt())).consumerMainThread((v,c)->{var p=c.get().getSender();if(p!=null)ActivityIntro.skip(p,v.sound,v.generation);}).add();
        CHANNEL.messageBuilder(LeaveActivity.class,5,NetworkDirection.PLAY_TO_SERVER).encoder((v,b)->{}).decoder(b->new LeaveActivity()).consumerMainThread((v,c)->{var p=c.get().getSender();if(p==null||!ActivityWorld.isActivity(p.level()))return;var s=OtherMotherLoop.active(p);if(s!=null&&s.owner.equals(p.getUUID())&&ActivityWorld.exitReady(s))ActivityWorld.exit(p,s);}).add();
    }
    public static int length(String name){
        if(lengths==null)try(var in=VisitAudio.class.getResourceAsStream("/assets/coraline/visit_audio_lengths.json")){lengths=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        catch(Exception e){lengths=new com.google.gson.JsonObject();com.mojang.logging.LogUtils.getLogger().error("Audio timing table unavailable; reading packaged recordings",e);}
        var value=lengths.get(name);
        if(value!=null&&value.isJsonPrimitive())try{int ticks=value.getAsInt();if(ticks>0)return ticks;}catch(RuntimeException ignored){}
        int ticks=recordingLength(name);lengths.addProperty(name,ticks);
        com.mojang.logging.LogUtils.getLogger().warn("Recovered missing audio duration for {}: {} ticks",name,ticks);
        return ticks;
    }
    private static int recordingLength(String name){
        try(var in=VisitAudio.class.getResourceAsStream("/assets/coraline/sounds.json")){
            var event=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject(name);
            int ticks=0;
            for(var entry:event.getAsJsonArray("sounds")){
                String id=entry.isJsonPrimitive()?entry.getAsString():entry.getAsJsonObject().get("name").getAsString();
                var resource=ResourceLocation.tryParse(id.contains(":")?id:"coraline:"+id);
                if(resource==null)continue;
                try(var sound=VisitAudio.class.getResourceAsStream("/assets/"+resource.getNamespace()+"/sounds/"+resource.getPath()+".ogg")){
                    if(sound!=null)ticks=Math.max(ticks,oggTicks(sound.readAllBytes()));
                }
            }
            if(ticks>0)return ticks;
        }catch(Exception e){com.mojang.logging.LogUtils.getLogger().error("Could not recover audio duration for "+name,e);}
        // A damaged/missing optional resource must not crash the server or trap the activity.
        return 200;
    }
    static int oggTicks(byte[] data){
        long samples=0;int rate=0;
        for(int offset=0;offset+27<=data.length;){
            if(data[offset]!='O'||data[offset+1]!='g'||data[offset+2]!='g'||data[offset+3]!='S')break;
            int segments=data[offset+26]&255,body=offset+27+segments;
            if(body>data.length)break;
            int size=0;for(int i=0;i<segments;i++)size+=data[offset+27+i]&255;
            if(body+size>data.length)break;
            if(rate==0&&size>=16&&data[body]==1&&data[body+1]=='v'&&data[body+2]=='o'&&data[body+3]=='r'&&data[body+4]=='b'&&data[body+5]=='i'&&data[body+6]=='s'){
                for(int i=0;i<4;i++)rate|=(data[body+12+i]&255)<<(8*i);
            }
            long granule=0;for(int i=0;i<8;i++)granule|=(data[offset+6+i]&255L)<<(8*i);
            if(granule>=0)samples=Math.max(samples,granule);
            offset=body+size;
        }
        return rate>0?(int)Math.ceil(samples*20.0/rate):0;
    }
    public static void play(ServerPlayer p,String name,boolean music){if(!music&&name.startsWith("other_mother_")){var s=OtherMotherLoop.scene(p);if(s!=null){s.loop.secrets.dialogue.putString("VoiceClockSound",name);s.loop.secrets.dialogue.putLong("VoiceClockEnd",System.currentTimeMillis()+length(name)*50L);s.loop.secrets.dialogue.putString("LoopVoice",name);s.loop.secrets.dialogue.putBoolean("LoopFinished",false);OtherMotherLoop.changed(p);}}storySend(p,new Cue(name,music,false));if(!music)ActivityIntro.offer(p,name);}
    public static void stopVoice(ServerPlayer p,String name){storySend(p,new Cue(name,false,true));}
    public static void stop(ServerPlayer p){storySend(p,new Cue("",true,true));}
    public static void stopVoice(ServerPlayer p){storySend(p,new Cue("",false,true));}
    public static void stopMusic(ServerPlayer p,String name){storySend(p,new Cue(name,true,true));}
}
