package com.coraline.registry;

import com.coraline.CoralineMod;
import com.coraline.block.DollBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CoralineMod.MOD_ID);
    public static final RegistryObject<Block> LOCKED_HOUSE_DOOR=BLOCKS.register("locked_house_door",com.coraline.block.LockedHouseDoorBlock::new);
    public static final RegistryObject<Block> INFINITE_SITE_CACHE=BLOCKS.register("infinite_site_cache",com.coraline.postgame.SiteCacheBlock::new);
    public static final RegistryObject<Block> INFINITE_THREAD_BINDING=BLOCKS.register("infinite_thread_binding",com.coraline.postgame.SiteThreadBinding::new);
    public static final RegistryObject<Block> BREACH_DRIVE=BLOCKS.register("breach_drive",com.coraline.postgame.BreachDriveBlock::new);
    public static final RegistryObject<Block> MAGIC_MIRROR=BLOCKS.register("magic_mirror",com.coraline.postgame.MagicMirrorBlock::new);
    public static final RegistryObject<Block> INFINITE_RETURN_PORTAL=BLOCKS.register("infinite_return_portal",com.coraline.postgame.InfiniteReturnPortal::new);
    public static final RegistryObject<Block> RIFT_DRILL_CORE=BLOCKS.register("rift_drill_core",()->new com.coraline.postgame.RiftMachineBlock(0));
    public static final RegistryObject<Block> RIFT_DRILL_CONDUIT=BLOCKS.register("rift_drill_conduit",()->new com.coraline.postgame.RiftMachineBlock(1));
    public static final RegistryObject<Block> RIFT_SECURITY_GATE=BLOCKS.register("rift_security_gate",()->new com.coraline.postgame.RiftMachineBlock(2));
    public static final RegistryObject<Block> SEAM_CORE=BLOCKS.register("dimensional_seam_core",()->new com.coraline.postgame.SeamAnchorBlock(true));
    public static final RegistryObject<Block> SEAM_DIAL=BLOCKS.register("dimensional_seam_dial",()->new com.coraline.postgame.SeamAnchorBlock(false));
    public static final RegistryObject<Block> BOUNDARY_BEACON=BLOCKS.register("boundary_defense_beacon",com.coraline.postgame.BoundaryDefenseBlock::new);
    public static final RegistryObject<Block> OBSERVATORY_TELESCOPE=BLOCKS.register("observatory_telescope",com.coraline.postgame.ObservatoryBlock::new);
    public static final RegistryObject<Block> OBSERVATORY_STAR=BLOCKS.register("observatory_star",com.coraline.postgame.ObservatoryStarBlock::new);
    public static final RegistryObject<Block> INFECTION_HEART=BLOCKS.register("infection_heart",()->new com.coraline.postgame.InfectionBlock(true));
    public static final java.util.Map<String,RegistryObject<Block>> INFECTION_VARIANTS=infectionVariants();
    private static java.util.Map<String,RegistryObject<Block>> infectionVariants(){var out=new java.util.LinkedHashMap<String,RegistryObject<Block>>();for(String kind:com.coraline.postgame.InfectionColonies.VARIANTS)out.put(kind,BLOCKS.register("infected_"+kind,()->new com.coraline.postgame.InfectionBlock(false)));return java.util.Collections.unmodifiableMap(out);}
    public static final RegistryObject<Block> FACTORY_CONTROL=BLOCKS.register("factory_control",com.coraline.postgame.FactoryControlBlock::new);
    public static final RegistryObject<Block> TRUE_HEART_ANCHOR=BLOCKS.register("true_heart_anchor",com.coraline.postgame.TrueHeartAnchorBlock::new);
    public static final RegistryObject<Block> WELL_SCAR_CLUE=BLOCKS.register("well_scar_clue",com.coraline.postgame.WellScarClueBlock::new);
    public static final RegistryObject<Block> ORPHANAGE_REGISTER=BLOCKS.register("orphanage_register",com.coraline.postgame.OrphanageBlock::new);
    public static final RegistryObject<Block> ORPHANAGE_BINDING=BLOCKS.register("orphanage_binding",com.coraline.postgame.OrphanageBindingBlock::new);
    public static final RegistryObject<Block> CARNIVAL_CONTROL=BLOCKS.register("carnival_control",com.coraline.postgame.CarnivalBlock::new);
    public static final RegistryObject<Block> PERFORMANCE_CONTROL=BLOCKS.register("performance_control",com.coraline.postgame.PerformanceBlock::new);
    public static final RegistryObject<Block> PERFORMANCE_TILE=BLOCKS.register("performance_tile",com.coraline.postgame.PerformanceTileBlock::new);
    public static final RegistryObject<Block> INSECT_PARLOR_WALLPAPER=BLOCKS.register("insect_parlor_wallpaper",()->new Block(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WHITE_CONCRETE).noLootTable()));

    public static final RegistryObject<Block> DOLL = BLOCKS.register(
            "doll",
            () -> new DollBlock(BlockBehaviour.Properties.of()
                    .strength(0.35F)
                    .sound(SoundType.WOOL)
                    .noOcclusion())
    );

    private ModBlocks() {
    }
    public static final RegistryObject<Block> STORY_WEB=BLOCKS.register("story_web",com.coraline.block.StoryWebBlock::new);
    public static final RegistryObject<Block> FADING_COBWEB=BLOCKS.register("fading_cobweb",com.coraline.clothing.FadingCobwebBlock::new);
    public static final RegistryObject<Block> CHEESE_PARCEL=BLOCKS.register("cheese_parcel",com.coraline.bobinsky.CheeseParcelBlock::new);
    public static final RegistryObject<Block> TEA_THEATRE_POSTER=BLOCKS.register("tea_theatre_poster",()->new Block(BlockBehaviour.Properties.of().strength(1).noOcclusion().noLootTable()));
    public static final RegistryObject<Block> TEA_MEMORIAL_DOG=BLOCKS.register("tea_memorial_dog",()->new Block(BlockBehaviour.Properties.of().strength(1).noOcclusion().noCollission().noLootTable()));
    public static final RegistryObject<Block> TEA_WEATHERED_BRICKS=BLOCKS.register("tea_weathered_bricks",()->new Block(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.BRICKS)));
    public static final RegistryObject<Block> TEA_VELVET_SOFA=furnishing("tea_velvet_sofa",com.coraline.block.FurnishingBlock.Kind.SOFA,0);
    public static final RegistryObject<Block> ROSEWOOD_MIRROR=rosewood("rosewood_mirror",com.coraline.otherworld.RosewoodFixtureBlock.Kind.MIRROR);
    public static final RegistryObject<Block> ROSEWOOD_DRAWER=BLOCKS.register("rosewood_drawer",()->new com.coraline.secrets.HouseStorageBlock(true));
    public static final RegistryObject<Block> ROSEWOOD_CABINET=BLOCKS.register("rosewood_cabinet",()->new com.coraline.secrets.HouseStorageBlock(false));
    public static final RegistryObject<Block> ROSEWOOD_GLASS_DOOR=BLOCKS.register("rosewood_glass_door",com.coraline.otherworld.RosewoodGlassDoorBlock::new);
    public static final RegistryObject<Block> ROSEWOOD_SINK=rosewood("rosewood_sink",com.coraline.otherworld.RosewoodFixtureBlock.Kind.SINK);
    public static final RegistryObject<Block> ROSEWOOD_COUNTER=rosewood("rosewood_counter",com.coraline.otherworld.RosewoodFixtureBlock.Kind.COUNTER);
    public static final RegistryObject<Block> ROSEWOOD_OVEN=rosewood("rosewood_oven",com.coraline.otherworld.RosewoodFixtureBlock.Kind.COUNTER);
    public static final RegistryObject<Block> ROSEWOOD_TOILET=rosewood("rosewood_toilet",com.coraline.otherworld.RosewoodFixtureBlock.Kind.TOILET);
    public static final RegistryObject<Block> ROSEWOOD_BATH=rosewood("rosewood_bath",com.coraline.otherworld.RosewoodFixtureBlock.Kind.BATH);
    public static final RegistryObject<Block> ROSEWOOD_TABLE=rosewood("rosewood_table",com.coraline.otherworld.RosewoodFixtureBlock.Kind.TABLE);
    public static final RegistryObject<Block> ROSEWOOD_CHAIR=rosewood("rosewood_chair",com.coraline.otherworld.RosewoodFixtureBlock.Kind.CHAIR);
    public static final RegistryObject<Block> ROSEWOOD_SOFA=rosewood("rosewood_sofa",com.coraline.otherworld.RosewoodFixtureBlock.Kind.SOFA);
    public static final RegistryObject<Block> ROSEWOOD_COAT_RACK=rosewood("rosewood_coat_rack",com.coraline.otherworld.RosewoodFixtureBlock.Kind.RACK);
    public static final RegistryObject<Block> ROSEWOOD_TABLE_VASE=rosewood("rosewood_table_vase",com.coraline.otherworld.RosewoodFixtureBlock.Kind.TABLE);
    public static final RegistryObject<Block> ROSEWOOD_TABLE_LAMP=rosewood("rosewood_table_lamp",com.coraline.otherworld.RosewoodFixtureBlock.Kind.TABLE);
    private static RegistryObject<Block> rosewood(String id,com.coraline.otherworld.RosewoodFixtureBlock.Kind kind){return BLOCKS.register(id,()->new com.coraline.otherworld.RosewoodFixtureBlock(kind));}
    public static final RegistryObject<Block> OTHER_DIAMOND_BLOCK=storage("other_diamond_block",5.0F);
    public static final RegistryObject<Block> OTHER_IRON_BLOCK=storage("other_iron_block",4.0F);
    public static final RegistryObject<Block> OTHER_IRON_ORE=ore("other_iron_ore");
    public static final RegistryObject<Block> OTHER_DIAMOND_ORE=ore("other_diamond_ore");
    public static final RegistryObject<Block> MOONSTONE_ORE=ore("moonstone_ore");
    public static final RegistryObject<Block> PERFECT_ORE=ore("perfect_ore");
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
    public static final BooleanProperty TUNNEL_LINED=BooleanProperty.create("lined");
    public static final RegistryObject<Block> TUNNEL_FABRIC=BLOCKS.register("tunnel_fabric",()->new Block(BlockBehaviour.Properties.of().strength(-1,3600000).lightLevel(s->8).sound(SoundType.WOOL)){
        {registerDefaultState(defaultBlockState().setValue(TUNNEL_LINED,false));}
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(TUNNEL_LINED);}
        @Override public RenderShape getRenderShape(BlockState state){return state.getValue(TUNNEL_LINED)?RenderShape.INVISIBLE:RenderShape.MODEL;}
    });
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
