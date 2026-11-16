package com.coraline.tycoon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Native ordinary containers and entities inside a household use the same
 * server permission boundary as custom furniture and menu packets. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonParcelAccess {
    private static boolean denied(ServerPlayer p,BlockPos at){
        if(p.serverLevel().dimension()!=TycoonWorld.KEY)return false;
        if(TycoonRetiredParcels.ordinaryAccess(p,at))return false;
        var h=TycoonCollaborationVisits.parcel(p.server,at).orElse(null);
        return h==null||!TycoonCollaborators.allowed(p,h.owner);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity() instanceof ServerPlayer p&&denied(p,event.getPos())){event.setUseBlock(Event.Result.DENY);event.setUseItem(Event.Result.DENY);event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);event.setCanceled(true);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entity(PlayerInteractEvent.EntityInteract event){
        if(event.getEntity() instanceof ServerPlayer p&&denied(p,event.getTarget().blockPosition())){event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);event.setCanceled(true);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void precise(PlayerInteractEvent.EntityInteractSpecific event){
        if(event.getEntity() instanceof ServerPlayer p&&denied(p,event.getTarget().blockPosition())){event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);event.setCanceled(true);}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&denied(p,event.getTarget().blockPosition()))event.setCanceled(true);
    }
    private TycoonParcelAccess(){}
}
