package com.coraline.postgame;

/** Production pin rules only, not a replacement for real UI/tool/world tests. */
public final class SiteLockRulesCheck {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int tier=1;tier<=3;tier++)for(int done=0;done<12;done++)require(SiteLockRules.target(tier,done)==.5,"Ordinary locks no longer use their fixed center band");
        double[] sequence={.25,.75,.5,.25,.75,.5};for(int i=0;i<sequence.length;i++)require(SiteLockRules.target(4,i)==sequence[i],"Supernatural lock failed its visible alternating pin target");
        require(SiteLockRules.pin(1,3,0,100,94,100,.047,.245)==SiteLockRules.Outcome.HIT,"First ordinary center pin unavailable");
        require(SiteLockRules.pin(4,6,0,100,94,100,.083,.17)==SiteLockRules.Outcome.MISS,"Tier 4 incorrectly accepts the old fixed-center cue");
        for(int tier=1;tier<=4;tier++){
            int pins=2+tier,done=0;long last=-6;double speed=.035+tier*.012,window=.27-tier*.025;
            for(long now=0;now<=1200&&done<pins;now++){
                var outcome=SiteLockRules.pin(tier,pins,done,0,last,now,speed,window);
                require(outcome!=SiteLockRules.Outcome.INVALID,"Normal configured attempt became invalid");
                if(outcome==SiteLockRules.Outcome.HIT){
                    int next=done+1;
                    if(next<pins)require(SiteLockRules.pin(tier,pins,next,0,now,now,speed,window)==SiteLockRules.Outcome.WAIT,"Replayed packet advances a second pin in one tick");
                    last=now;done=next;
                }
            }
            require(done==pins,"Normal lock cannot be completed before expiry: tier "+tier);
            require(SiteLockRules.pin(tier,pins,pins,0,last,1200,speed,window)==SiteLockRules.Outcome.INVALID,"Completed attempt accepted more progress");
        }
        for(long delta=0;delta<6;delta++)require(SiteLockRules.pin(2,4,1,100,100,100+delta,.059,.22)==SiteLockRules.Outcome.WAIT,"Minimum click interval bypassed");
        require(SiteLockRules.pin(2,4,1,100,100,106,.059,.22)!=SiteLockRules.Outcome.WAIT,"Six-tick boundary remains blocked");
        require(SiteLockRules.pin(2,4,1,100,94,1300,.059,.22)!=SiteLockRules.Outcome.INVALID,"Exact timeout boundary expired too early");
        require(SiteLockRules.pin(2,4,1,100,94,1301,.059,.22)==SiteLockRules.Outcome.INVALID,"Expired attempt accepted");
        require(SiteLockRules.pin(2,4,1,100,94,99,.059,.22)==SiteLockRules.Outcome.INVALID,"Clock rollback accepted");
        require(SiteLockRules.pin(2,4,1,100,120,110,.059,.22)==SiteLockRules.Outcome.INVALID,"Future click receipt accepted");
        require(SiteLockRules.pin(2,4,1,-1,-6,0,.059,.22)==SiteLockRules.Outcome.INVALID,"Negative clock accepted");
        require(SiteLockRules.pin(2,4,1,Long.MAX_VALUE-10,Long.MAX_VALUE-16,Long.MAX_VALUE,.059,.22)!=SiteLockRules.Outcome.INVALID,"Large valid clock overflowed");
        for(int tier:new int[]{0,5})require(SiteLockRules.pin(tier,4,0,0,-6,0,.059,.22)==SiteLockRules.Outcome.INVALID,"Invalid tier accepted");
        for(int pins:new int[]{1,13})require(SiteLockRules.pin(2,pins,0,0,-6,0,.059,.22)==SiteLockRules.Outcome.INVALID,"Invalid pin count accepted");
        for(int done:new int[]{-1,4,99})require(SiteLockRules.pin(2,4,done,0,-6,0,.059,.22)==SiteLockRules.Outcome.INVALID,"Invalid completed count accepted");
        for(double speed:new double[]{0,Double.NaN,Double.POSITIVE_INFINITY,.151})require(SiteLockRules.pin(2,4,0,0,-6,0,speed,.22)==SiteLockRules.Outcome.INVALID,"Invalid speed accepted");
        for(double window:new double[]{0,Double.NaN,Double.POSITIVE_INFINITY,.401})require(SiteLockRules.pin(2,4,0,0,-6,0,.059,window)==SiteLockRules.Outcome.INVALID,"Invalid hit window accepted");
        System.out.println("SITE_LOCK_RULES_OK checks="+checks);
    }
    private SiteLockRulesCheck(){}
}
