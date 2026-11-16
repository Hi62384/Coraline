package com.coraline.otherworld;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Sound and state for doors whose client-side opening prediction is suppressed. */
public final class AuthoritativeHouseDoors {
    public static boolean toggle(ServerPlayer player,BlockPos at){
        var state=player.serverLevel().getBlockState(at);
        if(!(state.getBlock() instanceof DoorBlock door)||!door.type().canOpenByHand()||door instanceof com.coraline.block.LockedHouseDoorBlock)return false;
        return setOpen(player.serverLevel(),at,!state.getValue(DoorBlock.OPEN));
    }
    public static boolean setOpen(ServerLevel level,BlockPos at,boolean open){
        var state=level.getBlockState(at);
        if(!(state.getBlock() instanceof DoorBlock))return false;
        if(state.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER){at=at.below();state=level.getBlockState(at);}
        if(!(state.getBlock() instanceof DoorBlock door)||state.getValue(DoorBlock.HALF)!=DoubleBlockHalf.LOWER||state.getValue(DoorBlock.OPEN)==open)return false;
        // Vanilla excludes its player argument because that client normally
        // predicts the sound. Here it does not: null broadcasts the one normal
        // door event to the clicker too, without duplicate sounds for others.
        door.setOpen(null,level,state,at,open);
        return true;
    }
    private AuthoritativeHouseDoors(){}
}
