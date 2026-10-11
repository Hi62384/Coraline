package com.coraline.food;

import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import java.util.List;

/** A serving uses normal eating hooks, while the persistent container/stick remains. */
public final class ReusableTreat extends Item {
    private final boolean bottomless;
    public ReusableTreat(boolean bottomless){super(new Properties().stacksTo(1).food(new FoodProperties.Builder().nutrition(2).saturationMod(bottomless?.01F:.1F).build()));this.bottomless=bottomless;}
    public static int bites(ItemStack stack){return stack.hasTag()?Math.max(0,Math.min(2,stack.getTag().getInt("CoralineBites"))):0;}
    @Override public FoodProperties getFoodProperties(ItemStack stack,LivingEntity entity){return FoodConfig.food(bottomless?"bottomless_candy_bag":"cotton_candy");}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        if(!level.isClientSide){
            // Consume a copy so vanilla food, statistics, criteria and eating events still run.
            entity.eat(level,stack.copyWithCount(1));
            if(!bottomless&&(!(entity instanceof Player p)||!p.getAbilities().instabuild)){
                int next=bites(stack)+1;
                if(next>=3)return ItemStack.EMPTY;
                stack.getOrCreateTag().putInt("CoralineBites",next);
            }
        }
        return stack;
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable(bottomless?"tooltip.coraline.bottomless_candy_bag":"tooltip.coraline.cotton_candy",3-bites(stack)).withStyle(net.minecraft.ChatFormatting.GREEN));
    }
}
