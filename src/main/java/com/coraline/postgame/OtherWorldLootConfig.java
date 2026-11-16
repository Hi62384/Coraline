package com.coraline.postgame;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-owned economy settings; no client packet can select a payout. */
public final class OtherWorldLootConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue BUTTON_DROPS=B.define("buttons.enabled",true);
    public static final ForgeConfigSpec.DoubleValue ORDINARY_CHANCE=B.defineInRange("buttons.ordinaryChance",.18,0,1);
    public static final ForgeConfigSpec.DoubleValue THEMED_CHANCE=B.defineInRange("buttons.buttonCreatureChance",.35,0,1);
    public static final ForgeConfigSpec.DoubleValue STRONG_CHANCE=B.defineInRange("buttons.strongEnemyChance",.28,0,1);
    public static final ForgeConfigSpec.DoubleValue LOOTING_BONUS=B.comment("Chance added per Looting level, capped at Looting III. A base chance of zero disables that class, including Looting.")
        .defineInRange("buttons.lootingChancePerLevel",.025,0,.1);
    public static final ForgeConfigSpec SPEC=B.build();
    private OtherWorldLootConfig(){}
}
