package com.coraline.feast;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/**
 * Item-only party wagering. Inventory and escrow share the player's saved NBT,
 * so an event-ledger save cannot duplicate a removed stake. No real-money wagers.
 */
@Mod.EventBusSubscriber(modid="coraline")
public final class PartyEscrow {
    public enum Outcome { WIN, LOSS, DRAW, CANCEL }
    private static final String KEY="CoralinePartyEscrow";
    private static final Set<Item> STAKES=Set.of(Items.DIAMOND,Items.EMERALD,Items.GOLD_INGOT,Items.IRON_INGOT,Items.COPPER_INGOT);
    private static CompoundTag root(ServerPlayer p){
        var data=p.getPersistentData();
        if(!data.contains(KEY,Tag.TAG_COMPOUND)){var n=new CompoundTag();n.putUUID("Owner",p.getUUID());n.put("Transactions",new CompoundTag());data.put(KEY,n);}
        var n=data.getCompound(KEY);
        if(!n.hasUUID("Owner")||!p.getUUID().equals(n.getUUID("Owner")))throw new IllegalStateException("Party escrow owner mismatch");
        return n;
    }
    public static boolean safeStake(ItemStack stack){
        // A narrow vanilla-material allowlist excludes every key, journal, secret,
        // quest item, temporary prop, container and tagged progression object.
        return !stack.isEmpty()&&STAKES.contains(stack.getItem())&&!stack.hasTag();
    }
    public static boolean outstanding(ServerPlayer p){
        if(!p.getPersistentData().contains(KEY))return false;
        for(String key:root(p).getCompound("Transactions").getAllKeys()){
            String state=root(p).getCompound("Transactions").getCompound(key).getString("State");
            if(state.equals("HELD")||state.equals("PAYOUT"))return true;
        }
        return false;
    }
    public static CompoundTag offer(ServerPlayer p,int serial,int slot,int count,List<ItemStack> proposedPrize){
        if(p.isCreative()||p.isSpectator()||outstanding(p)||slot<0||slot>=p.getInventory().items.size()||count<1||count>3)return new CompoundTag();
        var item=p.getInventory().getItem(slot);
        if(!safeStake(item)||item.getCount()<count||proposedPrize.isEmpty()||proposedPrize.size()>4)return new CompoundTag();
        var prizes=new ListTag();
        for(var prize:proposedPrize){
            if(!safeStake(prize)||prize.getCount()>16)return new CompoundTag();
            prizes.add(prize.copy().save(new CompoundTag()));
        }
        var n=new CompoundTag();n.putUUID("Nonce",UUID.randomUUID());n.putInt("Serial",serial);n.putInt("Slot",slot);
        n.put("Stake",item.copyWithCount(count).save(new CompoundTag()));n.put("Prize",prizes);
        root(p).put("Offer",n);save(p);
        return n.copy();
    }
    public static UUID accept(ServerPlayer p,int serial,UUID nonce){
        var r=root(p);if(outstanding(p)||!r.contains("Offer"))return null;
        var offer=r.getCompound("Offer");
        if(!offer.hasUUID("Nonce")||!nonce.equals(offer.getUUID("Nonce"))||offer.getInt("Serial")!=serial)return null;
        int slot=offer.getInt("Slot");if(slot<0||slot>=p.getInventory().items.size())return null;
        var item=p.getInventory().getItem(slot);var stake=ItemStack.of(offer.getCompound("Stake"));
        if(!safeStake(item)||!safeStake(stake)||!ItemStack.isSameItemSameTags(item,stake)||item.getCount()<stake.getCount())return null;
        var transaction=offer.copy();transaction.putUUID("Id",nonce);transaction.putString("State","HELD");transaction.remove("Nonce");
        // Both mutations are serialized in the same player-data file.
        item.shrink(stake.getCount());r.getCompound("Transactions").put(nonce.toString(),transaction);r.remove("Offer");save(p);
        return nonce;
    }
    public static boolean held(ServerPlayer p,int serial,UUID token){
        var n=transaction(p,token);return n!=null&&n.getString("State").equals("HELD")&&n.getInt("Serial")==serial;
    }
    private static CompoundTag transaction(ServerPlayer p,UUID token){
        if(token==null||!p.getPersistentData().contains(KEY))return null;
        var transactions=root(p).getCompound("Transactions");return transactions.contains(token.toString(),Tag.TAG_COMPOUND)?transactions.getCompound(token.toString()):null;
    }
    public static boolean settle(ServerPlayer p,UUID token,Outcome outcome){
        var n=transaction(p,token);if(n==null||!n.getString("State").equals("HELD"))return false;
        var payout=new ListTag();
        if(outcome!=Outcome.LOSS)payout.add(n.getCompound("Stake").copy());
        if(outcome==Outcome.WIN)for(var item:n.getList("Prize",Tag.TAG_COMPOUND))payout.add(item.copy());
        n.put("Payout",payout);n.putString("Outcome",outcome.name());n.putString("State",payout.isEmpty()?"SETTLED":"PAYOUT");
        n.remove("Stake");n.remove("Prize");save(p);
        // Delivery remains explicit/separate; a full inventory never drops or destroys the refund.
        return true;
    }
    public static int deliverPending(ServerPlayer p){
        if(!p.getPersistentData().contains(KEY))return 0;int delivered=0;boolean changed=false;
        for(String key:root(p).getCompound("Transactions").getAllKeys()){
            var n=root(p).getCompound("Transactions").getCompound(key);
            if(!n.getString("State").equals("PAYOUT"))continue;
            var list=n.getList("Payout",Tag.TAG_COMPOUND);
            for(int i=0;i<list.size();i++){
                var item=ItemStack.of(list.getCompound(i));int before=item.getCount();if(item.isEmpty())continue;
                insert(p,item);int amount=before-item.getCount();delivered+=amount;changed|=amount>0;
                list.set(i,item.save(new CompoundTag()));
            }
            boolean empty=true;for(var tag:list)if(!ItemStack.of((CompoundTag)tag).isEmpty())empty=false;
            if(empty){n.putString("State","SETTLED");n.remove("Payout");changed=true;}
        }
        if(changed)save(p);
        return delivered;
    }
    public static void cancelSerial(ServerPlayer p,int serial){
        if(!p.getPersistentData().contains(KEY))return;
        var r=root(p);r.remove("Offer");
        for(String key:new ArrayList<>(r.getCompound("Transactions").getAllKeys())){
            var n=r.getCompound("Transactions").getCompound(key);
            if(n.getInt("Serial")==serial&&n.getString("State").equals("HELD"))settle(p,UUID.fromString(key),Outcome.CANCEL);
        }
        deliverPending(p);
    }
    /** Caller supplies only currently persisted live matches, never guesses a winner after restart. */
    public static void recover(ServerPlayer p,Set<UUID> liveMatches){
        if(!p.getPersistentData().contains(KEY))return;
        var r=root(p);r.remove("Offer");
        for(String key:new ArrayList<>(r.getCompound("Transactions").getAllKeys())){
            var n=r.getCompound("Transactions").getCompound(key);
            if(n.getString("State").equals("HELD")&&!liveMatches.contains(UUID.fromString(key)))settle(p,UUID.fromString(key),Outcome.CANCEL);
        }
        deliverPending(p);save(p);
    }
    /** Preserves overflow even if the player changed to creative after accepting. */
    static void insert(ServerPlayer p,ItemStack stack){
        var inventory=p.getInventory();
        for(int pass=0;pass<2&&!stack.isEmpty();pass++)for(int slot=0;slot<36&&!stack.isEmpty();slot++){
            var current=inventory.getItem(slot);
            if(pass==0&&!current.isEmpty()&&ItemStack.isSameItemSameTags(current,stack)){int count=Math.min(stack.getCount(),current.getMaxStackSize()-current.getCount());if(count>0){current.grow(count);stack.shrink(count);}}
            else if(pass==1&&current.isEmpty()){int count=Math.min(stack.getCount(),stack.getMaxStackSize());inventory.setItem(slot,stack.copyWithCount(count));stack.shrink(count);}
        }
    }
    private static void save(ServerPlayer p){
        if(p.getPersistentData().contains(KEY)){
            var transactions=root(p).getCompound("Transactions");
            var settled=transactions.getAllKeys().stream().filter(k->transactions.getCompound(k).getString("State").equals("SETTLED")).sorted(Comparator.comparingInt(k->transactions.getCompound(k).getInt("Serial"))).toList();
            for(int i=0;i<settled.size()-128;i++)transactions.remove(settled.get(i));
        }
        p.getInventory().setChanged();p.inventoryMenu.broadcastChanges();
        // Public existing save API; active player's inventory and Forge persistent data are one file.
        p.server.getPlayerList().saveAll();
    }
    @SubscribeEvent public static void died(LivingDeathEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||!p.getPersistentData().contains(KEY))return;
        for(String key:new ArrayList<>(root(p).getCompound("Transactions").getAllKeys()))settle(p,UUID.fromString(key),Outcome.CANCEL);
    }
    @SubscribeEvent public static void cloned(PlayerEvent.Clone e){
        if(e.getOriginal().getPersistentData().contains(KEY))e.getEntity().getPersistentData().put(KEY,e.getOriginal().getPersistentData().getCompound(KEY).copy());
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)deliverPending(p);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%100==0&&!p.isDeadOrDying())deliverPending(p);
    }
    private PartyEscrow(){}
}
