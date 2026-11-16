package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

public final class PerformanceTileBlock extends Block {
    public static final IntegerProperty COLOR=IntegerProperty.create("color",0,3);public static final BooleanProperty LIT=BooleanProperty.create("lit");
    public PerformanceTileBlock(){super(BlockBehaviour.Properties.copy(Blocks.SMOOTH_QUARTZ).strength(-1,3600000).noLootTable().lightLevel(s->s.getValue(LIT)?15:0));registerDefaultState(stateDefinition.any().setValue(COLOR,0).setValue(LIT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(COLOR,LIT);}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
}
