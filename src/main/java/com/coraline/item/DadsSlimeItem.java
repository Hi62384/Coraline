package com.coraline.item;
import com.coraline.config.CoralineConfig;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.food.FoodProperties;

public final class DadsSlimeItem extends Item {
    public DadsSlimeItem(){super(new Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(2).saturationMod(.1F).build()));}
    @Override public FoodProperties getFoodProperties(ItemStack stack,LivingEntity entity){return new FoodProperties.Builder().nutrition(CoralineConfig.SLIME_NUTRITION.get()).saturationMod(.1F).build();}
    @Override public ItemStack finishUsingItem(ItemStack s,Level l,LivingEntity e){
        ItemStack out=super.finishUsingItem(s,l,e);
        if(e instanceof Player p&&!p.isCreative()){
            if(out.isEmpty())return new ItemStack(Items.BOWL);
            if(!l.isClientSide&&!p.getInventory().add(new ItemStack(Items.BOWL)))p.drop(new ItemStack(Items.BOWL),false);
        }return out;
    }
}
