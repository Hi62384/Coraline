package com.coraline.postgame;

import java.security.SecureRandom;
import java.nio.file.*;
import java.util.OptionalLong;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Stored with the world, outside per-owner VisitData and replay journals. */
public final class InfiniteWorldData extends SavedData {
    private static final SecureRandom ENTROPY=new SecureRandom();
    private static final String NAME="coraline_infinite_other_world";
    private final InfiniteWorldState state;
    private MinecraftServer server;
    private InfiniteWorldData(){state=InfiniteWorldState.fresh();}
    private InfiniteWorldData(CompoundTag saved){state=InfiniteWorldState.load(saved);}
    private static Path file(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");}
    public static InfiniteWorldData get(MinecraftServer server){
        var data=server.overworld().getDataStorage().computeIfAbsent(InfiniteWorldData::new,()->{
            // SavedData can fall back to its factory when disk decoding fails.
            // An existing/ambiguous file is never permission to choose a new seed.
            return Files.notExists(file(server),LinkOption.NOFOLLOW_LINKS)?new InfiniteWorldData():new InfiniteWorldData(new CompoundTag());
        },NAME);data.server=server;return data;
    }
    public InfiniteWorldState state(){return InfiniteWorldState.load(state.save());}
    public boolean active(){return state.active();}
    /** Read-only identity lookup for frequent native AI checks; no journal copy. */
    java.util.Optional<java.util.UUID> generation(){return state.generation();}
    public boolean completedBy(java.util.UUID owner){return state.completedBy(owner);}
    /** World access does not grant personal story completion or its rewards. */
    public boolean accessible(){return server!=null&&state.active()&&!TrueHeartBattle.resetting(server);}
    public CompoundTag canonicalCompletion(){return state.canonicalCompletion();}
    public CompoundTag completionWitness(java.util.UUID owner){return state.completionWitness(owner);}
    public java.util.Set<java.util.UUID> completionOwners(){return state.completionOwners();}
    public boolean matchesGeneration(java.util.UUID generation){return generation!=null&&state.active()&&state.generation().filter(generation::equals).isPresent();}
    InfiniteWorldState.Activation record(MinecraftServer server,InfiniteWorldState.Witness witness){
        if(TrueHeartBattle.resetting(server))return InfiniteWorldState.Activation.REFUSED;
        var outcome=state.activate(witness,server.overworld().getGameTime(),server.overworld().getSeed(),ENTROPY::nextLong);
        if(outcome==InfiniteWorldState.Activation.CREATED||outcome==InfiniteWorldState.Activation.OWNER_RECORDED)setDirty();if(outcome!=InfiniteWorldState.Activation.REFUSED)WybieRescue.finishStoryLoan(server,witness);return outcome;
    }
    boolean retireFromEnding(MinecraftServer server,java.util.UUID generation,java.util.UUID reset){
        if(!TrueHeartBattle.authorizes(server,generation,reset)||!GlobalHandEncounter.retire(server,generation,reset)||!state.retire(generation,reset,server.overworld().getGameTime()))return false;
        try{if(Files.isSymbolicLink(file(server))||Files.isSymbolicLink(file(server).getParent()))return false;setDirty();server.overworld().getDataStorage().save();var disk=com.coraline.rebirth.StoryRunStore.read(file(server)).getCompound("data");return disk.equals(state.save())&&InfiniteWorldState.load(disk).blocker().isEmpty();}catch(java.io.IOException|RuntimeException failed){return false;}
    }
    /** Bind a generator once through this checkpoint, never perform disk IO
     * per chunk. A logged SavedData write failure cannot expose a volatile seed. */
    public OptionalLong checkpointSeed(MinecraftServer server){
        if(!state.active())return OptionalLong.empty();
        try{
            if(Files.isSymbolicLink(file(server))||Files.isSymbolicLink(file(server).getParent()))return OptionalLong.empty();
            setDirty();server.overworld().getDataStorage().save();
            var saved=com.coraline.rebirth.StoryRunStore.read(file(server));
            return InfiniteWorldState.checkpointMatches(state.save(),saved)?state.seed():OptionalLong.empty();
        }catch(java.io.IOException|RuntimeException failure){return OptionalLong.empty();}
    }
    @Override public CompoundTag save(CompoundTag target){target.merge(state.save());return target;}
}
