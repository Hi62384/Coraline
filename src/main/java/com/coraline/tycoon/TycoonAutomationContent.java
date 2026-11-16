package com.coraline.tycoon;

import com.coraline.registry.ModEntities;
import net.minecraft.world.entity.*;
import net.minecraftforge.registries.RegistryObject;

public final class TycoonAutomationContent {
    public static final RegistryObject<EntityType<TycoonWorker>> WORKER=ModEntities.ENTITIES.register("tycoon_worker",()->EntityType.Builder.<TycoonWorker>of(TycoonWorker::new,MobCategory.CREATURE).sized(.6F,1.8F).clientTrackingRange(12).updateInterval(3).build("coraline:tycoon_worker"));
    public static void init(){}
    private TycoonAutomationContent(){}
}
