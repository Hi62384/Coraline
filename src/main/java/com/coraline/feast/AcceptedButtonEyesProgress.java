package com.coraline.feast;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Durable branch journal in the existing owner's story ledger. */
public final class AcceptedButtonEyesProgress {
    public static final String KEY="AcceptedButtonEyes";
    public enum Route { ACCEPTED_PROPOSAL, FINAL_BOSS_LOSS }
    public enum Phase {
        BASEMENT_INVITATION,WAITING_TO_SIT,APPROACH_CHAIR,TAKE_BUTTONS,TAKE_NEEDLE,RAISE_NEEDLE,
        BLACKOUT,PROCEDURE_COMPLETE,MEET_UPSTAIRS,ACTIVITY_INVITATION,ACTIVITY_RUNNING,ACTIVITY_REACTION,
        WORLD_COLLAPSED,BELDAM_REVEAL,MIRROR_IMPRISONMENT,GHOST_C,GHOST_D,GHOST_E,
        ACCEPTED_BUTTON_EYES_MIRROR_PRISON
    }
    public static CompoundTag data(VisitData.Scene scene){
        if(!scene.loop.story.contains(KEY,Tag.TAG_COMPOUND))scene.loop.story.put(KEY,new CompoundTag());
        return scene.loop.story.getCompound(KEY);
    }
    public static boolean active(VisitData.Scene scene){return scene!=null&&scene.loop.story.getCompound(KEY).getBoolean("Started");}
    public static CompoundTag scenery(VisitData.Scene scene){var n=data(scene);if(!n.contains("Scenery",Tag.TAG_COMPOUND))n.put("Scenery",new CompoundTag());return n.getCompound("Scenery");}
    public static Phase phase(CompoundTag n){
        try{return Phase.valueOf(n.getString("Phase"));}catch(IllegalArgumentException invalid){return Phase.BASEMENT_INVITATION;}
    }
    public static boolean terminal(CompoundTag n){return phase(n)==Phase.ACCEPTED_BUTTON_EYES_MIRROR_PRISON;}
    public static boolean begin(CompoundTag n,Route route,UUID door,int required,long now){
        if(n.getBoolean("Started"))return false;
        n.putInt("Schema",1);n.putBoolean("Started",true);n.putString("Route",route.name());n.putUUID("Door",door);
        n.putInt("RequiredActivities",Math.max(1,Math.min(20,required)));n.put("CompletedInstances",new ListTag());
        move(n,Phase.BASEMENT_INVITATION,now);return true;
    }
    public static void move(CompoundTag n,Phase phase,long now){
        if(terminal(n)&&phase!=Phase.ACCEPTED_BUTTON_EYES_MIRROR_PRISON)return;
        n.putString("Phase",phase.name());n.putLong("PhaseStarted",now);n.remove("VoiceStarted");
    }
    public static int completed(CompoundTag n){return n.getList("CompletedInstances",Tag.TAG_STRING).size();}
    public static int stage(CompoundTag n){return Math.max(0,Math.min(5,completed(n)*5/Math.max(1,n.getInt("RequiredActivities"))));}
    public static boolean openActivity(CompoundTag n,String activity,UUID instance,long now){
        if(phase(n)!=Phase.ACTIVITY_INVITATION||completed(n)>=Math.max(1,n.getInt("RequiredActivities")))return false;
        n.putString("Activity",activity);n.putUUID("ActivityInstance",instance);
        n.remove("GameCompleted");n.remove("NormalReturn");move(n,Phase.ACTIVITY_RUNNING,now);return true;
    }
    public static boolean gameCompleted(CompoundTag n,UUID instance){
        if(phase(n)!=Phase.ACTIVITY_RUNNING||!matches(n,instance))return false;
        n.putBoolean("GameCompleted",true);return true;
    }
    public static boolean normalReturn(CompoundTag n,UUID instance,long now){
        if(phase(n)!=Phase.ACTIVITY_RUNNING||!matches(n,instance)||!n.getBoolean("GameCompleted"))return false;
        n.putBoolean("NormalReturn",true);move(n,Phase.ACTIVITY_REACTION,now);return true;
    }
    /** Only the normal completion reaction commits the unique returned instance. */
    public static boolean commitReaction(CompoundTag n,long now){
        if(phase(n)!=Phase.ACTIVITY_REACTION||!n.getBoolean("GameCompleted")||!n.getBoolean("NormalReturn")||!n.hasUUID("ActivityInstance"))return false;
        String id=n.getUUID("ActivityInstance").toString();ListTag completed=n.getList("CompletedInstances",Tag.TAG_STRING);
        for(Tag prior:completed)if(prior.getAsString().equals(id))return false;
        completed.add(StringTag.valueOf(id));
        if(n.getBoolean("DebugCollapseAfterNext")){n.remove("DebugCollapseAfterNext");n.putInt("RequiredActivities",completed.size());}
        n.put("CompletedInstances",completed);
        n.remove("ActivityInstance");n.remove("GameCompleted");n.remove("NormalReturn");
        move(n,completed.size()>=Math.max(1,n.getInt("RequiredActivities"))?Phase.WORLD_COLLAPSED:Phase.ACTIVITY_INVITATION,now);
        return true;
    }
    public static void abortActivity(CompoundTag n,long now){
        if(phase(n)!=Phase.ACTIVITY_RUNNING&&phase(n)!=Phase.ACTIVITY_REACTION)return;
        n.remove("ActivityInstance");n.remove("GameCompleted");n.remove("NormalReturn");move(n,Phase.ACTIVITY_INVITATION,now);
    }
    private static boolean matches(CompoundTag n,UUID instance){return n.hasUUID("ActivityInstance")&&n.getUUID("ActivityInstance").equals(instance);}
    private AcceptedButtonEyesProgress(){}
}
