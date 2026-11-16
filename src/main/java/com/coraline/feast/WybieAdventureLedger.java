package com.coraline.feast;

import net.minecraft.nbt.CompoundTag;
import java.util.UUID;

/** Server-only, persisted rules. Never accepts score, elapsed time or capture from a client. */
public final class WybieAdventureLedger {
    public static final int DURATION=24_000, DOOR_TIMEOUT=2_400;
    public enum Mode { DOOR, INTRO, GARDEN, LEAD, FOLLOW, SEARCH, ENDING, DONE }
    public static CompoundTag begin(UUID owner,UUID door,int serial,long seed){
        var n=new CompoundTag();n.putUUID("Owner",owner);n.putUUID("Door",door);n.putUUID("Session",UUID.randomUUID());
        n.putInt("Serial",serial);n.putLong("Seed",seed);n.putString("Mode",Mode.DOOR.name());n.putBoolean("Pending",true);
        n.putInt("Remaining",DURATION);n.putInt("Checkpoint",0);return n;
    }
    public static boolean owned(CompoundTag n,UUID owner,UUID door,int serial){return n.hasUUID("Owner")&&owner.equals(n.getUUID("Owner"))&&n.hasUUID("Door")&&door.equals(n.getUUID("Door"))&&serial==n.getInt("Serial")&&n.hasUUID("Session");}
    public static Mode mode(CompoundTag n){try{return Mode.valueOf(n.getString("Mode"));}catch(IllegalArgumentException ignored){return Mode.DONE;}}
    public static void mode(CompoundTag n,Mode mode){n.putString("Mode",mode.name());n.putInt("ModeTicks",0);}
    public static boolean timed(CompoundTag n){return n.getBoolean("TimerStarted")&&n.getBoolean("Pending")&&mode(n)!=Mode.ENDING&&mode(n)!=Mode.DONE;}
    /** An interrupted introduction is replayed in full; the adventure clock is never reset. */
    public static boolean introFinished(CompoundTag n){
        if(mode(n)!=Mode.INTRO||n.getBoolean("TimerStarted")||n.getInt("IntroLength")<=0||n.getInt("IntroElapsed")<n.getInt("IntroLength"))return false;
        n.putBoolean("TimerStarted",true);mode(n,Mode.GARDEN);return true;
    }
    public static void tick(CompoundTag n){
        if(!n.getBoolean("Pending"))return;n.putLong("Clock",n.getLong("Clock")+1);n.putInt("ModeTicks",n.getInt("ModeTicks")+1);
        if(timed(n)&&n.getInt("Remaining")>0)n.putInt("Remaining",n.getInt("Remaining")-1);
        if(n.getBoolean("RunActive"))n.putInt("SectionTicks",n.getInt("SectionTicks")+1);
    }
    public static void startRun(CompoundTag n){
        if(!timed(n)||n.getInt("Remaining")<=0)return;
        n.putBoolean("RunActive",true);n.putInt("CurrentScore",0);n.putInt("Combo",0);n.putInt("Checkpoint",1);n.putInt("SectionTicks",0);n.putInt("Lap",0);n.putInt("AirTicks",0);n.putBoolean("JumpLanded",false);
    }
    public static int failRun(CompoundTag n,String reason){
        int score=n.getInt("CurrentScore");n.putInt("BestScore",Math.max(score,n.getInt("BestScore")));n.putInt("LastRunScore",score);
        n.putInt("CurrentScore",0);n.putInt("Combo",0);n.putInt("Checkpoint",0);n.putInt("SectionTicks",0);n.putBoolean("RunActive",false);n.putString("Failure",reason);return score;
    }
    /** Caller validates swept checkpoint crossing, rider ownership, direction and section geometry. */
    public static int checkpoint(CompoundTag n,int index,int count,int basePoints){
        if(!n.getBoolean("RunActive")||index!=n.getInt("Checkpoint")||count<6||index<0||index>=count)return 0;
        int combo=Math.min(20,n.getInt("Combo")+1);int awarded=Math.max(0,basePoints)+Math.min(50,(combo/3)*10);
        if(index==0){awarded+=250;n.putInt("Lap",n.getInt("Lap")+1);}
        int score=(int)Math.min(1_000_000L,(long)n.getInt("CurrentScore")+awarded);n.putInt("CurrentScore",score);n.putInt("BestScore",Math.max(score,n.getInt("BestScore")));
        n.putInt("Combo",combo);n.putInt("Checkpoint",(index+1)%count);n.putInt("SectionTicks",0);n.putInt("AirTicks",0);n.putBoolean("JumpLanded",false);return awarded;
    }
    public static int rewardTier(int best){return best>=5000?4:best>=3000?3:best>=1500?2:best>=500?1:0;}
    public static boolean banQualifies(boolean pending,boolean timerStarted,boolean physicalContact,boolean ownerMatches,boolean forbiddenArea,boolean restrictedEvidence){return pending&&timerStarted&&physicalContact&&ownerMatches&&(forbiddenArea||restrictedEvidence);}
    public static boolean warning(CompoundTag n,int threshold,String key){if(timed(n)&&n.getInt("Remaining")<=threshold&&!n.getBoolean(key)){n.putBoolean(key,true);return true;}return false;}
    public static void end(CompoundTag n,String reason){failRun(n,reason);n.putString("EndReason",reason);mode(n,Mode.ENDING);}
    private WybieAdventureLedger(){}
}
