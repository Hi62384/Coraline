package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Mutations target explicitly marked test residents; normal payments remain real. */
public final class TycoonPhaseThreeDebug {
    public static void controls(ListTag list){
        String[][] rows={{"Phase 3 — inspect selected personality / analysis","inspect"},{"Phase 3 — set test Giftableness negative","negative"},{"Phase 3 — set test Giftableness positive","positive"},{"Phase 3 — complete test doll analysis","analysis"},{"Phase 3 — assign test visitor to first attraction","assign"},{"Phase 3 — force test wandering","wander"},{"Phase 3 — return test child to house","return"},{"Phase 3 — inspect activity / gifts / evidence","memories"},{"Phase 3 — compare configured attraction profiles","profiles"},{"Phase 3 — inspect spies / temporary markers","spies"},{"Phase 3 — save/read back owner state","save"}};
        for(var entry:rows){var n=new CompoundTag();n.putString("Label",entry[0]);n.putString("Action","p3_"+entry[1]);list.add(n);}
    }
    public static String run(ServerPlayer p,String action){
        if(!p.hasPermissions(2))return "Operator only.";var h=TycoonWorld.home(p);var r=h.state.hasUUID("DebugSelected")?h.residents.get(h.state.getUUID("DebugSelected")):null;
        if(action.equals("profiles"))return Arrays.stream(TycoonAttractionType.values()).map(t->t.label+" "+Arrays.toString(t.profile)).toList().toString();
        if(action.equals("spies"))return h.state.getCompound("Spies")+"; markers="+TycoonSpies.markers(h);
        if(action.equals("save")){try{return TycoonCheckpoints.home(p)?"Owner state saved and read back.":"Read-back failed.";}catch(java.io.IOException failure){return "Checkpoint failed: "+failure.getMessage();}}
        if(r==null)return "Select an owned resident first.";
        if(action.equals("inspect"))return TycoonPersonality.exact(r)+"; analysis="+TycoonDollAnalysis.progress(r);
        if(action.equals("memories"))return "Activity="+r.n.getCompound("Activity")+"; gifts="+r.n.getList("GiftMemories",10)+"; evidence="+r.n.getList("EvidenceMemories",10);
        if(!r.n.getBoolean("DebugChild")||!r.active())return "Mutating controls require an active test resident.";
        String result;
        switch(action){
            case "negative","positive"->{TycoonPersonality.set(r,"Giftableness",action.equals("negative")?-.8:.8);result="Test Giftableness set; cake remains independent.";}
            case "analysis"->{r.n.putLong("AnalysisTicks",TycoonDollAnalysis.COMPLETE);result="Test analysis completed.";}
            case "assign"->{var keys=TycoonAttractions.records(h).getAllKeys();if(keys.isEmpty())return "Purchase a real attraction first.";return TycoonAttractions.assign(p,r,UUID.fromString(keys.iterator().next()));}
            case "wander"->{var n=r.n.getCompound("Activity");if(!n.hasUUID("Structure"))return "Assign the test visitor first.";n.putString("State","WANDERING");n.putLong("WanderAt",h.clock());n.putLong("WanderTarget",p.blockPosition().asLong());result="Test visitor follows a real route; no automatic suspicion or report.";}
            case "return"->{return TycoonAttractions.returnHouse(p,r);}
            default->{return "Unknown Phase 3 control.";}
        }TycoonWorld.dirty(p);return result;
    }
    private TycoonPhaseThreeDebug(){}
}
