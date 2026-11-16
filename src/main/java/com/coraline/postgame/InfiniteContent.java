package com.coraline.postgame;

import com.coraline.registry.ModBlocks;
import com.coraline.registry.ModItems;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

/** Authored postgame materials. Registration does not unlock the dimension. */
public final class InfiniteContent {
    private static final Map<String, RegistryObject<Block>> BLOCKS = new LinkedHashMap<>();
    public static final RegistryObject<Block> ILLUSION_MIRROR=ModBlocks.BLOCKS.register("illusion_mirror",IllusionMirrorBlock::new);
    static {
        add("stitched_grass", () -> new Block(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)));
        add("other_soil", () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIRT)));
        add("rootbound_soil", () -> new Block(BlockBehaviour.Properties.copy(Blocks.ROOTED_DIRT)));
        add("otherworld_moss", () -> new Block(BlockBehaviour.Properties.copy(Blocks.MOSS_BLOCK)));
        add("velvet_mycelium", () -> new Block(BlockBehaviour.Properties.copy(Blocks.MYCELIUM)));
        add("stitched_clay", () -> new Block(BlockBehaviour.Properties.copy(Blocks.TERRACOTTA)));
        for (String id : new String[]{"porcelain_stone", "voidstone", "candy_stone", "clockwork_stone", "mirrorstone", "threadbound_rock", "pale_rift_stone"})
            add(id, () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE).requiresCorrectToolForDrops()));
        add("mirror_sand", () -> new SandBlock(0x9cacbf, BlockBehaviour.Properties.copy(Blocks.SAND)));
        for (String species : new String[]{"buttonwood", "paperwood", "lanternwood"}) {
            int light = species.equals("lanternwood") ? 5 : 0;
            add(species + "_log", () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG).lightLevel(s -> light)));
            add(species + "_planks", () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).lightLevel(s -> light / 2)));
            add(species + "_leaves", () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).lightLevel(s -> light)));
        }
        add("velvet_mushroom_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.RED_MUSHROOM_BLOCK)));
        add("velvet_mushroom_stem", () -> new Block(BlockBehaviour.Properties.copy(Blocks.MUSHROOM_STEM)));
        ore("coal",0,2);ore("copper",0,0);ore("gold",0,0);ore("redstone",1,5);ore("lapis",2,5);ore("emerald",3,7);ore("quartz",2,5);
    }
    private static void ore(String name,int minExperience,int maxExperience){add("other_"+name+"_ore",()->new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_COAL_ORE).requiresCorrectToolForDrops(),net.minecraft.util.valueproviders.UniformInt.of(minExperience,maxExperience)));}
    static boolean material(net.minecraft.world.item.Item item){return item instanceof BlockItem block&&BLOCKS.values().stream().anyMatch(entry->entry.get()==block.getBlock());}
    private static void add(String id, Supplier<Block> factory) {
        RegistryObject<Block> block = ModBlocks.BLOCKS.register(id, factory);
        BLOCKS.put(id, block);
        ModItems.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    public static Block block(String id) {
        RegistryObject<Block> block = BLOCKS.get(id);
        if (block == null) throw new IllegalArgumentException("Unknown postgame material: " + id);
        return block.get();
    }
    public static void init() {}
    private InfiniteContent() {}
}
