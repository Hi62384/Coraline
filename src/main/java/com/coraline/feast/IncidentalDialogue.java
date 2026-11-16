package com.coraline.feast;

import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exact authored parent remarks, not GUI instructions or another speaker's voice. */
public final class IncidentalDialogue {
    private static final Map<String,String> LINES=load();
    private static Map<String,String> load(){
        var lines=new LinkedHashMap<String,String>();
        try(var in=Objects.requireNonNull(IncidentalDialogue.class.getResourceAsStream("/assets/coraline/incidental_dialogue.json"));
            var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){
            for(var value:JsonParser.parseReader(reader).getAsJsonArray()){
                var row=value.getAsJsonObject();String who=row.get("speaker").getAsString(),text=row.get("text").getAsString(),sound=row.get("sound").getAsString();
                if(!Set.of("Other Mother","Other Father").contains(who)||text.isBlank()||sound.isBlank()||lines.putIfAbsent(key(who,text),sound)!=null)
                    throw new IllegalStateException("Invalid/duplicate incidental parent dialogue");
            }
        }catch(java.io.IOException e){throw new IllegalStateException("Incidental dialogue catalogue unavailable",e);}
        return Collections.unmodifiableMap(lines);
    }
    private static String key(String who,String text){return who+"\u0000"+text;}
    static String sound(String who,String text){return LINES.get(key(who,text));}
    static Collection<String> soundIds(){return LINES.values();}
    public static boolean say(ServerPlayer p,String who,String text){
        String sound=sound(who,text);if(sound==null)return false;
        VisitAudio.play(p,sound,false);return true;
    }
    private IncidentalDialogue(){}
}
