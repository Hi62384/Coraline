package com.coraline.tycoon;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Nutrition is independent of care quality. Mods may register explicit tiers. */
public final class TycoonFoodQuality {
    public enum Tier {EXCELLENT,GOOD,NORMAL,POOR,TERRIBLE}
    private static final Map<ResourceLocation,Tier> REGISTERED=new HashMap<>();
    public static synchronized void register(ResourceLocation item,Tier tier){REGISTERED.put(Objects.requireNonNull(item),Objects.requireNonNull(tier));}
    public static Tier tier(ItemStack stack){
        var food=stack.getFoodProperties(null);if(food==null)return Tier.POOR;
        if(food.getEffects().stream().anyMatch(e->e.getSecond()>0&&!e.getFirst().getEffect().isBeneficial()))return Tier.TERRIBLE;
        var id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        if(id!=null){for(String override:TycoonFoodConfig.OVERRIDES.get()){String[] pair=override.split("=",2);if(pair[0].equals(id.toString()))return Tier.valueOf(pair[1]);}var explicit=REGISTERED.get(id);if(explicit!=null)return explicit;}
        double score=food.getNutrition()*(1+food.getSaturationModifier());return score>=18?Tier.EXCELLENT:score>=10?Tier.GOOD:score>=5?Tier.NORMAL:Tier.POOR;
    }
    public static double preference(TycoonData.Resident r,ItemStack food){var id=ForgeRegistries.ITEMS.getKey(food.getItem());return id!=null&&id.toString().equals(r.n.getString("Favorite"))?1.1:1+(r.trait("Curiosity")-.5)*.1;}
    public static double neglect(TycoonData.Resident r,ItemStack food,boolean served){var tier=tier(food);double delta=TycoonFoodConfig.CARE[tier.ordinal()].get();
        // A harmful food can never become a beneficial meal through preferences
        // or configuration, and is strictly less harmful than the missed episode.
        if(tier==Tier.TERRIBLE)delta=Math.max(.1,delta);else if(delta<0)delta*=preference(r,food);
        if(!served)delta+=TycoonFoodConfig.SELF.get();return delta>0?Math.min(delta,TycoonFoodConfig.MISSED.get()*.8):delta;
    }
    public static double hunger(ItemStack food){var properties=food.getFoodProperties(null);return properties==null?0:properties.getNutrition()*7+8;}
    private TycoonFoodQuality(){}
}
