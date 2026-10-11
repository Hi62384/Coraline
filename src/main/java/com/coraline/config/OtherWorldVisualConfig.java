package com.coraline.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only beautiful facade. No progression, gameplay or global video settings. */
public final class OtherWorldVisualConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.BooleanValue ENABLED=B.define("enabled",true);
    public static final ForgeConfigSpec.DoubleValue SATURATION=B.comment("Vivid Other World color richness; 1 disables the pass. Replaces the older palette.saturation setting so existing worlds receive the stronger facade.").defineInRange("palette.facadeSaturation",2.15,1,3);
    public static final ForgeConfigSpec.DoubleValue MOON_SIZE=B.defineInRange("sky.moonScale",1.55,.5,3);
    public static final ForgeConfigSpec.DoubleValue STAR_INTENSITY=B.defineInRange("sky.starIntensity",.85,0,2);
    public static final ForgeConfigSpec.DoubleValue TWINKLE=B.defineInRange("sky.twinkle",.09,0,.3);
    public static final ForgeConfigSpec.BooleanValue METEORS=B.define("sky.shootingStars",true);
    public static final ForgeConfigSpec.DoubleValue AURORA=B.defineInRange("sky.auroraIntensity",.075,0,.3);
    public static final ForgeConfigSpec.IntValue ZENITH=B.defineInRange("palette.zenith",0x101E49,0,0xFFFFFF);
    public static final ForgeConfigSpec.IntValue HORIZON=B.defineInRange("palette.horizon",0x455C87,0,0xFFFFFF);
    public static final ForgeConfigSpec.IntValue FOG=B.defineInRange("palette.haze",0x455C87,0,0xFFFFFF);
    public static final ForgeConfigSpec.IntValue WATER_FOG=B.defineInRange("palette.underwater",0x183D60,0,0xFFFFFF);
    public static final ForgeConfigSpec.DoubleValue MOONLIGHT=B.defineInRange("light.moonlightStrength",.53,0,.8);
    public static final ForgeConfigSpec.DoubleValue WARMTH=B.defineInRange("light.lampWarmth",.10,0,.25);
    public static final ForgeConfigSpec.DoubleValue FOG_START=B.defineInRange("fog.startFraction",.43,.15,.9);
    public static final ForgeConfigSpec.IntValue FOG_END=B.defineInRange("fog.distance",180,48,320);
    public static final ForgeConfigSpec.IntValue WATER_DISTANCE=B.defineInRange("fog.underwaterDistance",44,12,96);
    public static final ForgeConfigSpec.IntValue NATURE_RADIUS=B.defineInRange("nature.radius",36,12,64);
    public static final ForgeConfigSpec.IntValue SCAN_BUDGET=B.defineInRange("nature.columnsPerTick",12,2,48);
    public static final ForgeConfigSpec.IntValue DECOR_CAP=B.defineInRange("nature.maximumDecorations",220,0,600);
    public static final ForgeConfigSpec.DoubleValue FLOWERS=B.defineInRange("nature.flowerClusterDensity",.32,0,1);
    public static final ForgeConfigSpec.IntValue MOTES=B.defineInRange("nature.maximumMotes",100,0,250);
    public static final ForgeConfigSpec.DoubleValue FIREFLIES=B.defineInRange("nature.fireflyDensity",.7,0,1);
    public static final ForgeConfigSpec SPEC=B.build();
    public static float red(int color){return ((color>>16)&255)/255F;}
    public static float green(int color){return ((color>>8)&255)/255F;}
    public static float blue(int color){return (color&255)/255F;}
    private OtherWorldVisualConfig(){}
}
