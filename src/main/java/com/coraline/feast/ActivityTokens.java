package com.coraline.feast;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Awards bypass player-toss cancellation and Creative's discard-on-full path. */
public final class ActivityTokens {
    public static void give(ServerPlayer player,ItemStack awarded){giveTracked(player,awarded);}
    public static ItemEntity giveTracked(ServerPlayer player,ItemStack awarded){
        var remaining=awarded.copy();var inventory=player.getInventory();
        for(var held:inventory.items)if(ItemStack.isSameItemSameTags(held,remaining)){
            int count=Math.min(remaining.getCount(),held.getMaxStackSize()-held.getCount());
            if(count>0){held.grow(count);remaining.shrink(count);}if(remaining.isEmpty())break;
        }
        for(int i=0;i<inventory.items.size()&&!remaining.isEmpty();i++)if(inventory.items.get(i).isEmpty()){
            inventory.items.set(i,remaining.split(Math.min(remaining.getCount(),remaining.getMaxStackSize())));
        }
        inventory.setChanged();
        if(!remaining.isEmpty()){
            var drop=new ItemEntity(player.serverLevel(),player.getX(),player.getY()+.15,player.getZ(),remaining);
            drop.setDeltaMovement(0,.08,0);drop.setTarget(player.getUUID());drop.setPickUpDelay(20);
            drop.getPersistentData().putUUID("CoralineAwardOwner",player.getUUID());
            if(!player.serverLevel().addFreshEntity(drop))drop.getPersistentData().putBoolean("CoralineAwardFailed",true);return drop;
        }
        return null;
    }
    private ActivityTokens(){}
}
