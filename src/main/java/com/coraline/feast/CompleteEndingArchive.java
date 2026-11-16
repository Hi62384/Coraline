package com.coraline.feast;

import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import com.coraline.palace.PalaceWybieIdentity;

/** Reset-owned native identity handoff survives the absence of a Visit Scene.
 * These records are locations and receipts, never entity spawn snapshots. */
final class CompleteEndingArchive extends SavedData {
    private final UUID reset;private final CompoundTag raw;private String problem="";
    private CompleteEndingArchive(UUID reset){this.reset=reset;raw=new CompoundTag();raw.putInt("Version",1);raw.putUUID("Reset",reset);raw.put("Owners",new CompoundTag());}
    private CompleteEndingArchive(UUID reset,CompoundTag n){this.reset=reset;raw=n.copy();try{
        if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Reset")||!reset.equals(n.getUUID("Reset"))||!n.contains("Owners",Tag.TAG_COMPOUND)||n.getCompound("Owners").size()>4096)throw new IllegalArgumentException("Invalid ending archive");
        var actors=new HashSet<UUID>();for(String key:n.getCompound("Owners").getAllKeys()){var owner=UUID.fromString(key);if(!owner.toString().equals(key)||!n.getCompound("Owners").contains(key,Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid archive owner");var row=n.getCompound("Owners").getCompound(key);var identity=identity(owner,row.getCompound("Identity"));if(identity==null||!actors.add(identity.entity())||!row.contains("Destination",Tag.TAG_LONG))throw new IllegalArgumentException("Invalid archive identity");var at=net.minecraft.core.BlockPos.of(row.getLong("Destination"));if(Math.abs((long)at.getX())>29999920||Math.abs((long)at.getZ())>29999920||at.getY()< -64||at.getY()>319)throw new IllegalArgumentException("Invalid archive landing");if(row.contains("Archived")){var result=identity(owner,row.getCompound("Archived"));if(!row.contains("Archived",Tag.TAG_COMPOUND)||result==null||!result.entity().equals(identity.entity())||!result.stored()||!result.dimension().equals("minecraft:overworld")||result.position().asLong()!=row.getLong("Destination"))throw new IllegalArgumentException("Invalid archive receipt");}}
    }catch(IllegalArgumentException bad){problem=bad.getMessage();}}
    private static PalaceWybieIdentity identity(UUID owner,CompoundTag n){if(!n.hasUUID("Door"))return null;var identity=PalaceWybieIdentity.load(owner,n.getUUID("Door"),n);return identity.blocker().isEmpty()?identity:null;}
    private static String name(UUID reset){return "coraline_ending_wybie_"+reset;}
    static CompleteEndingArchive get(MinecraftServer server,UUID reset){var file=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name(reset)+".dat");return server.overworld().getDataStorage().computeIfAbsent(n->new CompleteEndingArchive(reset,n),()->Files.notExists(file,LinkOption.NOFOLLOW_LINKS)?new CompleteEndingArchive(reset):new CompleteEndingArchive(reset,new CompoundTag()),name(reset));}
    boolean ready(){return problem.isEmpty();}
    CompoundTag row(UUID owner){return ready()&&raw.getCompound("Owners").contains(owner.toString(),Tag.TAG_COMPOUND)?raw.getCompound("Owners").getCompound(owner.toString()):null;}
    boolean admit(MinecraftServer server,UUID owner,CompoundTag identity,net.minecraft.core.BlockPos destination){if(!ready()||row(owner)!=null||raw.getCompound("Owners").size()>=4096||identity(owner,identity)==null)return false;for(String key:raw.getCompound("Owners").getAllKeys())if(raw.getCompound("Owners").getCompound(key).getCompound("Identity").getUUID("Entity").equals(identity.getUUID("Entity")))return false;var row=new CompoundTag();row.put("Identity",identity.copy());row.putLong("Destination",destination.asLong());raw.getCompound("Owners").put(owner.toString(),row);return commit(server);}
    boolean archived(MinecraftServer server,UUID owner,CompoundTag identity){var row=row(owner);if(row==null||identity(owner,identity)==null||!row.getCompound("Identity").getUUID("Entity").equals(identity.getUUID("Entity"))||!identity.getBoolean("Stored")||!identity.getString("Dimension").equals("minecraft:overworld")||identity.getLong("Position")!=row.getLong("Destination"))return false;row.put("Archived",identity.copy());return commit(server);}
    private boolean commit(MinecraftServer server){if(!ready())return false;try{var file=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name(reset)+".dat");if(Files.isSymbolicLink(file)||Files.isSymbolicLink(file.getParent()))throw new java.io.IOException("Ambiguous ending archive");setDirty();server.overworld().getDataStorage().save();if(!com.coraline.rebirth.StoryRunStore.read(file).getCompound("data").equals(raw))throw new java.io.IOException("Ending archive checkpoint mismatch");return true;}catch(java.io.IOException|RuntimeException failed){problem="The ending archive could not be confirmed";return false;}}
    @Override public CompoundTag save(CompoundTag n){return n.merge(raw.copy());}
}
