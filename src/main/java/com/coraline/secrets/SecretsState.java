package com.coraline.secrets;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.core.BlockPos;

/** Stored inside the existing storyline ledger, never client item NBT. */
public final class SecretsState {
    public enum Search { NORMAL, MISSED_ACTIVITY_PENDING, SEARCHING, INVESTIGATING_NOISE, DIRECT_CHASE, CONFRONTING, RETURNING_HOME, SCRIPTED_TRAP }
    public final Set<String> journal=new LinkedHashSet<>(),exposed=new LinkedHashSet<>(),suspected=new LinkedHashSet<>();
    public final Map<UUID,String> issued=new LinkedHashMap<>();
    public final Map<UUID,List<String>> blackmail=new LinkedHashMap<>();
    public final Map<String,Long> cooldowns=new HashMap<>();
    public final Set<String> claimed=new HashSet<>();
    public final Set<Integer> unlocked=new HashSet<>();
    public CompoundTag world=new CompoundTag(),dialogue=new CompoundTag();
    public int hiding,visit=-1,searchTicks,rescued,pendingReward=-1;
    public long seed,pendingTicks,invisibleUntil,falseUntil,combatUntil;
    public double inspectionProgress;
    public Search search=Search.NORMAL;
    public BlockPos target=BlockPos.ZERO;
    public double[] overrides=new double[]{-1,-1,-1,-1,-1,-1};
    private int[] totals=new int[6],counters=new int[6];
    private Set<String> abilities=Set.of();
    public void recalculate(){totals=SecretRegistry.totals(journal);counters=SecretRegistry.totals(exposed);abilities=Set.copyOf(SecretRegistry.abilities(journal));}
    public int value(int i){return totals[i];}
    public double counter(int i){return overrides[i]>=0?overrides[i]:counters[i]/100.;}
    public double effective(int i){return Math.max(0,value(i)/100.-counter(i));}
    public double soulWeakening(){return Math.min(.15,Math.max(0,rescued)*.025);}
    public boolean ability(String id){return abilities.contains(id);}
    public boolean searching(){return search==Search.SEARCHING||search==Search.INVESTIGATING_NOISE||search==Search.DIRECT_CHASE;}
    public static double inspectionMinutes(double suspicion){return Math.max(4,suspicion<=.5?50-80*suspicion:10-6*(suspicion-.5)/.49);}
    static ListTag strings(Collection<String> values){var list=new ListTag();for(String s:values)list.add(StringTag.valueOf(s));return list;}
    static Set<String> strings(CompoundTag n,String key){var result=new LinkedHashSet<String>();for(var s:n.getList(key,8)){String id=s.getAsString();if(SecretRegistry.ALL.containsKey(id))result.add(id);}return result;}
    public CompoundTag save(){var n=new CompoundTag();n.put("Journal",strings(journal));n.put("Exposed",strings(exposed));n.put("Suspected",strings(suspected));n.putInt("Hiding",hiding);n.putInt("Visit",visit);n.putLong("Seed",seed);n.putDouble("Inspection",inspectionProgress);n.putString("Search",search.name());n.putLong("Pending",pendingTicks);n.putInt("SearchTicks",searchTicks);n.putLong("Target",target.asLong());n.putLong("Invisible",invisibleUntil);n.putLong("False",falseUntil);n.putLong("Combat",combatUntil);n.putInt("Rescued",rescued);n.putInt("Reward",pendingReward);n.put("World",world.copy());n.put("Dialogue",dialogue.copy());
        var issuedTag=new CompoundTag();issued.forEach((id,secret)->issuedTag.putString(id.toString(),secret));n.put("Issued",issuedTag);
        var mail=new CompoundTag();blackmail.forEach((id,ids)->mail.put(id.toString(),strings(ids)));n.put("Blackmail",mail);
        var cooldown=new CompoundTag();cooldowns.forEach(cooldown::putLong);n.put("Cooldowns",cooldown);
        var claims=new ListTag();claimed.forEach(s->claims.add(StringTag.valueOf(s)));n.put("Claimed",claims);n.putIntArray("Unlocked",unlocked.stream().mapToInt(Integer::intValue).toArray());
        for(int i=0;i<6;i++)n.putDouble("Override"+i,overrides[i]);return n;
    }
    public static SecretsState load(CompoundTag n){var s=new SecretsState();s.journal.addAll(strings(n,"Journal"));s.exposed.addAll(strings(n,"Exposed"));s.suspected.addAll(strings(n,"Suspected"));s.hiding=Math.max(0,Math.min(5,n.getInt("Hiding")));s.visit=n.contains("Visit")?n.getInt("Visit"):-1;s.seed=n.getLong("Seed");s.inspectionProgress=Double.isFinite(n.getDouble("Inspection"))?Math.max(0,Math.min(1,n.getDouble("Inspection"))):0;try{s.search=Search.valueOf(n.getString("Search"));}catch(Exception ignored){}s.pendingTicks=n.getLong("Pending");s.searchTicks=n.getInt("SearchTicks");s.target=BlockPos.of(n.getLong("Target"));s.invisibleUntil=n.getLong("Invisible");s.falseUntil=n.getLong("False");s.combatUntil=n.getLong("Combat");s.rescued=n.getInt("Rescued");s.pendingReward=n.contains("Reward")?n.getInt("Reward"):-1;s.world=n.getCompound("World").copy();s.dialogue=n.getCompound("Dialogue").copy();
        var issued=n.getCompound("Issued");for(String key:issued.getAllKeys())try{if(SecretRegistry.ALL.containsKey(issued.getString(key)))s.issued.put(UUID.fromString(key),issued.getString(key));}catch(Exception ignored){}
        var mail=n.getCompound("Blackmail");for(String key:mail.getAllKeys())try{s.blackmail.put(UUID.fromString(key),List.copyOf(strings(mail,key)));}catch(Exception ignored){}
        var cooldown=n.getCompound("Cooldowns");for(String key:cooldown.getAllKeys())s.cooldowns.put(key,cooldown.getLong(key));for(var tag:n.getList("Claimed",8))s.claimed.add(tag.getAsString());for(int i:n.getIntArray("Unlocked"))s.unlocked.add(i);
        for(int i=0;i<6;i++)s.overrides[i]=n.contains("Override"+i)?(Double.isFinite(n.getDouble("Override"+i))?Math.max(-1,Math.min(1,n.getDouble("Override"+i))):-1):-1;s.recalculate();return s;
    }
}
