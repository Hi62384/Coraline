package com.coraline.clothing;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server config is synchronized to clients for movement prediction and visual abilities. */
public final class ClothingConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue MIDNIGHT_START=B.comment("World ticks, inclusive; wraparound windows are supported.").defineInRange("midnight.start",17000,0,23999);
    public static final ForgeConfigSpec.IntValue MIDNIGHT_END=B.defineInRange("midnight.end",19000,0,23999);
    public static final ForgeConfigSpec.DoubleValue MIDNIGHT_SPEED=B.defineInRange("midnight.speedBonus",.25,0,3);
    public static final ForgeConfigSpec.DoubleValue MIDNIGHT_JUMP=B.comment("Additional upward velocity; vanilla jump is 0.42.").defineInRange("midnight.jumpBonus",.09,0,1);
    public static final ForgeConfigSpec.IntValue NIGHT_LIGHT=B.comment("Skylit surfaces only; does not grant Night Vision or illuminate caves.").defineInRange("midnight.nightVisibility",9,0,15);
    public static final ForgeConfigSpec.DoubleValue RAIN_SPEED=B.defineInRange("raincoat.speedBonus",.65,0,3);
    public static final ForgeConfigSpec.IntValue REGEN_TICKS=B.defineInRange("pajamas.healIntervalTicks",100,20,72000);
    public static final ForgeConfigSpec.DoubleValue REGEN_HEALTH=B.defineInRange("pajamas.healthPerInterval",1.0,0,20);
    public static final ForgeConfigSpec.DoubleValue FOOD_HEAL=B.comment("Health points per nutrition point consumed. Two health points = one heart.").defineInRange("overalls.healthPerNutrition",.5,0,5);
    public static final ForgeConfigSpec.DoubleValue CIRCUS_JUMP=B.defineInRange("circus.jumpBonus",.20,0,1);
    public static final ForgeConfigSpec.DoubleValue VISIBILITY=B.comment("Natural target detection multiplier; does not erase retaliation or existing targets.").defineInRange("slippers.detectionMultiplier",.5,.05,1);
    public static final ForgeConfigSpec.DoubleValue FALL_SPEED=B.defineInRange("featherBoots.maximumFallSpeed",.125,.03,.5);
    public static final ForgeConfigSpec.IntValue STAR_LIGHT=B.comment("Client-side moving light; never places blocks or changes mob spawning.").defineInRange("starShirt.lightLevel",10,1,15);
    public static final ForgeConfigSpec.DoubleValue DREAM_SPEED=B.defineInRange("sets.dreamweave.speedPerPiece",.10,0,1);
    public static final ForgeConfigSpec.DoubleValue SET_JUMP=B.defineInRange("sets.midnight.jumpBonus",.5,0,2);
    public static final ForgeConfigSpec.DoubleValue REFLECT=B.defineInRange("sets.buttonguard.reflectionPerPiece",.10,0,1);
    public static final ForgeConfigSpec.DoubleValue CLOAK_VISIBILITY=B.defineInRange("sets.midnightCloak.nightDetectionMultiplier",.2,.01,1);
    public static final ForgeConfigSpec.IntValue LUCK_LOOTING=B.defineInRange("sets.luck.extraLootingLevels",2,0,10);
    public static final ForgeConfigSpec.DoubleValue VINE_CHANCE=B.defineInRange("sets.gardenVines.retaliationChance",.5,0,1);
    public static final ForgeConfigSpec.IntValue VINE_TICKS=B.defineInRange("sets.gardenVines.effectTicks",80,1,1200);
    public static final ForgeConfigSpec.IntValue MINER_MULTIPLIER=B.comment("Multiplies ordinary block drops, including placed blocks. Never duplicates block entities or tagged items.").defineInRange("sets.miners.dropMultiplier",2,1,8);
    public static final ForgeConfigSpec.DoubleValue SOUL_HIT=B.defineInRange("sets.soulbound.healthOnHit",1.0,0,20);
    public static final ForgeConfigSpec.DoubleValue SOUL_KILL=B.defineInRange("sets.soulbound.healthOnKill",6.0,0,40);
    public static final ForgeConfigSpec.DoubleValue WEB_CHANCE=B.defineInRange("sets.spidervision.webChance",.3,0,1);
    public static final ForgeConfigSpec.IntValue WEB_COOLDOWN=B.defineInRange("sets.spidervision.webCooldownTicks",100,0,1200);
    public static final ForgeConfigSpec.IntValue WEB_LIFETIME=B.defineInRange("sets.spidervision.webLifetimeTicks",200,20,12000);
    public static final ForgeConfigSpec.DoubleValue WEB_SPEED=B.defineInRange("sets.spidervision.webMovementMultiplier",.85,.25,1);
    public static final ForgeConfigSpec.DoubleValue CLIMB_SPEED=B.defineInRange("sets.superSpidervision.climbSpeed",.10,.01,.4);
    public static final ForgeConfigSpec.IntValue HIGHLIGHT_RANGE=B.comment("Zero highlights all client-loaded mobs; an optional positive value caps range. Does not reveal unloaded chunks.").defineInRange("buttonEyes.highlightRange",0,0,1024);
    public static final ForgeConfigSpec SPEC=B.build();
    private ClothingConfig(){}
}
