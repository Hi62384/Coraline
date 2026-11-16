package com.coraline.tycoon;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.*;
public final class TycoonConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue BED_RADIUS,INFLUENCE,PLACEMENT_TICKS,INSPECTION_TICKS,SEVERE,RUNAWAY,FUTURE;
    public static final Map<String,ForgeConfigSpec.IntValue> PRICES=new LinkedHashMap<>();
    static{var b=new ForgeConfigSpec.Builder();BED_RADIUS=b.defineInRange("entranceBedRadius",12,2,32);INFLUENCE=b.defineInRange("householdRadius",16,8,32);PLACEMENT_TICKS=b.defineInRange("placementActiveTicks",72000,1200,1728000);INSPECTION_TICKS=b.defineInRange("inspectionActiveTicks",72000,24000,192000);FUTURE=b.defineInRange("futureAttractionNeglect",50,1,74);SEVERE=b.defineInRange("severeNeglect",75,75,99);RUNAWAY=b.defineInRange("runawayNeglect",100,100,100);String[] ids={"orphan_bed","orphan_food_table","orphan_fridge","water_station","orphan_rug","orphan_lamp","orphan_plant","toy_box","storage_cabinet"};int[] prices={16,6,10,8,3,4,2,6,4};b.push("prices");for(int i=0;i<ids.length;i++)PRICES.put(ids[i],b.defineInRange(ids[i],prices[i],1,4096));b.pop();SPEC=b.build();}
}
