package com.coraline.item;

import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.server.level.ServerPlayer;

/** Reusable, owner-bound activity fruit; never calls vanilla random teleport. */
public final class ActivityChorusFruitItem extends Item {
    public ActivityChorusFruitItem(){super(new Properties().stacksTo(1).food(new FoodProperties.Builder().nutrition(0).saturationMod(0).alwaysEat().build()));}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
        if(p instanceof ServerPlayer server&&!com.coraline.feast.EndCityActivity.canEat(server,p.getItemInHand(hand)))return InteractionResultHolder.fail(p.getItemInHand(hand));
        return super.use(l,p,hand);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(entity instanceof ServerPlayer p)com.coraline.feast.EndCityActivity.eat(p,stack);
        return stack; // unlimited charges; exact stack is removed by activity cleanup
    }
}
