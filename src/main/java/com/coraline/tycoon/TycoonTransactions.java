package com.coraline.tycoon;

import java.io.IOException;
import java.util.UUID;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Serial owner-local Phase 2 purchases. The server constructs every patch;
 * clients supply only validated selection IDs, never arbitrary NBT or prices.
 */
public final class TycoonTransactions {
    public static UUID receipt(ServerPlayer p){var n=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);return n.hasUUID("TycoonTransactionReceipt")?n.getUUID("TycoonTransactionReceipt"):null;}
    private static void receipt(ServerPlayer p,UUID id){var n=p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);if(id==null)n.remove("TycoonTransactionReceipt");else n.putUUID("TycoonTransactionReceipt",id);p.getPersistentData().put(Player.PERSISTED_NBT_TAG,n);}

    public static String purchase(ServerPlayer p,int cost,ItemStack gift,CompoundTag homePatch,UUID resident,CompoundTag residentPatch){
        return exchange(p,cost,gift,homePatch,resident,residentPatch,ItemStack.EMPTY);
    }
    public static String consumeHeld(ServerPlayer p,ItemStack expected,CompoundTag homePatch,UUID resident,CompoundTag residentPatch){return exchange(p,0,ItemStack.EMPTY,homePatch,resident,residentPatch,expected);}
    public static String consumeHeldCount(ServerPlayer p,int count,CompoundTag homePatch){if(count<1||count>64||p.getMainHandItem().getCount()<count)return "Invalid deposit amount.";return exchange(p,0,ItemStack.EMPTY,homePatch,null,new CompoundTag(),p.getMainHandItem().copyWithCount(count));}
    public static String collectMany(ServerPlayer p,java.util.List<ItemStack> gifts,CompoundTag homePatch){return exchangeMany(p,0,gifts,homePatch,null,new CompoundTag(),ItemStack.EMPTY);}
    private static String exchange(ServerPlayer p,int cost,ItemStack gift,CompoundTag homePatch,UUID resident,CompoundTag residentPatch,ItemStack consume){
        return exchangeMany(p,cost,gift.isEmpty()?java.util.List.of():java.util.List.of(gift),homePatch,resident,residentPatch,consume);
    }
    private static String exchangeMany(ServerPlayer p,int cost,java.util.List<ItemStack> gifts,CompoundTag homePatch,UUID resident,CompoundTag residentPatch,ItemStack consume){
        var h=TycoonWorld.home(p);
        if(!h.active()||cost<0)return "Upgrade unavailable.";
        if(TycoonReceiptRecovery.actorBusy(p,h))return "Finish recovery of your earlier household transaction first.";
        if(h.state.contains("PendingTransaction")||h.state.contains("PendingPlacement"))return "A saved purchase is being recovered. Please refresh.";
        if(!TycoonShop.has(p,TycoonContent.VOID_POWER.get(),cost))return "Requires "+cost+" Void Power from the active payment account.";
        if(gifts.size()>36)return "Collection exceeds the inventory limit.";
        var capacity=new net.minecraft.world.SimpleContainer(p.getInventory().items.size());for(int i=0;i<capacity.getContainerSize();i++)capacity.setItem(i,p.getInventory().items.get(i).copy());
        for(var gift:gifts)if(!capacity.addItem(gift.copy()).isEmpty())return "Free inventory space. Nothing spent.";
        if(resident!=null&&!h.residents.containsKey(resident))return "Resident no longer belongs to this household.";
        if(!consume.isEmpty()&&(p.getMainHandItem().getCount()<consume.getCount()||!ItemStack.isSameItemSameTags(p.getMainHandItem(),consume)))return "Hold the requested item; nothing consumed.";
        boolean warehouse=cost>0&&TycoonShop.sharedFunds(p,TycoonContent.VOID_POWER.get());
        var committedPatch=homePatch.copy();
        if(warehouse){var stock=(committedPatch.contains("Warehouse",Tag.TAG_COMPOUND)?committedPatch.getCompound("Warehouse"):h.state.getCompound("Warehouse")).copy();String power=TycoonWarehouse.key(TycoonContent.VOID_POWER.get());if(stock.getLong(power)<cost)return "Requires "+cost+" household supply Void Power.";stock.putLong(power,stock.getLong(power)-cost);committedPatch.put("Warehouse",stock);}
        var intent=new CompoundTag();intent.putUUID("Receipt",UUID.randomUUID());intent.putUUID("Actor",p.getUUID());intent.putUUID("Owner",h.owner);intent.put("HomePatch",committedPatch);intent.putBoolean("WarehousePayment",warehouse);intent.putInt("Cost",cost);
        if(resident!=null){intent.putUUID("Resident",resident);intent.put("ResidentPatch",residentPatch.copy());}
        TycoonReceiptRecovery.prepare(p,h,intent,"TycoonTransactionReceipt");
        h.state.put("PendingTransaction",intent);
        var inventory=p.getInventory().save(new ListTag());var old=receipt(p);boolean changed=false,committed=false,saveAttempted=false;
        try{
            if(!TycoonCheckpoints.player(p))throw new IOException("Original actor inventory read-back failed");
            if(!TycoonCheckpoints.home(p))throw new IOException("Owner intent read-back failed");
            if(!warehouse&&!TycoonShop.take(p,TycoonContent.VOID_POWER.get(),cost))throw new IOException("Currency changed before payment");changed=true;
            if(!consume.isEmpty())p.getMainHandItem().shrink(consume.getCount());
            for(var gift:gifts)if(!gift.isEmpty()&&!TycoonWorld.deliver(p,gift.copy()))throw new IOException("Delivery blocked");
            receipt(p,intent.getUUID("Receipt"));
            saveAttempted=true;
            if(!TycoonCheckpoints.player(p))throw new IOException("Payment/delivery read-back failed");committed=true;
            if(!reconcile(p))return "Purchase receipt saved; owner state will reconcile on reload.";
            return "Purchased for "+cost+" Void Power.";
        }catch(IOException|RuntimeException failure){
            if(committed||saveAttempted){reconcile(p);return "Purchase receipt is pending; the current inventory and this same purchase are retained for native recovery.";}
            if(changed){p.getInventory().load(inventory);receipt(p,old);p.server.getPlayerList().saveAll();}
            TycoonReceiptRecovery.recover(p.server,h,"PendingTransaction");TycoonWorld.dirty(p);return "Purchase paused; receipt recovery remains safe: "+failure.getMessage();
        }
    }

    public static boolean reconcile(ServerPlayer p){return TycoonReceiptRecovery.recover(p.server,TycoonWorld.home(p),"PendingTransaction");}
    static void applyHomePatch(TycoonData.Home h,CompoundTag patch){for(String key:patch.getAllKeys()){
            if(key.equals("AttractionChanges")||key.equals("FarmChanges")||key.equals("MachineChanges")){
                String collection=key.equals("AttractionChanges")?"Attractions":key.equals("FarmChanges")?"SoulFarms":"Machines";var records=h.state.getCompound(collection);var changes=patch.getCompound(key);
                for(String id:changes.getAllKeys()){var values=changes.getCompound(id);if(values.getBoolean("Remove")){records.remove(id);continue;}var record=records.getCompound(id);for(String field:values.getAllKeys())record.put(field,values.get(field).copy());records.put(id,record);}h.state.put(collection,records);
            }else if(key.equals("FinanceFlow")){var flow=patch.getCompound(key);TycoonFinance.record(h,flow.getString("Category"),flow.getLong("Micros"));}
            else h.state.put(key,patch.get(key).copy());
        }
    }
    private TycoonTransactions(){}
}
