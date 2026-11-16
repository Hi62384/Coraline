package com.coraline.secrets;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Focused rerun of a failed native case; unchanged passing families remain separate. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class CabinetLootRollGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CabinetLootQA"));
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void allRealCabinetTablesSupplyThemedLootWithoutProgressionTokens(GameTestHelper h) {
        var p=player(h);var found=new HashSet<String>();
        for(String profile:List.of("hall","kitchen","bedroom","sitting_room","storage","attic","basement","archive","archive_late","landing"))
            for(int seed=1;seed<=500;seed++) {
                var cabinet=new CompoundTag();
                h.assertTrue(CabinetTreasure.seed(p,cabinet,profile,seed)==CabinetTreasure.Result.SEEDED,"Real loot table missing/invalid: "+profile+" / seed "+seed);
                boolean buttons=false;
                for(var tag:cabinet.getList("Items",Tag.TAG_COMPOUND)) {
                    var stack=ItemStack.of((CompoundTag)tag);String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();found.add(id);
                    if(id.equals("coraline:button"))buttons=true;
                    h.assertFalse(id.equals("coraline:void_heart")||id.equals("coraline:other_void_sword")||id.startsWith("coraline:other_world_void_"),"Cabinet minted protected progression");
                    var metadata=stack.getTag()==null?new CompoundTag():stack.getTag().copy();
                    if(stack.isDamageableItem()){
                        h.assertTrue(!metadata.contains("Damage")||metadata.contains("Damage",Tag.TAG_INT)&&metadata.getInt("Damage")==0,"Ordinary loot generated damaged gear");metadata.remove("Damage");
                    }
                    h.assertTrue(metadata.isEmpty(),"Cabinet minted protected credentials or non-default item metadata");
                    if(id.equals("coraline:perfect_ingot"))h.assertTrue(profile.equals("archive_late"),"Early cabinet bypassed Perfect material progression");
                }
                h.assertTrue(buttons,"Ordinary secret exploration lacks recurring crafting Buttons");
            }
        for(String name:List.of("midnight_coat","star_shirt","raincoat","comfy_pajamas","garden_overalls","circus_jacket","silent_slippers","feather_boots","top_hat","midnight_goggles"))
            h.assertTrue(found.contains("coraline:"+name),"Themed loot-only clothing has no actual cabinet roll: "+name);
        h.assertTrue(CabinetTreasure.profile(7,8).equals("archive")&&CabinetTreasure.profile(7,9).equals("archive_late"),"Archive progression gate drifted");h.succeed();
    }
    @GameTest(template="empty")
    public static void preparedClothingRejectsCredentialsAndDamageWithoutLosingInventory(GameTestHelper h){
        for(boolean credential:new boolean[]{true,false}){
            var cabinet=new CompoundTag();var stored=new ListTag();stored.add(new ItemStack(Items.DIRT,7).save(new CompoundTag()));cabinet.put("Items",stored);
            var stack=new ItemStack(com.coraline.registry.ModItems.RAINCOAT.get());
            if(credential)stack.getOrCreateTag().putUUID("StoryOwner",UUID.randomUUID());else stack.setDamageValue(1);
            var pending=new ListTag();pending.add(stack.save(new CompoundTag()));cabinet.put("PreparedTreasure",pending);cabinet.putInt("PreparedTreasureVersion",1);var before=cabinet.copy();
            h.assertTrue(CabinetTreasure.seed(player(h),cabinet,"bedroom",1)==CabinetTreasure.Result.INVALID&&cabinet.equals(before),"Prepared gear accepted credentials/damage or overwrote inventory");
        }h.succeed();
    }
    private CabinetLootRollGameTests(){}
}
