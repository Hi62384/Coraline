package com.coraline.bobinsky;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
/** A private visit offer: a real parcel block, never an auto-picked-up item entity. */
public final class CheeseParcelBlock extends Block {
    public CheeseParcelBlock(){super(Properties.of().strength(-1,3600000).sound(SoundType.WOOL).noOcclusion().noLootTable());}
    @Override public VoxelShape getShape(BlockState s,net.minecraft.world.level.BlockGetter l,BlockPos p,CollisionContext c){return box(3,0,3,13,7,13);}
    @Override public InteractionResult use(BlockState s,Level level,BlockPos pos,Player p,InteractionHand hand,BlockHitResult hit){if(!level.isClientSide&&p instanceof net.minecraft.server.level.ServerPlayer owner)BobinskyVisit.takeParcel(owner,pos);return InteractionResult.sidedSuccess(level.isClientSide);}
}
