package com.coraline.postgame;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Fixed 32x32-chunk shards. Exploration never requires loading old world
 * chunks or scanning an ever-growing global list on every player tick. */
final class InfiniteDiscoveryRegion extends SavedData {
    private final UUID generation;
    private final int regionX,regionZ;
    private CompoundTag root;
    private boolean valid;
    private InfiniteDiscoveryRegion(UUID generation,int x,int z){this.generation=generation;regionX=x;regionZ=z;root=new CompoundTag();root.putInt("Version",1);root.putUUID("Generation",generation);root.putInt("RegionX",x);root.putInt("RegionZ",z);root.put("Owners",new CompoundTag());valid=true;}
    private InfiniteDiscoveryRegion(UUID generation,int x,int z,CompoundTag saved){this(generation,x,z);root=saved.copy();valid=validate();}
    static InfiniteDiscoveryRegion get(ServerLevel level,UUID generation,int chunkX,int chunkZ){
        int x=chunkX>>5,z=chunkZ>>5;String name="coraline_discovery_"+generation+"_"+x+"_"+z;
        var file=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/"+name+".dat");
        return level.getServer().overworld().getDataStorage().computeIfAbsent(n->new InfiniteDiscoveryRegion(generation,x,z,n),()->java.nio.file.Files.notExists(file,java.nio.file.LinkOption.NOFOLLOW_LINKS)?new InfiniteDiscoveryRegion(generation,x,z):new InfiniteDiscoveryRegion(generation,x,z,new CompoundTag()),name);
    }
    private boolean validate(){
        if(!root.contains("Version",Tag.TAG_INT)||root.getInt("Version")!=1||!root.hasUUID("Generation")||!generation.equals(root.getUUID("Generation"))||!root.contains("RegionX",Tag.TAG_INT)||root.getInt("RegionX")!=regionX||!root.contains("RegionZ",Tag.TAG_INT)||root.getInt("RegionZ")!=regionZ||!root.contains("Owners",Tag.TAG_COMPOUND))return false;
        if(root.contains("Finished")){if(!root.contains("Finished",Tag.TAG_COMPOUND)||root.getCompound("Finished").size()>64)return false;try{for(String key:root.getCompound("Finished").getAllKeys()){if(!UUID.fromString(key).toString().equals(key)||!root.getCompound("Finished").contains(key,Tag.TAG_COMPOUND))return false;var n=root.getCompound("Finished").getCompound(key);if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Owner")||!n.hasUUID("Receipt")||!n.contains("At",Tag.TAG_LONG)||n.getLong("At")<0||!n.contains("Family",Tag.TAG_STRING))return false;InfiniteStructurePlan.Family.valueOf(n.getString("Family"));if(n.contains("Participants")){if(!n.contains("Participants",Tag.TAG_LIST))return false;var participants=n.getList("Participants",Tag.TAG_STRING);if(participants.size()!=((ListTag)n.get("Participants")).size()||participants.size()>4096)return false;var unique=new HashSet<UUID>();for(var participant:participants){var id=UUID.fromString(participant.getAsString());if(!id.toString().equals(participant.getAsString())||!unique.add(id))return false;}}}}catch(IllegalArgumentException unknown){return false;}}
        if(root.contains("Contributors")){
            if(!root.contains("Contributors",Tag.TAG_COMPOUND)||root.getCompound("Contributors").size()>64)return false;
            try{for(String site:root.getCompound("Contributors").getAllKeys()){
                if(!UUID.fromString(site).toString().equals(site)||!root.getCompound("Contributors").contains(site,Tag.TAG_COMPOUND))return false;
                var people=root.getCompound("Contributors").getCompound(site);if(people.size()>4096)return false;
                for(String player:people.getAllKeys())if(!UUID.fromString(player).toString().equals(player)||!people.contains(player,Tag.TAG_LONG)||people.getLong(player)<0)return false;
            }}catch(IllegalArgumentException malformed){return false;}
        }
        var owners=root.getCompound("Owners");if(owners.size()>4096)return false;
        try{for(String key:owners.getAllKeys()){
            if(!UUID.fromString(key).toString().equals(key)||!owners.contains(key,Tag.TAG_COMPOUND))return false;var row=owners.getCompound(key);
            if(!row.contains("Viewed",Tag.TAG_LONG_ARRAY)||row.getLongArray("Viewed").length!=16||!row.contains("Sites",Tag.TAG_LIST)||!row.contains("Biomes",Tag.TAG_LIST))return false;
            var sites=row.getList("Sites",Tag.TAG_COMPOUND);var biomes=row.getList("Biomes",Tag.TAG_STRING);
            if(sites.size()!=((ListTag)row.get("Sites")).size()||sites.size()>32||biomes.size()!=((ListTag)row.get("Biomes")).size()||biomes.size()>32)return false;
            var seen=new HashSet<UUID>();for(var raw:sites){var site=(CompoundTag)raw;
                if(!site.hasUUID("Site")||!seen.add(site.getUUID("Site"))||!site.contains("Family",Tag.TAG_STRING)||!site.contains("Position",Tag.TAG_LONG)||!site.contains("Difficulty",Tag.TAG_INT))return false;
                var family=InfiniteStructurePlan.Family.valueOf(site.getString("Family"));var at=net.minecraft.core.BlockPos.of(site.getLong("Position"));
                if(family.difficulty!=site.getInt("Difficulty")||at.getX()>>9!=regionX||at.getZ()>>9!=regionZ||at.getY()< -64||at.getY()>320||!site.contains("Hooks",Tag.TAG_LIST))return false;
                var hooks=site.getList("Hooks",Tag.TAG_STRING);if(hooks.size()!=family.hooks.size()||hooks.size()!=((ListTag)site.get("Hooks")).size())return false;for(int i=0;i<hooks.size();i++)if(!family.hooks.get(i).equals(hooks.getString(i)))return false;
            }
            var names=new HashSet<String>();for(var biome:biomes)if(!names.add(biome.getAsString())||net.minecraft.resources.ResourceLocation.tryParse(biome.getAsString())==null)return false;
        }}catch(IllegalArgumentException malformed){return false;}return true;
    }
    private CompoundTag owner(UUID id){if(!valid)return null;var owners=root.getCompound("Owners");String key=id.toString();if(!owners.contains(key)){if(owners.size()>=4096)return null;var row=new CompoundTag();row.putLongArray("Viewed",new long[16]);row.put("Sites",new ListTag());row.put("Biomes",new ListTag());owners.put(key,row);setDirty();}return owners.getCompound(key);}
    boolean visit(UUID owner,int chunkX,int chunkZ){if(chunkX>>5!=regionX||chunkZ>>5!=regionZ)return false;var row=owner(owner);if(row==null)return false;var words=row.getLongArray("Viewed");int index=(chunkX&31)+((chunkZ&31)<<5),word=index>>6;long bit=1L<<(index&63);if((words[word]&bit)!=0)return false;words[word]|=bit;row.putLongArray("Viewed",words);setDirty();return true;}
    boolean biome(UUID owner,String biome){var row=owner(owner);if(row==null||net.minecraft.resources.ResourceLocation.tryParse(biome)==null)return false;var list=row.getList("Biomes",Tag.TAG_STRING);if(list.size()>=32||list.stream().anyMatch(n->n.getAsString().equals(biome)))return false;list.add(StringTag.valueOf(biome));setDirty();return true;}
    boolean site(UUID owner,InfiniteStructurePlan.Plan plan){if(!plan.generation().equals(generation)||plan.x()>>9!=regionX||plan.z()>>9!=regionZ)return false;var row=owner(owner);if(row==null)return false;var sites=row.getList("Sites",Tag.TAG_COMPOUND);if(sites.size()>=32||sites.stream().anyMatch(n->((CompoundTag)n).getUUID("Site").equals(plan.site())))return false;
        var n=new CompoundTag();n.putUUID("Site",plan.site());n.putString("Family",plan.family().name());n.putLong("Position",new net.minecraft.core.BlockPos(plan.x(),plan.y(),plan.z()).asLong());n.putInt("Difficulty",plan.family().difficulty);var hooks=new ListTag();for(String hook:plan.family().hooks)hooks.add(StringTag.valueOf(hook));n.put("Hooks",hooks);sites.add(n);setDirty();return true;
    }
    /** Query only the relevant already-known shard; no fabricated coordinates. */
    List<CompoundTag> knownSites(UUID owner){if(!valid)return List.of();var owners=root.getCompound("Owners");if(!owners.contains(owner.toString(),Tag.TAG_COMPOUND))return List.of();var result=new ArrayList<CompoundTag>();for(var raw:owners.getCompound(owner.toString()).getList("Sites",Tag.TAG_COMPOUND))result.add(((CompoundTag)raw).copy());return List.copyOf(result);}
    boolean usable(){return valid;}
    CompoundTag finished(UUID site){return valid&&root.getCompound("Finished").contains(site.toString(),Tag.TAG_COMPOUND)?root.getCompound("Finished").getCompound(site.toString()).copy():null;}
    /** Participation is recorded while the objective is still open. Completion
     * seals the list; arriving after victory never grants retroactive credit. */
    void participate(InfiniteStructurePlan.Plan plan,UUID player,long now){
        if(!valid||player==null||now<0||!plan.generation().equals(generation)||plan.x()>>9!=regionX||plan.z()>>9!=regionZ||finished(plan.site())!=null)return;
        if(!root.contains("Contributors"))root.put("Contributors",new CompoundTag());
        var sites=root.getCompound("Contributors");String site=plan.site().toString();
        if(!sites.contains(site)){if(sites.size()>=64)return;sites.put(site,new CompoundTag());}
        var people=sites.getCompound(site);String key=player.toString();
        if(!people.contains(key)&&people.size()>=4096)return;
        if(people.contains(key)&&now-people.getLong(key)<100&&now>=people.getLong(key))return;
        people.putLong(key,now);setDirty();
    }
    static boolean credited(CompoundTag proof,UUID player){
        if(proof==null||player==null)return false;if(proof.getUUID("Owner").equals(player))return true;
        return proof.getList("Participants",Tag.TAG_STRING).stream().anyMatch(n->n.getAsString().equals(player.toString()));
    }
    void finish(InfiniteStructurePlan.Plan plan,UUID owner,long now){if(!valid||!plan.generation().equals(generation)||plan.x()>>9!=regionX||plan.z()>>9!=regionZ||now<0)return;if(!root.contains("Finished"))root.put("Finished",new CompoundTag());var all=root.getCompound("Finished");if(all.contains(plan.site().toString())||all.size()>=64)return;var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Owner",owner);n.putUUID("Receipt",UUID.randomUUID());n.putLong("At",now);n.putString("Family",plan.family().name());var participants=new ListTag();var people=root.getCompound("Contributors").getCompound(plan.site().toString());
        for(String player:people.getAllKeys())if(now>=people.getLong(player)&&now-people.getLong(player)<=200)participants.add(StringTag.valueOf(player));
        n.put("Participants",participants);root.getCompound("Contributors").remove(plan.site().toString());all.put(plan.site().toString(),n);setDirty();}
    @Override public CompoundTag save(CompoundTag target){target.merge(root.copy());return target;}
}
