package com.coraline.postgame;

import java.util.*;
import net.minecraft.nbt.*;

/** World-global containment kernel. No packet, copied block snapshot or debug
 * flag is an objective witness. Physical drill/quest/avatar adapters must
 * attest their own live completion before invoking these package methods.
 * This kernel is not a device, boss, raid controller or gameplay acceptance. */
final class BreachState {
    static final int SCALE=1_000_000,MAX=2*SCALE,DAY=24000;
    enum Kind { RIFT_DRILL, DIMENSIONAL_SEAM, CORALINE_QUEST, WYBIE_QUEST, HEART_CHAMBER }
    enum Resolution { REDUCED, ZERO, UNPOWERED, BOSS_LOCKED, ALREADY_RESOLVED, CONFLICT, REFUSED }
    record Rules(int poweredRate,int unpoweredRate,int fuelPerStone){
        Rules{if(poweredRate<1||poweredRate>SCALE||unpoweredRate<2L*poweredRate||unpoweredRate>MAX||fuelPerStone<DAY||fuelPerStone>DAY*60)throw new IllegalArgumentException("Invalid containment balance");}
        static Rules defaults(){return new Rules(30000,120000,DAY*2);}
    }
    private CompoundTag data=new CompoundTag();private String blocker="";
    static BreachState fresh(){var s=new BreachState();s.data.putInt("Version",1);s.data.putBoolean("Activated",false);return s;}
    static BreachState load(CompoundTag saved){
        var s=new BreachState();s.data=saved.copy();
        try{
            if(!saved.contains("Version",Tag.TAG_INT)||saved.getInt("Version")!=1||!bool(saved,"Activated"))throw new IllegalArgumentException("Unsupported Breach schema");
            if(saved.contains("CompleteReset")&&!saved.hasUUID("CompleteReset"))throw new IllegalArgumentException("Invalid ending receipt");
            if(saved.contains("RetiredDrives")){if(!saved.contains("RetiredDrives",Tag.TAG_LIST))throw new IllegalArgumentException("Invalid retired drive records");var retired=saved.getList("RetiredDrives",Tag.TAG_COMPOUND);if(retired.size()!=((ListTag)saved.get("RetiredDrives")).size()||retired.size()>1024)throw new IllegalArgumentException("Unbounded retired drives");var ids=new HashSet<UUID>();for(var tag:retired){var row=(CompoundTag)tag;if(!row.hasUUID("Generation")||!row.hasUUID("Binding")||!row.hasUUID("Reset")||!ids.add(row.getUUID("Binding"))||!row.contains("Position",Tag.TAG_LONG))throw new IllegalArgumentException("Invalid retired drive identity");var at=net.minecraft.core.BlockPos.of(row.getLong("Position"));if(Math.abs((long)at.getX())>29999984||Math.abs((long)at.getZ())>29999984||at.getY()< -64||at.getY()>319)throw new IllegalArgumentException("Invalid retired drive position");}}
            if(!saved.getBoolean("Activated")){
                for(String key:List.of("Generation","ActivatedAt","LastTick","Value","Remainder","Fuel","Rules","Objectives","AvatarLocked","AvatarEntity","LastAvatarVictory"))if(saved.contains(key))throw new IllegalArgumentException("Dormant Breach has live state");
                return s;
            }
            if(!saved.hasUUID("Generation")||!saved.contains("ActivatedAt",Tag.TAG_LONG)||!saved.contains("LastTick",Tag.TAG_LONG)||saved.getLong("ActivatedAt")<0||saved.getLong("LastTick")<saved.getLong("ActivatedAt"))throw new IllegalArgumentException("Missing Breach clock/epoch");
            for(String key:List.of("Value","Remainder"))if(!saved.contains(key,Tag.TAG_INT))throw new IllegalArgumentException("Invalid fixed-point field");
            int value=saved.getInt("Value"),remainder=saved.getInt("Remainder");
            if(value<0||value>MAX||remainder<0||remainder>=DAY||value==MAX&&remainder!=0||!saved.contains("Fuel",Tag.TAG_LONG)||saved.getLong("Fuel")<0)throw new IllegalArgumentException("Out-of-range Breach/fuel");
            if(!bool(saved,"AvatarLocked")||saved.getBoolean("AvatarLocked")!=(value==MAX)||saved.contains("AvatarEntity")&&(!saved.getBoolean("AvatarLocked")||!saved.hasUUID("AvatarEntity")))throw new IllegalArgumentException("Invalid Avatar lock");
            if(!saved.contains("Rules",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Missing Breach balance");decodeRules(saved.getCompound("Rules"));
            if(!saved.contains("Objectives",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Missing containment receipts");
            var objectives=saved.getCompound("Objectives");var receipts=new HashSet<UUID>();
            for(String key:objectives.getAllKeys()){
                var id=UUID.fromString(key);if(!id.toString().equals(key)||!objectives.contains(key,Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid objective key");
                var row=objectives.getCompound(key);
                if(!row.hasUUID("Receipt")||!row.contains("Kind",Tag.TAG_STRING)||!row.contains("Outcome",Tag.TAG_STRING)||!row.contains("At",Tag.TAG_LONG)||row.getLong("At")<saved.getLong("ActivatedAt")||row.getLong("At")>saved.getLong("LastTick"))throw new IllegalArgumentException("Invalid completion provenance");
                if(!receipts.add(row.getUUID("Receipt")))throw new IllegalArgumentException("Completion receipt reused by another objective");
                Kind.valueOf(row.getString("Kind"));var outcome=Resolution.valueOf(row.getString("Outcome"));
                if(!row.contains("Amount",Tag.TAG_INT)||!row.contains("Applied",Tag.TAG_INT)||row.getInt("Amount")<1||row.getInt("Amount")>MAX||row.getInt("Applied")<0||row.getInt("Applied")>row.getInt("Amount"))throw new IllegalArgumentException("Invalid completion amount");
                if(outcome!=Resolution.REDUCED&&outcome!=Resolution.ZERO&&outcome!=Resolution.UNPOWERED&&outcome!=Resolution.BOSS_LOCKED||outcome==Resolution.REDUCED&&row.getInt("Applied")==0||outcome!=Resolution.REDUCED&&row.getInt("Applied")!=0)throw new IllegalArgumentException("Invalid resolved outcome");
            }
            if(saved.contains("LastAvatarVictory")){
                if(!saved.contains("LastAvatarVictory",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid Avatar receipt");var victory=saved.getCompound("LastAvatarVictory");
                if(!victory.hasUUID("Entity")||!victory.hasUUID("Receipt")||!victory.contains("At",Tag.TAG_LONG)||victory.getLong("At")<saved.getLong("ActivatedAt")||victory.getLong("At")>saved.getLong("LastTick"))throw new IllegalArgumentException("Invalid Avatar victory provenance");
                if(receipts.contains(victory.getUUID("Receipt")))throw new IllegalArgumentException("Avatar receipt reused by an ordinary objective");
                if(saved.hasUUID("AvatarEntity")&&saved.getUUID("AvatarEntity").equals(victory.getUUID("Entity")))throw new IllegalArgumentException("Defeated Avatar rebound as a live encounter");
            }
        }catch(IllegalArgumentException invalid){s.blocker=invalid.getMessage();}
        return s;
    }
    private static boolean bool(CompoundTag n,String key){return n.contains(key,Tag.TAG_BYTE)&&(n.getByte(key)==0||n.getByte(key)==1);}
    private static Rules decodeRules(CompoundTag n){for(String key:List.of("Powered","Unpowered","FuelPerStone"))if(!n.contains(key,Tag.TAG_INT))throw new IllegalArgumentException("Invalid rules schema");return new Rules(n.getInt("Powered"),n.getInt("Unpowered"),n.getInt("FuelPerStone"));}
    private void writeRules(Rules rules){var n=data.getCompound("Rules").copy();n.putInt("Powered",rules.poweredRate());n.putInt("Unpowered",rules.unpoweredRate());n.putInt("FuelPerStone",rules.fuelPerStone());data.put("Rules",n);}
    boolean active(){return blocker.isEmpty()&&data.getBoolean("Activated");}
    String blocker(){return blocker;}
    Optional<UUID> generation(){return active()?Optional.of(data.getUUID("Generation")):Optional.empty();}
    int value(){return active()?data.getInt("Value"):0;}
    long fuel(){return active()?data.getLong("Fuel"):0;}
    long lastTick(){return active()?data.getLong("LastTick"):-1;}
    boolean powered(){return active()&&fuel()>0;}
    boolean avatarLocked(){return active()&&data.getBoolean("AvatarLocked");}
    Optional<UUID> avatarEntity(){return active()&&data.hasUUID("AvatarEntity")?Optional.of(data.getUUID("AvatarEntity")):Optional.empty();}
    boolean avatarVictory(UUID entity,UUID receipt){var n=data.getCompound("LastAvatarVictory");return active()&&n.hasUUID("Entity")&&n.getUUID("Entity").equals(entity)&&n.hasUUID("Receipt")&&n.getUUID("Receipt").equals(receipt);}
    boolean containmentEnabled(){return powered()&&!avatarLocked();}
    Rules rules(){if(!active())throw new IllegalStateException("Inactive Breach");return decodeRules(data.getCompound("Rules"));}
    CompoundTag save(){return data.copy();}
    boolean activate(UUID generation,long now,Rules rules){
        if(!blocker.isEmpty()||generation==null||now<0||rules==null)return false;
        if(active())return generation.equals(data.getUUID("Generation"));
        data.putBoolean("Activated",true);data.putUUID("Generation",generation);data.putLong("ActivatedAt",now);data.putLong("LastTick",now);data.putInt("Value",0);data.putInt("Remainder",0);data.putLong("Fuel",0);data.putBoolean("AvatarLocked",false);data.put("Objectives",new CompoundTag());writeRules(rules);return true;
    }
    /** Game ticks only: no wall-clock/offline catch-up and no day-time input. */
    boolean advance(long now){
        if(!active()||now<lastTick())return false;long elapsed=now-lastTick();if(elapsed==0)return true;
        long powered=Math.min(elapsed,fuel());data.putLong("Fuel",fuel()-powered);var rules=rules();
        increase(powered,rules.poweredRate());increase(elapsed-powered,rules.unpoweredRate());data.putLong("LastTick",now);return true;
    }
    private void increase(long ticks,int rate){
        if(ticks<=0||value()==MAX)return;long needed=(long)(MAX-value())*DAY-data.getInt("Remainder");
        // Saturate before multiplication, including a hostile Long.MAX clock.
        if(ticks>=(needed+rate-1)/rate){data.putInt("Value",MAX);data.putInt("Remainder",0);data.putBoolean("AvatarLocked",true);return;}
        long numerator=data.getInt("Remainder")+ticks*rate;data.putInt("Value",value()+(int)(numerator/DAY));data.putInt("Remainder",(int)(numerator%DAY));
    }
    /** Adapter must atomically consume real Moonstone before exposing fuel. */
    boolean addFuel(int stones,long now){
        if(stones<1||stones>64||!active()||now<lastTick())return false;
        long before=data.getLong("Fuel");long elapsed=now-lastTick();long remaining=before-Math.min(before,elapsed),added=(long)stones*rules().fuelPerStone();
        if(remaining>Long.MAX_VALUE-added)return false;if(!advance(now))return false;data.putLong("Fuel",fuel()+added);return true;
    }
    boolean configure(Rules replacement,long now){if(replacement==null||!advance(now))return false;writeRules(replacement);return true;}
    /** Every ordinary containment source must come through this fuel/lock
     * gate. Unpowered completion is resolved once, never a deferred payout. */
    Resolution complete(Kind kind,UUID objective,UUID receipt,int amount,long now){
        if(kind==null||objective==null||receipt==null||amount<1||amount>MAX||!active()||now<lastTick())return Resolution.REFUSED;
        var objectives=data.getCompound("Objectives");var key=objective.toString();
        if(objectives.contains(key)){
            var prior=objectives.getCompound(key);return prior.getUUID("Receipt").equals(receipt)&&prior.getString("Kind").equals(kind.name())&&prior.getInt("Amount")==amount?Resolution.ALREADY_RESOLVED:Resolution.CONFLICT;
        }
        // A copied completion receipt cannot authorize another objective.
        if(data.contains("LastAvatarVictory",Tag.TAG_COMPOUND)&&receipt.equals(data.getCompound("LastAvatarVictory").getUUID("Receipt")))return Resolution.CONFLICT;
        for(String prior:objectives.getAllKeys())if(objectives.getCompound(prior).getUUID("Receipt").equals(receipt))return Resolution.CONFLICT;
        advance(now);int applied=0;Resolution outcome;
        if(avatarLocked())outcome=Resolution.BOSS_LOCKED;
        else if(!powered())outcome=Resolution.UNPOWERED;
        else{int before=value();applied=Math.min(before,amount);outcome=applied==0?Resolution.ZERO:Resolution.REDUCED;data.putInt("Value",before-applied);if(amount>before)data.putInt("Remainder",0);}
        var row=new CompoundTag();row.putUUID("Receipt",receipt);row.putString("Kind",kind.name());row.putString("Outcome",outcome.name());row.putLong("At",now);row.putInt("Amount",amount);row.putInt("Applied",applied);objectives.put(key,row);return outcome;
    }
    /** Binding is one-time within this encounter; missing is not defeated. */
    boolean bindAvatar(UUID entity,long now){
        if(entity==null||!advance(now)||!avatarLocked())return false;
        if(data.hasUUID("AvatarEntity"))return entity.equals(data.getUUID("AvatarEntity"));
        if(data.contains("LastAvatarVictory",Tag.TAG_COMPOUND)&&entity.equals(data.getCompound("LastAvatarVictory").getUUID("Entity")))return false;
        data.putUUID("AvatarEntity",entity);return true;
    }
    /** Only the real matching boss death adapter may call this. Fuel is
     * deliberately irrelevant to the 2.0 -> exactly 1.5 escape rule. */
    boolean avatarDefeated(UUID entity,UUID victoryReceipt,long now){
        if(entity==null||victoryReceipt==null||!advance(now)||!avatarLocked()||!data.hasUUID("AvatarEntity")||!entity.equals(data.getUUID("AvatarEntity")))return false;
        if(data.contains("LastAvatarVictory",Tag.TAG_COMPOUND)&&victoryReceipt.equals(data.getCompound("LastAvatarVictory").getUUID("Receipt")))return false;
        for(String key:data.getCompound("Objectives").getAllKeys())if(victoryReceipt.equals(data.getCompound("Objectives").getCompound(key).getUUID("Receipt")))return false;
        data.putInt("Value",SCALE*3/2);data.putInt("Remainder",0);data.putBoolean("AvatarLocked",false);data.remove("AvatarEntity");
        var row=new CompoundTag();row.putUUID("Entity",entity);row.putUUID("Receipt",victoryReceipt);row.putLong("At",now);data.put("LastAvatarVictory",row);return true;
    }
    private BreachState(){}
}
