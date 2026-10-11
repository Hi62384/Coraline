package com.coraline.registry;

import com.coraline.CoralineMod;
import com.coraline.item.DollItem;
import com.coraline.item.DowsingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CoralineMod.MOD_ID);

    public static final RegistryObject<Item> MIDNIGHT_COAT=clothing(com.coraline.clothing.ClothingItem.Style.MIDNIGHT_COAT);
    public static final RegistryObject<Item> STAR_SHIRT=clothing(com.coraline.clothing.ClothingItem.Style.STAR_SHIRT);
    public static final RegistryObject<Item> RAINCOAT=clothing(com.coraline.clothing.ClothingItem.Style.RAINCOAT);
    public static final RegistryObject<Item> COMFY_PAJAMAS=clothing(com.coraline.clothing.ClothingItem.Style.COMFY_PAJAMAS);
    public static final RegistryObject<Item> GARDEN_OVERALLS=clothing(com.coraline.clothing.ClothingItem.Style.GARDEN_OVERALLS);
    public static final RegistryObject<Item> CIRCUS_JACKET=clothing(com.coraline.clothing.ClothingItem.Style.CIRCUS_JACKET);
    public static final RegistryObject<Item> SILENT_SLIPPERS=clothing(com.coraline.clothing.ClothingItem.Style.SILENT_SLIPPERS);
    public static final RegistryObject<Item> FEATHER_BOOTS=clothing(com.coraline.clothing.ClothingItem.Style.FEATHER_BOOTS);
    public static final RegistryObject<Item> TOP_HAT=clothing(com.coraline.clothing.ClothingItem.Style.TOP_HAT);
    public static final RegistryObject<Item> MIDNIGHT_GOGGLES=clothing(com.coraline.clothing.ClothingItem.Style.MIDNIGHT_GOGGLES);
    public static final RegistryObject<Item> UNBINDING_BUTTON_EYES=clothing(com.coraline.clothing.ClothingItem.Style.UNBINDING_BUTTON_EYES);
    private static RegistryObject<Item> clothing(com.coraline.clothing.ClothingItem.Style style){return ITEMS.register(style.id(),()->new com.coraline.clothing.ClothingItem(style));}

    public static final RegistryObject<Item> DOWSING_ROD = ITEMS.register(
            "dowsing_rod",
            () -> new DowsingRodItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<Item> DOLL = ITEMS.register(
            "doll",
            () -> new DollItem(ModBlocks.DOLL.get(), new Item.Properties().stacksTo(1))
    );

    private ModItems() {
    }
    public static final RegistryObject<Item> WELCOME_CAKE=ITEMS.register("welcome_cake",com.coraline.feast.WelcomeCakeItem::new);
    public static final RegistryObject<Item> MOUSE_DEBUG=ITEMS.register("mouse_debug",com.coraline.item.MouseDebugItem::new);
    public static final RegistryObject<Item> DOLL_DEBUG=ITEMS.register("doll_debug",com.coraline.item.DollDebugItem::new);
    public static final RegistryObject<Item> FIRST_VISIT_DEBUG=ITEMS.register("first_visit_debug",com.coraline.item.FirstVisitDebugItem::new);
    public static final RegistryObject<Item> FAMILY_SETUP_DEBUG=ITEMS.register("family_setup_debug",com.coraline.item.FamilySetupDebugItem::new);
    public static final RegistryObject<Item> VILLAGE_ACTIVITY_DEBUG=ITEMS.register("village_activity_debug",com.coraline.item.VillageActivityDebugItem::new);
    public static final RegistryObject<Item> TRUST_UP_DEBUG=ITEMS.register("trust_up_debug",()->new com.coraline.item.RelationshipDebugItem(true,1));
    public static final RegistryObject<Item> TRUST_DOWN_DEBUG=ITEMS.register("trust_down_debug",()->new com.coraline.item.RelationshipDebugItem(true,-1));
    public static final RegistryObject<Item> SUSPICION_UP_DEBUG=ITEMS.register("suspicion_up_debug",()->new com.coraline.item.RelationshipDebugItem(false,1));
    public static final RegistryObject<Item> SUSPICION_DOWN_DEBUG=ITEMS.register("suspicion_down_debug",()->new com.coraline.item.RelationshipDebugItem(false,-1));
    public static final RegistryObject<Item> PARENTS_BED=ITEMS.register("parents_bed",()->new com.coraline.parents.ParentsBedItem(ModBlocks.PARENTS_BED.get(),new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TINY_DOOR=ITEMS.register("tiny_door",com.coraline.item.TinyDoorItem::new);
    public static final RegistryObject<Item> VILLAGE_TOKEN=ITEMS.register("village_token",()->new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> MAGIC_BUTTON_TOKEN=ITEMS.register("magic_button_token",()->new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> PERFECT_INGOT=ITEMS.register("perfect_ingot",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> SOULGLASS=ITEMS.register("soulglass",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> OTHER_DIAMOND_BLOCK=block("other_diamond_block",ModBlocks.OTHER_DIAMOND_BLOCK);
    public static final RegistryObject<Item> OTHER_IRON_BLOCK=block("other_iron_block",ModBlocks.OTHER_IRON_BLOCK);
    public static final RegistryObject<Item> OTHER_IRON_ORE=block("other_iron_ore",ModBlocks.OTHER_IRON_ORE);
    public static final RegistryObject<Item> OTHER_DIAMOND_ORE=block("other_diamond_ore",ModBlocks.OTHER_DIAMOND_ORE);
    public static final RegistryObject<Item> MOONSTONE_ORE=block("moonstone_ore",ModBlocks.MOONSTONE_ORE);
    public static final RegistryObject<Item> SOULGLASS_ORE=block("soulglass_ore",ModBlocks.SOULGLASS_ORE);
    public static final RegistryObject<Item> MOONSTONE_BLOCK=block("moonstone_block",ModBlocks.MOONSTONE_BLOCK);
    public static final RegistryObject<Item> PERFECT_BLOCK=block("perfect_block",ModBlocks.PERFECT_BLOCK);
    public static final RegistryObject<Item> SOULGLASS_BLOCK=block("soulglass_block",ModBlocks.SOULGLASS_BLOCK);
    public static final RegistryObject<Item> ARCHERY_BOW=ITEMS.register("archery_bow",com.coraline.item.ArcheryBowItem::new);
    public static final RegistryObject<Item> ARCHERY_ARROW=ITEMS.register("archery_arrow",com.coraline.item.ArcheryArrowItem::new);
    public static final RegistryObject<Item> ARCHERY_ACTIVITY_DEBUG=ITEMS.register("archery_activity_debug",com.coraline.item.ArcheryActivityDebugItem::new);
    public static final RegistryObject<Item> CAKE_ACTIVITY_DEBUG=ITEMS.register("cake_activity_debug",com.coraline.item.CakeActivityDebugItem::new);
    public static final RegistryObject<Item> CONTEST_CAKE=ITEMS.register("contest_cake",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CARNIVAL_TOKEN=ITEMS.register("carnival_token",()->new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> CARNIVAL_RING=ITEMS.register("carnival_ring",()->new com.coraline.item.CarnivalThrowableItem(false));
    public static final RegistryObject<Item> CARNIVAL_DART=ITEMS.register("carnival_dart",()->new com.coraline.item.CarnivalThrowableItem(true));
    public static final RegistryObject<Item> CARNIVAL_MALLET=ITEMS.register("carnival_mallet",()->new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CARNIVAL_ACTIVITY_DEBUG=ITEMS.register("carnival_activity_debug",com.coraline.item.CarnivalActivityDebugItem::new);
    public static final RegistryObject<Item> DREAM_PARKOUR_ACTIVITY_DEBUG=ITEMS.register("dream_parkour_activity_debug",com.coraline.item.DreamParkourActivityDebugItem::new);
    public static final RegistryObject<Item> FISHING_CONTEST_ROD=ITEMS.register("fishing_contest_rod",com.coraline.item.FishingContestRodItem::new);
    public static final RegistryObject<Item> FISHING_ACTIVITY_DEBUG=ITEMS.register("fishing_activity_debug",com.coraline.item.FishingActivityDebugItem::new);
    public static final RegistryObject<Item> GARDEN_MAZE_ACTIVITY_DEBUG=ITEMS.register("garden_maze_activity_debug",com.coraline.item.GardenMazeActivityDebugItem::new);
    public static final RegistryObject<Item> GRAND_FEAST_ACTIVITY_DEBUG=ITEMS.register("grand_feast_activity_debug",com.coraline.item.GrandFeastActivityDebugItem::new);
    public static final RegistryObject<Item> ICE_BOAT_RACE_ACTIVITY_DEBUG=ITEMS.register("ice_boat_race_activity_debug",com.coraline.item.IceBoatRaceActivityDebugItem::new);
    public static final RegistryObject<Item> MAGIC_SHOW_ACTIVITY_DEBUG=ITEMS.register("magic_show_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.MAGIC_SHOW));
    public static final RegistryObject<Item> MAGIC_GARDEN_ACTIVITY_DEBUG=ITEMS.register("magic_garden_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.MAGIC_GARDEN));
    public static final RegistryObject<Item> OCEAN_MONUMENT_ACTIVITY_DEBUG=ITEMS.register("ocean_monument_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.OTHER_OCEAN_MONUMENT));
    public static final RegistryObject<Item> MUSIC_FESTIVAL_ACTIVITY_DEBUG=ITEMS.register("music_festival_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.MUSIC_FESTIVAL));
    public static final RegistryObject<Item> DESERT_TEMPLE_ACTIVITY_DEBUG=ITEMS.register("desert_temple_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.OTHER_DESERT_TEMPLE));
    public static final RegistryObject<Item> END_CITY_ACTIVITY_DEBUG=ITEMS.register("end_city_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.OTHER_END_CITY));
    public static final RegistryObject<Item> ACTIVITY_CHORUS_FRUIT=ITEMS.register("activity_chorus_fruit",com.coraline.item.ActivityChorusFruitItem::new);
    public static final RegistryObject<Item> MINESHAFT_ACTIVITY_DEBUG=ITEMS.register("mineshaft_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.OTHER_MINESHAFT));
    public static final RegistryObject<Item> FORTRESS_ACTIVITY_DEBUG=ITEMS.register("fortress_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.OTHER_NETHER_FORTRESS));
    public static final RegistryObject<Item> MANSION_ACTIVITY_DEBUG=ITEMS.register("mansion_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.OTHER_WOODLAND_MANSION));
    public static final RegistryObject<Item> ACTIVITY_MINING_SHOVEL=ITEMS.register("activity_mining_shovel",com.coraline.item.ExpeditionItem::new);
    public static final RegistryObject<Item> ACTIVITY_MINE_MAP=ITEMS.register("activity_mine_map",com.coraline.item.ExpeditionItem::new);
    public static final RegistryObject<Item> ACTIVITY_MINING_PICK=ITEMS.register("activity_mining_pick",com.coraline.item.ExpeditionItem::new);
    public static final RegistryObject<Item> MANSION_KEY=ITEMS.register("mansion_key",com.coraline.item.ExpeditionItem::new);
    public static final RegistryObject<Item> SNOWBALL_POUCH=ITEMS.register("snowball_pouch",com.coraline.item.ExpeditionItem::new);
    public static final RegistryObject<Item> TREASURE_NOTE=ITEMS.register("treasure_note",com.coraline.item.ExpeditionItem::new);
    public static final RegistryObject<Item> TREASURE_ACTIVITY_DEBUG=ITEMS.register("treasure_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.TREASURE_HUNT));
    public static final RegistryObject<Item> MALL_ACTIVITY_DEBUG=ITEMS.register("mall_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.SHOPPING_MALL));
    public static final RegistryObject<Item> SNOWBALL_ACTIVITY_DEBUG=ITEMS.register("snowball_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.SNOWBALL_BATTLE));
    public static final RegistryObject<Item> MICE_CIRCUS_ACTIVITY_DEBUG=ITEMS.register("mice_circus_activity_debug",()->new com.coraline.item.MagicActivityDebugItem(com.coraline.feast.OtherMotherActivities.Activity.MICE_CIRCUS));
    public static final RegistryObject<Item> BLUE_VELVET_SOFA=block("blue_velvet_sofa",ModBlocks.BLUE_VELVET_SOFA);
    public static final RegistryObject<Item> ROSE_VELVET_SOFA=block("rose_velvet_sofa",ModBlocks.ROSE_VELVET_SOFA);
    public static final RegistryObject<Item> LOW_MARBLE_TABLE=block("low_marble_table",ModBlocks.LOW_MARBLE_TABLE);
    public static final RegistryObject<Item> BRASS_FLOOR_LAMP=block("brass_floor_lamp",ModBlocks.BRASS_FLOOR_LAMP);
    public static final RegistryObject<Item> CHERRY_SIDEBOARD=block("cherry_sideboard",ModBlocks.CHERRY_SIDEBOARD);
    public static final RegistryObject<Item> FLOWER_CONSOLE=block("flower_console",ModBlocks.FLOWER_CONSOLE);
    public static final RegistryObject<Item> BUTTON_ART_PANEL=block("button_art_panel",ModBlocks.BUTTON_ART_PANEL);
    public static final RegistryObject<Item> MOON_ART_PANEL=block("moon_art_panel",ModBlocks.MOON_ART_PANEL);
    public static final RegistryObject<Item> MODERN_WALL_LIGHT=block("modern_wall_light",ModBlocks.MODERN_WALL_LIGHT);
    public static final RegistryObject<Item> CAKE_COCOA_RACK=block("cake_cocoa_rack",ModBlocks.CAKE_COCOA_RACK);
    public static final RegistryObject<Item> CAKE_VANILLA_RACK=block("cake_vanilla_rack",ModBlocks.CAKE_VANILLA_RACK);
    public static final RegistryObject<Item> CAKE_BLUEBERRY_RACK=block("cake_blueberry_rack",ModBlocks.CAKE_BLUEBERRY_RACK);
    private static RegistryObject<Item> block(String id,RegistryObject<net.minecraft.world.level.block.Block> block){
        return ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()));
    }
    public static final RegistryObject<Item> DADS_SLIME=ITEMS.register("dads_slime",com.coraline.item.DadsSlimeItem::new);
    public static final RegistryObject<Item> MUSTARD_KETCHUP_SALSA_WRAP=ITEMS.register("mustard_ketchup_salsa_wrap",()->new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(4).saturationMod(.6F).build())));

    public static final RegistryObject<Item> WYBIE_SPAWN_EGG = ITEMS.register("wybie_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.WYBIE, 0x202731, 0xb58a5c, new Item.Properties()));
    public static final RegistryObject<Item> PARENTS_SPAWN_EGG = ITEMS.register("parents_spawn_egg",
            com.coraline.item.ParentsSpawnEggItem::new);

    public static void register(IEventBus modEventBus) {
        com.coraline.clothing.OtherMaterials.init();
        com.coraline.clothing.Weapons.init();
        com.coraline.clothing.ArmorSets.init();
        com.coraline.clothing.UtilityItems.init();
        com.coraline.clothing.ArtifactItems.init();
        ITEMS.register(modEventBus);
    }
}
