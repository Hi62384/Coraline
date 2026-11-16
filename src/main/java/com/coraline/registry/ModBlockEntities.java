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
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.MagicMirrorBlockEntity>> MAGIC_MIRROR=BLOCK_ENTITIES.register("magic_mirror",()->BlockEntityType.Builder.of(com.coraline.postgame.MagicMirrorBlockEntity::new,ModBlocks.MAGIC_MIRROR.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.SiteCacheBlockEntity>> INFINITE_SITE_CACHE=BLOCK_ENTITIES.register("infinite_site_cache",()->BlockEntityType.Builder.of(com.coraline.postgame.SiteCacheBlockEntity::new,ModBlocks.INFINITE_SITE_CACHE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.SiteBindingBlockEntity>> INFINITE_THREAD_BINDING=BLOCK_ENTITIES.register("infinite_thread_binding",()->BlockEntityType.Builder.of(com.coraline.postgame.SiteBindingBlockEntity::new,ModBlocks.INFINITE_THREAD_BINDING.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.BreachDriveBlockEntity>> BREACH_DRIVE=BLOCK_ENTITIES.register("breach_drive",()->BlockEntityType.Builder.of(com.coraline.postgame.BreachDriveBlockEntity::new,ModBlocks.BREACH_DRIVE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.RiftMachineEntity>> RIFT_MACHINE=BLOCK_ENTITIES.register("rift_machine",()->BlockEntityType.Builder.of(com.coraline.postgame.RiftMachineEntity::new,ModBlocks.RIFT_DRILL_CORE.get(),ModBlocks.RIFT_DRILL_CONDUIT.get(),ModBlocks.RIFT_SECURITY_GATE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.SeamAnchorEntity>> SEAM_ANCHOR=BLOCK_ENTITIES.register("seam_anchor",()->BlockEntityType.Builder.of(com.coraline.postgame.SeamAnchorEntity::new,ModBlocks.SEAM_CORE.get(),ModBlocks.SEAM_DIAL.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.BoundaryDefenseEntity>> BOUNDARY_DEFENSE=BLOCK_ENTITIES.register("boundary_defense",()->BlockEntityType.Builder.of(com.coraline.postgame.BoundaryDefenseEntity::new,ModBlocks.BOUNDARY_BEACON.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.ObservatoryEntity>> OBSERVATORY=BLOCK_ENTITIES.register("observatory",()->BlockEntityType.Builder.of(com.coraline.postgame.ObservatoryEntity::new,ModBlocks.OBSERVATORY_TELESCOPE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.WellScarClueEntity>> WELL_SCAR_CLUE=BLOCK_ENTITIES.register("well_scar_clue",()->BlockEntityType.Builder.of(com.coraline.postgame.WellScarClueEntity::new,ModBlocks.WELL_SCAR_CLUE.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.FactoryControlEntity>> FACTORY_CONTROL=BLOCK_ENTITIES.register("factory_control",()->BlockEntityType.Builder.of(com.coraline.postgame.FactoryControlEntity::new,ModBlocks.FACTORY_CONTROL.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.OrphanageEntity>> ORPHANAGE=BLOCK_ENTITIES.register("orphanage",()->BlockEntityType.Builder.of(com.coraline.postgame.OrphanageEntity::new,ModBlocks.ORPHANAGE_REGISTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.CarnivalEntity>> CARNIVAL=BLOCK_ENTITIES.register("carnival",()->BlockEntityType.Builder.of(com.coraline.postgame.CarnivalEntity::new,ModBlocks.CARNIVAL_CONTROL.get()).build(null));
    public static final RegistryObject<BlockEntityType<com.coraline.postgame.PerformanceEntity>> PERFORMANCE=BLOCK_ENTITIES.register("performance",()->BlockEntityType.Builder.of(com.coraline.postgame.PerformanceEntity::new,ModBlocks.PERFORMANCE_CONTROL.get()).build(null));

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
