package com.coraline.postgame;

import net.minecraftforge.common.ForgeConfigSpec;

/** Balance inputs for the shared containment state; not an activation switch. */
public final class BreachConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.DoubleValue POWERED=B.comment("Breach increase per 24000 game ticks while Moonstone is available.").defineInRange("powered_increase_per_day",.03,.000001,1);
    public static final ForgeConfigSpec.DoubleValue UNPOWERED=B.comment("Unpowered rate is at least twice the powered rate; lower requests are raised to that minimum.").defineInRange("unpowered_increase_per_day",.12,.000002,2);
    public static final ForgeConfigSpec.IntValue FUEL=B.comment("Fuel game ticks added per real Moonstone. Default: two Minecraft days.").defineInRange("moonstone_fuel_ticks",48000,24000,1440000);
    public static final ForgeConfigSpec SPEC=B.build();
    static BreachState.Rules rules(){int powered=(int)Math.round(POWERED.get()*BreachState.SCALE),unpowered=(int)Math.round(UNPOWERED.get()*BreachState.SCALE);return new BreachState.Rules(powered,Math.max(unpowered,powered*2),FUEL.get());}
    private BreachConfig(){}
}
