package com.coraline.registry;

import com.coraline.CoralineMod;
import com.coraline.block.entity.DollBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CoralineMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<DollBlockEntity>> DOLL = BLOCK_ENTITIES.register(
            "doll",
            () -> BlockEntityType.Builder.of(DollBlockEntity::new, ModBlocks.DOLL.get()).build(null)
    );

    private ModBlockEntities() {
    }
    public static final RegistryObject<BlockEntityType<com.coraline.otherworld.TunnelCoreEntity>> TUNNEL_CORE=BLOCK_ENTITIES.register("tunnel_core",()->BlockEntityType.Builder.of(com.coraline.otherworld.TunnelCoreEntity::new,ModBlocks.TUNNEL_CORE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.otherworld.DoorBlockEntity>> TINY_DOOR=BLOCK_ENTITIES.register("tiny_door",()->BlockEntityType.Builder.of(com.coraline.otherworld.DoorBlockEntity::new,ModBlocks.TINY_DOOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.parents.ParentsBedEntity>> PARENTS_BED=BLOCK_ENTITIES.register("parents_bed",()->BlockEntityType.Builder.of(com.coraline.parents.ParentsBedEntity::new,ModBlocks.PARENTS_BED.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
