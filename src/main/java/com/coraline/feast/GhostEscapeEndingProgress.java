package com.coraline.feast;

import java.util.UUID;
import net.minecraft.nbt.*;

/** A receipt for the real door-sealing handoff, not a shortcut through the
 * parent reunion, original souls' farewell or Crawling Hand prerequisites. */
final class GhostEscapeEndingProgress {
    static final String KEY="CanonicalEnding";
    enum Stage { RETURN_KEY, SEAL_DOOR, DOOR_SEALED, EPILOGUE, EPILOGUE_COMPLETE }
    static boolean mayIssueFirstKey(CompoundTag story,CompoundTag rescue){
        if(story.contains("LittleDoorKeyIssued")&&!story.contains("LittleDoorKeyIssued",Tag.TAG_BYTE)
            ||story.contains("LittleDoorKeyHolder")&&!story.contains("LittleDoorKeyHolder",Tag.TAG_STRING)
            ||story.contains("LittleDoorKeyCustodyVersion")&&!story.contains("LittleDoorKeyCustodyVersion",Tag.TAG_INT)
            ||rescue.contains("KeyCustodyVersion")&&!rescue.contains("KeyCustodyVersion",Tag.TAG_INT)
            ||rescue.contains("KeyTaken")&&!rescue.contains("KeyTaken",Tag.TAG_BYTE)
            ||rescue.contains("KeySwallowed")&&!rescue.contains("KeySwallowed",Tag.TAG_BYTE)
            ||rescue.contains("Custody")&&!rescue.contains("Custody",Tag.TAG_COMPOUND))return false;
        return !story.getBoolean("LittleDoorKeyIssued")&&story.getString("LittleDoorKeyHolder").isBlank()
            &&story.getInt("LittleDoorKeyCustodyVersion")==0&&rescue.getInt("KeyCustodyVersion")==0
            &&!rescue.getBoolean("KeyTaken")&&!rescue.getBoolean("KeySwallowed")
            &&!rescue.getCompound("Custody").contains("Key")&&!rescue.contains("LegacyKeyReplacementReceipt");
    }
    static boolean eligible(CompoundTag ghost,UUID owner,UUID door){
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        return owner!=null&&door!=null&&ghost.hasUUID("Owner")&&owner.equals(ghost.getUUID("Owner"))
            &&ghost.hasUUID("Door")&&door.equals(ghost.getUUID("Door"))&&GhostEscapeProgress.bodyRestored(ghost)
            &&GhostEscapeProgress.phase(escape)==GhostEscapeProgress.Phase.COMPLETE&&escape.getBoolean("WagerWon")
            &&escape.getBoolean("QuestEyesSecured")&&escape.getBoolean("ChildDepartureWitnessed")
            &&escape.getBoolean("FarewellComplete")&&escape.getBoolean("AgencyRegistrationSaved");
    }
    static CompoundTag existing(CompoundTag ghost,UUID owner,UUID door){
        if(!eligible(ghost,owner,door))return null;var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!escape.contains(KEY,Tag.TAG_COMPOUND))return null;var row=escape.getCompound(KEY);
        if(row.getInt("Version")!=1||!row.hasUUID("Owner")||!owner.equals(row.getUUID("Owner"))
            ||!row.hasUUID("Door")||!door.equals(row.getUUID("Door"))||!row.hasUUID("Session")||!ghost.getUUID("Session").equals(row.getUUID("Session"))
            ||!row.hasUUID("Attempt")||!escape.getUUID("Attempt").equals(row.getUUID("Attempt")))return null;
        try{Stage.valueOf(row.getString("Stage"));}catch(IllegalArgumentException invalid){return null;}
        return row;
    }
    static CompoundTag begin(CompoundTag ghost,UUID owner,UUID door){
        if(!eligible(ghost,owner,door))return null;var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        if(escape.contains(KEY))return existing(ghost,owner,door); // Never overwrite a foreign/corrupt journal.
        var row=new CompoundTag();row.putInt("Version",1);row.putUUID("Owner",owner);row.putUUID("Door",door);
        row.putUUID("Session",ghost.getUUID("Session"));row.putUUID("Attempt",escape.getUUID("Attempt"));row.putString("Stage",Stage.RETURN_KEY.name());
        escape.put(KEY,row);return row;
    }
    static boolean keyPresent(CompoundTag ghost,UUID owner,UUID door,boolean originalKeyInInventory){
        var row=existing(ghost,owner,door);if(row==null||!originalKeyInInventory||!row.getString("Stage").equals(Stage.RETURN_KEY.name()))return false;
        row.putString("Stage",Stage.SEAL_DOOR.name());return true;
    }
    static boolean sealed(CompoundTag ghost,UUID owner,UUID door,boolean actualSourceWorld,boolean originalKeyPresent,boolean actualClosedNonPortalDoor){
        var row=existing(ghost,owner,door);if(row==null||!actualSourceWorld||!originalKeyPresent||!actualClosedNonPortalDoor
            ||!java.util.Set.of(Stage.RETURN_KEY.name(),Stage.SEAL_DOOR.name()).contains(row.getString("Stage")))return false;
        row.putString("Stage",Stage.DOOR_SEALED.name());row.putBoolean("SealedByOwner",true);return true;
    }
    static boolean beginEpilogue(CompoundTag ghost,UUID owner,UUID door,CompoundTag chapter,UUID family,UUID mother,UUID father,boolean actualParentsReady,boolean originalEyesAccounted){
        return beginEpilogue(ghost,owner,door,chapter,family,mother,father,actualParentsReady,originalEyesAccounted,RescueChapter.Phase.REUNION_A);
    }
    static boolean beginEpilogue(CompoundTag ghost,UUID owner,UUID door,CompoundTag chapter,UUID family,UUID mother,UUID father,boolean actualParentsReady,boolean originalEyesAccounted,RescueChapter.Phase firstPhase){
        var row=existing(ghost,owner,door);if(row==null||!row.getString("Stage").equals(Stage.DOOR_SEALED.name())||!row.getBoolean("SealedByOwner")
            ||!actualParentsReady||!originalEyesAccounted||family==null||mother==null||father==null||mother.equals(father)
            ||firstPhase==null||!java.util.Set.of(RescueChapter.Phase.REUNION_A,RescueChapter.Phase.CAT_NOTICE,RescueChapter.Phase.RELEASED_SPIRITS).contains(firstPhase))return false;
        if(chapter.contains("AlternateGhostAttempt")&&(!chapter.hasUUID("AlternateGhostAttempt")||!row.getUUID("Attempt").equals(chapter.getUUID("AlternateGhostAttempt"))))return false;
        for(String key:java.util.List.of("RealFamily","RealMother","RealFather"))if(row.contains(key))return false;
        row.putUUID("RealFamily",family);row.putUUID("RealMother",mother);row.putUUID("RealFather",father);
        chapter.putUUID("AlternateGhostAttempt",row.getUUID("Attempt"));chapter.putUUID("AlternateGhostSession",row.getUUID("Session"));chapter.putUUID("AlternateGhostOwner",owner);chapter.putUUID("AlternateGhostDoor",door);
        chapter.putString("Phase",firstPhase.name());chapter.putInt("Ticks",0);chapter.remove("Audio");chapter.remove("AudioTail");chapter.remove("Recover");chapter.putUUID("Input",UUID.randomUUID());chapter.remove("LastInput");
        chapter.putBoolean("AlternateGhostEscape",true);chapter.putBoolean("Started",true);row.putString("Stage",Stage.EPILOGUE.name());return true;
    }
    static boolean epilogueOwns(CompoundTag ghost,UUID owner,UUID door,CompoundTag chapter){
        var row=existing(ghost,owner,door);if(row==null||!row.getBoolean("SealedByOwner")||!chapter.getBoolean("Started")||!chapter.getBoolean("AlternateGhostEscape")
            ||!java.util.Set.of(Stage.EPILOGUE.name(),Stage.EPILOGUE_COMPLETE.name()).contains(row.getString("Stage")))return false;
        for(String field:java.util.List.of("Owner","Door","Session","Attempt"))if(!chapter.hasUUID("AlternateGhost"+field)||!row.getUUID(field).equals(chapter.getUUID("AlternateGhost"+field)))return false;
        for(String field:java.util.List.of("RealFamily","RealMother","RealFather"))if(!row.hasUUID(field))return false;
        if(row.getUUID("RealMother").equals(row.getUUID("RealFather")))return false;
        try{return RescueChapter.Phase.valueOf(chapter.getString("Phase")).ordinal()>=RescueChapter.Phase.REUNION_A.ordinal();}catch(IllegalArgumentException bad){return false;}
    }
    static boolean parentsMatch(CompoundTag ghost,UUID owner,UUID door,UUID family,UUID mother,UUID father){
        var row=existing(ghost,owner,door);return row!=null&&family!=null&&mother!=null&&father!=null&&row.hasUUID("RealFamily")&&family.equals(row.getUUID("RealFamily"))
            &&row.hasUUID("RealMother")&&mother.equals(row.getUUID("RealMother"))&&row.hasUUID("RealFather")&&father.equals(row.getUUID("RealFather"));
    }
    static boolean finishEpilogue(CompoundTag ghost,UUID owner,UUID door,CompoundTag chapter,boolean originalSoulsReleased){
        var row=existing(ghost,owner,door);if(row==null||!row.getString("Stage").equals(Stage.EPILOGUE.name())||!epilogueOwns(ghost,owner,door,chapter)
            ||!chapter.getString("Phase").equals(RescueChapter.Phase.COMPLETE.name())||!chapter.getBoolean("ParentsReturned")
            ||!chapter.getBoolean("FarewellComplete")||!chapter.getBoolean("FarewellCatDeparted")||chapter.contains("VisionReturn")&&!chapter.getBoolean("VisionRestored")||!originalSoulsReleased)return false;
        row.putString("Stage",Stage.EPILOGUE_COMPLETE.name());row.putBoolean("AlternativeBeldamVictory",true);return true;
    }
    private GhostEscapeEndingProgress(){}
}
