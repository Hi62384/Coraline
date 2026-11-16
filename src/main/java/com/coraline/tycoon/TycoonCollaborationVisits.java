package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Visiting never replaces the guest's own household. The original return
 * receipt is saved before transport and retained across logout or revocation. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonCollaborationVisits {
    private static final String KEY="CoralineTycoonGuestReturn";
    public static boolean guest(ServerPlayer p){return p.getPersistentData().contains(KEY,Tag.TAG_COMPOUND);}
    private static boolean epoch(TycoonData.Home h,CompoundTag n){return h.state.hasUUID("CapabilityEpoch")?n.hasUUID("Epoch")&&h.state.getUUID("CapabilityEpoch").equals(n.getUUID("Epoch")):!n.contains("Epoch");}
    private static boolean saved(ServerPlayer p){
        try{var file=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return false;p.server.getPlayerList().saveAll();var data=com.coraline.rebirth.StoryRunStore.read(file).getCompound("ForgeData");return guest(p)?data.getCompound(KEY).equals(p.getPersistentData().getCompound(KEY)):!data.contains(KEY);}
        catch(java.io.IOException|RuntimeException failure){return false;}
    }
    public static boolean past(ServerPlayer p){return guest(p)&&p.getPersistentData().getCompound(KEY).contains("RetiredSlot",Tag.TAG_INT);}
    public static int visitPast(ServerPlayer p,int slot){
        if(!p.isAlive()||p.isPassenger()||p.getCamera()!=p||p.level()!=p.server.overworld()||guest(p)||!TycoonRetiredParcels.get(p.server).owns(p.getUUID(),slot)||com.coraline.postgame.TrueHeartBattle.recoveryPending(p))return 0;
        var level=p.server.getLevel(TycoonWorld.KEY);if(level==null)return 0;var at=TycoonWorld.landing(level,TycoonRetiredParcels.center(slot));if(at==null){TycoonWorld.message(p,"The preserved parcel has no clear arrival space.");return 0;}
        var n=new CompoundTag();n.putUUID("Owner",p.getUUID());n.putInt("RetiredSlot",slot);n.putString("Dimension","minecraft:overworld");n.putDouble("X",p.getX());n.putDouble("Y",p.getY());n.putDouble("Z",p.getZ());n.putFloat("Yaw",p.getYRot());n.putFloat("Pitch",p.getXRot());p.getPersistentData().put(KEY,n);if(!saved(p))return 0;
        p.closeContainer();p.stopRiding();p.fallDistance=0;p.teleportTo(level,at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),0);TycoonWorld.message(p,"Preserved builds visit. Use /coraline tycoon_access leave to return.");return 1;
    }
    public static boolean beforeEnter(ServerPlayer p,TycoonData.Home h){
        if(h.owner.equals(p.getUUID()))return !guest(p);
        if(!TycoonCollaborators.allowed(p,h.owner))return false;
        if(guest(p)){var n=p.getPersistentData().getCompound(KEY);return n.hasUUID("Owner")&&n.getUUID("Owner").equals(h.owner)&&epoch(h,n);}
        if(p.level()!=p.server.overworld())return false;
        var n=new CompoundTag();n.putUUID("Owner",h.owner);if(h.state.hasUUID("CapabilityEpoch"))n.putUUID("Epoch",h.state.getUUID("CapabilityEpoch"));n.putString("Dimension",p.level().dimension().location().toString());n.putDouble("X",p.getX());n.putDouble("Y",p.getY());n.putDouble("Z",p.getZ());n.putFloat("Yaw",p.getYRot());n.putFloat("Pitch",p.getXRot());p.getPersistentData().put(KEY,n);
        if(saved(p))return true;TycoonWorld.message(p,"Your return checkpoint could not be saved. Entry paused.");return false;
    }
    public static boolean leave(ServerPlayer p){
        if(!guest(p))return false;var n=p.getPersistentData().getCompound(KEY);
        if(!n.getString("Dimension").equals("minecraft:overworld")||!n.contains("X",Tag.TAG_DOUBLE)||!n.contains("Y",Tag.TAG_DOUBLE)||!n.contains("Z",Tag.TAG_DOUBLE)||!Double.isFinite(n.getDouble("X"))||!Double.isFinite(n.getDouble("Y"))||!Double.isFinite(n.getDouble("Z")))return false;
        var original=BlockPos.containing(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));var level=p.server.overworld();if(!level.getWorldBorder().isWithinBounds(original))return false;level.getChunkAt(original);
        var at=TycoonWorld.safeFeet(level,original)?original:TycoonWorld.landing(level,original);if(at==null)at=TycoonWorld.landing(level,level.getSharedSpawnPos());if(at==null)return false;
        p.closeContainer();p.stopRiding();p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.teleportTo(level,at.equals(original)?n.getDouble("X"):at.getX()+.5,at.equals(original)?n.getDouble("Y"):at.getY(),at.equals(original)?n.getDouble("Z"):at.getZ()+.5,n.getFloat("Yaw"),n.getFloat("Pitch"));
        p.getPersistentData().remove(KEY);TycoonNetwork.clear(p.getUUID());return saved(p);
    }
    public static Optional<java.util.UUID> visitingOwner(ServerPlayer p){
        if(!guest(p)||past(p)||p.serverLevel().dimension()!=TycoonWorld.KEY)return Optional.empty();var n=p.getPersistentData().getCompound(KEY);var h=n.hasUUID("Owner")?TycoonData.get(p.server).homes.get(n.getUUID("Owner")):null;return h!=null&&h.active()&&epoch(h,n)&&TycoonCollaborators.allowed(p,h.owner)?Optional.of(h.owner):Optional.empty();
    }
    public static Optional<TycoonData.Home> homeAt(ServerPlayer p,BlockPos at){
        if(p.serverLevel().dimension()!=TycoonWorld.KEY)return Optional.empty();
        return parcel(p.server,at).filter(h->TycoonCollaborators.allowed(p,h.owner));
    }
    public static Optional<TycoonData.Home> parcel(net.minecraft.server.MinecraftServer server,BlockPos at){
        return TycoonData.get(server).homes.values().stream().filter(h->h.active()&&Math.abs((long)at.getX()-h.center().getX())<=128&&Math.abs((long)at.getZ()-h.center().getZ())<=128&&TycoonLandscape.owned(h,at)).findFirst();
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||p.tickCount%20!=0||!guest(p)||com.coraline.postgame.TrueHeartBattle.recoveryPending(p))return;
        if(p.serverLevel().dimension()!=TycoonWorld.KEY){p.getPersistentData().remove(KEY);saved(p);return;}
        var n=p.getPersistentData().getCompound(KEY);if(n.contains("RetiredSlot",Tag.TAG_INT)){if(!TycoonRetiredParcels.get(p.server).owns(p.getUUID(),n.getInt("RetiredSlot"))||!TycoonRetiredParcels.get(p.server).contains(p.getUUID(),p.blockPosition()))leave(p);return;}var h=n.hasUUID("Owner")?TycoonData.get(p.server).homes.get(n.getUUID("Owner")):null;
        if(h==null||!h.active()||!epoch(h,n)||!TycoonCollaborators.allowed(p,h.owner)){leave(p);return;}
        if(Math.abs(p.getX()-h.center().getX())>128||Math.abs(p.getZ()-h.center().getZ())>128||p.getY()<h.center().getY()-12)TycoonScope.run(p,h.owner,()->TycoonWorld.enter(p));
    }
    private TycoonCollaborationVisits(){}
}
