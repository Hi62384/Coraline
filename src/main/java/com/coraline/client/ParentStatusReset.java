package com.coraline.client;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ParentStatusReset {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){
        com.coraline.parents.ParentStatus.clientHasParents=false;
    }
}
