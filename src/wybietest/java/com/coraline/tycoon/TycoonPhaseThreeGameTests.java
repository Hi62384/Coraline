package com.coraline.tycoon;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

/** Native assertions prepared for Phase 3. Must run before runtime acceptance. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TycoonPhaseThreeGameTests {
    @GameTest(template="empty") public static void migrationPreservesOldTraitsAndSignedGifts(GameTestHelper t){
        var r=TycoonCare.generate(UUID.randomUUID());double curiosity=r.n.getCompound("Traits").getDouble("Curiosity"),social=r.n.getCompound("Traits").getDouble("Sociability");
        TycoonPersonality.ensure(r);t.assertTrue(r.trait("Curiosity")==curiosity&&r.trait("Socialness")==social,"Phase 1 personality was replaced");
        TycoonPersonality.set(r,"Giftableness",-.8);t.assertTrue(r.trait("Giftableness")==-.8,"Negative Giftableness was normalized away");
        var snapshot=r.save();var loaded=new TycoonData.Resident(r.id,snapshot.copy());t.assertTrue(TycoonPersonality.exact(r).equals(TycoonPersonality.exact(loaded)),"Traits reroll after save/load");
        int appearance=r.n.getInt("Appearance");r.n.putInt("Appearance",appearance+1);t.assertTrue(r.trait("FoodInterest")==loaded.trait("FoodInterest"),"Body appearance controls food interest");t.succeed();
    }
    @GameTest(template="empty") public static void personalityDistributionContainsBothGiftSignsAndDistinctInterests(GameTestHelper t){
        int positive=0,negative=0;var exploration=new HashSet<Double>();
        for(int i=0;i<80;i++){var r=TycoonCare.generate(new UUID(i*31L,i+100));if(r.trait("Giftableness")>0)positive++;else negative++;exploration.add(r.trait("Explorativeness"));for(var trait:TycoonPersonality.TRAITS)t.assertTrue(r.trait(trait.key())>=trait.min()&&r.trait(trait.key())<=trait.max(),"Trait outside registered range");}
        t.assertTrue(positive>15&&negative>15&&exploration.size()>60,"Residents have generic clustered profiles");t.succeed();
    }
    @GameTest(template="empty") public static void analysisIntervalsConvergeAndMenuOpeningDoesNotRewardProgress(GameTestHelper t){
        var r=TycoonCare.generate(UUID.randomUUID());TycoonPersonality.set(r,"Giftableness",-.8);var trait=TycoonPersonality.definition("Giftableness");double lower=-1,upper=1;
        for(int ticks=0;ticks<=TycoonDollAnalysis.COMPLETE;ticks+=2400){r.n.putLong("AnalysisTicks",ticks);var interval=TycoonDollAnalysis.interval(r,trait);t.assertTrue(interval[0]>=lower&&interval[1]<=upper&&interval[0]<=-.8&&interval[1]>=-.8,"Analysis estimates reroll or fail to contain true trait");lower=interval[0];upper=interval[1];for(int i=0;i<5;i++)TycoonDollAnalysis.view(r);t.assertTrue(r.n.getLong("AnalysisTicks")==ticks,"Opening analysis generates free progress");}
        t.assertTrue(lower==-.8&&upper==-.8,"Complete analysis isn't exact");t.succeed();
    }
    @GameTest(template="empty") public static void observationReceiptsAndOtherResidentsRemainIndependent(GameTestHelper t){
        var r=TycoonCare.generate(UUID.randomUUID());var other=TycoonCare.generate(UUID.randomUUID());var doll=r.n.getCompound("Doll");doll.putString("Status","PLACED");r.n.put("Doll",doll);
        TycoonDollAnalysis.observe(r,"talk:one","Honest response",100,20);TycoonDollAnalysis.observe(r,"talk:one","Replay",100,40);
        t.assertTrue(r.n.getLong("AnalysisTicks")==100&&other.n.getLong("AnalysisTicks")==0,"Replay or another resident receives analysis progress");t.succeed();
    }
    private TycoonPhaseThreeGameTests(){}
    @GameTest(template="empty") public static void distinctStructuresRotateAndRoundTrip(GameTestHelper t){
        var hashes=new HashSet<Integer>();var at=t.absolutePos(new net.minecraft.core.BlockPos(8,4,8));
        for(var type:TycoonAttractionType.values()){
            var plan=TycoonAttractionPlan.create(type,at,0,1,new net.minecraft.nbt.CompoundTag());hashes.add(plan.hashCode());
            var saved=TycoonLandscape.encode(plan,at);t.assertTrue(TycoonLandscape.decode(t.getLevel(),saved).equals(plan),"Structure provenance changed after save");
            for(int rotation=0;rotation<4;rotation++){var p=TycoonAttractionPlan.at(at,rotation,0,1,-5);var rotated=TycoonAttractionPlan.create(type,at,rotation,1,new net.minecraft.nbt.CompoundTag());t.assertTrue(rotated.get(p).isAir(),"Rotated entrance is blocked");}
        }t.assertTrue(hashes.size()==10,"Attractions share one generic physical building");t.succeed();
    }
    @GameTest(template="empty") public static void matchingAndWanderingHaveSeparateInputs(GameTestHelper t){
        var r=TycoonCare.generate(UUID.randomUUID());var empty=new net.minecraft.nbt.CompoundTag();double before=TycoonAttractions.match(r,TycoonAttractionType.GARDEN,1,empty,1);
        TycoonPersonality.set(r,"Curiosity",1-r.trait("Curiosity"));TycoonPersonality.set(r,"Giftableness",-1);
        t.assertTrue(before==TycoonAttractions.match(r,TycoonAttractionType.GARDEN,1,empty,1),"Curiosity/gift acceptance incorrectly alters attraction fit");
        double[] expected={0,.05,.12,.22,.35};for(int i=0;i<5;i++)t.assertTrue(Math.abs(TycoonAttractions.wanderChance(i*.25)-expected[i])<.00001,"Curiosity curve differs from configured base design");t.succeed();
    }
    @GameTest(template="empty") public static void giftCategoriesDoNotTurnCakeIntoOrdinaryGift(GameTestHelper t){
        t.assertTrue(TycoonGifts.interest(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPASS)).equals("Explorativeness"),"Compass not exploration gift");
        t.assertTrue(TycoonGifts.interest(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND)).equals("Aweness"),"Gem doesn't use awe preference");
        t.assertTrue(TycoonGifts.interest(new net.minecraft.world.item.ItemStack(TycoonFoodContent.ITEMS.get(7).get())).isEmpty(),"Welcome cake entered repeatable gift system");t.succeed();
    }
}
