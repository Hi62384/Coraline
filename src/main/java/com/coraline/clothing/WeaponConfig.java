package com.coraline.clothing;
import net.minecraftforge.common.ForgeConfigSpec;
public final class WeaponConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue VINE_TICKS=B.defineInRange("vineSword.poisonAndSlowTicks",80,1,1200);
    public static final ForgeConfigSpec.DoubleValue MACE_RADIUS=B.defineInRange("buttonMace.splashRadius",2.5,.5,6);
    public static final ForgeConfigSpec.DoubleValue MACE_DAMAGE=B.defineInRange("buttonMace.splashDamage",14.0,0,100);
    public static final ForgeConfigSpec.DoubleValue WAND_DAMAGE=B.defineInRange("midnightWand.projectileDamage",5.0,0,100);
    public static final ForgeConfigSpec.IntValue WAND_COOLDOWN=B.comment("40 ticks = 2 seconds. The new key updates existing worlds that used the 50-tick default.").defineInRange("midnightWand.castCooldownTicksV2",40,1,2400);
    public static final ForgeConfigSpec.DoubleValue WHIP_PULL=B.defineInRange("threadWhip.pullStrength",.8,0,3);
    public static final ForgeConfigSpec.IntValue WHIP_COOLDOWN=B.defineInRange("threadWhip.cooldownTicks",20,1,2400);
    public static final ForgeConfigSpec SPEC=B.build();
}
