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

/** Focused rerun of a failed native case; unchanged passing families remain separate. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class VisitGiftPreferenceGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GiftBalanceQA"));
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void ownerInventoryPreferencesRetainTheFirstRarityBand(GameTestHelper h) {
        var player=player(h);var scene=new VisitData.Scene();scene.owner=player.getUUID();scene.loop.visits=20;scene.loop.generation=4;
        player.getInventory().setItem(0,new ItemStack(OtherMaterials.MOONSTONE.get()));
        player.getInventory().setItem(1,new ItemStack(OtherMaterials.OTHER_DIAMOND.get()));
        player.getInventory().setItem(2,new ItemStack(ModItems.PERFECT_INGOT.get()));
        var progress=VisitGiftPools.progress(player,scene);
        for(var kind:List.of(OtherMotherDialogue.Loot.WEAPONS,OtherMotherDialogue.Loot.ARMOR,OtherMotherDialogue.Loot.TOOLS,OtherMotherDialogue.Loot.CLOTHING,OtherMotherDialogue.Loot.MYSTERY)) {
            scene.loop.loot=kind;
            for(int seed=0;seed<1000;seed++) {
                int band=VisitGiftPools.band(VisitGiftPools.generate(kind,RandomSource.create(seed),progress));
                player.getRandom().setSeed(seed);
                h.assertTrue(VisitGiftPools.band(VisitGiftPools.generate(player,scene))==band,"Preference rerolled a rare reward admission");
            }
        }
        scene.loop.visits=1;scene.loop.loot=OtherMotherDialogue.Loot.CLOTHING;
        player.getInventory().setItem(3,new ItemStack(ModItems.RAINCOAT.get()));
        VisitGiftHistory.remember(scene.loop.story,3,List.of("coraline:raincoat"));
        int ordinary=0,preferred=0;var seedStream=RandomSource.create(742971);
        // Adjacent small java.util.Random seeds have correlated first bits;
        // sample independent full-width seeds for a frequency assertion.
        for(int sample=0;sample<4000;sample++) {
            long seed=seedStream.nextLong();
            if(VisitGiftPools.generate(scene.loop.loot,RandomSource.create(seed),VisitGiftPools.progress(player,scene)).get(0).is(ModItems.RAINCOAT.get()))ordinary++;
            player.getRandom().setSeed(seed);
            if(VisitGiftPools.generate(player,scene).get(0).is(ModItems.RAINCOAT.get()))preferred++;
        }
        h.assertTrue(ordinary>800&&ordinary<1200&&preferred>0&&preferred<ordinary/2,"Owned/recent gift frequencies failed: ordinary="+ordinary+", preferred="+preferred);
        h.assertTrue(VisitGiftHistory.read(scene.loop.story).size()==1,"Unissued candidates changed saved reward history");
        var restored=OtherMotherLoopState.load(scene.loop.save());
        h.assertTrue(VisitGiftHistory.read(restored.story).equals(VisitGiftHistory.read(scene.loop.story)),"Loop reload lost preferences");
        h.succeed();
    }
    private VisitGiftPreferenceGameTests(){}
}
