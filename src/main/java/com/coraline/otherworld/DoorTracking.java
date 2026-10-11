package com.coraline.otherworld;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="coraline")
public final class DoorTracking {
    private static void track(net.minecraft.world.entity.Entity entity){
        if(entity instanceof net.minecraft.world.entity.item.ItemEntity item&&entity.level() instanceof net.minecraft.server.level.ServerLevel level&&!item.getItem().isEmpty())DoorData.get(level.getServer()).track(item.getItem(),level,"entity",entity.blockPosition(),entity.getUUID());
    }
    @SubscribeEvent public static void join(net.minecraftforge.event.entity.EntityJoinLevelEvent event){track(event.getEntity());}
    @SubscribeEvent public static void leave(net.minecraftforge.event.entity.EntityLeaveLevelEvent event){if(event.getEntity().getRemovalReason()==net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK)track(event.getEntity());}
    @SubscribeEvent public static void move(net.minecraftforge.event.entity.EntityEvent.EnteringSection event){track(event.getEntity());}
}
