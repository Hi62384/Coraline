package com.coraline.wybie;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public final class CameraItem extends Item {
    public CameraItem(){super(new Properties().stacksTo(1));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){if(!level.isClientSide&&player instanceof net.minecraft.server.level.ServerPlayer p){WybieVisitNetwork.flash(p);player.getCooldowns().addCooldown(this,20);}return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);}
}
