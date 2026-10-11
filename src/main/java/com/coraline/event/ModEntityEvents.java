package com.coraline.event;

import com.coraline.CoralineMod;
import com.coraline.entity.WybieEntity;
import com.coraline.registry.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CoralineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {
    private ModEntityEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.WYBIE.get(), WybieEntity.createAttributes().build());
        event.put(ModEntities.MOM.get(), com.coraline.parents.ParentEntity.attributes().build());
        event.put(ModEntities.DAD.get(), com.coraline.parents.ParentEntity.attributes().build());
        event.put(ModEntities.MOUSE.get(),com.coraline.otherworld.MouseEntity.attributes().build());
        event.put(ModEntities.FISHING_CONTEST_NPC.get(),com.coraline.feast.FishingContestNpcEntity.attributes().build());
    }
}
