package com.coraline.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Invisible, precisely stepped collision under the rendered safe web strands. */
public final class StoryWebBlock extends Block {
    public static final IntegerProperty HEIGHT=IntegerProperty.create("height",1,16);
    public StoryWebBlock(){super(BlockBehaviour.Properties.of().strength(-1,3600000).noOcclusion().noLootTable().sound(SoundType.WOOL));registerDefaultState(stateDefinition.any().setValue(HEIGHT,16));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(HEIGHT);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Block.box(0,0,0,16,state.getValue(HEIGHT),16);}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
}
