package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Server-only menu projection: public infrastructure, private resident choices. */
public final class TycoonPhaseTwoMenus {
    public static void enrich(ServerPlayer p,String mode,UUID child,CompoundTag n){
        var h=TycoonWorld.home(p);
        if(mode.equals("visitor")&&child!=null){var r=h.residents.get(child);if(r!=null){n.putUUID("VisitorId",child);var talk=TycoonDialogue.open(p,r);for(String key:talk.getAllKeys())n.put(key,talk.get(key).copy());n.put("Attractions",TycoonAttractions.projection(p,true));n.putString("Activity",r.n.getString("Task"));n.putBoolean("ProposalPending",r.n.getLong("ProposalConfirmUntil")>h.clock());}return;}
        if(!mode.equals("worlddesk")&&!mode.equals("debug2"))return;
        TycoonFoodShop.enrich(n);
        TycoonAutomationMenus.enrich(p,n);
        n.putString("SoulBalanceExact",TycoonSoulLedger.display(h));n.put("SoulFarms",TycoonSoulFarms.projection(h));n.putLong("NextFarmSoulPrice",TycoonSoulFarms.priceOrdinal(h.state.getInt("FarmPurchases")+1));n.putInt("FarmPowerPrice",TycoonSoulConfig.FARM_POWER.get());
        var souls=new ListTag();var collection=TycoonSouls.collection(h);for(String id:collection.getAllKeys()){var row=collection.getCompound(id).copy();row.remove("History");souls.add(row);}n.put("Souls",souls);
        n.put("Attractions",TycoonAttractions.projection(p,false));n.put("SpyMarkers",TycoonSpies.markers(h));n.put("Spies",h.state.getCompound("Spies").copy());
        var catalog=new ListTag();for(var type:TycoonAttractionType.values()){var row=new CompoundTag();row.putString("Id",type.id());row.putString("Name",type.label);row.putInt("Price",TycoonAttractionConfig.PRICES.get(type.id()).get());row.putInt("Capacity",type.capacity);catalog.add(row);}n.put("AttractionCatalog",catalog);
        n.putLong("Center",h.center().asLong());n.putBoolean("Nighttime",TycoonVisits.nighttime(p));n.putLong("VisitorCount",TycoonVisits.count(h));n.putBoolean("VisitActive",h.state.getBoolean("VisitActive"));n.putInt("Eligible",TycoonVisits.eligible(p).size());n.putInt("HouseTier",h.state.getInt("HouseTier"));n.putInt("SkyTier",h.state.getInt("SkyTier"));
        var goodness=TycoonLandscape.goodness(h);n.putDouble("Goodness",goodness.total());n.putDouble("TerrainGoodness",goodness.terrain());n.putDouble("HouseGoodness",goodness.house());n.putDouble("SkyGoodness",goodness.sky());
        n.putInt("DollCost",TycoonPhaseTwoConfig.DOLL.get());n.putInt("ReplacementCost",TycoonPhaseTwoConfig.REPLACEMENT.get());
        var dolls=new ListTag();for(var r:h.residents.values())if(r.active()){
            var row=new CompoundTag();row.putUUID("Id",r.id);row.putString("Name",r.name());row.putInt("Appearance",r.n.getInt("Appearance"));row.putString("Bed",r.bed()==null?"Unassigned":r.bed().toShortString());var d=r.n.getCompound("Doll");row.putBoolean("Crafted",d.hasUUID("Id"));row.putBoolean("Delivered",d.getBoolean("Delivered"));row.putString("Status",d.getString("Status"));row.putString("VisitPhase",r.n.getCompound("Visit").getString("Phase"));dolls.add(row);
        }n.put("Dolls",dolls);
        var analysis=new CompoundTag();for(var r:h.residents.values())if(r.active()&&r.n.getCompound("Doll").hasUUID("Id"))analysis.put(r.id.toString(),TycoonDollAnalysis.view(r));n.put("DollAnalysis",analysis);
        var tiles=new ListTag();var owned=h.state.getCompound("Terrain");for(int x=-8;x<8;x++)for(int z=-8;z<8;z++){var row=new CompoundTag();row.putInt("X",x);row.putInt("Z",z);var tile=owned.getCompound(TycoonLandscape.key(x,z));row.putBoolean("Owned",!tile.isEmpty());row.putInt("Tier",tile.isEmpty()?-1:tile.getInt("Tier"));row.putBoolean("Available",!tile.isEmpty()||TycoonLandscape.adjacent(h,x,z));row.putBoolean("Core",(x==-1||x==0)&&(z==-1||z==0));tiles.add(row);}n.put("Tiles",tiles);
        var terrain=new int[6];var houses=new int[6];var skies=new int[6];for(int i=0;i<6;i++){terrain[i]=TycoonPhaseTwoConfig.TERRAIN[i].get();houses[i]=TycoonPhaseTwoConfig.HOUSE[i].get();skies[i]=TycoonPhaseTwoConfig.SKY[i].get();}n.putIntArray("TerrainPrices",terrain);n.putIntArray("HousePrices",houses);n.putIntArray("SkyPrices",skies);n.put("OwnedSkies",h.state.getCompound("OwnedSkies").copy());
        if(h.state.contains("BuildJob")){var job=h.state.getCompound("BuildJob");n.putString("Construction",job.getString("Name")+" — "+job.getInt("Cursor")+" / "+job.getCompound("After").getIntArray("Cells").length+" blocks");}
        if(mode.equals("debug2"))n.put("DebugControls",TycoonPhaseTwoDebug.controls());
    }

    public static String action(ServerPlayer p,String command){
        if(TycoonAutomationMenus.handles(command))return TycoonAutomationMenus.action(p,command);
        String[] pair=command.split(":",2);String key=pair[0],value=pair.length==2?pair[1]:"";
        try{return switch(key){
            case "doll"->TycoonDolls.craft(p,UUID.fromString(value),false);
            case "replace"->TycoonDolls.craft(p,UUID.fromString(value),true);
            case "bed"->TycoonDolls.reassign(p,UUID.fromString(value));
            case "invite_preview"->{var h=TycoonWorld.home(p);var group=TycoonVisits.eligible(p);var ids=new ListTag();group.forEach(r->ids.add(StringTag.valueOf(r.id.toString())));h.state.put("InvitationPreview",ids);h.state.putLong("InvitationPreviewAt",h.clock());yield TycoonVisits.nighttime(p)?"Eligible visitors: "+group.size()+". Confirm Spawn Mice to invite this group.":"The mice only come at night.";}
            case "invite_confirm"->{var h=TycoonWorld.home(p);if(!h.state.contains("InvitationPreviewAt")||h.clock()-h.state.getLong("InvitationPreviewAt")>600)yield "Preview eligible visitors before confirming.";var now=TycoonVisits.eligible(p).stream().map(r->r.id.toString()).collect(java.util.stream.Collectors.toSet());var before=new HashSet<String>();h.state.getList("InvitationPreview",8).forEach(id->before.add(id.getAsString()));if(!now.equals(before))yield "Eligibility changed. Preview the group again.";h.state.remove("InvitationPreviewAt");yield TycoonVisits.invite(p);}
            case "food"->{String[] parts=value.split(",");yield parts.length==2?TycoonFoodShop.buy(p,Integer.parseInt(parts[0]),Integer.parseInt(parts[1])):"Invalid food selection.";}
            case "supper"->TycoonFoodShop.furniture(p,value);
            case "sendhome"->TycoonVisits.sendHome(p);
            case "terrain"->{String[] parts=value.split(",");if(parts.length!=3)yield "Invalid terrain map selection.";yield TycoonLandscape.purchase(p,Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));}
            case "house"->TycoonHouses.purchase(p,Integer.parseInt(value));
            case "sky"->TycoonSkies.select(p,Integer.parseInt(value));
            case "attraction_preview"->{String[] parts=value.split(",");yield parts.length==4?TycoonAttractions.preview(p,parts[0],Integer.parseInt(parts[1]),Integer.parseInt(parts[2]),Integer.parseInt(parts[3])):"Invalid placement.";}
            case "attraction_confirm"->TycoonAttractions.confirm(p);
            case "attraction_upgrade"->{String[] parts=value.split(",");yield parts.length==2?TycoonAttractions.upgrade(p,UUID.fromString(parts[0]),Integer.parseInt(parts[1])):"Invalid upgrade.";}
            case "attraction_sale"->TycoonAttractions.sale(p,UUID.fromString(value),false);
            case "attraction_sell"->TycoonAttractions.sale(p,UUID.fromString(value),true);
            case "spy"->TycoonSpies.purchase(p,value);
            case "spy_upgrade"->TycoonSpies.upgrade(p,UUID.fromString(value));
            case "farm_preview"->{String[] parts=value.split(",");yield parts.length==3?TycoonSoulFarms.preview(p,Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2])):"Invalid farm placement.";}
            case "farm_confirm"->TycoonSoulFarms.confirm(p);
            case "farm_upgrade"->TycoonSoulFarms.upgrade(p,UUID.fromString(value),false);
            case "farm_repair"->TycoonSoulFarms.upgrade(p,UUID.fromString(value),true);
            case "farm_pause"->TycoonSoulFarms.pause(p,UUID.fromString(value));
            case "farm_secure"->TycoonSoulFarms.intervene(p,UUID.fromString(value),false);
            case "farm_concede_preview"->{var h=TycoonWorld.home(p);UUID id=UUID.fromString(value);h.state.putUUID("ConcedeFarm",id);h.state.putLong("ConcedeUntil",h.clock()+600);yield "Concede Breach and Evacuate permanently loses every soul in this farm. Confirm to proceed.";}
            case "farm_concede"->{var h=TycoonWorld.home(p);UUID id=UUID.fromString(value);if(!h.state.hasUUID("ConcedeFarm")||!id.equals(h.state.getUUID("ConcedeFarm"))||h.clock()>=h.state.getLong("ConcedeUntil"))yield "Preview this irreversible evacuation first.";h.state.remove("ConcedeFarm");yield TycoonSoulFarms.intervene(p,id,true);}
            case "soul_assign"->{String[] parts=value.split(",");yield parts.length==2?TycoonSouls.assign(p,UUID.fromString(parts[0]),UUID.fromString(parts[1])):"Invalid soul assignment.";}
            case "soul_unassign"->TycoonSouls.unassign(p,UUID.fromString(value));
            case "soul_release_preview"->TycoonSouls.release(p,UUID.fromString(value),false);
            case "soul_release"->TycoonSouls.release(p,UUID.fromString(value),true);
            case "soul_protect"->{var h=TycoonWorld.home(p);var soul=TycoonSouls.collection(h).getCompound(UUID.fromString(value).toString());if(soul.isEmpty())yield "Soul unavailable.";soul.putBoolean("Protected",!soul.getBoolean("Protected"));yield soul.getBoolean("Protected")?"Soul protected from automatic release.":"Automatic release protection removed.";}
            default->"Unknown Phase 2 selection.";
        };}catch(IllegalArgumentException invalid){return "Invalid or stale Phase 2 selection.";}
    }
    public static String talk(ServerPlayer p,UUID child,String value){
        try{
            String[] parts=value.split(":",2);if(parts.length!=2)return "Invalid response.";var r=TycoonWorld.home(p).residents.get(child);if(r==null||!r.active()||!TycoonVisits.inside(r))return "Visitor unavailable.";
            if(parts[0].equals("proposal"))return TycoonSouls.propose(p,r,parts[1].equals("confirm"));
            if(r.n.hasUUID("RescuingFarm"))return "They are trying to free the trapped souls. Secure the farm at its emergency control, or Concede Breach and Evacuate from the desk.";
            if(parts[0].equals("assign"))return TycoonAttractions.assign(p,r,UUID.fromString(parts[1]));
            if(parts[0].equals("returnhouse"))return TycoonAttractions.returnHouse(p,r);
            return TycoonDialogue.respond(p,r,UUID.fromString(parts[0]),Integer.parseInt(parts[1]));
        }catch(IllegalArgumentException invalid){return "Invalid response.";}
    }
    private TycoonPhaseTwoMenus(){}
}
