package com.coraline.clothing;

import com.coraline.food.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class FoodGearGameTests {
    @GameTest(template="empty") public static void cottonCandyHasThreePersistentBites(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();p.getFoodData().setFoodLevel(5);var item=TreatFoods.ITEMS.get("cotton_candy").get();var stack=item.getDefaultInstance();
        stack=item.finishUsingItem(stack,h.getLevel(),p);h.assertTrue(!stack.isEmpty()&&ReusableTreat.bites(stack)==1,"First candy bite consumed the stick");
        stack=ItemStack.of(stack.save(new net.minecraft.nbt.CompoundTag()));h.assertTrue(ReusableTreat.bites(stack)==1,"Candy bite was lost on save");
        stack=item.finishUsingItem(stack,h.getLevel(),p);h.assertTrue(!stack.isEmpty()&&ReusableTreat.bites(stack)==2,"Second candy stage missing");
        stack=item.finishUsingItem(stack,h.getLevel(),p);h.assertTrue(stack.isEmpty()&&p.getFoodData().getFoodLevel()==11,"Third bite must finish the candy and restore nutrition");h.succeed();
    }
    @GameTest(template="empty") public static void bottomlessCandyRemainsAfterEating(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();p.getFoodData().setFoodLevel(4);p.getFoodData().setSaturation(0);var item=TreatFoods.ITEMS.get("bottomless_candy_bag").get();var stack=item.getDefaultInstance();
        for(int i=0;i<4;i++)stack=item.finishUsingItem(stack,h.getLevel(),p);
        h.assertTrue(stack.getCount()==1&&p.getFoodData().getFoodLevel()==12,"Infinite candy did not retain the bag / restore one drumstick");
        h.assertTrue(p.getFoodData().getSaturationLevel()<.2,"Infinite candy gives too much saturation");h.succeed();
    }
    @GameTest(template="empty") public static void popcornUsesServingsGraduallyAndStopsWhenFull(GameTestHelper h){
        var p=h.makeMockSurvivalPlayer();p.getFoodData().setFoodLevel(18);var item=TreatFoods.ITEMS.get("popcorn").get();var stack=item.getDefaultInstance();
        item.onUseTick(h.getLevel(),p,stack,71991);h.assertTrue(p.getFoodData().getFoodLevel()==19&&stack.getDamageValue()==1,"Popcorn must restore one food point per nibble");
        item.onUseTick(h.getLevel(),p,stack,71981);h.assertTrue(p.getFoodData().getFoodLevel()==20&&stack.getDamageValue()==2,"Second popcorn nibble missing");
        item.onUseTick(h.getLevel(),p,stack,71971);h.assertTrue(stack.getDamageValue()==2,"Popcorn consumed while full");
        stack.setDamageValue(31);p.getFoodData().setFoodLevel(10);item.onUseTick(h.getLevel(),p,stack,71961);h.assertTrue(stack.isEmpty(),"Empty popcorn container did not finish");h.succeed();
    }
    @GameTest(template="empty") public static void otherToolsAreBetterThanVanilla(GameTestHelper h){
        h.assertTrue(OtherMaterials.DIAMOND.getUses()>Tiers.DIAMOND.getUses()&&OtherMaterials.DIAMOND.getSpeed()>Tiers.DIAMOND.getSpeed()&&OtherMaterials.DIAMOND.getAttackDamageBonus()>Tiers.DIAMOND.getAttackDamageBonus(),"Other diamond tier not stronger");
        h.assertTrue(OtherMaterials.IRON.getUses()>Tiers.IRON.getUses()&&OtherMaterials.IRON.getSpeed()>Tiers.IRON.getSpeed()&&OtherMaterials.IRON.getAttackDamageBonus()>Tiers.IRON.getAttackDamageBonus(),"Other iron tier not stronger");
        h.assertTrue(ArmorSets.Set.MOONSTONE.getRepairIngredient().test(OtherMaterials.MOONSTONE.get().getDefaultInstance()),"Moonstone armor does not repair with moonstone");h.succeed();
    }
}
