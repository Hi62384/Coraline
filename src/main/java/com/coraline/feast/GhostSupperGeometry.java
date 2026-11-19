package com.coraline.feast;

/** Shared floor, cushion and chair-forward convention for accepted-route supper seats. */
public final class GhostSupperGeometry {
    public static final double CUSHION_TOP=.52;
    // The parent uses the vanilla humanoid hip pivot (12 model pixels)
    // and the living renderer's 1.501-block model origin.
    static final double PARENT_HIP=1.501-12/16.0;
    public static boolean isSeat(int slot){return slot==101||slot==103||slot==104;}
    static float facing(double x,double z,double targetX,double targetZ){
        return (float)Math.toDegrees(Math.atan2(x-targetX,targetZ-z));
    }
    public static float meshRotation(float facing){return 180-facing;}
    public static float childScale(int appearance){return .90F+(appearance%5)*.035F;}
    static double riderOrigin(int slot,int appearance){
        return slot==101?CUSHION_TOP-(.52-.30)*childScale(appearance):CUSHION_TOP-PARENT_HIP;
    }
    static int elapsedFromLegacyBeat(int beat){
        return switch(beat){case 2->210;case 3->460;case 4->800;case 5->1150;case 6->1480;default->0;};
    }
    private GhostSupperGeometry(){}
}
