package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.gametest.*;

/** Requires fresh resources and a real registry/recipe manager. Not certified by
 * the standalone sampler distribution check. Disposable protochunks only. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class InfiniteRareOreGameTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void rareOrePlacementHarvestAndSilkTouch(GameTestHelper h) {
        var binding = new InfiniteGeneration(UUID.randomUUID(), 124119);
        var strata = new EnumMap<InfiniteTerrain.Biome, InfiniteChunkTerrain.Strata>(InfiniteTerrain.Biome.class);
        for (var biome : InfiniteTerrain.Biome.values()) strata.put(biome, new InfiniteChunkTerrain.Strata(
            Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState()));
        var writer = new InfiniteChunkTerrain(binding, new InfiniteChunkTerrain.Palette(strata, Blocks.WATER.defaultBlockState(), Blocks.BEDROCK.defaultBlockState()));
        var veins = new InfiniteOreVeins(binding.seed());
        for (var ore : List.of(InfiniteOreVeins.Ore.MOONSTONE, InfiniteOreVeins.Ore.PERFECT)) {
            InfiniteOreVeins.Point found = null;
            search: for (int cx=-10; cx<=10; cx++) for (int cz=-10; cz<=10; cz++)
                for (int cy=Math.floorDiv(ore.minY,ore.vertical); cy<=Math.floorDiv(ore.maxY,ore.vertical); cy++) {
                    var point=veins.candidate(ore,cx,cy,cz,InfiniteTerrain.Biome.STITCHED_MEADOWS);
                    if (point==null) continue;
                    var column=binding.terrain().column(point.x(),point.z());
                    if (point.y()>=column.surfaceY()-3 || binding.terrain().cave(point.x(),point.y(),point.z(),column)
                        || veins.sample(point.x(),point.y(),point.z(),column.biome())!=ore) continue;
                    found=point; break search;
                }
            h.assertTrue(found!=null,"No solid-rock rare ore candidate: "+ore);
            var chunk=new ProtoChunk(new ChunkPos(Math.floorDiv(found.x(),16),Math.floorDiv(found.z(),16)), UpgradeData.EMPTY,
                LevelHeightAccessor.create(InfiniteTerrain.MIN_Y,InfiniteTerrain.HEIGHT),h.getLevel().registryAccess().registryOrThrow(Registries.BIOME),null);
            chunk.setStatus(ChunkStatus.BIOMES);
            h.assertTrue(writer.fill(chunk)==InfiniteChunkTerrain.Result.FILLED,"Rare ore chunk not filled");
            var target=new BlockPos(found.x(),found.y(),found.z());
            var block=ore==InfiniteOreVeins.Ore.MOONSTONE ? com.coraline.registry.ModBlocks.MOONSTONE_ORE.get() : com.coraline.registry.ModBlocks.PERFECT_ORE.get();
            var state=chunk.getBlockState(target);
            h.assertTrue(state.is(block),"Registered rare ore missing from generated rock: "+ore);
            var pick=new ItemStack(Items.DIAMOND_PICKAXE);
            h.assertTrue(pick.isCorrectToolForDrops(state),"Diamond tool cannot harvest "+ore);
            h.assertFalse(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state),"Wood tool bypasses rare ore tier");
            var material=ore==InfiniteOreVeins.Ore.MOONSTONE ? com.coraline.clothing.OtherMaterials.MOONSTONE.get() : com.coraline.registry.ModItems.PERFECT_ORE.get();
            var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),target,null,null,pick);
            h.assertTrue(drops.size()==1 && drops.get(0).is(material) && drops.get(0).getCount()==1,"Unexpected normal rare ore yield");
            pick.enchant(Enchantments.SILK_TOUCH,1);
            drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),target,null,null,pick);
            h.assertTrue(drops.size()==1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount()==1,"Silk Touch lost obtainable ore block");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void perfectOreHasRealSmeltingAndBlastingRecipes(GameTestHelper h) {
        var input=new SimpleContainer(new ItemStack(com.coraline.registry.ModItems.PERFECT_ORE.get()));
        for (String method : List.of("smelting","blasting")) {
            var recipe=h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","perfect_ingot_from_"+method)).orElse(null);
            h.assertTrue(recipe instanceof AbstractCookingRecipe,"Perfect processing recipe missing: "+method);
            var cooking=(AbstractCookingRecipe)recipe;
            h.assertTrue(cooking.matches(input,h.getLevel()),"Registered Perfect Ore does not match processing input");
            h.assertFalse(cooking.matches(new SimpleContainer(new ItemStack(Items.STONE)),h.getLevel()),"Unrelated stone matches Perfect processing");
            var output=cooking.assemble(input,h.getLevel().registryAccess());
            h.assertTrue(output.is(com.coraline.registry.ModItems.PERFECT_INGOT.get()) && output.getCount()==1,"Perfect processing duplicates or loses material");
            h.assertTrue(cooking.getType()==(method.equals("smelting")?RecipeType.SMELTING:RecipeType.BLASTING),"Wrong processing recipe serializer");
        }
        h.succeed();
    }
    private InfiniteRareOreGameTests() {}
}
