package com.coraline.feast;

import java.util.*;

/** Three-minute solo note-block arrangements; no backing recordings or autoplay. */
public final class FestivalChart {
    public static final int DURATION=3600, APPROACH=30, WINDOW=5;
    public record Note(int tick,int lane,int midi,List<Integer> tones){}
    public record Audio(int tick,List<Integer> tones,int prompt){}
    public record Track(String title,String resource,int duration,List<Note> notes,List<Audio> score){}
    public static double approach(double tick,int duration){return 23-9*Math.max(0,Math.min(1,tick/duration));}
    private static final List<Track> TRACKS=load();
    private static List<Track> load(){
        try(var in=FestivalChart.class.getResourceAsStream("/assets/coraline/festival_charts.json")){
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(java.util.Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            var tracks=new ArrayList<Track>();
            for(var entry:json){var o=entry.getAsJsonObject();var notes=new ArrayList<Note>();
                for(var row:o.getAsJsonArray("notes")){var v=row.getAsJsonArray();var tones=new ArrayList<Integer>();if(v.size()>3)for(var tone:v.get(3).getAsJsonArray())tones.add(tone.getAsInt());else tones.add(v.get(2).getAsInt());notes.add(new Note(v.get(0).getAsInt(),v.get(1).getAsInt(),v.get(2).getAsInt(),List.copyOf(tones)));}
                var score=new ArrayList<Audio>();if(o.has("score"))for(var row:o.getAsJsonArray("score")){var v=row.getAsJsonArray();var tones=new ArrayList<Integer>();for(var tone:v.get(1).getAsJsonArray())tones.add(tone.getAsInt());score.add(new Audio(v.get(0).getAsInt(),List.copyOf(tones),v.get(2).getAsInt()));}
                tracks.add(new Track(o.get("title").getAsString(),o.get("resource").getAsString(),Math.min(DURATION,o.get("duration").getAsInt()),List.copyOf(notes.stream().filter(n->n.tick()<DURATION-WINDOW-2).toList()),List.copyOf(score)));
            }return List.copyOf(tracks);
        }catch(Exception ex){throw new IllegalStateException("Missing festival charts",ex);}
    }
    public static int count(){return TRACKS.size();}
    public static Track track(int index){return TRACKS.get(Math.floorMod(index,count()));}
    public static int choose(long seed,int previous){
        if(previous<0||previous>=count())return new Random(seed).nextInt(count());
        int index=new Random(seed).nextInt(count()-1);
        return previous>=0&&previous<count()&&index>=previous?index+1:index;
    }
    public static List<Note> generate(long seed){return track(Math.floorMod((int)seed,count())).notes();}
    public static float notePitch(int midi){return (float)Math.pow(2,(midi-66)/12.0);}
    public static int[] pack(List<Note> notes){return notes.stream().mapToInt(n->n.tick*4+n.lane).toArray();}
    public static final class Score {
        public int streak,hits,perfect,misses,wrong,crowd=6,peak=6;
        public void hit(boolean exact){hits++;streak++;if(exact)perfect++;if(streak%2==0)crowd=Math.min(48,crowd+(exact?2:1));peak=Math.max(peak,crowd);}
        public void miss(boolean spam){streak=0;if(spam)wrong++;else misses++;crowd=Math.max(0,crowd-(spam?3:2));}
        public int accuracy(){return hits*100/Math.max(1,hits+misses+wrong);}
        public int tier(){return crowd<5?0:crowd<13?1:crowd<26?2:crowd<41?3:4;}
    }
    public static float pitch(int lane){return (float)Math.pow(2,new int[]{0,3,7,10}[Math.floorMod(lane,4)]/12.0-.5);}
    private FestivalChart(){}
}
