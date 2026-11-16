package com.coraline.postgame;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

public final class OtherIronDrillDisruptor extends Item {
    public OtherIronDrillDisruptor(){super(new Properties().durability(160));}
    private boolean ready(ServerPlayer p,UseOnContext c,ItemStack tool,RiftMachineEntity machine){
        var at=c.getClickedPos();if(p.level()!=c.getLevel()||!p.isAlive()||p.isSpectator()||p.getItemInHand(c.getHand())!=tool||!tool.is(this)||tool.getMaxDamage()-tool.getDamageValue()<machine.sabotageCost()||p.distanceToSqr(at.getCenter())>25||!p.serverLevel().hasChunkAt(at)||!p.serverLevel().mayInteract(p,at)||!p.mayUseItemAt(at,c.getClickedFace(),tool)||!machine.authentic())return false;
        var ray=p.level().clip(new ClipContext(p.getEyePosition(),at.getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return ray.getType()==HitResult.Type.MISS||ray.getBlockPos().equals(at);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof RiftMachineEntity machine))return InteractionResult.PASS;
        if(context.getLevel().isClientSide)return InteractionResult.SUCCESS;
        if(!(context.getPlayer() instanceof ServerPlayer p))return InteractionResult.FAIL;var tool=p.getItemInHand(context.getHand());if(!ready(p,context,tool,machine))return InteractionResult.FAIL;
        var permission=net.minecraftforge.common.ForgeHooks.onRightClickBlock(p,context.getHand(),context.getClickedPos(),new BlockHitResult(context.getClickLocation(),context.getClickedFace(),context.getClickedPos(),false));
        if(permission.isCanceled()||permission.getUseBlock()==net.minecraftforge.eventbus.api.Event.Result.DENY||permission.getUseItem()==net.minecraftforge.eventbus.api.Event.Result.DENY||!ready(p,context,tool,machine)||!machine.sabotage(p)){
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.coraline.rift_sabotage_sequence"),true);return InteractionResult.FAIL;
        }
        if(!p.getAbilities().instabuild)tool.hurtAndBreak(machine.sabotageCost(),p,who->who.broadcastBreakEvent(context.getHand()));p.swing(context.getHand(),true);
        p.level().playSound(null,context.getClickedPos(),net.minecraft.sounds.SoundEvents.ANVIL_LAND,net.minecraft.sounds.SoundSource.BLOCKS,.45F,.7F);
        String feedback="message.coraline.rift_conduit_disabled";
        if(machine.isCore())feedback=switch(machine.containmentOutcome()){
            case "REDUCED"->"message.coraline.rift_drill_contained";
            case "UNPOWERED"->"message.coraline.rift_drill_no_fuel";
            case "BOSS_LOCKED"->"message.coraline.rift_drill_avatar_locked";
            default->"message.coraline.rift_drill_disabled";
        };
        p.displayClientMessage(net.minecraft.network.chat.Component.translatable(feedback),true);return InteractionResult.CONSUME;
    }
    @Override public boolean isValidRepairItem(ItemStack tool,ItemStack material){return material.is(com.coraline.clothing.OtherMaterials.OTHER_IRON.get())||super.isValidRepairItem(tool,material);}
    @Override public void appendHoverText(ItemStack stack,net.minecraft.world.level.Level level,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flags){lines.add(net.minecraft.network.chat.Component.translatable("tooltip.coraline.other_iron_drill_disruptor"));}
}
