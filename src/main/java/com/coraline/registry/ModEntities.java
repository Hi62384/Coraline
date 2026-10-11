package com.coraline.registry;

import com.coraline.CoralineMod;
import com.coraline.entity.WybieEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CoralineMod.MOD_ID);

    public static final RegistryObject<EntityType<WybieEntity>> WYBIE = ENTITIES.register(
            "wybie",
            () -> EntityType.Builder.of(WybieEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 1.8F)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .build("wybie")
    );

    private ModEntities() {
    }
    public static final RegistryObject<EntityType<com.coraline.clothing.MagicBolt>> MAGIC_BOLT=ENTITIES.register("magic_bolt",()->EntityType.Builder.<com.coraline.clothing.MagicBolt>of(com.coraline.clothing.MagicBolt::new,MobCategory.MISC).sized(.2F,.2F).clientTrackingRange(8).updateInterval(1).build("magic_bolt"));
    public static final RegistryObject<EntityType<com.coraline.feast.ScenePropEntity>> SCENE_PROP=ENTITIES.register("scene_prop",()->EntityType.Builder.<com.coraline.feast.ScenePropEntity>of(com.coraline.feast.ScenePropEntity::new,MobCategory.MISC).sized(.6F,.5F).clientTrackingRange(16).updateInterval(2).build("scene_prop"));
    public static final RegistryObject<EntityType<com.coraline.otherworld.MouseEntity>> MOUSE=ENTITIES.register("mouse",()->EntityType.Builder.of(com.coraline.otherworld.MouseEntity::new,MobCategory.CREATURE).sized(.3F,.25F).clientTrackingRange(12).updateInterval(1).build("mouse"));
    public static final RegistryObject<EntityType<com.coraline.feast.FishingContestNpcEntity>> FISHING_CONTEST_NPC=ENTITIES.register("fishing_contest_npc",()->EntityType.Builder.of(com.coraline.feast.FishingContestNpcEntity::new,MobCategory.CREATURE).sized(.6F,1.8F).clientTrackingRange(12).updateInterval(2).build("fishing_contest_npc"));
    public static final RegistryObject<EntityType<com.coraline.otherworld.DoorItemEntity>> TRACKED_DOOR=ENTITIES.register("tracked_door",()->EntityType.Builder.<com.coraline.otherworld.DoorItemEntity>of(com.coraline.otherworld.DoorItemEntity::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(6).updateInterval(20).build("tracked_door"));
    public static final RegistryObject<EntityType<com.coraline.parents.ParentEntity>> MOM=parent("mom");
    public static final RegistryObject<EntityType<com.coraline.parents.ParentEntity>> DAD=parent("dad");
    public static final RegistryObject<EntityType<com.coraline.doll.BoundDollEntity>> BOUND_DOLL=ENTITIES.register("bound_doll",()->EntityType.Builder.<com.coraline.doll.BoundDollEntity>of(com.coraline.doll.BoundDollEntity::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(6).updateInterval(20).build("bound_doll"));
    private static RegistryObject<EntityType<com.coraline.parents.ParentEntity>> parent(String name){
        return ENTITIES.register(name,()->EntityType.Builder.of(com.coraline.parents.ParentEntity::new,MobCategory.CREATURE).sized(.6F,1.8F).clientTrackingRange(10).updateInterval(2).build(name));
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}
