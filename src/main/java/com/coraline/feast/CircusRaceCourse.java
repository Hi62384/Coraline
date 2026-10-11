package com.coraline.feast;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** A continuous, authored obstacle route. Feet coordinates agree with the apparatus builders. */
final class CircusRaceCourse {
    static final int RACERS=5, RACE_TICKS=1600;
    record Leg(Vec3 from,Vec3 to,double arc,double weight,int act){}
    static final List<Leg> LEGS=new ArrayList<>();
    static final double LENGTH;
    static {
        Vec3 p=new Vec3(-11,0,-10);
        // Climb real stepping platforms, then run ON the beam (top of y=2 blocks).
        p=leg(p,-10,1,-10,1,0,0);p=leg(p,-9,2,-10,1,0,0);p=leg(p,-8,3,-10,1,0,0);
        p=leg(p,8,3,-10,0,0,0);p=leg(p,9,2,-10,1,0,0);p=leg(p,10,1,-10,1,0,0);p=leg(p,11,0,-10,1,0,0);
        p=leg(p,11,0,-5,0,0,1);
        for(int x=8;x>=-8;x-=4){p=leg(p,x+1.3,0,-5,0,0,1);p=leg(p,x-1.3,0,-5,2.1,0,1);}
        p=leg(p,-11,0,-5,0,0,2);p=leg(p,-11,0,0,0,0,2);
        // Discrete box jumps up and back down, not a levitating spiral.
        p=leg(p,-10,1,1,1,0,2);p=leg(p,-9,2,2,1,0,2);p=leg(p,-8,3,3,1,0,2);
        p=leg(p,-7,4,4,1,0,2);p=leg(p,-6,3,5,1,0,2);p=leg(p,-5,2,6,1,0,2);p=leg(p,-4,1,7,1,0,2);p=leg(p,-3,0,8,1,0,2);
        p=leg(p,-12,0,8,0,0,3);p=leg(p,-12,0,10,0,0,3);p=leg(p,-11,1,10,1,0,3);p=leg(p,-10,2,10,1,0,3);
        // Low weight gives the cannon a quick ballistic flight rather than slow floating.
        p=leg(p,10,0,10,6,2.4,3);p=leg(p,12,0,10,0,0,4);p=leg(p,12,0,7,0,0,4);p=leg(p,11,0,7,0,0,4);
        // Actual serpentine maze corridors, shared by every mouse.
        p=leg(p,11,0,1,0,0,4);p=leg(p,3,0,1,0,0,4);p=leg(p,3,0,3,0,0,4);
        p=leg(p,9,0,3,0,0,4);p=leg(p,9,0,5,0,0,4);p=leg(p,3,0,5,0,0,4);
        p=leg(p,3,0,7,0,0,4);p=leg(p,0,0,7,0,0,4);leg(p,0,0,12,0,0,4);
        LENGTH=LEGS.stream().mapToDouble(Leg::weight).sum();
    }
    private static Vec3 leg(Vec3 from,double x,double y,double z,double arc,double weight,int act){
        Vec3 to=new Vec3(x,y,z);LEGS.add(new Leg(from,to,arc,weight>0?weight:from.distanceTo(to),act));return to;
    }
    static long hash(long x){x=(x^(x>>>30))*0xbf58476d1ce4e5b9L;x=(x^(x>>>27))*0x94d049bb133111ebL;return x^(x>>>31);}
    static int finishTick(long seed,int mouse){return 1450+(int)Math.floorMod(hash(seed+mouse*917L),130);}
    private static double pace(long seed,int mouse,int interval){return .50+Math.floorMod(hash(seed+mouse*7919L+interval*313L),1100)/1000.0;}
    private static double distance(long seed,int mouse,int ticks){
        double distance=0;for(int t=0;t<ticks;t+=40)distance+=Math.min(40,ticks-t)*pace(seed,mouse,t/40);return distance;
    }
    static double progress(long seed,int mouse,int ticks){int finish=finishTick(seed,mouse);return Math.min(1,distance(seed,mouse,Math.max(0,Math.min(ticks,finish)))/distance(seed,mouse,finish));}
    static int winner(long seed){int winner=0;for(int i=1;i<RACERS;i++)if(finishTick(seed,i)<finishTick(seed,winner))winner=i;return winner;}
    static Leg legAt(double progress){double d=Math.max(0,Math.min(1,progress))*LENGTH;for(var l:LEGS){if(d<=l.weight)return l;d-=l.weight;}return LEGS.get(LEGS.size()-1);}
    static Vec3 position(double progress,int mouse){
        double d=Math.max(0,Math.min(1,progress))*LENGTH;
        for(int i=0;i<LEGS.size();i++){var l=LEGS.get(i);if(d<=l.weight||i==LEGS.size()-1){
            double t=Math.max(0,Math.min(1,d/l.weight));Vec3 at=l.from.lerp(l.to,t).add(0,4*l.arc*t*(1-t),0);
            // Narrow lanes remain inside the one-block beam and platform collision footprint.
            double lane=(mouse-2)*.075*Math.sin(Math.PI*t);Vec3 tangent=l.to.subtract(l.from);double n=Math.hypot(tangent.x,tangent.z);
            return n<.01?at:at.add(-tangent.z/n*lane,0,tangent.x/n*lane);
        }d-=l.weight;}throw new IllegalStateException();
    }
    private CircusRaceCourse(){}
}
