package com.coraline.postgame;

import java.util.*;
import java.util.stream.Stream;
import com.mojang.serialization.*;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;

/** Climate regions resolve to actual registered biome holders. A complete
 * distinct roster is required; there is no default Overworld biome fallback. */
public final class InfiniteBiomeSource extends BiomeSource {
    public static final Codec<InfiniteBiomeSource> CODEC=Codec.unboundedMap(Codec.STRING,Biome.CODEC).fieldOf("biomes").codec().comapFlatMap(rows->{
        try{return DataResult.success(new InfiniteBiomeSource(rows));}catch(IllegalArgumentException|NullPointerException invalid){return DataResult.error(()->invalid.getMessage()==null?"Invalid Infinite Other World biome roster":invalid.getMessage());}
    },InfiniteBiomeSource::encoded);
    private final Map<InfiniteTerrain.Biome,Holder<Biome>> biomes;
    private final Map<InfiniteTerrain.Cave,Holder<Biome>> caves;
    private volatile InfiniteGeneration generation;
    public InfiniteBiomeSource(Map<String,Holder<Biome>> rows){
        var mapped=new EnumMap<InfiniteTerrain.Biome,Holder<Biome>>(InfiniteTerrain.Biome.class);
        var underground=new EnumMap<InfiniteTerrain.Cave,Holder<Biome>>(InfiniteTerrain.Cave.class);
        for(var entry:rows.entrySet()){
            if(entry.getKey().startsWith("cave_")){var cave=InfiniteTerrain.Cave.valueOf(entry.getKey().substring(5).toUpperCase(Locale.ROOT));if(!entry.getKey().equals("cave_"+cave.name().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Noncanonical cave profile");underground.put(cave,Objects.requireNonNull(entry.getValue()));continue;}
            var biome=InfiniteTerrain.Biome.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            if(!entry.getKey().equals(biome.name().toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Noncanonical biome profile: "+entry.getKey());mapped.put(biome,Objects.requireNonNull(entry.getValue()));
        }
        if(mapped.size()!=InfiniteTerrain.Biome.values().length||mapped.values().stream().distinct().count()!=mapped.size())throw new IllegalArgumentException("Sixteen distinct registered postgame biomes are required");
        // Older saved generator maps keep their sixteen original holders.
        // Newly opened generations supply all six actual underground biomes.
        if(!underground.isEmpty()&&(underground.size()!=InfiniteTerrain.Cave.values().length||underground.values().stream().distinct().count()!=underground.size()||underground.values().stream().anyMatch(mapped::containsValue)))throw new IllegalArgumentException("Incomplete cave biome roster");
        biomes=Map.copyOf(mapped);caves=Map.copyOf(underground);
    }
    private Map<String,Holder<Biome>> encoded(){var out=new TreeMap<String,Holder<Biome>>();biomes.forEach((key,value)->out.put(key.name().toLowerCase(Locale.ROOT),value));caves.forEach((key,value)->out.put("cave_"+key.name().toLowerCase(Locale.ROOT),value));return out;}
    synchronized boolean bind(InfiniteGeneration binding){
        if(generation!=null)return generation.id().equals(binding.id())&&generation.seed()==binding.seed();generation=Objects.requireNonNull(binding);return true;
    }
    @Override protected Codec<? extends BiomeSource> codec(){return CODEC;}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.concat(biomes.values().stream(),caves.values().stream());}
    @Override public Holder<Biome> getNoiseBiome(int quartX,int quartY,int quartZ,Climate.Sampler unused){
        var binding=generation;if(binding==null)throw new IllegalStateException("Postgame generation has no durable seed binding");
        var terrain=binding.terrain();var column=terrain.column(quartX*4,quartZ*4);if(!caves.isEmpty()&&quartY*4<column.surfaceY()-8)return caves.get(terrain.caveEnvironment(quartX*4,quartY*4,quartZ*4,column));return biomes.get(column.biome());
    }
}
