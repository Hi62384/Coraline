package com.coraline.multiplayer;

import com.coraline.feast.*;
import com.coraline.rebirth.StoryRunStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

/** Shared ghost state, with a separately checkpointed actual body mode for
 * every guest. The owner remains the sole world/NPC director. */
public final class GhostCooperation {
    private static final String KEY="CoralineCoopGhostBody";
    private static boolean bound(ServerPlayer p,VisitData.Scene scene,CompoundTag ghost,CompoundTag backup){
        return backup.hasUUID("Player")&&p.getUUID().equals(backup.getUUID("Player"))&&backup.hasUUID("Owner")&&scene.owner.equals(backup.getUUID("Owner"))
            &&backup.hasUUID("Door")&&scene.door.equals(backup.getUUID("Door"))&&ghost.hasUUID("Session")&&backup.hasUUID("Ghost")&&ghost.getUUID("Session").equals(backup.getUUID("Ghost"));
    }
    public static boolean member(ServerPlayer p,VisitData.Scene scene,CompoundTag ghost){
        if(scene==null||!StoryCoop.member(p,scene)||!p.isAlive())return false;
        if(p.getUUID().equals(scene.owner))return GhostVictimMirrors.owned(p,scene,ghost);
        var frame=StorySessionData.get(p.server).frame(p.getUUID());var backup=p.getPersistentData().getCompound(KEY);
        return frame!=null&&!frame.paused()&&!frame.archived()&&frame.state().equals("ACTIVE")&&p.isSpectator()&&backup.getBoolean("Ready")&&bound(p,scene,ghost,backup);
    }
    private static boolean prepare(ServerPlayer p,VisitData.Scene scene,CompoundTag ghost){
        var inventory=p.getPersistentData().getCompound("CoralineAcceptedInventoryEscrow");
        if(!GroupInventoryEscrow.held(p)||!inventory.getBoolean("Stored")||!inventory.getString("CaptureState").equals("CLEARED")
            ||!AcceptedButtonEyesInventory.validSnapshot(inventory.getCompound("Snapshot"),p,scene.door))return false;
        var backup=p.getPersistentData().getCompound(KEY);
        if(!p.getPersistentData().contains(KEY)){
            backup.putUUID("Player",p.getUUID());backup.putUUID("Owner",scene.owner);backup.putUUID("Door",scene.door);backup.putUUID("Ghost",ghost.getUUID("Session"));
            backup.putInt("Mode",p.gameMode.getGameModeForPlayer().getId());p.getPersistentData().put(KEY,backup);
        }
        if(!bound(p,scene,ghost,backup))return false;if(backup.getBoolean("Ready"))return true;
        try{
            p.server.getPlayerList().saveAll();var nativePlayer=StoryRunStore.read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));
            if(!backup.equals(nativePlayer.getCompound("ForgeData").getCompound(KEY)))return false;
            backup.putBoolean("Ready",true);return true;
        }catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    public static void restoreMode(ServerPlayer p){
        var backup=p.getPersistentData().getCompound(KEY);if(!backup.hasUUID("Player")||!p.getUUID().equals(backup.getUUID("Player"))||!backup.contains("Mode",net.minecraft.nbt.Tag.TAG_INT))return;
        int mode=backup.getInt("Mode");if(mode<0||mode>3)return;
        p.setCamera(p);p.stopRiding();p.setForcedPose(null);p.setGameMode(GameType.byId(mode));p.getPersistentData().remove(KEY);
    }
    public static void tick(ServerPlayer guest,ServerPlayer owner,VisitData.Scene scene){
        if(!StoryCoop.member(guest,scene)||guest.getUUID().equals(scene.owner))return;
        var ghost=GhostVictimEntry.data(scene);
        if(!GhostVictimDirector.active(scene)||!ghost.hasUUID("Session"))return;
        if(GhostEscapeProgress.bodyRestored(ghost)){
            if(!GroupInventoryEscrow.held(guest)||GroupInventoryEscrow.restore(guest))restoreMode(guest);return;
        }
        if(GhostVictimProgress.phase(ghost)==GhostVictimProgress.Phase.ENTER_MIRROR||!owner.isSpectator())return;
        if(!prepare(guest,scene,ghost))return;
        if(!guest.isSpectator())guest.setGameMode(GameType.SPECTATOR);
        if(!GhostVictimProgress.imprisoned(ghost))GhostVictimEntry.ghostControls(guest,scene,ghost);
        if(GhostVictimProgress.imprisoned(ghost)&&guest.distanceToSqr(FirstEscapeScene.prison(scene).getCenter())>9){
            var level=owner.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);if(level!=null){var at=FirstEscapeScene.prison(scene);guest.teleportTo(level,at.getX()+.5,at.getY(),at.getZ()+.5,guest.getYRot(),guest.getXRot());}
        }
        if(guest.tickCount%10==0)GhostVictimSnapshot.send(guest,scene);
    }
    public static ServerPlayer speaker(ServerPlayer owner,VisitData.Scene scene,CompoundTag ghost){
        if(!ghost.hasUUID("ConversationPlayer"))return owner;
        var player=owner.server.getPlayerList().getPlayer(ghost.getUUID("ConversationPlayer"));
        return player!=null&&member(player,scene,ghost)?player:null;
    }
    private GhostCooperation(){}
}
