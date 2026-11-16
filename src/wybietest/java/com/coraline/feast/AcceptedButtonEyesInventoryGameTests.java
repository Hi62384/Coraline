package com.coraline.feast;

import java.util.UUID;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class AcceptedButtonEyesInventoryGameTests {
    private static void escapeForRestore(net.minecraft.server.level.ServerPlayer p,VisitData.Scene s){
        var accepted=AcceptedButtonEyesProgress.data(s);var ghost=new CompoundTag();GhostVictimProgress.begin(ghost,"ACCEPTED_PROPOSAL");ghost.putUUID("Owner",s.owner);ghost.putUUID("Door",s.door);ghost.putUUID("Session",UUID.randomUUID());
        GhostVictimProgress.newVictim(ghost,UUID.randomUUID(),new CompoundTag());accepted.put(GhostVictimProgress.KEY,ghost);
        var escape=GhostEscapeProgress.data(ghost);escape.putUUID("Attempt",UUID.randomUUID());escape.putUUID("Victim",GhostVictimProgress.child(ghost).getUUID("Id"));escape.putUUID("Session",ghost.getUUID("Session"));
        escape.putBoolean("WagerWon",true);escape.putBoolean("PlayerEyesHeld",true);escape.putUUID("OriginalEscrow",accepted.getCompound("InventoryEscrow").getUUID("Transaction"));GhostEscapeProgress.move(escape,GhostEscapeProgress.Phase.RESTORING,0);
        var door=new com.coraline.otherworld.DoorData.Door();door.id=s.door;door.owner=s.owner;door.dimension=p.level().dimension().location().toString();door.pos=p.blockPosition();com.coraline.otherworld.DoorData.get(p.server).doors.put(s.door,door);
    }
    @GameTest(template="empty") public static void provenEscapeRestoresEquipmentOnceAndReconcilesAStaleScene(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);try{
            var n=AcceptedButtonEyesProgress.data(s);AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,s.door,5,0);
            p.getInventory().setItem(0,new ItemStack(Items.EMERALD,8));p.getInventory().setItem(36,new ItemStack(Items.DIAMOND_BOOTS));p.getInventory().setItem(40,new ItemStack(Items.SHIELD));
            h.assertTrue(AcceptedButtonEyesInventory.capture(p,s),"Original escrow not captured");var expected=n.getCompound("InventoryEscrow").getList("Inventory",Tag.TAG_COMPOUND).copy();escapeForRestore(p,s);
            h.assertTrue(AcceptedButtonEyesInventory.restoreAfterGhostEscape(p,s),"Proven escape could not restore original equipment");
            h.assertTrue(p.getInventory().save(new ListTag()).equals(expected),"Original main/armor/offhand inventory changed");
            p.getInventory().setItem(5,new ItemStack(Items.DIAMOND,9));n.putBoolean("InventoryConfiscated",true);n.putString("CaptureState","CLEARED");
            AcceptedButtonEyesInventory.enforce(p);h.assertTrue(p.getInventory().getItem(5).getCount()==9,"Stale scene deleted already-restored inventory");
            h.assertTrue(AcceptedButtonEyesInventory.restoreAfterGhostEscape(p,s)&&p.getInventory().getItem(5).getCount()==9,"Replayed restore overwrote subsequent items");
            h.assertFalse(AcceptedButtonEyesInventory.capture(p,s),"Completed story confiscated the same inventory again");h.assertTrue(p.getInventory().getItem(5).getCount()==9,"Repeated capture destroyed restored inventory");
            h.assertFalse(AcceptedButtonEyesInventory.locked(p),"Restored receipt leaves controls locked");
        }finally{com.coraline.otherworld.DoorData.get(p.server).doors.remove(s.door);AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void fullCreativeInventoryRetainsOverflowUntilASlotIsFree(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);try{
            var n=AcceptedButtonEyesProgress.data(s);AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,s.door,5,0);
            for(int slot=0;slot<36;slot++)p.getInventory().setItem(slot,new ItemStack(Items.STONE,64));var overflow=new ListTag();overflow.add(new ItemStack(Items.EMERALD,3).save(new CompoundTag()));n.put("PendingOverflow",overflow);
            h.assertTrue(AcceptedButtonEyesInventory.capture(p,s),"Full original inventory not captured");escapeForRestore(p,s);h.assertTrue(AcceptedButtonEyesInventory.restoreAfterGhostEscape(p,s),"Full original inventory not restored");
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.tickCount=20;
            var tick=new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p);
            AcceptedButtonEyesInventory.returnOverflow(tick);var backup=p.getPersistentData().getCompound("CoralineAcceptedInventoryEscrow");
            h.assertTrue(backup.getList("ReturnOverflow",Tag.TAG_COMPOUND).size()==1,"Creative inventory discarded escrow overflow");
            p.getInventory().setItem(0,ItemStack.EMPTY);AcceptedButtonEyesInventory.returnOverflow(tick);
            h.assertTrue(p.getInventory().getItem(0).is(Items.EMERALD)&&p.getInventory().getItem(0).getCount()==3&&backup.getList("ReturnOverflow",Tag.TAG_COMPOUND).isEmpty(),"Overflow not returned exactly once after freeing space");
            AcceptedButtonEyesInventory.returnOverflow(tick);h.assertTrue(p.getInventory().getItem(0).getCount()==3,"Repeated recovery duplicated overflow");
        }finally{com.coraline.otherworld.DoorData.get(p.server).doors.remove(s.door);AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void preProcedureMenuOverflowSurvivesUntilLaterConfiscation(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);try{
            AcceptedButtonEyesProgress.begin(AcceptedButtonEyesProgress.data(s),AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,s.door,5,0);
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));
            p.inventoryMenu.setCarried(new ItemStack(Items.GOLD_INGOT,3));p.inventoryMenu.getSlot(1).set(new ItemStack(Items.EMERALD,2));
            AcceptedButtonEyesInventory.closeForScene(p,s);AcceptedButtonEyesInventory.closeForScene(p,s);
            h.assertTrue(!AcceptedButtonEyesInventory.locked(p)&&p.getInventory().getItem(0).getCount()==64,"Offer closure confiscated items before sewing");
            h.assertTrue(AcceptedButtonEyesInventory.capture(p,s),"Later confiscation failed");var snapshot=AcceptedButtonEyesProgress.data(s).getCompound("InventoryEscrow");
            h.assertTrue(count(snapshot,Items.STONE)==36*64&&count(snapshot,Items.GOLD_INGOT)==3&&count(snapshot,Items.EMERALD)==2,"Pre-procedure menu closure lost or duplicated cursor/crafting overflow");
        }finally{AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    private static int count(CompoundTag snapshot,Item item){
        int result=0;for(String key:new String[]{"Inventory","Overflow","Crafting"})for(Tag row:snapshot.getList(key,Tag.TAG_COMPOUND)){var stack=ItemStack.of((CompoundTag)row);if(stack.is(item))result+=stack.getCount();}
        var cursor=ItemStack.of(snapshot.getCompound("Cursor"));return result+(cursor.is(item)?cursor.getCount():0);
    }
    @GameTest(template="empty") public static void confiscationPreservesEquipmentCursorCraftingAndNeverOverwrites(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var other=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);var otherScene=OtherMotherLoop.scene(other);
        try{
            AcceptedButtonEyesProgress.begin(AcceptedButtonEyesProgress.data(s),AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,s.door,5,0);
            p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));p.getInventory().setItem(36,new ItemStack(Items.DIAMOND_HELMET));p.getInventory().setItem(40,new ItemStack(Items.SHIELD));
            p.inventoryMenu.setCarried(new ItemStack(Items.GOLD_INGOT,3));p.inventoryMenu.getSlot(1).set(new ItemStack(Items.EMERALD,2));other.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,9));
            h.assertTrue(AcceptedButtonEyesInventory.capture(p,s),"Capture failed");var n=AcceptedButtonEyesProgress.data(s);var saved=n.getCompound("InventoryEscrow").copy();
            h.assertTrue(count(saved,Items.DIAMOND)==7&&count(saved,Items.DIAMOND_HELMET)==1&&count(saved,Items.SHIELD)==1&&count(saved,Items.GOLD_INGOT)==3&&count(saved,Items.EMERALD)==2,"Equipment/cursor/crafting lost or duplicated");
            h.assertTrue(p.getInventory().isEmpty()&&p.inventoryMenu.getCarried().isEmpty()&&AcceptedButtonEyesInventory.locked(p),"Visible inventory remains accessible");
            p.getPersistentData().getCompound("CoralineAcceptedInventoryEscrow").putString("CaptureState","PREPARED");p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));
            h.assertTrue(AcceptedButtonEyesInventory.capture(p,s)&&p.getInventory().isEmpty()&&saved.equals(n.getCompound("InventoryEscrow"))&&p.getPersistentData().getCompound("CoralineAcceptedInventoryEscrow").getString("CaptureState").equals("CLEARED"),"Prepared player backup and cleared journal did not recover the same transaction");
            p.getInventory().setItem(0,new ItemStack(Items.NETHERITE_INGOT,4));AcceptedButtonEyesInventory.capture(p,s);AcceptedButtonEyesInventory.enforce(p);
            h.assertTrue(saved.equals(n.getCompound("InventoryEscrow"))&&p.getInventory().isEmpty(),"Reentry overwrote originals with a temporary reward");
            var restored=OtherMotherLoopState.load(s.loop.save());h.assertTrue(restored.story.getCompound(AcceptedButtonEyesProgress.KEY).getCompound("InventoryEscrow").equals(saved),"Escrow lost on save/load");
            h.assertTrue(other.getInventory().getItem(0).getCount()==9&&!AcceptedButtonEyesInventory.locked(other),"Another owner's inventory was confiscated");
        }finally{AcceptedBranchPlayers.remove(p,s);AcceptedBranchPlayers.remove(other,otherScene);}h.succeed();
    }
    @GameTest(template="empty") public static void fullInventoryMenuOverflowIsEscrowedInsteadOfDropped(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);try{
            AcceptedButtonEyesProgress.begin(AcceptedButtonEyesProgress.data(s),AcceptedButtonEyesProgress.Route.FINAL_BOSS_LOSS,s.door,5,0);
            for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.STONE,64));p.getInventory().setItem(40,new ItemStack(Items.SHIELD));
            p.inventoryMenu.setCarried(new ItemStack(Items.APPLE,3));p.inventoryMenu.getSlot(1).set(new ItemStack(Items.EMERALD,2));
            h.assertTrue(AcceptedButtonEyesInventory.capture(p,s),"Full capture failed");var snapshot=AcceptedButtonEyesProgress.data(s).getCompound("InventoryEscrow");
            h.assertTrue(count(snapshot,Items.STONE)==36*64&&count(snapshot,Items.APPLE)==3&&count(snapshot,Items.EMERALD)==2&&count(snapshot,Items.SHIELD)==1,"Full menu close lost/duplicated overflow");
            h.assertFalse(snapshot.getList("Overflow",Tag.TAG_COMPOUND).isEmpty(),"Actual Player.drop overflow interceptor did not run");
        }finally{AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void incompleteEscrowNeverClearsOriginalItems(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);try{
            var n=AcceptedButtonEyesProgress.data(s);AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,s.door,5,0);n.putBoolean("EscrowStored",true);n.putString("CaptureState","PREPARED");
            p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));h.assertFalse(AcceptedButtonEyesInventory.capture(p,s),"Incomplete snapshot accepted");
            h.assertTrue(p.getInventory().getItem(0).getCount()==7&&!n.getString("Blocker").isEmpty(),"Original inventory destroyed during incomplete recovery");
        }finally{AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void malformedOwnedSnapshotCannotDestroyVisibleOriginals(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var s=OtherMotherLoop.scene(p);try{
            var n=AcceptedButtonEyesProgress.data(s);AcceptedButtonEyesProgress.begin(n,AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,s.door,5,0);
            var snapshot=new CompoundTag();snapshot.putUUID("Transaction",UUID.randomUUID());snapshot.putUUID("Owner",p.getUUID());snapshot.putUUID("Door",s.door);
            var broken=new ListTag();var bad=new CompoundTag();bad.putByte("Slot",(byte)0);bad.putString("id","minecraft:air");bad.putByte("Count",(byte)0);broken.add(bad);
            snapshot.put("Inventory",broken);snapshot.put("Overflow",new ListTag());snapshot.put("Crafting",new ListTag());snapshot.put("Cursor",new CompoundTag());n.put("InventoryEscrow",snapshot);n.putBoolean("EscrowStored",true);n.putBoolean("InventoryConfiscated",true);n.putBoolean("SewingCompleted",true);n.putString("CaptureState","CLEARED");
            var backup=new CompoundTag();backup.putBoolean("Stored",true);backup.putUUID("Door",s.door);backup.put("Snapshot",snapshot.copy());backup.putString("CaptureState","CLEARED");p.getPersistentData().put("CoralineAcceptedInventoryEscrow",backup);
            p.getInventory().setItem(0,new ItemStack(Items.DIAMOND,7));p.inventoryMenu.setCarried(new ItemStack(Items.GOLD_INGOT,3));p.inventoryMenu.getSlot(1).set(new ItemStack(Items.EMERALD,2));
            h.assertFalse(AcceptedButtonEyesInventory.capture(p,s),"Malformed owned snapshot accepted");AcceptedButtonEyesInventory.enforce(p);AcceptedButtonEyesInventory.cleanTemporary(p);AcceptedButtonEyes.tick(p,s);
            h.assertTrue(p.getInventory().getItem(0).getCount()==7&&p.inventoryMenu.getCarried().getCount()==3&&p.inventoryMenu.getSlot(1).getItem().getCount()==2,"Malformed snapshot cleanup/tick cleared visible originals");
        }finally{AcceptedBranchPlayers.remove(p,s);}h.succeed();
    }
    @GameTest(template="empty") public static void activityLootIsDoubledEscrowedAndReturnedExactlyOnce(GameTestHelper h){
        var p=AcceptedBranchPlayers.fixture(h);var scene=OtherMotherLoop.scene(p);try{
            AcceptedButtonEyesProgress.begin(AcceptedButtonEyesProgress.data(scene),AcceptedButtonEyesProgress.Route.ACCEPTED_PROPOSAL,scene.door,5,0);
            p.getInventory().setItem(0,new ItemStack(Items.IRON_INGOT,3));h.assertTrue(AcceptedButtonEyesInventory.capture(p,scene),"Could not capture original inventory");
            h.assertTrue(com.coraline.multiplayer.StoryRewardDelivery.award(p,scene,"test_prize",java.util.List.of(new ItemStack(Items.DIAMOND,2))),"Accepted reward fell through to ordinary inventory");
            com.coraline.multiplayer.StoryRewardDelivery.award(p,scene,"test_prize",java.util.List.of(new ItemStack(Items.DIAMOND,2)));
            h.assertTrue(p.getInventory().items.stream().filter(stack->stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==4,"Doubled activity prize was not visible exactly once");
            AcceptedButtonEyesInventory.confiscateActivityLoot(p,scene);h.assertTrue(p.getInventory().isEmpty(),"Activity prize was not confiscated on departure");escapeForRestore(p,scene);
            h.assertTrue(AcceptedButtonEyesInventory.restoreAfterGhostEscape(p,scene),"Reward invalidated escrow restore");
            int diamonds=0,iron=0;for(var stack:p.getInventory().items){if(stack.is(Items.DIAMOND))diamonds+=stack.getCount();if(stack.is(Items.IRON_INGOT))iron+=stack.getCount();}
            h.assertTrue(diamonds==4&&iron==3,"Doubled reward or original belongings were lost/duplicated");
        }finally{AcceptedBranchPlayers.remove(p,scene);}h.succeed();
    }
    private AcceptedButtonEyesInventoryGameTests(){}
}
