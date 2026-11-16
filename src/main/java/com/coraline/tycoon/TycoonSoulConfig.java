package com.coraline.tycoon;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

public final class TycoonSoulConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue REJECT_TRUST,REJECT_SUSPICION,BASE_MIN,BASE_MAX;
    public static final ForgeConfigSpec.IntValue PROPOSAL_COOLDOWN,FARM_POWER,REPAIR_POWER;
    public static final List<ForgeConfigSpec.IntValue> CAPACITY=new ArrayList<>();
    public static final List<ForgeConfigSpec.DoubleValue> MULTIPLIER=new ArrayList<>();
    static{var b=new ForgeConfigSpec.Builder();REJECT_TRUST=b.defineInRange("proposalRejectionTrustLoss",.35,.1,1.);REJECT_SUSPICION=b.defineInRange("proposalRejectionSuspicionGain",.40,.1,1.);PROPOSAL_COOLDOWN=b.defineInRange("proposalCooldownTicks",24000,1200,96000);BASE_MIN=b.defineInRange("baseProductionMinimum",.015,.001,.1);BASE_MAX=b.defineInRange("baseProductionMaximum",.050,.001,.2);FARM_POWER=b.defineInRange("farmConstructionVoidPower",32,1,4096);REPAIR_POWER=b.defineInRange("farmRepairVoidPowerPerTier",8,1,256);double[] m={1,1.4,1.9,2.5,3.2};for(int i=1;i<=5;i++){CAPACITY.add(b.defineInRange("tiers."+i+".capacity",i,1,5));MULTIPLIER.add(b.defineInRange("tiers."+i+".productionMultiplier",m[i-1],.1,10.));}SPEC=b.build();}
    private TycoonSoulConfig(){}
}
