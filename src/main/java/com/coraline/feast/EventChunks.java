package com.coraline.feast;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import java.util.*;
/** Small saved chunk leases, preserving pre-existing forced chunks and overlapping owners. */
final class EventChunks {
    static void hold(ServerPlayer p,VisitData.Scene s,Collection<BlockPos> points){var chunks=new HashSet<Long>();for(var point:points)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)chunks.add(ChunkPos.asLong((point.getX()>>4)+x,(point.getZ()>>4)+z));var leases=s.loop.event.getList("EventChunks",10);for(long chunk:chunks){boolean already=false;for(var tag:leases)if(((CompoundTag)tag).getLong("Chunk")==chunk)already=true;if(already)continue;boolean original=p.serverLevel().getForcedChunks().contains(chunk);for(var other:VisitData.get(p.server).scenes.values())if(other!=s)for(var tag:other.loop.event.getList("EventChunks",10)){var n=(CompoundTag)tag;if(n.getLong("Chunk")==chunk)original=n.getBoolean("Original");}var n=new CompoundTag();n.putLong("Chunk",chunk);n.putBoolean("Original",original);leases.add(n);var pos=new ChunkPos(chunk);p.serverLevel().setChunkForced(pos.x,pos.z,true);}s.loop.event.put("EventChunks",leases);}
    static void release(ServerPlayer p,VisitData.Scene s){var level=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);if(level==null)return;for(var tag:s.loop.event.getList("EventChunks",10)){var n=(CompoundTag)tag;if(n.getBoolean("Original"))continue;long id=n.getLong("Chunk");boolean used=false;for(var other:VisitData.get(p.server).scenes.values())if(other!=s)for(var lease:other.loop.event.getList("EventChunks",10))if(((CompoundTag)lease).getLong("Chunk")==id)used=true;if(!used){var pos=new ChunkPos(id);level.setChunkForced(pos.x,pos.z,false);}}s.loop.event.remove("EventChunks");}
    private EventChunks(){}
}
