package com.coraline.clothing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A selected inventory item is repaired on the server for one string and one needle use. */
public final class BeldamSewingNeedle extends Item {
    public BeldamSewingNeedle(){super(new Item.Properties().durability(96));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack needle=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server)ArtifactNetwork.openNeedle(server);
        return InteractionResultHolder.sidedSuccess(needle,level.isClientSide);
    }
    public static void repair(ServerPlayer player,int slot){
        var inventory=player.getInventory();
        if(slot<0||slot>=inventory.getContainerSize())return;
        ItemStack needle=player.getMainHandItem().is(UtilityItems.SEWING_NEEDLE.get())?player.getMainHandItem():player.getOffhandItem();
        if(!needle.is(UtilityItems.SEWING_NEEDLE.get()))return;
        ItemStack target=inventory.getItem(slot);
        if(!target.isDamageableItem()||!target.isDamaged()||target.is(UtilityItems.SEWING_NEEDLE.get()))return;
        int stringSlot=-1;
        if(!player.getAbilities().instabuild){
            for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(Items.STRING)){stringSlot=i;break;}
            if(stringSlot<0)return;
        }
        if(stringSlot>=0)inventory.getItem(stringSlot).shrink(1);
        target.setDamageValue(Math.max(0,target.getDamageValue()-25));
        inventory.setChanged();
        needle.hurtAndBreak(1,player,p->p.broadcastBreakEvent(needle==player.getMainHandItem()?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND));
    }
}
