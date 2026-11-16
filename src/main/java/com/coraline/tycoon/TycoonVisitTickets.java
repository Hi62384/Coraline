package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Bounded owner-keyed Forge tickets; never changes vanilla /forceload tickets. */
@Mod.EventBusSubscriber(modid="coraline")
public final class TycoonVisitTickets {
    public static void home(ServerPlayer p){var h=TycoonWorld.home(p);if(h.entrance()==null)return;force(p,p.server.overworld(),h.entrance(),"VisitHomeTickets");}
    public static void world(ServerPlayer p,BlockPos at){var l=p.server.getLevel(TycoonWorld.KEY);if(l!=null)force(p,l,at,"VisitWorldTickets");}
    private static void force(ServerPlayer p,ServerLevel l,BlockPos at,String key){var h=TycoonWorld.home(p);var existing=new HashSet<Long>();for(long value:h.state.getLongArray(key))existing.add(value);var chunk=new ChunkPos(at);for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var pos=new ChunkPos(chunk.x+x,chunk.z+z);if(existing.add(pos.toLong()))ForgeChunkManager.forceChunk(l,"coraline",TycoonWorld.owner(p),pos.x,pos.z,true,true);l.getChunk(pos.x,pos.z);}h.state.putLongArray(key,existing.stream().mapToLong(Long::longValue).toArray());TycoonWorld.dirty(p);}
    public static void warmHome(ServerPlayer p){var h=TycoonWorld.home(p);h.state.putLong("VisitWarmUntil",h.clock()+400);home(p);if(h.entrance()==null)return;var c=new ChunkPos(h.entrance());for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)p.server.overworld().getChunk(c.x+x,c.z+z);}
    public static void release(net.minecraft.server.MinecraftServer server,TycoonData.Home h){for(String key:List.of("VisitHomeTickets","VisitWorldTickets")){var l=key.equals("VisitHomeTickets")?server.overworld():server.getLevel(TycoonWorld.KEY);if(l!=null)for(long value:h.state.getLongArray(key)){var pos=new ChunkPos(value);ForgeChunkManager.forceChunk(l,"coraline",h.owner,pos.x,pos.z,false,true);}h.state.remove(key);}TycoonData.get(server).setDirty();}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p){var h=TycoonData.get(p.server).homes.get(p.getUUID());if(h!=null)release(p.server,h);}}
    @SubscribeEvent public static void started(net.minecraftforge.event.server.ServerStartedEvent event){for(var h:TycoonData.get(event.getServer()).homes.values())release(event.getServer(),h);}
    private TycoonVisitTickets(){}
}
