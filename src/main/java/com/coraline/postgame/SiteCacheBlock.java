package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;

/** Generated cache only, no craftable/carryable block item or copied lock ID. */
public final class SiteCacheBlock extends ChestBlock {
    public SiteCacheBlock(){super(BlockBehaviour.Properties.copy(Blocks.CHEST).noLootTable(),()->com.coraline.registry.ModBlockEntities.INFINITE_SITE_CACHE.get());}
    @Override public BlockEntity newBlockEntity(BlockPos at,BlockState state){return new SiteCacheBlockEntity(at,state);}
    @Override public InteractionResult use(BlockState state,Level level,BlockPos at,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache))return InteractionResult.FAIL;
        if(cache.locked()){
            if(!level.isClientSide&&!cache.canPick()){player.displayClientMessage(Component.translatable(cache.blockedFeedback()),true);return InteractionResult.SUCCESS;}
            if(player.getItemInHand(hand).getItem() instanceof OtherIronLockpick)return InteractionResult.PASS;
            if(!level.isClientSide)player.displayClientMessage(Component.translatable("message.coraline.site_cache_locked",cache.tier()),true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        var before=player.containerMenu;var result=super.use(state,level,at,player,hand,hit);if(player instanceof net.minecraft.server.level.ServerPlayer server&&before!=player.containerMenu)cache.investigateFactory(server);return result;
    }
    @Override public float getDestroyProgress(BlockState state,Player player,BlockGetter level,BlockPos at){return level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache&&cache.locked()?0:super.getDestroyProgress(state,player,level,at);}
    @Override public float getExplosionResistance(BlockState state,BlockGetter level,BlockPos at,Explosion explosion){return level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache&&cache.locked()?Float.MAX_VALUE:super.getExplosionResistance(state,level,at,explosion);}
    @Override public boolean canEntityDestroy(BlockState state,BlockGetter level,BlockPos at,net.minecraft.world.entity.Entity entity){return !(level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache)||!cache.locked();}
}
