package com.coraline.feast;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;

/** Vanilla target UUIDs relax near despawn; visit overflow remains owner-only. */
@Mod.EventBusSubscriber(modid="coraline")
public final class VisitGiftOwnership {
    @SubscribeEvent public static void pickup(EntityItemPickupEvent event){
        var tag=event.getItem().getPersistentData();
        if(tag.hasUUID("CoralineVisitGiftOwner")&&!tag.getUUID("CoralineVisitGiftOwner").equals(event.getEntity().getUUID()))event.setCanceled(true);
    }
    private VisitGiftOwnership(){}
}
