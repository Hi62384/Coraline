package com.coraline.item;

import com.coraline.otherworld.OtherWorld;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Operator-free creative testing without changing the family's natural nightly roll. */
public final class MouseDebugItem extends Item {
    public MouseDebugItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(player instanceof ServerPlayer serverPlayer){
            player.displayClientMessage(Component.literal(OtherWorld.debugMice(serverPlayer)),false);
            player.getCooldowns().addCooldown(this,40);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Creative: right-click to test mice. Reports missing requirements.").withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.literal("Bypasses chance and the nightly limit, not event requirements.").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
