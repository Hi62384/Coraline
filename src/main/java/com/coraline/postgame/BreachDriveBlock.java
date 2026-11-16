package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class BreachDriveBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED=BooleanProperty.create("powered");
    public BreachDriveBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(-1,3600000).noOcclusion().noLootTable().lightLevel(state->state.getValue(POWERED)?9:0));registerDefaultState(stateDefinition.any().setValue(POWERED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(POWERED);}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new BreachDriveBlockEntity(at,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){return Block.box(1,0,1,15,16,15);}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(player instanceof net.minecraft.server.level.ServerPlayer p&&level.getBlockEntity(at) instanceof BreachDriveBlockEntity device&&device.canUse(p))net.minecraftforge.network.NetworkHooks.openScreen(p,device,at);
        else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline.breach_drive_dormant"),true);
        return InteractionResult.CONSUME;
    }
}
