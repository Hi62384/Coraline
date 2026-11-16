package com.coraline.postgame;

import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Three native products per factory, with issuance saved before insertion.
 * Dead, absent and unloaded workers are never replenished. */
final class FactoryBatchData extends SavedData {
    private final UUID generation;private final int x,z;private final CompoundTag raw;private boolean valid;
    private FactoryBatchData(UUID generation,int x,int z){this.generation=generation;this.x=x;this.z=z;raw=new CompoundTag();raw.putInt("Version",1);raw.putUUID("Generation",generation);raw.putInt("RegionX",x);raw.putInt("RegionZ",z);raw.put("Sites",new CompoundTag());valid=true;}
    private FactoryBatchData(UUID generation,int x,int z,CompoundTag saved){this.generation=generation;this.x=x;this.z=z;raw=saved.copy();valid=validate();}
    private static String name(UUID generation,int x,int z){return "coraline_factory_batches_"+generation+"_"+x+"_"+z;}
    static FactoryBatchData get(MinecraftServer server,InfiniteStructurePlan.Plan plan){int x=plan.x()>>9,z=plan.z()>>9;var file=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name(plan.generation(),x,z)+".dat");return server.overworld().getDataStorage().computeIfAbsent(n->new FactoryBatchData(plan.generation(),x,z,n),()->Files.notExists(file,LinkOption.NOFOLLOW_LINKS)?new FactoryBatchData(plan.generation(),x,z):new FactoryBatchData(plan.generation(),x,z,new CompoundTag()),name(plan.generation(),x,z));}
    static UUID worker(UUID site,int index){return UUID.nameUUIDFromBytes((site+":factory-product:"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private boolean validate(){try{if(!raw.contains("Version",Tag.TAG_INT)||raw.getInt("Version")!=1||!raw.hasUUID("Generation")||!generation.equals(raw.getUUID("Generation"))||!raw.contains("RegionX",Tag.TAG_INT)||raw.getInt("RegionX")!=x||!raw.contains("RegionZ",Tag.TAG_INT)||raw.getInt("RegionZ")!=z||!raw.contains("Sites",Tag.TAG_COMPOUND)||raw.getCompound("Sites").size()>64)return false;
        for(String key:raw.getCompound("Sites").getAllKeys()){var site=UUID.fromString(key);var row=raw.getCompound("Sites").getCompound(key);if(!site.toString().equals(key)||!raw.getCompound("Sites").contains(key,Tag.TAG_COMPOUND)||!row.contains("Workers",Tag.TAG_LIST)||!row.contains("Stopped",Tag.TAG_BYTE)||row.getByte("Stopped")<0||row.getByte("Stopped")>1)return false;var list=row.getList("Workers",Tag.TAG_INT_ARRAY);if(list.size()>3||list.size()!=((ListTag)row.get("Workers")).size())return false;for(int i=0;i<list.size();i++)if(!(list.get(i) instanceof IntArrayTag a)||a.getAsIntArray().length!=4||!worker(site,i).equals(NbtUtils.loadUUID(a)))return false;if(row.getBoolean("Stopped")&&(!row.hasUUID("StoppedBy")||!row.contains("StoppedAt",Tag.TAG_LONG)||row.getLong("StoppedAt")<0))return false;}
        return true;}catch(IllegalArgumentException bad){return false;}}
    boolean ready(){return valid;}
    int issued(UUID site){return valid?raw.getCompound("Sites").getCompound(site.toString()).getList("Workers",Tag.TAG_INT_ARRAY).size():3;}
    boolean stopped(UUID site){return valid&&raw.getCompound("Sites").getCompound(site.toString()).getBoolean("Stopped");}
    private CompoundTag row(UUID site){var sites=raw.getCompound("Sites");if(!sites.contains(site.toString())){if(sites.size()>=64)return null;var row=new CompoundTag();row.put("Workers",new ListTag());row.putBoolean("Stopped",false);sites.put(site.toString(),row);}return sites.getCompound(site.toString());}
    private boolean checkpoint(MinecraftServer server){if(!valid)return false;try{var file=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name(generation,x,z)+".dat");if(Files.isSymbolicLink(file)||Files.isSymbolicLink(file.getParent()))return false;setDirty();server.overworld().getDataStorage().save();if(!com.coraline.rebirth.StoryRunStore.read(file).getCompound("data").equals(raw)){valid=false;return false;}return true;}catch(java.io.IOException|RuntimeException failed){valid=false;return false;}}
    boolean issue(MinecraftServer server,InfiniteStructurePlan.Plan plan,int index){if(!valid||!generation.equals(plan.generation())||index<0||index>=3||issued(plan.site())!=index||stopped(plan.site()))return false;var row=row(plan.site());if(row==null)return false;row.getList("Workers",Tag.TAG_INT_ARRAY).add(NbtUtils.createUUID(worker(plan.site(),index)));return checkpoint(server);}
    boolean stop(MinecraftServer server,InfiniteStructurePlan.Plan plan,UUID owner){if(!valid||owner==null||!generation.equals(plan.generation()))return false;if(stopped(plan.site()))return true;var row=row(plan.site());if(row==null)return false;row.putBoolean("Stopped",true);row.putUUID("StoppedBy",owner);row.putLong("StoppedAt",server.overworld().getGameTime());return checkpoint(server);}
    @Override public CompoundTag save(CompoundTag tag){return tag.merge(raw.copy());}
}
