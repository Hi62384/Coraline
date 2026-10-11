package com.coraline.registry;

import com.coraline.CoralineMod;
import com.coraline.block.DollBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CoralineMod.MOD_ID);

    public static final RegistryObject<Block> DOLL = BLOCKS.register(
            "doll",
            () -> new DollBlock(BlockBehaviour.Properties.of()
                    .strength(0.35F)
                    .sound(SoundType.WOOL)
                    .noOcclusion())
    );

    private ModBlocks() {
    }
    public static final RegistryObject<Block> FADING_COBWEB=BLOCKS.register("fading_cobweb",com.coraline.clothing.FadingCobwebBlock::new);
    public static final RegistryObject<Block> OTHER_DIAMOND_BLOCK=storage("other_diamond_block",5.0F);
    public static final RegistryObject<Block> OTHER_IRON_BLOCK=storage("other_iron_block",4.0F);
    public static final RegistryObject<Block> OTHER_IRON_ORE=ore("other_iron_ore");
    public static final RegistryObject<Block> OTHER_DIAMOND_ORE=ore("other_diamond_ore");
    public static final RegistryObject<Block> MOONSTONE_ORE=ore("moonstone_ore");
    public static final RegistryObject<Block> SOULGLASS_ORE=ore("soulglass_ore");
    private static RegistryObject<Block> ore(String id){return BLOCKS.register(id,()->new Block(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE).strength(3.5F,3).requiresCorrectToolForDrops()));}
    public static final RegistryObject<Block> MOONSTONE_BLOCK=storage("moonstone_block",4.5F);
    public static final RegistryObject<Block> PERFECT_BLOCK=storage("perfect_block",6.0F);
    public static final RegistryObject<Block> PERFECT_FARMLAND=BLOCKS.register("perfect_farmland",com.coraline.block.PerfectFarmlandBlock::new);
    public static final RegistryObject<Block> MOONLIT_LANTERN=BLOCKS.register("moonlit_lantern",com.coraline.block.MoonlitLanternBlock::new);
    public static final RegistryObject<Block> SOULGLASS_BLOCK=BLOCKS.register("soulglass_block",()->
        new net.minecraft.world.level.block.GlassBlock(BlockBehaviour.Properties.of()
            .strength(.4F).sound(SoundType.GLASS).noOcclusion()));
    private static RegistryObject<Block> storage(String id,float hardness){return BLOCKS.register(id,()->
        new Block(BlockBehaviour.Properties.of().strength(hardness,hardness*6).sound(SoundType.METAL)));}
    public static final RegistryObject<Block> WELCOME_CAKE=BLOCKS.register("welcome_cake",com.coraline.feast.WelcomeCakeBlock::new);
    public static final RegistryObject<Block> TUNNEL_CORE=BLOCKS.register("tunnel_core",com.coraline.otherworld.TunnelCoreBlock::new);
    public static final RegistryObject<Block> TUNNEL_FABRIC=BLOCKS.register("tunnel_fabric",()->new Block(BlockBehaviour.Properties.of().strength(-1,3600000).lightLevel(s->8).sound(SoundType.WOOL)));
    public static final RegistryObject<Block> PARENTS_BED=BLOCKS.register("parents_bed",()->new com.coraline.parents.ParentsBedBlock(BlockBehaviour.Properties.of().strength(1).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> TINY_DOOR=BLOCKS.register("tiny_door",()->new com.coraline.block.TinyDoorBlock(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Block> BLUE_VELVET_SOFA=furnishing("blue_velvet_sofa",com.coraline.block.FurnishingBlock.Kind.SOFA,0);
    public static final RegistryObject<Block> ROSE_VELVET_SOFA=furnishing("rose_velvet_sofa",com.coraline.block.FurnishingBlock.Kind.SOFA,0);
    public static final RegistryObject<Block> LOW_MARBLE_TABLE=furnishing("low_marble_table",com.coraline.block.FurnishingBlock.Kind.TABLE,0);
    public static final RegistryObject<Block> BRASS_FLOOR_LAMP=furnishing("brass_floor_lamp",com.coraline.block.FurnishingBlock.Kind.LAMP,12);
    public static final RegistryObject<Block> CHERRY_SIDEBOARD=furnishing("cherry_sideboard",com.coraline.block.FurnishingBlock.Kind.SIDEBOARD,0);
    public static final RegistryObject<Block> FLOWER_CONSOLE=furnishing("flower_console",com.coraline.block.FurnishingBlock.Kind.CONSOLE,0);
    public static final RegistryObject<Block> BUTTON_ART_PANEL=furnishing("button_art_panel",com.coraline.block.FurnishingBlock.Kind.WALL_PANEL,0);
    public static final RegistryObject<Block> MOON_ART_PANEL=furnishing("moon_art_panel",com.coraline.block.FurnishingBlock.Kind.WALL_PANEL,0);
    public static final RegistryObject<Block> MODERN_WALL_LIGHT=furnishing("modern_wall_light",com.coraline.block.FurnishingBlock.Kind.WALL_PANEL,13);
    public static final RegistryObject<Block> CAKE_COCOA_RACK=cakeRack("cake_cocoa_rack");
    public static final RegistryObject<Block> CAKE_VANILLA_RACK=cakeRack("cake_vanilla_rack");
    public static final RegistryObject<Block> CAKE_BLUEBERRY_RACK=cakeRack("cake_blueberry_rack");
    private static RegistryObject<Block> cakeRack(String id){return BLOCKS.register(id,()->new Block(BlockBehaviour.Properties.of().strength(.8F).sound(SoundType.WOOD).noOcclusion()));}
    private static RegistryObject<Block> furnishing(String id,com.coraline.block.FurnishingBlock.Kind kind,int light){
        return BLOCKS.register(id,()->new com.coraline.block.FurnishingBlock(BlockBehaviour.Properties.of()
            .strength(1.5F).sound(SoundType.WOOD).noOcclusion().lightLevel(state->light),kind));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
