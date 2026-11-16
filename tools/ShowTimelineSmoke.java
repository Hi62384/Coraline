package com.coraline.feast;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/** Pure show geometry/cue regressions using stored original durations; no game world or audio claim. */
public final class ShowTimelineSmoke {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static long cue(ScriptedShowTimeline plan,String id){return plan.cues().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow().tick();}
    public static void main(String[] args)throws Exception{
        var lengths=new HashMap<String,Integer>();
        try(var reader=Files.newBufferedReader(Path.of("src/main/resources/assets/coraline/numbered_original_audio.json"))){
            for(var entry:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("recordings").entrySet()){
                var media=entry.getValue().getAsJsonObject();int rate=media.get("sample_rate").getAsInt();long frames=media.get("samples").getAsLong();int ticks=(int)((frames*20+rate-1)/rate);
                require(ticks==media.get("ticks").getAsInt(),"Stored frame/tick mismatch: "+entry.getKey());lengths.put(entry.getKey(),ticks);
            }
        }
        var theatre=ScriptedShowTimeline.theatre(lengths);var circus=ScriptedShowTimeline.circus(lengths);
        require(cue(theatre,"D_FREEZE")==theatre.segment("D").start()+50,"D+2.5s freeze");
        require(cue(theatre,"E_PLAYER_APPROACH")==theatre.segment("E").end()-20,"E final-second approach");
        require(cue(theatre,"E_PLAYER_GRAB")==theatre.segment("E").end(),"E end grab");
        require(cue(theatre,"F_THROW")==theatre.segment("F").start()+480,"F+24s throw");
        require(cue(theatre,"F_RESCUE")<theatre.segment("F").end(),"Rescue exceeds original F duration");
        require(cue(circus,"INTRO_REVEAL")==circus.segment("INTRO").end()-60,"Intro final-three-second reveal");
        require(cue(circus,"MUSIC_END")==circus.segment("MUSIC").end(),"Circus extends beyond music");
        require(cue(circus,"BOBINSKY_APPEAR")==cue(circus,"MUSIC_END"),"Bobinsky prematurely visible");
        for(var plan:List.of(theatre,circus)){
            for(var segment:plan.segments()){
                require(plan.at(segment.start()).orElseThrow().equals(segment),"Wrong active segment at start");
                require(plan.at(segment.end()-1).orElseThrow().equals(segment),"Wrong active segment before end");
            }
            long last=-1;for(var c:plan.cues()){require(c.tick()>=last,"Unordered cue");last=c.tick();}
            boolean rejected=false;try{plan.crossed(20,10);}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Backward authoritative timeline accepted");
        }
        for(int tick=0;tick<=4000;tick++){
            var a=TheatreOpeningChoreography.position(-1,tick/4000.0);var b=TheatreOpeningChoreography.position(1,tick/4000.0);
            for(var point:List.of(a,b))require(Double.isFinite(point.x())&&Double.isFinite(point.y())&&Double.isFinite(point.z())&&Math.abs(point.x())<=18.001&&point.y()>=1&&point.y()<=2.801&&point.z()>=-39.001&&point.z()<=-25.999,"Opening leaves stage");
            require(b.x()-a.x()>=5.999,"Opening performers collide");
        }
        for(double boundary:new double[]{.16,.55,.82})for(int side:new int[]{-1,1}){
            var a=TheatreOpeningChoreography.position(side,boundary-.000001);var b=TheatreOpeningChoreography.position(side,boundary+.000001);
            require(Math.sqrt(Math.pow(a.x()-b.x(),2)+Math.pow(a.y()-b.y(),2)+Math.pow(a.z()-b.z(),2))<.002,"Opening act discontinuity");
        }
        for(int tick=0;tick<=lengths.get("theatre_f");tick++){
            var point=ScriptedShowTimeline.theatrePlayer(tick,lengths.get("theatre_f"));require(Double.isFinite(point.x())&&Double.isFinite(point.y())&&Double.isFinite(point.z())&&point.y()>=.25,"Unsafe stunt position");
        }
        require(ScriptedShowTimeline.theatrePlayer(540,lengths.get("theatre_f")).y()==1.05,"Rescue not above pool");
        for(int index=0;index<48;index++){
            require(ScriptedShowTimeline.mouse(index,48,1).equals(ScriptedShowTimeline.star(index,48)),"Final mouse pose differs at music end");
            for(int tick=0;tick<=1000;tick++){
                var point=ScriptedShowTimeline.mouse(index,48,tick/1000.0);require(Double.isFinite(point.x())&&Double.isFinite(point.y())&&Double.isFinite(point.z())&&Math.abs(point.x())<=20&&Math.abs(point.z())<=20&&point.y()>=0&&point.y()<=20,"Mouse outside bounded choreography");
            }
        }
        var missing=new HashMap<>(lengths);missing.remove("theatre_music");boolean rejected=false;try{ScriptedShowTimeline.theatre(missing);}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Missing music was guessed");
        var shortF=new HashMap<>(lengths);shortF.put("theatre_f",479);rejected=false;try{ScriptedShowTimeline.theatre(shortF);}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Incompatible F duration accepted");
        System.out.println("SHOW_TIMELINE_SMOKE_OK: "+checks+" assertions; stored original durations, exact cue offsets and bounded choreography. NOT native audio/visual acceptance.");
    }
}
