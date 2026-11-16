package com.coraline.tycoon;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

/** Operator observability; accelerated mutations are restricted to a debug household. */
public final class TycoonPhaseFiveDebug {
    public static void controls(ListTag list){String[][] entries={{"Inspect workers / salary / blockers","workers"},{"Inspect converters / actual output","machines"},{"Inspect all automation policies","policies"},{"Inspect cash flow / active-minute history","finance"},{"Inspect fictional stock quotes / portfolio","market"},{"Inspect real supply storage / carries","supplies"},{"Pause every worker and machine","pause"},{"Test household: supply 64 Power and real ingredients","stock"},{"Test household: fund 100 Essence","fund"},{"Test household: unlock first worker Level 5","level"},{"Test household: finish next paid converter cycle","cycle"},{"Save/read back automation state","save"}};for(var entry:entries){var row=new CompoundTag();row.putString("Label","Phase 5 — "+entry[0]);row.putString("Action","p5_"+entry[1]);list.add(row);}}
    public static String run(ServerPlayer p,String action){
        if(!p.hasPermissions(2))return "Operator only.";var h=TycoonWorld.home(p);
        switch(action){
            case "workers"->{return TycoonWorkers.projection(h).toString();}
            case "machines"->{return TycoonMachines.projection(h).toString();}
            case "policies"->{return TycoonPolicies.state(h).toString();}
            case "finance"->{return TycoonFinance.view(h).toString();}
            case "market"->{return TycoonMarket.view(p).toString();}
            case "supplies"->{return "Supplies="+h.state.getCompound("Warehouse")+"; produced="+h.state.getCompound("ProducedResources")+"; reservations="+h.state.getCompound("WorkerReservations");}
            case "save"->{try{return TycoonCheckpoints.home(p)?"Automation saved and read back exactly.":"Checkpoint mismatch.";}catch(java.io.IOException failure){return "Checkpoint failed.";}}
            case "pause"->{for(String key:TycoonWorkers.workers(h).getAllKeys()){var w=TycoonWorkers.workers(h).getCompound(key);w.putBoolean("Break",true);w.putBoolean("Paid",false);}for(String key:TycoonMachines.machines(h).getAllKeys())TycoonMachines.machines(h).getCompound(key).putBoolean("Break",true);TycoonWorld.dirty(p);return "All automation paused; resources and pending deliveries retained.";}
        }
        if(!h.state.getBoolean("DebugActivation"))return "Accelerated controls require a debugger-activated test household.";
        switch(action){
            case "stock"->{for(var item:java.util.List.of(TycoonContent.VOID_POWER.get(),Items.EMERALD,Items.AMETHYST_SHARD,Items.COAL,Items.REDSTONE,Items.BREAD,Items.BUCKET))TycoonWarehouse.add(h,item,64);}
            case "fund"->TycoonSoulLedger.creditMicros(h,100*TycoonSoulLedger.SCALE);
            case "level"->{var all=TycoonWorkers.workers(h);if(all.isEmpty())return "Hire a real worker first.";var w=all.getCompound(all.getAllKeys().iterator().next());w.putInt("Unlocked",5);w.putInt("Operating",5);}
            case "cycle"->{for(String key:TycoonMachines.machines(h).getAllKeys())TycoonMachines.machines(h).getCompound(key).putInt("Progress",1180);}
            default->{return "Unknown Phase 5 control.";}
        }TycoonWorld.dirty(p);return "Test checkpoint updated; normal payment, supply, ownership and capacity checks still apply.";
    }
    private TycoonPhaseFiveDebug(){}
}
