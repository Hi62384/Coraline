package com.coraline.multiplayer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline")
public final class StoryGroupControls {
    private static boolean locked(PlayerInteractEvent e){return e.getEntity() instanceof ServerPlayer p&&StoryCoop.locked(p);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void block(PlayerInteractEvent.RightClickBlock e){if(locked(e)){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void item(PlayerInteractEvent.RightClickItem e){if(locked(e)){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entity(PlayerInteractEvent.EntityInteract e){if(locked(e)){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void entityAt(PlayerInteractEvent.EntityInteractSpecific e){if(locked(e)){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&StoryCoop.locked(p))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void hurt(LivingHurtEvent e){if(e.getEntity() instanceof ServerPlayer p&&StoryCoop.locked(p))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(LivingAttackEvent e){if(e.getEntity() instanceof ServerPlayer p&&StoryCoop.locked(p))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void mount(EntityMountEvent e){
        if(e.isDismounting()&&e.getEntityMounting() instanceof ServerPlayer p&&!p.getPersistentData().getBoolean("CoopInternalMove")&&!p.getPersistentData().getBoolean("CoralineRecovering")&&e.getEntityBeingMounted() instanceof com.coraline.feast.ScenePropEntity seat&&StoryCoop.proxySeat(seat)&&StoryCoop.seatNeeded(p))e.setCanceled(true);
    }
    private StoryGroupControls(){}
}
