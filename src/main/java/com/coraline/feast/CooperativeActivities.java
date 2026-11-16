package com.coraline.feast;

import java.util.*;
import com.coraline.multiplayer.*;
import net.minecraft.server.level.ServerPlayer;

/** Physical participants only; callers continue ticking one native director. */
public final class CooperativeActivities {
    public static List<ServerPlayer> playing(ServerPlayer director,VisitData.Scene scene){
        var data=StorySessionData.get(director.server);
        if(!data.groupActivity(scene))return director.isAlive()&&!director.isSpectator()?List.of(director):List.of();
        return data.participants(director.server,scene.owner,false).stream().filter(p->p.serverLevel()==director.serverLevel()&&p.isAlive()&&!p.isSpectator()).toList();
    }
    public static boolean near(ServerPlayer director,VisitData.Scene scene,net.minecraft.world.phys.Vec3 at,double distance){return playing(director,scene).stream().anyMatch(p->p.distanceToSqr(at)<distance);}
    public static boolean exitNear(ServerPlayer director,VisitData.Scene scene,net.minecraft.world.phys.Vec3 at,double distance){return playing(director,scene).stream().anyMatch(p->(p==director||ActivityWorld.groupFinished(scene))&&p.distanceToSqr(at)<distance);}
    public static boolean failed(ServerPlayer player,VisitData.Scene scene){return StorySessionData.get(player.server).eliminateActivity(player,scene);}
    public static void regroup(ServerPlayer actor,VisitData.Scene scene){
        var owner=actor.server.getPlayerList().getPlayer(scene.owner);if(owner==null||!StoryCoop.member(actor,scene))return;
        for(var p:playing(owner,scene))if(p!=actor){p.stopRiding();p.setForcedPose(null);p.teleportTo(actor.serverLevel(),actor.getX(),actor.getY(),actor.getZ(),actor.getYRot(),actor.getXRot());p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.fallDistance=0;}
    }
    public static void cleanPersonal(ServerPlayer p){
        com.coraline.multiplayer.ActivityPlayerState.restore(p);
        ArcheryActivity.removeIssued(p);FishingContestActivity.removeIssued(p);CakeContestActivity.removeCake(p);CarnivalActivity.removeTokens(p);VillageActivity.removeTokens(p);
        FestivalNetwork.clear(p);MineshaftActivity.leaveViewer(p);ShoppingMallActivity.clear(p);OceanMonumentActivity.restoreNativeBreathing(p);for(var stack:p.getInventory().items)if(ExpeditionActivities.temporary(stack))stack.setCount(0);if(ExpeditionActivities.temporary(p.getOffhandItem()))p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,net.minecraft.world.item.ItemStack.EMPTY);
        for(var stack:p.getInventory().items)if(stack.is(com.coraline.registry.ModItems.ACTIVITY_CHORUS_FRUIT.get())&&stack.hasTag()&&stack.getTag().hasUUID("Owner")&&stack.getTag().contains("Generation"))stack.setCount(0);
    }
    public static void cleanGuests(ServerPlayer director,VisitData.Scene scene){
        for(var p:StorySessionData.get(director.server).participants(director.server,scene.owner,true))if(p!=director)cleanPersonal(p);
    }
    private CooperativeActivities(){}
}
