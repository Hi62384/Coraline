package com.coraline.client;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="coraline",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ColonyInfectionLayers {
    @SuppressWarnings({"rawtypes","unchecked"}) @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event){for(var type:java.util.List.of(EntityType.COW,EntityType.SHEEP,EntityType.PIG,EntityType.WOLF,EntityType.ZOMBIE,EntityType.SKELETON)){net.minecraft.client.renderer.entity.LivingEntityRenderer renderer=event.getRenderer(type);if(renderer!=null)renderer.addLayer(new com.coraline.client.renderer.ColonyInfectionLayer(renderer));}}
    private ColonyInfectionLayers(){}
}
