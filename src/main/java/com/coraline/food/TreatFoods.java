package com.coraline.food;

import com.coraline.registry.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

public final class TreatFoods {
    public static final Map<String,RegistryObject<Item>> ITEMS=new LinkedHashMap<>();
    static {
        food("omelet",7,.6F);food("bacon_and_eggs",9,.7F);food("sweet_strawberries",3,.3F);
        food("scrambled_eggs",5,.5F);food("muffin",5,.4F);food("pizza",8,.65F);food("cupcakes",5,.3F);
        food("lemonade",3,.15F);food("orange_juice",4,.25F);food("hot_dog_in_bun",8,.6F);
        food("panini",8,.7F);food("cocoa_beetles",3,.3F);food("lollipop",2,.1F);food("perfect_roast_dinner",16,1.0F);
        ITEMS.put("cotton_candy",ModItems.ITEMS.register("cotton_candy",()->new ReusableTreat(false)));
        ITEMS.put("bottomless_candy_bag",ModItems.ITEMS.register("bottomless_candy_bag",()->new ReusableTreat(true)));
        ITEMS.put("popcorn",ModItems.ITEMS.register("popcorn",PopcornItem::new));
    }
    private static void food(String id,int hunger,float saturation){
        ITEMS.put(id,ModItems.ITEMS.register(id,()->new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(hunger).saturationMod(saturation).build())){
            @Override public FoodProperties getFoodProperties(ItemStack stack,net.minecraft.world.entity.LivingEntity entity){return FoodConfig.food(id);}
            @Override public UseAnim getUseAnimation(ItemStack stack){return id.equals("lemonade")||id.equals("orange_juice")?UseAnim.DRINK:UseAnim.EAT;}
        }));
    }
    public static void init(){}
}
