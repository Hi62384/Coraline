package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Heightmap;

/** Noise-stage block writer for the future registered chunk generator. Writes
 * only an empty protochunk; never rewrites a live/previously generated chunk.
 * Registered postgame palettes, biomes, ores and decoration remain required. */
public final class InfiniteChunkTerrain {
    public record Strata(BlockState surface,BlockState subsoil,BlockState rock){
        public Strata{solid(surface);solid(subsoil);solid(rock);}
    }
    public static final class Palette {
        private final Map<InfiniteTerrain.Biome,Strata> strata;
        private final BlockState water,foundation;
        public Palette(Map<InfiniteTerrain.Biome,Strata> strata,BlockState water,BlockState foundation){
            var copy=new EnumMap<InfiniteTerrain.Biome,Strata>(InfiniteTerrain.Biome.class);copy.putAll(strata);
            for(var biome:InfiniteTerrain.Biome.values())Objects.requireNonNull(copy.get(biome),"Missing strata for "+biome);
            if(copy.size()!=InfiniteTerrain.Biome.values().length)throw new IllegalArgumentException("Incomplete biome palette");
            this.strata=Map.copyOf(copy);this.water=Objects.requireNonNull(water);solid(foundation);this.foundation=foundation;
            if(water.hasBlockEntity()||water.getFluidState().isEmpty())throw new IllegalArgumentException("Water palette requires a real fluid block");
        }
        public Strata strata(InfiniteTerrain.Biome biome){return strata.get(biome);}
        BlockState waterState(){return water;}
        BlockState foundationState(){return foundation;}
    }
    public enum Result { FILLED, ALREADY_GENERATED, NONEMPTY, HEIGHT_MISMATCH }
    private final InfiniteGeneration generation;
    private final Palette palette;
    private final InfiniteOreVeins ores;
    public InfiniteChunkTerrain(InfiniteGeneration generation,Palette palette){this.generation=Objects.requireNonNull(generation);this.palette=Objects.requireNonNull(palette);this.ores=new InfiniteOreVeins(generation.seed());}
    private static void solid(BlockState state){if(state==null||state.isAir()||state.hasBlockEntity()||!state.getFluidState().isEmpty())throw new IllegalArgumentException("Terrain strata cannot contain air, fluid or a block entity");}
    BlockState blockAt(int x,int y,int z,InfiniteTerrain.Column column){
        if(y<InfiniteTerrain.MIN_Y||y>=InfiniteTerrain.MIN_Y+InfiniteTerrain.HEIGHT)return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if(y<InfiniteTerrain.MIN_Y+5)return palette.foundation;
        if(y>column.surfaceY())return y<=InfiniteTerrain.SEA_LEVEL?palette.water:net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        if(generation.terrain().cave(x,y,z,column)){var formation=InfiniteCaveEnvironments.formation(generation.terrain(),x,y,z,column);return formation!=null?formation:InfiniteCaveEnvironments.flooded(generation.terrain(),x,y,z,column)?palette.water:net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();}
        var strata=palette.strata(column.biome());if(y==column.surfaceY())return strata.surface();if(y>=column.surfaceY()-3)return strata.subsoil();
        return switch(ores.sample(x,y,z,column.biome())){
            case OTHER_IRON->com.coraline.registry.ModBlocks.OTHER_IRON_ORE.get().defaultBlockState();
            case OTHER_DIAMOND->com.coraline.registry.ModBlocks.OTHER_DIAMOND_ORE.get().defaultBlockState();
            case MOONSTONE->com.coraline.registry.ModBlocks.MOONSTONE_ORE.get().defaultBlockState();
            case PERFECT->com.coraline.registry.ModBlocks.PERFECT_ORE.get().defaultBlockState();
            case SOUL_GLASS->com.coraline.registry.ModBlocks.SOULGLASS_ORE.get().defaultBlockState();
            case OTHER_COAL->InfiniteContent.block("other_coal_ore").defaultBlockState();
            case OTHER_COPPER->InfiniteContent.block("other_copper_ore").defaultBlockState();
            case OTHER_GOLD->InfiniteContent.block("other_gold_ore").defaultBlockState();
            case OTHER_REDSTONE->InfiniteContent.block("other_redstone_ore").defaultBlockState();
            case OTHER_LAPIS->InfiniteContent.block("other_lapis_ore").defaultBlockState();
            case OTHER_EMERALD->InfiniteContent.block("other_emerald_ore").defaultBlockState();
            case OTHER_QUARTZ->InfiniteContent.block("other_quartz_ore").defaultBlockState();
            default->InfiniteCaveEnvironments.stone(generation.terrain(),x,y,z,column,strata.rock());
        };
    }
    /** Called once by the generator's exclusive noise-stage task. Chunk
     * coordinates never seed another RNG or alter the shared terrain field. */
    public Result fill(ProtoChunk chunk){
        if(chunk.getStatus().isOrAfter(ChunkStatus.NOISE))return Result.ALREADY_GENERATED;
        if(chunk.getMinBuildHeight()!=InfiniteTerrain.MIN_Y||chunk.getHeight()!=InfiniteTerrain.HEIGHT)return Result.HEIGHT_MISMATCH;
        for(var section:chunk.getSections())if(!section.hasOnlyAir())return Result.NONEMPTY;
        var terrain=generation.terrain();var pos=new BlockPos.MutableBlockPos();var origin=chunk.getPos();
        for(int localX=0;localX<16;localX++)for(int localZ=0;localZ<16;localZ++){
            int x=origin.getMinBlockX()+localX,z=origin.getMinBlockZ()+localZ;var column=terrain.column(x,z);
            for(int y=InfiniteTerrain.MIN_Y;y<=Math.max(column.surfaceY(),InfiniteTerrain.SEA_LEVEL);y++){
                var block=blockAt(x,y,z,column);if(block.isAir())continue;
                chunk.setBlockState(pos.set(x,y,z),block,false);
            }
        }
        Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG,Heightmap.Types.OCEAN_FLOOR_WG));chunk.setUnsaved(true);
        return Result.FILLED;
    }
}
