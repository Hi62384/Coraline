package com.coraline.feast;

import java.util.UUID;
import net.minecraft.nbt.*;

/** Identity and action receipts for the alternate route's one anatomical Hand. */
final class GhostEscapeHandProgress {
    static final String KEY="HandDetachment";
    enum Phase { REACHING, FALLING, DETACHED }
    private static CompoundTag ending(CompoundTag ghost,UUID owner,UUID door){
        var row=GhostEscapeEndingProgress.existing(ghost,owner,door);
        return row!=null&&row.getString("Stage").equals(GhostEscapeEndingProgress.Stage.EPILOGUE_COMPLETE.name())&&row.getBoolean("AlternativeBeldamVictory")?row:null;
    }
    static CompoundTag existing(CompoundTag ghost,UUID owner,UUID door,UUID mother){
        var root=ending(ghost,owner,door);if(root==null||mother==null||!root.contains(KEY,Tag.TAG_COMPOUND))return null;var row=root.getCompound(KEY);
        for(String field:java.util.List.of("Owner","Door","Session","Attempt"))if(!row.hasUUID(field)||!root.getUUID(field).equals(row.getUUID(field)))return null;
        if(row.getInt("Version")!=1||!row.hasUUID("Mother")||!mother.equals(row.getUUID("Mother"))||!row.hasUUID("Hand")||mother.equals(row.getUUID("Hand")))return null;
        try{Phase.valueOf(row.getString("Phase"));}catch(IllegalArgumentException invalid){return null;}return row;
    }
    static CompoundTag begin(CompoundTag ghost,UUID owner,UUID door,UUID mother,UUID hand,boolean actualPartProven,boolean existingCanonicalCut){
        var root=ending(ghost,owner,door);if(root==null||mother==null||hand==null||mother.equals(hand)||!actualPartProven)return null;
        if(root.contains(KEY)){var prior=existing(ghost,owner,door,mother);return prior!=null&&hand.equals(prior.getUUID("Hand"))?prior:null;}
        var row=new CompoundTag();row.putInt("Version",1);for(String field:java.util.List.of("Owner","Door","Session","Attempt"))row.putUUID(field,root.getUUID(field));
        row.putUUID("Mother",mother);row.putUUID("Hand",hand);row.putString("Phase",existingCanonicalCut?Phase.DETACHED.name():Phase.REACHING.name());row.putBoolean("LandingProven",existingCanonicalCut);root.put(KEY,row);return row;
    }
    static boolean cut(CompoundTag ghost,UUID owner,UUID door,UUID mother,UUID hand,boolean actualClosedDoor,boolean actualOriginalKey,boolean actualReach){
        var row=existing(ghost,owner,door,mother);if(row==null||hand==null||!hand.equals(row.getUUID("Hand"))||!row.getString("Phase").equals(Phase.REACHING.name())||!actualClosedDoor||!actualOriginalKey||!actualReach)return false;
        row.putString("Phase",Phase.FALLING.name());row.putBoolean("CutByOriginalDoor",true);return true;
    }
    static boolean landed(CompoundTag ghost,UUID owner,UUID door,UUID mother,UUID hand,boolean actualGroundContact){
        var row=existing(ghost,owner,door,mother);if(row==null||hand==null||!hand.equals(row.getUUID("Hand"))||!row.getString("Phase").equals(Phase.FALLING.name())||!row.getBoolean("CutByOriginalDoor")||!actualGroundContact)return false;
        row.putString("Phase",Phase.DETACHED.name());row.putBoolean("LandingProven",true);return true;
    }
    static boolean handoffReady(CompoundTag ghost,UUID owner,UUID door,UUID mother,UUID canonicalHand){
        var row=existing(ghost,owner,door,mother);return row!=null&&canonicalHand!=null&&canonicalHand.equals(row.getUUID("Hand"))&&row.getString("Phase").equals(Phase.DETACHED.name())&&row.getBoolean("LandingProven");
    }
    private GhostEscapeHandProgress(){}
}
