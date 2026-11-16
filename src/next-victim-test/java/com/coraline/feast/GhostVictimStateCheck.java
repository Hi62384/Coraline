package com.coraline.feast;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

/** Small actual-NBT runtime checks, independent of a Minecraft server. Native
 * entity/navigation/client checks remain separate and must still be run. */
public final class GhostVictimStateCheck {
    private static int checks;
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    private static CompoundTag session(){var n=new CompoundTag();GhostVictimProgress.begin(n,"ACCEPTED_PROPOSAL");return n;}
    public static void main(String[] args){
        var n=session();var tuning=GhostVictimProgress.Tuning.defaults();
        for(int i=0;i<1200;i++)GhostVictimProgress.tick(n,tuning);
        require(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.ENTER_MIRROR,"Prison exit was automatic");
        require(GhostVictimProgress.leavePrison(n),"Manual mirror exit failed");
        var identity=new CompoundTag();identity.putString("Name","Robin Ellis");UUID id=UUID.randomUUID();
        require(GhostVictimProgress.newVictim(n,id,identity),"Identity did not start");
        require(!GhostVictimProgress.newVictim(n,UUID.randomUUID(),identity),"Active victim replaced");
        GhostVictimProgress.trust(n,.65,1);require(GhostVictimProgress.ghostTrust(n)==.99,"High Mother trust did not cap alliance");
        GhostVictimProgress.trust(n,.5,.6);var before=GhostVictimProgress.phase(n);n.putDouble("Attention",1);
        require(GhostVictimProgress.tick(n,tuning)&&GhostVictimProgress.imprisoned(n),"Attention did not impose punishment");
        for(int i=0;i<1200;i++)GhostVictimProgress.tick(n,tuning);
        require(!GhostVictimProgress.imprisoned(n)&&n.getInt("Punishments")==1,"Punishment repeated or never expired");
        require(GhostVictimProgress.phase(n)==before,"Punishment changed the child's schedule");
        n.putDouble("Energy",1);require(GhostVictimProgress.enterMirror(n,"bedroom",tuning),"Mirror refused energy");
        UUID nonce=n.getUUID("Conversation");require(!GhostVictimProgress.dialogue(n,UUID.randomUUID(),GhostVictimProgress.Dialogue.HELP,true),"Forged choice accepted");
        require(!GhostVictimProgress.dialogue(n,nonce,GhostVictimProgress.Dialogue.HELP,false),"Absent child heard ghost");
        for(int i=0;i<250;i++)GhostVictimProgress.tick(n,tuning);
        require(!GhostVictimProgress.mirror(n)&&!n.hasUUID("Conversation"),"Mirror remained active at zero energy");
        n.putDouble("Energy",1);double mother=GhostVictimProgress.motherTrust(n);
        GhostVictimProgress.disturb(n,"food",GhostVictimProgress.Disturbance.FOOD,new GhostVictimProgress.Observation(false,false,false,false,false),true);
        require(GhostVictimProgress.motherTrust(n)==mother,"Unseen event changed belief");
        n.putLong("ActiveTicks",GhostVictimProgress.clock(n)+601);n.putDouble("Energy",1);
        GhostVictimProgress.disturb(n,"food",GhostVictimProgress.Disturbance.FOOD,new GhostVictimProgress.Observation(true,false,false,false,false),true);
        mother=GhostVictimProgress.motherTrust(n);n.putInt("Day",2);n.putDouble("Energy",1);
        GhostVictimProgress.disturb(n,"food",GhostVictimProgress.Disturbance.FOOD,new GhostVictimProgress.Observation(true,false,false,false,false),true);
        require(GhostVictimProgress.motherTrust(n)==mother,"Same evidence farmed on another day");
        var plan=GhostVictimSchedule.today(n).copy();require(plan.equals(GhostVictimSchedule.today(n)),"Daily plan rerolled");
        GhostVictimProgress.move(n,GhostVictimProgress.Phase.NIGHT_PREPARATION);
        require(GhostVictimSchedule.morning(n)&&!GhostVictimSchedule.morning(n),"Morning transition duplicated");
        require(GhostVictimProgress.child(n).getUUID("Id").equals(id),"Morning replaced identity");
        GhostVictimProgress.trust(n,.4,1);GhostVictimProgress.tick(n,tuning);
        require(!n.getBoolean("EscapeStarted")&&!n.getBoolean("SoulRestored"),"Trust restored a soul before later escape");
        var oldRoute=new CompoundTag();oldRoute.putLong("Started",1);
        for(String key:new String[]{"MotherArrivalRoute","MotherSupperSeatRoute","FatherSupperSeatRoute","MotherRoute","FatherRoute"})n.put(key,oldRoute.copy());
        n.putLong("ReactionLook",17);n.putLong("ReactionLookUntil",999999);n.put("MotherOuting",oldRoute.copy());
        require(GhostVictimProgress.archiveVictim(n,"CAPTURED")&&!GhostVictimProgress.archiveVictim(n,"CAPTURED"),"Outcome archived twice");
        require(!n.contains("MotherArrivalRoute")&&!n.contains("MotherSupperSeatRoute")&&!n.contains("FatherSupperSeatRoute")&&!n.contains("MotherRoute")&&!n.contains("FatherRoute")&&!n.contains("ReactionLook")&&!n.contains("ReactionLookUntil"),"New victim inherited expired parent routes or reaction targets");
        require(n.getCompound("MotherOuting").equals(oldRoute),"Archiving discarded a parent's pending return travel record");
        n.put("MotherArrivalRoute",oldRoute.copy());n.putLong("ReactionLookUntil",999999); // Older save archived before route cleanup existed.
        require(GhostVictimProgress.newVictim(n,UUID.randomUUID(),identity)&&GhostVictimProgress.motherTrust(n)==.5&&GhostVictimProgress.ghostTrust(n)==0,"Fresh victim inherited old relationships");
        require(!n.contains("MotherArrivalRoute")&&!n.contains("ReactionLookUntil")&&n.getCompound("MotherOuting").equals(oldRoute),"Older prepared save retained stale routine or lost return travel");
        var departure=n.copy();var participant=GhostVictimProgress.child(departure).getUUID("Id");
        GhostVictimProgress.move(departure,GhostVictimProgress.Phase.BETWEEN_ACTIVITIES);
        String outing=GhostVictimSchedule.currentActivity(departure);int index=GhostVictimSchedule.today(departure).getInt("ActivityIndex");
        double trust=GhostVictimProgress.motherTrust(departure);
        require(!GhostVictimSchedule.skipObstructedDeparture(departure,participant,outing),"Departure skipped before its recovery window");
        departure.putLong("ActiveTicks",GhostVictimProgress.clock(departure)+1200);
        require(!GhostVictimSchedule.skipObstructedDeparture(departure,UUID.randomUUID(),outing),"Foreign child skipped an outing");
        require(!GhostVictimSchedule.skipObstructedDeparture(departure,participant,"wrong_outing"),"Wrong outing skipped");
        require(GhostVictimSchedule.skipObstructedDeparture(departure,participant,outing),"Obstructed departure remained stuck");
        require(GhostVictimSchedule.today(departure).getInt("ActivityIndex")==index+1,"Departure did not advance exactly one outing");
        require(GhostVictimProgress.motherTrust(departure)==trust,"Skipped outing granted trust");
        require(!GhostVictimSchedule.skipObstructedDeparture(departure,participant,outing),"Repeated recovery skipped another outing");
        require(!GhostVictimSchedule.skipObstructedDeparture(departure,participant,GhostVictimSchedule.currentActivity(departure)),"Next outing inherited an expired recovery timer");
        var gates=n.copy();GhostVictimProgress.move(gates,GhostVictimProgress.Phase.FIRST_SUPPER);
        require(!GhostVictimSchedule.manifestationAvailable(gates,"supper_food"),"Meal manifested before seating");
        gates.putLong("SupperSeated",GhostVictimProgress.clock(gates));
        require(GhostVictimSchedule.manifestationAvailable(gates,"supper_knife"),"Seated supper knife unavailable");
        GhostVictimProgress.move(gates,GhostVictimProgress.Phase.EXPLORATION);
        require(!GhostVictimSchedule.manifestationAvailable(gates,"supper_food"),"Old supper food remained usable");
        GhostVictimProgress.move(gates,GhostVictimProgress.Phase.DAILY_GIFT);gates.putBoolean("GiftPlaced",true);
        require(GhostVictimSchedule.manifestationAvailable(gates,"gift_"+GhostVictimSchedule.today(gates).getString("Gift")),"Current gift unavailable");
        require(!GhostVictimSchedule.manifestationAvailable(gates,"gift_not_todays_gift"),"Old gift remained usable");
        GhostVictimProgress.move(gates,GhostVictimProgress.Phase.ACTIVITY);String current=GhostVictimSchedule.currentActivity(gates);
        require(GhostVictimSchedule.manifestationAvailable(gates,"outing_"+current+"_secret"),"Current outing secret unavailable");
        require(!GhostVictimSchedule.manifestationAvailable(gates,"outing_wrong_event_secret"),"Unscheduled outing secret remained usable");
        GhostVictimProgress.move(gates,GhostVictimProgress.Phase.BETWEEN_ACTIVITIES);
        require(!GhostVictimSchedule.manifestationAvailable(gates,"outing_"+current+"_secret"),"Finished outing secret remained usable");
        GhostVictimProgress.move(gates,GhostVictimProgress.Phase.NIGHT_PREPARATION);
        String tomorrow=GhostVictimSchedule.tomorrow(gates).getList("Activities",net.minecraft.nbt.Tag.TAG_STRING).getString(0);
        double energy=GhostVictimProgress.energy(gates),belief=GhostVictimProgress.motherTrust(gates);
        require(GhostVictimSchedule.previewAvailable(gates,"outing_"+tomorrow+"_secret"),"Tomorrow's scheduled secret cannot be scouted");
        require(!GhostVictimSchedule.manifestationAvailable(gates,"outing_"+tomorrow+"_secret"),"Night scouting activated a future event");
        require(!GhostVictimSchedule.previewAvailable(gates,"outing_not_scheduled_secret"),"Unscheduled event can be scouted");
        require(GhostVictimProgress.energy(gates)==energy&&GhostVictimProgress.motherTrust(gates)==belief,"Scouting eligibility consumed energy or changed trust");
        String futureGift="gift_"+GhostVictimSchedule.tomorrow(gates).getString("Gift");
        require(GhostVictimSchedule.previewAvailable(gates,futureGift),"Tomorrow's gift cannot be inspected");
        require(!GhostVictimSchedule.manifestationAvailable(gates,futureGift),"Inspecting tomorrow's gift can activate it early");
        GhostVictimMirrors.register(gates,"bedroom",new net.minecraft.core.BlockPos(2,3,4),0,"test:room");
        GhostVictimMirrors.register(gates,"unplanned",new net.minecraft.core.BlockPos(5,3,4),0,"test:room");
        require(GhostVictimMirrors.previewAvailable(gates,"bedroom","test:room"),"Tomorrow's bedroom mirror cannot be scouted");
        require(!GhostVictimMirrors.previewAvailable(gates,"bedroom","test:other_room"),"Mirror preview crosses dimensions");
        require(!GhostVictimMirrors.previewAvailable(gates,"unplanned","test:room"),"Unscheduled mirror preview is active");
        GhostVictimProgress.move(gates,GhostVictimProgress.Phase.DAILY_ARRIVAL);
        require(!GhostVictimMirrors.previewAvailable(gates,"bedroom","test:room"),"Night mirror preview remains active after morning");
        System.out.println("GHOST_VICTIM_NBT_STATE_OK checks="+checks+"; no game/world/visual acceptance");
    }
    private GhostVictimStateCheck(){}
}
