package com.coraline.feast;

/** Mixer deadlines use elapsed monotonic time, never a server's catch-up tick burst. */
public final class StoryPlaybackClock {
    public static int elapsedTicks(long start,long now){return (int)Math.min(Integer.MAX_VALUE,Math.max(0,now-start)/50_000_000L);}
    public static boolean complete(long start,long now,int duration){return duration>=0&&elapsedTicks(start,now)>=duration;}
    public static int endWaitTicks(long start,long now,int duration){return Math.max(0,elapsedTicks(start,now)-Math.max(0,duration));}
    private StoryPlaybackClock(){}
}
