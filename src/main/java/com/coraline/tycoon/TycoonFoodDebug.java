package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;

/** Explicit developer tools. Mutating welfare/visit checkpoints require a test
 * resident; normal purchases/gifts/routes still use their production handlers. */
public final class TycoonFoodDebug {
    private static final String[][] BUTTONS={
        {"Select nearest owned resident","select"},{"Inspect live needs / meal / gift / receipts","inspect"},{"Show Tycoon Resident Stats Above Heads — toggle","labels"},
        {"Set test Hunger to10","hunger"},{"Set test Neglect to40","neglect"},{"Give rotten flesh (actual item)","rotten"},{"Give golden carrot (actual item)","excellent"},{"Preview held food quality / care effect","quality"},
        {"Test manual serving normally","manual"},{"Force test self-serving route","self"},{"Trigger one missed-meal episode (test)","missed"},{"Empty owned nearby fridge into inventory","empty"},
        {"Give owned Other World Supper Table","table"},{"Give owned Other World Fridge","fridge"},{"Give special soup to stock manually","soup"},{"Stock owned nearby Other Fridge (debug)","stock"},{"Start test visitor's real supper route","supper"},
        {"Give Welcome Home Cake","cake"},{"Test first-visit eligibility (test child only)","first"},{"Test later-visit rejection (test child only)","later"},{"Give held cake through normal handler","gift"},{"Inspect exact cake delta / repeat guard","cake_receipt"},
        {"Save/read back current food/gift checkpoint","save"},{"Open actual Food Shop nearby","shop"},{"Inspect Soul starter/session receipts","soul_receipts"}
    };
    public static ListTag controls(){var out=new ListTag();for(var entry:BUTTONS){var n=new CompoundTag();n.putString("Label",entry[0]);n.putString("Action",entry[1]);out.add(n);}return out;}
    private static TycoonNpc actor(ServerPlayer p,TycoonData.Resident r){return r!=null&&r.n.hasUUID("Entity")&&p.serverLevel().getEntity(r.n.getUUID("Entity")) instanceof TycoonNpc a&&p.getUUID().equals(a.owner)&&p.distanceToSqr(a)<=32*32?a:null;}
    public static String run(ServerPlayer p,String action){
        if(!p.hasPermissions(2))return "Operator only.";var h=TycoonWorld.home(p);var r=h.state.hasUUID("DebugSelected")?h.residents.get(h.state.getUUID("DebugSelected")):null;var a=actor(p,r);
        if(action.equals("select")){var nearest=p.serverLevel().getEntitiesOfClass(TycoonNpc.class,p.getBoundingBox().inflate(32),npc->!npc.staff()&&p.getUUID().equals(npc.owner)).stream().min(Comparator.comparingDouble(p::distanceToSqr));if(nearest.isEmpty())return "No owned resident nearby.";h.state.putUUID("DebugSelected",nearest.get().resident);TycoonWorld.dirty(p);return "Selected "+nearest.get().getName().getString();}
        if(action.equals("labels")){h.state.putBoolean("DebugFoodStats",!h.state.getBoolean("DebugFoodStats"));TycoonWorld.dirty(p);TycoonFoodSync.send(p,true);return "Live owner stats "+(h.state.getBoolean("DebugFoodStats")?"ON":"OFF")+"; hidden to normal players.";}
        if(Set.of("rotten","excellent","soup","cake","table","fridge").contains(action)){var item=switch(action){case "rotten"->new ItemStack(Items.ROTTEN_FLESH,4);case "excellent"->new ItemStack(Items.GOLDEN_CARROT,4);case "soup"->TycoonFoodContent.owned(p,2,4);case "cake"->TycoonFoodContent.owned(p,7,1);case "table"->TycoonWorld.bound(p,"other_world_supper_table");default->TycoonWorld.bound(p,"other_world_fridge");};return TycoonWorld.deliver(p,item)?"Actual item delivered for testing.":"Free an inventory slot.";}
        if(action.equals("shop")){var desk=h.center().offset(1,0,-1);if(p.serverLevel().dimension()!=TycoonWorld.KEY||p.distanceToSqr(desk.getCenter())>64)return "Approach your personal Work Desk.";TycoonNetwork.open(p,"worlddesk",desk);return "Use the Other World Food tab.";}
        if(action.equals("soul_receipts"))return "Soul balance="+h.state.getLong("SoulEssence")+"; durable credits="+h.state.getCompound("SoulReceipts")+"; reward cooldown="+Math.max(0,h.state.getLong("SoulVisitRewardAfter")-h.clock());
        if(action.equals("save")){try{return TycoonCheckpoints.home(p)&&TycoonCheckpoints.player(p)?"Food/gift state and actual player inventory match disk read-back; this is not a restarted-client test.":"Checkpoint read-back mismatch; no test pass claimed.";}catch(java.io.IOException e){return "Checkpoint unavailable: "+e.getMessage();}}
        if(action.equals("quality"))return "Held food quality="+TycoonFoodQuality.tier(p.getMainHandItem())+", Hunger restoration="+TycoonFoodQuality.hunger(p.getMainHandItem())+(r==null?"":"; manual Neglect="+TycoonFoodQuality.neglect(r,p.getMainHandItem(),true)+", self="+TycoonFoodQuality.neglect(r,p.getMainHandItem(),false));
        if(action.equals("empty")||action.equals("stock")){String key=p.serverLevel().dimension()==TycoonWorld.KEY?"OtherFurniture":"Furniture";for(String location:h.state.getCompound(key).getAllKeys()){
            var pos=BlockPos.of(Long.parseLong(location));if(p.distanceToSqr(pos.getCenter())>64||!(p.serverLevel().getBlockEntity(pos) instanceof TycoonBlockEntity be)||!p.getUUID().equals(be.owner)||!be.kind().endsWith("fridge"))continue;
            if(action.equals("stock")){if(!be.kind().equals("other_world_fridge"))return "Use an Other World Fridge.";for(int i=0;i<be.getContainerSize();i++)if(be.getItem(i).isEmpty()){be.setItem(i,TycoonFoodContent.owned(p,2,4));return "Four real debug soup servings stocked.";}return "Fridge is full.";}
            for(int i=0;i<be.getContainerSize();i++){var item=be.getItem(i);if(!item.isEmpty()){if(!TycoonWorld.deliver(p,item.copy()))return "Free inventory space; remaining stock preserved.";be.removeItem(i,item.getCount());}}return "Fridge emptied into your actual inventory.";
        }return "Approach an owned fridge.";}
        if(r==null||a==null)return "Select an owned resident in this dimension within32blocks.";
        if(action.equals("inspect")||action.equals("cake_receipt"))return r.name()+": Hunger="+r.need("Hunger")+", Neglect="+r.need("Neglect")+", Trustness="+TycoonDialogue.trust(r)+", Suspicion="+TycoonDialogue.suspicion(r)+", eligibleCake="+TycoonWelcomeCake.eligible(p,r)+", cakeReceived="+r.n.getBoolean("welcome_home_cake_received")+", gift="+r.n.getCompound("CakeGift")+", meal="+r.n.getCompound("Meal")+", lastMeal="+r.n.getString("LastMealReceipt");
        if(action.equals("gift"))return TycoonWelcomeCake.give(p,a)?"Normal gift handler checked; inspect actual values/receipt.":"Hold the real Welcome Home Cake.";
        if(action.equals("manual"))return TycoonCare.serve(p,a)?"Normal serving handler checked.":"Hold actual food, approach a hungry resident and wait for meal cooldown.";
        if(!r.n.getBoolean("DebugChild"))return "Checkpoint mutations only target explicit test residents; real children preserved.";
        switch(action){case "hunger"->r.need("Hunger",10);case "neglect"->r.need("Neglect",40);case "missed"->{r.n.putInt("HungryMealTicks",TycoonFoodConfig.EPISODE.get()-20);TycoonMeals.missed(p,r,p.serverLevel().dimension()==TycoonWorld.KEY);}case "self"->{r.need("Hunger",10);r.n.putLong("MealAfter",0);r.n.putLong("MealRequest",h.clock()-2000);r.n.putLong("FoodRetry",0);}case "supper"->{r.need("Hunger",40);r.n.putLong("MealAfter",0);TycoonMeals.otherWorld(p,r,a);}case "first","later"->{if(!r.n.getCompound("Visit").getString("Phase").equals("INSIDE"))return "Invite this test child physically first.";r.n.putInt("VisitCount",action.equals("first")?1:2);r.n.putBoolean("FirstVisitEnded",action.equals("later"));r.n.putUUID("FirstVisitSession",r.n.getCompound("Visit").getUUID("Session"));if(action.equals("first")){r.n.remove("welcome_home_cake_received");r.n.remove("CakeGiftReceipt");}}default->{return "Unknown food debug action.";}}
        TycoonWorld.dirty(p);TycoonFoodSync.send(p,true);return "Test checkpoint applied. Real movement, food and gift handlers remain required.";
    }
    private TycoonFoodDebug(){}
}
