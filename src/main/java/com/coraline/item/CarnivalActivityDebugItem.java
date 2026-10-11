package com.coraline.item;

import com.coraline.feast.ActivityWorld;
import com.coraline.feast.OtherMotherActivities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.List;

/** Creative-only shortcut to the carnival activity. */
public final class CarnivalActivityDebugItem extends Item {
    public CarnivalActivityDebugItem(){super(new Properties().stacksTo(1));}
    private void travel(Player player){if(player instanceof ServerPlayer p){
        p.sendSystemMessage(Component.literal(ActivityWorld.enterDebug(p,OtherMotherActivities.Activity.CARNIVAL)));
        p.getCooldowns().addCooldown(this,20);
    }}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        travel(player);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getPlayer()!=null)travel(context.getPlayer());return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Creative: enter the Carnival directly.").withStyle(ChatFormatting.GREEN));
        lines.add(Component.literal("Complete the first visit first. The return portal brings you back here.").withStyle(ChatFormatting.GRAY));
    }
}
