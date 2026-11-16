package com.coraline.postgame;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Native inventory transactions over a single shared global Moonstone slot.
 * Only short-sized display words cross the menu protocol, never the journal. */
public final class BreachDriveMenu extends AbstractContainerMenu {
    private final BreachDriveBlockEntity device;
    private final ContainerData data;
    public BreachDriveMenu(int id,Inventory inventory,FriendlyByteBuf buffer){this(id,inventory,(BreachDriveBlockEntity)null);buffer.readBlockPos();}
    public BreachDriveMenu(int id,Inventory inventory,BreachDriveBlockEntity device){
        super(com.coraline.registry.ModMenus.BREACH_DRIVE.get(),id);this.device=device;
        boolean server=!inventory.player.level().isClientSide;var store=server?BreachWorldData.get(inventory.player.getServer()):null;
        Container fuel=server?store.input():new SimpleContainer(1);
        addSlot(new Slot(fuel,0,88,95){@Override public boolean mayPlace(ItemStack stack){return BreachWorldData.acceptsFuel(stack);}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inventory,x+y*9+9,16+x*18,132+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inventory,x,16+x*18,190));
        data=server?new ContainerData(){
            @Override public int get(int index){
                var view=store.view();
                if(index<2)return (view.millionths()>>>(index*16))&65535;
                if(index<6)return (int)((view.fuelTicks()>>>((index-2)*16))&65535);
                if(index<8)return (view.dailyMillionths()>>>((index-6)*16))&65535;
                return (view.active()?1:0)|(view.fuelTicks()>0?2:0)|(view.containmentEnabled()?4:0)|(view.avatarLocked()?8:0);
            }
            @Override public void set(int index,int value){}
            @Override public int getCount(){return 9;}
        }:new SimpleContainerData(9);
        addDataSlots(data);
    }
    public int millionths(){return (data.get(0)&65535)|((data.get(1)&65535)<<16);}
    public long fuelTicks(){long value=0;for(int i=0;i<4;i++)value|=(long)(data.get(i+2)&65535)<<(i*16);return value;}
    public int dailyMillionths(){return (data.get(6)&65535)|((data.get(7)&65535)<<16);}
    public boolean powered(){return (data.get(8)&2)!=0;}
    public boolean enabled(){return (data.get(8)&4)!=0;}
    public boolean avatarLocked(){return (data.get(8)&8)!=0;}
    @Override public boolean stillValid(Player player){return player.level().isClientSide||device!=null&&device.canUse(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();boolean moved;
        if(index==0)moved=moveItemStackTo(stack,1,37,true);
        else if(BreachWorldData.acceptsFuel(stack))moved=moveItemStackTo(stack,0,1,false);
        else moved=index<28?moveItemStackTo(stack,28,37,false):moveItemStackTo(stack,1,28,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
