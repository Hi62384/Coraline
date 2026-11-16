package com.coraline.postgame;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.*;
import java.util.UUID;

/** Keeps native random-spread placement and map lookup on the durably bound
 * independent seed. A registry placement is bound once per generation; neither
 * an Overworld seed nor a reset can silently retarget an existing placement. */
public final class IndependentStructureSpread extends RandomSpreadStructurePlacement {
    public static final Codec<IndependentStructureSpread> CODEC=RecordCodecBuilder.<IndependentStructureSpread>mapCodec(i->i.group(
        Codec.intRange(16,256).fieldOf("spacing").forGetter(IndependentStructureSpread::spacing),
        Codec.intRange(8,255).fieldOf("separation").forGetter(IndependentStructureSpread::separation),
        Codec.intRange(0,Integer.MAX_VALUE).fieldOf("salt").forGetter(IndependentStructureSpread::placementSalt)
    ).apply(i,IndependentStructureSpread::new)).flatXmap(IndependentStructureSpread::valid,IndependentStructureSpread::valid).codec();
    private record Binding(UUID generation,long seed){}
    private volatile Binding binding;
    private IndependentStructureSpread(int spacing,int separation,int salt){super(spacing,separation,RandomSpreadType.LINEAR,salt);}
    private int placementSalt(){return salt();}
    IndependentStructureSpread copyUnbound(){return new IndependentStructureSpread(spacing(),separation(),salt());}
    private static DataResult<IndependentStructureSpread> valid(IndependentStructureSpread p){return p.separation()<p.spacing()?DataResult.success(p):DataResult.error(()->"Independent structure separation must be smaller than spacing");}
    synchronized boolean bind(InfiniteGeneration generation){
        if(binding!=null)return binding.generation.equals(generation.id())&&binding.seed==generation.seed();
        binding=new Binding(generation.id(),generation.seed());return true;
    }
    @Override public ChunkPos getPotentialStructureChunk(long ignoredLevelSeed,int x,int z){
        var current=binding;if(current==null)throw new IllegalStateException("Structure placement requested before durable independent seed binding");
        return super.getPotentialStructureChunk(current.seed,x,z);
    }
    @Override protected boolean isPlacementChunk(ChunkGeneratorStructureState ignored,int x,int z){var at=getPotentialStructureChunk(0,x,z);return at.x==x&&at.z==z;}
    @Override public StructurePlacementType<?> type(){return InfiniteSiteStructures.SPREAD.get();}
}
