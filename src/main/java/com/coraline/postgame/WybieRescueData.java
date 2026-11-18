package com.coraline.postgame;

import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** A generation-owned identity lease and quest journal. No entity snapshots,
 * spawn recipes, inventory restoration or implicit repair of unknown data. */
final class WybieRescueData extends SavedData {
    enum Stage { SEARCH, TRAIL_ONE, TRAIL_TWO, TRAIL_THREE, CAGE, ESCORT, RETURNING, COMPLETE, FAILED, MOVING, STORY_LOAN, ARCHIVED }
    private final UUID generation;private CompoundTag raw;private String problem="";
    private WybieRescueData(UUID generation){this.generation=generation;raw=new CompoundTag();raw.putInt("Version",1);raw.putUUID("Generation",generation);raw.put("Owners",new CompoundTag());}
    private WybieRescueData(UUID generation,CompoundTag n){this.generation=generation;raw=n.copy();try{
        if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Generation")||!generation.equals(n.getUUID("Generation"))||!n.contains("Owners",Tag.TAG_COMPOUND)||n.getCompound("Owners").size()>4096)throw new IllegalArgumentException("Invalid rescue journal");
        var actors=new HashSet<UUID>();for(String owner:n.getCompound("Owners").getAllKeys()){if(!UUID.fromString(owner).toString().equals(owner)||!n.getCompound("Owners").contains(owner,Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid rescue owner");var row=n.getCompound("Owners").getCompound(owner);if(!valid(row)||!actors.add(row.getUUID("Actor")))throw new IllegalArgumentException("Ambiguous original actor lease");}
        if(n.contains("Coraline")&&(!n.contains("Coraline",Tag.TAG_COMPOUND)||!n.getCompound("Coraline").hasUUID("Actor")||!n.getCompound("Coraline").contains("Home",Tag.TAG_LONG)))throw new IllegalArgumentException("Invalid Coraline identity");
    }catch(IllegalArgumentException unknown){problem=unknown.getMessage();}}
    private static boolean valid(CompoundTag n){try{for(String k:List.of("Actor","Door","WellSession"))if(!n.hasUUID(k))return false;if(!n.contains("Stage",Tag.TAG_STRING)||!n.contains("Dimension",Tag.TAG_STRING)||net.minecraft.resources.ResourceLocation.tryParse(n.getString("Dimension"))==null||!n.contains("Position",Tag.TAG_LONG))return false;var at=net.minecraft.core.BlockPos.of(n.getLong("Position"));if(Math.abs((long)at.getX())>29999920||Math.abs((long)at.getZ())>29999920||at.getY()< -2048||at.getY()>2047)return false;var stage=Stage.valueOf(n.getString("Stage"));if(n.contains("Origin")&&(!n.contains("Origin",Tag.TAG_STRING)||!n.getString("Origin").equals("SURVIVED")))return false;if(stage==Stage.STORY_LOAN&&(!n.hasUUID("LoanDoor")||!n.hasUUID("Completion")||!n.contains("RewardIssued",Tag.TAG_BYTE)||!n.getBoolean("RewardIssued")&&!n.hasUUID("RewardRecipient")))return false;if(n.contains("RewardRecipient")&&!n.hasUUID("RewardRecipient"))return false;if(stage==Stage.COMPLETE&&(!n.hasUUID("Completion")||!n.contains("RewardIssued",Tag.TAG_BYTE)||!n.getBoolean("RewardIssued")&&!n.hasUUID("RewardRecipient")))return false;if(n.contains("PreviousDimension")&&(!n.contains("PreviousDimension",Tag.TAG_STRING)||net.minecraft.resources.ResourceLocation.tryParse(n.getString("PreviousDimension"))==null||!n.contains("PreviousPosition",Tag.TAG_LONG)))return false;if(n.contains("Site")&&!n.hasUUID("Generation"))return false;if(stage==Stage.MOVING&&(!n.contains("TargetDimension",Tag.TAG_STRING)||net.minecraft.resources.ResourceLocation.tryParse(n.getString("TargetDimension"))==null||!n.contains("TargetPosition",Tag.TAG_LONG)||!n.contains("AfterMove",Tag.TAG_STRING)||Stage.valueOf(n.getString("AfterMove"))==Stage.MOVING))return false;if(n.contains("Site")&&(!n.hasUUID("Site")||!n.contains("AnchorX",Tag.TAG_INT)||!n.contains("AnchorZ",Tag.TAG_INT)||!n.contains("Seed",Tag.TAG_LONG)))return false;return true;}catch(IllegalArgumentException bad){return false;}}
    private static String name(UUID id){return "coraline_wybie_rescue_"+id;}
    static WybieRescueData get(MinecraftServer server,UUID generation){var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name(generation)+".dat");return server.overworld().getDataStorage().computeIfAbsent(n->new WybieRescueData(generation,n),()->Files.notExists(path,LinkOption.NOFOLLOW_LINKS)?new WybieRescueData(generation):new WybieRescueData(generation,new CompoundTag()),name(generation));}
    String problem(){return problem;}
    CompoundTag row(UUID owner){return problem.isEmpty()&&raw.getCompound("Owners").contains(owner.toString(),Tag.TAG_COMPOUND)?raw.getCompound("Owners").getCompound(owner.toString()):null;}
    boolean claim(MinecraftServer server,UUID owner,CompoundTag row){
        if(!valid(row))return false;var canonical=InfiniteWorldData.get(server).canonicalCompletion();
        if(!canonical.hasUUID("Owner")||!canonical.getUUID("Owner").equals(owner)||!canonical.hasUUID("Wybie")||!row.hasUUID("Actor")||!canonical.getUUID("Wybie").equals(row.getUUID("Actor"))||!canonical.getUUID("Door").equals(row.getUUID("Door"))||!canonical.getUUID("WellSession").equals(row.getUUID("WellSession")))return false;
        if(!problem.isEmpty()||row(owner)!=null||!valid(row)||!raw.getCompound("Owners").isEmpty())return false;for(String key:raw.getCompound("Owners").getAllKeys())if(raw.getCompound("Owners").getCompound(key).getUUID("Actor").equals(row.getUUID("Actor")))return false;raw.getCompound("Owners").put(owner.toString(),row.copy());return checkpoint(server);}
    UUID sharedOwner(MinecraftServer server){var witness=InfiniteWorldData.get(server).canonicalCompletion();return problem.isEmpty()&&witness.hasUUID("Owner")?witness.getUUID("Owner"):null;}
    CompoundTag shared(MinecraftServer server){var owner=sharedOwner(server);var n=owner==null?null:row(owner);if(n==null)return null;var witness=InfiniteWorldData.get(server).canonicalCompletion();return witness.hasUUID("Wybie")&&witness.getUUID("Wybie").equals(n.getUUID("Actor"))&&witness.getUUID("Door").equals(n.getUUID("Door"))&&witness.getUUID("WellSession").equals(n.getUUID("WellSession"))?n:null;}
    boolean migrateLegacy(MinecraftServer server){
        var canonical=sharedOwner(server);if(canonical==null)return raw.getCompound("Owners").isEmpty();
        for(String key:new java.util.ArrayList<>(raw.getCompound("Owners").getAllKeys())){
            if(key.equals(canonical.toString()))continue;var row=raw.getCompound("Owners").getCompound(key);if(row.getString("Stage").equals("ARCHIVED"))continue;
            var level=com.coraline.parents.FamilyData.level(server,row.getString("Dimension"));if(level!=null)com.coraline.feast.LoopProps.hold(level,net.minecraft.core.BlockPos.of(row.getLong("Position")));
            com.coraline.entity.CharacterEntity actor=null;for(var world:server.getAllLevels())if(world.getEntity(row.getUUID("Actor")) instanceof com.coraline.entity.CharacterEntity found){if(actor!=null)return false;actor=found;}
            if(actor==null)return false;row.putBoolean("LegacyRetiring",true);if(!checkpoint(server))return false;
            if(!com.coraline.feast.CanonicalRealWybie.retireLegacyPostgame(server,UUID.fromString(key),row.getUUID("Door"),actor))return false;
            // Locate the same UUID after native dimensional travel.
            com.coraline.entity.CharacterEntity original=null;for(var world:server.getAllLevels())if(world.getEntity(row.getUUID("Actor")) instanceof com.coraline.entity.CharacterEntity found){if(original!=null)return false;original=found;}if(original==null||!com.coraline.feast.CanonicalRealWybie.dormant(original))return false;
            original.getPersistentData().remove("CoralinePostgameWybieLease");row.putString("Dimension",original.level().dimension().location().toString());row.putLong("Position",original.blockPosition().asLong());row.putString("Stage","ARCHIVED");row.remove("LegacyRetiring");if(!server.saveEverything(true,true,true)||!checkpoint(server))return false;return false;
        }return true;
    }
    CompoundTag coraline(){return problem.isEmpty()&&raw.contains("Coraline",Tag.TAG_COMPOUND)?raw.getCompound("Coraline"):null;}
    boolean admitCoraline(MinecraftServer server,UUID actor,net.minecraft.core.BlockPos home){if(!problem.isEmpty()||raw.contains("Coraline"))return false;var n=new CompoundTag();n.putUUID("Actor",actor);n.putLong("Home",home.asLong());n.putBoolean("Pending",true);raw.put("Coraline",n);return checkpoint(server);}
    boolean reopenCoraline(MinecraftServer server,UUID identity){
        var record=coraline();
        if(record==null||!identity.equals(record.getUUID("Actor"))||record.contains("Pending"))return false;
        record.putBoolean("Pending",true);
        return checkpoint(server);
    }
    boolean finishCoraline(MinecraftServer server){
        var record=coraline();
        if(record==null)return false;
        if(record.contains("Pending",Tag.TAG_BYTE)&&!record.getBoolean("Pending"))return true;
        record.putBoolean("Pending",false);
        if(checkpoint(server))return true;
        record.putBoolean("Pending",true);
        return false;
    }
    boolean checkpoint(MinecraftServer server){if(!problem.isEmpty())return false;try{var path=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name(generation)+".dat");if(Files.isSymbolicLink(path)||Files.isSymbolicLink(path.getParent()))throw new java.io.IOException("Ambiguous rescue storage");setDirty();server.overworld().getDataStorage().save();var disk=com.coraline.rebirth.StoryRunStore.read(path);if(!disk.contains("data",Tag.TAG_COMPOUND)||!disk.getCompound("data").equals(raw))throw new java.io.IOException("Rescue checkpoint did not match");return true;}catch(java.io.IOException|RuntimeException failed){problem="The rescue checkpoint could not be saved safely";return false;}}
    @Override public CompoundTag save(CompoundTag n){return n.merge(raw.copy());}
}
