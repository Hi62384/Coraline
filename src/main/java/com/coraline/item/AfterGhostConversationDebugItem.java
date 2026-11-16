package com.coraline.item;

import com.coraline.feast.AcceptedButtonEyes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class AfterGhostConversationDebugItem extends Item {
    public AfterGhostConversationDebugItem(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){return Component.literal("Debug: After Ghost Children Talk");}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!level.isClientSide&&player instanceof ServerPlayer p&&hand==InteractionHand.MAIN_HAND&&!p.getCooldowns().isOnCooldown(this)){
            p.sendSystemMessage(Component.literal(AcceptedButtonEyes.debugAfterGhostConversation(p)));
            p.getCooldowns().addCooldown(this,10);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
        var player=context.getPlayer();if(player==null)return InteractionResult.PASS;
        return use(context.getLevel(),player,context.getHand()).getResult();
    }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Operator only. Accept the button-eye proposal first."));
        lines.add(Component.literal("Skips to the mirror prison after the children speak."));
    }
}
