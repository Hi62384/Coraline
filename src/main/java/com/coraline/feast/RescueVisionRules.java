package com.coraline.feast;

/**
 * Dependency-free, directly testable survival-vital restoration rules for the
 * post-boss ghost-child dream. An interrupted or malformed NBT record must not
 * set player health to zero or NaN when returning to the original Overworld.
 */
public final class RescueVisionRules {
    public static float restoredHealth(boolean hasSaved,float saved,float current,float maximum){
        float cap=Float.isFinite(maximum)&&maximum>0?maximum:20;
        float value=hasSaved&&Float.isFinite(saved)&&saved>0?saved
                :Float.isFinite(current)&&current>0?current:1;
        return Math.max(Math.min(cap,0.01F),Math.min(cap,value));
    }
    public static int restoredFood(boolean hasSaved,double saved,int current){
        return hasSaved&&Double.isFinite(saved)&&saved>=0&&saved<=20&&saved==Math.floor(saved)
                ?(int)saved:Math.max(0,Math.min(20,current));
    }
    public static float restoredSaturation(boolean hasSaved,float saved,float current,int food){
        float value=hasSaved&&Float.isFinite(saved)&&saved>=0?saved
                :Float.isFinite(current)&&current>=0?current:0;
        return Math.max(0,Math.min(Math.max(0,food),value));
    }
    private RescueVisionRules(){}
}
