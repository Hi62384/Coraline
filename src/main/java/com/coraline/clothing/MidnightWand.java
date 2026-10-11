package com.coraline.clothing;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class MidnightWand extends Item {
    public MidnightWand(){super(new Properties().durability(384));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
        if(!level.isClientSide){
            var bolt=new MagicBolt(level,player);bolt.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.5F,.3F);level.addFreshEntity(bolt);
            player.getCooldowns().addCooldown(this,WeaponConfig.WAND_COOLDOWN.get());stack.hurtAndBreak(1,player,p->p.broadcastBreakEvent(hand));
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.coraline.midnight_wand").withStyle(net.minecraft.ChatFormatting.GREEN));}
}
