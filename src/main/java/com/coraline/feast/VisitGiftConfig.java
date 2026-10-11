package com.coraline.feast;

import java.util.*;
import net.minecraftforge.common.ForgeConfigSpec;

/** Relative gift weights; this config does not change activity selection or loot. */
public final class VisitGiftConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final Map<OtherMotherDialogue.Loot,ForgeConfigSpec.IntValue> WEIGHTS=new EnumMap<>(OtherMotherDialogue.Loot.class);
    public static final ForgeConfigSpec.BooleanValue EQUAL_CATEGORY_CHANCES;
    static {
        EQUAL_CATEGORY_CHANCES=B.comment("Give every enabled visit-gift category equal chances, including with old saved weights. Set false only to opt into custom weighted odds.").define("equal_category_chances",true);
        B.comment("Zero disables a category. Positive values are equally likely when equal_category_chances is true; otherwise these are relative weights. All zero falls back to Food and Treasure.").push("weights");
        for(var kind:OtherMotherDialogue.Loot.values())WEIGHTS.put(kind,B.defineInRange(kind.id(),kind.defaultWeight,0,10000));
        B.pop();
    }
    public static final ForgeConfigSpec SPEC=B.build();
    private VisitGiftConfig(){}
}
