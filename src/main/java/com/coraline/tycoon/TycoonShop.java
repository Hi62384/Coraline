package com.coraline.tycoon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
public final class TycoonShop {
    public static boolean sharedFunds(ServerPlayer p,Item item){return !TycoonWorld.owner(p).equals(p.getUUID())&&(item==TycoonContent.VOID_POWER.get()||item==Items.EMERALD);}
    public static int count(ServerPlayer p,Item item){if(sharedFunds(p,item))return (int)Math.min(Integer.MAX_VALUE,TycoonWarehouse.count(TycoonWorld.home(p),item));int count=0;for(var stack:p.getInventory().items)if(stack.is(item))count+=stack.getCount();return count;}
    public static boolean has(ServerPlayer p,Item item,int amount){return amount>=0&&count(p,item)>=amount;}
    public static boolean take(ServerPlayer p,Item item,int amount){if(amount==0)return true;if(sharedFunds(p,item))return false;if(!has(p,item,amount))return false;for(var stack:p.getInventory().items)if(stack.is(item)){int n=Math.min(amount,stack.getCount());stack.shrink(n);amount-=n;if(amount==0)break;}p.getInventory().setChanged();return true;}
    public static String buy(ServerPlayer p,String id){
        var h=TycoonWorld.home(p);if(!h.active()||!TycoonConfig.PRICES.containsKey(id))return "Upgrade unavailable.";
        int price=TycoonConfig.PRICES.get(id).get();var purchases=h.state.getCompound("Purchases").copy();purchases.putInt(id,Math.min(Integer.MAX_VALUE-1,purchases.getInt(id))+1);
        var patch=new net.minecraft.nbt.CompoundTag();patch.put("Purchases",purchases);
        return TycoonTransactions.purchase(p,price,TycoonWorld.bound(p,id),patch,null,new net.minecraft.nbt.CompoundTag());
    }
    public static String phone(ServerPlayer p){
        var h=TycoonWorld.home(p);if(!h.active()||h.state.getBoolean("PhoneIssued"))return "Your free phone has already been issued.";
        var patch=new net.minecraft.nbt.CompoundTag();patch.putBoolean("PhoneIssued",true);
        String result=TycoonTransactions.purchase(p,0,TycoonWorld.bound(p,"orphanage_phone"),patch,null,new net.minecraft.nbt.CompoundTag());
        return h.state.getBoolean("PhoneIssued")?"Orphanage Phone claimed. Use it in the Overworld.":result;
    }
    public static String recoverPhone(ServerPlayer p){if(!p.hasPermissions(2))return "Operator only.";if(p.getInventory().getFreeSlot()<0)return "Free a slot; old phone capability unchanged.";var h=TycoonWorld.home(p);h.state.putUUID("PhoneToken",java.util.UUID.randomUUID());TycoonWorld.deliver(p,TycoonWorld.bound(p,"orphanage_phone"));h.state.putBoolean("PhoneIssued",true);TycoonWorld.dirty(p);return "Recovered one phone. All prior phone capabilities are now inactive.";}
}
