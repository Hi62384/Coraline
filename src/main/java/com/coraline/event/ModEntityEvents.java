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
        event.put(com.coraline.tycoon.TycoonContent.NPC.get(),com.coraline.tycoon.TycoonNpc.attributes().build());
        event.put(com.coraline.tycoon.TycoonAutomationContent.WORKER.get(),com.coraline.tycoon.TycoonWorker.attributes().build());
        com.coraline.registry.CharacterCast.ENTITIES.values().forEach(type->event.put(type.get(),com.coraline.entity.CharacterEntity.attributes().build()));
        event.put(ModEntities.WYBIE.get(), WybieEntity.createAttributes().build());
        event.put(ModEntities.WYBIE_VISITOR.get(), com.coraline.entity.CharacterEntity.attributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,.3).build());
        event.put(ModEntities.BOBINSKY_VISITOR.get(),com.coraline.entity.CharacterEntity.attributes().build());
        event.put(ModEntities.SPINK_VISITOR.get(),com.coraline.entity.CharacterEntity.attributes().build());event.put(ModEntities.FORCIBLE_VISITOR.get(),com.coraline.entity.CharacterEntity.attributes().build());
        event.put(ModEntities.MOM.get(), com.coraline.parents.ParentEntity.attributes().build());
        event.put(ModEntities.DAD.get(), com.coraline.parents.ParentEntity.attributes().build());
        event.put(ModEntities.MOUSE.get(),com.coraline.otherworld.MouseEntity.attributes().build());
        event.put(ModEntities.FISHING_CONTEST_NPC.get(),com.coraline.feast.FishingContestNpcEntity.attributes().build());
    }
}
