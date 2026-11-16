package com.coraline.rebirth;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Owner-local write-ahead record. No world, inventory or entity mutations live here. */
public final class StoryRun {
    public enum Phase { IDLE, SUMMONING, BINDING, FIRST_THREAD, FIRST_DEATH,
        FINAL_THREAD, FINAL_DEATH, ABSORBING, REWINDING, RESETTING, RESTORING, RETURNING }
    private final UUID owner;
    private final CompoundTag data;
    private StoryRun(UUID owner, CompoundTag data) { this.owner=owner; this.data=data; }
    public static StoryRun first(UUID owner) {
        var n=new CompoundTag(); n.putInt("Version",1); n.putUUID("Owner",owner);
        n.putLong("Run",1); n.putLong("Completed",0); n.putLong("HeartsEarned",0);
        n.putString("Phase",Phase.IDLE.name()); return new StoryRun(owner,n);
    }
    /** Reject corruption/future schemas rather than treating them as a fresh reward ledger. */
    public static StoryRun load(UUID owner,CompoundTag n) {
        if(n.getInt("Version")!=1||!n.hasUUID("Owner")||!owner.equals(n.getUUID("Owner"))
                ||!n.contains("Run",Tag.TAG_LONG)||n.getLong("Run")<1
                ||!n.contains("Completed",Tag.TAG_LONG)||!n.contains("HeartsEarned",Tag.TAG_LONG)
                ||n.getLong("Completed")<0||n.getLong("HeartsEarned")!=n.getLong("Completed")
                ||n.getLong("Completed")>n.getLong("Run"))throw new IllegalArgumentException("Invalid story-run identity/counters");
        Phase.valueOf(n.getString("Phase"));
        if(n.getBoolean("Complete")&&n.getLong("Completed")!=n.getLong("Run")
                ||!n.getBoolean("Complete")&&n.getLong("Completed")!=n.getLong("Run")-1)
            throw new IllegalArgumentException("Completion does not match this run");
        if(n.getBoolean("RewardPending")&&(!n.getBoolean("Complete")||!n.hasUUID("RewardReceipt")))
            throw new IllegalArgumentException("Invalid pending Void Heart receipt");
        if(n.getBoolean("RewardClaimed")&&(!n.getBoolean("Complete")||n.getBoolean("RewardPending")))
            throw new IllegalArgumentException("Invalid claimed Void Heart receipt");
        if(n.getBoolean("Complete")&&(!n.hasUUID("RewardReceipt")||n.getBoolean("RewardPending")==n.getBoolean("RewardClaimed")))
            throw new IllegalArgumentException("Completed run must have exactly one pending or claimed receipt");
        if(!n.getString("Phase").equals("IDLE")&&(!n.hasUUID("Session")||!n.hasUUID("Family")
                ||!n.hasUUID("Mother")||!n.hasUUID("Father")||!n.hasUUID("Door")))
            throw new IllegalArgumentException("Incomplete committed ritual identity");
        var phase=Phase.valueOf(n.getString("Phase"));
        if(phase!=Phase.IDLE&&!location(n.getCompound("Ritual")))throw new IllegalArgumentException("Invalid committed bedroom location");
        if(n.contains("SacrificedParents")){
            if(!n.contains("SacrificedParents",Tag.TAG_LIST))throw new IllegalArgumentException("Malformed sacrifice tombstones");
            var deaths=(net.minecraft.nbt.ListTag)n.get("SacrificedParents");var unique=new java.util.HashSet<UUID>();
            if(!deaths.isEmpty()&&deaths.getElementType()!=Tag.TAG_INT_ARRAY)throw new IllegalArgumentException("Malformed sacrifice UUIDs");
            for(var tag:deaths)if(!(tag instanceof net.minecraft.nbt.IntArrayTag array)||array.getAsIntArray().length!=4||!unique.add(net.minecraft.nbt.NbtUtils.loadUUID(tag)))throw new IllegalArgumentException("Invalid/duplicate sacrifice UUID");
        }
        if(phase!=Phase.IDLE&&phase!=Phase.RETURNING&&(!n.getBoolean("Complete")||!n.getBoolean("RewardClaimed")
            ||!n.contains("CommittedRun",Tag.TAG_LONG)||n.getLong("CommittedRun")!=n.getLong("Run")))
            throw new IllegalArgumentException("Ritual is not bound to a completed rewarded run");
        if(phase.ordinal()>=Phase.ABSORBING.ordinal()&&(!n.getCompound("Ritual").getBoolean("MotherDead")||!n.getCompound("Ritual").getBoolean("FatherDead")))
            throw new IllegalArgumentException("Rewind requires both genuine sacrifice receipts");
        return new StoryRun(owner,n.copy());
    }
    private static boolean location(CompoundTag n){
        if(!n.contains("Center",Tag.TAG_LONG)||!n.contains("PortraitPosition",Tag.TAG_LONG)||!n.hasUUID("Portrait"))return false;
        var at=net.minecraft.core.BlockPos.of(n.getLong("Center"));var picture=net.minecraft.core.BlockPos.of(n.getLong("PortraitPosition"));
        return Math.abs((long)at.getX())<29999984&&Math.abs((long)at.getZ())<29999984&&at.getY()>=-64&&at.getY()<320&&at.distSqr(picture)<64;
    }
    public StoryRun copy(){return load(owner,data);}
    public CompoundTag save(){return data.copy();}
    public UUID owner(){return owner;}
    public long run(){return data.getLong("Run");}
    public long completed(){return data.getLong("Completed");}
    public long earned(){return data.getLong("HeartsEarned");}
    public Phase phase(){return Phase.valueOf(data.getString("Phase"));}
    public boolean active(){return phase()!=Phase.IDLE;}
    public boolean complete(){return data.getBoolean("Complete");}
    public boolean rewardPending(){return data.getBoolean("RewardPending");}
    public boolean claimed(){return data.getBoolean("RewardClaimed");}
    public UUID receipt(){return data.hasUUID("RewardReceipt")?data.getUUID("RewardReceipt"):null;}
    public UUID session(){return data.hasUUID("Session")?data.getUUID("Session"):null;}
    public long ticks(){return data.getLong("Ticks");}
    public CompoundTag scene(){return data.getCompound("Scene").copy();}
    public UUID id(String key){return data.hasUUID(key)?data.getUUID(key):null;}
    public CompoundTag details(){return data.getCompound("Ritual").copy();}
    public void details(CompoundTag n){data.put("Ritual",n.copy());}
    public boolean tombstoned(UUID entity){
        if(entity==null)return false;
        for(var tag:data.getList("SacrificedParents",Tag.TAG_INT_ARRAY))if(entity.equals(net.minecraft.nbt.NbtUtils.loadUUID(tag)))return true;
        return false;
    }
    /** Called only after the real canonical entity has completed its health-zero death call. */
    public boolean recordDeath(UUID session,boolean mother){
        if(!java.util.Objects.equals(session(),session)||phase()!=(mother?Phase.FIRST_DEATH:Phase.FINAL_DEATH))return false;
        var id=id(mother?"Mother":"Father");var list=data.getList("SacrificedParents",Tag.TAG_INT_ARRAY);
        if(!tombstoned(id)){var encoded=new CompoundTag();encoded.putUUID("Id",id);list.add(encoded.get("Id").copy());}data.put("SacrificedParents",list);
        var ritual=details();ritual.putBoolean(mother?"MotherDead":"FatherDead",true);details(ritual);return true;
    }
    /** Only the canonical ending adapter calls this, never ritual activation. */
    public boolean finish(boolean canonicalEnding) {
        if(!canonicalEnding||active()||complete()||completed()!=run()-1||run()==Long.MAX_VALUE)return false;
        data.putBoolean("Complete",true); data.putLong("Completed",completed()+1);
        data.putLong("HeartsEarned",earned()+1);data.putUUID("RewardReceipt",UUID.randomUUID());
        data.putBoolean("RewardPending",true);return true;
    }
    public boolean delivered(UUID receipt){
        if(!rewardPending()||!java.util.Objects.equals(receipt(),receipt))return false;
        data.putBoolean("RewardPending",false);data.putBoolean("RewardClaimed",true);return true;
    }
    public boolean commit(UUID family,UUID mother,UUID father,UUID door,CompoundTag snapshot) {
        if(active()||!complete()||!claimed()||family==null||mother==null||father==null||mother.equals(father)||door==null||!location(details()))return false;
        data.putUUID("Family",family);data.putUUID("Mother",mother);data.putUUID("Father",father);data.putUUID("Door",door);
        data.putUUID("Session",UUID.randomUUID());data.put("Scene",snapshot.copy());
        data.putLong("CommittedRun",run());phase(Phase.SUMMONING);return true;
    }
    private void phase(Phase next){data.putString("Phase",next.name());data.putLong("Ticks",0);}
    public boolean advance(UUID session,Phase expected,Phase next){
        if(!active()||!java.util.Objects.equals(session(),session)||phase()!=expected
                ||expected==Phase.FIRST_THREAD||expected==Phase.FINAL_THREAD
                ||next.ordinal()!=expected.ordinal()+1)return false;
        phase(next);return true;
    }
    /** These two transitions are exclusively explicit owner input, never a timer. */
    public boolean cut(UUID owner,UUID session,boolean first){
        if(!this.owner.equals(owner)||!java.util.Objects.equals(session(),session))return false;
        if(first&&phase()==Phase.FIRST_THREAD){phase(Phase.FIRST_DEATH);return true;}
        if(!first&&phase()==Phase.FINAL_THREAD){phase(Phase.FINAL_DEATH);return true;}
        return false;
    }
    public void tick(){if(active()&&ticks()<Long.MAX_VALUE)data.putLong("Ticks",ticks()+1);}
    /** Recorded only after all reset adapters have durably applied the same transaction. */
    public boolean nextRun(UUID session){
        if(!java.util.Objects.equals(session(),session)||phase()!=Phase.RESTORING)return false;
        long old=data.getLong("CommittedRun");
        if(run()==old){if(old==Long.MAX_VALUE)return false;data.putLong("Run",old+1);}
        else if(run()!=old+1)return false;
        data.putBoolean("Complete",false);data.putBoolean("RewardClaimed",false);data.remove("RewardReceipt");
        phase(Phase.RETURNING);return true;
    }
    public boolean completeEndingTimeline(){var previous=data.getCompound("PreviousRitual");return data.hasUUID("CompleteReset")&&previous.hasUUID("CompleteReset")&&data.getUUID("CompleteReset").equals(previous.getUUID("CompleteReset"));}
    /** Complete-ending reset preserves the one-heart-per-real-run audit ledger.
     * It does not call the parent-sacrifice ritual or grant a reward. */
    public boolean completeEnding(UUID reset){return completeEnding(reset,new CompoundTag());}
    public boolean completeEnding(UUID reset,CompoundTag originalWybie){return completeEnding(reset,originalWybie,new CompoundTag());}
    public boolean completeEnding(UUID reset,CompoundTag originalWybie,CompoundTag originalCat){
        if(reset==null||phase()!=Phase.IDLE||rewardPending())return false;
        if(data.hasUUID("CompleteReset")&&reset.equals(data.getUUID("CompleteReset")))return true;
        long completed=data.getLong("Completed");if(completed==Long.MAX_VALUE)return false;
        var retired=data.getList("SacrificedParents",net.minecraft.nbt.Tag.TAG_INT_ARRAY).copy();
        var fresh=first(owner).save();fresh.putLong("Run",completed+1);fresh.putLong("Completed",completed);fresh.putLong("HeartsEarned",data.getLong("HeartsEarned"));fresh.putUUID("CompleteReset",reset);if(originalWybie!=null&&!originalWybie.isEmpty()){var previous=new CompoundTag();previous.putUUID("CompleteReset",reset);var retained=new CompoundTag();retained.put("Wybie",originalWybie.copy());previous.put("Retained",retained);fresh.put("PreviousRitual",previous);}
        if(originalCat!=null&&!originalCat.isEmpty()){var previous=fresh.getCompound("PreviousRitual");previous.putUUID("CompleteReset",reset);var retained=previous.getCompound("Retained");retained.put("Cat",originalCat.copy());previous.put("Retained",retained);fresh.put("PreviousRitual",previous);}
        if(!retired.isEmpty())fresh.put("SacrificedParents",retired);
        for(String key:new java.util.HashSet<>(data.getAllKeys()))data.remove(key);data.merge(fresh);return true;
    }
    public boolean returned(UUID session){
        if(phase()!=Phase.RETURNING||!java.util.Objects.equals(session(),session))return false;
        var previous=data.getCompound("Ritual").copy();previous.put("Retained",scene());data.put("PreviousRitual",previous);
        for(String key:new String[]{"Session","Family","Mother","Father","Door","Scene","Ritual","CommittedRun"})data.remove(key);
        phase(Phase.IDLE);return true;
    }
}
