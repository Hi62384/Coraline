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

public final class BoundaryDefenseBlock extends BaseEntityBlock {
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,3);
    public BoundaryDefenseBlock(){super(BlockBehaviour.Properties.copy(Blocks.BEACON).strength(-1,3600000).noLootTable().noOcclusion().lightLevel(s->15));registerDefaultState(stateDefinition.any().setValue(STAGE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(STAGE);}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new BoundaryDefenseEntity(at,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){if(!level.isClientSide&&hand==InteractionHand.MAIN_HAND&&player instanceof net.minecraft.server.level.ServerPlayer server&&level.getBlockEntity(at) instanceof BoundaryDefenseEntity defense)defense.start(server);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide?createTickerHelper(type,com.coraline.registry.ModBlockEntities.BOUNDARY_DEFENSE.get(),BoundaryDefenseEntity::tick):null;}
}
