package com.coraline.feast;

import java.util.*;
import net.minecraft.nbt.*;

/** Durable escape journal. Only the physical server director may submit these
 * observations: timers never invent path arrival, perception, key theft or wins.
 * This component is not a second inventory escrow or an active scene by itself. */
public final class GhostEscapeProgress {
    public static final String KEY="GhostEscape";
    public enum Phase { ALLIED_BEDTIME, HOME_ERRAND, STONE_REPORT, READY,
        APPROACH_ROOM, WAIT_GO, LOCKPICK, SCAN, LEAVE_ROOM, ROOM_REPORT,
        APPROACH_AMBUSH, WAIT_LURE, STEAL_KEY, KEY_REPORT, COLLECT_EYES,
        APPROACH_EXIT, INTERCEPT, WAGER, HIDDEN, MOVING_HIDE, FINAL_CROSSING,
        OPEN_DOOR, CRAWL, SAFE, RESTORING, FAREWELL, COMPLETE, FAILED_SEWING }
    public enum Discovery { NONE, LOCKPICKING, MINOR_TRESPASS, STEALING,
        EXPOSED_SEEING_STONE, EYE_ROOM, PHYSICAL_CAPTURE }
    public static CompoundTag data(CompoundTag ghost){if(!ghost.contains(KEY))ghost.put(KEY,new CompoundTag());return ghost.getCompound(KEY);}
    public static boolean active(CompoundTag n){return n.hasUUID("Attempt")&&phase(n)!=Phase.COMPLETE&&phase(n)!=Phase.FAILED_SEWING;}
    public static Phase phase(CompoundTag n){try{return Phase.valueOf(n.getString("Phase"));}catch(IllegalArgumentException invalid){return Phase.ALLIED_BEDTIME;}}
    public static boolean hunting(CompoundTag n){return Set.of(Phase.HIDDEN,Phase.MOVING_HIDE,Phase.FINAL_CROSSING,Phase.OPEN_DOOR,Phase.CRAWL).contains(phase(n));}
    public static void move(CompoundTag n,Phase phase,long clock){n.putString("Phase",phase.name());n.putLong("PhaseAt",clock);n.remove("OpportunityAt");n.remove("PickStarted");}
    public static long age(CompoundTag n,long clock){return Math.max(0,clock-n.getLong("PhaseAt"));}
    public static boolean begin(CompoundTag ghost,List<String> validRooms,Set<String> unresolvedGhosts,boolean actualBedtimeMirror){
        if(!actualBedtimeMirror||GhostVictimProgress.phase(ghost)!=GhostVictimProgress.Phase.BEDTIME
            ||!GhostVictimProgress.hasVictim(ghost)||!GhostVictimProgress.readyToHelp(ghost)
            ||!GhostVictimProgress.mirror(ghost)||validRooms==null||validRooms.isEmpty()||unresolvedGhosts!=null&&(unresolvedGhosts.size()>3||unresolvedGhosts.stream().anyMatch(Objects::isNull)))return false;
        var old=ghost.getCompound(KEY);if(old.hasUUID("Attempt"))return false;
        var rooms=new TreeSet<String>();for(String room:validRooms)if(room!=null&&room.matches("[a-z_]{1,40}"))rooms.add(room);
        if(rooms.isEmpty())return false;var n=new CompoundTag();UUID victim=GhostVictimProgress.child(ghost).getUUID("Id"),attempt=UUID.randomUUID();
        n.putUUID("Attempt",attempt);n.putUUID("Victim",victim);n.putUUID("Session",ghost.getUUID("Session"));
        var candidates=new ArrayList<>(rooms);n.putString("EyeRoom",candidates.get(Math.floorMod(attempt.hashCode(),candidates.size())));
        var savedRooms=new ListTag();for(String room:candidates)savedRooms.add(StringTag.valueOf(room));n.put("Rooms",savedRooms);
        var eyes=new ListTag();if(unresolvedGhosts!=null)for(String id:new TreeSet<>(unresolvedGhosts))eyes.add(StringTag.valueOf(id));
        n.put("UnresolvedGhosts",eyes);n.putDouble("Suspicion",0);move(n,Phase.ALLIED_BEDTIME,GhostVictimProgress.clock(ghost));ghost.put(KEY,n);return true;
    }
    public static boolean owns(CompoundTag ghost,CompoundTag n){return n.hasUUID("Attempt")&&n.hasUUID("Victim")&&n.hasUUID("Session")&&ghost.hasUUID("Session")
        &&n.getUUID("Session").equals(ghost.getUUID("Session"))&&GhostVictimProgress.hasVictim(ghost)&&n.getUUID("Victim").equals(GhostVictimProgress.child(ghost).getUUID("Id"));}
    public static double suspicion(CompoundTag n){return GhostVictimProgress.unit(n.getDouble("Suspicion"));}
    public static void suspicion(CompoundTag n,double delta){if(Double.isFinite(delta))n.putDouble("Suspicion",GhostVictimProgress.unit(suspicion(n)+delta));}
    public static void ordinarySecond(CompoundTag n){if(phase(n)==Phase.READY)suspicion(n,-.0008);}
    public static boolean bedtimeAgreement(CompoundTag ghost,boolean actualMirrorConversation){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.ALLIED_BEDTIME||!actualMirrorConversation||!GhostVictimProgress.mirror(ghost))return false;
        move(n,Phase.HOME_ERRAND,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean homeErrand(CompoundTag ghost,boolean actuallyAtOverworldHome){
        var n=data(ghost);if(!owns(ghost,n)||!actuallyAtOverworldHome||!ghost.getBoolean("ChildAtHome")||phase(n)!=Phase.HOME_ERRAND)return false;
        // The runtime must finish the offscreen Spink/Forcible visit before
        // submitting this fact, using the authentic existing Seeing Stone item.
        n.putBoolean("StoneObtained",true);move(n,Phase.STONE_REPORT,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean reportStone(CompoundTag ghost,boolean actualMirrorConversation){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.STONE_REPORT||!n.getBoolean("StoneObtained")||!actualMirrorConversation||!GhostVictimProgress.mirror(ghost))return false;
        move(n,Phase.READY,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean assignAmbush(CompoundTag ghost,String authoredSpot,boolean actualMirrorConversation){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.ROOM_REPORT||!n.getBoolean("EyeRoomKnown")||n.getBoolean("KeyStolen")
            ||!actualMirrorConversation||!GhostVictimProgress.mirror(ghost)||authoredSpot==null||!authoredSpot.matches("[a-z_0-9]{1,40}"))return false;
        n.putString("TargetHide",authoredSpot);move(n,Phase.APPROACH_AMBUSH,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean assignedRoom(CompoundTag ghost,String room,boolean actualMirrorConversation,boolean retrieve){
        var n=data(ghost);if(!owns(ghost,n)||!n.getBoolean("StoneObtained")||!actualMirrorConversation||!GhostVictimProgress.mirror(ghost)
            ||!(phase(n)==Phase.READY||phase(n)==Phase.ROOM_REPORT||phase(n)==Phase.KEY_REPORT))return false;
        boolean valid=false;for(Tag candidate:n.getList("Rooms",Tag.TAG_STRING))valid|=candidate.getAsString().equals(room);
        if(!valid||retrieve&&(!n.getBoolean("KeyStolen")||!n.getBoolean("EyeRoomKnown")||!n.getString("EyeRoom").equals(room)))return false;
        n.remove("ReportSpoken");n.remove("Blocker");n.putString("AssignedRoom",room);n.putBoolean("Retrieving",retrieve);move(n,Phase.APPROACH_ROOM,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean roomWaiting(CompoundTag ghost,boolean actualSafeArrival){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.APPROACH_ROOM||!actualSafeArrival)return false;
        move(n,Phase.WAIT_GO,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean signal(CompoundTag ghost,boolean go,boolean nearbyPhysicalPoint,boolean motherWitnessed){
        var n=data(ghost);if(!owns(ghost,n)||!active(n)||!nearbyPhysicalPoint||GhostVictimProgress.imprisoned(ghost)||GhostVictimProgress.suppressed(ghost)||GhostVictimProgress.mirror(ghost))return false;
        var phase=phase(n);boolean allowed=go?Set.of(Phase.WAIT_GO,Phase.HIDDEN).contains(phase):stopSignalAvailable(phase);
        String target=n.getString("TargetHide");boolean realTarget=target.equals("final_door")||GhostEscapeHiding.node(n,target).contains("Position");
        if(!allowed||go&&phase==Phase.HIDDEN&&!realTarget||!GhostVictimProgress.spend(ghost,.08))return false;
        if(motherWitnessed)ghost.putDouble("Attention",GhostVictimProgress.unit(GhostVictimProgress.attention(ghost)+.08));
        if(go)move(n,phase==Phase.WAIT_GO?Phase.LOCKPICK:n.getString("TargetHide").equals("final_door")?Phase.FINAL_CROSSING:Phase.MOVING_HIDE,GhostVictimProgress.clock(ghost));
        else if(phase==Phase.MOVING_HIDE)n.putBoolean("StopRequested",true); // Physical runtime finds a nearby real hiding spot; no teleport.
        else{n.putBoolean("ConcealStone",true);move(n,phase==Phase.WAIT_GO||phase==Phase.WAIT_LURE||phase==Phase.STEAL_KEY?Phase.READY:Phase.LEAVE_ROOM,GhostVictimProgress.clock(ghost));}
        return true;
    }
    static boolean stopSignalAvailable(Phase phase){
        return Set.of(Phase.WAIT_GO,Phase.LOCKPICK,Phase.SCAN,Phase.COLLECT_EYES,Phase.WAIT_LURE,Phase.STEAL_KEY,Phase.MOVING_HIDE).contains(phase);
    }
    static String objective(CompoundTag n){return switch(phase(n)){
        case ALLIED_BEDTIME->"Speak to the child through the mirror.";
        case HOME_ERRAND->"The child is going home for a Seeing Stone.";
        case STONE_REPORT,ROOM_REPORT,KEY_REPORT->"Meet the child at the bedroom mirror.";
        case READY->"Use a mirror to plan the child's next move.";
        case APPROACH_ROOM->"Follow the child to the assigned room.";
        case WAIT_GO->"Watch Other Mother, then signal Go or Stop.";
        case LOCKPICK->"Keep watch while the child picks the lock. Signal Stop if it is unsafe.";
        case SCAN->"Keep watch while the child searches with the Seeing Stone.";
        case COLLECT_EYES->"Keep Other Mother away while the child retrieves the eyes.";
        case LEAVE_ROOM->"Help the child leave the room and close the door.";
        case APPROACH_AMBUSH->"Follow the child to the planned hiding place.";
        case WAIT_LURE,STEAL_KEY->"Lure Other Mother close and keep her distracted.";
        case APPROACH_EXIT->"Follow the child to the Little Door.";
        case INTERCEPT->"Approach Other Mother at the Little Door to hear her wager.";
        case WAGER->"Let the child reach the starting hiding place.";
        case HIDDEN->"Scout ahead and signal the child's next hiding place.";
        case MOVING_HIDE->"Watch Other Mother while the child moves between hiding places.";
        case FINAL_CROSSING,OPEN_DOOR,CRAWL->"Distract the Beldam. Help the child escape through the Little Door.";
        case SAFE,RESTORING->"You reached safety. Your original body is returning.";
        case FAREWELL->"The child is heading home.";
        case FAILED_SEWING->"Other Mother has caught the child.";
        case COMPLETE->"";
    };}
    static long countdown(CompoundTag n,long clock){
        long remaining=switch(phase(n)){
            case WAIT_GO->2400-age(n,clock);
            case WAIT_LURE,STEAL_KEY->6000-Math.max(0,clock-n.getLong("AmbushStarted"));
            case SCAN,COLLECT_EYES->300-age(n,clock);
            case OPEN_DOOR->60-age(n,clock);
            case LOCKPICK->n.contains("PickStarted")?100-Math.max(0,clock-n.getLong("PickStarted")):0;
            default->0;
        };return Math.max(0,remaining);
    }
    public static boolean unlockedAndEntered(CompoundTag ghost,boolean actualDoorOpen,boolean physicallyInside){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.LOCKPICK||!actualDoorOpen||!physicallyInside)return false;
        n.putBoolean("StoneVisible",true);move(n,n.getBoolean("Retrieving")?Phase.COLLECT_EYES:Phase.SCAN,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean scanned(CompoundTag ghost,boolean stillPhysicallyScanning){
        var n=data(ghost);if(!owns(ghost,n)||!stillPhysicallyScanning||phase(n)!=Phase.SCAN||age(n,GhostVictimProgress.clock(ghost))<300)return false;
        if(n.getString("EyeRoom").equals(n.getString("AssignedRoom")))n.putBoolean("EyeRoomKnown",true);
        n.remove("StoneVisible");move(n,Phase.LEAVE_ROOM,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean collect(CompoundTag ghost,boolean atActualContainer,boolean usedActualStolenKey){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.COLLECT_EYES||age(n,GhostVictimProgress.clock(ghost))<300
            ||!atActualContainer||!usedActualStolenKey||!n.getBoolean("KeyStolen")||!n.getString("AssignedRoom").equals(n.getString("EyeRoom")))return false;
        n.putBoolean("PlayerEyesHeld",true);n.put("GhostEyesHeld",n.getList("UnresolvedGhosts",Tag.TAG_STRING).copy());n.remove("StoneVisible");move(n,Phase.LEAVE_ROOM,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean leftRoom(CompoundTag ghost,boolean actualExitAndClosedDoor){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.LEAVE_ROOM||!actualExitAndClosedDoor)return false;
        n.remove("ConcealStone");n.remove("StoneVisible");move(n,n.getBoolean("PlayerEyesHeld")?Phase.APPROACH_EXIT:n.getBoolean("EyeRoomKnown")?Phase.ROOM_REPORT:Phase.READY,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean hiddenForKey(CompoundTag ghost,String hidingSpot,boolean actualConcealment){
        var n=data(ghost);if(!owns(ghost,n)||!actualConcealment||!n.getBoolean("EyeRoomKnown")||n.getBoolean("KeyStolen")||phase(n)!=Phase.APPROACH_AMBUSH||hidingSpot==null||hidingSpot.isBlank())return false;
        n.putString("HidingSpot",hidingSpot);n.putLong("AmbushStarted",GhostVictimProgress.clock(ghost));move(n,Phase.WAIT_LURE,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean keyOpportunity(CompoundTag ghost,boolean motherActuallyNearby,boolean distractedAway,boolean childActuallyReaching){
        var n=data(ghost);if(!owns(ghost,n)||!(phase(n)==Phase.WAIT_LURE||phase(n)==Phase.STEAL_KEY))return false;
        long clock=GhostVictimProgress.clock(ghost);
        if(!motherActuallyNearby||!distractedAway||!childActuallyReaching){n.remove("OpportunityAt");return false;}
        if(!n.contains("OpportunityAt")){move(n,Phase.STEAL_KEY,clock);n.putLong("OpportunityAt",clock);return false;}
        if(clock-n.getLong("OpportunityAt")<60)return false;
        n.putBoolean("KeyStolen",true);move(n,Phase.KEY_REPORT,clock);return true;
    }
    public static void witnessed(CompoundTag ghost,Discovery discovery,boolean actualObservation){
        var n=data(ghost);if(!owns(ghost,n)||!active(n)||!actualObservation||discovery==Discovery.NONE)return;
        if(Set.of(Phase.INTERCEPT,Phase.WAGER,Phase.SAFE,Phase.RESTORING,Phase.FAREWELL).contains(phase(n)))return;
        if(discovery==Discovery.PHYSICAL_CAPTURE||discovery==Discovery.EXPOSED_SEEING_STONE||discovery==Discovery.EYE_ROOM){
            n.putString("FailureReason",discovery.name());n.remove("StoneVisible");n.putBoolean("StoneConfiscated",true);move(n,Phase.FAILED_SEWING,GhostVictimProgress.clock(ghost));return;
        }
        suspicion(n,discovery==Discovery.STEALING?.35:discovery==Discovery.LOCKPICKING?.2:.12);
        n.putBoolean("ConcealStone",true);move(n,discovery==Discovery.STEALING?Phase.READY:Phase.LEAVE_ROOM,GhostVictimProgress.clock(ghost));
    }
    public static boolean expireAssignment(CompoundTag ghost){
        var n=data(ghost);if(!owns(ghost,n))return false;long age=age(n,GhostVictimProgress.clock(ghost));
        if(phase(n)==Phase.WAIT_GO&&age>=2400||phase(n)==Phase.APPROACH_AMBUSH&&age>=1200
            ||(phase(n)==Phase.WAIT_LURE||phase(n)==Phase.STEAL_KEY)&&GhostVictimProgress.clock(ghost)-n.getLong("AmbushStarted")>=6000){
            n.remove("AssignedRoom");n.remove("HidingSpot");n.remove("TargetHide");n.remove("AmbushStarted");n.remove("ReportSpoken");
            move(n,Phase.READY,GhostVictimProgress.clock(ghost));return true;
        }return false;
    }
    /** Abandon an obstructed search, never its actual exit or collected custody.
     * Inside-room attempts still have to walk out and close the real door. */
    public static boolean abandonBlockedRoom(CompoundTag ghost){
        var n=data(ghost);var phase=phase(n);
        if(!owns(ghost,n)||age(n,GhostVictimProgress.clock(ghost))<1200
            ||!Set.of(Phase.APPROACH_ROOM,Phase.LOCKPICK,Phase.SCAN,Phase.COLLECT_EYES).contains(phase))return false;
        n.putString("Blocker","Room search interrupted; the child must leave physically before retrying.");
        n.remove("StoneVisible");n.remove("ReportSpoken");n.putBoolean("ConcealStone",true);
        move(n,phase==Phase.APPROACH_ROOM?Phase.READY:Phase.LEAVE_ROOM,GhostVictimProgress.clock(ghost));
        return true;
    }
    public static boolean resolveHunt(CompoundTag ghost,boolean crossedActualSafePoint,boolean physicallyCaught){
        var n=data(ghost);if(!owns(ghost,n)||!Set.of(Phase.HIDDEN,Phase.MOVING_HIDE,Phase.FINAL_CROSSING,Phase.OPEN_DOOR,Phase.CRAWL).contains(phase(n)))return false;
        // A crossing already proven safe wins even if pursuit reaches the old
        // position in this same server tick. Touching the door is insufficient.
        if(crossedActualSafePoint&&phase(n)==Phase.CRAWL&&n.getBoolean("PlayerEyesHeld")){move(n,Phase.SAFE,GhostVictimProgress.clock(ghost));n.putBoolean("WagerWon",true);return true;}
        if(physicallyCaught){witnessed(ghost,Discovery.PHYSICAL_CAPTURE,true);return true;}return false;
    }
    public static boolean intercepted(CompoundTag ghost,boolean physicallyNearLittleDoor){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.APPROACH_EXIT||!n.getBoolean("PlayerEyesHeld")||!physicallyNearLittleDoor)return false;
        move(n,Phase.INTERCEPT,GhostVictimProgress.clock(ghost));return true;
    }
    static boolean handoffFailure(CompoundTag ghost){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.FAILED_SEWING||!n.getBoolean("QuestEyesReturnedToBeldam")||n.getBoolean("FailureHandedOff"))return false;
        GhostVictimProgress.move(ghost,GhostVictimProgress.Phase.VICTIM_CAPTURED);ghost.remove("ChildRoutine");n.putBoolean("FailureHandedOff",true);return true;
    }
    public static boolean wagerAgreed(CompoundTag ghost,boolean actualMotherConversation,boolean playerAccepted){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.INTERCEPT||!n.getBoolean("PlayerEyesHeld")||!actualMotherConversation||!playerAccepted)return false;
        n.putBoolean("WagerAgreed",true);move(n,Phase.WAGER,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean hidingArrival(CompoundTag ghost,String spot,boolean actualConcealment){
        var n=data(ghost);if(!owns(ghost,n)||!actualConcealment||spot==null||!spot.equals(n.getString("TargetHide"))||!Set.of(Phase.WAGER,Phase.MOVING_HIDE).contains(phase(n)))return false;
        if(phase(n)==Phase.WAGER&&(!n.getBoolean("WagerAgreed")||!n.getBoolean("PlayerEyesHeld")||!spot.equals(n.getString("InitialHide"))))return false;
        n.putString("HidingSpot",spot);n.remove("TargetHide");n.remove("StopRequested");move(n,Phase.HIDDEN,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean reachedDoor(CompoundTag ghost,boolean physicalArrival){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.FINAL_CROSSING||!physicalArrival)return false;
        move(n,Phase.OPEN_DOOR,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean openedDoor(CompoundTag ghost,boolean actualOpenDoor){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.OPEN_DOOR||!actualOpenDoor||age(n,GhostVictimProgress.clock(ghost))<60)return false;
        move(n,Phase.CRAWL,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean beginRestoration(CompoundTag ghost,CompoundTag originalSnapshot,boolean actualOverworldArrival){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.SAFE||!n.getBoolean("WagerWon")||!actualOverworldArrival||originalSnapshot==null
            ||!originalSnapshot.hasUUID("Transaction")||!originalSnapshot.hasUUID("Owner")||!originalSnapshot.hasUUID("Door")
            ||!ghost.hasUUID("Owner")||!ghost.hasUUID("Door")||!ghost.getUUID("Owner").equals(originalSnapshot.getUUID("Owner"))||!ghost.getUUID("Door").equals(originalSnapshot.getUUID("Door")))return false;
        n.putUUID("OriginalEscrow",originalSnapshot.getUUID("Transaction"));move(n,Phase.RESTORING,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean restorationCommitted(CompoundTag ghost,UUID originalEscrowTransaction,boolean originalEscrowRestored,boolean normalBodyRestored){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.RESTORING||!n.getBoolean("WagerWon")||!n.getBoolean("PlayerEyesHeld")
            ||originalEscrowTransaction==null||!n.hasUUID("OriginalEscrow")||!originalEscrowTransaction.equals(n.getUUID("OriginalEscrow"))||!originalEscrowRestored||!normalBodyRestored||n.getBoolean("RestorationCommitted"))return false;
        n.putUUID("RestoredEscrow",originalEscrowTransaction);n.putBoolean("RestorationCommitted",true);move(n,Phase.FAREWELL,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean finishFarewell(CompoundTag ghost,boolean actualDeparture,boolean agencySaved){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.FAREWELL||!n.getBoolean("RestorationCommitted")||!n.getBoolean("WagerWon")||!actualDeparture||!agencySaved)return false;
        n.putBoolean("ChildDepartureWitnessed",true);n.putBoolean("FarewellComplete",true);move(n,Phase.COMPLETE,GhostVictimProgress.clock(ghost));return true;
    }
    public static boolean bodyRestored(CompoundTag ghost){
        var n=ghost.getCompound(KEY);
        return owns(ghost,n)&&n.getBoolean("RestorationCommitted")&&Set.of(Phase.FAREWELL,Phase.COMPLETE).contains(phase(n))
            &&n.hasUUID("OriginalEscrow")&&n.hasUUID("RestoredEscrow")&&n.getUUID("OriginalEscrow").equals(n.getUUID("RestoredEscrow"));
    }
    public static boolean childDeparture(CompoundTag ghost,boolean physicallyDepartedHome){
        var n=data(ghost);if(!owns(ghost,n)||phase(n)!=Phase.FAREWELL||!n.getBoolean("RestorationCommitted")||!physicallyDepartedHome)return false;
        n.putBoolean("ChildDepartureWitnessed",true);return true;
    }
    private GhostEscapeProgress(){}
}
