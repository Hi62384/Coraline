package com.coraline.multiplayer;

import com.coraline.feast.*;
import com.coraline.rebirth.StoryRunStore;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Fatal Hand-combat damage holds the actual participant without dropping or
 * copying inventory. The original mode is checkpointed before spectator mode. */
@Mod.EventBusSubscriber(modid="coraline")
public final class StoryCombatSafety {
    public static final String KEY="CoralineStoryCombatMode";
    private static java.util.UUID token(VisitData.Scene scene){
        var hand=PalaceWellEncounter.combatSession(scene);if(hand!=null)return hand;
        if(scene!=null&&RescueChapter.started(scene)&&RescueBosses.fighting(RescueChapter.phase(scene))&&RescueChapter.data(scene).hasUUID("Input"))return RescueChapter.data(scene).getUUID("Input");return null;
    }
    public static boolean active(VisitData.Scene scene){return PalaceWellEncounter.cooperativeCombat(scene)||scene!=null&&RescueChapter.started(scene)&&RescueBosses.fighting(RescueChapter.phase(scene));}
    public static boolean held(ServerPlayer p,VisitData.Scene scene){
        var token=token(scene);var n=p.getPersistentData().getCompound(KEY);
        return token!=null&&n.getBoolean("Ready")&&n.hasUUID("Player")&&p.getUUID().equals(n.getUUID("Player"))
            &&n.hasUUID("Owner")&&scene.owner.equals(n.getUUID("Owner"))&&n.hasUUID("Door")&&scene.door.equals(n.getUUID("Door"))
            &&n.hasUUID("Combat")&&token.equals(n.getUUID("Combat"));
    }
    public static boolean eliminate(ServerPlayer p){return hold(p,true);}
    private static boolean hold(ServerPlayer p){return hold(p,false);}
    private static boolean hold(ServerPlayer p,boolean scriptedBoundary){
        var scene=OtherMotherLoop.scene(p);var data=StorySessionData.get(p.server);
        if(scene==null||!data.hasGuests(scene.owner)||!StoryCoop.member(p,scene)||!active(scene)||p.isSpectator())return false;
        var owner=p.server.getPlayerList().getPlayer(scene.owner);if(owner==null||p.serverLevel()!=owner.serverLevel())return false;
        if(!scriptedBoundary){boolean nearby=PalaceWellEncounter.cooperativeCombat(scene)?com.coraline.palace.PalaceSiteData.get(p.server).position(com.coraline.palace.PalaceSitePlan.Kind.WELL).filter(at->p.distanceToSqr(at.getCenter())<48*48).isPresent():RescueBosses.nearArena(p,scene);if(!nearby)return false;}
        var token=token(scene);if(token==null)return false;
        var n=p.getPersistentData().getCompound(KEY);
        if(!p.getPersistentData().contains(KEY)){
            n.putUUID("Player",p.getUUID());n.putUUID("Owner",scene.owner);n.putUUID("Door",scene.door);n.putUUID("Combat",token);
            n.putInt("Mode",p.gameMode.getGameModeForPlayer().getId());p.getPersistentData().put(KEY,n);
        }
        if(!n.hasUUID("Player")||!p.getUUID().equals(n.getUUID("Player"))||!n.hasUUID("Combat")||!token.equals(n.getUUID("Combat")))return false;
        try{
            if(p.getHealth()<=0)p.setHealth(1);p.server.getPlayerList().saveAll();
            var file=StoryRunStore.read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));
            if(!n.equals(file.getCompound("ForgeData").getCompound(KEY)))return false;
            n.putBoolean("Ready",true);p.stopRiding();p.setForcedPose(null);p.setGameMode(GameType.SPECTATOR);
            var target=data.participants(p.server,scene.owner,true).stream().filter(other->other!=p&&other.isAlive()&&!other.isSpectator()&&other.serverLevel()==p.serverLevel()).findFirst().orElse(null);
            if(target!=null)p.setCamera(target);return true;
        }catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    public static void restore(ServerPlayer p){
        var n=p.getPersistentData().getCompound(KEY);if(!n.hasUUID("Player")||!p.getUUID().equals(n.getUUID("Player"))||!n.contains("Mode",net.minecraft.nbt.Tag.TAG_INT))return;
        int mode=n.getInt("Mode");if(mode<0||mode>3)return;
        var at=com.coraline.tycoon.TycoonWorld.landing(p.serverLevel(),p.blockPosition());if(at==null)return;
        p.setCamera(p);p.stopRiding();p.setForcedPose(null);p.teleportTo(p.serverLevel(),at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());p.fallDistance=0;p.setGameMode(GameType.byId(mode));
        try{p.server.getPlayerList().saveAll();var nativePlayer=StoryRunStore.read(p.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat"));if(nativePlayer.getInt("playerGameType")==mode)p.getPersistentData().remove(KEY);}catch(java.io.IOException|RuntimeException unavailable){/* Retain the recovery receipt. */}
    }
    public static void restoreGroup(ServerPlayer owner,VisitData.Scene scene){for(var p:StorySessionData.get(owner.server).participants(owner.server,scene.owner,true))restore(p);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void fatal(LivingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p&&e.getAmount()>=p.getHealth()&&hold(p))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p&&hold(p))e.setCanceled(true);}
    @SubscribeEvent public static void recover(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0||!p.getPersistentData().contains(KEY))return;
        var scene=OtherMotherLoop.scene(p);if(scene==null||!held(p,scene)||!StoryCoop.member(p,scene)||!active(scene))restore(p);
    }
    private StoryCombatSafety(){}
}
