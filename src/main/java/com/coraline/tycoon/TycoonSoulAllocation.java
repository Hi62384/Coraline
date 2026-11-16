package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Bounded best-improvement search over feasible moves/swaps, not random sorting.
 * At most 30 farms x 5 souls; run once per active minute, not every actor tick. */
public final class TycoonSoulAllocation {
    private static boolean available(CompoundTag farm){return !farm.isEmpty()&&!farm.getBoolean("Damaged")&&!farm.getBoolean("Paused")&&farm.getBoolean("Verified")&&!TycoonSoulFarms.breaching(farm);}
    public static CompoundTag best(TycoonData.Home h){
        var farms=new ArrayList<CompoundTag>();for(String key:TycoonSoulFarms.farms(h).getAllKeys()){var farm=TycoonSoulFarms.farms(h).getCompound(key);if(available(farm))farms.add(farm);}farms.sort(Comparator.comparing(f->f.getUUID("Id").toString()));
        var result=new CompoundTag();long best=0;
        for(int i=0;i<farms.size();i++)for(int j=i+1;j<farms.size();j++){
            var a=farms.get(i);var b=farms.get(j);var left=TycoonSoulFarms.assigned(h,a.getUUID("Id"));var right=TycoonSoulFarms.assigned(h,b.getUUID("Id"));long baseline=TycoonSoulFarms.rateFor(h,a,left)+TycoonSoulFarms.rateFor(h,b,right);
            for(var source:left){if(right.size()<TycoonSoulFarms.capacity(b)){var x=new ArrayList<>(left);var y=new ArrayList<>(right);x.remove(source);y.add(source);long gain=TycoonSoulFarms.rateFor(h,a,x)+TycoonSoulFarms.rateFor(h,b,y)-baseline;if(gain>best){best=gain;result=move(source,b,gain);}}
                for(var other:right){var x=new ArrayList<>(left);var y=new ArrayList<>(right);x.remove(source);x.add(other);y.remove(other);y.add(source);long gain=TycoonSoulFarms.rateFor(h,a,x)+TycoonSoulFarms.rateFor(h,b,y)-baseline;if(gain>best){best=gain;result=move(source,b,gain);result.putUUID("Swap",other.getUUID("Id"));}}
            }
            if(left.size()<TycoonSoulFarms.capacity(a))for(var source:right){var x=new ArrayList<>(left);var y=new ArrayList<>(right);x.add(source);y.remove(source);long gain=TycoonSoulFarms.rateFor(h,a,x)+TycoonSoulFarms.rateFor(h,b,y)-baseline;if(gain>best){best=gain;result=move(source,a,gain);}}
        }return result;
    }
    private static CompoundTag move(CompoundTag soul,CompoundTag destination,long gain){var n=new CompoundTag();n.putUUID("Soul",soul.getUUID("Id"));n.putUUID("From",soul.getUUID("Farm"));n.putUUID("To",destination.getUUID("Id"));n.putLong("GainMicros",gain);return n;}
    public static boolean commit(ServerPlayer p,CompoundTag plan){
        if(!plan.hasUUID("Soul")||!plan.hasUUID("From")||!plan.hasUUID("To"))return false;var h=TycoonWorld.home(p);var soul=TycoonSouls.collection(h).getCompound(plan.getUUID("Soul").toString());var a=TycoonSoulFarms.farms(h).getCompound(plan.getUUID("From").toString());var b=TycoonSoulFarms.farms(h).getCompound(plan.getUUID("To").toString());
        if(!available(a)||!available(b)||!TycoonSouls.usable(soul)||!soul.hasUUID("Farm")||!plan.getUUID("From").equals(soul.getUUID("Farm")))return false;
        var left=TycoonSoulFarms.assigned(h,a.getUUID("Id"));var right=TycoonSoulFarms.assigned(h,b.getUUID("Id"));long before=TycoonSoulFarms.rateFor(h,a,left)+TycoonSoulFarms.rateFor(h,b,right);left.remove(soul);right.add(soul);CompoundTag other=null;
        if(plan.hasUUID("Swap")){other=TycoonSouls.collection(h).getCompound(plan.getUUID("Swap").toString());if(!TycoonSouls.usable(other)||!other.hasUUID("Farm")||!plan.getUUID("To").equals(other.getUUID("Farm")))return false;right.remove(other);left.add(other);}
        if(left.size()>TycoonSoulFarms.capacity(a)||right.size()>TycoonSoulFarms.capacity(b)||TycoonSoulFarms.rateFor(h,a,left)+TycoonSoulFarms.rateFor(h,b,right)<=before)return false;
        soul.putUUID("Farm",plan.getUUID("To"));if(other!=null)other.putUUID("Farm",plan.getUUID("From"));TycoonWorld.dirty(p);return true;
    }
    private TycoonSoulAllocation(){}
}
