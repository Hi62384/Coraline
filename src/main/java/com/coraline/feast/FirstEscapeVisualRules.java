package com.coraline.feast;

/** Shared presentation rules, independent of a running client or world. */
public final class FirstEscapeVisualRules {
    public static final int RESCUE_HAND_CUE=1;
    /** The mother's old carrier and Wybie's later guide grip are not rescue arms. */
    public static boolean rescueHand(String phase,boolean rescued,int cue){return phase.equals("ESCAPE_A")&&!rescued&&cue==RESCUE_HAND_CUE;}
    public static boolean prison(String phase){return switch(phase){case "MIRROR_A","MIRROR_RETRACT","MIRROR_B","MIRROR_C","MIRROR_D","MIRROR_E"->true;default->false;};}
    public static float prisonLight(int block){float lamp=Math.max(0,Math.min(15,block))/15F;return .075F+.32F*lamp*lamp;}
    /** Readable post-proposal house silhouettes without lifting the prison. */
    public static float darkHouseLight(int block){float lamp=Math.max(0,Math.min(15,block))/15F;return .065F+.76F*lamp*lamp;}
    /** The revealed insect parlor is a colorful lit set, not the dark hallway. */
    public static boolean brightParlor(String phase){try{var p=FirstEscapeTimeline.Phase.valueOf(phase);return p.ordinal()>=FirstEscapeTimeline.Phase.ROOM_DARK.ordinal()&&p.ordinal()<=FirstEscapeTimeline.Phase.MIRROR_THROW.ordinal();}catch(IllegalArgumentException invalid){return false;}}
    /** Match the original lamp/sconce reveal, never switch to full light on entry. */
    public static float parlorBlend(String phase,int ticks){
        if(!brightParlor(phase)||phase.equals("ROOM_DARK"))return 0;
        return phase.equals("ROOM_TRANSFORMING")?CocoaBeetlesChoreography.ramp(ticks,80,90):1;
    }
    /** A carried camera must not cross a dark hallway lightmap boundary mid-drag. */
    public static boolean carryParlorLight(String phase,boolean grabbed){return phase.equals("MIRROR_DRAG")||phase.equals("MIRROR_THROW")||phase.equals("COCOA_G")&&grabbed;}
    public static float parlorLight(int block){return .68F+.32F*Math.max(0,Math.min(15,block))/15F;}
    /** Opening the room must not introduce another global lightmap blackout. */
    public static boolean inheritedDarkHouse(String phase){try{var p=FirstEscapeTimeline.Phase.valueOf(phase);return p.ordinal()<=FirstEscapeTimeline.Phase.MIRROR_THROW.ordinal();}catch(IllegalArgumentException invalid){return false;}}
    public static int mirrorThrowCue(int duration){return Math.max(1,duration-40);}
    public static boolean helperWatchesEntrance(String phase,int ticks){return phase.equals("ESCAPE_G")||phase.equals("ESCAPE_D")&&ticks>=40;}
    /** North wall's inner face is z=-3; the revealed face must project into it. */
    public static double prisonHeadOffset(float reveal){return -.45+Math.max(0,Math.min(1,reveal))*1.05;}
    /** NativeImage ABGR, applied after gamma. Preserve alpha; affect only sampled world lighting. */
    public static int prisonPixel(int pixel){return pixel;}
    public static boolean ghostSad(int cue,boolean farewell,int time){return cue<3||RescueChallengeMath.farewellSad(cue,farewell,time);}
    /** Head-local metres: lifted corners and a lower centre, on the actual jaw. */
    public static double stitchedSmileY(double x){double u=Math.max(-1,Math.min(1,x/.105));return (-1.52-.78*u*u)/16.;}
    private FirstEscapeVisualRules(){}
}
