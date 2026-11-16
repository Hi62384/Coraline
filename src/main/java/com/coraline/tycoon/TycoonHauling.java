package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;

/** Saved worker carry escrow; source/destination chunks acknowledge each debit. */
public final class TycoonHauling {
    public static void resumeDestination(ServerPlayer p,CompoundTag worker,TycoonWorker actor){
        var carry=worker.getCompound("Carry");var level=com.coraline.parents.FamilyData.level(p.server,carry.getString("DestDimension"));
        if(level==null||actor.level()!=level){worker.putString("Blocker","Journaled cargo destination is in another dimension.");return;}
        var at=BlockPos.of(carry.getLong("Destination"));level.getChunkAt(at);
        if(!(level.getBlockEntity(at) instanceof TycoonBlockEntity be)||!TycoonWorld.owner(p).equals(be.owner)){worker.putString("Blocker","Restore the original cargo destination; cargo remains reserved.");return;}
        BlockPos goal=null;for(var direction:Direction.Plane.HORIZONTAL)if(TycoonWorld.safeFeet(level,at.relative(direction))){goal=at.relative(direction);break;}
        worker.putString("Task","FINISHING_SAVED_HANDOFF");if(TycoonWorkers.approach(p,worker,actor,goal))toContainer(p,worker,be);
    }
    public static ItemStack item(CompoundTag worker){return ItemStack.of(worker.getCompound("Carry").getCompound("Item"));}
    public static boolean fromWarehouse(ServerPlayer p,CompoundTag worker,Item item,int count){
        var h=TycoonWorld.home(p);if(worker.contains("Carry")||count<1||count>item.getMaxStackSize()||TycoonWarehouse.count(h,item)<count)return false;
        var carry=new CompoundTag();carry.putUUID("Id",UUID.randomUUID());var stack=new ItemStack(item,count);if(TycoonFoodContent.index(stack)>=0)TycoonWorld.tagOwned(p,stack);carry.put("Item",stack.save(new CompoundTag()));TycoonWarehouse.take(h,item,count);worker.put("Carry",carry);TycoonWorld.dirty(p);return true;
    }
    public static boolean fromContainer(ServerPlayer p,CompoundTag worker,TycoonBlockEntity be,int slot){
        if(worker.contains("Carry"))return recover(p,worker);if(!TycoonWorld.owner(p).equals(be.owner)||slot<0||slot>=be.getContainerSize()||be.getItem(slot).isEmpty())return false;
        var carry=new CompoundTag();carry.putUUID("Id",UUID.randomUUID());carry.putBoolean("Reserving",true);carry.put("Item",be.getItem(slot).copyWithCount(1).save(new CompoundTag()));carry.putLong("Source",be.getBlockPos().asLong());carry.putString("Dimension",be.getLevel().dimension().location().toString());carry.putInt("Slot",slot);carry.put("Before",be.getItem(slot).save(new CompoundTag()));var after=be.getItem(slot).copy();after.shrink(1);carry.put("After",after.save(new CompoundTag()));worker.put("Carry",carry);TycoonWorld.dirty(p);
        try{if(!TycoonCheckpoints.home(p)){worker.remove("Carry");return false;}}catch(java.io.IOException failure){worker.remove("Carry");return false;}return recover(p,worker);
    }
    public static boolean recover(ServerPlayer p,CompoundTag worker){
        var n=worker.getCompound("Carry");if(n.isEmpty())return false;if(!n.getBoolean("Reserving"))return true;
        var l=com.coraline.parents.FamilyData.level(p.server,n.getString("Dimension"));if(l==null)return false;var at=BlockPos.of(n.getLong("Source"));l.getChunkAt(at);if(!(l.getBlockEntity(at) instanceof TycoonBlockEntity be)||!TycoonWorld.owner(p).equals(be.owner))return false;String receipt=n.getUUID("Id").toString();
        if(!be.giftDebits.getBoolean(receipt)){
            if(n.getBoolean("Water")){if(be.water!=n.getInt("WaterBefore")||be.water<=0)return false;be.water--;}
            else{int slot=n.getInt("Slot");if(slot<0||slot>=be.getContainerSize())return false;var before=ItemStack.of(n.getCompound("Before"));var actual=be.getItem(slot);if(actual.getCount()!=before.getCount()||!ItemStack.isSameItemSameTags(actual,before))return false;be.setItem(slot,ItemStack.of(n.getCompound("After")));}
            be.giftDebits.putBoolean(receipt,true);be.setChanged();p.server.saveEverything(true,true,true);
        }
        n.putBoolean("Reserving",false);TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();return true;
    }
    public static boolean toContainer(ServerPlayer p,CompoundTag worker,TycoonBlockEntity be){
        if(!TycoonWorld.owner(p).equals(be.owner)||!recover(p,worker))return false;var carry=worker.getCompound("Carry");var item=item(worker);if(item.isEmpty())return false;String receipt="in:"+carry.getUUID("Id");
        if(!carry.contains("Destination")){
            int slot=-1;for(int i=0;i<be.getContainerSize();i++)if(be.canPlaceItem(i,item)&&(be.getItem(i).isEmpty()||ItemStack.isSameItemSameTags(be.getItem(i),item)&&be.getItem(i).getCount()+item.getCount()<=be.getItem(i).getMaxStackSize())){slot=i;break;}if(slot<0)return false;
            carry.putLong("Destination",be.getBlockPos().asLong());carry.putString("DestDimension",be.getLevel().dimension().location().toString());carry.putInt("DestSlot",slot);carry.put("DestBefore",be.getItem(slot).save(new CompoundTag()));var after=be.getItem(slot).isEmpty()?item.copy():be.getItem(slot).copy();if(!be.getItem(slot).isEmpty())after.grow(item.getCount());carry.put("DestAfter",after.save(new CompoundTag()));TycoonWorld.dirty(p);try{if(!TycoonCheckpoints.home(p))return false;}catch(java.io.IOException failed){return false;}
        }
        if(carry.getLong("Destination")!=be.getBlockPos().asLong()||!carry.getString("DestDimension").equals(be.getLevel().dimension().location().toString()))return false;
        if(!be.giftDebits.getBoolean(receipt)){int slot=carry.getInt("DestSlot");var before=ItemStack.of(carry.getCompound("DestBefore"));var actual=be.getItem(slot);if(actual.getCount()!=before.getCount()||!ItemStack.isSameItemSameTags(actual,before))return false;be.setItem(slot,ItemStack.of(carry.getCompound("DestAfter")));be.giftDebits.putBoolean(receipt,true);be.setChanged();p.server.saveEverything(true,true,true);}
        retire(p,worker);be.giftDebits.remove(receipt);be.setChanged();return true;
    }
    public static boolean meal(ServerPlayer p,CompoundTag worker,TycoonData.Resident r,boolean personal,boolean served){
        if(!recover(p,worker)||r.n.contains("Meal")||r.n.contains("CakeGift")||!r.active()||!TycoonMeals.ready(TycoonWorld.home(p),r))return false;var carry=worker.getCompound("Carry");var food=item(worker);if(food.isEmpty()||!food.isEdible())return false;
        if(food.getCount()!=1)return false;var meal=new CompoundTag();meal.putUUID("Id",carry.getUUID("Id"));meal.put("Food",food.save(new CompoundTag()));meal.putBoolean("Personal",personal);meal.putBoolean("Served",served);meal.putString("Status","EATING");r.n.put("Meal",meal);retire(p,worker);return true;
    }
    public static boolean water(ServerPlayer p,CompoundTag worker,TycoonBlockEntity source){
        if(worker.contains("Carry"))return recover(p,worker);if(!TycoonWorld.owner(p).equals(source.owner)||source.water<=0)return false;var n=new CompoundTag();n.putUUID("Id",UUID.randomUUID());n.putBoolean("Water",true);n.putBoolean("Reserving",true);n.putInt("WaterBefore",source.water);n.putLong("Source",source.getBlockPos().asLong());n.putString("Dimension",source.getLevel().dimension().location().toString());n.put("Item",new ItemStack(Items.POTION).save(new CompoundTag()));worker.put("Carry",n);TycoonWorld.dirty(p);try{if(!TycoonCheckpoints.home(p)){worker.remove("Carry");return false;}}catch(java.io.IOException failed){worker.remove("Carry");return false;}return recover(p,worker);
    }
    public static boolean hydrate(ServerPlayer p,CompoundTag worker,TycoonData.Resident r){
        if(!recover(p,worker)||!r.active()||!worker.getCompound("Carry").getBoolean("Water")&&!item(worker).is(Items.WATER_BUCKET))return false;boolean bucket=item(worker).is(Items.WATER_BUCKET);if(bucket&&TycoonWarehouse.total(TycoonWorld.home(p))>=TycoonWarehouse.capacity(TycoonWorld.home(p)))return false;r.need("Thirst",r.need("Thirst")+50);r.n.remove("WaterRequest");if(bucket)TycoonWarehouse.add(TycoonWorld.home(p),Items.BUCKET,1);retire(p,worker);return true;
    }
    public static boolean returnSupply(ServerPlayer p,CompoundTag worker){if(!recover(p,worker))return false;var stack=item(worker);if(stack.isEmpty())return false;var carry=worker.getCompound("Carry");
        if(carry.getBoolean("Water")){
            var level=com.coraline.parents.FamilyData.level(p.server,carry.getString("Dimension"));if(level==null)return false;var at=BlockPos.of(carry.getLong("Source"));level.getChunkAt(at);if(!(level.getBlockEntity(at) instanceof TycoonBlockEntity be)||!TycoonWorld.owner(p).equals(be.owner))return false;
            String receipt="water-in:"+carry.getUUID("Id");if(!be.giftDebits.getBoolean(receipt)){if(be.water>=32)return false;carry.putBoolean("ReturningWater",true);TycoonWorld.dirty(p);try{if(!TycoonCheckpoints.home(p))return false;}catch(java.io.IOException failed){return false;}be.water++;be.giftDebits.putBoolean(receipt,true);be.setChanged();p.server.saveEverything(true,true,true);}retire(p,worker);be.giftDebits.remove(receipt);be.setChanged();return true;
        }
        if(stack.hasTag()&&carry.contains("Source")){var l=com.coraline.parents.FamilyData.level(p.server,carry.getString("Dimension"));if(l!=null&&l.getBlockEntity(BlockPos.of(carry.getLong("Source"))) instanceof TycoonBlockEntity be)return toContainer(p,worker,be);return false;}if(!TycoonWarehouse.add(TycoonWorld.home(p),stack.getItem(),stack.getCount()))return false;retire(p,worker);return true;}
    public static void consumed(ServerPlayer p,CompoundTag worker){retire(p,worker);}
    public static boolean cake(ServerPlayer p,CompoundTag worker,TycoonData.Resident r){
        if(!recover(p,worker)||TycoonFoodContent.index(item(worker))!=7||!TycoonWelcomeCake.eligible(p,r))return false;var gift=new CompoundTag();gift.putUUID("Receipt",worker.getCompound("Carry").getUUID("Id"));gift.putUUID("Session",r.n.getCompound("Visit").getUUID("Session"));gift.put("Item",item(worker).save(new CompoundTag()));gift.putInt("Progress",0);r.n.put("CakeGift",gift);r.n.putUUID("FirstVisitSession",gift.getUUID("Session"));retire(p,worker);return true;
    }
    public static boolean gift(ServerPlayer p,CompoundTag worker,TycoonData.Resident r){
        if(!recover(p,worker)||r.n.contains("Gift")||r.trait("Giftableness")<=0||TycoonWorld.home(p).clock()<r.n.getLong("GiftAfter"))return false;String category=TycoonGifts.interest(item(worker));if(category.isEmpty())return false;var gift=new CompoundTag();gift.putUUID("Id",worker.getCompound("Carry").getUUID("Id"));gift.put("Item",item(worker).save(new CompoundTag()));gift.putString("Category",category);gift.putInt("Progress",0);r.n.put("Gift",gift);retire(p,worker);return true;
    }
    private static void retire(ServerPlayer p,CompoundTag worker){
        var n=worker.getCompound("Carry").copy();worker.remove("Carry");worker.putLong("CompletedJobs",worker.getLong("CompletedJobs")+1);TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();
        if(n.contains("Source")){var l=com.coraline.parents.FamilyData.level(p.server,n.getString("Dimension"));if(l!=null&&l.getBlockEntity(BlockPos.of(n.getLong("Source"))) instanceof TycoonBlockEntity be){be.giftDebits.remove(n.getUUID("Id").toString());be.setChanged();}}
    }
    private TycoonHauling(){}
}
