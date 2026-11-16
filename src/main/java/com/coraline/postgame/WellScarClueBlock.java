package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class WellScarClueBlock extends BaseEntityBlock {
    public static final IntegerProperty CLUE=IntegerProperty.create("clue",0,3);
    public WellScarClueBlock(){super(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS).strength(-1,3600000).noLootTable());registerDefaultState(stateDefinition.any().setValue(CLUE,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(CLUE);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new WellScarClueEntity(p,s);}
    @Override public boolean canEntityDestroy(BlockState s,BlockGetter l,BlockPos p,net.minecraft.world.entity.Entity e){return false;}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos at,Player p,InteractionHand hand,BlockHitResult hit){if(!l.isClientSide&&hand==InteractionHand.MAIN_HAND&&p instanceof net.minecraft.server.level.ServerPlayer server&&l.getBlockEntity(at) instanceof WellScarClueEntity clue)clue.read(server);return InteractionResult.sidedSuccess(l.isClientSide);}
}
