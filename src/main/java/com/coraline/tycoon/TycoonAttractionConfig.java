package com.coraline.tycoon;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

/** Server balance data shared by construction, activities and their previews. */
public final class TycoonAttractionConfig {
    public static final ForgeConfigSpec SPEC;
    public static final Map<String,ForgeConfigSpec.IntValue> PRICES=new LinkedHashMap<>();
    public static final Map<String,List<ForgeConfigSpec.DoubleValue>> PROFILES=new LinkedHashMap<>();
    public static final ForgeConfigSpec.IntValue MIN_ACTIVITY,MAX_ACTIVITY,GIFT_COOLDOWN,TRACK_TICKS;
    public static final ForgeConfigSpec.DoubleValue REFUND,WANDER_SCALE;
    public static final List<ForgeConfigSpec.DoubleValue> TRUST=new ArrayList<>(),SUSPICION=new ArrayList<>();
    static {
        var b=new ForgeConfigSpec.Builder();
        MIN_ACTIVITY=b.defineInRange("minimumActivityTicks",2400,1200,12000);
        MAX_ACTIVITY=b.defineInRange("maximumActivityTicks",6000,2400,18000);
        GIFT_COOLDOWN=b.defineInRange("giftCooldownTicks",2400,200,24000);
        TRACK_TICKS=b.defineInRange("spyTrackingTicks",500,100,1200);
        REFUND=b.defineInRange("saleRefundFraction",.60,0.,.75);
        WANDER_SCALE=b.defineInRange("curiosityWanderMultiplier",1.,0.,2.);
        double[] t={-.015,-.005,.003,.015,.025},s={.025,.010,0.,-.005,-.010};
        for(int i=0;i<5;i++){TRUST.add(b.defineInRange("outcomes."+i+".trust",t[i],-.1,.1));SUSPICION.add(b.defineInRange("outcomes."+i+".suspicion",s[i],-.1,.1));}
        for(var type:TycoonAttractionType.values()){
            PRICES.put(type.id(),b.defineInRange("structures."+type.id()+".voidPower",type.price,1,4096));
            var profile=new ArrayList<ForgeConfigSpec.DoubleValue>();
            for(int i=0;i<TycoonAttractionType.INTERESTS.length;i++)profile.add(b.defineInRange("structures."+type.id()+"."+TycoonAttractionType.INTERESTS[i],type.profile[i],0.,1.));
            PROFILES.put(type.id(),List.copyOf(profile));
        }
        SPEC=b.build();
    }
    private TycoonAttractionConfig(){}
}
