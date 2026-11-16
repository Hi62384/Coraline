package com.coraline.client.clothing;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class PerfectPowerVisuals {
    private static final Set<UUID> POWERED=new HashSet<>();
    public static void set(UUID player,boolean active){if(active)POWERED.add(player);else POWERED.remove(player);}
    public static boolean powered(LivingEntity entity){return POWERED.contains(entity.getUUID());}
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){POWERED.clear();}
    private PerfectPowerVisuals(){}
}
