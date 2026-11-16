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
            ||!ghost.contains("BedtimeWindowStarted")||GhostVictimProgress.ghostTrust(ghost)<1||GhostVictimProgress.motherTrust(ghost)>=.6
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
                if(!e.contains("ErrandHomeAt"))e.putLong("ErrandHomeAt",clock);
                if(clock-e.getLong("ErrandHomeAt")<100)return true;
                var item=new ItemStack(ModItems.SEEING_STONE.get());item.getOrCreateTag().putUUID("StoryOwner",s.owner);item.getTag().putUUID("StoryDoor",s.door);
                var inventory=e.getCompound("QuestInventory");inventory.put("Stone",item.save(new CompoundTag()));e.put("QuestInventory",inventory);e.putString("StoneSource","SPINK_AND_FORCIBLE_OFFSCREEN_HOME_VISIT");
                GhostEscapeProgress.homeErrand(ghost,true);OtherMotherLoop.changed(p);return false;
            }
            case STONE_REPORT,ROOM_REPORT,KEY_REPORT->{
                if(child.level()!=level||ghost.getBoolean("ChildAtHome")||GhostVictimProgress.phase(ghost)==GhostVictimProgress.Phase.DAILY_ARRIVAL)return false;
                if(GhostVictimRoutines.travel(p,s,ghost,child,GhostVictimWorld.bedroom(s),false)==GhostVictimNavigation.Result.ARRIVED){
                    hands(child);GhostVictimRoutines.lookAtMirror(ghost,child);
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
                int turn=(int)(GhostEscapeProgress.age(e,clock)/50)%4;var direction=net.minecraft.core.Direction.from2DDataValue(turn);var at=center.relative(direction,2);
                if(com.coraline.secrets.HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,at)&&GhostVictimNavigation.clear(level,child,at))GhostVictimRoutines.travel(p,s,ghost,child,at,false);
                child.getLookControl().setLookAt(center.getX()+direction.getStepX()*3+.5,center.getY()+1,center.getZ()+direction.getStepZ()*3+.5,25,25);
                if(GhostEscapeProgress.scanned(ghost,com.coraline.secrets.HouseCabinets.contains(AcceptedButtonEyesWorld.house(s),room,child.blockPosition()))){hands(child);ghost.remove("ChildRoutine");GhostVictimDirector.words(ghost,e.getBoolean("EyeRoomKnown")?"They're here, but I can't open the container.":"I don't see the eyes in here.");}
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
                hands(child);if(child.level()!=level)return true;var outside=GhostEscapeRooms.outside(s,e.getString("AssignedRoom"),level);if(outside==null)return true;
                if(GhostVictimRoutines.travel(p,s,ghost,child,outside,false)==GhostVictimNavigation.Result.ARRIVED&&GhostEscapeRooms.closeAfterLeaving(p,s,ghost,child)){
                    GhostEscapeProgress.leftRoom(ghost,true);e.remove("ReportSpoken");ghost.remove("ChildRoutine");OtherMotherLoop.changed(p);
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
    private static com.coraline.secrets.HouseCabinets.Room selectedRoom(String id){for(var room:com.coraline.secrets.HouseCabinets.ROOMS)if(room.id().equals(id))return room;return null;}
    static ItemStack stone(CompoundTag e){return ItemStack.of(e.getCompound("QuestInventory").getCompound("Stone"));}
    static boolean keyAuthentic(ItemStack item,VisitData.Scene s,CompoundTag e){if(item.isEmpty()||!item.hasTag())return false;var tag=item.getTag();return tag.hasUUID("GhostKeyOwner")&&s.owner.equals(tag.getUUID("GhostKeyOwner"))&&tag.hasUUID("GhostKeyDoor")&&s.door.equals(tag.getUUID("GhostKeyDoor"))&&tag.hasUUID("GhostKeyAttempt")&&e.getUUID("Attempt").equals(tag.getUUID("GhostKeyAttempt"));}
    static void hands(com.coraline.tycoon.TycoonNpc child){child.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);child.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);}
    private GhostEscapeCovert(){}
}
