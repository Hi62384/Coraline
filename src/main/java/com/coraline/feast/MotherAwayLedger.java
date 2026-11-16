package com.coraline.feast;
import net.minecraft.nbt.*;
import java.util.*;
/** Hidden active-play clock and bounded, legitimate observations. Never sent as a client snapshot. */
public final class MotherAwayLedger {
    public static boolean owned(CompoundTag n,UUID owner,int serial){return owner!=null&&n.hasUUID("Owner")&&owner.equals(n.getUUID("Owner"))&&serial==n.getInt("Serial")&&n.getBoolean("Pending");}
    public static void prepare(CompoundTag n,UUID owner,int serial,long seed){n.putUUID("Owner",owner);n.putInt("Serial",serial);n.putLong("Seed",seed);n.putBoolean("Pending",true);n.putString("Stage","LEAVING");n.putLong("Clock",0);n.put("Reports",new ListTag());}
    public static void departed(CompoundTag n){if(!n.getBoolean("Pending")||!n.getString("Stage").equals("LEAVING")||n.contains("Duration"))return;int duration=12000+new Random(n.getLong("Seed")).nextInt(30001);n.putInt("Duration",duration);n.putInt("Remaining",duration);n.putBoolean("Offsite",true);n.putString("Stage","ABSENT");}
    public static void tick(CompoundTag n,boolean paused){if(!n.getBoolean("Pending")||paused)return;n.putLong("Clock",n.getLong("Clock")+1);if(n.getString("Stage").equals("ABSENT")){int left=Math.max(0,n.getInt("Remaining")-1);n.putInt("Remaining",left);if(left==0)beginReturn(n,false);}else if(n.getString("Stage").equals("RETURN_DELAY")){int left=Math.max(0,n.getInt("TravelRemaining")-1);n.putInt("TravelRemaining",left);if(left==0)n.putString("Stage","RETURNING");}}
    private static void beginReturn(CompoundTag n,boolean early){if(!n.getString("Stage").equals("ABSENT"))return;n.putBoolean("Early",early);n.putString("Stage","RETURN_DELAY");n.putInt("TravelRemaining",300+new Random(n.getLong("Seed")^0x74726176656cL).nextInt(501));n.putInt("ReturnCount",n.getInt("ReturnCount")+1);}
    public static boolean report(CompoundTag n,UUID witness,String source,String offense,long position,String identified,long observedAt){
        if(witness==null||!Set.of("rat","doll").contains(source)||!Set.of("location","drawer","forbidden","restricted_item","investigation").contains(offense)||!n.getBoolean("Pending")||!Set.of("ABSENT","RETURN_DELAY","RETURNING","HOME_CHECK","SEARCHING").contains(n.getString("Stage")))return false;
        var reports=n.getList("Reports",Tag.TAG_COMPOUND);for(var tag:reports){var old=(CompoundTag)tag;if(old.hasUUID("Witness")&&old.getUUID("Witness").equals(witness)&&old.getLong("ObservedAt")==observedAt)return true;}
        var report=new CompoundTag();report.putUUID("Witness",witness);report.putString("Source",source);report.putString("Offense",offense);report.putLong("Position",position);report.putLong("ObservedAt",observedAt);if(identified!=null&&!identified.isEmpty())report.putString("Identified",identified);
        reports.add(report);while(reports.size()>32)reports.remove(0);n.putLong("LastReportedPosition",position);n.putBoolean("ActionableReport",n.getBoolean("ActionableReport")||!offense.equals("location")||identified!=null&&!identified.isEmpty());
        if(n.getString("Stage").equals("ABSENT"))beginReturn(n,true);return true;
    }
    public static void home(CompoundTag n,boolean nearby){if(!n.getString("Stage").equals("RETURNING"))return;n.putBoolean("Offsite",false);n.putBoolean("HomeChecked",true);n.putBoolean("NearbyAtReturn",nearby);n.putString("Stage","HOME_CHECK");}
    public static void searching(CompoundTag n){if(!n.getBoolean("Pending"))return;n.putString("Stage","SEARCHING");n.putBoolean("SearchBegun",true);}
    public static boolean safeReturn(CompoundTag n){return n.getBoolean("Pending")&&n.getString("Stage").equals("HOME_CHECK")&&n.getBoolean("HomeChecked")&&n.getBoolean("NearbyAtReturn")&&!n.getBoolean("ActionableReport");}
    public static void completed(CompoundTag n){n.putBoolean("Pending",false);n.putString("Stage","COMPLETED");}
    private MotherAwayLedger(){}
}
