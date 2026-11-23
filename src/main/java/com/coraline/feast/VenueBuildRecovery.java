package com.coraline.feast;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Crash-safe ownership checkpoint for the one-time authored performance venues.
 * The saved building marker is flushed BEFORE the first physical block write.
 * A restart may replay only known authored building materials, never arbitrary
 * player builds, inventories or block entities. */
final class VenueBuildRecovery {
    static boolean accepted(BlockState state,boolean resuming,Set<Block> authored){
        return state.isAir()||resuming&&authored.contains(state.getBlock());
    }
    static boolean begin(ServerPlayer player,ServerLevel level,ShowSiteData sites,
                         CompoundTag owner,String name,BlockPos min,BlockPos max,
                         Set<Block> authored){
        String marker=name+"Building";
        boolean resuming=owner.getBoolean(marker);
        if(level.isOutsideBuildHeight(min)||level.isOutsideBuildHeight(max)
                ||!level.getWorldBorder().isWithinBounds(min)
                ||!level.getWorldBorder().isWithinBounds(max))return false;
        for(var pos:BlockPos.betweenClosed(min,max)){
            var state=level.getBlockState(pos);
            if(!accepted(state,resuming,authored)||!state.isAir()&&level.getBlockEntity(pos)!=null)
                return false;
        }
        if(!resuming){
            owner.putBoolean(marker,true);
            sites.setDirty();
            // Without this durable checkpoint, a crash can leave authored
            // blocks but no evidence that the unfinished structure is ours.
            player.server.overworld().getDataStorage().save();
        }
        return true;
    }
    static void complete(ShowSiteData sites,CompoundTag owner,String name){
        owner.putBoolean(name+"Built",true);
        owner.remove(name+"Building");
        sites.setDirty();
    }
    private VenueBuildRecovery(){}
}
