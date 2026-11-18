package com.coraline.feast;

import java.util.*;
import net.minecraft.nbt.*;

/** Persisted, deterministic day plans over the existing activity roster. The
 * physical director uses these IDs to build/reuse the corresponding real arena,
 * navigate the NPC participant and place only that day's manifestation props. */
public final class GhostVictimSchedule {
    public record Activity(String id,String title,String useful,String risk){}
    public static final List<Activity> ROSTER=List.of(
        new Activity("magic_garden","Magical Garden","flower_illusion","branch_movement"),
        new Activity("mice_circus","Mouse Circus","performer_strings","trick_interruption"),
        new Activity("other_village","Other Village","repeating_villager","moving_shop_object"),
        new Activity("magic_show","Magic Show","false_backdrop","threatening_prop"),
        new Activity("treasure_hunt","Treasure Hunt","secret_clue","frightening_decoy"),
        new Activity("fishing_contest","Fishing Contest","artificial_catch","sudden_water_noise"),
        new Activity("cake_making_contest","Cake Making Contest","hidden_ingredient","floating_knife"),
        new Activity("carnival","Carnival","broken_illusion","sudden_rattle"),
        new Activity("grand_feast","Grand Feast","dish_illusion","floating_knife"));
    public static final List<String> GIFTS=List.of("toy","music_box","cake","flower","magical_trinket","doll","gadget","personal_present");
    public static Activity activity(String id){return ROSTER.stream().filter(a->a.id().equals(id)).findFirst().orElse(null);}
    public static CompoundTag prepare(CompoundTag session,int day){
        if(!GhostVictimProgress.hasVictim(session)||day<1)throw new IllegalArgumentException("A valid current victim/day is required");
        var c=GhostVictimProgress.child(session);String key="Plan_"+day;
        var plans=session.getCompound("DayPlans");
        if(plans.contains(key,Tag.TAG_COMPOUND)){
            var existing=plans.getCompound(key);
            if(!existing.hasUUID("Victim")||!existing.getUUID("Victim").equals(c.getUUID("Id")))throw new IllegalStateException("Day plan belongs to another victim");
            return existing;
        }
        UUID id=c.getUUID("Id");var random=new Random(id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),17)^((long)day*0x9E3779B97F4A7C15L));
        var choices=new ArrayList<>(ROSTER);Collections.shuffle(choices,random);int count=2+random.nextInt(2);
        var result=new CompoundTag();result.putInt("Day",day);result.putUUID("Victim",id);
        var activities=new ListTag();var opportunities=new ListTag();
        for(int i=0;i<count;i++){
            var activity=choices.get(i);activities.add(StringTag.valueOf(activity.id()));
            opportunities.add(StringTag.valueOf(activity.id()+":"+activity.useful()));
            opportunities.add(StringTag.valueOf(activity.id()+":"+activity.risk()));
        }
        var mirrors=new ListTag();mirrors.add(StringTag.valueOf("bedroom"));
        mirrors.add(StringTag.valueOf(List.of("main_hall","living_room","upstairs_corridor").get(random.nextInt(3))));
        if(choices.subList(0,count).stream().anyMatch(a->a.id().equals("magic_garden")))mirrors.add(StringTag.valueOf("magic_garden:reflection"));
        result.put("Activities",activities);result.put("Opportunities",opportunities);result.put("Mirrors",mirrors);
        int gift=random.nextInt(GIFTS.size());String previous=plans.getCompound("Plan_"+(day-1)).getString("Gift");
        if(GIFTS.get(gift).equals(previous))gift=(gift+1)%GIFTS.size();result.putString("Gift",GIFTS.get(gift));
        result.putInt("HouseSecretVariant",random.nextInt(4));result.putInt("ActivityIndex",0);
        plans.put(key,result);session.put("DayPlans",plans);return plans.getCompound(key);
    }
    public static CompoundTag today(CompoundTag session){return prepare(session,Math.max(1,session.getInt("Day")));}
    public static CompoundTag tomorrow(CompoundTag session){return prepare(session,Math.max(1,session.getInt("Day"))+1);}
    public static boolean morning(CompoundTag session){
        if(GhostVictimProgress.phase(session)!=GhostVictimProgress.Phase.NIGHT_PREPARATION)return false;
        var plan=tomorrow(session);session.putInt("Day",plan.getInt("Day"));
        session.remove("PointCooldowns");session.remove("PointUses");session.remove("GiftUsed");
        GhostVictimProgress.leaveMirror(session);GhostVictimProgress.move(session,GhostVictimProgress.Phase.DAILY_ARRIVAL);return true;
    }
    public static String currentActivity(CompoundTag session){
        var plan=today(session);var list=plan.getList("Activities",Tag.TAG_STRING);int index=plan.getInt("ActivityIndex");
        return index>=0&&index<list.size()?list.getString(index):"";
    }
    public static boolean completeActivity(CompoundTag session,UUID participant,String id){
        if(!GhostVictimProgress.hasVictim(session)||participant==null||!participant.equals(GhostVictimProgress.child(session).getUUID("Id"))
                ||GhostVictimProgress.phase(session)!=GhostVictimProgress.Phase.ACTIVITY||!currentActivity(session).equals(id))return false;
        var plan=today(session);plan.putInt("ActivityIndex",plan.getInt("ActivityIndex")+1);
        GhostVictimProgress.move(session,GhostVictimProgress.Phase.BETWEEN_ACTIVITIES);return true;
    }
    /** A blocked departure is not an attended activity. Authenticate the same
     * participant and current itinerary before advancing without any rewards. */
    public static boolean skipObstructedDeparture(CompoundTag session,UUID participant,String id){
        if(!GhostVictimProgress.hasVictim(session)||participant==null||!participant.equals(GhostVictimProgress.child(session).getUUID("Id"))
                ||GhostVictimProgress.phase(session)!=GhostVictimProgress.Phase.BETWEEN_ACTIVITIES||GhostVictimProgress.age(session)<1200
                ||!currentActivity(session).equals(id))return false;
        var plan=today(session);plan.putInt("ActivityIndex",plan.getInt("ActivityIndex")+1);
        GhostVictimProgress.move(session,GhostVictimProgress.Phase.BETWEEN_ACTIVITIES);return true;
    }
    public static boolean opportunity(CompoundTag session,String id){
        var phase=GhostVictimProgress.phase(session);if(phase!=GhostVictimProgress.Phase.ACTIVITY&&phase!=GhostVictimProgress.Phase.NIGHT_PREPARATION)return false;
        var plan=phase==GhostVictimProgress.Phase.NIGHT_PREPARATION?tomorrow(session):today(session);
        for(Tag at:plan.getList("Opportunities",Tag.TAG_STRING))if(at.getAsString().equals(id))return phase==GhostVictimProgress.Phase.NIGHT_PREPARATION||id.startsWith(currentActivity(session)+":");
        return false;
    }
    /** The same event gate is used for HUD hints and server-side actions. Old
     * display entities can remain in a venue without becoming reusable secrets. */
    public static boolean manifestationAvailable(CompoundTag session,String point){
        if(point==null||!GhostVictimProgress.hasVictim(session))return false;
        var phase=GhostVictimProgress.phase(session);
        if(java.util.Set.of("house_hall_vase","house_hall_book","house_kitchen_cup","house_bedroom_toy",
                "house_bedroom_clock","supper_bottle","kitchen_knife","supper_cup").contains(point))return false;
        var escape=session.getCompound(GhostEscapeProgress.KEY);
        if(point.startsWith("room_light_")&&GhostEscapeProgress.owns(session,escape)
                &&GhostEscapeProgress.phase(escape)==GhostEscapeProgress.Phase.WAIT_LURE)return true;
        if(point.startsWith("supper_"))return phase==GhostVictimProgress.Phase.FIRST_SUPPER&&session.contains("SupperSeated");
        if(point.startsWith("gift_"))return phase==GhostVictimProgress.Phase.DAILY_GIFT&&session.getBoolean("GiftPlaced")
            &&point.equals("gift_"+today(session).getString("Gift"));
        if(point.startsWith("outing_")){
            if(phase!=GhostVictimProgress.Phase.ACTIVITY)return false;
            String current=currentActivity(session);
            return !current.isEmpty()&&(point.equals("outing_"+current+"_secret")||point.equals("outing_"+current+"_rattle"));
        }
        if(point.startsWith("room_light_")||point.startsWith("house_")||point.equals("kitchen_knife"))return phase==GhostVictimProgress.Phase.FIRST_SUPPER
            ||phase==GhostVictimProgress.Phase.EXPLORATION||phase==GhostVictimProgress.Phase.BEDTIME||phase==GhostVictimProgress.Phase.DAILY_GIFT;
        return false;
    }
    public static boolean previewAvailable(CompoundTag session,String point){
        if(point==null||!GhostVictimProgress.hasVictim(session)||GhostVictimProgress.phase(session)!=GhostVictimProgress.Phase.NIGHT_PREPARATION)return false;
        var plan=tomorrow(session);
        if(point.equals("gift_"+plan.getString("Gift")))return true;
        for(Tag entry:plan.getList("Activities",Tag.TAG_STRING)){
            String id=entry.getAsString();if(point.equals("outing_"+id+"_secret")||point.equals("outing_"+id+"_rattle"))return true;
        }
        return false;
    }
    private GhostVictimSchedule(){}
}
