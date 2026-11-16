package com.coraline.postgame;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Designated stitched cells. Never replace ordinary web, story restraints,
 * unrelated locks or NPC custody with a generic shears interaction. */
public final class SiteThreadBinding extends Block implements EntityBlock {
    public static final BooleanProperty OPEN=BooleanProperty.create("open");
    public SiteThreadBinding(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BARS).strength(-1,3600000).noOcclusion().dynamicShape().noLootTable());registerDefaultState(stateDefinition.any().setValue(OPEN,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(OPEN);}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new SiteBindingBlockEntity(at,state);}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){return state.getValue(OPEN)?Shapes.empty():Shapes.block();}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){
        if(!state.getValue(OPEN))return Shapes.block();
        if(level instanceof net.minecraft.server.level.ServerLevel)return level.getBlockEntity(at) instanceof SiteBindingBlockEntity binding&&binding.alreadyCut()&&binding.authentic()?Shapes.empty():Shapes.block();
        return Shapes.empty();
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
        if(state.getValue(OPEN)||player.getItemInHand(hand).getItem() instanceof OtherIronThreadCutter)return InteractionResult.PASS;
        if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.coraline.thread_cutter_required"),true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
