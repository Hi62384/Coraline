package com.coraline.block;

import com.coraline.block.entity.DollBlockEntity;
import com.coraline.item.DollItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class DollBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);

    public DollBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DollBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,com.coraline.registry.ModBlockEntities.DOLL.get(),DollBlockEntity::serverTick);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level instanceof net.minecraft.server.level.ServerLevel sl&&level.getBlockEntity(pos) instanceof DollBlockEntity doll&&!doll.moving)
            com.coraline.doll.DollBondData.get(sl.getServer()).lost(doll.createDollStack(),sl);
        super.onRemove(state,level,pos,next,moving);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level.getBlockEntity(pos) instanceof DollBlockEntity doll)) {
            return;
        }

        CompoundTag owner = stack.getTagElement("BlockEntityTag");
        if (owner != null && owner.hasUUID(DollItem.OWNER_ID)) {
            doll.load(owner);
            doll.setOwner(owner.getUUID(DollItem.OWNER_ID), owner.getString(DollItem.OWNER_NAME));
        } else if (placer != null) {
            doll.setOwner(placer.getUUID(), placer.getName().getString());
        }
        if(level instanceof net.minecraft.server.level.ServerLevel sl)
            com.coraline.doll.DollBondData.get(sl.getServer()).placed(doll.createDollStack(),sl,pos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative()
                && level.getBlockEntity(pos) instanceof DollBlockEntity doll) {
            popResource(level, pos, doll.createDollStack());
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
