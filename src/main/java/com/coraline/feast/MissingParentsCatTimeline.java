package com.coraline.feast;

/** Shared visual geometry; only the server's acknowledged recording clock advances the story. */
public final class MissingParentsCatTimeline {
    public static final int WRITE_TICKS=160,READ_TICKS=20,SHATTER_TICKS=30;
    private static final double[][] STROKES={
        {0,0,0,1},{.55,0,.55,1},{0,.5,.55,.5},
        {1,0,1,1},{1,1,1.55,1},{1,.5,1.45,.5},{1,0,1.55,0},
        {2,1,2,0},{2,0,2.55,0},
        {3,0,3,1},{3,1,3.55,1},{3.55,1,3.55,.5},{3.55,.5,3,.5},
        {4.4,1,4.4,0},{4.4,0,4.95,0},{4.95,0,4.95,1},
        {5.95,1,5.4,1},{5.4,1,5.4,.5},{5.4,.5,5.95,.5},{5.95,.5,5.95,0},{5.95,0,5.4,0}};
    public static float progress(int ticks,int duration){return Math.max(0,Math.min(1,ticks/(float)Math.max(1,duration)));}
    public static int shatterCue(int duration){return Math.max(1,duration-50);}
    public static float dollPull(int ticks,int duration){float u=progress(ticks,duration);return u*u*(3-2*u);}
    public static int strokes(){return STROKES.length;}
    // The viewer faces local +Z; their screen-right is local -X, not +X.
    public static double[] point(int index,double fraction){var s=STROKES[Math.max(0,Math.min(STROKES.length-1,index))];double u=Math.max(0,Math.min(1,fraction));return new double[]{(3-s[0]-(s[2]-s[0])*u)*.29,1.22+(s[1]+(s[3]-s[1])*u)*.36};}
    public static double[] finger(float progress){double at=Math.max(0,Math.min(.999999,progress))*strokes();return point((int)at,at-Math.floor(at));}
    private MissingParentsCatTimeline(){}
}
