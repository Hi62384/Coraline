package com.coraline.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Farmland tilled by the perfect hoe: its own marked surface and 30% extra crop ticks. */
public final class PerfectFarmlandBlock extends FarmBlock {
    public PerfectFarmlandBlock(){super(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.FARMLAND));}
    @Override public boolean canSustainPlant(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.core.Direction direction,net.minecraftforge.common.IPlantable plant){
        return direction==net.minecraft.core.Direction.UP&&plant.getPlantType(level,pos.above())==net.minecraftforge.common.PlantType.CROP||super.canSustainPlant(state,level,pos,direction,plant);
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        super.randomTick(state,level,pos,random);
        if(level.getBlockState(pos).getBlock()!=this||random.nextFloat()>=.30F)return;
        var above=pos.above();var crop=level.getBlockState(above);
        if(crop.getBlock() instanceof CropBlock&&!((CropBlock)crop.getBlock()).isMaxAge(crop))crop.randomTick(level,above,random);
    }
}
