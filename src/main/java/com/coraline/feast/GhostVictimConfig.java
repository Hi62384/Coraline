package com.coraline.feast;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server tuning for the owner-local ghost director. Registration is performed
 * by the continuation's normal integration, never by the frozen prior release. */
public final class GhostVictimConfig {
    private static final ForgeConfigSpec.Builder B=new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.DoubleValue REGEN=B.comment("Manifestation energy regenerated per second outside mirrors; a positive minimum keeps mandatory abilities recoverable after exhaustion.").defineInRange("manifestationRegeneration",.025,.001,1.);
    public static final ForgeConfigSpec.DoubleValue MIRROR_ENTRY=B.defineInRange("mirrorEntryEnergy",.10,0.,1.);
    public static final ForgeConfigSpec.DoubleValue MIRROR_DRAIN=B.defineInRange("mirrorEnergyPerSecond",.08,.001,1.);
    public static final ForgeConfigSpec.DoubleValue ATTENTION_DECAY=B.defineInRange("beldamAttentionDecayPerSecond",.005,0.,1.);
    public static final ForgeConfigSpec.DoubleValue PUNISHMENT_TRUST_LOSS=B.defineInRange("punishmentGhostTrustLoss",.10,0.,1.);
    public static final ForgeConfigSpec.DoubleValue PUNISHMENT_MOTHER_GAIN=B.defineInRange("punishmentMotherTrustGain",.05,0.,1.);
    public static final ForgeConfigSpec.IntValue PUNISHMENT_SECONDS=B.defineInRange("ghostPunishmentSeconds",60,1,600);
    public static final ForgeConfigSpec.IntValue REBUILD_DELAY_SECONDS=B.defineInRange("reconstructionDelaySeconds",3,1,600);
    public static final ForgeConfigSpec.IntValue REBUILD_SECONDS=B.comment("Owner-local gradual reverse deterioration; shared structures are never deleted.").defineInRange("reconstructionSeconds",60,10,600);
    public static final ForgeConfigSpec.IntValue EXPLORATION_SECONDS=B.defineInRange("victimExplorationSeconds",600,30,600);
    public static final ForgeConfigSpec.IntValue PREPARATION_SECONDS=B.defineInRange("ghostNightPreparationSeconds",45,10,300);
    public static final ForgeConfigSpec.IntValue BEDTIME_SECONDS=B.defineInRange("victimBedtimeConversationSeconds",30,5,60);
    public static final ForgeConfigSpec.IntValue ACTIVITY_SECONDS=B.comment("NPC activity pacing; no ghost physical minigame input. Hard maximum ten minutes.").defineInRange("ghostVictimActivitySeconds",180,30,600);
    public static final ForgeConfigSpec.IntValue GIFT_SECONDS=B.defineInRange("victimGiftOpportunitySeconds",30,10,120);
    public static final ForgeConfigSpec.IntValue POINT_COOLDOWN_SECONDS=B.defineInRange("manifestationPointCooldownSeconds",30,5,600);
    public static final ForgeConfigSpec.DoubleValue SMALL_LIGHT=B.defineInRange("lightFlickerEnergy",.08,0.,1.);
    public static final ForgeConfigSpec.DoubleValue SMALL_OBJECT=B.defineInRange("objectMovementEnergy",.12,0.,1.);
    public static final ForgeConfigSpec.DoubleValue FALLING_OBJECT=B.defineInRange("fallingObjectEnergy",.15,0.,1.);
    public static final ForgeConfigSpec.DoubleValue ILLUSION=B.defineInRange("illusionRevealEnergy",.25,0.,1.);
    public static final ForgeConfigSpec.DoubleValue RESCUED_NEGLECT=B.comment("One-time starting Neglect for a rescued child. Normal care rules apply after adoption.").defineInRange("rescuedChildStartingNeglect",2.,0.,5.);
    public static final ForgeConfigSpec SPEC=B.build();
    public static GhostVictimProgress.Tuning tuning(){return new GhostVictimProgress.Tuning(REGEN.get(),MIRROR_ENTRY.get(),MIRROR_DRAIN.get(),ATTENTION_DECAY.get(),PUNISHMENT_TRUST_LOSS.get(),PUNISHMENT_MOTHER_GAIN.get(),PUNISHMENT_SECONDS.get()*20,SMALL_LIGHT.get(),SMALL_OBJECT.get(),FALLING_OBJECT.get(),ILLUSION.get(),POINT_COOLDOWN_SECONDS.get()*20);}
    private GhostVictimConfig(){}
}
