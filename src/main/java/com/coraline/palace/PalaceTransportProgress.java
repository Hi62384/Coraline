package com.coraline.palace;

import java.util.*;
import net.minecraft.nbt.*;

/** Real transport clock; only its physical server adapter can enter/finish
 * motion stages. Does not teleport players or assume an attachment succeeded.
 */
public final class PalaceTransportProgress {
    public enum Phase { NOTICE,LIFT,CARRY,THROW,CLOSE,TRANSIT,ARRIVE,EXIT,COMPLETE }
    private final UUID owner,door;
    private UUID session;
    private Phase phase;
    private int ticks;
    private long last=-1;
    private String blocker="";
    private CompoundTag original;
    private PalaceTransportProgress(UUID owner,UUID door){this.owner=Objects.requireNonNull(owner);this.door=Objects.requireNonNull(door);}
    public static PalaceTransportProgress start(UUID owner,UUID door){var s=new PalaceTransportProgress(owner,door);s.session=UUID.randomUUID();s.phase=Phase.NOTICE;return s;}
    public Phase phase(){return phase;}public int ticks(){return ticks;}public UUID session(){return session;}public String blocker(){return blocker;}
    private boolean owns(UUID actor,UUID nonce){return blocker.isEmpty()&&owner.equals(actor)&&Objects.equals(session,nonce);}
    /** Target readiness is established BEFORE closing starts the clock. Failure
     * to preload cannot silently extend the mandatory five-second drive.
     */
    public boolean tick(UUID actor,UUID nonce,long serverTick,boolean physicallyAttached){
        if(!owns(actor,nonce)||serverTick<0||serverTick<=last||phase==Phase.COMPLETE)return false;
        if(phase!=Phase.NOTICE&&phase!=Phase.EXIT&&!physicallyAttached)return false;
        int limit=switch(phase){case NOTICE->60;case LIFT->30;case CARRY->20_000;case THROW->30;case CLOSE->40;case TRANSIT->PalaceJourneyTiming.TRUCK_TRANSFER;case ARRIVE->40;case EXIT->20_000;case COMPLETE->0;};
        last=serverTick;if(ticks<limit)ticks++;return true;
    }
    public boolean motionFinished(UUID actor,UUID nonce,Phase next,boolean realMotionComplete,boolean destinationPreloaded){
        if(!owns(actor,nonce)||!realMotionComplete||phase==Phase.COMPLETE||next.ordinal()!=phase.ordinal()+1)return false;
        int minimum=switch(phase){case NOTICE->40;case LIFT,THROW->30;case CLOSE,ARRIVE->40;case TRANSIT->160;default->0;};
        if(ticks<minimum||!destinationPreloaded&&(next==Phase.LIFT||next==Phase.TRANSIT||next==Phase.ARRIVE))return false;
        phase=next;ticks=0;return true;
    }
    public boolean resume(UUID actor){if(!blocker.isEmpty()||!owner.equals(actor)||phase==Phase.COMPLETE)return false;last=-1;session=UUID.randomUUID();return true;}
    public CompoundTag save(){
        if(original!=null){var n=original.copy();n.putString("Blocker",blocker);return n;}
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Owner",owner);n.putUUID("Door",door);n.putUUID("Session",session);n.putString("Phase",phase.name());n.putInt("Ticks",ticks);n.putLong("Last",last);return n;
    }
    public static boolean matchesRide(CompoundTag ride,UUID owner,UUID door){
        return ride.hasUUID("Owner")&&owner.equals(ride.getUUID("Owner"))
                &&ride.hasUUID("Door")&&door.equals(ride.getUUID("Door"));
    }
    public static int savedBay(CompoundTag ride,boolean destination){
        String key=destination&&ride.contains("DestinationBay")?"DestinationBay":"Bay";
        return ride.contains(key,Tag.TAG_INT)&&ride.getInt(key)>=0&&ride.getInt(key)<2?ride.getInt(key):-1;
    }
    public static boolean unstartedRegistration(CompoundTag ride,UUID owner,UUID door){
        if(!matchesRide(ride,owner,door)||!ride.contains("Version",Tag.TAG_INT)
                ||ride.getInt("Version")!=1||ride.contains("Arrived"))return false;
        for(String key:List.of("Truck","Carrier","Mover0","Mover1","Mover2","Mover3"))
            if(ride.contains(key)||ride.contains(key+"Position"))return false;
        if(!ride.contains("Progress"))return !ride.contains("ActorSession");
        var saved=load(owner,door,ride.getCompound("Progress"));
        return saved.blocker().isEmpty()&&saved.phase()==Phase.NOTICE
                &&(!ride.contains("ActorSession")||ride.hasUUID("ActorSession")&&saved.session().equals(ride.getUUID("ActorSession")));
    }
    public static PalaceTransportProgress load(UUID owner,UUID door,CompoundTag n){
        var s=new PalaceTransportProgress(owner,door);
        try{
            if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||n.contains("Blocker"))throw new IllegalArgumentException("Unknown/blocked transport schema");
            for(String key:new String[]{"Owner","Door","Session"})if(!n.hasUUID(key))throw new IllegalArgumentException("Missing "+key);
            if(!owner.equals(n.getUUID("Owner"))||!door.equals(n.getUUID("Door")))throw new IllegalArgumentException("Owner/door mismatch");
            s.session=n.getUUID("Session");s.phase=Phase.valueOf(n.getString("Phase"));
            if(!n.contains("Ticks",Tag.TAG_INT)||!n.contains("Last",Tag.TAG_LONG)||n.getInt("Ticks")<0||n.getInt("Ticks")>20_000||n.getLong("Last")< -1)throw new IllegalArgumentException("Malformed active clock");
            s.ticks=n.getInt("Ticks");s.last=n.getLong("Last");
            int max=switch(s.phase){case NOTICE->60;case LIFT,THROW->30;case CLOSE,ARRIVE->40;case TRANSIT->160;case COMPLETE->0;default->20_000;};
            if(s.ticks>max)throw new IllegalArgumentException("Cursor outside transport phase");
        }catch(IllegalArgumentException bad){s.blocker="Saved transport needs review: "+bad.getMessage();s.original=n.copy();}
        return s;
    }
}
