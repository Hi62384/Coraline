package com.coraline.feast;

/** Pure rules only: not native combat/camera/multiplayer acceptance. */
public final class RescueChallengeRulesSmoke {
    private static int checks;
    private static void check(boolean result,String message){checks++;if(!result)throw new AssertionError(message);}
    public static void main(String[] args){
        check(RescueChallengeMath.tier(100,100)==0&&RescueChallengeMath.tier(67,100)==1&&RescueChallengeMath.tier(34,100)==2,"Three health tiers");
        for(int tier=0;tier<3;tier++)for(int patterns:new int[]{4,5}){
            var seen=new java.util.HashSet<Integer>();
            for(int t=0;t<6000;t++){int pattern=RescueChallengeMath.attack(t,tier,patterns),beat=RescueChallengeMath.beat(t,tier);seen.add(pattern);check(pattern>=0&&pattern<patterns,"Pattern range");check(RescueChallengeMath.vulnerable(t,tier)==(beat>=100),"Cannot strike through attack/telegraph");}
            check(seen.size()==patterns,"Every attack pattern is reachable");
        }
        check(!RescueChallengeMath.hears(0,true,false,false,true),"Stationary crouched player is not noisy merely for standing on silk");
        check(!RescueChallengeMath.hears(.03,true,false,false,false),"Quiet crouch remains useful");
        check(RescueChallengeMath.hears(.04,true,false,false,true),"Moving through sticky silk is audible");
        check(RescueChallengeMath.hears(.03,false,true,false,false)&&RescueChallengeMath.hears(0,true,false,true,false),"Sprint and landing make noise");
        for(int batch=1;batch<=100;batch++){
            var quiet=new RescueChallengeMath.StepBatch();for(int i=0;i<batch;i++)quiet.record(.06476);
            check(quiet.count()==batch&&Math.abs(quiet.distance()-.06476*batch)<1e-9,"Accepted steps were lost");
            check(!RescueChallengeMath.hears(quiet.loudestWithUnaccounted(quiet.distance()),true,false,false,false),"Lag-batched quiet crouch falsely heard: "+batch);
            quiet.record(.21);for(int i=0;i<batch;i++)quiet.record(0);
            check(RescueChallengeMath.hears(quiet.loudestWithUnaccounted(quiet.distance()),true,false,false,false),"Quiet/empty packets diluted a genuinely fast step");
        }
        var pushed=new RescueChallengeMath.StepBatch();pushed.record(.06476);check(pushed.loudestWithUnaccounted(.5)>.4,"Unaccounted server displacement was silently dropped");
        var invalid=new RescueChallengeMath.StepBatch();invalid.record(Double.NaN);check(RescueChallengeMath.hears(invalid.loudestWithUnaccounted(0),true,false,false,false),"Invalid travel silenced pursuit");
        long start=1_000_000_000L;for(int duration:new int[]{1,200,623,10000})for(int ticks=0;ticks<=duration+201;ticks++){long now=start+ticks*50_000_000L;check(StoryPlaybackClock.complete(start,now,duration)==(ticks>=duration),"Catch-up server ticks cannot finish a real recording early");check(StoryPlaybackClock.endWaitTicks(start,now,duration)==Math.max(0,ticks-duration),"End deadline must begin after the original duration");}
        for(int spacing:new int[]{3,4,5}){
            float progress=0;int t=0;for(;t<500&&progress<1;t++)progress=RescueChallengeMath.struggle(progress,t%spacing==0?1:0,false);
            check(t>=100&&t<=200,"Ordinary 4-6 clicks per second should take 5-10 seconds, not two seconds");
        }
        check(RescueChallengeMath.struggle(.2F,100,false)==RescueChallengeMath.struggle(.2F,1,false),"A batched/replayed packet cannot inject 100 clicks");
        check(RescueChallengeMath.struggle(0,-1,false)==0,"Negative input cannot corrupt progress");
        double last=200;for(int t=0;t<=1500;t++){double radius=RescueChallengeMath.collapse(t,1500,200,48);check(radius<=last&&radius>=48,"Monotonic collapse preserves house refuge");last=radius;}
        check(RescueChallengeMath.collapse(1600,1500,200,48)==48,"Collapse stops at refuge boundary");
        check(RescueChallengeMath.pursuit(332,332,20)==20&&RescueChallengeMath.pursuit(500,332,20)==20,"Pursuing door uses full recorded duration and cannot overshoot");
        for(int cue=0;cue<=5;cue++)for(int time=-1;time<=800;time++)for(boolean farewell:new boolean[]{false,true})check(RescueChallengeMath.farewellSad(cue,farewell,time)==(cue>=3&&(cue==5||farewell&&time>=240)),"Ghost sorrow used a lagging server cue or changed prison faces");
        System.out.println("RESCUE_CHALLENGE_RULES_PASSED checks="+checks+"; not native scene acceptance");
    }
    private RescueChallengeRulesSmoke(){}
}
