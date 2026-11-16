package com.coraline.feast;

/** Shared rope/bar and performer anchors. All coordinates are relative to the venue center. */
public final class TheatreAerialGeometry {
    public record Bar(double x,double y,double z,double anchorX,double anchorY,double anchorZ){}
    public record Performer(double x,double y,double z,double flip,boolean airborne){}
    public static final double GRIP_HEIGHT=2.10;
    public static Bar swing(int side,double tick){
        double a=Math.sin(tick*Math.PI/73+side*.6)*.70+Math.sin(tick*Math.PI/137+side)*.18;
        double b=Math.sin(tick*Math.PI/109+side*1.2)*.29;
        double x=side*8+Math.sin(b)*15,y=29-Math.cos(a)*Math.cos(b)*15,z=-24+Math.sin(a)*15;
        return new Bar(x,y,z,side*8,29,-24);
    }
    public static Bar approach(int side,double tick,int duration,double targetX,double targetY,double targetZ){
        var base=swing(side,tick);if(side>0||tick<duration-20)return base;
        double u=ScriptedShowTimeline.smooth((tick-duration+20)/20.0);
        return new Bar(base.x+(targetX-base.x)*u,base.y+(targetY+GRIP_HEIGHT-base.y)*u,base.z+(targetZ-base.z)*u,base.anchorX,base.anchorY,base.anchorZ);
    }
    private static Performer routine(int side,double tick){
        int cycle=(int)Math.floor(Math.max(0,tick)/100);double phase=tick-cycle*100;int from=(cycle%2==0?side:-side);
        if(phase<70){var q=swing(from,tick);return new Performer(q.x,q.y-GRIP_HEIGHT,q.z,0,false);}
        double u=(phase-70)/30;var a=swing(from,cycle*100+70);var b=swing(-from,cycle*100+100);double v=ScriptedShowTimeline.smooth(u);
        return new Performer(a.x+(b.x-a.x)*v,a.y+(b.y-a.y)*v-GRIP_HEIGHT+Math.sin(u*Math.PI)*4.0,a.z+(b.z-a.z)*v,-Math.PI*4*u,true);
    }
    /** Alternate actual bars, launch, double-backflip and catch; finish at the player-grab bar. */
    public static Performer performer(int side,double tick,int duration,double targetX,double targetY,double targetZ){
        if(tick<duration-60)return routine(side,tick);
        var from=routine(side,Math.max(0,duration-60));var to=approach(side,tick,duration,targetX,targetY,targetZ);double u=ScriptedShowTimeline.smooth((tick-duration+60)/60.0);
        return new Performer(from.x+(to.x-from.x)*u,from.y+(to.y-GRIP_HEIGHT-from.y)*u+Math.sin(u*Math.PI)*2,from.z+(to.z-from.z)*u,from.flip*(1-u)-Math.PI*2*u,u>0&&u<.97);
    }
    public static Bar carrying(int side,double tick,int duration,boolean rescued){
        // Entity data and performer-pose packets may arrive in different frames.
        // An uninitialized/approach prop is not the validated F recording. Never
        // call its strict server timeline from that transient render snapshot.
        if(duration<560)return swing(side,tick);
        var q=ScriptedShowTimeline.theatrePlayer((int)tick,duration);
        if(tick<480&&!rescued)return new Bar(q.x()+side*.95,q.y()+GRIP_HEIGHT+.75,q.z(),side*8,32,-24);
        if(tick<540&&!rescued)return swing(side,tick);
        return new Bar(side*.9,GRIP_HEIGHT+.2,-25,side*8,32,-24);
    }
    public static double rise(double tick){return -.9+ScriptedShowTimeline.smooth((tick-510)/25.0)*1.1;}
    private TheatreAerialGeometry(){}
}
