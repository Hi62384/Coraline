package com.coraline.palace;

import java.util.Optional;

/** Bounded, deterministic proposals only. Never loads chunks or edits terrain. */
public final class PalaceSitePlan {
    public static final int MIN_DISTANCE=1000,MAX_DISTANCE=10000,REGION_RADIUS=360,MAX_CANDIDATES=4096;
    public static final int WORLD_LIMIT=29_999_984;
    public enum Kind { PALACE,COMPANY,WELL }
    public record Point(int x,int z) {
        public Point {if(Math.abs((long)x)>WORLD_LIMIT||Math.abs((long)z)>WORLD_LIMIT)throw new IllegalArgumentException("Outside world bounds");}
        public double distance(Point other){return Math.hypot((double)x-other.x,(double)z-other.z);}
    }
    private static long mix(long value){value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;return value^(value>>>31);}
    private static double unit(long value){return (mix(value)>>>11)*0x1.0p-53;}
    public static Optional<Point> candidate(long seed,Point spawn,Kind kind,int index,Point palace){
        if(index<0||index>=MAX_CANDIDATES)throw new IllegalArgumentException("Candidate budget exhausted");
        if(kind==null||spawn==null||kind==Kind.WELL&&palace==null)throw new IllegalArgumentException("Missing landmark origin");
        long salt=seed^((long)index*0x9e3779b97f4a7c15L)^switch(kind){case PALACE->0x50414c414345L;case COMPANY->0x434f4d50414e59L;case WELL->0x57454c4cL;};
        double angle=unit(salt)*Math.PI*2;
        // Leave room around the selected palace; rounding cannot put its
        // anchor on the wrong side of the mandatory spawn-distance limits.
        double radius=switch(kind){case PALACE->1400+unit(salt+7)*8000;case COMPANY->800+unit(salt+7)*3200;case WELL->250+unit(salt+7)*45;};
        Point origin=kind==Kind.WELL?palace:spawn;
        long x=origin.x+Math.round(Math.cos(angle)*radius),z=origin.z+Math.round(Math.sin(angle)*radius);
        int margin=kind==Kind.PALACE?450:kind==Kind.COMPANY?64:48;
        if(Math.abs(x)>WORLD_LIMIT-margin||Math.abs(z)>WORLD_LIMIT-margin)return Optional.empty();
        var point=new Point((int)x,(int)z);
        if(kind==Kind.COMPANY&&palace!=null&&point.distance(palace)<600)return Optional.empty();
        return Optional.of(point);
    }
    public static boolean validPalaceDistance(Point spawn,Point palace){double distance=spawn.distance(palace);return distance>=MIN_DISTANCE&&distance<=MAX_DISTANCE;}
    /** A star-shaped connected region, with a soft 64 m outer transition.
     * This is an eventual regional placement weight, not a global biome sampler.
     */
    public static double regionWeight(long seed,Point palace,double x,double z){
        if(palace==null||!Double.isFinite(x)||!Double.isFinite(z))throw new IllegalArgumentException("Invalid region sample");
        double dx=x-palace.x,dz=z-palace.z,angle=Math.atan2(dz,dx);
        double edge=REGION_RADIUS*(.94+.035*Math.sin(angle*3+unit(seed)*Math.PI*2)+.025*Math.cos(angle*5+unit(seed+1)*Math.PI*2));
        double t=Math.max(0,Math.min(1,(edge-Math.hypot(dx,dz))/64));return t*t*(3-2*t);
    }
    private PalaceSitePlan(){}
}
