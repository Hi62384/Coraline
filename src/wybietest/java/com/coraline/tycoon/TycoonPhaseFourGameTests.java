package com.coraline.tycoon;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;

/** Prepared native economic invariants; source guards are not their execution. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TycoonPhaseFourGameTests {
    @GameTest(template="empty") public static void farmPricesAreLinearAndCapped(GameTestHelper t){long[] expected={0,1,10,20,30,40};for(int i=1;i<=6;i++)t.assertTrue(TycoonSoulFarms.priceOrdinal(i)==expected[i-1],"Farm price progression differs");t.assertTrue(TycoonSoulFarms.priceOrdinal(30)==280&&TycoonSoulFarms.LIMIT==30&&TycoonSoulFarms.BREACH_TICKS==12000,"Farm cap or ten-minute breach changed");t.succeed();}
    @GameTest(template="empty") public static void fixedPointPreservesSmallProductionAcrossReloads(GameTestHelper t){var h=new TycoonData.Home(UUID.randomUUID(),0);for(int i=0;i<1000;i++)TycoonSoulLedger.creditMicros(h,15000);t.assertTrue(h.state.getLong("SoulEssence")==15&&h.state.getLong("SoulFraction")==0,"Fractional production was rounded away");TycoonSoulLedger.creditMicros(h,123);var saved=h.save();t.assertTrue(saved.getCompound("State").getLong("SoulFraction")==123,"Fraction not saved");t.assertTrue(!TycoonSoulLedger.creditMicros(h,-1),"Negative credit accepted");t.succeed();}
    @GameTest(template="empty") public static void soulsDeriveOnceFromPersonalityAndPreserveIdentity(GameTestHelper t){var h=new TycoonData.Home(UUID.randomUUID(),0);var r=TycoonCare.generate(UUID.randomUUID());var id=UUID.randomUUID();var soul=TycoonSouls.derive(h,r,id);var copy=TycoonSouls.derive(h,r,id);t.assertTrue(soul.equals(copy),"Soul rerolled on inspection");t.assertTrue(soul.getUUID("Resident").equals(r.id)&&soul.getUUID("Owner").equals(h.owner)&&soul.getCompound("Personality").equals(TycoonPersonality.exact(r)),"Soul identity or personality lost");t.assertTrue(soul.getLong("BaseMicros")>=15000&&soul.getLong("BaseMicros")<=50000,"Base rate outside configured defaults");t.succeed();}
    @GameTest(template="empty") public static void resonanceIsBoundedAndReleasedSoulsAreUnusable(GameTestHelper t){var h=new TycoonData.Home(UUID.randomUUID(),0);var list=new ArrayList<CompoundTag>();for(int i=0;i<5;i++)list.add(TycoonSouls.derive(h,TycoonCare.generate(UUID.randomUUID()),UUID.randomUUID()));double value=TycoonSoulFarms.resonance(list);t.assertTrue(value>=.85&&value<=1.25,"Unbounded resonance");var soul=list.get(0);soul.putString("Status","ESCAPED");t.assertTrue(!TycoonSouls.usable(soul),"Escaped soul remains reusable");soul.putString("Status","RELEASED");t.assertTrue(!TycoonSouls.usable(soul),"Released soul remains reusable");t.succeed();}
    @GameTest(template="empty") public static void discoveryCannotBeCalmedAway(GameTestHelper t){var r=TycoonCare.generate(UUID.randomUUID());r.n.putBoolean("KnowsSoulFarm",true);TycoonDialogue.set(r,.8,.1);t.assertTrue(TycoonDialogue.suspicion(r)==1,"Known souls erased by ordinary trust/gift/care update");t.succeed();}
    private TycoonPhaseFourGameTests(){}
}
