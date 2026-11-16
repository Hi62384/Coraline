package com.coraline.feast;

import com.coraline.secrets.HouseCabinets;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.*;

/** Requires real Forge+Mixin loading. These are intentionally excluded from the cloud NBT-stub runner. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class PlayerPartyStorageGameTests {
    private record Fixture(GameTestHelper helper,VisitData.Scene scene,BlockPos at,ChestBlockEntity chest,IItemHandler cached){}
    private static void withChest(GameTestHelper h,Consumer<Fixture> test){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(2,3,2));h.assertTrue(level.getBlockState(at).isAir(),"Storage test requires an empty owned test cell");
        level.setBlock(at,Blocks.CHEST.defaultBlockState(),3);var chest=(ChestBlockEntity)level.getBlockEntity(at);chest.setItem(0,new ItemStack(Items.COOKIE,8));
        var cached=chest.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(()->new AssertionError("Chest handler absent"));
        var s=new VisitData.Scene();s.owner=UUID.randomUUID();s.door=UUID.randomUUID();s.table=at;s.piano=at;s.loop.event.putString("Type","PLAYER_PARTY");s.loop.event.putInt("Serial",77);
        var n=new CompoundTag();HousePartyLedger.begin(n,s.owner,77,1,8);PlayerPartyLedger.initialize(n,UUID.randomUUID());
        var offset=HouseCabinets.at(BlockPos.ZERO,20,-1,20);var house=at.subtract(offset);n.putLong("House",house.asLong());n.putString("HouseDimension",level.dimension().location().toString());s.loop.event.put("Party",n);
        VisitData.get(level.getServer()).scenes.put(s.owner,s);
        try{h.assertTrue(chest instanceof PartyStorageGuard.GuardedContainer,"Actual container mixin was not applied");test.accept(new Fixture(h,s,at,chest,cached));}
        finally{VisitData.get(level.getServer()).scenes.remove(s.owner);PartyStorageGuard.release();level.removeBlock(at,false);}
        h.succeed();
    }
    @GameTest(template="empty") public static void storageProtectsOfflineOwnerAndAllDirectExtraction(GameTestHelper h){withChest(h,f->{
        h.assertTrue(PartyStorageGuard.protectedAt(h.getLevel(),f.at),"Offline owner lost storage protection");h.assertTrue(f.chest.removeItem(0,1).isEmpty(),"Direct extraction bypassed protection");h.assertTrue(f.chest.removeItemNoUpdate(0).isEmpty(),"No-update extraction bypassed protection");
        f.chest.setItem(0,ItemStack.EMPTY);f.chest.clearContent();var copied=f.chest.getItem(0);copied.setCount(0);h.assertTrue(f.chest.getItem(0).getCount()==8,"Set, clear, or aliased stack mutated protected storage");
        h.assertTrue(PartyStorageGuard.protects(new CompoundContainer(new SimpleContainer(27),f.chest)),"Double chest second half was unprotected");
    });}
    @GameTest(template="empty") public static void cachedForgeHandlerCannotSimulateOrExtract(GameTestHelper h){withChest(h,f->{
        var handler=f.cached;h.assertTrue(handler instanceof PartyStorageGuard.GuardedHandler,"Actual Forge wrapper mixin not applied");h.assertTrue(PartyStorageGuard.protects(new net.minecraftforge.items.wrapper.InvWrapper(f.chest)),"Untracked wrapper did not resolve its real container");
        h.assertTrue(handler.extractItem(0,1,true).isEmpty()&&handler.extractItem(0,1,false).isEmpty(),"Forge simulated/actual extraction bypass");var view=handler.getStackInSlot(0);view.setCount(0);h.assertTrue(f.chest.getItem(0).getCount()==8,"Handler leaked a mutable stack");
        if(handler instanceof IItemHandlerModifiable modifiable)modifiable.setStackInSlot(0,ItemStack.EMPTY);h.assertTrue(f.chest.getItem(0).getCount()==8,"Modifiable handler bypass");
    });}
    @GameTest(template="empty") public static void itemStackHandlerAndCleanupPreserveContents(GameTestHelper h){withChest(h,f->{
        var handler=new ItemStackHandler(1);handler.setStackInSlot(0,new ItemStack(Items.APPLE,5));PartyStorageGuard.track(f.chest,handler);h.assertTrue(handler instanceof PartyStorageGuard.GuardedHandler,"ItemStackHandler mixin missing");
        h.assertTrue(handler.extractItem(0,3,false).isEmpty(),"Tracked mod inventory extracted");handler.setStackInSlot(0,ItemStack.EMPTY);h.assertTrue(handler.getStackInSlot(0).getCount()==5,"Tracked mod inventory erased");
        HouseParty.data(f.scene).putBoolean("Pending",false);h.assertTrue(handler.extractItem(0,3,false).getCount()==3&&handler.getStackInSlot(0).getCount()==2,"Cleanup failed to restore legitimate access or lost items");
    });}
    @GameTest(template="empty") public static void storageCannotBeDestroyedAndPublicInventoryIsUntouched(GameTestHelper h){withChest(h,f->{
        h.assertTrue(h.getLevel() instanceof PartyStorageGuard.GuardedWorld,"Actual world mutation guard missing");h.assertFalse(h.getLevel().destroyBlock(f.at,true),"Destroy bypass removed protected storage");h.assertFalse(h.getLevel().setBlock(f.at,Blocks.AIR.defaultBlockState(),3),"Replacement bypass removed protected storage");h.assertTrue(h.getLevel().getBlockEntity(f.at)==f.chest&&f.chest.getItem(0).getCount()==8,"Protected storage contents/identity changed");
        var ordinary=new ItemStackHandler(1);ordinary.setStackInSlot(0,new ItemStack(Items.BREAD,3));h.assertTrue(ordinary.extractItem(0,2,false).getCount()==2,"Unrelated inventory was blocked");
    });}
    @GameTest(template="empty") public static void protectedReadDoesNotConsumeLazyLoot(GameTestHelper h){withChest(h,f->{
        f.chest.setLootTable(new net.minecraft.resources.ResourceLocation("minecraft","chests/simple_dungeon"),11L);f.chest.getItem(0);var saved=f.chest.saveWithFullMetadata();h.assertTrue(saved.contains("LootTable"),"Protected read consumed a lazy loot table into blocked writes");
    });}
    private PlayerPartyStorageGameTests(){}
}
