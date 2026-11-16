package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Authored constellation markers cannot be displaced by water or destroyed
 * to strand the research objective. They have no inventory item or loot. */
public final class ObservatoryStarBlock extends Block {
    public static final IntegerProperty COLOR=IntegerProperty.create("color",0,3);
    public ObservatoryStarBlock(){super(BlockBehaviour.Properties.copy(Blocks.SEA_LANTERN).strength(-1,3600000).noLootTable().lightLevel(s->15));registerDefaultState(stateDefinition.any().setValue(COLOR,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(COLOR);}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,Entity entity){return false;}
}
