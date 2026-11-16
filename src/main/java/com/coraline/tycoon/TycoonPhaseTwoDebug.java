package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Explicit operator/developer tools. Forced actors/outcomes target test residents. */
public final class TycoonPhaseTwoDebug {
    private static final String[][] BUTTONS={
        {"Dolls — open workshop nearby","workshop"},{"Dolls — craft selected test resident","doll"},{"Dolls — inspect exact identity","doll_id"},{"Dolls — deliver held doll normally","give"},{"Dolls — pick up bedside doll","remove"},{"Dolls — verify owner/resident/doll nonce","verify"},{"Dolls — wrong-resident rejection test","wrong"},
        {"Visits — force nighttime","night"},{"Visits — list eligible residents","eligible"},{"Visits — Spawn Mice normally","mice"},{"Visits — prepare selected test invitation","invite"},{"Visits — inspect group and transfer phases","group"},{"Visits — portal-transfer selected test resident","transfer"},{"Visits — force test group crossing","force_group"},{"Visits — show count","count"},{"Visits — Send All Visitors Home","home"},{"Visits — force blocked test departure","blocked"},{"Visits — recover canonical test visitor","recover"},
        {"Relationships — exact Trustness/Suspicion","relationship"},{"Relationships — set test Trustness0.5","trust"},{"Relationships — set test Suspicion0.5","suspicion"},{"Relationships — inspect mandatory first-visit defaults","defaults"},{"Relationships — sample contextual dialogue","dialogue"},{"Relationships — apply honest response once","response"},{"Relationships — inspect personality modifiers","personality"},{"Relationships — trigger bounded social check","social"},{"Relationships — show recent history","history"},
        {"Terrain — inspect purchased grid","grid"},{"Terrain — grant64 Power","power"},{"Terrain — purchase connected grassland","land"},{"Terrain — upgrade first owned chunk","upgrade"},{"Terrain — check deterministic continuity","continuity"},{"Terrain — compare all six tier plans","tiers"},{"Terrain — check16x16 boundary","boundary"},{"Terrain — show Goodness breakdown","goodness"},{"Terrain — inspect protected construction","protected"},
        {"House — purchase next tier normally","house"},{"House — inspect tier and generated cells","house_id"},{"House — verify real desk/door preserved","core"},
        {"Sky — purchase/select next sky normally","sky"},{"Sky — select owned blue sky","blue"},{"Sky — reset owned sky to white","white"},{"Sky — inspect owner-specific rendering state","sky_owner"},
        {"Persistence — save/read back owner state","save"},{"Persistence — simulate story retirement on copies","reset"},{"Persistence — inspect permanent ownership","owner"},{"Persistence — end interrupted group safely","interrupt"},{"Persistence — detect duplicate NPCs","duplicates"}
    };
    public static ListTag controls(){var out=new ListTag();for(var button:BUTTONS){var row=new CompoundTag();row.putString("Label",button[0]);row.putString("Action",button[1]);out.add(row);}TycoonPhaseThreeDebug.controls(out);TycoonPhaseFourDebug.controls(out);TycoonPhaseFiveDebug.controls(out);return out;}
    private static TycoonData.Resident selected(ServerPlayer p){var h=TycoonWorld.home(p);return h.state.hasUUID("DebugSelected")?h.residents.get(h.state.getUUID("DebugSelected")):null;}
    private static boolean test(TycoonData.Resident r){return r!=null&&r.n.getBoolean("DebugChild")&&r.active();}
    public static String run(ServerPlayer p,String action){
        if(action.startsWith("p3_"))return TycoonPhaseThreeDebug.run(p,action.substring(3));
        if(action.startsWith("p4_"))return TycoonPhaseFourDebug.run(p,action.substring(3));
        if(action.startsWith("p5_"))return TycoonPhaseFiveDebug.run(p,action.substring(3));
        if(!p.hasPermissions(2))return "Operator permission required.";var h=TycoonWorld.home(p);var r=selected(p);String result;
        switch(action){
            case "workshop"->{var at=h.center().offset(1,0,-1);TycoonNetwork.open(p,"worlddesk",at);return "Open the Doll Workshop tab.";}
            case "doll"->{if(!test(r))return "Select an owned test resident in the Phase1 debugger.";return TycoonDolls.craft(p,r.id,r.n.getCompound("Doll").hasUUID("Id"));}
            case "doll_id","verify"->{return r==null?"Select a resident first.":"Owner "+h.owner+"; resident "+r.id+"; doll "+r.n.getCompound("Doll")+"; live valid="+TycoonDolls.valid(p,r);}
            case "give"->{if(r==null||!r.n.hasUUID("Entity")||!(p.server.overworld().getEntity(r.n.getUUID("Entity")) instanceof TycoonNpc actor)||p.level()!=actor.level()||p.distanceToSqr(actor)>25)return "Approach the selected resident with their real doll.";return TycoonDolls.give(p,actor)?"Doll handoff processed.":"Hold their doll.";}
            case "remove"->{if(r==null||!r.n.getCompound("Doll").hasUUID("Entity")||!(p.server.overworld().getEntity(r.n.getCompound("Doll").getUUID("Entity")) instanceof TycoonDollEntity doll))return "Placed doll unavailable.";TycoonDolls.pickup(p,doll);return "Pickup attempted using owner/range/inventory checks.";}
            case "wrong"->{if(!test(r))return "Select a test resident.";var item=p.getMainHandItem();return !item.is(TycoonPhaseTwoContent.DOLL.get())?"Hold a real doll.":item.hasTag()&&item.getTag().hasUUID("Resident")&&!r.id.equals(item.getTag().getUUID("Resident"))?"Identity mismatch confirmed: normal interaction rejects without consumption.":"Hold a different resident's doll to test a wrong handoff.";}
            case "night"->{p.server.overworld().setDayTime(Math.floorDiv(p.server.overworld().getDayTime(),24000)*24000+13000);result="Overworld time set to nighttime by operator.";}
            case "eligible"->{return TycoonVisits.eligible(p).stream().map(TycoonData.Resident::name).toList().toString();}
            case "mice"->{return TycoonVisits.invite(p);}
            case "invite"->{if(!test(r))return "Select a test resident.";r.need("Neglect",60);r.need("Hunger",90);r.need("Thirst",90);result="Selected test resident welfare checkpoint set; a real delivered doll and nighttime are still required.";}
            case "group","count"->{return "Inside="+TycoonVisits.count(h)+"; session="+h.state.getBoolean("VisitActive")+"; "+h.residents.values().stream().filter(TycoonVisits::visiting).map(child->child.name()+":"+child.n.getCompound("Visit").getString("Phase")).toList();}
            case "transfer"->{if(!test(r))return "Select a test resident.";return TycoonVisits.debugEnter(p,r);}
            case "force_group"->{int count=0;for(var child:h.residents.values())if(test(child)){TycoonVisits.debugEnter(p,child);count++;}result="Attempted same-identity portal entry for "+count+" test residents.";}
            case "home","interrupt"->{return TycoonVisits.sendHome(p);}
            case "blocked","recover"->{if(!test(r)||!TycoonVisits.inside(r))return "Select a visiting test resident.";var visit=r.n.getCompound("Visit");visit.putString("Phase","LEAVING");visit.putLong("LeavingAt",h.clock()-1300);result="Selected test resident will use the bounded same-identity return recovery.";}
            case "relationship"->{return r==null?"Select a resident.":"Trustness="+TycoonDialogue.trust(r)+"; Suspicion="+TycoonDialogue.suspicion(r);}
            case "trust","suspicion"->{if(!test(r))return "Select a test resident.";TycoonDialogue.set(r,action.equals("trust")?.5:TycoonDialogue.trust(r),action.equals("suspicion")?.5:TycoonDialogue.suspicion(r));result="Selected test relationship updated.";}
            case "defaults"->{return "First-entry Suspicion is exactly0.5; configured initial Trustness="+TycoonPhaseTwoConfig.INITIAL_TRUST.get()+". Existing initialized relationships are never reset by this check.";}
            case "dialogue"->{if(r==null)return "Select a resident.";return TycoonDialogue.open(p,r).getString("Dialogue");}
            case "response"->{if(!test(r))return "Select a visiting test resident.";var dialogue=TycoonDialogue.open(p,r);return dialogue.hasUUID("Question")?TycoonDialogue.respond(p,r,dialogue.getUUID("Question"),0):"Conversation is on cooldown.";}
            case "personality"->{return r==null?"Select a resident.":r.n.getCompound("Traits").toString();}
            case "social"->{h.state.putLong("SocialAfter",0);result="The next visiting social tick may converse if a suitable real partner is nearby.";}
            case "history"->{return r==null?"Select a resident.":r.n.getList("DialogueHistory",10).toString();}
            case "grid"->{return "Owned tiles: "+h.state.getCompound("Terrain").getAllKeys();}
            case "power"->{TycoonWorld.deliver(p,new ItemStack(TycoonContent.VOID_POWER.get(),64));result="Granted64 debug Power.";}
            case "land"->{for(int x=-8;x<8;x++)for(int z=-8;z<8;z++)if(TycoonLandscape.adjacent(h,x,z)&&!h.state.getCompound("Terrain").contains(TycoonLandscape.key(x,z)))return TycoonLandscape.purchase(p,x,z,1);return "No connected unowned tile.";}
            case "upgrade"->{for(String key:h.state.getCompound("Terrain").getAllKeys()){int old=h.state.getCompound("Terrain").getCompound(key).getInt("Tier");if(old>=5)continue;String[] parts=key.split(",");return TycoonLandscape.purchase(p,Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),old+1);}return "No owned upgradeable terrain.";}
            case "continuity"->{int max=0;for(int x=-32;x<32;x++)for(int z=-32;z<32;z++)max=Math.max(max,Math.abs(TycoonLandscape.height(h,x,z)-TycoonLandscape.height(h,x+1,z)));return "Global deterministic surface; maximum sampled adjacent height step="+max+". Tiers share boundary heights.";}
            case "tiers"->{var sizes=new ArrayList<Integer>();for(int tier=0;tier<6;tier++)sizes.add(TycoonLandscape.terrain(h,1,0,tier).size());return "Six distinct plans, cell counts="+sizes+"; native visual capture checks appearance separately.";}
            case "boundary"->{return "Valid [-8..7] x [-8..7] =256 tiles; outside(-9,0)="+TycoonLandscape.inBounds(-9,0)+", outside(8,0)="+TycoonLandscape.inBounds(8,0);}
            case "goodness"->{return TycoonLandscape.goodness(h).toString();}
            case "protected"->{return "Protected player edits="+h.state.getCompound("PlayerEdits").size()+"; active job="+h.state.contains("BuildJob");}
            case "house"->{return TycoonHouses.purchase(p,Math.min(5,h.state.getInt("HouseTier")+1));}
            case "house_id"->{return "House tier="+h.state.getInt("HouseTier")+"; generated cells="+h.state.getCompound("HouseGenerated").getIntArray("Cells").length;}
            case "core"->{var l=p.server.getLevel(TycoonWorld.KEY);return l==null?"Dimension unavailable.":"Desk="+l.getBlockState(h.center().offset(1,0,-1)).is(TycoonContent.DESK.get())+"; door="+l.getBlockState(h.center().offset(-1,0,-1)).is(TycoonContent.DOOR.get());}
            case "sky"->{return TycoonSkies.select(p,Math.min(5,h.state.getInt("SkyTier")+1));}
            case "blue"->{return TycoonSkies.select(p,1);}
            case "white"->{return TycoonSkies.select(p,0);}
            case "sky_owner","owner"->{return "Owner="+h.owner+"; region="+h.slot+"; center="+h.center()+"; selected sky="+h.state.getInt("SkyTier");}
            case "save"->{try{return TycoonCheckpoints.home(p)?"Owner/visitor state saved and read back exactly.":"Checkpoint mismatch.";}catch(java.io.IOException failure){return "Save failed: "+failure.getMessage();}}
            case "reset"->{return TycoonDebug.run(p,"reset_simulation");}
            case "duplicates"->{return TycoonDebug.run(p,"duplicates");}
            default->{return "Unknown Phase2 debug action.";}
        }
        TycoonWorld.dirty(p);return result;
    }
    private TycoonPhaseTwoDebug(){}
}
