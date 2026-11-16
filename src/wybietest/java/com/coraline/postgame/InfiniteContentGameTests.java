package com.coraline.postgame;

import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraftforge.gametest.*;

/** Actual datapack biome/terrain decoding, distinct from static resource links. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class InfiniteContentGameTests {
    @GameTest(template="empty",timeoutTicks=200)
    public static void authoredPaletteResolvesActualBiomesAndStrata(GameTestHelper h) throws Exception {
        var access=h.getLevel().registryAccess();
        var registry=access.registryOrThrow(Registries.BIOME);
        var resource=h.getLevel().getServer().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("coraline","postgame/terrain.json")).orElseThrow();
        InfiniteChunkGenerator generator;
        try (var reader=resource.openAsReader()) {
            var decoded=ChunkGenerator.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,access),JsonParser.parseReader(reader)).getOrThrow(false,h::fail);
            h.assertTrue(decoded instanceof InfiniteChunkGenerator,"Authored terrain decoded as another generator");
            generator=(InfiniteChunkGenerator)decoded;
        }
        var possible=generator.getBiomeSource().possibleBiomes();
        h.assertTrue(possible.size()==InfiniteTerrain.Biome.values().length+InfiniteTerrain.Cave.values().length,"Incomplete authored surface/cave biome roster");
        for (var profile:InfiniteTerrain.Biome.values()) {
            var id=ResourceLocation.fromNamespaceAndPath("coraline","postgame/"+profile.name().toLowerCase(Locale.ROOT));
            var biome=registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME,id));
            h.assertTrue(possible.contains(biome),"Palette silently substituted another biome: "+id);
        }
        for (var profile:InfiniteTerrain.Cave.values()) {
            var id=ResourceLocation.fromNamespaceAndPath("coraline","postgame/cave_"+profile.name().toLowerCase(Locale.ROOT));
            var biome=registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME,id));
            h.assertTrue(possible.contains(biome),"Cave palette silently substituted another biome: "+id);
        }
        var generation=new InfiniteGeneration(UUID.randomUUID(),124119);
        h.assertTrue(generator.bind(generation),"Authored generator could not bind");
        var chunk=new ProtoChunk(new ChunkPos(-1,0),UpgradeData.EMPTY,LevelHeightAccessor.create(InfiniteTerrain.MIN_Y,InfiniteTerrain.HEIGHT),registry,null);
        chunk.setStatus(ChunkStatus.BIOMES);
        generator.createBiomes(Runnable::run,null,null,null,chunk).join();
        generator.fillFromNoise(Runnable::run,null,null,null,chunk).join();
        int custom=0;
        for (int x=-16;x<0;x++) for(int z=0;z<16;z++) {
            var column=generation.terrain().column(x,z);
            for(int y=25;y<column.surfaceY()-3;y++) {
                if(generation.terrain().cave(x,y,z,column))continue;
                var state=chunk.getBlockState(new net.minecraft.core.BlockPos(x,y,z));
                var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
                h.assertTrue(id!=null&&id.getNamespace().equals("coraline"),"Authored rock was replaced by vanilla/default terrain");
                custom++;
            }
        }
        h.assertTrue(custom>0,"No custom rock generated");
        h.succeed();
    }
    private InfiniteContentGameTests() {}
}
