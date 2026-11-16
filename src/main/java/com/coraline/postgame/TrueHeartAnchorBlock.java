package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

/** Real mechanisms and the breakable final core; no inventory or copied BE. */
public final class TrueHeartAnchorBlock extends Block {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,4);
    public static final BooleanProperty SEALED=BooleanProperty.create("sealed");
    public TrueHeartAnchorBlock(){super(Properties.copy(Blocks.QUARTZ_BLOCK).strength(8,3600000).noLootTable().lightLevel(s->s.getValue(SEALED)?8:14).pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));registerDefaultState(stateDefinition.any().setValue(PART,0).setValue(SEALED,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PART,SEALED);}
    @Override public float getDestroyProgress(BlockState state,Player player,BlockGetter level,BlockPos at){return state.getValue(PART)!=0||state.getValue(SEALED)?0:super.getDestroyProgress(state,player,level,at);}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){if(level.isClientSide)return InteractionResult.SUCCESS;if(player instanceof net.minecraft.server.level.ServerPlayer p&&hand==InteractionHand.MAIN_HAND)TrueHeartBattle.interact(p,at);return InteractionResult.CONSUME;}
    @Override public void onRemove(BlockState old,Level level,BlockPos at,BlockState next,boolean moved){if(!level.isClientSide&&old.getValue(PART)==0&&old.getBlock()!=next.getBlock()&&level instanceof net.minecraft.server.level.ServerLevel server)TrueHeartBattle.coreRemoved(server,at);super.onRemove(old,level,at,next,moved);}
}
