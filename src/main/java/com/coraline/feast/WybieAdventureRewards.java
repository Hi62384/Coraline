package com.coraline.feast;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Payout and inventory share the player file; a full inventory leaves an owned pending prize. */
@Mod.EventBusSubscriber(modid="coraline")
public final class WybieAdventureRewards {
    private static final String KEY="CoralineWybiePrizes";
    public static void settle(ServerPlayer p,CompoundTag event){
        if(!event.hasUUID("Session")||!event.hasUUID("Owner")||!p.getUUID().equals(event.getUUID("Owner"))||!event.getBoolean("TimerStarted")||!event.getBoolean("Completed")||!event.getString("EndReason").equals("RETURN"))return;
        var root=p.getPersistentData().getCompound(KEY);String id=event.getUUID("Session").toString();
        if(!root.contains(id)){
            var tx=new CompoundTag();var items=new ListTag();int tier=WybieAdventureLedger.rewardTier(event.getInt("BestScore"));
            for(var item:prize(tier))items.add(item.save(new CompoundTag()));tx.put("Pending",items);tx.putInt("BestScore",event.getInt("BestScore"));tx.putInt("Tier",tier);root.put(id,tx);p.getPersistentData().put(KEY,root);
            // This write precedes any insertion. Replayed scene data reuses the same session key.
            save(p);
        }
        event.putBoolean("RewardSettled",true);if(!p.isDeadOrDying())deliver(p);OtherMotherLoop.changed(p);
    }
    static List<ItemStack> prize(int tier){return switch(tier){case 4->List.of(new ItemStack(Items.DIAMOND,2),new ItemStack(Items.GOLD_INGOT,4),new ItemStack(Items.GOLDEN_CARROT,4));case 3->List.of(new ItemStack(Items.EMERALD,5),new ItemStack(Items.GOLD_INGOT,3));case 2->List.of(new ItemStack(Items.EMERALD,3),new ItemStack(Items.IRON_INGOT,6));case 1->List.of(new ItemStack(Items.IRON_INGOT,3),new ItemStack(Items.BREAD,2));default->List.of(new ItemStack(Items.COOKIE,2));};}
    public static void deliver(ServerPlayer p){
        if(!p.getPersistentData().contains(KEY))return;boolean changed=false;var root=p.getPersistentData().getCompound(KEY);
        for(String id:root.getAllKeys()){var tx=root.getCompound(id);if(tx.getBoolean("Paid"))continue;var items=tx.getList("Pending",10);boolean empty=true;
            for(int i=0;i<items.size();i++){var item=ItemStack.of(items.getCompound(i));int before=item.getCount();PartyEscrow.insert(p,item);changed|=before!=item.getCount();items.set(i,item.save(new CompoundTag()));empty&=item.isEmpty();}
            if(empty){tx.putBoolean("Paid",true);tx.remove("Pending");changed=true;}
        }if(changed)save(p);
    }
    private static void save(ServerPlayer p){p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();p.server.getPlayerList().saveAll();}
    @SubscribeEvent public static void clonePlayer(net.minecraftforge.event.entity.player.PlayerEvent.Clone e){if(e.getOriginal().getPersistentData().contains(KEY))e.getEntity().getPersistentData().put(KEY,e.getOriginal().getPersistentData().getCompound(KEY).copy());}
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent e){if(e.phase==net.minecraftforge.event.TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%100==0&&!p.isDeadOrDying())deliver(p);}
    private WybieAdventureRewards(){}
}
