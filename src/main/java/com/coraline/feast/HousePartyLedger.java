package com.coraline.feast;

import net.minecraft.nbt.*;
import java.util.*;

/** Durable party clocks and observed incidents. World perception/navigation remain server inputs. */
public final class HousePartyLedger {
    public static final int SOCIAL_TICKS=7200;
    public enum Notice { NONE, WARN, COMPLIED, REPORT }
    public enum FoodNotice { ALLOW, WARN, BLOCK }
    public static void begin(CompoundTag n,UUID owner,int serial,long seed,int capacity){
        n.putUUID("Owner",owner);n.putInt("Serial",serial);n.putLong("Seed",seed);n.putInt("Capacity",Math.max(6,Math.min(40,capacity)));
        n.putLong("Clock",0);n.putInt("SocialElapsed",0);n.putInt("FeastElapsed",0);
        n.putInt("FeastDuration",1200);n.putBoolean("Pending",true);
        n.putBoolean("FeastDue",false);n.putBoolean("Suspended",false);n.put("Guests",new ListTag());
    }
    public static boolean owned(CompoundTag n,UUID owner,int serial){return n.hasUUID("Owner")&&n.getUUID("Owner").equals(owner)&&n.getInt("Serial")==serial&&n.getBoolean("Pending");}
    public static int absenceDelay(double suspicion,double unit){
        double s=Double.isFinite(suspicion)?Math.max(0,Math.min(1,suspicion)):0,u=Double.isFinite(unit)?Math.max(0,Math.min(1,unit)):.5;
        return (int)Math.round(1200*(3+7*Math.pow(u,.45+5.5*s)));
    }
    /** activeDelta is owner-online, unpaused server time; never wall time while disconnected. */
    public static void advance(CompoundTag n,int activeDelta,boolean social,boolean feast,boolean confrontation){
        if(activeDelta<0||activeDelta>20)throw new IllegalArgumentException("Unbounded party tick delta");
        if(!n.getBoolean("Pending")||confrontation||n.getBoolean("Suspended"))return;
        n.putLong("Clock",n.getLong("Clock")+activeDelta);
        if(social){n.putInt("SocialElapsed",Math.min(SOCIAL_TICKS,n.getInt("SocialElapsed")+activeDelta));if(n.getInt("SocialElapsed")>=SOCIAL_TICKS)n.putBoolean("FeastDue",true);}
        if(feast)n.putInt("FeastElapsed",Math.min(n.getInt("FeastDuration"),n.getInt("FeastElapsed")+activeDelta));
    }
    /** Returns true once. Re-entering a doorway can never cancel a search already triggered. */
    public static boolean absence(CompoundTag n,boolean permitted,boolean alreadySearching,double suspicion,double random,int delta,long position){
        if(delta<0||delta>20)throw new IllegalArgumentException("Unbounded absence tick delta");
        if(permitted){
            n.putLong("LastPermittedPosition",position);
            if(!alreadySearching&&!n.getBoolean("AbsenceReported"))n.remove("AbsenceRemaining");
            return false;
        }
        if(alreadySearching||n.getBoolean("AbsenceReported"))return false;
        if(!n.contains("AbsenceRemaining"))n.putInt("AbsenceRemaining",absenceDelay(suspicion,random));
        int left=Math.max(0,n.getInt("AbsenceRemaining")-delta);n.putInt("AbsenceRemaining",left);
        if(left==0){n.putBoolean("AbsenceReported",true);return true;}return false;
    }
    public static void reconciled(CompoundTag n){n.putBoolean("Suspended",false);n.putBoolean("AbsenceReported",false);n.remove("AbsenceRemaining");}
    public static CompoundTag addGuest(CompoundTag n,String kind,UUID counterpart,int order){
        var guest=new CompoundTag();
        UUID id=UUID.nameUUIDFromBytes((n.getUUID("Owner")+":"+n.getInt("Serial")+":"+kind+":"+(counterpart==null?order:counterpart)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        guest.putUUID("Id",id);guest.putString("Kind",kind);guest.putString("State","QUEUED");guest.putInt("Order",order);
        guest.putInt("ArrivalAt",40+order*80);guest.putInt("Patience",0);
        if(counterpart!=null)guest.putUUID("Counterpart",counterpart);
        n.getList("Guests",Tag.TAG_COMPOUND).add(guest);return guest;
    }
    public static CompoundTag guest(CompoundTag n,UUID id){for(var tag:n.getList("Guests",Tag.TAG_COMPOUND)){var g=(CompoundTag)tag;if(g.hasUUID("Id")&&g.getUUID("Id").equals(id))return g;}return null;}
    private static void clearWarning(CompoundTag g){for(String key:List.of("Recognition","WarningUntil","ObservedOffense","IdentifiedSecret","ObservedPosition","LastObserved","Incident"))g.remove(key);}
    /**
     * A warning only escalates while the same offense is still actually visible.
     * Leaving view is not evidence that a closed drawer is still being searched.
     */
    public static Notice observe(CompoundTag guest,String visibleOffense,String identified,long position,long clock,int delta,int warningTicks){
        if(delta<0||delta>20)throw new IllegalArgumentException("Unbounded recognition tick delta");
        if(guest.getBoolean("ReportPending")||guest.getBoolean("ReportDelivered"))return Notice.NONE;
        if(visibleOffense==null||visibleOffense.isEmpty()){
            boolean warned=guest.contains("WarningUntil");clearWarning(guest);return warned?Notice.COMPLIED:Notice.NONE;
        }
        if(!visibleOffense.equals(guest.getString("ObservedOffense"))){clearWarning(guest);guest.putString("ObservedOffense",visibleOffense);}
        guest.putLong("LastObserved",clock);guest.putLong("ObservedPosition",position);
        if(identified!=null&&!identified.isEmpty())guest.putString("IdentifiedSecret",identified);
        int recognized=guest.getInt("Recognition")+delta;guest.putInt("Recognition",recognized);
        if(recognized<20)return Notice.NONE;
        if(!guest.contains("WarningUntil")){
            guest.putUUID("Incident",UUID.randomUUID());guest.putLong("WarningUntil",clock+Math.max(100,Math.min(200,warningTicks-guest.getInt("Patience")*10)));
            guest.putInt("Patience",Math.min(10,guest.getInt("Patience")+1));return Notice.WARN;
        }
        if(clock<guest.getLong("WarningUntil"))return Notice.NONE;
        guest.putBoolean("ReportPending",true);guest.putString("State","REPORTING");return Notice.REPORT;
    }
    /** Navigation code may call this only after speaker/listener proximity and line of sight pass. */
    public static boolean receiveReport(CompoundTag guest){
        if(!guest.getBoolean("ReportPending")||guest.getBoolean("ReportDelivered"))return false;
        guest.putBoolean("ReportPending",false);guest.putBoolean("ReportDelivered",true);return true;
    }
    public static void finishReport(CompoundTag guest){
        guest.remove("ReportDelivered");guest.remove("ReportPending");clearWarning(guest);guest.putString("State","WANDERING");
    }
    /** Only witnessed inventory-taking enters this rolling counter; eating never calls it. */
    public static FoodNotice foodTaking(CompoundTag n,long clock,boolean witnessed){
        if(!witnessed)return FoodNotice.ALLOW;
        if(n.getLong("FoodBlockedUntil")>clock)return FoodNotice.BLOCK;
        var times=new ArrayList<Long>();for(long at:n.getLongArray("WitnessedFoodTakes"))if(clock-at<=400)times.add(at);
        times.add(clock);n.putLongArray("WitnessedFoodTakes",times);
        if(times.size()<=3)return FoodNotice.ALLOW;
        if(!n.getBoolean("FoodWarned")||clock>=n.getLong("FoodWarningExpires")){
            n.putBoolean("FoodWarned",true);n.putLong("FoodWarningExpires",clock+1200);return FoodNotice.WARN;
        }
        n.putLong("FoodBlockedUntil",clock+200);return FoodNotice.BLOCK;
    }
    public static boolean mayClaimSecret(CompoundTag party,CompoundTag guest){return party.getInt("SecretClaims")<3&&!guest.getBoolean("SecretClaimed");}
    public static void claimedSecret(CompoundTag party,CompoundTag guest,String id){if(!mayClaimSecret(party,guest))throw new IllegalStateException("Party secret claim cap");guest.putBoolean("SecretClaimed",true);guest.putString("ClaimedSecret",id);party.putInt("SecretClaims",party.getInt("SecretClaims")+1);}
    private HousePartyLedger(){}
}
