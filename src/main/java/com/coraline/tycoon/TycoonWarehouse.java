package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

/** Bounded real-item stock ledger. Nothing enters without a debit/paid recipe. */
public final class TycoonWarehouse {
    public static final int BASE_CAPACITY=4096,MAX_CAPACITY=262144;
    public static String key(Item item){return ForgeRegistries.ITEMS.getKey(item).toString();}
    public static long count(TycoonData.Home h,Item item){return Math.max(0,h.state.getCompound("Warehouse").getLong(key(item)));}
    public static long total(TycoonData.Home h){var n=h.state.getCompound("Warehouse");long total=0;for(String key:n.getAllKeys())total+=Math.max(0,n.getLong(key));return total;}
    public static int capacity(TycoonData.Home h){return Math.min(MAX_CAPACITY,BASE_CAPACITY<<Math.max(0,Math.min(6,h.state.getInt("WarehouseTier"))));}
    public static boolean add(TycoonData.Home h,Item item,long amount){if(amount<=0||amount>MAX_CAPACITY||total(h)>capacity(h)-amount)return false;var stock=h.state.getCompound("Warehouse");stock.putLong(key(item),stock.getLong(key(item))+amount);h.state.put("Warehouse",stock);return true;}
    public static boolean take(TycoonData.Home h,Item item,long amount){if(amount<0||count(h,item)<amount)return false;var stock=h.state.getCompound("Warehouse");stock.putLong(key(item),count(h,item)-amount);h.state.put("Warehouse",stock);return true;}
    public static String deposit(ServerPlayer p,int count){
        var h=TycoonWorld.home(p);var item=p.getMainHandItem();if(item.isEmpty()||item.hasTag()||count<1||count>item.getCount())return "Hold plain, untagged supply items. Named/enchanted/bound items are not accepted.";
        if(total(h)>capacity(h)-count)return "Supply storage is full.";var stock=h.state.getCompound("Warehouse").copy();stock.putLong(key(item.getItem()),stock.getLong(key(item.getItem()))+count);var patch=new CompoundTag();patch.put("Warehouse",stock);return TycoonTransactions.consumeHeldCount(p,count,patch);
    }
    public static String collect(ServerPlayer p,String itemId,boolean produced){
        var h=TycoonWorld.home(p);String ledger=produced?"ProducedResources":"Warehouse";var stock=h.state.getCompound(ledger).copy();long amount=stock.getLong(itemId);var id=net.minecraft.resources.ResourceLocation.tryParse(itemId);var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);if(item==null||amount<=0)return "No stored resource.";
        int room=0;var sample=new ItemStack(item);for(var slot:p.getInventory().items){if(slot.isEmpty())room+=sample.getMaxStackSize();else if(ItemStack.isSameItemSameTags(slot,sample))room+=Math.max(0,slot.getMaxStackSize()-slot.getCount());}int count=(int)Math.min(Math.min(amount,sample.getMaxStackSize()),room);if(count<=0)return "Your inventory is full; all output remains stored.";
        stock.putLong(itemId,amount-count);var patch=new CompoundTag();patch.put(ledger,stock);return TycoonTransactions.purchase(p,0,new ItemStack(item,count),patch,null,new CompoundTag());
    }
    public static String expand(ServerPlayer p){var h=TycoonWorld.home(p);int tier=h.state.getInt("WarehouseTier");if(tier>=6)return "Storage is at maximum capacity.";var patch=new CompoundTag();patch.putInt("WarehouseTier",tier+1);return TycoonTransactions.purchase(p,16*(tier+1),ItemStack.EMPTY,patch,null,new CompoundTag());}
    public static String collectMany(ServerPlayer p,String selected,boolean produced){
        var h=TycoonWorld.home(p);String ledger=produced?"ProducedResources":"Warehouse";var stock=h.state.getCompound(ledger).copy();
        var inventory=new net.minecraft.world.SimpleContainer(p.getInventory().items.size());for(int i=0;i<inventory.getContainerSize();i++)inventory.setItem(i,p.getInventory().items.get(i).copy());
        var gifts=new ArrayList<ItemStack>();var keys=new ArrayList<>(stock.getAllKeys());Collections.sort(keys);
        for(String key:keys){if(!selected.isEmpty()&&!selected.equals(key))continue;var id=net.minecraft.resources.ResourceLocation.tryParse(key);var item=id==null?null:ForgeRegistries.ITEMS.getValue(id);if(item==null||item==Items.AIR)continue;
            long amount=stock.getLong(key);while(amount>0&&gifts.size()<36){int requested=(int)Math.min(amount,item.getMaxStackSize());var gift=new ItemStack(item,requested);var remainder=inventory.addItem(gift.copy());int accepted=requested-remainder.getCount();if(accepted==0)break;gift.setCount(accepted);gifts.add(gift);amount-=accepted;}stock.putLong(key,amount);
        }
        if(gifts.isEmpty())return "Nothing fits; all resources remain stored.";var patch=new CompoundTag();patch.put(ledger,stock);return TycoonTransactions.collectMany(p,gifts,patch);
    }
    public static boolean purchase(ServerPlayer p,int power,CompoundTag homePatch,UUID resident,CompoundTag residentPatch){
        var h=TycoonWorld.home(p);if(power<0||h.state.contains("PendingTransaction")||h.state.contains("PendingPlacement")||count(h,TycoonContent.VOID_POWER.get())<power||resident!=null&&!h.residents.containsKey(resident))return false;
        take(h,TycoonContent.VOID_POWER.get(),power);TycoonTransactions.applyHomePatch(h,homePatch);if(resident!=null){var r=h.residents.get(resident);for(String field:residentPatch.getAllKeys())r.n.put(field,residentPatch.get(field).copy());}TycoonWorld.dirty(p);return true;
    }
    public static ListTag projection(CompoundTag stock){var list=new ListTag();var keys=new ArrayList<>(stock.getAllKeys());Collections.sort(keys);for(String id:keys)if(stock.getLong(id)>0){var n=new CompoundTag();n.putString("Id",id);n.putLong("Count",stock.getLong(id));list.add(n);}return list;}
    private TycoonWarehouse(){}
}
