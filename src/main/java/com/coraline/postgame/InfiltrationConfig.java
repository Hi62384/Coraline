package com.coraline.postgame;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

/** Four lock tiers; existing story Code Knowledge and Mother counters are not
 * rewritten by independent postgame structure tools. */
public final class InfiltrationConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final List<ForgeConfigSpec.IntValue> COSTS=new ArrayList<>(),PINS=new ArrayList<>();
    public static final List<ForgeConfigSpec.DoubleValue> SPEEDS=new ArrayList<>(),WINDOWS=new ArrayList<>();
    static{
        for(int tier=1;tier<=4;tier++){
            B.push("lock_tier_"+tier);COSTS.add(B.defineInRange("durability_cost",tier,1,12));PINS.add(B.defineInRange("pins",2+tier,2,12));
            SPEEDS.add(B.defineInRange("cursor_speed",.035+tier*.012,.02,.15));WINDOWS.add(B.defineInRange("hit_window",.27-tier*.025,.08,.4));B.pop();
        }
    }
    public static final ForgeConfigSpec SPEC=B.build();
    private InfiltrationConfig(){}
}
