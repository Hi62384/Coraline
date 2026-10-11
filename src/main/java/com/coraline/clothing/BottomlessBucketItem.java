package com.coraline.clothing;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Places fluid without consuming the bucket, but wears down on each source placed. */
public final class BottomlessBucketItem extends Item {
    private final boolean lava;
    public BottomlessBucketItem(boolean lava){super(new Item.Properties().durability(512));this.lava=lava;}

    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        BlockHitResult hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.NONE);
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResultHolder.pass(stack);
        BlockPos pos=hit.getBlockPos();
        if(!level.getBlockState(pos).canBeReplaced())pos=pos.relative(hit.getDirection());
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).canBeReplaced()||
            !level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack))return InteractionResultHolder.fail(stack);
        if(!lava&&level.dimensionType().ultraWarm())return InteractionResultHolder.fail(stack);
        if(!level.isClientSide){
            level.setBlock(pos,lava?Blocks.LAVA.defaultBlockState():Blocks.WATER.defaultBlockState(),3);
            level.gameEvent(player,GameEvent.FLUID_PLACE,pos);
            level.playSound(null,pos,lava?SoundEvents.BUCKET_EMPTY_LAVA:SoundEvents.BUCKET_EMPTY,SoundSource.BLOCKS,1F,1F);
            if(!player.getAbilities().instabuild)stack.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));
        }
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
