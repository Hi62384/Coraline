package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

public final class TycoonPhaseFourDebug {
    public static void controls(ListTag list){String[][] rows={{"Phase 4 — inspect selected proposal / conversion","inspect"},{"Phase 4 — set test Trustness exactly 1","ready"},{"Phase 4 — set test Trustness 0.99","not_ready"},{"Phase 4 — explain proposal normally","offer"},{"Phase 4 — confirm proposal normally","confirm"},{"Phase 4 — finish pending test ritual","finish"},{"Phase 4 — inspect unique soul collection","souls"},{"Phase 4 — inspect farms / rates / prices","farms"},{"Phase 4 — force test visitor discovery","discover"},{"Phase 4 — set test breach to 9 minutes","breach"},{"Phase 4 — concede first active breach","concede"},{"Phase 4 — save/read back economy","save"}};for(var entry:rows){var row=new CompoundTag();row.putString("Label",entry[0]);row.putString("Action","p4_"+entry[1]);list.add(row);}}
    public static String run(ServerPlayer p,String action){
        if(!p.hasPermissions(2))return "Operator only.";var h=TycoonWorld.home(p);var r=h.state.hasUUID("DebugSelected")?h.residents.get(h.state.getUUID("DebugSelected")):null;
        if(action.equals("souls"))return TycoonSouls.collection(h).toString();if(action.equals("farms"))return TycoonSoulFarms.projection(h)+"; next price="+TycoonSoulFarms.priceOrdinal(h.state.getInt("FarmPurchases")+1)+"; balance="+TycoonSoulLedger.display(h);
        if(action.equals("save")){try{return TycoonCheckpoints.home(p)?"Economy saved and read back.":"Read-back failed.";}catch(java.io.IOException failed){return "Checkpoint unavailable.";}}
        if(action.equals("concede")){for(String key:TycoonSoulFarms.farms(h).getAllKeys()){var n=TycoonSoulFarms.farms(h).getCompound(key);if(TycoonSoulFarms.breaching(n))return TycoonSoulFarms.intervene(p,n.getUUID("Id"),true);}return "No active breach.";}
        if(r==null)return "Select an owned test resident.";if(action.equals("inspect"))return "Trustness="+TycoonDialogue.trust(r)+"; suspicion="+TycoonDialogue.suspicion(r)+"; proposals="+r.n.getList("ProposalMemories",10)+"; ritual="+r.n.getCompound("ConversionRitual");
        if(!r.n.getBoolean("DebugChild")||!r.active())return "Mutations require an active test resident.";
        switch(action){
            case "ready","not_ready"->TycoonDialogue.set(r,action.equals("ready")?1:.99,TycoonDialogue.suspicion(r));
            case "offer"->{return TycoonSouls.propose(p,r,false);}
            case "confirm"->{return TycoonSouls.propose(p,r,true);}
            case "finish"->{if(!r.n.contains("ConversionRitual"))return "Start a valid test proposal first.";r.n.getCompound("ConversionRitual").putInt("Progress",140);}
            case "discover","breach"->{var all=TycoonSoulFarms.farms(h);if(all.isEmpty())return "Purchase a real farm and assign a soul first.";var farm=all.getCompound(all.getAllKeys().iterator().next());if(!TycoonVisits.inside(r)||TycoonSoulFarms.assigned(h,farm.getUUID("Id")).isEmpty())return "A visiting test resident and occupied farm are required.";TycoonSoulFarms.discover(p,r,farm);if(action.equals("breach"))farm.getCompound("Breach").putInt("Progress",10800);}
            default->{return "Unknown Phase 4 control.";}
        }TycoonWorld.dirty(p);return "Selected test checkpoint updated.";
    }
    private TycoonPhaseFourDebug(){}
}
