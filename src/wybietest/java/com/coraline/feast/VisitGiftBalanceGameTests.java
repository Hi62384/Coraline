package com.coraline.feast;

import java.util.*;
import com.coraline.clothing.*;
import com.coraline.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Actual registered gift factories; requires native registry/config loading. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class VisitGiftBalanceGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GiftBalanceQA"));
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void themedEquipmentAndAllTenClothingItemsHaveRealGiftPaths(GameTestHelper h) {
        var early=new VisitGiftPools.Progress(0,false,false,false);int customWeapons=0,customTools=0;
        for(int seed=0;seed<2000;seed++) {
            var weapon=VisitGiftPools.generate(OtherMotherDialogue.Loot.WEAPONS,RandomSource.create(seed),early).get(0);
            var tool=VisitGiftPools.generate(OtherMotherDialogue.Loot.TOOLS,RandomSource.create(seed),early).get(0);
            var armor=VisitGiftPools.generate(OtherMotherDialogue.Loot.ARMOR,RandomSource.create(seed),early).get(0);
            if(ForgeRegistries.ITEMS.getKey(weapon.getItem()).getNamespace().equals("coraline"))customWeapons++;
            if(ForgeRegistries.ITEMS.getKey(tool.getItem()).getNamespace().equals("coraline"))customTools++;
            h.assertFalse(weapon.is(Items.IRON_SWORD),"Generic Iron Sword still dominates gifts");
            h.assertTrue(armor.getItem() instanceof SetArmorItem set&&(set.set==ArmorSets.Set.COMFY_SILK||set.set==ArmorSets.Set.MIDNIGHT),"Early armor jumped to advanced protection");
        }
        h.assertTrue(customWeapons>1750&&customTools>1750,"Themed items do not dominate equipment gifts");
        var found=new HashSet<Item>();
        for(int seed=0;seed<12000;seed++)found.add(VisitGiftPools.generate(OtherMotherDialogue.Loot.CLOTHING,RandomSource.create(seed),new VisitGiftPools.Progress(2,false,false,false)).get(0).getItem());
        for(var item:List.of(ModItems.MIDNIGHT_COAT,ModItems.STAR_SHIRT,ModItems.RAINCOAT,ModItems.COMFY_PAJAMAS,ModItems.GARDEN_OVERALLS,ModItems.CIRCUS_JACKET,ModItems.SILENT_SLIPPERS,ModItems.FEATHER_BOOTS,ModItems.TOP_HAT,ModItems.MIDNIGHT_GOGGLES))
            h.assertTrue(found.contains(item.get()),"Specified loot-only clothing missing from real gift pool: "+item.getId());
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void treasureMaterialsHaveRealProgressionAndNoOrdinaryButtons(GameTestHelper h) {
        int priorMoon=VisitGiftConfig.MOONSTONE_ONE_IN.get(),priorPerfect=VisitGiftConfig.PERFECT_ONE_IN.get();
        try {
            VisitGiftConfig.MOONSTONE_ONE_IN.set(16);VisitGiftConfig.PERFECT_ONE_IN.set(160);
            for(int tier=0;tier<=2;tier++) {
                int diamonds=0,moon=0,perfect=0;
                for(int seed=0;seed<12000;seed++) {
                    var items=VisitGiftPools.generate(OtherMotherDialogue.Loot.TREASURE,RandomSource.create(seed),new VisitGiftPools.Progress(tier,false,false,false));
                    h.assertTrue(items.get(0).is(OtherMaterials.OTHER_IRON.get()),"Themed treasure lacks basic supernatural material");
                    for(var stack:items) {
                        h.assertFalse(stack.is(OtherMaterials.BUTTON.get()),"Ordinary gifts introduced Buttons");
                        if(stack.is(OtherMaterials.OTHER_DIAMOND.get()))diamonds++;
                        if(stack.is(OtherMaterials.MOONSTONE.get()))moon++;
                        if(stack.is(ModItems.PERFECT_INGOT.get()))perfect++;
                    }
                }
                if(tier==0)h.assertTrue(diamonds==0&&moon==0&&perfect==0,"First visits bypassed material progression");
                else h.assertTrue(diamonds>moon&&moon>0,"Mid/late material supply lacks rarity separation");
                if(tier<2)h.assertTrue(perfect==0,"Perfect appeared before late visits");
                else h.assertTrue(perfect>0&&perfect<moon/4,"Perfect ingots are absent or no longer very rare");
            }
        } finally {VisitGiftConfig.MOONSTONE_ONE_IN.set(priorMoon);VisitGiftConfig.PERFECT_ONE_IN.set(priorPerfect);}
        h.succeed();
    }

    private VisitGiftBalanceGameTests() {}
}
