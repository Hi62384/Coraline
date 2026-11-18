package com.coraline.postgame;

import java.util.*;
import java.util.function.LongSupplier;
import com.coraline.palace.PalaceWellProgress.WybieLife;
import net.minecraft.nbt.*;

/** Global generation identity. Ordinary story replay has no reset operation on
 * this journal. Terrain, portals and full-reset retirement are separate adapters. */
public final class InfiniteWorldState {
    public enum Activation { CREATED, OWNER_RECORDED, ALREADY_RECORDED, REFUSED }
    public record Witness(UUID owner,UUID door,UUID wellSession,UUID keyReceipt,UUID wybie,WybieLife wybieLife) {
        boolean valid(){return owner!=null&&door!=null&&wellSession!=null&&keyReceipt!=null&&wybieLife!=null&&(wybie!=null||wybieLife==WybieLife.DEAD_BEFORE_WELL);}
    }
    private CompoundTag data=new CompoundTag();
    private String blocker="";
    private InfiniteWorldState(){}
    public static InfiniteWorldState fresh(){
        var state=new InfiniteWorldState();state.data.putInt("Version",1);state.data.putBoolean("Activated",false);state.data.put("Owners",new CompoundTag());return state;
    }
    public static InfiniteWorldState load(CompoundTag saved){
        var state=new InfiniteWorldState();state.data=saved.copy();
        try{
            if(!saved.contains("Version",Tag.TAG_INT)||saved.getInt("Version")!=1)throw new IllegalArgumentException("Unsupported Infinite Other World journal version");
            if(!saved.contains("Activated",Tag.TAG_BYTE)||(saved.getByte("Activated")!=0&&saved.getByte("Activated")!=1)||!saved.contains("Owners",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid activation journal");
            if(saved.getBoolean("Activated")){
                if(!saved.hasUUID("Generation")||!saved.contains("Seed",Tag.TAG_LONG)||!saved.contains("ActivatedAt",Tag.TAG_LONG)||saved.getLong("ActivatedAt")<0)throw new IllegalArgumentException("Missing independent generation identity");
                if(!saved.contains("TerrainVersion",Tag.TAG_INT)||saved.getInt("TerrainVersion")!=InfiniteTerrain.VERSION)throw new IllegalArgumentException("Unsupported terrain version; generation was not rerolled");
                if(saved.getCompound("Owners").isEmpty())throw new IllegalArgumentException("Activation lacks a canonical completion witness");
                if(saved.contains("CanonicalCompletion")){
                    if(!saved.contains("CanonicalCompletion",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid canonical world outcome");var canonical=saved.getCompound("CanonicalCompletion");
                    if(!canonical.hasUUID("Owner")||!canonical.hasUUID("Door")||!canonical.hasUUID("WellSession")||!canonical.hasUUID("KeyReceipt")||canonical.getLong("RecordedAt")!=saved.getLong("ActivatedAt")||!saved.getCompound("Owners").contains(canonical.getUUID("Owner").toString()))throw new IllegalArgumentException("Invalid canonical world witness");
                    var fate=WybieLife.valueOf(canonical.getString("WybieLife"));var witness=new Witness(canonical.getUUID("Owner"),canonical.getUUID("Door"),canonical.getUUID("WellSession"),canonical.getUUID("KeyReceipt"),canonical.hasUUID("Wybie")?canonical.getUUID("Wybie"):null,fate);if(!witness.valid())throw new IllegalArgumentException("Incomplete canonical world witness");
                }
            }else if(saved.contains("Generation")||saved.contains("Seed")||saved.contains("ActivatedAt")||saved.contains("TerrainVersion")||!saved.getCompound("Owners").isEmpty())throw new IllegalArgumentException("Inactive journal contains live generation data");
            if(saved.contains("RecordedWitnesses")){if(!saved.contains("RecordedWitnesses",Tag.TAG_COMPOUND)||saved.getCompound("RecordedWitnesses").size()>32768)throw new IllegalArgumentException("Invalid completion history");for(String receipt:saved.getCompound("RecordedWitnesses").getAllKeys())if(!UUID.fromString(receipt).toString().equals(receipt)||!saved.getCompound("RecordedWitnesses").hasUUID(receipt))throw new IllegalArgumentException("Invalid recorded completion receipt");}
            if(saved.getCompound("Owners").size()>4096)throw new IllegalArgumentException("Unbounded completion owners");
            if(saved.contains("Retired")){if(!saved.contains("Retired",Tag.TAG_LIST))throw new IllegalArgumentException("Invalid retired generations");var retired=saved.getList("Retired",Tag.TAG_COMPOUND);if(retired.size()!=((ListTag)saved.get("Retired")).size()||retired.size()>1024)throw new IllegalArgumentException("Unbounded retired generations");var ids=new HashSet<UUID>();var resets=new HashSet<UUID>();for(var tag:retired){var row=(CompoundTag)tag;if(!row.hasUUID("Generation")||!row.hasUUID("Reset")||!ids.add(row.getUUID("Generation"))||!resets.add(row.getUUID("Reset"))||!row.contains("At",Tag.TAG_LONG)||row.getLong("At")<0)throw new IllegalArgumentException("Invalid retired identity");}if(saved.getBoolean("Activated")&&ids.contains(saved.getUUID("Generation")))throw new IllegalArgumentException("A retired generation is active");}
            if(saved.contains("RetiredWitnesses")){if(!saved.contains("RetiredWitnesses",Tag.TAG_COMPOUND)||saved.getCompound("RetiredWitnesses").size()>65536)throw new IllegalArgumentException("Invalid retired witness ledger");for(String receipt:saved.getCompound("RetiredWitnesses").getAllKeys())if(!UUID.fromString(receipt).toString().equals(receipt)||!saved.getCompound("RetiredWitnesses").hasUUID(receipt))throw new IllegalArgumentException("Invalid retired completion receipt");}
            for(String name:saved.getCompound("Owners").getAllKeys()){
                UUID owner;try{owner=UUID.fromString(name);}catch(IllegalArgumentException bad){throw new IllegalArgumentException("Invalid completion owner");}
                if(!owner.toString().equals(name))throw new IllegalArgumentException("Noncanonical completion owner");
                var owners=saved.getCompound("Owners");if(!owners.contains(name,Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid completion witness");var row=owners.getCompound(name);
                for(String key:List.of("Owner","Door","WellSession","KeyReceipt"))if(!row.hasUUID(key))throw new IllegalArgumentException("Incomplete completion witness");
                if(!owner.equals(row.getUUID("Owner"))||!row.contains("RecordedAt",Tag.TAG_LONG)||row.getLong("RecordedAt")<saved.getLong("ActivatedAt"))throw new IllegalArgumentException("Foreign or out-of-order completion witness");
                if(!row.contains("WybieLife",Tag.TAG_STRING)||row.contains("Wybie")&&!row.hasUUID("Wybie"))throw new IllegalArgumentException("Invalid original Wybie provenance");
                var life=WybieLife.valueOf(row.getString("WybieLife"));if(life!=WybieLife.DEAD_BEFORE_WELL&&!row.hasUUID("Wybie"))throw new IllegalArgumentException("Missing is not an original Wybie outcome");
            }
        }catch(IllegalArgumentException invalid){state.blocker=invalid.getMessage();}
        return state;
    }
    public String blocker(){return blocker;}
    public boolean active(){return blocker.isEmpty()&&data.getBoolean("Activated");}
    public OptionalLong seed(){return active()?OptionalLong.of(data.getLong("Seed")):OptionalLong.empty();}
    public Optional<UUID> generation(){return active()?Optional.of(data.getUUID("Generation")):Optional.empty();}
    public boolean completedBy(UUID owner){return active()&&owner!=null&&data.getCompound("Owners").contains(owner.toString(),Tag.TAG_COMPOUND);}
    public boolean retired(UUID generation){if(!blocker.isEmpty()||generation==null)return false;for(var tag:data.getList("Retired",Tag.TAG_COMPOUND))if(((CompoundTag)tag).getUUID("Generation").equals(generation))return true;return false;}
    /** Small read-only views for live NPC and encounter admission. Callers
     * receive copies; no complete retirement/history journal is cloned. */
    /** The generation's first physical well outcome is immutable across personal replays. */
    public CompoundTag canonicalCompletion(){
        if(!active())return new CompoundTag();if(data.contains("CanonicalCompletion",Tag.TAG_COMPOUND))return data.getCompound("CanonicalCompletion").copy();
        CompoundTag candidate=null;for(String owner:data.getCompound("Owners").getAllKeys()){var row=data.getCompound("Owners").getCompound(owner);if(row.getLong("RecordedAt")!=data.getLong("ActivatedAt"))continue;if(candidate!=null)return new CompoundTag();candidate=row;}
        return candidate==null?new CompoundTag():candidate.copy();
    }
    public CompoundTag completionWitness(UUID owner){return active()&&owner!=null?data.getCompound("Owners").getCompound(owner.toString()).copy():new CompoundTag();}
    public Set<UUID> completionOwners(){if(!active())return Set.of();var owners=new HashSet<UUID>();for(String owner:data.getCompound("Owners").getAllKeys())owners.add(UUID.fromString(owner));return Set.copyOf(owners);}
    public CompoundTag save(){return data.copy();}
    boolean canRetire(){if(!active()||data.getList("Retired",Tag.TAG_COMPOUND).size()>=1024)return false;var witnesses=new HashSet<>(data.getCompound("RetiredWitnesses").getAllKeys());for(String owner:data.getCompound("Owners").getAllKeys()){var row=data.getCompound("Owners").getCompound(owner);witnesses.add(row.getUUID("KeyReceipt").toString());witnesses.add(row.getUUID("WellSession").toString());}return witnesses.size()<=65536;}
    /** Ending adapter only. The caller first returns native players and
     * durably hands infection snapshots to their independent cleanup queue.
     * Old well/key witnesses remain revoked after activation becomes dormant. */
    boolean retire(UUID expected,UUID reset,long gameTime){
        if(!blocker.isEmpty()||expected==null||reset==null||gameTime<0)return false;
        for(var tag:data.getList("Retired",Tag.TAG_COMPOUND)){var row=(CompoundTag)tag;if(row.getUUID("Generation").equals(expected)||row.getUUID("Reset").equals(reset))return !active()&&row.getUUID("Generation").equals(expected)&&row.getUUID("Reset").equals(reset);}
        if(!active()||!data.getUUID("Generation").equals(expected)||gameTime<data.getLong("ActivatedAt"))return false;
        var retired=data.getList("Retired",Tag.TAG_COMPOUND);var witnesses=data.getCompound("RetiredWitnesses").copy();if(retired.size()>=1024)return false;
        for(String owner:data.getCompound("Owners").getAllKeys()){var row=data.getCompound("Owners").getCompound(owner);witnesses.putUUID(row.getUUID("KeyReceipt").toString(),reset);witnesses.putUUID(row.getUUID("WellSession").toString(),reset);}if(witnesses.size()>65536)return false;
        var row=new CompoundTag();row.putUUID("Generation",expected);row.putUUID("Reset",reset);row.putLong("At",gameTime);retired.add(row);data.put("Retired",retired);data.put("RetiredWitnesses",witnesses);
        data.putBoolean("Activated",false);data.put("Owners",new CompoundTag());for(String key:List.of("Generation","Seed","ActivatedAt","TerrainVersion","CanonicalCompletion"))data.remove(key);return true;
    }
    static boolean checkpointMatches(CompoundTag expected,CompoundTag savedFile){
        if(!savedFile.contains("data",Tag.TAG_COMPOUND))return false;
        var persisted=load(savedFile.getCompound("data"));return persisted.active()&&load(expected).active()&&expected.equals(persisted.save());
    }
    /** Called only with canonical well/key-custody receipts, never a debug
     * completion flag. The injected source makes one-time entropy testable. */
    public Activation activate(Witness witness,long gameTime,long overworldSeed,LongSupplier independentRandom){
        if(!blocker.isEmpty()||witness==null||!witness.valid()||gameTime<0||independentRandom==null)return Activation.REFUSED;
        if(active()&&gameTime<data.getLong("ActivatedAt"))return Activation.REFUSED;
        var retired=data.getCompound("RetiredWitnesses");if(retired.contains(witness.keyReceipt().toString())||retired.contains(witness.wellSession().toString()))return Activation.REFUSED;
        var history=data.getCompound("RecordedWitnesses").copy();if(history.contains(witness.keyReceipt().toString())||history.contains(witness.wellSession().toString()))return Activation.REFUSED;
        boolean returningOwner=completedBy(witness.owner());
        if(returningOwner){
            var old=data.getCompound("Owners").getCompound(witness.owner().toString());
            boolean sameKey=old.getUUID("KeyReceipt").equals(witness.keyReceipt());
            boolean sameWell=old.getUUID("WellSession").equals(witness.wellSession());
            UUID originalWybie=old.hasUUID("Wybie")?old.getUUID("Wybie"):null;
            if(sameKey&&sameWell){
                return old.getUUID("Door").equals(witness.door())
                        &&Objects.equals(originalWybie,witness.wybie())&&old.getString("WybieLife").equals(witness.wybieLife().name())
                        ?Activation.ALREADY_RECORDED:Activation.REFUSED;
            }
            if(sameKey||sameWell||gameTime<old.getLong("RecordedAt")
                    ||originalWybie!=null&&!originalWybie.equals(witness.wybie())||history.size()>32766)return Activation.REFUSED;
            history.putUUID(old.getUUID("KeyReceipt").toString(),witness.owner());
            history.putUUID(old.getUUID("WellSession").toString(),witness.owner());
        }
        if(!returningOwner&&data.getCompound("Owners").size()>=4096)return Activation.REFUSED;
        // A copied disposal receipt cannot authorize a second player's unlock.
        for(String name:data.getCompound("Owners").getAllKeys()){
            var prior=data.getCompound("Owners").getCompound(name);
            if(witness.keyReceipt().equals(prior.getUUID("KeyReceipt"))||witness.wellSession().equals(prior.getUUID("WellSession")))return Activation.REFUSED;
        }
        boolean first=!active();
        if(first){
            long chosen;try{chosen=independentRandom.getAsLong();}catch(RuntimeException unavailable){return Activation.REFUSED;}
            // Preserve independent entropy even in the exceptionally unlikely
            // collision with the Overworld seed; never copy that seed.
            if(chosen==overworldSeed)chosen^=0x9E3779B97F4A7C15L;
            data.putUUID("Generation",UUID.randomUUID());data.putLong("Seed",chosen);data.putLong("ActivatedAt",gameTime);data.putInt("TerrainVersion",InfiniteTerrain.VERSION);data.putBoolean("Activated",true);
        }
        if(returningOwner)data.put("RecordedWitnesses",history);
        var row=new CompoundTag();row.putUUID("Owner",witness.owner());row.putUUID("Door",witness.door());row.putUUID("WellSession",witness.wellSession());row.putUUID("KeyReceipt",witness.keyReceipt());row.putLong("RecordedAt",gameTime);
        row.putString("WybieLife",witness.wybieLife().name());if(witness.wybie()!=null)row.putUUID("Wybie",witness.wybie());
        if(first)data.put("CanonicalCompletion",row.copy());
        else if(!data.contains("CanonicalCompletion",Tag.TAG_COMPOUND)){var canonical=canonicalCompletion();if(!canonical.isEmpty())data.put("CanonicalCompletion",canonical);}
        data.getCompound("Owners").put(witness.owner().toString(),row);return first?Activation.CREATED:Activation.OWNER_RECORDED;
    }
}
