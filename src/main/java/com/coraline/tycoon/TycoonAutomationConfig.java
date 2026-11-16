package com.coraline.tycoon;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

public final class TycoonAutomationConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_WORKERS,MAX_CONVERTERS;
    public static final Map<String,ForgeConfigSpec.IntValue> PRICES=new LinkedHashMap<>();
    public static final Map<String,List<ForgeConfigSpec.DoubleValue>> SALARIES=new LinkedHashMap<>();
    public static final List<ForgeConfigSpec.DoubleValue> CONVERTER_MULTIPLIERS=new ArrayList<>();
    public static final Map<String,ForgeConfigSpec.IntValue> OUTPUTS=new LinkedHashMap<>();
    public static final Map<String,ForgeConfigSpec.DoubleValue> OUTPUT_COSTS=new LinkedHashMap<>();
    static{
        var b=new ForgeConfigSpec.Builder();MAX_WORKERS=b.defineInRange("maximumWorkers",48,1,96);MAX_CONVERTERS=b.defineInRange("maximumConverters",32,1,64);
        double[] base={1,2.5,5,9,15},multipliers={1,1.5,2.2,3.2,5};for(int i=0;i<5;i++)CONVERTER_MULTIPLIERS.add(b.defineInRange("converterLevels."+(i+1)+".multiplier",multipliers[i],.1,10.));
        for(var type:TycoonWorkerType.values()){
            PRICES.put(type.id(),b.defineInRange("workers."+type.id()+".hireVoidPower",type.price,1,4096));var salaries=new ArrayList<ForgeConfigSpec.DoubleValue>();
            for(int i=0;i<5;i++)salaries.add(b.defineInRange("workers."+type.id()+".salaryPerMinute."+(i+1),base[i]*type.salaryScale,.01,1000.));SALARIES.put(type.id(),List.copyOf(salaries));
        }
        String[] ids={"oak_log","spruce_log","stone","cobblestone","iron_ingot","gold_ingot","copper_ingot","coal","redstone","lapis_lazuli","emerald","diamond","obsidian","quartz","netherite_scrap"};int[] count={16,16,24,16,8,4,8,8,8,8,4,2,4,8,1};double[] cost={1,1,1,1,2,3,2,2,2,2,6,8,4,3,30};
        for(int i=0;i<ids.length;i++){String key="minecraft:"+ids[i];OUTPUTS.put(key,b.defineInRange("recipes."+ids[i]+".itemsPerMinute",count[i],1,64));OUTPUT_COSTS.put(key,b.defineInRange("recipes."+ids[i]+".essencePerMinute",cost[i],.01,1000.));}
        OUTPUTS.put("minecraft:amethyst_shard",b.defineInRange("recipes.amethyst_shard.itemsPerMinute",8,1,64));OUTPUT_COSTS.put("minecraft:amethyst_shard",b.defineInRange("recipes.amethyst_shard.essencePerMinute",2.,.01,1000.));
        SPEC=b.build();
    }
    private TycoonAutomationConfig(){}
}
