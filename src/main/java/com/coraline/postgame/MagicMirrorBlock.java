package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** A paired, solid tall mirror. Door pairing supplies placement and teardown,
 * but neither interaction nor redstone ever swings the mirror open. */
public final class MagicMirrorBlock extends DoorBlock implements EntityBlock {
    public MagicMirrorBlock(){super(BlockBehaviour.Properties.copy(Blocks.OAK_DOOR).strength(3,6).noOcclusion(),BlockSetType.OAK);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){var state=super.getStateForPlacement(context);return state==null?null:state.setValue(OPEN,false).setValue(POWERED,false);}
    @Override public void setOpen(net.minecraft.world.entity.Entity entity,Level level,BlockState state,BlockPos at,boolean open){}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return state.getValue(HALF)==DoubleBlockHalf.LOWER?new MagicMirrorBlockEntity(at,state):null;}
    @Override public RenderShape getRenderShape(BlockState state){return state.getValue(HALF)==DoubleBlockHalf.LOWER?RenderShape.ENTITYBLOCK_ANIMATED:RenderShape.INVISIBLE;}
    @Override public void neighborChanged(BlockState state,Level level,BlockPos at,Block block,BlockPos from,boolean moving){}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.isClientSide)return InteractionResult.SUCCESS;
        var root=state.getValue(HALF)==DoubleBlockHalf.UPPER?at.below():at;
        if(player instanceof net.minecraft.server.level.ServerPlayer p&&level.getBlockEntity(root) instanceof MagicMirrorBlockEntity mirror&&mirror.canUse(p))net.minecraftforge.network.NetworkHooks.openScreen(p,mirror,root);
        else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline.mirror.access"),true);
        return InteractionResult.CONSUME;
    }
}
