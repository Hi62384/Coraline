package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class CarnivalBlock extends BaseEntityBlock {
    public static final IntegerProperty PHASE=IntegerProperty.create("phase",0,2);
    public CarnivalBlock(){super(BlockBehaviour.Properties.copy(Blocks.NOTE_BLOCK).strength(-1,3600000).noLootTable());registerDefaultState(stateDefinition.any().setValue(PHASE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PHASE);}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new CarnivalEntity(at,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){if(!level.isClientSide&&hand==InteractionHand.MAIN_HAND&&player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(at) instanceof CarnivalEntity game)game.start(server);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide?createTickerHelper(type,com.coraline.registry.ModBlockEntities.CARNIVAL.get(),CarnivalEntity::tick):null;}
}
