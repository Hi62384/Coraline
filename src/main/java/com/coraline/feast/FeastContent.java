package com.coraline.feast;

import com.coraline.registry.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.registries.RegistryObject;
import java.util.*;

public final class FeastContent {
    public static final Map<String,RegistryObject<Item>> FOODS=new LinkedHashMap<>();
    static {
        for(String name:new String[]{"golden_chicken_breast","corn_on_the_cob","sweet_peas","mashed_potatoes","strawberry_pancakes","sausages","mango_milkshake","strawberry_milkshake","kiwi_milkshake","chocolate_milkshake","fruit_punch"}){
            boolean drink=name.endsWith("milkshake")||name.equals("fruit_punch");
            FOODS.put(name,ModItems.ITEMS.register(name,()->new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(drink?5:8).saturationMod(.65F).build())){
                @Override public UseAnim getUseAnimation(ItemStack stack){return drink?UseAnim.DRINK:UseAnim.EAT;}
            }));
        }
    }
    public static void init(){}
    /** Each serving platter keeps a coherent menu; no meat mixed into dessert or fruit. */
    public static ItemStack serving(net.minecraft.util.RandomSource random,int theme){
        Item[] pool=switch(Math.floorMod(theme,4)){
            case 0->new Item[]{Items.COOKED_BEEF,Items.COOKED_CHICKEN,Items.COOKED_PORKCHOP,Items.COOKED_MUTTON,Items.COOKED_RABBIT,Items.COOKED_COD,Items.COOKED_SALMON,FOODS.get("golden_chicken_breast").get(),FOODS.get("sausages").get()};
            case 1->new Item[]{Items.BAKED_POTATO,Items.CARROT,Items.GOLDEN_CARROT,Items.BREAD,FOODS.get("corn_on_the_cob").get(),FOODS.get("sweet_peas").get(),FOODS.get("mashed_potatoes").get()};
            case 2->new Item[]{Items.APPLE,Items.GOLDEN_APPLE,Items.MELON_SLICE};
            default->new Item[]{Items.COOKIE,Items.PUMPKIN_PIE,FOODS.get("strawberry_pancakes").get()};
        };
        return new ItemStack(pool[random.nextInt(pool.length)],2);
    }
    public static boolean drink(ItemStack s){return FOODS.entrySet().stream().anyMatch(e->(e.getKey().endsWith("milkshake")||e.getKey().equals("fruit_punch"))&&s.is(e.getValue().get()));}
    public static ItemStack random(net.minecraft.util.RandomSource random,boolean drink){
        var pool=new ArrayList<Item>();
        if(!drink)Collections.addAll(pool,Items.APPLE,Items.GOLDEN_APPLE,Items.MELON_SLICE,Items.CARROT,Items.GOLDEN_CARROT,Items.BAKED_POTATO,Items.COOKED_BEEF,Items.COOKED_PORKCHOP,Items.COOKED_MUTTON,Items.COOKED_CHICKEN,Items.COOKED_RABBIT,Items.COOKED_COD,Items.COOKED_SALMON,Items.BREAD,Items.COOKIE,Items.PUMPKIN_PIE);
        FOODS.forEach((name,item)->{if((name.endsWith("milkshake")||name.equals("fruit_punch"))==drink)pool.add(item.get());});
        return new ItemStack(pool.get(random.nextInt(pool.size())),drink?1:2+random.nextInt(4));
    }
}
