package com.coraline.postgame;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

public final class OtherIronThreadCutter extends Item {
    public OtherIronThreadCutter(){super(new Properties().durability(128));}
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var at=context.getClickedPos();var hand=context.getHand();
        if(!level.getBlockState(at).is(com.coraline.registry.ModBlocks.INFINITE_THREAD_BINDING.get())||!(level.getBlockEntity(at) instanceof SiteBindingBlockEntity binding)||level.getBlockState(at).getValue(SiteThreadBinding.OPEN))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        if(!(context.getPlayer() instanceof ServerPlayer player)||!player.isAlive()||player.isSpectator()||player.distanceToSqr(at.getCenter())>25||!level.hasChunkAt(at)||!level.mayInteract(player,at)||!player.mayUseItemAt(at,context.getClickedFace(),player.getItemInHand(hand))||!binding.authentic())return InteractionResult.FAIL;
        var tool=player.getItemInHand(hand);if(!tool.is(this)||tool.getDamageValue()>=tool.getMaxDamage())return InteractionResult.FAIL;
        var ray=level.clip(new ClipContext(player.getEyePosition(),at.getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(ray.getType()!=HitResult.Type.MISS&&!ray.getBlockPos().equals(at))return InteractionResult.FAIL;
        var permission=net.minecraftforge.common.ForgeHooks.onRightClickBlock(player,hand,at,new BlockHitResult(context.getClickLocation(),context.getClickedFace(),at,false));
        if(permission.isCanceled()||permission.getUseBlock()==net.minecraftforge.eventbus.api.Event.Result.DENY||permission.getUseItem()==net.minecraftforge.eventbus.api.Event.Result.DENY)return InteractionResult.FAIL;
        // Protection listeners may replace the hand, move the player or
        // replace the block. Never charge a detached stack for a live cut.
        if(player.level()!=level||!player.isAlive()||player.isSpectator()||player.getItemInHand(hand)!=tool||!tool.is(this)||tool.getDamageValue()>=tool.getMaxDamage()||player.distanceToSqr(at.getCenter())>25
            ||!level.mayInteract(player,at)||!player.mayUseItemAt(at,context.getClickedFace(),tool)||!binding.authentic())return InteractionResult.FAIL;
        ray=level.clip(new ClipContext(player.getEyePosition(),at.getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(ray.getType()!=HitResult.Type.MISS&&!ray.getBlockPos().equals(at))return InteractionResult.FAIL;
        if(!binding.cut())return InteractionResult.FAIL;
        if(!player.getAbilities().instabuild)tool.hurtAndBreak(1,player,who->who.broadcastBreakEvent(hand));
        level.playSound(null,at,net.minecraft.sounds.SoundEvents.SHEEP_SHEAR,net.minecraft.sounds.SoundSource.BLOCKS,.8F,.75F);player.swing(hand,true);
        player.displayClientMessage(Component.translatable("message.coraline.thread_cut"),true);return InteractionResult.CONSUME;
    }
    @Override public boolean isValidRepairItem(ItemStack tool,ItemStack material){return material.is(com.coraline.clothing.OtherMaterials.OTHER_IRON.get())||super.isValidRepairItem(tool,material);}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<Component> lines,TooltipFlag flags){lines.add(Component.translatable("tooltip.coraline.other_iron_thread_cutter"));}
}
