package com.coraline.client.clothing;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

@Mod.EventBusSubscriber(modid="coraline",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class SculptedArmorReload {
    private SculptedArmorReload(){}
    @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event){
        event.registerReloadListener((ResourceManagerReloadListener)manager->SculptedArmorModel.clearCache());
    }
}
