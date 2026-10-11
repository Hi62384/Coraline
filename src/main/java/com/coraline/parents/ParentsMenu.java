package com.coraline.parents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class ParentsMenu extends AbstractContainerMenu {
    private final ParentEntity parent;
    private final boolean mom;
    private final ContainerData data;
    public ParentsMenu(int id,Inventory inv,FriendlyByteBuf buf){
        this(id,inv,inv.player.level().getEntity(buf.readVarInt()) instanceof ParentEntity p?p:null,buf.readBoolean());
    }
    public ParentsMenu(int id,Inventory inv,ParentEntity parent){this(id,inv,parent,parent.isMom());}
    private ParentsMenu(int id,Inventory inv,ParentEntity parent,boolean mom){
        super(com.coraline.registry.ModMenus.PARENTS.get(),id);this.parent=parent;this.mom=mom;
        data=parent!=null&&!inv.player.level().isClientSide?new ContainerData(){
            public int get(int i){var f=parent.family();return f==null?0:i==0?f.momTask:f.dadTask;}
            public void set(int i,int v){} public int getCount(){return 2;}
        }:new SimpleContainerData(2);
        addDataSlots(data);
    }
    public int selected(boolean mom){return data.get(mom?0:1);}
    public boolean isMom(){return mom;}
    @Override public boolean stillValid(Player player){
        if(player.level().isClientSide)return true;
        if(parent==null||!parent.isAlive()||parent.isRemoved()||parent.isOtherParent()||player.level()!=parent.level()||player.distanceToSqr(parent)>64)return false;
        var f=parent.family();return f!=null&&f.isAvailable(player.getServer())&&player.getUUID().equals(f.owner);
    }
    @Override public boolean clickMenuButton(Player player,int id){
        if(player.level().isClientSide||!stillValid(player))return false;
        boolean mom=id<100;int task=mom?id:id-100;
        if(mom!=this.mom)return false;
        if(task<0||task>(mom?5:3))return false;
        var f=parent.family();if(mom)f.momTask=task;else f.dadTask=task;
        FamilyData.get(player.getServer()).setDirty();broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
}
