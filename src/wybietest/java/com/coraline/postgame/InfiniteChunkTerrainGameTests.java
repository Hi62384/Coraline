package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.*;

/** Disposable protochunks only; no existing world blocks are replaced. Vanilla
 * test strata are fixtures, not the missing runtime postgame art/palette. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class InfiniteChunkTerrainGameTests {
    private static ProtoChunk chunk(GameTestHelper h,int x,int z){
        var chunk=new ProtoChunk(new ChunkPos(x,z),UpgradeData.EMPTY,LevelHeightAccessor.create(InfiniteTerrain.MIN_Y,InfiniteTerrain.HEIGHT),h.getLevel().registryAccess().registryOrThrow(Registries.BIOME),null);
        chunk.setStatus(ChunkStatus.BIOMES);return chunk;
    }
    private static InfiniteChunkTerrain.Palette palette(){
        var rows=new EnumMap<InfiniteTerrain.Biome,InfiniteChunkTerrain.Strata>(InfiniteTerrain.Biome.class);
        for(var biome:InfiniteTerrain.Biome.values())rows.put(biome,new InfiniteChunkTerrain.Strata(Blocks.GRASS_BLOCK.defaultBlockState(),Blocks.DIRT.defaultBlockState(),Blocks.STONE.defaultBlockState()));
        return new InfiniteChunkTerrain.Palette(rows,Blocks.WATER.defaultBlockState(),Blocks.BEDROCK.defaultBlockState());
    }
    @GameTest(template="empty",timeoutTicks=200) public static void chunkOrderRetainsDensityAndHeightmaps(GameTestHelper h){
        var generation=new InfiniteGeneration(UUID.randomUUID(),124119);var writer=new InfiniteChunkTerrain(generation,palette());
        var left=chunk(h,-1,0);var right=chunk(h,0,0);var replayRight=chunk(h,0,0);var replayLeft=chunk(h,-1,0);
        h.assertTrue(writer.fill(left)==InfiniteChunkTerrain.Result.FILLED&&writer.fill(right)==InfiniteChunkTerrain.Result.FILLED&&writer.fill(replayRight)==InfiniteChunkTerrain.Result.FILLED&&writer.fill(replayLeft)==InfiniteChunkTerrain.Result.FILLED,"Fresh protochunks could not generate");
        for(var pair:List.of(new ProtoChunk[]{left,replayLeft},new ProtoChunk[]{right,replayRight})){
            var first=pair[0];var replay=pair[1];var pos=new BlockPos.MutableBlockPos();
            for(int localX=0;localX<16;localX++)for(int localZ=0;localZ<16;localZ++){
                int x=first.getPos().getMinBlockX()+localX,z=first.getPos().getMinBlockZ()+localZ;var column=generation.terrain().column(x,z);int surface=Integer.MIN_VALUE;
                for(int y=InfiniteTerrain.MIN_Y;y<InfiniteTerrain.MIN_Y+InfiniteTerrain.HEIGHT;y++){
                    pos.set(x,y,z);var block=first.getBlockState(pos);h.assertTrue(block.equals(replay.getBlockState(pos)),"Visitation order changed an actual generated voxel");
                    if(!block.isAir())surface=y;
                    if(y<InfiniteTerrain.MIN_Y+5)h.assertTrue(block.is(Blocks.BEDROCK),"Bottom foundation was carved away");
                    if(y>Math.max(column.surfaceY(),InfiniteTerrain.SEA_LEVEL))h.assertTrue(block.isAir(),"Terrain placed blocks above its sampled surface/water");
                    if(y>24&&y<=column.surfaceY()&&generation.terrain().cave(x,y,z,column))h.assertTrue(block.isAir(),"Cave field did not create actual empty voxels");
                }
                h.assertTrue(first.getHeight(Heightmap.Types.WORLD_SURFACE_WG,localX,localZ)==surface,"Surface heightmap disagrees with real generated blocks");
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void terrainRefusesExistingBlocksAndGeneratedChunks(GameTestHelper h){
        var writer=new InfiniteChunkTerrain(new InfiniteGeneration(UUID.randomUUID(),124119),palette());var existing=chunk(h,5,5);var at=new BlockPos(80,70,80);
        existing.setBlockState(at,Blocks.DIAMOND_BLOCK.defaultBlockState(),false);
        h.assertTrue(writer.fill(existing)==InfiniteChunkTerrain.Result.NONEMPTY&&existing.getBlockState(at).is(Blocks.DIAMOND_BLOCK),"Writer replaced existing blocks in an incomplete chunk");
        var generated=chunk(h,6,5);generated.setStatus(ChunkStatus.NOISE);
        h.assertTrue(writer.fill(generated)==InfiniteChunkTerrain.Result.ALREADY_GENERATED&&generated.getSections()[0].hasOnlyAir(),"Writer regenerated an already completed noise stage");
        boolean rejected=false;try{new InfiniteChunkTerrain.Strata(Blocks.CHEST.defaultBlockState(),Blocks.DIRT.defaultBlockState(),Blocks.STONE.defaultBlockState());}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,"Terrain strata accepted a functional block entity");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void generatorCodecRetainsRosterAndRequiresTheSameGeneration(GameTestHelper h){
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);var holders=registry.holders().limit(InfiniteTerrain.Biome.values().length).toList();
        var rows=new HashMap<String,net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>();int index=0;
        for(var biome:InfiniteTerrain.Biome.values())rows.put(biome.name().toLowerCase(Locale.ROOT),holders.get(index++));
        var source=new InfiniteBiomeSource(rows);var generator=new InfiniteChunkGenerator(source,palette());boolean refused=false;
        try{generator.getBaseHeight(-1,0,Heightmap.Types.WORLD_SURFACE_WG,null,null);}catch(IllegalStateException expected){refused=true;}
        h.assertTrue(refused,"Unbound generator silently used an Overworld/default seed");
        var binding=new InfiniteGeneration(UUID.randomUUID(),124119);h.assertTrue(generator.bind(binding),"Generator could not bind its one generation");
        h.assertFalse(generator.bind(new InfiniteGeneration(UUID.randomUUID(),124119)),"Generator accepted another epoch");
        h.assertFalse(generator.bind(new InfiniteGeneration(binding.id(),124120)),"Generator changed seed within an epoch");
        var chunk=chunk(h,-1,0);generator.createBiomes(Runnable::run,null,null,null,chunk).join();generator.fillFromNoise(Runnable::run,null,null,null,chunk).join();
        for(int x=-16;x<0;x+=3)for(int z=0;z<16;z+=3){
            h.assertTrue(generator.getBaseHeight(x,z,Heightmap.Types.WORLD_SURFACE_WG,null,null)==chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x&15,z&15)+1,"Generator height query differs from real terrain");
            var column=generator.getBaseColumn(x,z,null,null);for(int y:new int[]{-64,-50,0,24,64,120,319})h.assertTrue(column.getBlock(y).equals(chunk.getBlockState(new BlockPos(x,y,z))),"Base column differs from actual noise-stage blocks");
        }
        var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,h.getLevel().registryAccess());
        var encoded=InfiniteChunkGenerator.CODEC.encodeStart(ops,generator).getOrThrow(false,message->h.fail(message));
        var restored=InfiniteChunkGenerator.CODEC.parse(ops,encoded).getOrThrow(false,message->h.fail(message));h.assertTrue(restored.bind(binding),"Codec roundtrip lost the generator configuration");
        h.assertTrue(restored.getBaseColumn(-1,0,null,null).getBlock(0).equals(generator.getBaseColumn(-1,0,null,null).getBlock(0)),"Reloaded palette changed a terrain block");
        var damaged=(net.minecraft.nbt.CompoundTag)encoded.copy();damaged.getCompound("palette").getCompound("strata").remove("stitched_meadows");
        h.assertTrue(InfiniteChunkGenerator.CODEC.parse(ops,damaged).error().isPresent(),"Incomplete palette silently fell back to vanilla terrain");
        damaged=(net.minecraft.nbt.CompoundTag)encoded.copy();damaged.getCompound("biome_source").getCompound("biomes").remove("stitched_meadows");
        h.assertTrue(InfiniteChunkGenerator.CODEC.parse(ops,damaged).error().isPresent(),"Missing biome profile silently duplicated another biome");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200) public static void naturalVeinsUseExistingOreBlocksToolTiersAndDrops(GameTestHelper h){
        var binding=new InfiniteGeneration(UUID.randomUUID(),124119);var veins=new InfiniteOreVeins(binding.seed());var writer=new InfiniteChunkTerrain(binding,palette());
        for(var ore:List.of(InfiniteOreVeins.Ore.OTHER_IRON,InfiniteOreVeins.Ore.OTHER_DIAMOND,InfiniteOreVeins.Ore.SOUL_GLASS)){
            InfiniteOreVeins.Point found=null;
            search:for(int cellX=-6;cellX<=6;cellX++)for(int cellZ=-6;cellZ<=6;cellZ++)for(int cellY=Math.floorDiv(ore.minY,ore.vertical);cellY<=Math.floorDiv(ore.maxY,ore.vertical);cellY++){
                var candidate=veins.candidate(ore,cellX,cellY,cellZ,InfiniteTerrain.Biome.STITCHED_MEADOWS);if(candidate==null)continue;var column=binding.terrain().column(candidate.x(),candidate.z());
                if(candidate.y()>=column.surfaceY()-3||binding.terrain().cave(candidate.x(),candidate.y(),candidate.z(),column)||veins.sample(candidate.x(),candidate.y(),candidate.z(),column.biome())!=ore)continue;
                found=candidate;break search;
            }
            h.assertTrue(found!=null,"No actual rock candidate for "+ore);var target=new BlockPos(found.x(),found.y(),found.z());var chunk=chunk(h,Math.floorDiv(found.x(),16),Math.floorDiv(found.z(),16));
            h.assertTrue(writer.fill(chunk)==InfiniteChunkTerrain.Result.FILLED,"Natural ore protochunk failed generation");
            var expected=switch(ore){case OTHER_IRON->com.coraline.registry.ModBlocks.OTHER_IRON_ORE.get();case OTHER_DIAMOND->com.coraline.registry.ModBlocks.OTHER_DIAMOND_ORE.get();case SOUL_GLASS->com.coraline.registry.ModBlocks.SOULGLASS_ORE.get();default->throw new IllegalStateException();};
            var state=chunk.getBlockState(target);h.assertTrue(state.is(expected),"Natural vein produced another block for "+ore);
            var tool=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);h.assertTrue(tool.isCorrectToolForDrops(state),"Existing ore has no valid registered mining tier: "+ore);
            var drops=net.minecraft.world.level.block.Block.getDrops(state,h.getLevel(),target,null,null,tool);
            var item=switch(ore){case OTHER_IRON->com.coraline.clothing.OtherMaterials.OTHER_IRON.get();case OTHER_DIAMOND->com.coraline.clothing.OtherMaterials.OTHER_DIAMOND.get();case SOUL_GLASS->com.coraline.registry.ModItems.SOULGLASS.get();default->throw new IllegalStateException();};
            h.assertTrue(drops.stream().anyMatch(stack->stack.is(item)&&stack.getCount()>0),"Natural ore lost its actual processing/crafting material: "+ore);
        }
        h.succeed();
    }
    private InfiniteChunkTerrainGameTests(){}
}
