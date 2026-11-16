package com.coraline.tycoon;

import net.minecraftforge.common.ForgeConfigSpec;

/** Phase 2 prices use only physical Void Power; no future soul producers. */
public final class TycoonPhaseTwoConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DOLL, REPLACEMENT, MIN_VISIT, MAX_VISIT;
    public static final ForgeConfigSpec.DoubleValue INITIAL_TRUST;
    public static final ForgeConfigSpec.IntValue[] TERRAIN=new ForgeConfigSpec.IntValue[6],HOUSE=new ForgeConfigSpec.IntValue[6],SKY=new ForgeConfigSpec.IntValue[6];
    static {
        var b=new ForgeConfigSpec.Builder();
        DOLL=b.defineInRange("dollVoidPower",8,1,4096);
        REPLACEMENT=b.defineInRange("replacementDollVoidPower",8,1,4096);
        MIN_VISIT=b.defineInRange("minimumVisitActiveTicks",9600,200,72000);
        MAX_VISIT=b.defineInRange("maximumVisitActiveTicks",18000,200,144000);
        INITIAL_TRUST=b.defineInRange("initialTrustness",.1,0,1);
        int[] terrain={8,16,28,44,68,96},house={0,24,48,80,128,192},sky={0,12,24,40,64,96};
        for(int i=0;i<6;i++){TERRAIN[i]=b.defineInRange("terrainTier"+i,terrain[i],1,8192);HOUSE[i]=b.defineInRange("houseTier"+i,house[i],0,8192);SKY[i]=b.defineInRange("skyTier"+i,sky[i],0,8192);}
        SPEC=b.build();
    }
    private TycoonPhaseTwoConfig(){}
}
