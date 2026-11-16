package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Generated production press and three color-coded shutdown clutches. */
public final class FactoryControlBlock extends BaseEntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,3);public static final BooleanProperty ACTIVE=BooleanProperty.create("active"),PULSE=BooleanProperty.create("pulse");
    public FactoryControlBlock(){super(Properties.copy(Blocks.IRON_BLOCK).strength(-1,3600000).noLootTable().lightLevel(s->s.getValue(ACTIVE)?6:0));registerDefaultState(stateDefinition.any().setValue(PART,0).setValue(ACTIVE,true).setValue(PULSE,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PART,ACTIVE,PULSE);}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new FactoryControlEntity(at,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){if(level.isClientSide)return InteractionResult.SUCCESS;if(hand==InteractionHand.MAIN_HAND&&player instanceof net.minecraft.server.level.ServerPlayer p&&level.getBlockEntity(at) instanceof FactoryControlEntity control)control.operate(p);return InteractionResult.CONSUME;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide&&state.getValue(PART)==0?createTickerHelper(type,com.coraline.registry.ModBlockEntities.FACTORY_CONTROL.get(),FactoryControlEntity::tick):null;}
}
