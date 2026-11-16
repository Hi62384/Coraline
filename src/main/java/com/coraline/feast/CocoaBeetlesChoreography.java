package com.coraline.feast;

/** Shared movie-reference beats, in game ticks; recordings are never shortened. */
public final class CocoaBeetlesChoreography {
    public static final int WARDROBE_BLOCKED=70, MUSIC_START=WARDROBE_BLOCKED+4*20;
    public static float ramp(double tick,double start,double length){
        double t=Math.max(0,Math.min(1,(tick-start)/length));return (float)(t*t*(3-2*t));
    }
    public static float wardrobe(double tick){return ramp(tick,0,WARDROBE_BLOCKED);}
    public static boolean music(double tick,boolean rejected){return tick>=MUSIC_START&&!rejected;}
    public static float reveal(double tick,int fixture){return ramp(tick,80+fixture*10,20);}
    public static float lampLean(double tick){return ramp(tick,0,45)*(1-ramp(tick,115,45));}
    public static float trayOpen(double tick){return ramp(tick,35,30);}
    public static boolean beetleTaken(double tick,int duration){return tick>=Math.max(65,duration-55);}
    public static float biteLift(String phase,double tick,int duration){return phase.equals("COCOA_B")?ramp(tick,Math.max(65,duration-55),55):phase.equals("COCOA_BITE")?1:0;}
    public static float beetleRemaining(String phase,double tick){return phase.equals("COCOA_BITE")?1-ramp(tick,20,9):1;}
    public static float sconce(double tick,int index){return ramp(tick,86+index*18,22);}
    /** Neck/head leads; torso follows, then long arms, then the lower silhouette. */
    public static float morph(float progress,String bone){
        return morph(progress,morphRegion(bone));
    }
    public static int morphRegion(String bone){return bone.equals("head")?0:bone.contains("arm")?2:bone.contains("leg")||bone.contains("shin")?3:1;}
    public static float morph(float progress,int region){return switch(region){case 0->ramp(progress,0,.60);case 2->ramp(progress,.32,.68);case 3->ramp(progress,.24,.76);default->ramp(progress,.12,.78);};}
    private CocoaBeetlesChoreography(){}
}
