package com.coraline.food;

import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;

public final class FoodConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    private record Entry(ForgeConfigSpec.IntValue hunger,ForgeConfigSpec.DoubleValue saturation){}
    private static final Map<String,Entry> VALUES=new HashMap<>();
    static {
        add("omelet",7,.6);add("bacon_and_eggs",9,.7);add("sweet_strawberries",3,.3);add("scrambled_eggs",5,.5);
        add("muffin",5,.4);add("pizza",8,.65);add("cupcakes",5,.3);add("lemonade",3,.15);add("orange_juice",4,.25);
        add("hot_dog_in_bun",8,.6);add("panini",8,.7);add("cocoa_beetles",3,.3);add("lollipop",2,.1);
        add("perfect_roast_dinner",16,1);add("cotton_candy",2,.1);add("bottomless_candy_bag",2,.01);add("popcorn",1,.05);
    }
    private static void add(String id,int hunger,double saturation){VALUES.put(id,new Entry(B.comment("Food points; two points = one drumstick. Per bite for multi-use foods.").defineInRange(id+".nutrition",hunger,0,40),B.defineInRange(id+".saturationMultiplier",saturation,0,3)));}
    public static final ForgeConfigSpec.IntValue POPCORN_SERVINGS=B.defineInRange("popcorn.servings",32,1,256);
    public static final ForgeConfigSpec.IntValue POPCORN_INTERVAL=B.defineInRange("popcorn.ticksBetweenBites",10,2,100);
    public static final ForgeConfigSpec SPEC=B.build();
    public static FoodProperties food(String id){var entry=VALUES.get(id);return new FoodProperties.Builder().nutrition(entry.hunger.get()).saturationMod(entry.saturation.get().floatValue()).build();}
}
