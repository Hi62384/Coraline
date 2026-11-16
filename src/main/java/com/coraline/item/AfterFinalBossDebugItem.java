package com.coraline.item;
import com.coraline.feast.BeldamFinale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class AfterFinalBossDebugItem extends Item {
    public AfterFinalBossDebugItem(){super(new Properties().stacksTo(1));}
    @Override public Component getName(ItemStack stack){return Component.literal("Debug: After Final Boss");}
    private void activate(net.minecraft.world.entity.player.Player player){if(player instanceof ServerPlayer p&&!p.getCooldowns().isOnCooldown(this)){p.sendSystemMessage(Component.literal(BeldamFinale.debugAfterFinalBoss(p)));p.getCooldowns().addCooldown(this,10);}}
    @Override public InteractionResultHolder<ItemStack> use(Level level,net.minecraft.world.entity.player.Player player,InteractionHand hand){activate(player);return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){if(context.getPlayer()!=null)activate(context.getPlayer());return InteractionResult.sidedSuccess(context.getLevel().isClientSide);}
}
