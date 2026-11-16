package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.*;

/** Structure eligibility and pieces use the durably bound generation. */
public final class InfiniteSiteStructure extends Structure {
    private final InfiniteStructurePlan.Family family;
    InfiniteSiteStructure(StructureSettings settings,InfiniteStructurePlan.Family family){super(settings);this.family=Objects.requireNonNull(family);}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context){
        if(!(context.chunkGenerator() instanceof InfiniteChunkGenerator generator))return Optional.empty();
        var generation=generator.structureGeneration();
        if(context.seed()!=generation.seed())return Optional.empty();
        var chunk=context.chunkPos();
        return InfiniteStructurePlan.at(generation.id(),generation.seed(),family,chunk.getMiddleBlockX(),chunk.getMiddleBlockZ())
            .map(plan->new GenerationStub(new BlockPos(plan.x(),plan.y(),plan.z()),builder->{
                for(int i=0;i<plan.modules().size();i++)builder.addPiece(new InfiniteSitePiece(plan,i));
            }));
    }
    @Override public StructureType<?> type(){return InfiniteSiteStructures.type(family);}
}
