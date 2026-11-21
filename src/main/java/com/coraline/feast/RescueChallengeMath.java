package com.coraline.feast;

/** Deterministic challenge rules used by runtime and lightweight regressions. */
public final class RescueChallengeMath {
    public static int tier(float health,float maximum){return health>maximum*.67F?0:health>maximum*.34F?1:2;}
    public static int attack(int clock,int tier,int patterns){return Math.floorMod(clock/(180-tier*20),patterns);}
    public static int beat(int clock,int tier){return Math.floorMod(clock,180-tier*20);}
    public static boolean vulnerable(int clock,int tier){return beat(clock,tier)>=100;}
    public static float struggle(float progress,int clicks,boolean accessible){return Math.max(0,Math.min(1,progress+Math.max(0,Math.min(1,clicks))*(accessible?.045F:.034F)-.0015F));}
    public static double pursuit(int clock,int duration,double length){return length*Math.max(0,Math.min(1,clock/(double)Math.max(1,duration)));}
    public static double collapse(int clock,int duration,double start,double refuge){return start+(refuge-start)*Math.max(0,Math.min(1,clock/(double)Math.max(1,duration)));}
    /** A saved pickup placement is not evidence that the original Cat is still reachable.
     * Only re-stage before the player has rescued it; callers still verify the
     * actor UUID, target footing and collision before moving the SAME Cat. */
    public static boolean restageUnrescuedCat(boolean placed,boolean rescued,double distanceSquared,boolean obstructed){
        return !rescued&&(!placed||obstructed||!Double.isFinite(distanceSquared)||distanceSquared>64);
    }
    public static boolean hears(double moved,boolean crouching,boolean sprinting,boolean landing,boolean sticky){return landing||sticky&&moved>.005||moved>(sprinting?.025:crouching?.18:.075);}
    /** Facial emotion follows the heard farewell, even while the server's cue packet is late. */
    public static boolean farewellSad(int cue,boolean farewell,int recordingTick){return cue>=3&&(cue==5||farewell&&recordingTick>=240);}
    /** Preserve the loudest accepted step: extra quiet/zero packets cannot dilute it. */
    public static final class StepBatch {
        private double distance,loudest;private int count;
        public void record(double step){if(!Double.isFinite(step)||step<0){distance=loudest=Double.POSITIVE_INFINITY;count++;return;}if(step<=1e-7)return;distance+=step;loudest=Math.max(loudest,step);count++;}
        public double distance(){return distance;}
        public double loudestWithUnaccounted(double observed){return !Double.isFinite(observed)||observed<0?Double.POSITIVE_INFINITY:Math.max(loudest,Math.max(0,observed-distance));}
        public int count(){return count;}
    }
    private RescueChallengeMath(){}
}
