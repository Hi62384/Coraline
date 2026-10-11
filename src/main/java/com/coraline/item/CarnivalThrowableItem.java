package com.coraline.item;

import com.coraline.feast.CarnivalActivity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** An issued, aimed carnival ring or dart; the server validates the matching active booth. */
public final class CarnivalThrowableItem extends Item {
    private final boolean dart;
    public CarnivalThrowableItem(boolean dart){super(new Properties().stacksTo(16));this.dart=dart;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide&&player instanceof net.minecraft.server.level.ServerPlayer p)CarnivalActivity.throwProjectile(p,stack,dart);
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
