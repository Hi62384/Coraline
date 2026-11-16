package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Selection IDs only: budgets, prices, roles and all patches stay server-side. */
public final class TycoonAutomationMenus {
    public static void enrich(ServerPlayer p,CompoundTag n){
        var h=TycoonWorld.home(p);n.put("Workers",TycoonWorkers.projection(h));n.put("Machines",TycoonMachines.projection(h));n.put("Policies",TycoonPolicies.state(h).copy());n.put("Finances",TycoonFinance.view(h));n.put("Market",TycoonMarket.view(p));n.put("Warehouse",TycoonWarehouse.projection(h.state.getCompound("Warehouse")));n.put("ProducedResources",TycoonWarehouse.projection(h.state.getCompound("ProducedResources")));n.putInt("SupplyCapacity",TycoonWarehouse.capacity(h));n.putLong("SupplyUsed",TycoonWarehouse.total(h));n.putLong("ProducedUsed",TycoonMachines.producedCount(h));
        var catalog=new ListTag();for(var type:TycoonWorkerType.values()){var row=new CompoundTag();row.putString("Id",type.id());row.putString("Name",type.label);row.putInt("Price",TycoonAutomationConfig.PRICES.get(type.id()).get());row.putBoolean("Overworld",type.overworld);catalog.add(row);}n.put("WorkerCatalog",catalog);
        var recipes=new ListTag();for(String id:TycoonAutomationConfig.OUTPUTS.keySet()){var row=new CompoundTag();row.putString("Id",id);row.putInt("Output",TycoonAutomationConfig.OUTPUTS.get(id).get());row.putDouble("Cost",TycoonAutomationConfig.OUTPUT_COSTS.get(id).get());recipes.add(row);}n.put("ConverterRecipes",recipes);
        var policies=new ListTag();for(String key:TycoonPolicies.FLAGS){var row=new CompoundTag();row.putString("Key",key);row.putBoolean("Flag",true);policies.add(row);}for(var e:TycoonPolicies.NUMBERS.entrySet()){var row=new CompoundTag();row.putString("Key",e.getKey());row.putLong("Min",e.getValue()[1]);row.putLong("Max",e.getValue()[2]);policies.add(row);}for(var e:TycoonPolicies.CHOICES.entrySet()){var row=new CompoundTag();row.putString("Key",e.getKey());var choices=new ListTag();e.getValue().forEach(choice->choices.add(StringTag.valueOf(choice)));row.put("Choices",choices);policies.add(row);}for(var type:TycoonAttractionType.values()){var row=new CompoundTag();row.putString("Key","allow_structure_"+type.id());row.putBoolean("Flag",true);policies.add(row);}for(var company:TycoonMarket.COMPANIES){var row=new CompoundTag();row.putString("Key","allow_company_"+company.id());row.putBoolean("Flag",true);policies.add(row);}n.put("PolicyControls",policies);
    }
    public static String action(ServerPlayer p,String command){
        try{
            String[] pair=command.split(":",2);String key=pair[0],value=pair.length>1?pair[1]:"";String[] parts=value.split(",");
            return switch(key){
                case "worker_hire"->TycoonWorkers.hire(p,value);
                case "worker_upgrade"->TycoonWorkers.upgrade(p,UUID.fromString(value));
                case "worker_set"->parts.length==3?TycoonWorkers.configure(p,UUID.fromString(parts[0]),parts[1],parts[2]):"Invalid worker setting.";
                case "policy"->{String[] setting=value.split("=",2);yield setting.length==2?TycoonPolicies.set(p,setting[0],setting[1]):"Invalid policy.";}
                case "machine_build"->parts.length==3?TycoonMachines.build(p,parts[0],Integer.parseInt(parts[1]),Integer.parseInt(parts[2])):"Invalid machine plot.";
                case "machine_upgrade"->TycoonMachines.upgrade(p,UUID.fromString(value));
                case "machine_repair"->TycoonMaintenance.repair(p,UUID.fromString(value),true);
                case "structure_repair"->TycoonMaintenance.repair(p,UUID.fromString(value),false);
                case "machine_set"->{String[] setting=value.split(",",3);yield setting.length==3?TycoonMachines.configure(p,UUID.fromString(setting[0]),setting[1],setting[2]):"Invalid machine setting.";}
                case "supply_deposit"->TycoonWarehouse.deposit(p,Math.min(p.getMainHandItem().getCount(),Integer.parseInt(value)));
                case "supply_collect"->TycoonWarehouse.collect(p,value,false);
                case "resource_collect"->TycoonWarehouse.collect(p,value,true);
                case "supply_collect_many"->TycoonWarehouse.collectMany(p,value,false);
                case "resource_collect_many"->TycoonWarehouse.collectMany(p,value,true);
                case "storage_upgrade"->TycoonWarehouse.expand(p);
                case "trade"->{String[] order=value.split(",");yield order.length==5?TycoonMarket.trade(p,order[0],Integer.parseInt(order[1]),order[2].equals("buy"),Long.parseLong(order[3]),UUID.fromString(order[4]),false):"Invalid stock order.";}
                default->"Unknown automation selection.";
            };
        }catch(IllegalArgumentException|ArithmeticException invalid){return "Invalid or stale automation selection.";}
    }
    public static boolean handles(String command){String key=command.split(":",2)[0];return Set.of("worker_hire","worker_upgrade","worker_set","policy","machine_build","machine_upgrade","machine_repair","structure_repair","machine_set","supply_deposit","supply_collect","resource_collect","supply_collect_many","resource_collect_many","storage_upgrade","trade").contains(key);}
    private TycoonAutomationMenus(){}
}
