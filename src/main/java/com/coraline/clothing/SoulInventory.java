package com.coraline.clothing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** Nine item slots saved inside each soul's record on the transferable capturer. */
public final class SoulInventory extends SimpleContainer {
    private final ItemStack capturer;
    private final CompoundTag record;
    private boolean loading=true;
    public SoulInventory(ItemStack capturer,CompoundTag record){
        super(9);this.capturer=capturer;this.record=record;
        for(int i=0;i<9;i++)super.setItem(i,get(record,i));
        loading=false;
    }
    public static ItemStack get(CompoundTag record,int slot){
        if(slot<0||slot>=9)return ItemStack.EMPTY;
        ListTag list=record.getList("Hotbar",Tag.TAG_COMPOUND);
        for(int i=0;i<list.size();i++)if(list.getCompound(i).getInt("Slot")==slot)return ItemStack.of(list.getCompound(i).getCompound("Item"));
        return ItemStack.EMPTY;
    }
    public static void set(CompoundTag record,int slot,ItemStack stack){
        if(slot<0||slot>=9)return;
        ListTag list=record.getList("Hotbar",Tag.TAG_COMPOUND);
        for(int i=list.size()-1;i>=0;i--)if(list.getCompound(i).getInt("Slot")==slot)list.remove(i);
        if(!stack.isEmpty()){
            CompoundTag entry=new CompoundTag();entry.putInt("Slot",slot);entry.put("Item",stack.save(new CompoundTag()));list.add(entry);
        }
        record.put("Hotbar",list);
    }
    public static boolean insert(CompoundTag record,ItemStack loot){
        if(loot.isEmpty())return true;
        for(int i=0;i<9;i++){
            ItemStack current=get(record,i);
            if(!current.isEmpty()&&ItemStack.isSameItemSameTags(current,loot)&&current.getCount()<current.getMaxStackSize()){
                int moved=Math.min(loot.getCount(),current.getMaxStackSize()-current.getCount());current.grow(moved);loot.shrink(moved);set(record,i,current);
                if(loot.isEmpty())return true;
            }
        }
        for(int i=0;i<9;i++)if(get(record,i).isEmpty()){set(record,i,loot.copy());return true;}
        return false;
    }
    @Override public void setChanged(){
        super.setChanged();if(loading)return;
        for(int i=0;i<9;i++)set(record,i,getItem(i));
        if(capturer.hasTag())capturer.getTag().put("CoralineCaptured",capturer.getTag().getList("CoralineCaptured",Tag.TAG_COMPOUND));
    }
}
