package com.coraline.doll;

import com.coraline.config.CoralineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import java.util.UUID;

public final class DollHome {
    public static boolean nearBed(ServerLevel level,BlockPos pos,UUID owner){
        if(owner==null)return false;
        var player=level.getServer().getPlayerList().getPlayer(owner);
        if(player==null||player.getRespawnPosition()==null||!level.dimension().equals(player.getRespawnDimension()))return false;
        return nearBed(level,pos,player.getRespawnPosition());
    }
    static boolean nearBed(ServerLevel level,BlockPos pos,BlockPos bed){
        int radius=CoralineConfig.DOLL_HOME_RADIUS.get();
        if(Math.abs(pos.getY()-bed.getY())>CoralineConfig.DOLL_HOME_HEIGHT.get()
                ||Math.pow(pos.getX()-bed.getX(),2)+Math.pow(pos.getZ()-bed.getZ(),2)>radius*radius)return false;
        // Resolve only the owner's actual respawn-bed chunk, never arbitrary storage chunks.
        level.getChunkAt(bed);
        return level.getBlockState(bed).getBlock() instanceof BedBlock;
    }
}
