package com.coraline.feast;

/** Shared deterministic controls for player and NPC bicycles, in blocks/server tick. */
public final class WybieBicyclePhysics {
    public static final double MAX_SPEED=.34, ACCELERATION=.014, BRAKE=.035;
    public record Motion(double speed,float yaw){}
    public static Motion step(double speed,float yaw,float throttle,float steering,boolean brake){
        if(!Double.isFinite(speed)||!Float.isFinite(yaw)||!Float.isFinite(throttle)||!Float.isFinite(steering))return new Motion(0,0);
        speed=Math.max(0,Math.min(MAX_SPEED,speed));throttle=Math.max(0,Math.min(1,throttle));steering=Math.max(-1,Math.min(1,steering));
        speed=brake?Math.max(0,speed-BRAKE):Math.min(MAX_SPEED,Math.max(0,speed*.975+throttle*ACCELERATION));
        // Progressive turning avoids snapping, drifting and turning a parked bicycle on the spot.
        float turn=steering*(float)(Math.min(1,speed/.09)*(5.5-3*speed/MAX_SPEED));
        return new Motion(speed,wrap(yaw+turn));
    }
    public static float wrap(float degrees){float d=degrees%360;return d>=180?d-360:d< -180?d+360:d;}
    /** Fast authored NPC departure; ordinary player bicycle handling is unchanged. */
    public static Motion departure(double speed,float yaw,float throttle,float steering,boolean brake){
        if(!Double.isFinite(speed)||!Float.isFinite(yaw)||!Float.isFinite(throttle)||!Float.isFinite(steering))return new Motion(0,0);
        speed=brake?Math.max(0,speed-.08):Math.min(.72,Math.max(.20,speed)*.98+Math.max(0,Math.min(1,throttle))*.045);
        float turn=Math.max(-1,Math.min(1,steering))*8;
        return new Motion(speed,wrap(yaw+turn));
    }
    public static boolean forwardCrossing(double beforeX,double beforeZ,double afterX,double afterZ,double x,double z,double nx,double nz,double halfWidth){
        double a=(beforeX-x)*nx+(beforeZ-z)*nz,b=(afterX-x)*nx+(afterZ-z)*nz;
        if(a>=0||b<0||b-a<.001)return false;double t=-a/(b-a),cx=beforeX+(afterX-beforeX)*t-x,cz=beforeZ+(afterZ-beforeZ)*t-z;
        return Math.abs(-nz*cx+nx*cz)<=halfWidth;
    }
    private WybieBicyclePhysics(){}
}
