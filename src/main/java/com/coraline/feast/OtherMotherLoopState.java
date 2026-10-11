package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Family-scoped, durable progress. Rewards are authoritative here, never in decorative entities. */
public final class OtherMotherLoopState {
    public enum Phase { NONE, WELCOME, BEFORE, GIFTS, AFTER, EXTRA, TUTORIAL, PROPOSAL, WARNING, WINDOW, EXPLORING, ARRIVAL, READY, ACTIVITY, COMPLETE }
    public com.coraline.secrets.SecretsState secrets=new com.coraline.secrets.SecretsState();
    public CompoundTag ocean=new CompoundTag();
    public CompoundTag festival=new CompoundTag(),temple=new CompoundTag(),endCity=new CompoundTag();
    public CompoundTag expedition=new CompoundTag();
    public int visits, generation;
    public float trust, suspicion;
    public Phase phase=Phase.NONE;
    public boolean giftPrepared;
    public String giftBefore="",giftAfter="";
    public long giftRevealUntil;
    public OtherMotherDialogue.Loot activityLoot=OtherMotherDialogue.Loot.FOOD;
    public OtherMotherDialogue.Loot activityLoot(){return loot.expanded()?activityLoot:loot;}
    public OtherMotherDialogue.Loot loot=OtherMotherDialogue.Loot.FOOD;
    public OtherMotherActivities.Activity activity=OtherMotherActivities.Activity.OTHER_VILLAGE;
    public int villageTokensRemaining;
    public long villageGiftAt;
    public boolean villageIssued;
    public boolean villageSpawned;
    public int archeryShotsRemaining;
    public long archeryGiftAt;
    public boolean archeryIssued;
    /** Cake contest stages: 0 speech, 1 walk, 2 countdown, 3 bake, 4 carry, 5 sit, 6 judge, 7 announce, 8 complete. */
    public int cakeStage,cakeStep,cakeScore,cakeMistakes,cakeRank,cakeAnnounceIndex;
    public int cakeIngredients,cakeMixCount,cakeFlavor=-1,cakeIcing=-1,cakeTopper=-1;
    public long cakeOvenReadyAt;
    /** Player-owned recipe. The legacy flavor/icing/topper fields remain for old saves. */
    public int cakeBase=-1,cakeFilling=-1,cakeFrosting=-1,cakeLayers=2,cakeToppings,cakePiping=-1,cakeExtra=-1;
    public int cakeMixQuality,cakeBakeQuality;
    public int cakeFrostCoverage,cakeFrostQuality,cakeDecorationSlots;
    public long cakeMixStartedAt,cakeOvenStartedAt;
    public long cakeVoiceUntil,cakeCountdownUntil,cakeBakeUntil,cakeStepAt,cakeJudgeUntil,cakeAnnounceAt,cakeSeed,cakeLastActionAt;
    public boolean cakeTaken,cakeSubmitted;
    public int carnivalTokensRemaining,carnivalBooth=-1,carnivalProgress,carnivalFailures,carnivalTarget;
    public long carnivalGiftAt,carnivalGameUntil,carnivalRoundAt,carnivalLastActionAt,carnivalSeed;
    public int carnivalRide=-1;
    public long carnivalRideUntil;
    public boolean carnivalIssued;
    public long parkourVoiceUntil,parkourEndAt,parkourSeed;
    public boolean parkourRewarded;
    public long gardenVoiceUntil,gardenEndAt;
    public boolean gardenFinished,gardenEligible;
    public int gardenClaimMask;
    public long feastVoiceUntil,feastEndAt;
    public boolean feastFinished;
    public final int[] feastPlateFood=new int[16];
    public final long[] feastPlateRefillAt=new long[16];
    public long feastNextWaveAt;
    public long feastCartAt,feastGuestCanEatAt;
    public int feastWave,feastWaveKind,feastBasketCount,feastCartSlot;
    public final int[] feastBasketFood=new int[16];
    /** Fishing contest: 0 voice, 1 lake, 2 walk to judges, 3 judging, 4 results, 5 complete. */
    public int fishingStage,fishingScore,fishingCatches,fishingRank,fishingAnnounceIndex;
    public final int[] fishingRivalScores=new int[4];
    public long fishingVoiceUntil,fishingEndAt,fishingWalkUntil,fishingJudgeUntil,fishingAnnounceAt,fishingLastNpcAt,fishingSeed;
    /** Ice boat race: 0 introduction, 1 countdown, 2 racing, 3 result/exit. */
    public int iceStage,iceLap,iceCheckpoint,iceRank;
    public long iceVoiceUntil,iceCountdownUntil,iceResultAt,iceSeed;
    public boolean iceRewarded;
    public final double[] iceRivalProgress=new double[6];
    public final int[] iceRivalFinish=new int[6];
    /** Magic show: 0 introduction, 1 opening, 2 memorize, 3 closing,
     * 4 reopening, 5 answer, 6 feedback, 7 prize counter, 8 complete, 9 interlude. */
    public int magicStage,magicRound,magicTokens,magicAnswer;
    public long magicSeed,magicVoiceUntil,magicPhaseUntil;
    /** Magic garden: 0 intro, 1 boarding, 2 ascent, 3 reveal, 4 descent, 5 harvest, 6 done. */
    public int flyingGardenStage;
    public long flyingGardenVoiceUntil,flyingGardenPhaseUntil,flyingGardenHarvestUntil,flyingGardenCollectedMask;
    public int flyingGardenHarvestCount;
    public final long[] flyingGardenRegrowAt=new long[60];
    public final int[] flyingGardenPickCount=new int[60];
    /** Mice circus: speech, seven acts, finale; wager is escrowed until the result. */
    public long circusVoiceUntil,circusActAt,circusSeed;
    public int circusAct,circusSelected=-1,circusBetMouse=-1,circusBetState;
    public final int[] circusScores=new int[6];
    public ItemStack circusWager=ItemStack.EMPTY;
    public int circusPhase,circusBookieMouse=-1;
    public long circusPhaseUntil;
    public boolean circusGifted;
    public boolean extra, opened, late;
    public long deadline, arrivalDeadline;
    public BlockPos portal=BlockPos.ZERO;
    public BlockPos motherPos=BlockPos.ZERO;
    public final List<ItemStack> gifts=new ArrayList<>();
    public boolean active(){return phase!=Phase.NONE;}
    public boolean canLeave(){return (phase==Phase.WINDOW||phase==Phase.EXPLORING)&&!secrets.searching();}
    public boolean collected(){return gifts.stream().allMatch(ItemStack::isEmpty);}
    public static float clamp(float n){return Float.isFinite(n)?Math.max(0,Math.min(1,n)):0;}
    public static OtherMotherWelcome.State classify(float trust,float suspicion){
        trust=clamp(trust);suspicion=clamp(suspicion);
        if(trust>=.35F&&trust<=.65F&&suspicion>=.35F&&suspicion<=.65F)return OtherMotherWelcome.State.MID_TRUST_MID_SUSPICION;
        if(trust<.5F)return suspicion<.5F?OtherMotherWelcome.State.LOW_TRUST_LOW_SUSPICION:OtherMotherWelcome.State.HIGH_SUSPICION_LOW_TRUST;
        return suspicion<.5F?OtherMotherWelcome.State.HIGH_TRUST_LOW_SUSPICION:OtherMotherWelcome.State.HIGH_TRUST_HIGH_SUSPICION;
    }
    public CompoundTag save(){
        // Activity-specific state travels in the same durable family ledger.
        var n=new CompoundTag();n.put("Secrets",secrets.save());n.put("Ocean",ocean.copy());n.putInt("Visits",visits);n.putInt("Generation",generation);n.putFloat("Trust",trust);n.putFloat("Suspicion",suspicion);
        n.put("Festival",festival.copy());n.put("Temple",temple.copy());n.put("EndCity",endCity.copy());n.put("Expedition",expedition.copy());
        n.putBoolean("GiftPrepared",giftPrepared);n.putString("GiftBefore",giftBefore);n.putString("GiftAfter",giftAfter);n.putLong("GiftRevealUntil",giftRevealUntil);n.putString("ActivityLoot",activityLoot.name());
        n.putString("Phase",phase.name());n.putString("Loot",loot.name());n.putString("Activity",activity.name());
        n.putInt("VillageTokensRemaining",villageTokensRemaining);n.putLong("VillageGiftAt",villageGiftAt);n.putBoolean("VillageIssued",villageIssued);n.putBoolean("VillageSpawned",villageSpawned);
        n.putInt("ArcheryShotsRemaining",archeryShotsRemaining);n.putLong("ArcheryGiftAt",archeryGiftAt);n.putBoolean("ArcheryIssued",archeryIssued);
        n.putInt("CakeStage",cakeStage);n.putInt("CakeStep",cakeStep);n.putInt("CakeScore",cakeScore);n.putInt("CakeMistakes",cakeMistakes);n.putInt("CakeRank",cakeRank);n.putInt("CakeAnnounceIndex",cakeAnnounceIndex);
        n.putInt("CakeIngredients",cakeIngredients);n.putInt("CakeMixCount",cakeMixCount);n.putInt("CakeFlavor",cakeFlavor);n.putInt("CakeIcing",cakeIcing);n.putInt("CakeTopper",cakeTopper);n.putLong("CakeOvenReadyAt",cakeOvenReadyAt);
        n.putInt("CakeBase",cakeBase);n.putInt("CakeFilling",cakeFilling);n.putInt("CakeFrosting",cakeFrosting);n.putInt("CakeLayers",cakeLayers);
        n.putInt("CakeToppings",cakeToppings);n.putInt("CakePiping",cakePiping);n.putInt("CakeExtra",cakeExtra);
        n.putInt("CakeMixQuality",cakeMixQuality);n.putInt("CakeBakeQuality",cakeBakeQuality);
        n.putInt("CakeFrostCoverage",cakeFrostCoverage);n.putInt("CakeFrostQuality",cakeFrostQuality);n.putInt("CakeDecorationSlots",cakeDecorationSlots);
        n.putLong("CakeMixStartedAt",cakeMixStartedAt);n.putLong("CakeOvenStartedAt",cakeOvenStartedAt);
        n.putLong("CakeVoiceUntil",cakeVoiceUntil);n.putLong("CakeCountdownUntil",cakeCountdownUntil);n.putLong("CakeBakeUntil",cakeBakeUntil);
        n.putLong("CakeStepAt",cakeStepAt);n.putLong("CakeJudgeUntil",cakeJudgeUntil);n.putLong("CakeAnnounceAt",cakeAnnounceAt);
        n.putLong("CakeSeed",cakeSeed);n.putLong("CakeLastActionAt",cakeLastActionAt);n.putBoolean("CakeTaken",cakeTaken);n.putBoolean("CakeSubmitted",cakeSubmitted);
        n.putInt("CarnivalTokensRemaining",carnivalTokensRemaining);n.putInt("CarnivalBooth",carnivalBooth);n.putInt("CarnivalProgress",carnivalProgress);
        n.putInt("CarnivalFailures",carnivalFailures);n.putInt("CarnivalTarget",carnivalTarget);n.putLong("CarnivalGiftAt",carnivalGiftAt);
        n.putLong("CarnivalGameUntil",carnivalGameUntil);n.putLong("CarnivalRoundAt",carnivalRoundAt);n.putLong("CarnivalLastActionAt",carnivalLastActionAt);
        n.putLong("CarnivalSeed",carnivalSeed);n.putBoolean("CarnivalIssued",carnivalIssued);
        n.putInt("CarnivalRide",carnivalRide);n.putLong("CarnivalRideUntil",carnivalRideUntil);
        n.putLong("ParkourVoiceUntil",parkourVoiceUntil);n.putLong("ParkourEndAt",parkourEndAt);n.putLong("ParkourSeed",parkourSeed);n.putBoolean("ParkourRewarded",parkourRewarded);
        n.putLong("GardenVoiceUntil",gardenVoiceUntil);n.putLong("GardenEndAt",gardenEndAt);n.putBoolean("GardenFinished",gardenFinished);
        n.putBoolean("GardenEligible",gardenEligible);n.putInt("GardenClaimMask",gardenClaimMask);
        n.putLong("FeastVoiceUntil",feastVoiceUntil);n.putLong("FeastEndAt",feastEndAt);n.putBoolean("FeastFinished",feastFinished);
        n.putIntArray("FeastPlateFood",feastPlateFood);n.putLongArray("FeastPlateRefillAt",feastPlateRefillAt);
        n.putLong("FeastNextWaveAt",feastNextWaveAt);n.putInt("FeastWave",feastWave);n.putInt("FeastWaveKind",feastWaveKind);
        n.putLong("FeastCartAt",feastCartAt);n.putLong("FeastGuestCanEatAt",feastGuestCanEatAt);n.putInt("FeastCartSlot",feastCartSlot);
        n.putInt("FeastBasketCount",feastBasketCount);n.putIntArray("FeastBasketFood",feastBasketFood);
        n.putInt("FishingStage",fishingStage);n.putInt("FishingScore",fishingScore);n.putInt("FishingCatches",fishingCatches);
        n.putInt("FishingRank",fishingRank);n.putInt("FishingAnnounceIndex",fishingAnnounceIndex);n.putIntArray("FishingRivalScores",fishingRivalScores);
        n.putLong("FishingVoiceUntil",fishingVoiceUntil);n.putLong("FishingEndAt",fishingEndAt);n.putLong("FishingWalkUntil",fishingWalkUntil);
        n.putLong("FishingJudgeUntil",fishingJudgeUntil);n.putLong("FishingAnnounceAt",fishingAnnounceAt);
        n.putLong("FishingLastNpcAt",fishingLastNpcAt);n.putLong("FishingSeed",fishingSeed);
        n.putInt("IceStage",iceStage);n.putInt("IceLap",iceLap);n.putInt("IceCheckpoint",iceCheckpoint);n.putInt("IceRank",iceRank);
        n.putLong("IceVoiceUntil",iceVoiceUntil);n.putLong("IceCountdownUntil",iceCountdownUntil);n.putLong("IceResultAt",iceResultAt);n.putLong("IceSeed",iceSeed);
        n.putBoolean("IceRewarded",iceRewarded);var iceProgressBits=new long[iceRivalProgress.length];
        for(int i=0;i<iceProgressBits.length;i++)iceProgressBits[i]=Double.doubleToLongBits(iceRivalProgress[i]);
        n.putLongArray("IceRivalProgress",iceProgressBits);n.putIntArray("IceRivalFinish",iceRivalFinish);
        n.putInt("MagicStage",magicStage);n.putInt("MagicRound",magicRound);n.putInt("MagicTokens",magicTokens);n.putInt("MagicAnswer",magicAnswer);
        n.putLong("MagicSeed",magicSeed);n.putLong("MagicVoiceUntil",magicVoiceUntil);n.putLong("MagicPhaseUntil",magicPhaseUntil);
        n.putInt("FlyingGardenStage",flyingGardenStage);n.putLong("FlyingGardenVoiceUntil",flyingGardenVoiceUntil);
        n.putLong("FlyingGardenPhaseUntil",flyingGardenPhaseUntil);n.putLong("FlyingGardenHarvestUntil",flyingGardenHarvestUntil);
        n.putLong("FlyingGardenCollectedMask",flyingGardenCollectedMask);
        n.putInt("FlyingGardenHarvestCount",flyingGardenHarvestCount);
        n.putLongArray("FlyingGardenRegrowAt",flyingGardenRegrowAt);
        n.putIntArray("FlyingGardenPickCount",flyingGardenPickCount);
        n.putLong("CircusVoiceUntil",circusVoiceUntil);n.putLong("CircusActAt",circusActAt);n.putLong("CircusSeed",circusSeed);
        n.putInt("CircusAct",circusAct);n.putInt("CircusSelected",circusSelected);n.putInt("CircusBetMouse",circusBetMouse);n.putInt("CircusBetState",circusBetState);
        n.putIntArray("CircusScores",circusScores);n.put("CircusWager",circusWager.save(new CompoundTag()));
        n.putInt("CircusPhase",circusPhase);n.putInt("CircusBookieMouse",circusBookieMouse);n.putLong("CircusPhaseUntil",circusPhaseUntil);n.putBoolean("CircusGifted",circusGifted);
        n.putBoolean("Extra",extra);n.putBoolean("Opened",opened);n.putBoolean("Late",late);n.putLong("Deadline",deadline);n.putLong("ArrivalDeadline",arrivalDeadline);n.putLong("Portal",portal.asLong());n.putLong("MotherPosition",motherPos.asLong());
        var items=new ListTag();for(var item:gifts)items.add(item.save(new CompoundTag()));n.put("Gifts",items);return n;
    }
    public static OtherMotherLoopState load(CompoundTag n){
        var s=new OtherMotherLoopState();s.secrets=com.coraline.secrets.SecretsState.load(n.getCompound("Secrets"));s.visits=n.getInt("Visits");s.generation=n.getInt("Generation");s.trust=clamp(n.getFloat("Trust"));s.suspicion=clamp(n.getFloat("Suspicion"));
        try{s.phase=Phase.valueOf(n.getString("Phase"));s.loot=OtherMotherDialogue.Loot.valueOf(n.getString("Loot"));s.activity=OtherMotherActivities.Activity.valueOf(n.getString("Activity"));}catch(IllegalArgumentException ignored){s.phase=Phase.NONE;}
        s.giftPrepared=n.getBoolean("GiftPrepared")||s.phase==Phase.GIFTS||s.phase==Phase.AFTER||!n.getList("Gifts",10).isEmpty();
        s.giftBefore=n.getString("GiftBefore");s.giftAfter=n.getString("GiftAfter");s.giftRevealUntil=n.getLong("GiftRevealUntil");
        try{s.activityLoot=OtherMotherDialogue.Loot.valueOf(n.getString("ActivityLoot"));}catch(IllegalArgumentException ignored){s.activityLoot=s.loot.expanded()?OtherMotherDialogue.Loot.TREASURE:s.loot;}
        s.villageTokensRemaining=n.getInt("VillageTokensRemaining");s.villageGiftAt=n.getLong("VillageGiftAt");s.villageIssued=n.getBoolean("VillageIssued");s.villageSpawned=n.getBoolean("VillageSpawned");
        s.archeryShotsRemaining=n.getInt("ArcheryShotsRemaining");s.archeryGiftAt=n.getLong("ArcheryGiftAt");s.archeryIssued=n.getBoolean("ArcheryIssued");
        s.cakeStage=n.getInt("CakeStage");s.cakeStep=n.getInt("CakeStep");s.cakeScore=n.getInt("CakeScore");s.cakeMistakes=n.getInt("CakeMistakes");s.cakeRank=n.getInt("CakeRank");s.cakeAnnounceIndex=n.getInt("CakeAnnounceIndex");
        s.cakeIngredients=n.getInt("CakeIngredients");s.cakeMixCount=n.getInt("CakeMixCount");s.cakeFlavor=n.contains("CakeFlavor")?n.getInt("CakeFlavor"):-1;
        s.cakeIcing=n.contains("CakeIcing")?n.getInt("CakeIcing"):-1;s.cakeTopper=n.contains("CakeTopper")?n.getInt("CakeTopper"):-1;s.cakeOvenReadyAt=n.getLong("CakeOvenReadyAt");
        s.cakeBase=n.contains("CakeBase")?n.getInt("CakeBase"):s.cakeFlavor;
        s.cakeFilling=n.contains("CakeFilling")?n.getInt("CakeFilling"):-1;
        s.cakeFrosting=n.contains("CakeFrosting")?n.getInt("CakeFrosting"):s.cakeIcing;
        s.cakeLayers=n.contains("CakeLayers")?n.getInt("CakeLayers"):2;
        s.cakeToppings=n.getInt("CakeToppings");s.cakePiping=n.contains("CakePiping")?n.getInt("CakePiping"):-1;s.cakeExtra=n.contains("CakeExtra")?n.getInt("CakeExtra"):-1;
        s.cakeMixQuality=n.getInt("CakeMixQuality");s.cakeBakeQuality=n.getInt("CakeBakeQuality");
        s.cakeFrostCoverage=n.getInt("CakeFrostCoverage");s.cakeFrostQuality=n.getInt("CakeFrostQuality");s.cakeDecorationSlots=n.getInt("CakeDecorationSlots");
        s.cakeMixStartedAt=n.getLong("CakeMixStartedAt");s.cakeOvenStartedAt=n.getLong("CakeOvenStartedAt");
        s.cakeVoiceUntil=n.getLong("CakeVoiceUntil");s.cakeCountdownUntil=n.getLong("CakeCountdownUntil");s.cakeBakeUntil=n.getLong("CakeBakeUntil");
        s.cakeStepAt=n.getLong("CakeStepAt");s.cakeJudgeUntil=n.getLong("CakeJudgeUntil");s.cakeAnnounceAt=n.getLong("CakeAnnounceAt");
        s.cakeSeed=n.getLong("CakeSeed");s.cakeLastActionAt=n.getLong("CakeLastActionAt");s.cakeTaken=n.getBoolean("CakeTaken");s.cakeSubmitted=n.getBoolean("CakeSubmitted");
        s.carnivalTokensRemaining=n.getInt("CarnivalTokensRemaining");s.carnivalBooth=n.contains("CarnivalBooth")?n.getInt("CarnivalBooth"):-1;
        s.carnivalProgress=n.getInt("CarnivalProgress");s.carnivalFailures=n.getInt("CarnivalFailures");s.carnivalTarget=n.getInt("CarnivalTarget");
        s.carnivalGiftAt=n.getLong("CarnivalGiftAt");s.carnivalGameUntil=n.getLong("CarnivalGameUntil");s.carnivalRoundAt=n.getLong("CarnivalRoundAt");
        s.carnivalLastActionAt=n.getLong("CarnivalLastActionAt");s.carnivalSeed=n.getLong("CarnivalSeed");s.carnivalIssued=n.getBoolean("CarnivalIssued");
        s.carnivalRide=n.contains("CarnivalRide")?n.getInt("CarnivalRide"):-1;s.carnivalRideUntil=n.getLong("CarnivalRideUntil");
        s.parkourVoiceUntil=n.getLong("ParkourVoiceUntil");
        s.parkourEndAt=n.getLong("ParkourEndAt");s.parkourSeed=n.getLong("ParkourSeed");s.parkourRewarded=n.getBoolean("ParkourRewarded");
        s.gardenVoiceUntil=n.getLong("GardenVoiceUntil");s.gardenEndAt=n.getLong("GardenEndAt");
        s.gardenFinished=n.getBoolean("GardenFinished");s.gardenEligible=n.getBoolean("GardenEligible");s.gardenClaimMask=n.getInt("GardenClaimMask");
        s.feastVoiceUntil=n.getLong("FeastVoiceUntil");s.feastEndAt=n.getLong("FeastEndAt");s.feastFinished=n.getBoolean("FeastFinished");
        var feastFood=n.getIntArray("FeastPlateFood");System.arraycopy(feastFood,0,s.feastPlateFood,0,Math.min(s.feastPlateFood.length,feastFood.length));
        var feastRefill=n.getLongArray("FeastPlateRefillAt");System.arraycopy(feastRefill,0,s.feastPlateRefillAt,0,Math.min(s.feastPlateRefillAt.length,feastRefill.length));
        s.feastNextWaveAt=n.getLong("FeastNextWaveAt");s.feastWave=n.getInt("FeastWave");s.feastWaveKind=n.getInt("FeastWaveKind");s.feastBasketCount=n.getInt("FeastBasketCount");
        s.feastCartAt=n.getLong("FeastCartAt");s.feastGuestCanEatAt=n.getLong("FeastGuestCanEatAt");s.feastCartSlot=n.getInt("FeastCartSlot");
        var basket=n.getIntArray("FeastBasketFood");System.arraycopy(basket,0,s.feastBasketFood,0,Math.min(s.feastBasketFood.length,basket.length));
        s.fishingStage=n.getInt("FishingStage");s.fishingScore=n.getInt("FishingScore");s.fishingCatches=n.getInt("FishingCatches");
        s.fishingRank=n.getInt("FishingRank");s.fishingAnnounceIndex=n.getInt("FishingAnnounceIndex");
        var rival=n.getIntArray("FishingRivalScores");System.arraycopy(rival,0,s.fishingRivalScores,0,Math.min(4,rival.length));
        s.fishingVoiceUntil=n.getLong("FishingVoiceUntil");s.fishingEndAt=n.getLong("FishingEndAt");
        s.fishingWalkUntil=n.getLong("FishingWalkUntil");s.fishingJudgeUntil=n.getLong("FishingJudgeUntil");
        s.fishingAnnounceAt=n.getLong("FishingAnnounceAt");s.fishingLastNpcAt=n.getLong("FishingLastNpcAt");s.fishingSeed=n.getLong("FishingSeed");
        s.iceStage=n.getInt("IceStage");s.iceLap=n.getInt("IceLap");s.iceCheckpoint=n.getInt("IceCheckpoint");s.iceRank=n.getInt("IceRank");
        s.iceVoiceUntil=n.getLong("IceVoiceUntil");s.iceCountdownUntil=n.getLong("IceCountdownUntil");s.iceResultAt=n.getLong("IceResultAt");s.iceSeed=n.getLong("IceSeed");s.iceRewarded=n.getBoolean("IceRewarded");
        var iceProgress=n.getLongArray("IceRivalProgress");for(int i=0;i<Math.min(6,iceProgress.length);i++)s.iceRivalProgress[i]=Double.longBitsToDouble(iceProgress[i]);
        var iceFinish=n.getIntArray("IceRivalFinish");System.arraycopy(iceFinish,0,s.iceRivalFinish,0,Math.min(6,iceFinish.length));
        s.magicStage=n.getInt("MagicStage");s.magicRound=n.getInt("MagicRound");s.magicTokens=n.getInt("MagicTokens");s.magicAnswer=n.getInt("MagicAnswer");
        s.magicSeed=n.getLong("MagicSeed");s.magicVoiceUntil=n.getLong("MagicVoiceUntil");s.magicPhaseUntil=n.getLong("MagicPhaseUntil");
        s.flyingGardenStage=n.getInt("FlyingGardenStage");s.flyingGardenVoiceUntil=n.getLong("FlyingGardenVoiceUntil");
        s.flyingGardenPhaseUntil=n.getLong("FlyingGardenPhaseUntil");s.flyingGardenHarvestUntil=n.getLong("FlyingGardenHarvestUntil");
        s.flyingGardenCollectedMask=n.getLong("FlyingGardenCollectedMask");
        s.flyingGardenHarvestCount=n.getInt("FlyingGardenHarvestCount");
        var regrow=n.getLongArray("FlyingGardenRegrowAt");System.arraycopy(regrow,0,s.flyingGardenRegrowAt,0,Math.min(regrow.length,s.flyingGardenRegrowAt.length));
        var picks=n.getIntArray("FlyingGardenPickCount");System.arraycopy(picks,0,s.flyingGardenPickCount,0,Math.min(picks.length,s.flyingGardenPickCount.length));
        s.circusVoiceUntil=n.getLong("CircusVoiceUntil");s.circusActAt=n.getLong("CircusActAt");s.circusSeed=n.getLong("CircusSeed");
        s.circusAct=n.getInt("CircusAct");s.circusSelected=n.contains("CircusSelected")?n.getInt("CircusSelected"):-1;
        s.circusBetMouse=n.contains("CircusBetMouse")?n.getInt("CircusBetMouse"):-1;s.circusBetState=n.getInt("CircusBetState");
        var circusScores=n.getIntArray("CircusScores");System.arraycopy(circusScores,0,s.circusScores,0,Math.min(circusScores.length,s.circusScores.length));
        s.circusWager=ItemStack.of(n.getCompound("CircusWager"));
        s.circusPhase=n.getInt("CircusPhase");s.circusBookieMouse=n.contains("CircusBookieMouse")?n.getInt("CircusBookieMouse"):-1;
        s.circusPhaseUntil=n.getLong("CircusPhaseUntil");s.circusGifted=n.getBoolean("CircusGifted");
        s.ocean=n.getCompound("Ocean").copy();
        s.festival=n.getCompound("Festival").copy();s.temple=n.getCompound("Temple").copy();s.endCity=n.getCompound("EndCity").copy();s.expedition=n.getCompound("Expedition").copy();
        s.extra=n.getBoolean("Extra");s.opened=n.getBoolean("Opened");s.late=n.getBoolean("Late");s.deadline=n.getLong("Deadline");s.arrivalDeadline=n.getLong("ArrivalDeadline");s.portal=BlockPos.of(n.getLong("Portal"));
        s.motherPos=BlockPos.of(n.getLong("MotherPosition"));
        for(var tag:n.getList("Gifts",10))s.gifts.add(ItemStack.of((CompoundTag)tag));return s;
    }
}
