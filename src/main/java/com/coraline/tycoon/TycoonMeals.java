package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** One saved food escrow per resident. Real routes, inventory debit, seating,
 * eating and one authoritative completion, sharing the existing need meters. */
public final class TycoonMeals {
    public static boolean ready(TycoonData.Home h,TycoonData.Resident r){return r.need("Hunger")<55&&!r.n.contains("Meal")&&h.clock()>=r.n.getLong("MealAfter");}
    private static CompoundTag escrow(ItemStack food,boolean served,boolean personal){var n=new CompoundTag();n.putUUID("Id",UUID.randomUUID());n.put("Food",food.copyWithCount(1).save(new CompoundTag()));n.putBoolean("Served",served);n.putBoolean("Personal",personal);n.putInt("Progress",0);n.putString("Status","EATING");return n;}
    public static boolean manual(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        if(!TycoonWorld.owner(p).equals(actor.owner)||!r.active()||TycoonWorld.home(p).residents.get(r.id)!=r||!r.n.hasUUID("Entity")||!r.n.getUUID("Entity").equals(actor.getUUID())||p.distanceToSqr(actor)>25||actor.level()!=p.server.overworld()||!ready(TycoonWorld.home(p),r)||!TycoonBlockEntity.food(p.getMainHandItem()))return false;
        var patch=new CompoundTag();patch.put("Meal",escrow(p.getMainHandItem(),true,false));var result=TycoonTransactions.consumeHeld(p,p.getMainHandItem().copyWithCount(1),new CompoundTag(),r.id,patch);
        if(!r.n.contains("Meal")){TycoonWorld.message(p,result);return true;}actor.getNavigation().stop();tick(p,r,actor);return true;
    }
    /** Native manual/self-care testing also uses this exact quality function. */
    public static void applyCare(TycoonData.Resident r,ItemStack food,boolean served){r.need("Hunger",r.need("Hunger")+TycoonFoodQuality.hunger(food));r.need("Neglect",r.need("Neglect")+TycoonFoodQuality.neglect(r,food,served));r.n.remove("MealRequest");r.n.putBoolean(served?"PlayerServed":"SelfServed",true);r.n.putInt("HungryMealTicks",0);}
    public static boolean tick(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        if(!r.n.contains("Meal"))return false;var meal=r.n.getCompound("Meal");var h=TycoonWorld.home(p);if(meal.contains("Table"))r.n.putLong("DiningUntil",h.clock()+120);
        if(meal.getString("Status").equals("REFUND_PENDING")){refund(p,r,actor);return false;}
        // An unresolved durable reservation must not capture the visit controller.
        // Keep its escrow for reconciliation, but allow normal safe departure.
        if(meal.getString("Status").equals("RESERVING")){if(!debit(p,r,meal))return false;meal.putString("Status","EATING");}
        var food=ItemStack.of(meal.getCompound("Food"));if(food.isEmpty()){r.n.remove("Meal");release(actor);return false;}
        boolean leaving=meal.getBoolean("Personal")&&leaving(p,r);
        if(leaving&&meal.getInt("Progress")<80){refund(p,r,actor);return false;}
        int progress=meal.getInt("Progress")+20;meal.putInt("Progress",progress);actor.getNavigation().stop();actor.setItemInHand(InteractionHand.MAIN_HAND,food);
        r.n.putString("Task",progress<20?"REACHING_FOR_MEAL":progress<80?"EATING":"FINISHING_MEAL");actor.gesture(progress<=20?8:1,25);
        if(progress<100&&!leaving){TycoonWorld.dirty(p);return true;}
        // Need/reward and escrow retirement share the SAME SavedData snapshot.
        if(!meal.getBoolean("Applied")){
            if(meal.getBoolean("Personal")){r.need("Hunger",r.need("Hunger")+TycoonFoodQuality.hunger(food));int i=TycoonFoodContent.index(food);if(i>=0&&i<7)TycoonDialogue.set(r,TycoonDialogue.trust(r)+TycoonFoodConfig.TRUST[i].get()*TycoonFoodQuality.preference(r,food),TycoonDialogue.suspicion(r));}
            else applyCare(r,food,meal.getBoolean("Served"));meal.putBoolean("Applied",true);r.n.putUUID("LastMealReceipt",meal.getUUID("Id"));
        }
        r.n.putLong("MealAfter",h.clock()+TycoonFoodConfig.COOLDOWN.get());r.n.putInt("HungryMealTicks",0);r.n.remove("Meal");actor.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);actor.gesture(0,0);release(actor);TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();return true;
    }
    public static void missed(ServerPlayer p,TycoonData.Resident r,boolean personal){
        int ticks=r.n.getInt("HungryMealTicks")+20;r.n.putInt("HungryMealTicks",ticks);
        if(ticks<TycoonFoodConfig.EPISODE.get())return;r.n.putInt("HungryMealTicks",0);r.n.putInt("MissedMealEpisodes",r.n.getInt("MissedMealEpisodes")+1);
        if(personal)TycoonDialogue.set(r,TycoonDialogue.trust(r)-TycoonFoodConfig.OTHER_MISSED.get(),TycoonDialogue.suspicion(r));else r.need("Neglect",r.need("Neglect")+TycoonFoodConfig.MISSED.get());
        TycoonWorld.message(p,r.name()+": Is there anything to eat? Maybe I should go home soon.");TycoonWorld.dirty(p);
    }
    private static boolean reserve(ServerPlayer p,TycoonData.Resident r,TycoonBlockEntity fridge,int slot,boolean personal){
        var food=fridge.getItem(slot);if(food.isEmpty()||personal&&!fridge.canPlaceItem(slot,food)||!personal&&!TycoonBlockEntity.food(food))return false;
        var n=escrow(food,false,personal);n.putString("Status","RESERVING");n.putLong("Source",fridge.getBlockPos().asLong());n.putString("Dimension",fridge.getLevel().dimension().location().toString());n.putInt("Slot",slot);n.put("Before",food.save(new CompoundTag()));var after=food.copy();after.shrink(1);n.put("After",after.save(new CompoundTag()));r.n.put("Meal",n);TycoonWorld.dirty(p);
        try{if(!TycoonCheckpoints.home(p)){r.n.remove("Meal");return false;}}catch(java.io.IOException failure){r.n.remove("Meal");return false;}
        return debit(p,r,n);
    }
    private static boolean debit(ServerPlayer p,TycoonData.Resident r,CompoundTag n){
        var level=com.coraline.parents.FamilyData.level(p.server,n.getString("Dimension"));if(level==null)return false;var at=BlockPos.of(n.getLong("Source"));level.getChunkAt(at);
        if(!(level.getBlockEntity(at) instanceof TycoonBlockEntity be)||!TycoonWorld.owner(p).equals(be.owner)){n.putString("Blocker","Restore the owned meal source so its saved reservation can recover.");return false;}
        if(n.getUUID("Id").equals(be.mealDebit)){n.putString("Status","EATING");return true;}
        int slot=n.getInt("Slot");var before=ItemStack.of(n.getCompound("Before"));var actual=be.getItem(slot);
        if(actual.getCount()!=before.getCount()||!ItemStack.isSameItemSameTags(actual,before)){n.putString("Blocker","Meal source changed before the reserved serving was committed.");return false;}
        be.openForMeal();be.setItem(slot,ItemStack.of(n.getCompound("After")));be.mealDebit=n.getUUID("Id");be.setChanged();p.server.saveEverything(true,true,true);
        n.putString("Status","EATING");TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();return true;
    }
    private static List<TycoonBlockEntity> facilities(ServerPlayer p,TycoonNpc a,String kind,boolean personal){
        var h=TycoonWorld.home(p);var list=new ArrayList<TycoonBlockEntity>();var furniture=h.state.getCompound(personal?"OtherFurniture":"Furniture");
        for(String key:furniture.getAllKeys())if(furniture.getString(key).equals(kind)){BlockPos at;try{at=BlockPos.of(Long.parseLong(key));}catch(NumberFormatException invalid){continue;}
            if(a.distanceToSqr(at.getCenter())>24*24||!a.level().hasChunkAt(at))continue;
            if(a.level().getBlockEntity(at) instanceof TycoonBlockEntity be&&TycoonWorld.owner(p).equals(be.owner)&&!be.getBlockState().getValue(TycoonBlock.BROKEN))list.add(be);
        }list.sort(Comparator.comparingDouble(be->a.distanceToSqr(be.getBlockPos().getCenter())));return list;
    }
    static BlockPos approach(ServerPlayer p,TycoonNpc a,BlockPos at){
        var l=(ServerLevel)a.level();for(var dir:Direction.Plane.HORIZONTAL)for(int distance=1;distance<=2;distance++){
            var candidate=at.relative(dir,distance);if(!TycoonWorld.safeFeet(l,candidate))continue;var path=a.getNavigation().createPath(candidate,0);if(a.distanceToSqr(Vec3.atBottomCenterOf(candidate))<1.25||path!=null&&path.canReach())return candidate;
        }return null;
    }
    public static void overworld(ServerPlayer p,TycoonData.Home h,TycoonData.Resident r,TycoonNpc a,TycoonRooms.Survey survey){
        if(!ready(h,r))return;missed(p,r,false);var tables=facilities(p,a,"orphan_food_table",false);
        if(!r.n.contains("MealRequest")){r.n.putLong("MealRequest",h.clock());TycoonWorld.message(p,r.name()+" is hungry. Serve actual food at the Orphan Food Table.");}
        boolean fetch=h.clock()-r.n.getLong("MealRequest")>200+1200*r.trait("Patience")-150*r.trait("Independence");
        if(!fetch){r.n.putString("Task","WAITING_FOR_MEAL");for(var table:tables){var at=approach(p,a,table.getBlockPos());if(at!=null){TycoonCare.move(a,at,.8);a.gesture(3,25);break;}}return;}
        r.n.putString("Task","FETCHING_FOOD");if(h.clock()<r.n.getLong("FoodRetry"))return;
        for(var fridge:facilities(p,a,"orphan_fridge",false)){var at=approach(p,a,fridge.getBlockPos());if(at==null)continue;int slot=-1;for(int i=0;i<fridge.getContainerSize();i++)if(TycoonBlockEntity.food(fridge.getItem(i))){slot=i;break;}if(slot<0)continue;
            if(!TycoonCare.move(a,at,.8))return;if(reserve(p,r,fridge,slot,false))tick(p,r,a);return;
        }r.n.putLong("FoodRetry",h.clock()+200);
    }
    public static boolean otherWorld(ServerPlayer p,TycoonData.Resident r,TycoonNpc a){
        var h=TycoonWorld.home(p);if(!ready(h,r))return false;missed(p,r,true);r.n.putString("Task","SEEKING_OTHER_WORLD_SUPPER");
        var tables=facilities(p,a,"other_world_supper_table",true);var fridges=facilities(p,a,"other_world_fridge",true);
        for(var table:tables){var seatAt=approach(p,a,table.getBlockPos());if(seatAt==null)continue;
            boolean taken=false;for(var other:h.residents.values())if(!other.id.equals(r.id)&&other.n.contains("DiningTable")&&other.n.getLong("DiningTable")==table.getBlockPos().asLong()&&other.n.getLong("DiningUntil")>h.clock())taken=true;
            if(taken)continue;r.n.putLong("DiningTable",table.getBlockPos().asLong());r.n.putLong("DiningUntil",h.clock()+100);
            for(var fridge:fridges){if(fridge.getBlockPos().distSqr(table.getBlockPos())>12*12)continue;var fridgeAt=approach(p,a,fridge.getBlockPos());if(fridgeAt==null)continue;
                int slot=-1;for(int i=0;i<fridge.getContainerSize();i++)if(fridge.canPlaceItem(i,fridge.getItem(i))){slot=i;break;}if(slot<0)continue;
                if(!r.n.getBoolean("SupperReached")){if(TycoonCare.move(a,seatAt,.85))r.n.putBoolean("SupperReached",true);return true;}
                // Fetch physically first, then carry the escrow to the table.
                if(!TycoonCare.move(a,fridgeAt,.85))return true;if(!reserve(p,r,fridge,slot,true))return true;
                var meal=r.n.getCompound("Meal");meal.putLong("Table",table.getBlockPos().asLong());meal.putLong("SeatAt",seatAt.asLong());meal.putString("Status","CARRYING");r.n.remove("SupperReached");return true;
            }
        }
        if(h.clock()>=r.n.getLong("FoodComplaintAfter")){TycoonWorld.message(p,r.name()+": There's no meal I can reach here.");r.n.putLong("FoodComplaintAfter",h.clock()+1200);}return false;
    }
    public static boolean carryToTable(ServerPlayer p,TycoonData.Resident r,TycoonNpc a){var meal=r.n.getCompound("Meal");if(!meal.getString("Status").equals("CARRYING"))return false;
        var h=TycoonWorld.home(p);r.n.putLong("DiningUntil",h.clock()+120);int ticks=meal.getInt("CarryTicks")+20;meal.putInt("CarryTicks",ticks);
        var at=BlockPos.of(meal.getLong("SeatAt"));a.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.of(meal.getCompound("Food")));a.gesture(8,25);
        if(leaving(p,r)||ticks>=600){if(ticks>=600)TycoonWorld.message(p,r.name()+": I can't reach that seat. Please clear the route.");refund(p,r,a);return false;}
        if(!TycoonWorld.safeFeet((ServerLevel)a.level(),at)||!(a.level().getBlockEntity(BlockPos.of(meal.getLong("Table"))) instanceof TycoonBlockEntity)){refund(p,r,a);return false;}
        if(!TycoonCare.move(a,at,.85))return true;
        var seat=TycoonFoodContent.SEAT.get().create(a.level());if(seat==null)return true;seat.owner=TycoonWorld.owner(p);seat.setPos(Vec3.atBottomCenterOf(at).add(0,.2,0));if(a.level().addFreshEntity(seat)){a.getNavigation().stop();a.setPos(seat.position());a.startRiding(seat,true);meal.putString("Status","EATING");}return true;
    }
    private static boolean leaving(ServerPlayer p,TycoonData.Resident r){var h=TycoonWorld.home(p);var v=r.n.getCompound("Visit");if(!v.getString("Phase").equals("INSIDE"))return true;
        if(h.state.getBoolean("SendHome")||h.clock()>=v.getLong("LeaveAfter")||r.need("Neglect")>=TycoonConfig.SEVERE.get()||r.need("Thirst")<18||r.need("Hunger")<18||TycoonDialogue.suspicion(r)>.92){v.putString("Phase","LEAVING");v.putLong("LeavingAt",h.clock());TycoonWorld.dirty(p);return true;}return false;}
    private static void release(TycoonNpc a){if(a.getVehicle() instanceof TycoonSupperSeat seat){a.stopRiding();seat.discard();}}
    private static void refund(ServerPlayer p,TycoonData.Resident r,TycoonNpc a){var meal=r.n.getCompound("Meal");meal.putString("Status","REFUND_PENDING");release(a);a.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        var patch=new CompoundTag();patch.put("Meal",new CompoundTag());var result=TycoonTransactions.purchase(p,0,ItemStack.of(meal.getCompound("Food")),new CompoundTag(),r.id,patch);if(result.startsWith("Purchased")||result.startsWith("Purchase receipt"))r.n.remove("Meal");TycoonWorld.dirty(p);
    }
    private TycoonMeals(){}
}
