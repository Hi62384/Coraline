package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class RiftMachineBlock extends BaseEntityBlock {
    public static final BooleanProperty OPEN=BooleanProperty.create("open"),ACTIVE=BooleanProperty.create("active");
    private final RiftMachineLayout.Kind kind;
    public RiftMachineBlock(int kind){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(-1,3600000).dynamicShape().noOcclusion().noLootTable().lightLevel(state->state.getValue(ACTIVE)?5:0));this.kind=RiftMachineLayout.Kind.values()[kind];registerDefaultState(stateDefinition.any().setValue(OPEN,false).setValue(ACTIVE,true));}
    static Block block(RiftMachineLayout.Kind kind){return switch(kind){case CORE->com.coraline.registry.ModBlocks.RIFT_DRILL_CORE.get();case CONDUIT->com.coraline.registry.ModBlocks.RIFT_DRILL_CONDUIT.get();case GATE->com.coraline.registry.ModBlocks.RIFT_SECURITY_GATE.get();};}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(OPEN,ACTIVE);}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new RiftMachineEntity(at,state);}
    @Override public RenderShape getRenderShape(BlockState state){return kind==RiftMachineLayout.Kind.GATE?RenderShape.MODEL:RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){return kind==RiftMachineLayout.Kind.GATE&&state.getValue(OPEN)?Shapes.empty():Shapes.block();}
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos at,CollisionContext context){
        if(kind!=RiftMachineLayout.Kind.GATE)return Shapes.block();
        if(level instanceof net.minecraft.server.level.ServerLevel)return level.getBlockEntity(at) instanceof RiftMachineEntity machine&&machine.authentic()&&!machine.locked()?Shapes.empty():Shapes.block();
        return state.getValue(OPEN)?Shapes.empty():Shapes.block();
    }
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
        if(kind==RiftMachineLayout.Kind.GATE&&state.getValue(OPEN))return InteractionResult.PASS;
        if(kind!=RiftMachineLayout.Kind.GATE&&!state.getValue(ACTIVE)){
            if(!level.isClientSide)player.displayClientMessage(net.minecraft.network.chat.Component.translatable(kind==RiftMachineLayout.Kind.CORE?"message.coraline.rift_drill_disabled":"message.coraline.rift_conduit_disabled"),true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        var item=player.getItemInHand(hand).getItem();if(kind==RiftMachineLayout.Kind.GATE&&item instanceof OtherIronLockpick||kind!=RiftMachineLayout.Kind.GATE&&item instanceof OtherIronDrillDisruptor)return InteractionResult.PASS;
        if(!level.isClientSide)player.displayClientMessage(net.minecraft.network.chat.Component.translatable(kind==RiftMachineLayout.Kind.GATE?"message.coraline.rift_security_locked":"message.coraline.rift_disruptor_required"),true);return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return !level.isClientSide&&kind==RiftMachineLayout.Kind.CORE?createTickerHelper(type,com.coraline.registry.ModBlockEntities.RIFT_MACHINE.get(),RiftMachineEntity::tick):null;}
}
