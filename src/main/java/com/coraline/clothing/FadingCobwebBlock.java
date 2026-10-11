package com.coraline.clothing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;

/** Scheduled expiry survives saves/chunk unloading and never removes replacement blocks. */
public final class FadingCobwebBlock extends WebBlock {
    public FadingCobwebBlock(){super(BlockBehaviour.Properties.copy(Blocks.COBWEB).noLootTable());}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){if(!level.isClientSide)level.scheduleTick(pos,this,ClothingConfig.WEB_LIFETIME.get());}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){if(level.getBlockState(pos).is(this))level.removeBlock(pos,false);}
}
