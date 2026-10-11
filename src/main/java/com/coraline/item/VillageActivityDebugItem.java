package com.coraline.item;

import com.coraline.feast.ActivityWorld;
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

/** Creative shortcut to the playable Other Village activity. */
public final class VillageActivityDebugItem extends Item {
    public VillageActivityDebugItem(){super(new Properties().stacksTo(1));}
    private void travel(Player player){
        if(player instanceof ServerPlayer p){
            p.sendSystemMessage(Component.literal(ActivityWorld.enterDebug(p)));
            p.getCooldowns().addCooldown(this,20);
        }
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        travel(player);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getPlayer()!=null)travel(context.getPlayer());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal("Creative: teleport directly to the Other Village.").withStyle(ChatFormatting.GREEN));
        lines.add(Component.literal("Finish the first visit first. The portal returns you here.").withStyle(ChatFormatting.GRAY));
    }
}
