package com.coraline.item;

import com.coraline.feast.FirstVisit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Creative test shortcut; never grants the first-visit rewards. */
public final class FirstVisitDebugItem extends Item {
    public FirstVisitDebugItem(){super(new Properties().stacksTo(1));}
    private void complete(Player player){
        if(player instanceof ServerPlayer p){p.displayClientMessage(Component.literal(FirstVisit.debugComplete(p)),false);p.getCooldowns().addCooldown(this,20);}
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        complete(player);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getPlayer()!=null)complete(context.getPlayer());return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Creative: completes the first visit for your current parents.").withStyle(net.minecraft.ChatFormatting.GREEN));
        lines.add(Component.literal("The next entry starts the main loop. No rewards are granted.").withStyle(net.minecraft.ChatFormatting.GREEN));
    }
}
