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

public final class SeamAnchorBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION=IntegerProperty.create("rotation",0,3);
    public static final BooleanProperty ACTIVE=BooleanProperty.create("active");
    private final boolean core;
    public SeamAnchorBlock(boolean core){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(-1,3600000).noLootTable().noOcclusion().lightLevel(state->state.getValue(ACTIVE)?7:0));this.core=core;registerDefaultState(stateDefinition.any().setValue(ROTATION,0).setValue(ACTIVE,true));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ROTATION,ACTIVE);}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new SeamAnchorEntity(at,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.phys.shapes.CollisionContext context){
        if(!core)return Block.box(1,0,1,15,5,15);
        return net.minecraft.world.phys.shapes.Shapes.or(Block.box(1,0,1,15,3,15),Block.box(2,3,6,4,30,10),Block.box(12,3,6,14,30,10),Block.box(4,28,6,12,31,10));
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&hand==InteractionHand.MAIN_HAND&&level.getBlockEntity(at) instanceof SeamAnchorEntity seam)seam.interact(server);
        return InteractionResult.CONSUME;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide&&core?createTickerHelper(type,com.coraline.registry.ModBlockEntities.SEAM_ANCHOR.get(),SeamAnchorEntity::tick):null;}
}
