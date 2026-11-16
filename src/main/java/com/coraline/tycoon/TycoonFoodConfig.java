package com.coraline.tycoon;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

public final class TycoonFoodConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue[] CARE=new ForgeConfigSpec.DoubleValue[5],TRUST=new ForgeConfigSpec.DoubleValue[7];
    public static final ForgeConfigSpec.IntValue[] PRICES=new ForgeConfigSpec.IntValue[8];
    public static final ForgeConfigSpec.DoubleValue SELF,MISSED,OTHER_MISSED;
    public static final ForgeConfigSpec.IntValue COOLDOWN,EPISODE,TABLE,FRIDGE,STARTER,SESSION_REWARD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OVERRIDES;
    static{var b=new ForgeConfigSpec.Builder();String[] names={"excellent","good","normal","poor","terrible"};double[] care={-6,-4,-2,-.5,2};
        for(int i=0;i<5;i++)CARE[i]=b.defineInRange(names[i]+"NeglectChange",care[i],-20,20);
        SELF=b.defineInRange("selfServiceModifier",2.0,0.0,10.0);MISSED=b.defineInRange("missedOverworldMealPenalty",5.0,.1,30.0);OTHER_MISSED=b.defineInRange("missedOtherWorldMealTrustLoss",.01,0.0,.1);
        COOLDOWN=b.defineInRange("mealCooldownActiveTicks",1200,200,24000);EPISODE=b.defineInRange("missedMealEpisodeActiveTicks",2400,600,24000);
        TABLE=b.defineInRange("supperTableVoidPower",10,1,4096);FRIDGE=b.defineInRange("otherFridgeVoidPower",12,1,4096);
        STARTER=b.defineInRange("oneTimeStarterSoulEssence",12,0,100);SESSION_REWARD=b.defineInRange("successfulVisitSoulEssence",3,0,20);
        int[] prices={2,3,5,8,10,14,20,80};double[] trust={.005,.008,.012,.018,.022,.030,.045};
        for(int i=0;i<8;i++){PRICES[i]=b.defineInRange("food"+i+"SoulPrice",prices[i],1,8192);if(i<7)TRUST[i]=b.defineInRange("food"+i+"BaseTrust",trust[i],0,.1);}
        OVERRIDES=b.defineListAllowEmpty("qualityOverrides",List.of("minecraft:golden_carrot=EXCELLENT"),v->v instanceof String s&&s.length()<160&&s.matches("[a-z0-9_.-]+:[a-z0-9_./-]+=(EXCELLENT|GOOD|NORMAL|POOR|TERRIBLE)"));SPEC=b.build();
    }
    private TycoonFoodConfig(){}
}
