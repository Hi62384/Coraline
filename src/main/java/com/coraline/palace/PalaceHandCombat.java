package com.coraline.palace;

import java.util.*;
import net.minecraft.nbt.*;

/** Three actual-hit phases, tuned for roughly 3-5 minutes of mobile combat.
 * No elapsed-time victory, offline catch-up, projectile rescue or client health.
 * One successful strike per telegraphed recovery, with at least 60 s per phase. */
public final class PalaceHandCombat {
    public enum Beat { TELEGRAPH, STRIKE, RECOVERY, PURSUE, COMPLETE }
    private final UUID owner,door,hand;
    private UUID session;
    private int phase=1,hits=12,ticks,total,lastHitCycle=-1;
    private long lastTick=-1;
    private boolean complete;
    private String blocker="";
    private CompoundTag original;
    private PalaceHandCombat(UUID owner,UUID door,UUID hand,UUID session){this.owner=owner;this.door=door;this.hand=hand;this.session=session;}
    public static PalaceHandCombat start(UUID owner,UUID door,UUID hand,UUID session){return new PalaceHandCombat(Objects.requireNonNull(owner),Objects.requireNonNull(door),Objects.requireNonNull(hand),Objects.requireNonNull(session));}
    public int phase(){return phase;} public int hits(){return hits;} public int ticks(){return ticks;} public int totalTicks(){return total;} public String blocker(){return blocker;}
    public UUID session(){return session;}
    public int period(){return phase==1?140:phase==2?120:100;}
    public int cycle(){return ticks/period();} public int cursor(){return ticks%period();}
    public Beat beat(){return complete?Beat.COMPLETE:cursor()<32?Beat.TELEGRAPH:cursor()<44?Beat.STRIKE:cursor()<75?Beat.RECOVERY:Beat.PURSUE;}
    public boolean tick(UUID actor,UUID token,long serverTick,boolean realHandReady){
        if(!owns(actor,token)||complete||!realHandReady||serverTick<0||serverTick<=lastTick||ticks==Integer.MAX_VALUE||total==Integer.MAX_VALUE)return false;
        lastTick=serverTick;ticks++;total++;
        if(hits==0&&ticks>=1200){if(phase==3)complete=true;else{phase++;hits=12;ticks=0;lastHitCycle=-1;}}
        return true;
    }
    public boolean hit(UUID actor,UUID token,UUID target,long serverTick,boolean actualMelee,boolean inReach,boolean visible,boolean cooled){
        int at=ticks+(serverTick>lastTick?1:0),cursor=at%period(),cycle=at/period();
        if(!owns(actor,token)||complete||!hand.equals(target)||lastTick<0||serverTick<lastTick||serverTick>lastTick+1||!actualMelee||!inReach||!visible||!cooled
                ||cursor<44||cursor>=75||hits<=0||lastHitCycle==cycle)return false;
        lastHitCycle=cycle;hits--;return true;
    }
    private boolean owns(UUID actor,UUID token){return blocker.isEmpty()&&owner.equals(actor)&&session.equals(token);}
    public boolean resume(UUID actor,UUID token){if(!blocker.isEmpty()||!owner.equals(actor)||token==null||complete)return false;session=token;lastTick=-1;return true;}
    public boolean complete(){return blocker.isEmpty()&&complete;}
    public CompoundTag save(){
        if(!blocker.isEmpty())return original.copy();var n=original==null?new CompoundTag():original.copy();n.putInt("Version",1);n.putUUID("Owner",owner);n.putUUID("Door",door);n.putUUID("Hand",hand);n.putUUID("Session",session);
        n.putInt("Phase",phase);n.putInt("Hits",hits);n.putInt("Ticks",ticks);n.putInt("Total",total);n.putInt("LastHitCycle",lastHitCycle);n.putLong("LastTick",lastTick);n.putBoolean("Complete",complete);return n;
    }
    public static PalaceHandCombat load(UUID owner,UUID door,UUID hand,CompoundTag n){
        var s=new PalaceHandCombat(owner,door,hand,new UUID(0,0));s.original=n.copy();
        try{
            if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1)throw new IllegalArgumentException("Unknown hand combat schema");
            for(String key:new String[]{"Owner","Door","Hand","Session"})if(!n.hasUUID(key))throw new IllegalArgumentException("Missing combat identity");
            if(!owner.equals(n.getUUID("Owner"))||!door.equals(n.getUUID("Door"))||!hand.equals(n.getUUID("Hand")))throw new IllegalArgumentException("Foreign combat identity");
            for(String key:new String[]{"Phase","Hits","Ticks","Total","LastHitCycle"})if(!n.contains(key,Tag.TAG_INT))throw new IllegalArgumentException("Malformed combat clock");
            if(!n.contains("LastTick",Tag.TAG_LONG)||!n.contains("Complete",Tag.TAG_BYTE)||n.getByte("Complete")<0||n.getByte("Complete")>1)throw new IllegalArgumentException("Malformed completion");
            s.session=n.getUUID("Session");s.phase=n.getInt("Phase");s.hits=n.getInt("Hits");s.ticks=n.getInt("Ticks");s.total=n.getInt("Total");s.lastTick=n.getLong("LastTick");s.lastHitCycle=n.getInt("LastHitCycle");s.complete=n.getBoolean("Complete");
            if(s.phase<1||s.phase>3||s.hits<0||s.hits>12||s.ticks<0||s.total<s.ticks+(s.phase-1)*1200||s.lastTick< -1||s.lastHitCycle< -1||s.lastHitCycle>s.cycle()
                    ||(s.hits==12)!=(s.lastHitCycle==-1)
                    ||s.complete&&(s.phase!=3||s.hits!=0||s.ticks<1200||s.total<3600))throw new IllegalArgumentException("Impossible hand combat state");
        }catch(IllegalArgumentException bad){s.blocker=bad.getMessage();}return s;
    }
}
