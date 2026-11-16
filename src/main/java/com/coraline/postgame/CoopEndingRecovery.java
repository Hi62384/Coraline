package com.coraline.postgame;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import com.coraline.multiplayer.*;
import com.coraline.feast.AcceptedButtonEyesInventory;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Restores real player files before a committed reset retires scene metadata.
 * Offline files are merged atomically; no simulated players or NPC snapshots. */
final class CoopEndingRecovery {
    private static final String BACKUP="CoralineAcceptedInventoryEscrow",MARKER="CoralineCoopCompleteEnding";
    static boolean advance(MinecraftServer server,CompoundTag state,java.util.function.BooleanSupplier checkpoint)throws IOException{
        UUID generation=state.getUUID("Generation"),reset=state.getUUID("Reset");if(!TrueHeartBattle.authorizes(server,generation,reset))return false;
        if(!QuestItemRecovery.drainForEnding(server))return false;
        var sessions=StorySessionData.get(server);if(!sessions.prepareCompleteEnding(server,generation,reset))return false;
        var scope=state.getList("Scope",Tag.TAG_INT_ARRAY);int cursor=state.getInt("CoopReturnCursor");if(cursor>=scope.size())return true;
        UUID player=NbtUtils.loadUUID(scope.get(cursor));var file=server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player+".dat");var online=server.getPlayerList().getPlayer(player);
        if(Files.isSymbolicLink(file)||Files.isSymbolicLink(file.getParent()))throw new IOException("Symlinked native player checkpoint");
        if(online!=null){online.closeContainer();com.coraline.multiplayer.StoryCoop.clear(online);((com.coraline.mixin.MirrorPlayerSaveAccess)server.getPlayerList()).coraline$saveMirrorPlayer(online);}
        if(Files.notExists(file,LinkOption.NOFOLLOW_LINKS)){
            // Story owners without a native player file cannot have a player inventory escrow.
            if(!sessions.endingReturn(player).isEmpty()||!sessions.endingEscrow(player).isEmpty())throw new IOException("Missing native participant file");
            state.putInt("CoopReturnCursor",cursor+1);checkpoint.getAsBoolean();return false;
        }
        var before=com.coraline.rebirth.StoryRunStore.read(file);if(!before.hasUUID("UUID")||!before.getUUID("UUID").equals(player))throw new IOException("Native player UUID mismatch");
        CompoundTag target=before.getCompound("ForgeData").hasUUID(MARKER)&&before.getCompound("ForgeData").getUUID(MARKER).equals(reset)?before:plan(server,player,before,sessions,reset);
        if(target!=before){
            if(online!=null)applyOnline(online,target,reset);else atomic(file,before,target);
        }
        var saved=com.coraline.rebirth.StoryRunStore.read(file);if(!sessions.finishEndingPlayer(server,player,generation,reset,saved))return false;
        state.putInt("CoopReturnCursor",cursor+1);checkpoint.getAsBoolean();return false;
    }
    private static CompoundTag plan(MinecraftServer server,UUID player,CompoundTag before,StorySessionData sessions,UUID reset)throws IOException{
        var target=before.copy();var forge=target.getCompound("ForgeData");target.put("ForgeData",forge);var backup=forge.getCompound(BACKUP);
        if(backup.getBoolean("Stored")){
            var snapshot=backup.getCompound("Snapshot");if(!snapshot.hasUUID("Door")||!AcceptedButtonEyesInventory.validSnapshot(snapshot,player,snapshot.getUUID("Door")))throw new IOException("Invalid native story inventory escrow");
            var lane=sessions.endingEscrow(player);if(lane.isEmpty()){var scene=com.coraline.feast.VisitData.get(server).scenes.get(player);if(scene!=null&&com.coraline.feast.AcceptedButtonEyesProgress.active(scene))lane=com.coraline.feast.AcceptedButtonEyesProgress.data(scene).getCompound("InventoryEscrow");}if(!lane.isEmpty()&&!lane.equals(snapshot))throw new IOException("Native and session escrows differ; both retained");
            var originals=snapshot;
            if(!backup.getString("CaptureState").equals("CLEARED")){
                if(!backup.getString("CaptureState").equals("PREPARED")||!com.coraline.palace.PalaceKeyCheckpoint.inventory(target,snapshot.getList("Inventory",Tag.TAG_COMPOUND)))throw new IOException("Incomplete inventory capture; visible items retained");
                // The native file proves the original inventory never left. Only
                // the separately held menu overflow needs to be merged back.
                originals=snapshot.copy();originals.put("Inventory",new ListTag());
            }
            var merged=GroupInventoryEscrow.planNative(target.getList("Inventory",Tag.TAG_COMPOUND),originals);target.put("Inventory",merged.getList("Inventory",Tag.TAG_COMPOUND));backup.put("ReturnOverflow",merged.getList("Overflow",Tag.TAG_COMPOUND));backup.putBoolean("Stored",false);backup.putBoolean("Restored",true);backup.putString("CaptureState","RESTORED");backup.putUUID("RestoredTransaction",snapshot.getUUID("Transaction"));backup.putUUID("ReturnProof",snapshot.getUUID("Transaction"));forge.put(BACKUP,backup);
        }
        var expected=sessions.endingEscrow(player);
        if(!expected.isEmpty()&&(!backup.getBoolean("Restored")||!backup.hasUUID("RestoredTransaction")||!backup.getUUID("RestoredTransaction").equals(expected.getUUID("Transaction"))||!backup.getCompound("Snapshot").equals(expected)))throw new IOException("Participant inventory return is not proven");
        var back=sessions.endingReturn(player);int mode=back.contains("Mode",Tag.TAG_INT)?back.getInt("Mode"):target.getInt("playerGameType");
        for(String key:List.of("CoralineStoryCombatMode","CoralineCoopGhostBody")){
            var held=forge.getCompound(key);if(held.isEmpty())continue;
            if(!held.hasUUID("Player")||!player.equals(held.getUUID("Player"))||!held.contains("Mode",Tag.TAG_INT)||held.getInt("Mode")<0||held.getInt("Mode")>3)throw new IOException("Invalid actual participant mode recovery receipt");
            if(back.isEmpty()&&mode==net.minecraft.world.level.GameType.SPECTATOR.getId())mode=held.getInt("Mode");
        }
        var personalScene=com.coraline.feast.VisitData.get(server).scenes.get(player);if(back.isEmpty()&&mode==net.minecraft.world.level.GameType.SPECTATOR.getId()&&personalScene!=null){var ghost=com.coraline.feast.GhostVictimEntry.data(personalScene);if(ghost.getBoolean("Started")){String name=ghost.getString("BodyGameMode");boolean found=false;for(var type:net.minecraft.world.level.GameType.values())if(type.getName().equals(name)){mode=type.getId();found=true;break;}if(!found)throw new IOException("The original ghost body game mode is not recorded");}}if(mode<0||mode>3)throw new IOException("Invalid original participant mode");target.putInt("playerGameType",mode);
        var abilities=target.getCompound("abilities");abilities.putBoolean("mayfly",mode==1||mode==3);abilities.putBoolean("flying",mode==3);abilities.putBoolean("invulnerable",mode==1||mode==3);abilities.putBoolean("instabuild",mode==1);abilities.putBoolean("mayBuild",mode==0||mode==1);target.put("abilities",abilities);
        // Existing public-world positions remain intact. Private dimensions are about to retire.
        boolean privateWorld=target.getString("Dimension").startsWith("coraline:");boolean landingNeeded=before.getInt("playerGameType")==net.minecraft.world.level.GameType.SPECTATOR.getId()&&mode!=net.minecraft.world.level.GameType.SPECTATOR.getId();
        if(!back.isEmpty()&&back.contains("Dimension")||privateWorld||landingNeeded){
            var level=back.contains("Dimension")?com.coraline.parents.FamilyData.level(server,back.getString("Dimension")):null;net.minecraft.core.BlockPos at=null;if(level==null&&landingNeeded&&!privateWorld){level=com.coraline.parents.FamilyData.level(server,target.getString("Dimension"));var savedPos=target.getList("Pos",Tag.TAG_DOUBLE);var feet=net.minecraft.core.BlockPos.containing(savedPos.getDouble(0),savedPos.getDouble(1),savedPos.getDouble(2));if(level!=null){level.getChunkAt(feet);if(com.coraline.tycoon.TycoonWorld.safeFeet(level,feet))at=feet;}}
            if(back.contains("Dimension")&&level!=null&&!level.dimension().location().getNamespace().equals("coraline")){var requested=net.minecraft.core.BlockPos.containing(back.getDouble("X"),back.getDouble("Y"),back.getDouble("Z"));level.getChunkAt(requested);if(com.coraline.tycoon.TycoonWorld.safeFeet(level,requested))at=requested;}
            if(at==null){level=server.overworld();at=com.coraline.tycoon.TycoonWorld.landing(level,level.getSharedSpawnPos());}if(at==null)throw new IOException("No safe native ending return position");
            target.putString("Dimension",level.dimension().location().toString());var pos=new ListTag();pos.add(DoubleTag.valueOf(at.getX()+.5));pos.add(DoubleTag.valueOf(at.getY()));pos.add(DoubleTag.valueOf(at.getZ()+.5));target.put("Pos",pos);target.remove("RootVehicle");target.putFloat("FallDistance",0);
        }
        for(String key:List.of("CoralineInTunnel","CoralineReturning","CoopCinematic","CoopInternalMove","CoralineCoopSpectatorMode","CoralineCoopSpectatorSession","CoralineStoryCombatMode","CoralineCoopGhostBody"))forge.remove(key);
        forge.putUUID(MARKER,reset);return target;
    }
    private static void applyOnline(ServerPlayer p,CompoundTag target,UUID reset)throws IOException{
        p.getPersistentData().putBoolean("CoopInternalMove",true);try{
            p.stopRiding();p.setCamera(p);p.setForcedPose(null);p.getInventory().load(target.getList("Inventory",Tag.TAG_COMPOUND));p.getPersistentData().put(BACKUP,target.getCompound("ForgeData").getCompound(BACKUP).copy());
            for(String key:List.of("CoralineInTunnel","CoralineReturning","CoopCinematic","CoralineCoopSpectatorMode","CoralineCoopSpectatorSession","CoralineStoryCombatMode","CoralineCoopGhostBody"))p.getPersistentData().remove(key);
            p.setGameMode(net.minecraft.world.level.GameType.byId(target.getInt("playerGameType")));var level=com.coraline.parents.FamilyData.level(p.server,target.getString("Dimension"));if(level==null)throw new IOException("Missing native return dimension");var pos=target.getList("Pos",Tag.TAG_DOUBLE);p.teleportTo(level,pos.getDouble(0),pos.getDouble(1),pos.getDouble(2),p.getYRot(),p.getXRot());p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.getPersistentData().putUUID(MARKER,reset);p.getInventory().setChanged();p.inventoryMenu.broadcastFullState();
        }finally{p.getPersistentData().remove("CoopInternalMove");}
        ((com.coraline.mixin.MirrorPlayerSaveAccess)p.server.getPlayerList()).coraline$saveMirrorPlayer(p);
    }
    private static void atomic(Path file,CompoundTag expected,CompoundTag target)throws IOException{
        if(!com.coraline.rebirth.StoryRunStore.read(file).equals(expected))throw new IOException("Native player file changed during ending recovery");
        var temp=Files.createTempFile(file.getParent(),file.getFileName()+"-ending-",".pending");try{
            NbtIo.writeCompressed(target,temp.toFile());try(var channel=FileChannel.open(temp,StandardOpenOption.WRITE)){channel.force(true);}if(!com.coraline.rebirth.StoryRunStore.read(temp).equals(target))throw new IOException("Native player ending read-back mismatch");
            Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);if(!com.coraline.rebirth.StoryRunStore.read(file).equals(target))throw new IOException("Native player ending commit mismatch");
        }finally{Files.deleteIfExists(temp);}
    }
    private CoopEndingRecovery(){}
}
