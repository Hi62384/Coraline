package com.coraline.feast;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Strict original-recording lookup. VisitAudio's 200-tick optional fallback is never used here. */
public final class WybieAdventureAudio {
    public record Recording(String sound,int ticks,String problem){public boolean ready(){return !sound.isEmpty()&&ticks>0&&problem.isEmpty();}}
    private static Recording cached;
    public static Recording introduction(){
        if(cached!=null)return cached;
        try(var in=WybieAdventureAudio.class.getResourceAsStream("/assets/coraline/sounds.json")){
            if(in==null)return cached=new Recording("",0,"The sound registry is missing.");
            var sounds=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var entry:sounds.entrySet())for(var sound:entry.getValue().getAsJsonObject().getAsJsonArray("sounds")){
                String resource=sound.isJsonPrimitive()?sound.getAsString():sound.getAsJsonObject().get("name").getAsString();
                String file=resource.substring(resource.lastIndexOf('/')+1).replaceAll("[^A-Za-z]","").toLowerCase(Locale.ROOT);
                if(!file.equals("wybieintroduce"))continue;
                String namespace=resource.contains(":")?resource.substring(0,resource.indexOf(':')):"coraline",path=resource.contains(":")?resource.substring(resource.indexOf(':')+1):resource;
                try(var audio=WybieAdventureAudio.class.getResourceAsStream("/assets/"+namespace+"/sounds/"+path+".ogg")){
                    if(audio==null)return cached=new Recording("",0,"The registered original WybieIntroduce recording is not packaged.");
                    int actual=VisitAudio.oggTicks(audio.readAllBytes());if(actual<=0)return cached=new Recording("",0,"The original WybieIntroduce recording has no valid audio duration.");
                    if(!VisitAudio.SOUNDS.containsKey(entry.getKey()))return cached=new Recording("",0,"The original WybieIntroduce sound is missing from VisitAudio registration.");
                    return cached=new Recording(entry.getKey(),actual,"");
                }
            }
            return cached=new Recording("",0,"Import the existing voicelines/othermother/extra/WybieIntroduce.wav through the audio pipeline. It is not registered; no substitute was used.");
        }catch(IOException|RuntimeException e){return cached=new Recording("",0,"Cannot validate the original WybieIntroduce recording: "+e.getMessage());}
    }
    public static void clear(){cached=null;}
    private WybieAdventureAudio(){}
}
