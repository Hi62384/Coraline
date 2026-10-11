package com.coraline.feast;
import net.minecraftforge.common.ForgeConfigSpec;
public final class VisitConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue FEAST_TICKS=B.defineInRange("feastTicks",1200,100,12000);
    public static final ForgeConfigSpec.IntValue SUPPER_VOICE_GAP=B.comment("Pause between supper dialogue recordings; supper extends if needed to finish them.").defineInRange("supperDialogueGapTicks",100,20,600);
    public static final ForgeConfigSpec.IntValue REFILL_TICKS=B.defineInRange("servingRefillMinimumTicks",300,100,2400);
    public static final ForgeConfigSpec.IntValue REFILL_JITTER=B.defineInRange("servingRefillRandomExtraTicks",200,0,2400);
    public static final ForgeConfigSpec.IntValue CAKE_WAIT=B.defineInRange("cakeRevealTicks",60,20,1200);
    public static final ForgeConfigSpec.DoubleValue PIANO_RANGE=B.defineInRange("pianoSceneRadius",6.0,4,12);
    public static final ForgeConfigSpec.IntValue PIANO_MIN=B.defineInRange("pianoStagingMinimumDistance",40,10,80);
    public static final ForgeConfigSpec.IntValue PIANO_MAX=B.defineInRange("pianoStagingMaximumDistance",72,18,100);
    public static final ForgeConfigSpec.IntValue STARTUP_RETRY=B.defineInRange("startupRetryTicks",100,20,1200);
    public static final ForgeConfigSpec.IntValue SEAT_STALL_TICKS=B.defineInRange("supperSeatStallRecoveryTicks",200,40,1200);
    public static final ForgeConfigSpec.IntValue SEAT_TRAVEL_TICKS=B.comment("Maximum walk to supper before safe seat recovery; capped at 20 seconds.").defineInRange("supperSeatMaximumTravelTicks",400,100,400);
    public static final ForgeConfigSpec.IntValue PLACEMENT_ATTEMPTS=B.defineInRange("placementSearchAttempts",512,64,2048);
    public static final ForgeConfigSpec.IntValue PLACEMENT_HEIGHT=B.defineInRange("placementVerticalRange",16,2,48);
    public static final ForgeConfigSpec.DoubleValue MUSIC_VOLUME=B.defineInRange("musicVolume",.65,0,1);
    public static final ForgeConfigSpec.DoubleValue VOICE_VOLUME=B.defineInRange("voiceVolume",1.0,0,2);
    public static final ForgeConfigSpec SPEC=B.build();
}
