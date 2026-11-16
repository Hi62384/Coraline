package com.coraline.postgame;

import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

/** Roll forward only the native dead-body/broken-core receipt. Each boundary
 * confirms the real stores before advancing; ordinary replay never enters it.
 * Multiplayer escrows merge only into each actual native player file. */
final class CompleteEndingTransaction {
    static void advance(MinecraftServer server,CompoundTag state,BooleanSupplier checkpoint){
        var generation=state.getUUID("Generation");var reset=state.getUUID("Reset");if(!TrueHeartBattle.authorizes(server,generation,reset))return;
        try{switch(state.getString("Step")){
            case "RETURN"->{if(!CoopEndingRecovery.advance(server,state,checkpoint))return;for(var p:server.getPlayerList().getPlayers())if(p.level().dimension().location().getNamespace().equals("coraline")&&!TrueHeartRealm.leave(p))return;next(state,"INFECTION",checkpoint);}
            case "INFECTION"->{if(InfectionColonies.retire(server,generation))next(state,"OWNERS",checkpoint);}
            case "OWNERS"->{var scope=state.getList("Scope",Tag.TAG_INT_ARRAY);int cursor=state.getInt("Cursor");if(cursor>=scope.size()){next(state,"TYCOON",checkpoint);return;}var owner=NbtUtils.loadUUID(scope.get(cursor));if(!com.coraline.feast.MainStoryRebirth.completeEndingOwner(server,owner,reset))return;if(!ownerStores(server))return;state.putInt("Cursor",cursor+1);checkpoint.getAsBoolean();}
            case "TYCOON"->{var owners=new HashSet<UUID>();for(var tag:state.getList("Scope",Tag.TAG_INT_ARRAY))owners.add(NbtUtils.loadUUID(tag));if(com.coraline.tycoon.TycoonData.get(server).retireHomes(server,reset,owners))next(state,"BREACH",checkpoint);}
            case "BREACH"->{if(BreachWorldData.get(server).retireFromEnding(server,generation,reset))next(state,"EPOCH",checkpoint);}
            case "EPOCH"->{for(var p:server.getPlayerList().getPlayers())if(!player(p,state,checkpoint))return;if(!InfiniteWorldData.get(server).retireFromEnding(server,generation,reset))return;WybieRescue.retireGuides(server,generation);InfinitePalacePortal.extinguish(server,generation);state.putString("Stage","DONE");if(checkpoint.getAsBoolean())server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.translatable("message.coraline.heart_ending_complete"),false);}
            default->throw new IllegalStateException("Unknown complete-ending step");
        }}catch(java.io.IOException|RuntimeException failed){var reason=failed.getMessage()==null?failed.getClass().getSimpleName():failed.getMessage();if(!reason.equals(state.getString("PauseReason"))){state.putString("PauseReason",reason);com.mojang.logging.LogUtils.getLogger().error("The committed Coraline ending is paused without discarding its recovery records",failed);checkpoint.getAsBoolean();}}
    }
    private static void next(CompoundTag state,String step,BooleanSupplier checkpoint){state.putString("Step",step);state.remove("PauseReason");checkpoint.getAsBoolean();}
    private static boolean store(MinecraftServer server,String name,SavedData data)throws java.io.IOException{var file=server.getWorldPath(LevelResource.ROOT).resolve("data/"+name+".dat");return com.coraline.rebirth.StoryRunStore.read(file).getCompound("data").equals(data.save(new CompoundTag()));}
    private static boolean ownerStores(MinecraftServer server)throws java.io.IOException{server.overworld().getDataStorage().save();return store(server,"coraline_first_visits",com.coraline.feast.VisitData.get(server))&&store(server,"coraline_doors",com.coraline.otherworld.DoorData.get(server))&&store(server,"coraline_doll_bonds",com.coraline.doll.DollBondData.get(server))&&store(server,"coraline_other_realms",com.coraline.otherworld.RealmData.get(server))&&store(server,"coraline_families",com.coraline.parents.FamilyData.get(server))&&store(server,"coraline_bobinsky_visits",com.coraline.bobinsky.BobinskyVisitData.get(server));}
    /** A player applies the same cleanup on their next real login. Native save
     * confirms it before the pending receipt is removed; no item lists are read
     * back into a live inventory and unrelated persistence remains untouched. */
    static boolean player(ServerPlayer p,CompoundTag state,BooleanSupplier checkpoint){
        if(state.getString("Stage").equals("RESETTING")&&!state.getString("Step").equals("EPOCH"))return !p.level().dimension().location().getNamespace().equals("coraline")||TrueHeartRealm.leave(p);
        var pending=state.getCompound("PendingPlayers");if(!pending.hasUUID(p.getUUID().toString())){if(TrueHeartRealm.isHeart(p.level())&&!state.getString("Stage").equals("BODY")&&!state.getString("Stage").equals("ANCHORS")&&!state.getString("Stage").equals("CORE"))return TrueHeartRealm.leave(p);return true;}var reset=pending.getUUID(p.getUUID().toString());try{
        if(p.level().dimension().location().getNamespace().equals("coraline")&&!TrueHeartRealm.leave(p))return false;
        var path=p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");boolean saved=false;if(java.nio.file.Files.isRegularFile(path,java.nio.file.LinkOption.NOFOLLOW_LINKS)){var disk=com.coraline.rebirth.StoryRunStore.read(path).getCompound("ForgeData");saved=disk.hasUUID("CoralineFullResetApplied")&&reset.equals(disk.getUUID("CoralineFullResetApplied"));}
        if(!saved){if(!com.coraline.feast.MainStoryRebirth.prepareStartingParents(p,reset))return false;com.coraline.feast.MainStoryRebirth.completeEndingPlayer(p);p.getPersistentData().putUUID("CoralineFullResetApplied",reset);((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);var disk=com.coraline.rebirth.StoryRunStore.read(path).getCompound("ForgeData");if(!disk.hasUUID("CoralineFullResetApplied")||!reset.equals(disk.getUUID("CoralineFullResetApplied")))return false;}
        pending.remove(p.getUUID().toString());return checkpoint.getAsBoolean();
    }catch(java.io.IOException|RuntimeException failed){return false;}}
    private CompleteEndingTransaction(){}
}
