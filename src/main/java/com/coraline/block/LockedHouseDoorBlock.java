package com.coraline.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/** Normal spruce appearance/collision, but no client-side opening prediction or redstone bypass. */
public final class LockedHouseDoorBlock extends DoorBlock {
    public LockedHouseDoorBlock(){super(Properties.copy(Blocks.SPRUCE_DOOR),BlockSetType.SPRUCE);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer p)com.coraline.secrets.HouseCabinets.interact(p,pos);
        else if(!state.getValue(OPEN)&&player.getItemInHand(hand).is(com.coraline.secrets.SecretsItems.KEY.get()))player.startUsingItem(hand);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,BlockPos from,boolean moving){
        // Door support/half updates are handled by updateShape. Power must never
        // open a keyed room; only the authoritative key/event access path may.
    }
}
