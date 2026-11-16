package com.coraline.feast;

import com.google.gson.*;
import com.coraline.secrets.SecretDialogue;
import net.minecraft.server.level.ServerPlayer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exact authored text and recording identities for the additive numbered events. */
public final class NumberedEventDialogue {
    public record Line(String speaker,String text,String sound){}
    private static final Map<String,Line> LINES=load();
    private static final Map<String,Integer> DURATIONS=new HashMap<>();
    private static JsonObject recordings;
    private static Map<String,Line> load(){
        try(var in=Objects.requireNonNull(NumberedEventDialogue.class.getResourceAsStream("/assets/coraline/numbered_event_dialogue.json"));var reader=new InputStreamReader(in,StandardCharsets.UTF_8)){
            var result=new LinkedHashMap<String,Line>();
            for(var e:JsonParser.parseReader(reader).getAsJsonObject().entrySet()){
                var n=e.getValue().getAsJsonObject();result.put(e.getKey(),new Line(n.get("speaker").getAsString(),n.get("text").getAsString(),n.has("sound")?n.get("sound").getAsString():""));
            }return Collections.unmodifiableMap(result);
        }catch(IOException e){throw new IllegalStateException("Numbered dialogue catalog unavailable",e);}
    }
    public static Line line(String event,String id){return Objects.requireNonNull(LINES.get(event+"/"+id),"Unknown numbered dialogue: "+event+"/"+id);}
    public static Collection<String> soundIds(){return LINES.values().stream().map(Line::sound).filter(s->!s.isEmpty()).distinct().toList();}
    /** New events cannot silently substitute VisitAudio's optional-resource fallback. */
    public static boolean ready(String event){
        boolean found=false;
        for(var entry:LINES.entrySet())if(entry.getKey().startsWith(event+"/")){
            found=true;if(!entry.getValue().sound.isEmpty()&&recordedTicks(entry.getValue().sound)<=0)return false;
        }
        return found;
    }
    public static int recordedTicks(String sound){
        if(DURATIONS.containsKey(sound))return DURATIONS.get(sound);
        if(!VisitAudio.SOUNDS.containsKey(sound))return 0;
        try{
            if(recordings==null)try(var in=NumberedEventDialogue.class.getResourceAsStream("/assets/coraline/sounds.json")){
                if(in==null)return 0;recordings=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            }
            var event=recordings.getAsJsonObject(sound);if(event==null)return 0;var entries=event.getAsJsonArray("sounds");if(entries==null||entries.size()!=1)return 0;
            var entry=entries.get(0);String path=entry.isJsonPrimitive()?entry.getAsString():entry.getAsJsonObject().get("name").getAsString();
            var id=net.minecraft.resources.ResourceLocation.tryParse(path.contains(":")?path:"coraline:"+path);if(id==null)return 0;
            try(var in=NumberedEventDialogue.class.getResourceAsStream("/assets/"+id.getNamespace()+"/sounds/"+id.getPath()+".ogg")){
                if(in==null)return 0;byte[] audio=in.readNBytes(16*1024*1024+1);if(audio.length>16*1024*1024)return 0;
                int ticks=VisitAudio.oggTicks(audio);if(ticks>0)DURATIONS.put(sound,ticks);return ticks;
            }
        }catch(IOException|RuntimeException invalid){return 0;}
    }
    /** Caption-only playback is reserved for the explicitly declared text-only voices. */
    public static int say(ServerPlayer p,String event,String id){
        var line=line(event,id);int ticks=line.sound.isEmpty()?Math.max(60,Math.min(480,line.text.length()*2)):recordedTicks(line.sound);
        if(ticks<=0)return 0;
        if(!line.sound.isEmpty())VisitAudio.play(p,line.sound,false);
        else VisitAudio.caption(p,line.speaker,line.text,ticks);
        return ticks;
    }
    public static int mother(ServerPlayer p,String event,String prefix){return say(p,event,prefix+"_"+SecretDialogue.stateId(SecretDialogue.state(p))+"_"+(1+p.getRandom().nextInt(2)));}
    private NumberedEventDialogue(){}
}
