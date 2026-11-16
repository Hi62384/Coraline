package com.coraline.feast;

/** Deterministic route/timing regression, not evidence of game navigation or visual acceptance. */
public final class RescueWebRulesSmoke {
    private static int checks;
    private static void check(boolean result,String why){checks++;if(!result)throw new AssertionError(why);}
    public static void main(String[] args){
        for(int lane=0;lane<3;lane++){
            double length=0;var previous=RescueWebLayout.point(lane,0);
            for(int sample=1;sample<=RescueWebLayout.SAMPLES;sample++){
                var point=RescueWebLayout.point(lane,sample/(double)RescueWebLayout.SAMPLES);double step=Math.sqrt(Math.pow(point.x()-previous.x(),2)+Math.pow(point.y()-previous.y(),2)+Math.pow(point.z()-previous.z(),2));length+=step;
                check(step<.37,"Collision samples cannot jump a block gap");check(Math.abs(point.y()-previous.y())<.05,"Crouch-compatible vertical grade");check(Math.abs(point.x())<34&&Math.abs(point.z())<34,"Web stays in its owner site, not another player's venue");previous=point;
            }
            check(length/1.3>=120&&length/1.3<=240,"Crouched physical route should take two to four minutes without padding timers");
            var start=RescueWebLayout.point(lane,0);var end=RescueWebLayout.point(lane,1);check(Math.abs(start.x()-28)<.0001&&Math.abs(start.z())<.0001,"All routes share the entrance");check(Math.abs(end.x()+14)<.0001&&Math.abs(end.z())<.0001,"All routes share the real exit");
        }
        check(RescueWebLayout.sticky(0,200)&&RescueWebLayout.sticky(2,600)&&!RescueWebLayout.sticky(1,200),"Multiple routes with readable sticky hazards and a quiet alternative");
        for(int ticks=0;ticks<100;ticks++)check(!RescueWebLayout.struggleFinished(ticks,1),"No flooded/accessibility input may finish a sub-five-second struggle");
        check(RescueWebLayout.struggleFinished(100,1)&&!RescueWebLayout.struggleFinished(100,.9F),"Timing and actual effort are both required");
        for(int duration:new int[]{100,341,1000}){check(!RescueWebLayout.blindAt(duration-81,duration)&&RescueWebLayout.blindAt(duration-80,duration),"Buttons removed exactly four seconds before N ends");check(!RescueWebLayout.swatAt(duration-41,duration)&&RescueWebLayout.swatAt(duration-40,duration),"Cat swatted exactly two seconds before N ends");}
        check(RescueWebLayout.arc(2,5,0,3)==2&&RescueWebLayout.arc(2,5,1,3)==5,"Thrown objects arrive at their physical endpoints");
        for(int rest=1;rest<=16;rest++){int previous=1;for(int i=0;i<=200;i++){double distance=i/40.;int sag=RescueWebLayout.weightedHeight(rest,distance,true);check(sag>=1&&sag<=rest&&sag>=rest-2,"Weight must retain real support within an eighth block of rest");check(sag>=previous,"Weight depression must recover smoothly away from the player's footprint");check(RescueWebLayout.weightedHeight(rest,distance,false)==rest,"Unweighted strands must recover their original collision top");previous=sag;}}
        check(!RescueWebLayout.audibleLanding(10,9.875,true),"Quiet settling must not masquerade as a jump landing");check(RescueWebLayout.audibleLanding(10,9.7,true),"Actual landing should remain audible");check(!RescueWebLayout.audibleLanding(10,9,false),"Airborne descent is not a landing yet");
        for(int ticks=0;ticks<=800;ticks++)check(RescueWebLayout.crawlPursuit(ticks)+1.1<ticks*.065,"Normal land-crawl must be able to outrun the door through the full physical corridor");
        check(0<RescueWebLayout.crawlPursuit(140)+1.1,"The production catch condition must catch a stationary player");
        check(!(0<RescueWebLayout.crawlPursuit(0)+1.1),"The door must not catch the player on corridor entry");
        System.out.println("RESCUE_WEB_RULES_PASSED checks="+checks+"; NOT native route/visual acceptance");
    }
    private RescueWebRulesSmoke(){}
}
