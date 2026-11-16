package com.coraline.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tracking-scoped, expiring appearance messages cannot authorize conversion. */
@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT)
public final class ColonyMobVisuals {
    private static final Map<UUID,Long> INFECTED=new HashMap<>();private static long tick;
    public static void update(UUID actor,boolean infected){if(infected){if(INFECTED.size()<128||INFECTED.containsKey(actor))INFECTED.put(actor,tick+240);}else INFECTED.remove(actor);}
    public static boolean infected(Entity actor){return INFECTED.getOrDefault(actor.getUUID(),0L)>tick;}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){if(event.phase!=TickEvent.Phase.END)return;tick++;if(Minecraft.getInstance().level==null)INFECTED.clear();else INFECTED.entrySet().removeIf(e->e.getValue()<=tick);}
    private ColonyMobVisuals(){}
}
