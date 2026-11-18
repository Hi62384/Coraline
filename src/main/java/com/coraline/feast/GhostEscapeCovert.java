package com.coraline.feast;

import com.coraline.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

/** Physical covert-stage adapter. The complete chapter controller must compose
 * this with wager, hunt and canonical ending before enabling escape entry. */
public final class GhostEscapeCovert {
    public static boolean tryBegin(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var child=GhostVictimActors.find(p,s,ghost);
        if(!GhostVictimMirrors.owned(p,s,ghost)||child==null||GhostVictimProgress.phase(ghost)!=GhostVictimProgress.Phase.BEDTIME
            ||!ghost.contains("BedtimeWindowStarted")||!GhostVictimProgress.readyToHelp(ghost)
            ||!GhostVictimMirrors.childPresent(p,ghost,ghost.getString("Mirror"),child))return false;
        var rooms=GhostEscapeRooms.prepare(p,s,ghost,child);
        if(!GhostEscapeProgress.begin(ghost,rooms,GhostEscapeEyes.unresolved(s),true))return false;
        if(!GhostEscapeHiding.prepare(p,s,ghost,child)){ghost.putString("Blocker",ghost.getCompound(GhostEscapeProgress.KEY).getString("Blocker"));ghost.remove(GhostEscapeProgress.KEY);return false;}
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);GhostEscapeProgress.bedtimeAgreement(ghost,true);escape.putLong("DepartAfter",GhostVictimProgress.clock(ghost)+100);
        GhostVictimDirector.words(ghost,"I believe you. I want to help you get out of here. I'll ask Miss Spink and Miss Forcible for a Seeing Stone.");OtherMotherLoop.changed(p);return true;
    }
    /** Runs for every escape adapter, after an actual safe crossing gets its
     * winning tie, before ambush/covert handlers can short-circuit dispatch. */
    public static void monitor(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var e=ghost.getCompound(GhostEscapeProgress.KEY);if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,e))return;
        var child=GhostVictimActors.find(p,s,ghost);if(child==null)return;
        var mother=AcceptedButtonEyesWorld.mother(p,s);
        var discovery=GhostEscapePerception.observe(p,s,ghost,child,mother);
        if(discovery!=GhostEscapeProgress.Discovery.NONE){
            var old=GhostEscapeProgress.phase(e);GhostEscapeProgress.witnessed(ghost,discovery,true);
            if(old!=GhostEscapeProgress.phase(e)){hands(child);ghost.remove("ChildRoutine");GhostVictimDirector.words(ghost,"She's seen me. I have to stop!");}
        }
        if(GhostEscapeProgress.expireAssignment(ghost)){hands(child);child.getNavigation().stop();ghost.remove("ChildRoutine");GhostVictimDirector.words(ghost,"I can't wait here any longer. We can try again later.");OtherMotherLoop.changed(p);}
        if(GhostEscapeProgress.abandonBlockedRoom(ghost)){hands(child);child.getNavigation().stop();ghost.remove("ChildRoutine");GhostVictimDirector.words(ghost,"I can't reach it. I'll step back and try again later.");OtherMotherLoop.changed(p);}
    }
    public static boolean tick(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var e=ghost.getCompound(GhostEscapeProgress.KEY);if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,e))return false;
        var child=GhostVictimActors.find(p,s,ghost);
        if(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.FAILED_SEWING){
            if(!e.getBoolean("FailureHandedOff")){
                var stone=stone(e);if(!stone.isEmpty())e.put("ConfiscatedStone",stone.save(new CompoundTag()));
                if(!GhostEscapeEyes.forfeit(p,s))return true;
                if(child!=null){hands(child);child.getNavigation().stop();child.setSprinting(false);child.stopRiding();child.setPose(net.minecraft.world.entity.Pose.STANDING);child.refreshDimensions();child.gesture(5,60);}
                GhostEscapeProgress.handoffFailure(ghost);OtherMotherLoop.changed(p);
            }
            return false; // The existing physical sewing scene owns the loss.
        }
        if(child==null)return true;
        long clock=GhostVictimProgress.clock(ghost);var level=AcceptedButtonEyesWorld.level(p);var mother=AcceptedButtonEyesWorld.mother(p,s);
        switch(GhostEscapeProgress.phase(e)){
            case HOME_ERRAND->{
                if(clock<e.getLong("DepartAfter")){child.getNavigation().stop();return true;}
                if(GhostVictimProgress.phase(ghost)==GhostVictimProgress.Phase.BEDTIME){GhostVictimProgress.leaveMirror(ghost);GhostVictimMirrors.close(p);GhostVictimSchedule.tomorrow(ghost);GhostVictimProgress.move(ghost,GhostVictimProgress.Phase.NIGHT_PREPARATION);ghost.remove("ChildRoutine");}
                if(!ghost.getBoolean("ChildAtHome"))return false;
                var source=com.coraline.otherworld.DoorData.get(p.server).doors.get(s.door);
                if(source==null||!s.owner.equals(source.owner)||!source.dimension.equals(child.level().dimension().location().toString())||!child.isInvisible())return false;
                if(!retrieveStoneFromVisitors(p,s,e,child))return true;
                GhostEscapeProgress.homeErrand(ghost,true);OtherMotherLoop.changed(p);return false;
            }
            case STONE_REPORT,ROOM_REPORT,KEY_REPORT->{
                if(child.level()!=level||ghost.getBoolean("ChildAtHome")||GhostVictimProgress.phase(ghost)==GhostVictimProgress.Phase.DAILY_ARRIVAL)return false;
                var destination=GhostVictimWorld.bedtimeConversationPosition(s,level,child,ghost);
                if(destination==null){
                    if(clock%40==0){
                        ghost.remove("WallMirrorsPrepared");
                        GhostVictimWorld.ensureHouseProps(p,s,ghost);
                    }
                    e.putString("Blocker","Clear the floor and sightline in front of the bedroom mirror.");
                    return true;
                }
                if(GhostVictimRoutines.travel(p,s,ghost,child,destination,false)==GhostVictimNavigation.Result.ARRIVED){
                    GhostVictimMirrors.faceMirror(ghost,"bedroom",child);
                    if(!GhostVictimMirrors.childAtMirror(ghost,"bedroom",child,true))return true;
                    e.remove("Blocker");
                    hands(child);
                    if(!e.getBoolean("ReportSpoken")){GhostVictimDirector.words(ghost,switch(GhostEscapeProgress.phase(e)){case STONE_REPORT->"I have the Seeing Stone. Tell me where to look.";case KEY_REPORT->"I got her key. We have to be careful.";default->e.getBoolean("KeyStolen")?"Tell me when it's safe to get the eyes.":"I found the eyes, but the container is locked.";});e.putBoolean("ReportSpoken",true);}
                }
                return true;
            }
            case APPROACH_ROOM->{
                if(child.level()!=level)return false;var at=GhostEscapeRooms.outside(s,e.getString("AssignedRoom"),level);
                if(at==null){e.putString("Blocker","The selected room's original door is unavailable.");return true;}
                if(GhostVictimRoutines.travel(p,s,ghost,child,at,false)==GhostVictimNavigation.Result.ARRIVED){GhostEscapeProgress.roomWaiting(ghost,true);ghost.remove("ChildRoutine");hands(child);GhostVictimDirector.words(ghost,"I'll wait for your signal.");}
                return true;
            }
            case WAIT_GO->{child.getNavigation().stop();hands(child);return true;}
            case LOCKPICK->{
                if(child.level()!=level)return true;String id=e.getString("AssignedRoom");var outside=GhostEscapeRooms.outside(s,id,level);var inside=GhostEscapeRooms.inside(s,id,level);
                if(outside==null||inside==null){e.putString("Blocker","The selected room's entrance changed.");return true;}
                if(!e.getCompound("UnlockedRooms").getBoolean(id)){
                    if(GhostVictimRoutines.travel(p,s,ghost,child,outside,false)!=GhostVictimNavigation.Result.ARRIVED)return true;
                    child.gesture(2,5);child.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_NUGGET));var door=GhostEscapeRooms.door(s,id);child.getLookControl().setLookAt(door.getX()+.5,door.getY()+.8,door.getZ()+.5,30,30);
                    if(!e.contains("PickStarted"))e.putLong("PickStarted",clock);
                    if(clock-e.getLong("PickStarted")<100||!GhostEscapeRooms.openPickedDoor(p,s,ghost,child))return true;e.remove("PickStarted");ghost.remove("ChildRoutine");
                }
                if(GhostVictimRoutines.travel(p,s,ghost,child,inside,false)==GhostVictimNavigation.Result.ARRIVED){
                    var room=selectedRoom(e.getString("AssignedRoom"));boolean entered=room!=null&&com.coraline.secrets.HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,child.blockPosition());
                    if(GhostEscapeProgress.unlockedAndEntered(ghost,true,entered)){ghost.remove("ChildRoutine");child.setItemSlot(EquipmentSlot.MAINHAND,stone(e));}
                }
                return true;
            }
            case SCAN->{
                var room=selectedRoom(e.getString("AssignedRoom"));if(room==null||child.level()!=level)return true;
                child.gesture(8,5);child.setItemSlot(EquipmentSlot.MAINHAND,stone(e));var center=GhostEscapeRooms.container(ghost,e.getString("AssignedRoom"));if(center==null)return true;
                String assigned=e.getString("AssignedRoom");
                if(!GhostEscapeRooms.validContainer(p,s,ghost,assigned))return true;
                var at=GhostEscapeRooms.approachContainer(child,center);
                if(at==null||GhostVictimRoutines.travel(p,s,ghost,child,at,false)!=GhostVictimNavigation.Result.ARRIVED)return true;
                var focus=center.getCenter();
                child.getLookControl().setLookAt(focus.x,focus.y,focus.z,25,25);
                var delta=focus.subtract(child.getEyePosition());
                if(delta.lengthSqr()>16||GhostVictimNavigation.view(child).dot(delta.normalize())<.6)return true;
                var hit=level.clip(new net.minecraft.world.level.ClipContext(child.getEyePosition(),focus,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,child));
                if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&!center.equals(hit.getBlockPos()))return true;
                if(GhostEscapeProgress.scanned(ghost,com.coraline.secrets.HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,child.blockPosition()))){
                    hands(child);ghost.remove("ChildRoutine");
                    GhostVictimDirector.words(ghost,e.getBoolean("EyeRoomKnown")?"The Stone showed the ghost eyes in this cabinet. I need the key.":"The Stone doesn't show any eyes in this cabinet.");
                }
                return true;
            }
            case COLLECT_EYES->{
                if(child.level()!=level)return true;var at=GhostEscapeRooms.container(ghost,e.getString("EyeRoom"));if(at==null||!GhostEscapeRooms.validContainer(p,s,ghost,e.getString("EyeRoom")))return true;
                var approach=GhostVictimNavigation.approach(child,at);if(approach==null)return true;
                if(GhostVictimRoutines.travel(p,s,ghost,child,approach,false)!=GhostVictimNavigation.Result.ARRIVED)return true;
                child.gesture(2,5);var key=ItemStack.of(e.getCompound("QuestInventory").getCompound("ContainerKey"));if(!keyAuthentic(key,s,e)){e.putString("Blocker","The original stolen key is not in the child's concealed quest inventory.");return true;}
                child.setItemSlot(EquipmentSlot.MAINHAND,key.copy());child.getLookControl().setLookAt(at.getX()+.5,at.getY()+.5,at.getZ()+.5,30,30);
                var before=e.copy();
                if(GhostEscapeProgress.collect(ghost,true,true)){
                    if(!GhostEscapeEyes.collectWithChild(p,s)){ghost.put(GhostEscapeProgress.KEY,before);return true;}
                    hands(child);ghost.remove("ChildRoutine");GhostVictimDirector.words(ghost,"I've got the eyes. Let's get out of here.");
                }
                return true;
            }
            case LEAVE_ROOM->{
                hands(child);if(child.level()!=level)return true;String roomId=e.getString("AssignedRoom");
                var exit=e.contains("ExitDoor")?BlockPos.of(e.getLong("ExitDoor")):null;
                var outside=GhostEscapeRooms.outside(s,roomId,exit,level);
                if(outside==null){exit=GhostEscapeRooms.reachableExit(s,roomId,level,child);if(exit==null){e.putString("Blocker","All authored room exits are blocked. Restore one existing doorway, then the child can leave safely.");return true;}
                    e.putLong("ExitDoor",exit.asLong());outside=GhostEscapeRooms.outside(s,roomId,exit,level);OtherMotherLoop.changed(p);}
                if(outside==null)return true;
                if(GhostVictimRoutines.travel(p,s,ghost,child,outside,false)==GhostVictimNavigation.Result.ARRIVED&&GhostEscapeRooms.closeAfterLeaving(p,s,ghost,child)){
                    GhostEscapeProgress.leftRoom(ghost,true);e.remove("ExitDoor");e.remove("ReportSpoken");ghost.remove("ChildRoutine");OtherMotherLoop.changed(p);
                }
                return true;
            }
            case APPROACH_EXIT->{
                if(child.level()!=level)return true;var realm=AcceptedButtonEyesWorld.realm(p,s);if(realm==null||realm.doorPos==null)return true;
                var approach=GhostVictimNavigation.approach(child,realm.doorPos);if(approach==null){e.putString("Blocker","The Little Door has no clear reachable approach.");return true;}
                if(GhostVictimRoutines.travel(p,s,ghost,child,approach,true)!=GhostVictimNavigation.Result.ARRIVED)return true;
                if(mother!=null&&mother.level()==child.level()&&p.level()==level&&mother.distanceToSqr(child)<16
                    &&GhostVictimNavigation.canSee(mother,child,4,360)&&p.distanceToSqr(realm.doorPos.getCenter())<144){
                    if(GhostEscapeProgress.intercepted(ghost,true)){ghost.remove("ChildRoutine");child.getNavigation().stop();GhostEscapeWager.open(p,s,ghost);OtherMotherLoop.changed(p);}
                }else{child.getLookControl().setLookAt(realm.doorPos.getX()+.5,realm.doorPos.getY()+.5,realm.doorPos.getZ()+.5,30,30);}
                return true;
            }
            case INTERCEPT->{
                hands(child);child.getNavigation().stop();child.setSprinting(false);
                if(mother!=null&&mother.level()==child.level())child.getLookControl().setLookAt(mother,30,30);
                return true; // The wager owns the child; bedtime cannot walk it away.
            }
            default->{return false;}
        }
    }
    private static boolean retrieveStoneFromVisitors(ServerPlayer p,VisitData.Scene s,CompoundTag escape,
                                                   com.coraline.tycoon.TycoonNpc child){
        var held=stone(escape);
        if(!held.isEmpty()){
            boolean authentic=authenticStone(s,escape,held);
            if(!authentic)escape.putString("Blocker","The saved Seeing Stone conflicts with this child's handoff. Its original record was preserved.");
            return authentic;
        }
        var home=(net.minecraft.server.level.ServerLevel)child.level();
        var front=child.blockPosition();
        var first=com.coraline.otherworld.OtherWorld.safePlayer(home,front.east(2));
        var second=com.coraline.otherworld.OtherWorld.safePlayer(home,front.west(2));
        if(first==null||second==null||first.equals(second)||first.distSqr(front)>25||second.distSqr(front)>25)return false;
        var spink=ShowActors.acquire(p,s,"miss_spink",home,net.minecraft.world.phys.Vec3.atBottomCenterOf(first));
        var forcible=ShowActors.acquire(p,s,"miss_forcible",home,net.minecraft.world.phys.Vec3.atBottomCenterOf(second));
        if(spink==null||forcible==null||spink.level()!=home||forcible.level()!=home
                ||spink.distanceToSqr(child)>25||forcible.distanceToSqr(child)>25)return false;
        var issuance=escape.getCompound("StoneIssuance");
        if(!escape.contains("StoneIssuance")){
            if(escape.contains("StoneReceipt")&&(!escape.hasUUID("StoneReceipt")
                    ||!matches(escape,"StoneSpink",spink.getUUID())||!matches(escape,"StoneForcible",forcible.getUUID())))return false;
            var receipt=escape.hasUUID("StoneReceipt")?escape.getUUID("StoneReceipt"):java.util.UUID.randomUUID();
            issuance.putUUID("Owner",s.owner);
            issuance.putUUID("Door",s.door);
            issuance.putUUID("Child",child.getUUID());
            issuance.putUUID("Spink",spink.getUUID());
            issuance.putUUID("Forcible",forcible.getUUID());
            issuance.putUUID("Receipt",receipt);
            issuance.putString("Stage","ISSUED");
            var authorized=new ItemStack(ModItems.SEEING_STONE.get());
            var tag=authorized.getOrCreateTag();
            tag.putUUID("StoryOwner",s.owner);tag.putUUID("StoryDoor",s.door);tag.putUUID("GhostStoneReceipt",receipt);
            issuance.put("SourceStack",authorized.save(new CompoundTag()));
            escape.put("StoneIssuance",issuance);
            OtherMotherLoop.changed(p);
            p.server.overworld().getDataStorage().save();
            return false;
        }
        if(!"ISSUED".equals(issuance.getString("Stage"))||!matches(issuance,"Owner",s.owner)
                ||!matches(issuance,"Door",s.door)||!matches(issuance,"Child",child.getUUID())
                ||!matches(issuance,"Spink",spink.getUUID())||!matches(issuance,"Forcible",forcible.getUUID())
                ||!issuance.hasUUID("Receipt"))return false;
        var item=ItemStack.of(issuance.getCompound("SourceStack"));
        if(!stoneMatches(s,item,issuance.getUUID("Receipt"))||!savedIssuance(p,s,escape,issuance))return false;
        var originalHeld=forcible.getMainHandItem();
        forcible.setItemSlot(EquipmentSlot.MAINHAND,item);
        child.getLookControl().setLookAt(forcible,30,30);
        forcible.getLookControl().setLookAt(child,30,30);
        var inventory=escape.getCompound("QuestInventory");
        inventory.put("Stone",forcible.getMainHandItem().save(new CompoundTag()));
        escape.put("QuestInventory",inventory);
        forcible.setItemSlot(EquipmentSlot.MAINHAND,originalHeld);
        issuance.remove("SourceStack");
        issuance.putString("Stage","TRANSFERRED");
        escape.remove("Blocker");
        escape.putString("StoneSource","SPINK_AND_FORCIBLE_HANDOFF");
        ShowActors.releaseHidden(p,s,"miss_spink");
        ShowActors.releaseHidden(p,s,"miss_forcible");
        OtherMotherLoop.changed(p);
        return true;
    }
    private static boolean savedIssuance(ServerPlayer p,VisitData.Scene scene,CompoundTag escape,CompoundTag issuance){
        long now=p.server.overworld().getGameTime(),after=escape.getLong("StoneSaveRetryAt");
        if(after>now&&after-now<=40)return false;
        var file=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/coraline_first_visits.dat");
        try{
            var saved=VisitData.load(com.coraline.rebirth.StoryRunStore.read(file).getCompound("data")).scenes.get(scene.owner);
            if(saved!=null&&scene.door.equals(saved.door)){
                var disk=GhostVictimEntry.data(saved).getCompound(GhostEscapeProgress.KEY);
                if(matches(disk,"Attempt",escape.getUUID("Attempt"))&&issuance.equals(disk.getCompound("StoneIssuance"))){
                    escape.remove("StoneSaveRetryAt");
                    return true;
                }
            }
        }catch(java.io.IOException|RuntimeException unavailable){
            // The source capability remains in issuance custody while storage recovers.
        }
        escape.putLong("StoneSaveRetryAt",now+40);
        escape.putString("Blocker","Waiting for the Seeing Stone issuance to be saved.");
        OtherMotherLoop.changed(p);
        p.server.overworld().getDataStorage().save();
        return false;
    }
    private static boolean matches(CompoundTag n,String key,java.util.UUID id){
        return n.hasUUID(key)&&id.equals(n.getUUID(key));
    }
    private static boolean stoneMatches(VisitData.Scene scene,ItemStack item,java.util.UUID receipt){
        var tag=item.getTag();
        return item.is(ModItems.SEEING_STONE.get())&&item.getCount()==1&&tag!=null
                &&matches(tag,"StoryOwner",scene.owner)&&matches(tag,"StoryDoor",scene.door)
                &&matches(tag,"GhostStoneReceipt",receipt);
    }
    private static boolean authenticStone(VisitData.Scene scene,CompoundTag escape,ItemStack item){
        if(!item.is(ModItems.SEEING_STONE.get())||item.getCount()!=1)return false;
        if(!escape.contains("StoneIssuance")){
            if(escape.hasUUID("StoneReceipt"))return stoneMatches(scene,item,escape.getUUID("StoneReceipt"));
            return escape.getBoolean("StoneObtained")
                    &&(item.getTag()==null||!item.getTag().contains("StoryOwner")||matches(item.getTag(),"StoryOwner",scene.owner))
                    &&(item.getTag()==null||!item.getTag().contains("StoryDoor")||matches(item.getTag(),"StoryDoor",scene.door));
        }
        var issuance=escape.getCompound("StoneIssuance");
        return "TRANSFERRED".equals(issuance.getString("Stage"))&&issuance.hasUUID("Receipt")
                &&matches(issuance,"Owner",scene.owner)&&matches(issuance,"Door",scene.door)
                &&escape.hasUUID("Victim")&&matches(issuance,"Child",escape.getUUID("Victim"))
                &&issuance.hasUUID("Spink")&&issuance.hasUUID("Forcible")&&stoneMatches(scene,item,issuance.getUUID("Receipt"));
    }
    private static com.coraline.secrets.HouseCabinets.Room selectedRoom(String id){for(var room:com.coraline.secrets.HouseCabinets.ROOMS)if(room.id().equals(id))return room;return null;}
    static ItemStack stone(CompoundTag e){return ItemStack.of(e.getCompound("QuestInventory").getCompound("Stone"));}
    static boolean keyAuthentic(ItemStack item,VisitData.Scene s,CompoundTag e){if(item.isEmpty()||!item.hasTag())return false;var tag=item.getTag();return tag.hasUUID("GhostKeyOwner")&&s.owner.equals(tag.getUUID("GhostKeyOwner"))&&tag.hasUUID("GhostKeyDoor")&&s.door.equals(tag.getUUID("GhostKeyDoor"))&&tag.hasUUID("GhostKeyAttempt")&&e.getUUID("Attempt").equals(tag.getUUID("GhostKeyAttempt"));}
    static void hands(com.coraline.tycoon.TycoonNpc child){child.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);child.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);}
    private GhostEscapeCovert(){}
}
