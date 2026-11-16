package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.gametest.*;

/** Disposable feature-stage chunks. Must run with fresh postgame registrations. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class InfiniteVegetationGameTests {
    private static InfiniteChunkTerrain.Palette palette() {
        var rows=new EnumMap<InfiniteTerrain.Biome,InfiniteChunkTerrain.Strata>(InfiniteTerrain.Biome.class);
        for(var biome:InfiniteTerrain.Biome.values()) rows.put(biome,new InfiniteChunkTerrain.Strata(InfiniteContent.block("stitched_grass").defaultBlockState(),InfiniteContent.block("other_soil").defaultBlockState(),InfiniteContent.block("threadbound_rock").defaultBlockState()));
        return new InfiniteChunkTerrain.Palette(rows,Blocks.WATER.defaultBlockState(),Blocks.BEDROCK.defaultBlockState());
    }
    private static ProtoChunk chunk(GameTestHelper h,int x,int z,InfiniteGeneration generation,InfiniteChunkTerrain.Palette palette) {
        var chunk=new ProtoChunk(new ChunkPos(x,z),UpgradeData.EMPTY,LevelHeightAccessor.create(InfiniteTerrain.MIN_Y,InfiniteTerrain.HEIGHT),h.getLevel().registryAccess().registryOrThrow(Registries.BIOME),null);
        chunk.setStatus(ChunkStatus.BIOMES);
        h.assertTrue(new InfiniteChunkTerrain(generation,palette).fill(chunk)==InfiniteChunkTerrain.Result.FILLED,"Feature fixture terrain did not generate");
        chunk.setStatus(ChunkStatus.NOISE); return chunk;
    }
    private static InfiniteVegetationPlan.Plant crossingPlant() {
        var plan=new InfiniteVegetationPlan(124119);
        for(int x=-64;x<=64;x+=7)for(int z=-64;z<=64;z+=7)for(var plant:plan.forChunk(x,z))
            if((plant.x()&15)+plant.radius()>15)return plant;
        throw new AssertionError("No natural cross-border plant");
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void treeDecorationIsStableAcrossNeighborGenerationOrder(GameTestHelper h) {
        var generation=new InfiniteGeneration(UUID.randomUUID(),124119);var palette=palette();var p=crossingPlant();
        int x=Math.floorDiv(p.x(),16),z=Math.floorDiv(p.z(),16);
        var left=chunk(h,x,z,generation,palette);var right=chunk(h,x+1,z,generation,palette);
        var replayLeft=chunk(h,x,z,generation,palette);var replayRight=chunk(h,x+1,z,generation,palette);
        int changed=InfiniteVegetation.decorate(left,generation,palette)+InfiniteVegetation.decorate(right,generation,palette);
        InfiniteVegetation.decorate(replayRight,generation,palette);InfiniteVegetation.decorate(replayLeft,generation,palette);
        h.assertTrue(changed>0,"Natural materials never produced actual trees/mushrooms");
        var at=new BlockPos.MutableBlockPos();int foliage=0;
        for(var pair:List.of(new ProtoChunk[]{left,replayLeft},new ProtoChunk[]{right,replayRight}))
            for(int lx=0;lx<16;lx++)for(int lz=0;lz<16;lz++)for(int y=InfiniteTerrain.MIN_Y;y<InfiniteTerrain.MIN_Y+InfiniteTerrain.HEIGHT;y++) {
                at.set(pair[0].getPos().getMinBlockX()+lx,y,pair[0].getPos().getMinBlockZ()+lz);
                var state=pair[0].getBlockState(at);
                h.assertTrue(state.equals(pair[1].getBlockState(at)),"Decoration order changed a real voxel across a chunk edge");
                if(state.getBlock() instanceof LeavesBlock) {
                    foliage++;
                    h.assertFalse(state.getValue(LeavesBlock.PERSISTENT),"Natural foliage was made permanently nondecaying");
                    h.assertTrue(state.getValue(LeavesBlock.DISTANCE)<7,"Generated foliage has no supported log distance");
                }
            }
        h.assertTrue(foliage>0||p.kind()==InfiniteVegetationPlan.Kind.VELVET_MUSHROOM,"No custom foliage generated");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void decorationPreservesExistingBlocksAndRejectsCompletedFeatures(GameTestHelper h) {
        var generation=new InfiniteGeneration(UUID.randomUUID(),124119);var palette=palette();var plant=crossingPlant();
        var chunk=chunk(h,Math.floorDiv(plant.x(),16),Math.floorDiv(plant.z(),16),generation,palette);
        var protectedAt=new BlockPos(plant.x(),plant.y(),plant.z());
        chunk.setBlockState(protectedAt,Blocks.DIAMOND_BLOCK.defaultBlockState(),false);
        InfiniteVegetation.decorate(chunk,generation,palette);
        h.assertTrue(chunk.getBlockState(protectedAt).is(Blocks.DIAMOND_BLOCK),"Decoration overwrote an existing functional/protected placement");
        chunk.setStatus(ChunkStatus.FEATURES);
        h.assertTrue(InfiniteVegetation.decorate(chunk,generation,palette)==0,"Completed feature stage was regenerated");
        var empty=new ProtoChunk(new ChunkPos(0,0),UpgradeData.EMPTY,LevelHeightAccessor.create(InfiniteTerrain.MIN_Y,InfiniteTerrain.HEIGHT),h.getLevel().registryAccess().registryOrThrow(Registries.BIOME),null);
        h.assertTrue(InfiniteVegetation.decorate(empty,generation,palette)==0,"Vegetation generated before terrain");
        h.succeed();
    }
    private InfiniteVegetationGameTests() {}
    @GameTest(template="empty",timeoutTicks=200)
    public static void structureRoomAirIsNotTreatedAsFreeFoliageSpace(GameTestHelper h){
        var generation=new InfiniteGeneration(UUID.randomUUID(),124119);var palette=palette();var plant=crossingPlant();
        int x=Math.floorDiv(plant.x(),16),z=Math.floorDiv(plant.z(),16);
        var baseline=chunk(h,x,z,generation,palette);var guarded=chunk(h,x,z,generation,palette);
        var column=new net.minecraft.world.level.levelgen.structure.BoundingBox(plant.x(),plant.y(),plant.z(),plant.x(),plant.y()+plant.height()+1,plant.z());
        int normal=InfiniteVegetation.decorate(baseline,generation,palette);
        int protectedCount=InfiniteVegetation.decorate(guarded,generation,palette,List.of(column));
        h.assertTrue(normal>protectedCount&&protectedCount>0,"Exclusion fixture did not distinguish room air from ordinary nearby foliage");
        for(int y=plant.y();y<plant.y()+plant.height();y++){
            var at=new BlockPos(plant.x(),y,plant.z());
            h.assertTrue(!baseline.getBlockState(at).isAir()&&guarded.getBlockState(at).isAir(),"Feature pass filled protected room/doorway air");
        }
        h.succeed();
    }
}
