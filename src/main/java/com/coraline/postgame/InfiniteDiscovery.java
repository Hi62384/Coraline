package com.coraline.postgame;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Register actual visited biomes and native structure pieces. Discovery never
 * grants sabotage credit, an actor identity, items, or quest completion. */
@Mod.EventBusSubscriber(modid="coraline")
public final class InfiniteDiscovery {
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||event.getServer().getTickCount()%20!=0)return;
        for(var player:event.getServer().getPlayerList().getPlayers()){
            if(!player.isAlive()||player.isCreative()||player.isSpectator()||!(player.serverLevel().getChunkSource().getGenerator() instanceof InfiniteChunkGenerator generator)||!InfiniteWorldData.get(event.getServer()).accessible())continue;
            var level=player.serverLevel();var generation=generator.structureGeneration();if(!InfiniteWorldData.get(event.getServer()).matchesGeneration(generation.id()))continue;var current=player.chunkPosition();var chunk=level.getChunkSource().getChunkNow(current.x,current.z);if(chunk==null)continue;
            var region=InfiniteDiscoveryRegion.get(level,generation.id(),current.x,current.z);boolean first=region.visit(player.getUUID(),current.x,current.z);
            if(first)level.getBiome(player.blockPosition()).unwrapKey().ifPresent(key->{if(region.biome(player.getUUID(),key.location().toString()))player.displayClientMessage(Component.translatable("message.coraline.postgame_region_biome",Component.translatable(key.location().toLanguageKey("biome"))),true);});
            // References point to start chunks. Inspect only starts whose
            // native chunks are already loaded; never force-load an old site.
            var candidates=new LinkedHashSet<Long>();candidates.add(current.toLong());for(var refs:chunk.getAllReferences().values())for(long at:refs)if(candidates.size()<16)candidates.add(at);
            var seen=new HashSet<UUID>();
            for(long at:candidates){var pos=new ChunkPos(at);var origin=level.getChunkSource().getChunkNow(pos.x,pos.z);if(origin==null)continue;
                for(var start:origin.getAllStarts().values()){
                    if(!start.isValid()||!start.getBoundingBox().isInside(player.blockPosition()))continue;
                    for(var piece:start.getPieces())if(piece instanceof InfiniteSitePiece site){var plan=site.sourcePlan().orElse(null);
                        if(plan==null||!plan.generation().equals(generation.id())||plan.seed()!=generation.seed()||!seen.add(plan.site()))continue;
                        WybieRescue.discover(player,plan);
                        PostgameQuests.discover(player,plan);
                        var shard=InfiniteDiscoveryRegion.get(level,generation.id(),plan.x()>>4,plan.z()>>4);
                        if(shard.site(player.getUUID(),plan))player.sendSystemMessage(Component.translatable("message.coraline.postgame_site_discovered",Component.translatable("structure.coraline."+plan.family().id())));
                    }
                }
            }
        }
    }
    private InfiniteDiscovery(){}
}
