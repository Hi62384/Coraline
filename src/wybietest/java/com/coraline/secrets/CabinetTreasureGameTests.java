package com.coraline.secrets;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Native table loading and saved cabinet inventory, not complete lock routes. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class CabinetTreasureGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        return FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"CabinetLootQA"));
    }

    @GameTest(template="empty")
    public static void fullCabinetKeepsOnePreparedRollAndPreservesExistingContents(GameTestHelper h) {
        var p=player(h);var cabinet=new CompoundTag();var items=new ListTag();
        for(int i=0;i<25;i++)items.add(new ItemStack(Items.DIRT).save(new CompoundTag()));
        cabinet.put("Items",items.copy());cabinet.putUUID("SecretToken",UUID.randomUUID());var token=cabinet.getUUID("SecretToken");
        h.assertTrue(CabinetTreasure.seed(p,cabinet,"hall",8147)==CabinetTreasure.Result.PREPARED,"Full cabinet did not retain an unopened roll");
        h.assertTrue(cabinet.getList("Items",Tag.TAG_COMPOUND).equals(items)&&token.equals(cabinet.getUUID("SecretToken")),"Loot upgrade overwrote existing contents/evidence");
        var prepared=cabinet.getList("PreparedTreasure",Tag.TAG_COMPOUND).copy();
        h.assertTrue(CabinetTreasure.seed(p,cabinet,"archive_late",1291)==CabinetTreasure.Result.UNCHANGED&&prepared.equals(cabinet.getList("PreparedTreasure",Tag.TAG_COMPOUND)),"Reopening/changing progress rerolled a pending treasure");
        cabinet.put("Items",new ListTag());
        h.assertTrue(CabinetTreasure.seed(p,cabinet,"archive_late",91)==CabinetTreasure.Result.SEEDED&&cabinet.getList("Items",Tag.TAG_COMPOUND).equals(prepared),"Making room lost or rerolled the original prepared payout");
        h.assertTrue(token.equals(cabinet.getUUID("SecretToken")),"Seeding retired unrelated secret identity");h.succeed();
    }
    @GameTest(template="empty")
    public static void savedEmptyCabinetDoesNotRegenerateItsTreasure(GameTestHelper h) {
        var p=player(h);var cabinet=new CompoundTag();
        h.assertTrue(CabinetTreasure.seed(p,cabinet,"attic",7291)==CabinetTreasure.Result.SEEDED,"Initial real table seed failed");
        cabinet.put("Items",new ListTag());var state=new SecretsState();var root=new CompoundTag();var cabinets=new ListTag();cabinets.add(cabinet);root.put("Cabinets",cabinets);state.world.put("HouseCabinets",root);
        var restored=SecretsState.load(state.save());var same=restored.world.getCompound("HouseCabinets").getList("Cabinets",Tag.TAG_COMPOUND).getCompound(0);var before=same.copy();
        h.assertTrue(CabinetTreasure.seed(p,same,"attic",99999)==CabinetTreasure.Result.UNCHANGED&&same.equals(before),"Actual secret-state reload regenerated already collected equipment");h.succeed();
    }
    @GameTest(template="empty")
    public static void missingTablesAndFutureOrMalformedStateDoNotMintLoot(GameTestHelper h) {
        var p=player(h);var missing=new CompoundTag();
        h.assertTrue(CabinetTreasure.seed(p,missing,"unregistered_profile",1)==CabinetTreasure.Result.MISSING_TABLE&&missing.isEmpty(),"Missing table silently committed empty/guessed loot");
        var future=new CompoundTag();future.putInt("TreasureVersion",2);var before=future.copy();
        h.assertTrue(CabinetTreasure.seed(p,future,"hall",1)==CabinetTreasure.Result.INVALID&&future.equals(before),"Future treasure state was reset and paid again");
        var corrupt=new CompoundTag();corrupt.putString("Items","preserve corrupt data");before=corrupt.copy();
        h.assertTrue(CabinetTreasure.seed(p,corrupt,"hall",1)==CabinetTreasure.Result.INVALID&&corrupt.equals(before),"Malformed old inventory was overwritten");h.succeed();
    }
    private CabinetTreasureGameTests() {}
}
