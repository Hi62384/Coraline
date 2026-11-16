package com.coraline.feast;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Concealed quest inventory uses the original eye identities in the same
 * VisitData transaction. No replacement eye is dropped or awarded to a player. */
public final class GhostEscapeEyes {
    public static Set<String> unresolved(VisitData.Scene s){
        var ids=new LinkedHashSet<String>();if(RescueEyes.ledger(s).getBoolean("Released"))return ids;
        for(int i=0;i<3;i++)if(!RescueEyes.eye(s,i).getBoolean("Claimed"))ids.add("Eye"+i);return Set.copyOf(ids);
    }
    public static boolean collectWithChild(ServerPlayer p,VisitData.Scene s){
        if(s==null||!p.getUUID().equals(s.owner))return false;var ghost=GhostVictimEntry.data(s);var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        var child=GhostVictimActors.find(p,s,ghost);var at=GhostEscapeRooms.container(ghost,escape.getString("EyeRoom"));
        if(!GhostEscapeProgress.owns(ghost,escape)||!GhostVictimActors.owns(child,s,ghost)||!escape.getBoolean("PlayerEyesHeld")||!escape.getBoolean("KeyStolen")
            ||GhostEscapeProgress.phase(escape)!=GhostEscapeProgress.Phase.LEAVE_ROOM||at==null||child.level()!=AcceptedButtonEyesWorld.level(p)||child.distanceToSqr(at.getCenter())>9
            ||!escape.getString("EyeRoom").equals(escape.getString("AssignedRoom"))||!GhostEscapeRooms.validContainer(p,s,ghost,escape.getString("EyeRoom")))return false;
        var sight=child.level().clip(new net.minecraft.world.level.ClipContext(child.getEyePosition(),at.getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,child));
        if(sight.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&!sight.getBlockPos().equals(at))return false;
        var inventory=escape.getCompound("QuestInventory");var attempt=escape.getUUID("Attempt");
        for(Tag id:escape.getList("GhostEyesHeld",Tag.TAG_STRING)){
            String key=id.getAsString();if(!key.matches("Eye[0-2]"))return false;int index=key.charAt(3)-'0';var original=RescueEyes.eye(s,index);
            if(original.getBoolean("Claimed")||RescueEyes.ledger(s).getBoolean("Released"))continue;
            if(original.hasUUID("GhostEscapeHolder")&&!attempt.equals(original.getUUID("GhostEscapeHolder")))return false;
            if(inventory.contains(key)){if(!RescueEyes.authentic(ItemStack.of(inventory.getCompound(key)),s,index))return false;continue;}
        }
        for(Tag id:escape.getList("GhostEyesHeld",Tag.TAG_STRING)){
            String key=id.getAsString();int index=key.charAt(3)-'0';var original=RescueEyes.eye(s,index);
            if(original.getBoolean("Claimed")||RescueEyes.ledger(s).getBoolean("Released")||inventory.contains(key))continue;
            var item=new ItemStack(com.coraline.registry.ModItems.GHOST_EYE.get());var tag=item.getOrCreateTag();tag.putInt("GhostEye",index);tag.putUUID("EyeOwner",s.owner);tag.putUUID("EyeRealm",s.door);tag.putUUID("EyeIdentity",original.getUUID("Identity"));
            inventory.put(key,item.save(new CompoundTag()));original.putUUID("GhostEscapeHolder",attempt);
        }
        escape.put("QuestInventory",inventory);escape.putBoolean("QuestEyesSecured",true);OtherMotherLoop.changed(p);return true;
    }
    public static boolean forfeit(ServerPlayer p,VisitData.Scene s){
        if(s==null||!p.getUUID().equals(s.owner))return false;var ghost=GhostVictimEntry.data(s);
        if(!GhostVictimMirrors.owned(p,s,ghost)||!returnFailedCustody(s,ghost))return false;
        OtherMotherLoop.changed(p);return true;
    }
    /** Release only this failed attempt's canonical custody, even if its actor
     * is unloaded. Identities and already-freed souls are never recreated. */
    static boolean returnFailedCustody(VisitData.Scene s,CompoundTag ghost){
        if(s==null)return false;return returnFailedCustody(ghost,s.owner,s.door,RescueEyes.ledger(s));
    }
    static boolean returnFailedCustody(CompoundTag ghost,UUID owner,UUID door,CompoundTag eyeLedger){
        if(owner==null||door==null||!ghost.hasUUID("Owner")||!owner.equals(ghost.getUUID("Owner"))||!ghost.hasUUID("Door")||!door.equals(ghost.getUUID("Door")))return false;
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostEscapeProgress.owns(ghost,escape)||GhostEscapeProgress.phase(escape)!=GhostEscapeProgress.Phase.FAILED_SEWING)return false;
        if(!eyeLedger.getBoolean("Released"))for(int index=0;index<3;index++){
            var original=eyeLedger.getCompound("Eye"+index);
            if(original.hasUUID("GhostEscapeHolder")&&original.getUUID("GhostEscapeHolder").equals(escape.getUUID("Attempt")))original.remove("GhostEscapeHolder");
        }
        escape.remove("QuestInventory");escape.putBoolean("QuestEyesReturnedToBeldam",true);return true;
    }
    /** Move only the child's actual collected capabilities into canonical
     * farewell custody. Previously recovered player eyes use normal custody. */
    public static boolean prepareFarewell(ServerPlayer p,VisitData.Scene s){
        if(s==null||!p.getUUID().equals(s.owner))return false;var ghost=GhostVictimEntry.data(s);var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostEscapeProgress.owns(ghost,escape)||!escape.getBoolean("RestorationCommitted")||!escape.getBoolean("WagerWon"))return false;
        if(RescueEyes.ledger(s).getBoolean("Released"))return true;
        var inventory=escape.getCompound("QuestInventory");var root=ParentRescueStory.data(s);var custody=root.getCompound("Custody");root.put("Custody",custody);
        for(int index=0;index<3;index++){
            var original=RescueEyes.eye(s,index);if(original.getBoolean("Claimed"))continue;String key="Eye"+index;
            var item=ItemStack.of(inventory.getCompound(key));if(!RescueEyes.authentic(item,s,index)||!original.hasUUID("GhostEscapeHolder")||!escape.getUUID("Attempt").equals(original.getUUID("GhostEscapeHolder")))return false;
            var held=custody.getCompound("RetiredEye"+index);if(held.getBoolean("Returned")||held.getBoolean("Destroyed")||held.contains("Stack")&&!held.getCompound("Stack").equals(item.save(new CompoundTag())))return false;
        }
        for(int index=0;index<3;index++){
            var original=RescueEyes.eye(s,index);if(original.getBoolean("Claimed"))continue;String key="Eye"+index;
            var held=custody.getCompound("RetiredEye"+index);held.put("Stack",inventory.getCompound(key).copy());custody.put("RetiredEye"+index,held);inventory.remove(key);
            original.putBoolean("Claimed",true);original.putBoolean("RecoveredByGhostEscape",true);original.remove("GhostEscapeHolder");
        }
        OtherMotherLoop.changed(p);return RescueEyes.all(s);
    }
    private GhostEscapeEyes(){}
}
