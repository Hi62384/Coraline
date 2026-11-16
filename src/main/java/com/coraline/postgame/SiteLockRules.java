package com.coraline.postgame;

/** Server pin timing, independent of UI and live-world target authorization. */
final class SiteLockRules {
    enum Outcome { INVALID, WAIT, HIT, MISS }
    static double target(int tier,int completed){return tier==4?switch(completed%3){case 0->.25;case 1->.75;default->.5;}: .5;}
    static Outcome pin(int tier,int pins,int completed,long start,long last,long now,double speed,double window){
        if(tier<1||tier>4||pins<2||pins>12||completed<0||completed>=pins||!Double.isFinite(speed)||speed<.02||speed>.15||!Double.isFinite(window)||window<.08||window>.4
            ||start<0||now<start||now-start>1200||last<start-6||last>now)return Outcome.INVALID;
        if(now-last<6)return Outcome.WAIT;
        double cursor=(Math.sin((now-start)*speed)+1)/2;
        return Math.abs(cursor-target(tier,completed))<=window/2?Outcome.HIT:Outcome.MISS;
    }
    private SiteLockRules(){}
}
