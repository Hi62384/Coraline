package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** An item-free exit remains usable even if a player loses every possession. */
public final class InfiniteReturnPortal extends Block {
    public InfiniteReturnPortal(){super(BlockBehaviour.Properties.copy(Blocks.NETHER_PORTAL).strength(-1,3600000).noCollission().noLootTable().lightLevel(s->11));}
    @Override public boolean canBeReplaced(BlockState state,net.minecraft.world.level.material.Fluid fluid){return false;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter level,BlockPos at,CollisionContext context){return Block.box(1,0,7,15,16,9);}
    @Override public void entityInside(BlockState state,Level level,BlockPos at,net.minecraft.world.entity.Entity entity){if(entity instanceof net.minecraft.server.level.ServerPlayer player)InfiniteRealm.leave(player,false);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,net.minecraft.world.entity.player.Player player,InteractionHand hand,BlockHitResult hit){if(player instanceof net.minecraft.server.level.ServerPlayer p)InfiniteRealm.leave(p,false);return InteractionResult.sidedSuccess(level.isClientSide);}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return false;}
    @Override public void animateTick(BlockState state,Level level,BlockPos at,net.minecraft.util.RandomSource random){level.addParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,at.getX()+random.nextDouble(),at.getY()+random.nextDouble(),at.getZ()+.5,0,.02,0);}
}
