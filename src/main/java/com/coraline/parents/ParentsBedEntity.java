package com.coraline.parents;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
public final class ParentsBedEntity extends BlockEntity implements net.minecraft.world.Container,net.minecraft.world.MenuProvider {
    public UUID familyId;
    private final net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items=net.minecraft.core.NonNullList.withSize(27,net.minecraft.world.item.ItemStack.EMPTY);
    public ParentsBedEntity(BlockPos p,BlockState s){super(com.coraline.registry.ModBlockEntities.PARENTS_BED.get(),p,s);}
    public FamilyData.Family family(){return level instanceof ServerLevel sl&&familyId!=null?FamilyData.get(sl.getServer()).families.get(familyId):null;}
    public static void serverTick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,ParentsBedEntity bed){
        if(com.coraline.otherworld.OtherWorld.isOther(level))return;
        if(level.getGameTime()%20!=0)return;var f=bed.family();
        boolean available=f!=null&&f.isAvailable(((ServerLevel)level).getServer());
        for(int part=0;part<4;part++){
            var cell=ParentsBedBlock.partPos(pos,state.getValue(ParentsBedBlock.FACING),part);var s=level.getBlockState(cell);
            if(s.is(state.getBlock())&&s.getValue(ParentsBedBlock.PART)==part&&s.getValue(ParentsBedBlock.FACING)==state.getValue(ParentsBedBlock.FACING)&&s.getValue(ParentsBedBlock.AVAILABLE)!=available)
                level.setBlock(cell,s.setValue(ParentsBedBlock.AVAILABLE,available),3);
        }
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);if(familyId!=null)n.putUUID("Family",familyId);net.minecraft.world.ContainerHelper.saveAllItems(n,items);}
    @Override public void load(CompoundTag n){super.load(n);familyId=n.hasUUID("Family")?n.getUUID("Family"):null;items.clear();net.minecraft.world.ContainerHelper.loadAllItems(n,items);}
    @Override public int getContainerSize(){return items.size();}
    @Override public boolean isEmpty(){return items.stream().allMatch(net.minecraft.world.item.ItemStack::isEmpty);}
    @Override public net.minecraft.world.item.ItemStack getItem(int slot){return items.get(slot);}
    @Override public net.minecraft.world.item.ItemStack removeItem(int slot,int amount){var result=net.minecraft.world.ContainerHelper.removeItem(items,slot,amount);if(!result.isEmpty())setChanged();return result;}
    @Override public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot){return net.minecraft.world.ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,net.minecraft.world.item.ItemStack stack){items.set(slot,stack);stack.setCount(Math.min(stack.getCount(),Math.min(getMaxStackSize(),stack.getMaxStackSize())));setChanged();}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return !isRemoved()&&level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public net.minecraft.network.chat.Component getDisplayName(){return net.minecraft.network.chat.Component.literal("Parents' Bed Storage");}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.world.entity.player.Player player){return net.minecraft.world.inventory.ChestMenu.threeRows(id,inventory,this);}
}
