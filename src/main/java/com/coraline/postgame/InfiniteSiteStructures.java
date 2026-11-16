package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

/** Native bounded structure pieces and codecs, not tick-time world editing. */
public final class InfiniteSiteStructures {
    private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"coraline");
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,"coraline");
    private static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>> PLACEMENTS=DeferredRegister.create(Registries.STRUCTURE_PLACEMENT,"coraline");
    static final RegistryObject<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<IndependentStructureSpread>> SPREAD=PLACEMENTS.register("independent_spread",()->()->IndependentStructureSpread.CODEC);
    static final RegistryObject<StructurePieceType> MODULE=PIECES.register("infinite_site_module",()->(context,tag)->new InfiniteSitePiece(tag));
    private static final Map<InfiniteStructurePlan.Family,RegistryObject<StructureType<InfiniteSiteStructure>>> FAMILIES=new EnumMap<>(InfiniteStructurePlan.Family.class);
    static{
        for(var family:InfiniteStructurePlan.Family.values())FAMILIES.put(family,TYPES.register(family.id(),()->()->Structure.simpleCodec(settings->new InfiniteSiteStructure(settings,family))));
    }
    static StructureType<InfiniteSiteStructure> type(InfiniteStructurePlan.Family family){return FAMILIES.get(family).get();}
    public static void register(IEventBus bus){TYPES.register(bus);PIECES.register(bus);PLACEMENTS.register(bus);}
    private InfiniteSiteStructures(){}
}
