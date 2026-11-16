package com.coraline.feast;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original identity and packaged-granule timing. Missing originals block the show, not the player. */
public final class ShowRecordings {
    public static final Map<String,String> SOURCES=Map.ofEntries(
        Map.entry("theatre_music","music/eventbased/Theatre.mp3"),
        Map.entry("theatre_a","voicelines/MissSpinkAndForcible/other/A.wav"),Map.entry("theatre_b","voicelines/MissSpinkAndForcible/other/B.wav"),
        Map.entry("theatre_c","voicelines/MissSpinkAndForcible/other/C.wav"),Map.entry("theatre_d","voicelines/MissSpinkAndForcible/other/D.wav"),
        Map.entry("theatre_e","voicelines/MissSpinkAndForcible/other/E.wav"),Map.entry("theatre_f","voicelines/MissSpinkAndForcible/other/F.wav"),
        Map.entry("theatre_meet_parents","voicelines/MissSpinkAndForcible/other/MeetParentsAfterEnd.wav"),
        Map.entry("mouse_circus_invite","voicelines/othermother/extra/BobinskyMiceCircusInvite.wav"),
        Map.entry("mouse_circus_intro","voicelines/bobinsky/other/CircusIntroduction.wav"),
        Map.entry("mouse_circus_music","music/eventbased/micecircus.mp3"),Map.entry("mouse_circus_end","voicelines/bobinsky/other/CircusEnd.wav"));
    private static final Map<String,Integer> MEASURED=new HashMap<>();
    public static int duration(String key){
        if(MEASURED.containsKey(key))return MEASURED.get(key);
        if(!VisitAudio.SOUNDS.containsKey(key))return 0;
        try(var in=ShowRecordings.class.getResourceAsStream("/assets/coraline/sounds.json")){
            if(in==null)return 0;var event=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject(key);
            if(event==null||event.getAsJsonArray("sounds").size()!=1)return 0;
            var entry=event.getAsJsonArray("sounds").get(0);String path=entry.isJsonPrimitive()?entry.getAsString():entry.getAsJsonObject().get("name").getAsString();
            var id=ResourceLocation.tryParse(path.contains(":")?path:"coraline:"+path);if(id==null)return 0;
            try(var recording=ShowRecordings.class.getResourceAsStream("/assets/"+id.getNamespace()+"/sounds/"+id.getPath()+".ogg")){
                if(recording==null)return 0;int n=VisitAudio.oggTicks(recording.readAllBytes());if(n>0)MEASURED.put(key,n);return n;
            }
        }catch(IOException|RuntimeException bad){return 0;}
    }
    public static Map<String,Integer> measured(){var result=new HashMap<String,Integer>();for(String key:SOURCES.keySet()){int ticks=duration(key);if(ticks>0)result.put(key,ticks);}return result;}
    public static String blocker(boolean theatre){
        for(var e:SOURCES.entrySet())if(e.getKey().startsWith(theatre?"theatre_":"mouse_circus_")&&duration(e.getKey())<=0)return "Original recording is not ready: "+e.getValue();
        try{if(theatre)ScriptedShowTimeline.theatre(measured());else ScriptedShowTimeline.circus(measured());}catch(IllegalArgumentException conflict){return conflict.getMessage();}return "";
    }
    private ShowRecordings(){}
}
