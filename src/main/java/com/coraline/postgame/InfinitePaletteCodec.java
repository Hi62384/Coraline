package com.coraline.postgame;

import java.util.*;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;

/** Explicit datapack palette, never silently substituted vanilla terrain. */
final class InfinitePaletteCodec {
    private record Layer(BlockState surface,BlockState subsoil,BlockState rock){}
    private record Data(Map<String,InfiniteChunkTerrain.Strata> strata,BlockState water,BlockState foundation){}
    private static final Codec<Layer> LAYER=RecordCodecBuilder.create(i->i.group(BlockState.CODEC.fieldOf("surface").forGetter(Layer::surface),BlockState.CODEC.fieldOf("subsoil").forGetter(Layer::subsoil),BlockState.CODEC.fieldOf("rock").forGetter(Layer::rock)).apply(i,Layer::new));
    private static final Codec<InfiniteChunkTerrain.Strata> STRATA=LAYER.comapFlatMap(row->{
        try{return DataResult.success(new InfiniteChunkTerrain.Strata(row.surface(),row.subsoil(),row.rock()));}catch(IllegalArgumentException invalid){return DataResult.error(invalid::getMessage);}
    },row->new Layer(row.surface(),row.subsoil(),row.rock()));
    private static final Codec<Data> DATA=RecordCodecBuilder.create(i->i.group(Codec.unboundedMap(Codec.STRING,STRATA).fieldOf("strata").forGetter(Data::strata),BlockState.CODEC.fieldOf("water").forGetter(Data::water),BlockState.CODEC.fieldOf("foundation").forGetter(Data::foundation)).apply(i,Data::new));
    static final Codec<InfiniteChunkTerrain.Palette> CODEC=DATA.comapFlatMap(row->{
        try{
            var mapped=new EnumMap<InfiniteTerrain.Biome,InfiniteChunkTerrain.Strata>(InfiniteTerrain.Biome.class);
            for(var entry:row.strata().entrySet()){var biome=InfiniteTerrain.Biome.valueOf(entry.getKey().toUpperCase(Locale.ROOT));if(!entry.getKey().equals(biome.name().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Noncanonical palette profile");mapped.put(biome,entry.getValue());}
            return DataResult.success(new InfiniteChunkTerrain.Palette(mapped,row.water(),row.foundation()));
        }catch(IllegalArgumentException|NullPointerException invalid){return DataResult.error(()->invalid.getMessage()==null?"Incomplete terrain palette":invalid.getMessage());}
    },palette->{var rows=new TreeMap<String,InfiniteChunkTerrain.Strata>();for(var biome:InfiniteTerrain.Biome.values())rows.put(biome.name().toLowerCase(Locale.ROOT),palette.strata(biome));return new Data(rows,palette.waterState(),palette.foundationState());});
    private InfinitePaletteCodec(){}
}
