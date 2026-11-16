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

public final class ObservatoryBlock extends BaseEntityBlock {
    public static final IntegerProperty OBSERVED=IntegerProperty.create("observed",0,4);
    public static final IntegerProperty ROTATION=IntegerProperty.create("rotation",0,7);
    public ObservatoryBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(-1,3600000).noLootTable().noOcclusion().lightLevel(s->s.getValue(OBSERVED)*3));registerDefaultState(stateDefinition.any().setValue(OBSERVED,0).setValue(ROTATION,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(OBSERVED,ROTATION);}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new ObservatoryEntity(at,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){if(!level.isClientSide&&level.getBlockEntity(at) instanceof ObservatoryEntity telescope)player.displayClientMessage(net.minecraft.network.chat.Component.translatable(telescope.complete()?"message.coraline.observatory_complete":"message.coraline.observatory_hint"),true);return player.getItemInHand(hand).is(net.minecraft.world.item.Items.SPYGLASS)?InteractionResult.PASS:InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide?createTickerHelper(type,com.coraline.registry.ModBlockEntities.OBSERVATORY.get(),ObservatoryEntity::tick):null;}
}
