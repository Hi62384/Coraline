package com.coraline.feast;

/** Shared server-collision/client-mesh coordinates. No player position enters hearing AI. */
public final class RescueWebLayout {
    public static int weightedHeight(int rest,double distance,boolean bearing){int safe=Math.max(1,Math.min(16,rest));if(!bearing||!Double.isFinite(distance)||distance>=2.5)return safe;double weight=Math.max(0,1-distance*distance/6.25);return Math.max(1,safe-(int)Math.round(2*weight));}
    public static boolean audibleLanding(double previousY,double currentY,boolean landed){return landed&&previousY-currentY>.18;}
    /** Vanilla land-crawl is about 0.065 m/tick; pursuit must be slower, not an unavoidable catch. */
    public static double crawlPursuit(int ticks){return -8+Math.max(0,ticks)*.05;}
    public record Point(double x,double y,double z){}
    public static final int LANES=3,SAMPLES=900;
    public static Point point(int lane,double progress){
        double u=Math.max(0,Math.min(1,progress)),angle=u*Math.PI*3,r=28-u*14;
        // Three routes meet at both ends, and at five cross-threads.
        double split=Math.sin(Math.PI*u)*(lane-1)*2.2;
        double radius=r+split;
        return new Point(Math.cos(angle)*radius,8+u*9-Math.sin(Math.PI*u)*2.1,Math.sin(angle)*radius);
    }
    public static boolean sticky(int lane,int sample){return lane==0&&sample>170&&sample<265||lane==2&&sample>540&&sample<665;}
    public static double distance(double x,double z){
        double best=Double.POSITIVE_INFINITY;
        for(int lane=0;lane<LANES;lane++)for(int i=0;i<=SAMPLES;i+=2){var p=point(lane,i/(double)SAMPLES);best=Math.min(best,Math.hypot(x-p.x,z-p.z));}
        return best;
    }
    /** Five-to-ten seconds, even with accessibility gain or forged click floods. */
    public static boolean struggleFinished(int ticks,float progress){return ticks>=100&&ticks<=240&&progress>=1;}
    /** A missing recording must not disable the Cat swat or strand FINAL_N.
     * Keep the real cue duration unmodified when packaged audio exists. */
    static int catSwatDuration(int recordedTicks){return Math.max(120,recordedTicks);}
    /** N offsets are from the real end, never from guessed voice timestamps. */
    public static boolean blindAt(int time,int duration){return duration>=80&&time>=duration-80;}
    public static boolean swatAt(int time,int duration){return duration>=40&&time>=duration-40;}
    public static double arc(double start,double end,double time,double height){double u=Math.max(0,Math.min(1,time));return start+(end-start)*u+Math.sin(Math.PI*u)*height;}
    private RescueWebLayout(){}
}
