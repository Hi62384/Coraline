package com.coraline.tycoon;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class TycoonFoodShop {
    public static String buy(ServerPlayer p,int index,int quantity){
        if(p.serverLevel().dimension()!=TycoonWorld.KEY||index<0||index>=8||quantity<1||quantity>8||index==7&&quantity!=1)return "Invalid food selection.";
        var h=TycoonWorld.home(p);long cost=(long)TycoonFoodConfig.PRICES[index].get()*quantity,balance=h.state.getLong("SoulEssence");
        if(balance<cost)return "Requires "+cost+" Soul Essence. Nothing spent.";
        var patch=new CompoundTag();patch.putLong("SoulEssence",balance-cost);var flow=new CompoundTag();flow.putString("Category",index==7?"cake":"food");flow.putLong("Micros",-cost*TycoonSoulLedger.SCALE);patch.put("FinanceFlow",flow);String result=TycoonTransactions.purchase(p,0,TycoonFoodContent.owned(p,index,quantity),patch,null,new CompoundTag());
        return result.startsWith("Purchased")?"Purchased "+quantity+" "+TycoonFoodContent.NAMES[index]+" for "+cost+" Soul Essence. Stock meals manually; give cake personally.":result;
    }
    public static String furniture(ServerPlayer p,String id){if(p.serverLevel().dimension()!=TycoonWorld.KEY||!id.equals("other_world_supper_table")&&!id.equals("other_world_fridge"))return "Use your personal Work Desk.";
        int cost=id.endsWith("table")?TycoonFoodConfig.TABLE.get():TycoonFoodConfig.FRIDGE.get();return TycoonTransactions.purchase(p,cost,TycoonWorld.bound(p,id),new CompoundTag(),null,new CompoundTag());}
    public static void enrich(CompoundTag n){var rows=new ListTag();for(int i=0;i<8;i++){var r=new CompoundTag();r.putInt("Index",i);r.putString("Id",TycoonFoodContent.IDS[i]);r.putString("Name",TycoonFoodContent.NAMES[i]);r.putInt("Price",TycoonFoodConfig.PRICES[i].get());r.putInt("Nutrition",TycoonFoodContent.NUTRITION[i]);r.putDouble("Hunger",i==7?0:TycoonFoodQuality.hunger(new ItemStack(TycoonFoodContent.ITEMS.get(i).get())));r.putDouble("Trust",i==7?.2:TycoonFoodConfig.TRUST[i].get());r.putString("Quality",i==7?"Special Gifts — first visit only":i<2?"Basic":i==2?"Normal":i<5?"Good":"Excellent");rows.add(r);}n.put("FoodShop",rows);n.putInt("SupperTablePrice",TycoonFoodConfig.TABLE.get());n.putInt("SupperFridgePrice",TycoonFoodConfig.FRIDGE.get());}
    public static void firstEntry(ServerPlayer p,TycoonData.Resident r){var h=TycoonWorld.home(p);var visit=r.n.getCompound("Visit");
        if(r.n.getInt("VisitCount")==1&&visit.hasUUID("Session")&&!r.n.hasUUID("FirstVisitSession"))r.n.putUUID("FirstVisitSession",visit.getUUID("Session"));
        if(!r.n.getBoolean("DebugChild")&&h.creditSoul("visitor_starter",TycoonFoodConfig.STARTER.get())){TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();TycoonWorld.message(p,"Starter Soul Essence: "+TycoonFoodConfig.STARTER.get()+". Spend it on Other World meals at your Work Desk.");}
    }
    public static void completed(ServerPlayer p,TycoonData.Resident r){var h=TycoonWorld.home(p);var v=r.n.getCompound("Visit");
        if(r.n.getInt("VisitCount")==1&&v.contains("EnteredAt"))r.n.putBoolean("FirstVisitEnded",true);
        if(h.state.getInt("FarmPurchases")==0&&!r.n.getBoolean("DebugChild")&&r.active()&&v.hasUUID("Session")&&v.contains("EnteredAt")&&h.clock()-v.getLong("EnteredAt")>=2400&&r.need("Hunger")>30&&r.need("Thirst")>30&&r.need("Neglect")<75&&h.clock()>=h.state.getLong("SoulVisitRewardAfter")){
            if(h.creditSoul("visit:"+v.getUUID("Session"),TycoonFoodConfig.SESSION_REWARD.get()))h.state.putLong("SoulVisitRewardAfter",h.clock()+24000);
        }TycoonWorld.dirty(p);
    }
    private TycoonFoodShop(){}
}
