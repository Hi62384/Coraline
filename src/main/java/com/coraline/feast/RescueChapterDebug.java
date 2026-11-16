package com.coraline.feast;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;

/** Operator diagnostics never manufacture eyes, items, audio acknowledgements or chapter eligibility. */
final class RescueChapterDebug {
    static Map<String,String> actions(){var result=new LinkedHashMap<String,String>();result.put("state","Inspect canonical actors, recordings, custody and physical gates");result.put("begin","Start only after the completed kitchen disappearance");result.put("recover","Restart only the current safe checkpoint");result.put("retry","Retry the current fight, chase or physical challenge");return result;}
    static String run(ServerPlayer p,VisitData.Scene s,String action){
        if(!NumberedDebugActions.operator(p,s))return "Owner/operator authorization required.";
        if(action.equals("state"))return "Actual rescue state refreshed.";
        if(action.equals("begin"))return RescueChapter.started(s)?"Rescue chapter already started.":RescueChapter.begin(p,s)?"Eligible rescue started at the garden search.":"Complete the real kitchen N recording, disappearance and chair release first.";
        if(!RescueChapter.owns(p,s)||RescueChapter.finished(s))return "No active owner rescue challenge.";
        if(action.equals("recover")){RescueChapter.data(s).putBoolean("Recover",true);OtherMotherLoop.changed(p);return "Current checkpoint will restart with its full recording; progress was not granted.";}
        if(action.equals("retry")){var phase=RescueChapter.phase(s);if(RescueBosses.fighting(phase)||phase==RescueChapter.Phase.RAT_CHASE||phase==RescueChapter.Phase.COLLAPSE||phase==RescueChapter.Phase.CAT_THROW||phase==RescueChapter.Phase.WEB_STEALTH||phase==RescueChapter.Phase.DOOR_STRUGGLE||phase==RescueChapter.Phase.TUNNEL_CHASE){RescueChapter.recover(p,s);return "Current challenge reset through its normal retry path; no eye or key was granted.";}return "This phase has no challenge retry; use recording recovery if needed.";}
        return "Unknown rescue control; no changes made.";
    }
    static List<String> inspect(VisitData.Scene s){
        var n=RescueChapter.data(s);var audio=ShowPlayback.data(s);var lines=new ArrayList<String>();
        lines.add("Rescue phase "+RescueChapter.phase(s)+" | started "+RescueChapter.started(s)+" | complete "+RescueChapter.finished(s));
        lines.add("Recording "+audio.getString("Sound")+" | "+audio.getInt("Position")+" / "+audio.getInt("Duration")+" | start ACK "+audio.getBoolean("Started")+" | end ACK "+audio.getBoolean("Ended")+" | physical tail "+n.getInt("AudioTail"));
        for(int i=0;i<3;i++){var eye=RescueEyes.eye(s,i);lines.add("Eye "+i+" | discovered "+eye.getBoolean("Discovered")+" | defeated "+eye.getBoolean("Defeated")+" | claimed "+eye.getBoolean("Claimed"));}
        lines.add("Boss HP "+n.getFloat("BossHealth")+" / "+n.getFloat("BossMaximum")+" | pattern "+n.getInt("BossPattern")+" | weakness "+n.getBoolean("Vulnerable"));
        lines.add("Canonical Cat "+s.loop.story.getCompound("CanonicalStoryCat").getString("Dimension")+" | rescued "+n.getBoolean("CatRescued")+" | mantle "+n.getBoolean("CatMantel"));
        for(String key:List.of("Key","Stone")){var receipt=ParentRescueStory.data(s).getCompound("Custody").getCompound(key);lines.add(key+" custody "+receipt.contains("Stack")+" | returned "+receipt.getBoolean("Returned")+" | destroyed "+receipt.getBoolean("Destroyed"));}
        lines.add("Web ready "+n.getBoolean("WebReady")+" | cells "+n.getCompound("WebCells").size()+" | tunnel ready "+n.getBoolean("PassageReady")+" | cells "+n.getCompound("PassageCells").size());
        lines.add("Cat blinded Beldam "+n.getBoolean("BeldamBlind")+" | original key recovered "+n.getBoolean("KeyRecovered")+" | actual crawl exit "+n.getBoolean("CrawlReachedOverworld")+" | permanent seal "+n.getBoolean("PermanentlySealed"));
        lines.add("Blocker "+n.getString("Blocker"));return List.copyOf(lines);
    }
    private RescueChapterDebug(){}
}
