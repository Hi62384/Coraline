package com.coraline.feast;

import java.util.*;

/** Pure, deterministic cue plan. Durations are measured recording ticks, never estimates. */
public final class ScriptedShowTimeline {
    public enum Show { THEATRE, MOUSE_CIRCUS }
    public record Cue(String id,long tick) {}
    public record Segment(String id,String sound,long start,int duration,boolean music) {
        public long end(){return start+duration;}
    }
    public record Point(double x,double y,double z,float pitch,float roll) {
        public Point blend(Point b,double t){t=smooth(t);return new Point(x+(b.x-x)*t,y+(b.y-y)*t,z+(b.z-z)*t,(float)(pitch+(b.pitch-pitch)*t),(float)(roll+(b.roll-roll)*t));}
    }
    private final List<Cue> cues;
    private final List<Segment> segments;
    private final long duration;
    private ScriptedShowTimeline(List<Cue> cues,List<Segment> segments){this.cues=List.copyOf(cues.stream().sorted(Comparator.comparingLong(Cue::tick)).toList());this.segments=List.copyOf(segments);duration=segments.get(segments.size()-1).end();}
    public List<Cue> cues(){return cues;}
    public List<Segment> segments(){return segments;}
    public long duration(){return duration;}
    public Segment segment(String id){return segments.stream().filter(s->s.id.equals(id)).findFirst().orElseThrow();}
    public Optional<Segment> at(long tick){return segments.stream().filter(s->tick>=s.start&&tick<s.end()).findFirst();}
    public List<Cue> crossed(long previous,long current){if(current<previous)throw new IllegalArgumentException("Timeline cannot run backwards");return cues.stream().filter(c->c.tick>previous&&c.tick<=current).toList();}
    public String cue(long tick){String result="READY";for(var c:cues){if(c.tick>tick)break;result=c.id;}return result;}
    private static int require(Map<String,Integer> lengths,String sound,int minimum){Integer n=lengths.get(sound);if(n==null||n<minimum)throw new IllegalArgumentException("Missing/incompatible measured duration: "+sound+" requires at least "+minimum+" ticks");return n;}
    public static ScriptedShowTimeline theatre(Map<String,Integer> lengths){
        var c=new ArrayList<Cue>();var s=new ArrayList<Segment>();long t=0;
        int music=require(lengths,"theatre_music",1);s.add(new Segment("MUSIC","theatre_music",t,music,true));c.add(new Cue("THEATRE_MUSIC_START",t));t+=music;c.add(new Cue("THEATRE_MUSIC_END",t));t+=80;c.add(new Cue("ACT_TWO_REVEAL",t));
        for(String letter:List.of("a","b","c","d","e","f")){
            String sound="theatre_"+letter;int minimum=letter.equals("d")?51:letter.equals("e")?21:letter.equals("f")?560:1;
            int n=require(lengths,sound,minimum);String id=letter.toUpperCase(Locale.ROOT);s.add(new Segment(id,sound,t,n,false));c.add(new Cue(id+"_START",t));
            if(letter.equals("c"))c.add(new Cue("C_UNZIP",t));
            if(letter.equals("d")){c.add(new Cue("D_UNZIP",t));c.add(new Cue("D_FREEZE",t+50));}
            if(letter.equals("e")){c.add(new Cue("E_TRAPEZE",t));c.add(new Cue("E_PLAYER_APPROACH",t+n-20));c.add(new Cue("E_PLAYER_GRAB",t+n));}
            if(letter.equals("f")){c.add(new Cue("F_SWING",t));c.add(new Cue("F_THROW",t+480));c.add(new Cue("F_RESCUE",t+540));}
            t+=n;c.add(new Cue(id+"_END",t));
        }
        c.add(new Cue("F_FINISH",t));c.add(new Cue("ROSE_TOSS",t));return new ScriptedShowTimeline(c,s);
    }
    public static ScriptedShowTimeline circus(Map<String,Integer> lengths){
        int intro=require(lengths,"mouse_circus_intro",61),music=require(lengths,"mouse_circus_music",100),end=require(lengths,"mouse_circus_end",1);
        var s=List.of(new Segment("INTRO","mouse_circus_intro",0,intro,false),new Segment("MUSIC","mouse_circus_music",intro,music,true),new Segment("ENDING","mouse_circus_end",intro+music,end,false));
        var c=new ArrayList<Cue>();c.add(new Cue("INTRO_START",0));c.add(new Cue("INTRO_REVEAL",intro-60));c.add(new Cue("INTRO_END",intro));c.add(new Cue("MUSIC_START",intro));
        String[] acts={"MOUSE_PARADE","MOUSE_ACROBATICS","MOUSE_BALANCE","MOUSE_AERIAL","MOUSE_FINALE"};
        for(int i=0;i<acts.length;i++)c.add(new Cue(acts[i],intro+(long)music*i/5));
        c.add(new Cue("MUSIC_END",intro+music));c.add(new Cue("BOBINSKY_APPEAR",intro+music));c.add(new Cue("BOBINSKY_APPROACH",intro+music));c.add(new Cue("BOBINSKY_END_DIALOGUE",intro+music));c.add(new Cue("EXIT_READY",intro+music+end));return new ScriptedShowTimeline(c,s);
    }
    /** Forty-eight bounded, non-AI actors, star plus orbit. Index is stable across reloads. */
    public static Point star(int index,int count){
        if(index>=count*3/4){double a=2*Math.PI*(index-count*3/4)/(count-count*3/4);return new Point(Math.cos(a)*14,.1,Math.sin(a)*14,0,0);}
        double u=(double)index/(count*3/4)*10;int edge=(int)u;double q=u-edge,a=-Math.PI/2+edge*Math.PI/5,b=a+Math.PI/5;double ra=edge%2==0?10:4.3,rb=edge%2==0?4.3:10;
        return new Point(Math.cos(a)*ra*(1-q)+Math.cos(b)*rb*q,.1,Math.sin(a)*ra*(1-q)+Math.sin(b)*rb*q,0,0);
    }
    /** Analytic motion sampled on the audio cursor; endpoints are exact and no random AI runs. */
    public static Point mouse(int index,int count,double progress){
        progress=clamp(progress);Point finish=star(index,count);if(progress>=1)return finish;
        double phase=progress*5;int act=Math.min(4,(int)phase);double u=phase-act,a=index*2*Math.PI/count;
        Point raw=switch(act){
            case 0 -> new Point(Math.cos(a+u*2*Math.PI)*(8+3*Math.sin(a*3)),.12,Math.sin(a+u*2*Math.PI)*(8+3*Math.sin(a*3)),0,0);
            case 1 -> {double aJump=u*6*Math.PI+(index%6)*Math.PI/3;double lane=(index%8-3.5)*2.2;yield new Point(lane,1.1+Math.abs(Math.cos(aJump))*4,Math.sin(aJump)*8,(float)Math.toDegrees(aJump),0);}
            case 2 -> {double q=(Math.sin((u+index/(double)count)*2*Math.PI)+1)/2;yield new Point(-12+24*q,4+Math.sin(q*2*Math.PI)*.13,(index%6-2.5)*3.0,0,(float)(Math.sin(q*8*Math.PI)*9));}
            case 3 -> {double q=(u*2+(index%8)/8.0)%1;double swing=Math.sin(q*2*Math.PI)*.9;yield new Point((index%8-3.5)*2.6,16-Math.cos(swing)*8,Math.sin(swing)*8,0,(float)(-Math.toDegrees(swing)));}
            default -> {double turn=(1-u)*2*Math.PI;yield new Point(Math.cos(a+turn)*(14-8*u),Math.sin(Math.PI*u)*5,Math.sin(a+turn)*(14-8*u),0,0);}
        };
        // Blend act boundaries, landing and finale instead of discontinuous position teleports.
        if(act==0&&u<.12)return finish.blend(raw,u/.12);
        if(act==4&&u>=.10)return raw.blend(finish,Math.max(0,(u-.45)/.55));
        if(u<.10){Point previous=mouse(index,count,Math.max(0,act/5.0-.000001));return previous.blend(raw,u/.10);}
        return raw;
    }
    public static Point theatrePlayer(int fTicks,int fDuration){
        if(fDuration<560)throw new IllegalArgumentException("F recording cannot contain mandatory 24-second throw and rescue");
        if(fTicks<480){double a=fTicks*Math.PI/100;Point swing=new Point(Math.sin(a)*10,12+Math.cos(a)*4,Math.cos(a)*7-18,0,0);if(fTicks<60)return new Point(-2,2.25,18,0,0).blend(swing,fTicks/60.0);if(fTicks>420)return swing.blend(new Point(0,18,-25,0,0),(fTicks-420)/60.0);return swing;}
        if(fTicks<540){double u=smooth((fTicks-480)/60.0);return new Point(0,18*(1-u)+1.05*u,-25,0,0);}
        return new Point(0,1.05,-25,0,0);
    }
    public static double clamp(double t){return Math.max(0,Math.min(1,t));}
    public static double smooth(double t){t=clamp(t);return t*t*(3-2*t);}
    private ScriptedShowTimeline(){throw new AssertionError();}
}
