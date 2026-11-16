package com.coraline.multiplayer;

import java.util.*;
import com.coraline.feast.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Durable actual-player receipts. Full inventories leave rewards queued; no
 * item entities or copied owner inventories are involved in group payouts. */
public final class StoryRewardDelivery {
    private static final String RECEIPTS="CoralineStoryRewardReceipts";
    public static boolean award(ServerPlayer actor,VisitData.Scene scene,String key,List<ItemStack> items){
        if(AcceptedButtonEyesInventory.activityReward(actor,scene,key,items))return true;
        var data=StorySessionData.get(actor.server);if(!data.groupActivity(scene))return false;
        if(AcceptedButtonEyesInventory.locked(actor))return true;
        if(!data.queueActivityReward(actor,scene,key,items))actor.displayClientMessage(net.minecraft.network.chat.Component.literal("The group reward is waiting for its saved delivery receipt."),true);return true;
    }
    public static void tick(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||AcceptedButtonEyesInventory.locked(p)||StoryCoop.locked(p))return;
        var data=StorySessionData.get(p.server);var pending=data.pendingRewards(p);if(pending.isEmpty()||p.containerMenu!=p.inventoryMenu)return;
        for(var grant:pending){
            if(!grant.hasUUID("Id")||!grant.hasUUID("Player")||!grant.getUUID("Player").equals(p.getUUID()))continue;
            UUID id=grant.getUUID("Id");var all=p.getPersistentData().getCompound(RECEIPTS);
            if(!p.getPersistentData().contains(RECEIPTS))p.getPersistentData().put(RECEIPTS,all);
            if(!all.contains(id.toString())){
                if(all.size()>=65536)return;var target=plan(p,grant.getList("Items",Tag.TAG_COMPOUND));if(target==null)return;
                var receipt=new CompoundTag();receipt.putUUID("Id",id);receipt.putString("State","PREPARED");receipt.put("Before",p.getInventory().save(new ListTag()));receipt.put("Target",target);all.put(id.toString(),receipt);
            }
            var receipt=all.getCompound(id.toString());if(!receipt.hasUUID("Id")||!receipt.getUUID("Id").equals(id))return;
            p.closeContainer();
            try{
                // Persist the intent with the actual inventory before inserting anything.
                p.server.getPlayerList().saveAll();var file=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");
                if(java.nio.file.Files.isSymbolicLink(file)||java.nio.file.Files.isSymbolicLink(file.getParent()))return;
                var saved=com.coraline.rebirth.StoryRunStore.read(file);var proof=saved.getCompound("ForgeData").getCompound(RECEIPTS).getCompound(id.toString());
                if(!proof.hasUUID("Id")||!proof.getUUID("Id").equals(id))return;
                if(proof.getString("State").equals("DELIVERED")){grant.putString("State","DELIVERED");data.touch(p.server);if(data.checkpoint(p.server))compact(receipt,grant);continue;}
                if(!proof.equals(receipt)||!receipt.getString("State").equals("PREPARED"))return;
                var current=new CompoundTag();current.put("Inventory",p.getInventory().save(new ListTag()));
                if(!com.coraline.palace.PalaceKeyCheckpoint.inventory(current,receipt.getList("Before",Tag.TAG_COMPOUND)))return;
                p.getInventory().load(receipt.getList("Target",Tag.TAG_COMPOUND));receipt.putString("State","DELIVERED");p.getInventory().setChanged();p.inventoryMenu.broadcastFullState();
                p.server.getPlayerList().saveAll();saved=com.coraline.rebirth.StoryRunStore.read(file);proof=saved.getCompound("ForgeData").getCompound(RECEIPTS).getCompound(id.toString());
                if(!proof.getString("State").equals("DELIVERED")||!proof.hasUUID("Id")||!proof.getUUID("Id").equals(id)||!com.coraline.palace.PalaceKeyCheckpoint.inventory(saved,receipt.getList("Target",Tag.TAG_COMPOUND)))return;
                grant.putString("State","DELIVERED");data.touch(p.server);if(!data.checkpoint(p.server))return;compact(receipt,grant);data.touch(p.server);
            }catch(java.io.IOException|RuntimeException unavailable){return;}
            // One payout per tick avoids simultaneous inventory plans.
            return;
        }
    }
    private static void compact(CompoundTag receipt,CompoundTag grant){receipt.remove("Before");receipt.remove("Target");grant.remove("Items");}
    private static ListTag plan(ServerPlayer p,ListTag items){
        var main=new net.minecraft.world.SimpleContainer(36);for(int i=0;i<36;i++)main.setItem(i,p.getInventory().getItem(i).copy());
        for(var raw:items){var stack=ItemStack.of((CompoundTag)raw);if(stack.isEmpty()||!main.addItem(stack.copy()).isEmpty())return null;}
        var packed=p.getInventory().save(new ListTag());packed.removeIf(raw->(((CompoundTag)raw).getByte("Slot")&255)<36);
        for(int i=0;i<36;i++)if(!main.getItem(i).isEmpty()){var row=main.getItem(i).save(new CompoundTag());row.putByte("Slot",(byte)i);packed.add(row);}return packed;
    }
    private StoryRewardDelivery(){}
}
