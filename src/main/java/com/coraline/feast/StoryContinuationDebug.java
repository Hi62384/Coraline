package com.coraline.feast;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Operator controls retain genuine eligibility, ownership and recording completion gates. */
public final class StoryContinuationDebug {
    public static boolean section(String id){return id.equals("cat_story")||id.equals("parent_rescue")||id.equals("rescue_chapter");}
    public static Map<String,String> actions(String section){if(section.equals("rescue_chapter"))return RescueChapterDebug.actions();if(section.equals("cat_story"))return MissingParentsCatDebug.actions();var m=new LinkedHashMap<String,String>();m.put("state","Inspect phase, actual recording and saved blockers");m.put("recover","Replay only the current recording checkpoint");if(section.equals("cat_story")){m.put("start","Start eligible Cat encounter without waiting");m.put("notice","Look toward the actual Cat");m.put("mirror","Go near the actual mirror");m.put("break","Interact with mirror at the valid break prompt");m.put("bed","Go near the canonical family bed");}else{m.put("key","Claim the canonical return key (after Cat visit)");m.put("enter","Enter through the restored Little Door");m.put("seat","Interact with the actual nearby kitchen chair");}return m;}
    public static String run(ServerPlayer p,VisitData.Scene s,String section,String action){
        if(!NumberedDebugActions.operator(p,s))return "Owner/operator authorization required.";if(section.equals("rescue_chapter"))return RescueChapterDebug.run(p,s,action);if(section.equals("cat_story"))return MissingParentsCatDebug.run(p,s,action);var n=section.equals("cat_story")?MissingParentsCat.data(s):ParentRescueStory.data(s);
        if(action.equals("state"))return "Current state refreshed.";
        if(action.equals("recover")){if(!n.getBoolean("Started"))return "This scene has not started.";n.putBoolean("Recover",true);OtherMotherLoop.changed(p);return "Current checkpoint will restart; no recording was skipped.";}
        if(section.equals("cat_story")){
            if(!MissingParentsCat.eligible(s))return "Complete the second tea visit and resolve the Seeing Stone delivery first.";
            if(action.equals("start")){if(n.getBoolean("Complete"))return "The Cat visit is already complete.";n.putInt("Delay",0);MissingParentsCat.tick(p,s);}
            else if(action.equals("notice")){var cat=ShowActors.actor(p,s,"cat");if(cat!=null&&cat.level()==p.level())p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,cat.getEyePosition());}
            else if(action.equals("break")&&n.contains("Mirror"))MissingParentsCat.block(p,BlockPos.of(n.getLong("Mirror")));
            else if(action.equals("mirror")&&n.contains("MirrorApproach"))near(p,BlockPos.of(n.getLong("MirrorApproach")));
            else if(action.equals("bed")&&n.contains("Bed"))near(p,BlockPos.of(n.getLong("Bed")).south(2));
        }else{
            if(action.equals("key")){if(!MissingParentsCat.data(s).getBoolean("Complete"))return "Finish the Cat visit before obtaining the return key.";return ParentRescueStory.grantKey(p,s)?"Canonical key available; no duplicate issued.":"Key unavailable: free an inventory slot or retrieve the previously issued key.";}
            if(action.equals("enter")){var d=com.coraline.otherworld.DoorData.get(p.server).doors.get(s.door);if(d!=null&&p.serverLevel().getBlockEntity(d.pos) instanceof com.coraline.otherworld.DoorBlockEntity door&&p.distanceToSqr(d.pos.getCenter())<16)StoryTunnel.enter(p,door);else return "Approach your actual Little Door in the Overworld.";}
            if(action.equals("seat"))for(var at:BlockPos.betweenClosed(p.blockPosition().offset(-3,-1,-3),p.blockPosition().offset(3,1,3)))if(ParentRescueStory.block(p,at))break;
        }
        OtherMotherLoop.changed(p);return "Guarded scene control checked. See current phase and blocker.";
    }
    private static void near(ServerPlayer p,BlockPos at){var safe=com.coraline.otherworld.OtherWorld.safePlayer(p.serverLevel(),at);if(safe!=null)p.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);}
    public static List<String> inspect(VisitData.Scene s,String section){if(section.equals("rescue_chapter"))return RescueChapterDebug.inspect(s);if(section.equals("cat_story"))return MissingParentsCatDebug.inspect(s);var n=section.equals("cat_story")?MissingParentsCat.data(s):ParentRescueStory.data(s);var sound=ShowPlayback.data(s);return List.of("Scene "+section+" | phase "+n.getString("Phase"),"Started "+n.getBoolean("Started")+" | complete "+n.getBoolean("Complete"),"Recording "+sound.getString("Sound")+" | "+ShowPlayback.position(s)+" / "+sound.getInt("Duration"),"Playback acknowledged "+sound.getBoolean("Started")+" | ended "+sound.getBoolean("Ended"),"Mirror broken "+n.getBoolean("MirrorBroken")+" | dolls retrieved "+n.getBoolean("DollsRetrieved"),"Tunnel permanent "+MissingParentsCat.tunnelRestored(s)+" | key holder "+s.loop.story.getString("LittleDoorKeyHolder"),"Key taken "+n.getBoolean("KeyTaken")+" | swallowed "+n.getBoolean("KeySwallowed")+" | wardrobe blocking "+n.getBoolean("WardrobeBlocking"),"Seated "+n.getBoolean("Seated")+" | meal placed "+n.getBoolean("MealPlaced")+" | mother absent "+n.getBoolean("MotherAbsent"),"Blocker "+n.getString("Blocker"));}
    private StoryContinuationDebug(){}
}
