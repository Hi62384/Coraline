package com.coraline.postgame;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Per-generation arrival construction and per-player return receipts. No
 * inventory is copied or restored by travel recovery. Unknown data is retained. */
final class InfiniteTransitData extends SavedData {
    private final UUID generation;
    private CompoundTag state;
    private String problem="";
    private InfiniteTransitData(UUID id){generation=id;state=new CompoundTag();state.putInt("Version",1);state.putUUID("Generation",id);state.put("Entries",new CompoundTag());state.putInt("Candidate",0);}
    private InfiniteTransitData(UUID id,CompoundTag n){
        generation=id;state=n.copy();
        try{
            if(!n.contains("Version",Tag.TAG_INT)||n.getInt("Version")!=1||!n.hasUUID("Generation")||!id.equals(n.getUUID("Generation"))||!n.contains("Entries",Tag.TAG_COMPOUND)||!n.contains("Candidate",Tag.TAG_INT)||n.getInt("Candidate")<0||n.getInt("Candidate")>128)throw new IllegalArgumentException("Invalid transit identity");
            if(n.contains("Anchor")&&!n.contains("Anchor",Tag.TAG_LONG)||n.contains("Built")&&(!n.contains("Built",Tag.TAG_BYTE)||n.getByte("Built")<0||n.getByte("Built")>1)||n.getBoolean("Built")&&!n.contains("Anchor",Tag.TAG_LONG))throw new IllegalArgumentException("Invalid arrival construction receipt");
            if(n.contains("Anchor")&&!bounded(BlockPos.of(n.getLong("Anchor"))))throw new IllegalArgumentException("Invalid arrival position");
            if(n.contains("PalaceDoor")&&!n.contains("PalaceDoor",Tag.TAG_LONG))throw new IllegalArgumentException("Invalid Palace portal receipt");
            var entries=n.getCompound("Entries");for(String key:entries.getAllKeys()){
                if(!UUID.fromString(key).toString().equals(key)||!entries.contains(key,Tag.TAG_COMPOUND))throw new IllegalArgumentException("Invalid travel owner");
                var row=entries.getCompound(key);if(!row.contains("Return",Tag.TAG_LONG)||!bounded(BlockPos.of(row.getLong("Return")))||!row.contains("Inside",Tag.TAG_BYTE)||row.getByte("Inside")<0||row.getByte("Inside")>1)throw new IllegalArgumentException("Invalid return receipt");
            }
        }catch(IllegalArgumentException invalid){problem=invalid.getMessage();}
    }
    private static boolean bounded(BlockPos at){return Math.abs((long)at.getX())<29999980&&Math.abs((long)at.getZ())<29999980&&at.getY()>=-64&&at.getY()<320;}
    private static String name(UUID id){return "coraline_infinite_transit_"+id;}
    static InfiniteTransitData get(MinecraftServer server,UUID id){var file=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name(id)+".dat");return server.overworld().getDataStorage().computeIfAbsent(n->new InfiniteTransitData(id,n),()->Files.notExists(file,LinkOption.NOFOLLOW_LINKS)?new InfiniteTransitData(id):new InfiniteTransitData(id,new CompoundTag()),name(id));}
    String problem(){return problem;}
    int candidate(){return state.getInt("Candidate");}
    void rejectCandidate(){if(problem.isEmpty()&&!state.contains("Anchor")&&candidate()<128){state.putInt("Candidate",candidate()+1);setDirty();}}
    Optional<BlockPos> anchor(){return problem.isEmpty()&&state.contains("Anchor",Tag.TAG_LONG)?Optional.of(BlockPos.of(state.getLong("Anchor"))):Optional.empty();}
    boolean built(){return problem.isEmpty()&&state.getBoolean("Built");}
    boolean plan(MinecraftServer server,BlockPos at){if(!problem.isEmpty()||!bounded(at))return false;if(anchor().isPresent())return anchor().orElseThrow().equals(at);state.putLong("Anchor",at.asLong());state.putBoolean("Built",false);return checkpoint(server);}
    boolean built(MinecraftServer server){if(anchor().isEmpty())return false;state.putBoolean("Built",true);return checkpoint(server);}
    boolean palaceDoor(MinecraftServer server,BlockPos at){if(!problem.isEmpty()||!bounded(at))return false;if(state.contains("PalaceDoor"))return state.getLong("PalaceDoor")==at.asLong();state.putLong("PalaceDoor",at.asLong());return checkpoint(server);}
    boolean ownsPalaceDoor(BlockPos at){return problem.isEmpty()&&state.contains("PalaceDoor",Tag.TAG_LONG)&&state.getLong("PalaceDoor")==at.asLong();}
    boolean enter(MinecraftServer server,UUID player,BlockPos back){if(!problem.isEmpty()||!bounded(back))return false;var row=new CompoundTag();row.putLong("Return",back.asLong());row.putBoolean("Inside",true);state.getCompound("Entries").put(player.toString(),row);return checkpoint(server);}
    boolean inside(UUID player){return problem.isEmpty()&&state.getCompound("Entries").getCompound(player.toString()).getBoolean("Inside");}
    Optional<BlockPos> back(UUID player){var row=state.getCompound("Entries").getCompound(player.toString());return problem.isEmpty()&&row.contains("Return",Tag.TAG_LONG)?Optional.of(BlockPos.of(row.getLong("Return"))):Optional.empty();}
    boolean leave(MinecraftServer server,UUID player){if(!problem.isEmpty())return false;var row=state.getCompound("Entries").getCompound(player.toString());if(!row.contains("Return",Tag.TAG_LONG))return false;row.putBoolean("Inside",false);state.getCompound("Entries").put(player.toString(),row);return checkpoint(server);}
    private boolean checkpoint(MinecraftServer server){
        if(!problem.isEmpty())return false;try{
            var file=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name(generation)+".dat");if(Files.isSymbolicLink(file)||Files.isSymbolicLink(file.getParent()))return false;
            setDirty();server.overworld().getDataStorage().save();var disk=com.coraline.rebirth.StoryRunStore.read(file);
            if(!disk.contains("data",Tag.TAG_COMPOUND)||!disk.getCompound("data").equals(state))throw new java.io.IOException("Transit save did not match");return true;
        }catch(java.io.IOException|RuntimeException bad){problem="A travel checkpoint could not be saved safely";return false;}
    }
    @Override public CompoundTag save(CompoundTag n){return n.merge(state.copy());}
}
