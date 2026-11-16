package com.coraline.clothing;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PerfectPowerConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.DoubleValue MINING=B.defineInRange("powered_mining_multiplier",1.6,1,8);
    public static final ForgeConfigSpec.DoubleValue SPEED=B.defineInRange("powered_attack_speed_bonus",.35,0,2);
    public static final ForgeConfigSpec.DoubleValue HOSTILE_DAMAGE=B.defineInRange("other_world_damage_multiplier",1.4,1,4);
    public static final ForgeConfigSpec.DoubleValue ARMOR_REDUCTION=B.defineInRange("supernatural_reduction_per_armor_piece",.05,0,.15);
    public static final ForgeConfigSpec.IntValue COOLDOWN=B.defineInRange("ability_cooldown_ticks",80,20,1200);
    public static final ForgeConfigSpec.IntValue BARRIER_COOLDOWN=B.defineInRange("barrier_cooldown_ticks",600,100,6000);
    public static final ForgeConfigSpec.IntValue BARRIER_DURATION=B.defineInRange("barrier_duration_ticks",100,20,400);
    public static final ForgeConfigSpec SPEC=B.build();
    private PerfectPowerConfig(){}
}
