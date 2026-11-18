package com.coraline.multiplayer;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class QuestItemRecoveryGameTests {
    @GameTest(template="empty", batch="story_softlocks")
    public static void changedInventoryOnlyReceivesMissingOriginalQuestItems(GameTestHelper helper) {
        var profile=new CompoundTag();var inventory=new ListTag();var held=new ItemStack(Items.DIAMOND);held.setCount(1);
        var slot=held.save(new CompoundTag());slot.putByte("Slot",(byte)4);inventory.add(slot);profile.put("Inventory",inventory);
        profile.put("EnderItems",new ListTag());
        var row=new CompoundTag();var originals=new ListTag();originals.add(held.save(new CompoundTag()));originals.add(new ItemStack(Items.EMERALD).save(new CompoundTag()));row.put("Items",originals);

        var missing=QuestItemRecovery.missingDeliveryItems(profile,row);
        helper.assertTrue(missing.size()==1,"A partially present delivery must plan only its missing original item");
        helper.assertTrue(ItemStack.of(missing.getCompound(0)).is(Items.EMERALD),"The already present original item must not be duplicated");
        helper.succeed();
    }
}
