package com.coraline.item;

import com.coraline.feast.RescueChapter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class AfterOtherFatherDebugItem extends Item {
    public AfterOtherFatherDebugItem(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){return Component.literal("Debug: After Other Father Boss");}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide&&player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND&&!p.getCooldowns().isOnCooldown(this)){
            p.sendSystemMessage(Component.literal(RescueChapter.debugAfterOtherFather(p)));
            p.getCooldowns().addCooldown(this,10);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        return use(context.getLevel(),player,context.getHand()).getResult();
    }
}
