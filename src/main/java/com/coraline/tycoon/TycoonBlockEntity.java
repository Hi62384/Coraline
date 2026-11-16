package com.coraline.tycoon;
import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class TycoonBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public UUID owner,epoch;public int water;public UUID mealDebit;public int openUsers;public long mealDoorUntil;public CompoundTag giftDebits=new CompoundTag();
    private void doorTick(){if(level instanceof net.minecraft.server.level.ServerLevel sl&&kind().equals("other_world_fridge"))sl.scheduleTick(worldPosition,getBlockState().getBlock(),1);}
    @Override public void startOpen(Player p){if(stillValid(p)){openUsers++;doorTick();}}
    @Override public void stopOpen(Player p){openUsers=Math.max(0,openUsers-1);doorTick();}
    @Override public void onLoad(){super.onLoad();doorTick();}
    public void openForMeal(){if(level!=null){mealDoorUntil=level.getGameTime()+30;doorTick();}}
    private NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY);
    public TycoonBlockEntity(BlockPos pos,BlockState state){super(TycoonContent.STORAGE.get(),pos,state);}
    public String kind(){return ((TycoonBlock)getBlockState().getBlock()).kind;}
    @Override public int getContainerSize(){return items.size();}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int count){var result=ContainerHelper.removeItem(items,i,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    @Override public void setItem(int i,ItemStack s){if(kind().equals("other_world_fridge")&&!s.isEmpty()&&!canPlaceItem(i,s))return;items.set(i,s);s.setCount(Math.min(s.getCount(),getMaxStackSize()));setChanged();}
    @Override public boolean canPlaceItem(int slot,ItemStack item){if(kind().equals("other_world_fridge"))return TycoonFoodContent.meal(item)&&item.hasTag()&&item.getTag().hasUUID("TycoonOwner")&&owner!=null&&owner.equals(item.getTag().getUUID("TycoonOwner"));return !kind().equals("orphan_fridge")||food(item);}
    // Generic capability queries have no acting player or household authority.
    // The native household workers use the owner-checked hauling journal instead.
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,Direction side){
        if(capability==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)return net.minecraftforge.common.util.LazyOptional.empty();
        return super.getCapability(capability,side);
    }
    // Owner-operated furniture is not an automation interface in Phase 1.
    @Override public int[] getSlotsForFace(Direction face){return new int[0];}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack item,Direction face){return false;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack item,Direction face){return false;}
    public static boolean food(ItemStack item){return item.isEdible()&&item.getFoodProperties(null)!=null;}
    @Override public boolean stillValid(Player p){return owner!=null&&(owner.equals(p.getUUID())||p instanceof ServerPlayer actor&&TycoonCollaborators.storageAllowed(actor,this))&&level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getCenter())<=64;}
    @Override public void clearContent(){items=NonNullList.withSize(9,ItemStack.EMPTY);setChanged();}
    @Override protected Component getDefaultName(){return Component.literal(TycoonContent.label(kind()));}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return kind().equals("other_world_fridge")?new TycoonFridgeMenu(id,inventory,this):new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1,id,inventory,this,1);}
    @Override public void load(CompoundTag n){super.load(n);owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;epoch=n.hasUUID("Epoch")?n.getUUID("Epoch"):null;water=Math.max(0,Math.min(32,n.getInt("Water")));mealDebit=n.hasUUID("MealDebit")?n.getUUID("MealDebit"):null;giftDebits=n.getCompound("GiftDebits");items=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(n,items);}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);if(owner!=null)n.putUUID("Owner",owner);if(epoch!=null)n.putUUID("Epoch",epoch);n.putInt("Water",water);if(mealDebit!=null)n.putUUID("MealDebit",mealDebit);n.put("GiftDebits",giftDebits);ContainerHelper.saveAllItems(n,items);}
    public void refill(ServerPlayer p){var stack=p.getMainHandItem();if(stack.is(Items.WATER_BUCKET)&&water<=24){water+=8;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));setChanged();}else p.displayClientMessage(Component.literal("Drinking water: "+water+" / 32. Refill with a water bucket (+8 servings)."),true);}
}
