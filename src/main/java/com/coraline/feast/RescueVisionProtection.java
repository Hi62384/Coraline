package com.coraline.feast;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
@Mod.EventBusSubscriber(modid="coraline")
public final class RescueVisionProtection {
    private static boolean locked(net.minecraft.world.entity.player.Player p){var s=OtherMotherLoop.scene((ServerPlayer)p);return s!=null&&RescueEpilogue.protectedVision(s);}
    @SubscribeEvent public static void interact(PlayerInteractEvent e){if(e.getEntity() instanceof ServerPlayer p&&e.isCancelable()&&locked(p))e.setCanceled(true);}
    @SubscribeEvent public static void breakBlock(net.minecraftforge.event.level.BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&locked(p))e.setCanceled(true);}
    @SubscribeEvent public static void attack(net.minecraftforge.event.entity.player.AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&locked(p))e.setCanceled(true);}
    private RescueVisionProtection(){}
}
