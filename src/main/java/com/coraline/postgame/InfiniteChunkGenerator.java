package com.coraline.postgame;

import java.util.*;
import java.util.concurrent.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.block.state.BlockState;

/** Actual chunk-generation adapter. No dimension JSON or entrance is supplied
 * until the required custom content is ready. Vanilla Overworld generation,
 * structure placement, features and seeds are deliberately not inherited. */
public final class InfiniteChunkGenerator extends ChunkGenerator {
    public static final Codec<InfiniteChunkGenerator> CODEC=RecordCodecBuilder.create(i->i.group(InfiniteBiomeSource.CODEC.fieldOf("biome_source").forGetter(g->g.source),InfinitePaletteCodec.CODEC.fieldOf("palette").forGetter(g->g.palette)).apply(i,InfiniteChunkGenerator::new));
    private final InfiniteBiomeSource source;
    private final InfiniteChunkTerrain.Palette palette;
    private volatile InfiniteGeneration generation;
    private volatile InfiniteChunkTerrain writer;
    private volatile ChunkGeneratorStructureState independentStructures;
    private ChunkGeneratorStructureState mapStructures;
    private net.minecraft.world.level.levelgen.structure.StructureSet ownedStructureSet;
    private IndependentStructureSpread ownedPlacement;
    public InfiniteChunkGenerator(InfiniteBiomeSource source,InfiniteChunkTerrain.Palette palette){super(source);this.source=source;this.palette=palette;}
    public synchronized boolean bind(MinecraftServer server){
        var checkpoint=InfiniteGeneration.checkpoint(server);return checkpoint.isPresent()&&bind(checkpoint.orElseThrow());
    }
    synchronized boolean bind(InfiniteGeneration binding){
        if(generation!=null)return generation.id().equals(binding.id())&&generation.seed()==binding.seed();
        if(!source.bind(binding)||ownedPlacement!=null&&!ownedPlacement.bind(binding))return false;writer=new InfiniteChunkTerrain(binding,palette);generation=binding;return true;
    }
    private InfiniteChunkTerrain writer(){var bound=writer;if(bound==null)throw new IllegalStateException("Infinite terrain requested before durable seed activation");return bound;}
    private InfiniteGeneration generation(){var bound=generation;if(bound==null)throw new IllegalStateException("Infinite biome requested before durable seed activation");return bound;}
    InfiniteGeneration structureGeneration(){return generation();}
    private static net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.StructureSet> sitesKey(){return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE_SET,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline","infinite_sites"));}
    private void ownStructureSet(net.minecraft.world.level.levelgen.structure.StructureSet definition){
        if(ownedStructureSet!=null)return;
        if(!(definition.placement() instanceof IndependentStructureSpread prototype))throw new IllegalStateException("Infinite structure set lacks independent placement");
        // Registry data is shared across dimensions. Never bind its prototype:
        // an old generator and its replacement must retain separate lifetimes.
        ownedPlacement=prototype.copyUnbound();ownedStructureSet=new net.minecraft.world.level.levelgen.structure.StructureSet(definition.structures(),ownedPlacement);
        if(generation!=null&&!ownedPlacement.bind(generation))throw new IllegalStateException("Infinite placement seed binding failed");
    }
    @Override public synchronized ChunkGeneratorStructureState createState(net.minecraft.core.HolderLookup<net.minecraft.world.level.levelgen.structure.StructureSet> lookup,RandomState random,long ignoredOverworldSeed){
        ownStructureSet(lookup.getOrThrow(sitesKey()).value());
        if(mapStructures==null)mapStructures=ChunkGeneratorStructureState.createForFlat(random,0,source,java.util.stream.Stream.of(net.minecraft.core.Holder.direct(ownedStructureSet)));
        else if(mapStructures.randomState()!=random)throw new IllegalStateException("Infinite map random-state binding changed");
        // Map queries use the owned placement's bound seed. No canonical seed
        // is guessed before durable activation. This set has no vanilla rings,
        // probability reduction or exclusion rules which could consult zero.
        return mapStructures;
    }
    private synchronized ChunkGeneratorStructureState independentStructures(RegistryAccess registries,ChunkGeneratorStructureState supplied){
        if(independentStructures==null){
            ownStructureSet(registries.registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).getHolderOrThrow(sitesKey()).value());
            if(!ownedPlacement.bind(generation()))throw new IllegalStateException("Infinite placement belongs to another generation");
            independentStructures=ChunkGeneratorStructureState.createForFlat(supplied.randomState(),generation().seed(),source,java.util.stream.Stream.of(net.minecraft.core.Holder.direct(ownedStructureSet)));
        }else if(independentStructures.randomState()!=supplied.randomState())throw new IllegalStateException("Infinite structure random-state binding changed");
        return independentStructures;
    }
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public CompletableFuture<ChunkAccess> createBiomes(Executor executor,RandomState random,Blender blender,StructureManager structures,ChunkAccess chunk){
        generation();return CompletableFuture.supplyAsync(()->{
            // Climate is two dimensional. Cache only this chunk's quart
            // columns instead of recomputing it at every biome height.
            var columns=new HashMap<Long,net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>();
            chunk.fillBiomesFromNoise((x,y,z,unused)->columns.computeIfAbsent(((long)x<<32)^Integer.toUnsignedLong(z),key->source.getNoiseBiome(x,y,z,null)),null);return chunk;
        },executor);
    }
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk){
        var bound=writer();return CompletableFuture.supplyAsync(()->{
            if(!(chunk instanceof ProtoChunk proto))throw new IllegalStateException("Infinite generation refuses a live chunk");
            var result=bound.fill(proto);if(result!=InfiniteChunkTerrain.Result.FILLED&&result!=InfiniteChunkTerrain.Result.ALREADY_GENERATED)throw new IllegalStateException("Infinite terrain refused chunk: "+result);return chunk;
        },executor);
    }
    @Override public int getGenDepth(){return InfiniteTerrain.HEIGHT;}
    @Override public int getSeaLevel(){return InfiniteTerrain.SEA_LEVEL;}
    @Override public int getMinY(){return InfiniteTerrain.MIN_Y;}
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor height,RandomState random){
        var column=generation().terrain().column(x,z);var bound=writer();
        for(int y=Math.max(column.surfaceY(),InfiniteTerrain.SEA_LEVEL);y>=getMinY();y--)if(type.isOpaque().test(bound.blockAt(x,y,z,column)))return y+1;return getMinY();
    }
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor height,RandomState random){
        var column=generation().terrain().column(x,z);var bound=writer();var blocks=new BlockState[getGenDepth()];
        for(int index=0;index<blocks.length;index++)blocks[index]=bound.blockAt(x,getMinY()+index,z,column);return new NoiseColumn(getMinY(),blocks);
    }
    @Override public void addDebugScreenInfo(List<String> lines,RandomState random,BlockPos at){
        var bound=generation;lines.add(bound==null?"Infinite terrain: awaiting canonical seed checkpoint":"Infinite terrain generation: "+bound.id()+" / algorithm "+InfiniteTerrain.VERSION);
    }
    // Density generation already contains its surface and cave pass. Distinct
    // custom ores, decoration, structures and spawns are subsequent adapters;
    // do not accidentally run their vanilla Overworld equivalents meanwhile.
    @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState random,ChunkAccess chunk){}
    @Override public void applyCarvers(WorldGenRegion region,long ignoredOverworldSeed,RandomState random,BiomeManager biomes,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step){}
    @Override public void applyBiomeDecoration(WorldGenLevel level,ChunkAccess chunk,StructureManager structures){
        // The authored biome feature lists are empty. Native decoration places
        // the saved custom structure pieces with normal per-chunk clipping;
        // our terrain vegetation remains the separate bounded custom pass.
        generation();super.applyBiomeDecoration(level,chunk,structures);
        var sites=structures.startsForStructure(chunk.getPos(),structure->structure instanceof InfiniteSiteStructure).stream()
            .filter(net.minecraft.world.level.levelgen.structure.StructureStart::isValid)
            .map(net.minecraft.world.level.levelgen.structure.StructureStart::getBoundingBox).map(box->box.inflatedBy(2)).toList();
        InfiniteVegetation.decorate(chunk,generation(),palette,sites);
    }
    @Override public void spawnOriginalMobs(WorldGenRegion region){}
    @Override public void createStructures(RegistryAccess registries,ChunkGeneratorStructureState state,StructureManager structures,ChunkAccess chunk,StructureTemplateManager templates){super.createStructures(registries,independentStructures(registries,state),structures,chunk,templates);}
    @Override public void createReferences(WorldGenLevel level,StructureManager structures,ChunkAccess chunk){generation();super.createReferences(level,structures,chunk);}
}
