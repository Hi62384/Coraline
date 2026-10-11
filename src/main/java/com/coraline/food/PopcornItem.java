package com.coraline.food;

import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.*;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Durability is servings, not enchantment-repairable equipment damage. */
public final class PopcornItem extends Item {
    public PopcornItem(){super(new Properties().durability(32).food(new FoodProperties.Builder().nutrition(1).saturationMod(.05F).build()));}
    @Override public int getUseDuration(ItemStack stack){return 72000;}
    @Override public FoodProperties getFoodProperties(ItemStack stack,LivingEntity entity){return FoodConfig.food("popcorn");}
    @Override public int getMaxDamage(ItemStack stack){return FoodConfig.POPCORN_SERVINGS.get();}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.EAT;}
    @Override public boolean isBarVisible(ItemStack stack){return true;}
    @Override public int getBarColor(ItemStack stack){return 0xe6bd57;}
    @Override public boolean isEnchantable(ItemStack stack){return false;}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return false;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient){return false;}
    @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
        if(level.isClientSide||!(entity instanceof Player p))return;
        if(!p.canEat(false)){p.stopUsingItem();return;}
        if((getUseDuration(stack)-remaining+1)%FoodConfig.POPCORN_INTERVAL.get()!=0)return;
        var food=getFoodProperties(stack,p);p.getFoodData().eat(food.getNutrition(),food.getSaturationModifier());
        if(com.coraline.clothing.ClothingEffects.wearing(p,com.coraline.clothing.ClothingItem.Style.GARDEN_OVERALLS))p.heal((float)(food.getNutrition()*com.coraline.clothing.ClothingConfig.FOOD_HEAL.get()));
        p.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        p.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT);
        level.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EAT,net.minecraft.sounds.SoundSource.PLAYERS,.4F,1.15F);
        if(!p.getAbilities().instabuild){
            int used=stack.getDamageValue()+1;
            if(used>=stack.getMaxDamage()){stack.shrink(1);p.stopUsingItem();}else stack.setDamageValue(used);
        }
    }
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.coraline.popcorn",stack.getMaxDamage()-stack.getDamageValue()).withStyle(net.minecraft.ChatFormatting.GREEN));}
}
