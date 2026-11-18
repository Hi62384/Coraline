package com.coraline.feast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Reconciles the one-time accepted-story escrow and durable rescued-child
 * agency receipt only after the real player is back in the tracked home world. */
public final class GhostEscapeRestoration {
    public static boolean tick(ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var e=ghost.getCompound(GhostEscapeProgress.KEY);if(!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,e))return false;
        if(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.COMPLETE){
            if(!e.getBoolean("RoomStorageRetired")&&p.tickCount%100==0&&GhostEscapeRooms.retire(p,s,ghost)){
                e.putBoolean("RoomStorageRetired",true);
                OtherMotherLoop.changed(p);
            }
            GhostEscapeEnding.tick(p,s,ghost);
            return true;
        }
        var door=com.coraline.otherworld.DoorData.get(p.server).doors.get(s.door);
        if(door==null||!s.owner.equals(door.owner))return false;
        var home=com.coraline.parents.FamilyData.level(p.server,door.dimension);if(home==null)return false;
        var phase=GhostEscapeProgress.phase(e);
        if(p.serverLevel()!=home&&(phase==GhostEscapeProgress.Phase.SAFE||phase==GhostEscapeProgress.Phase.RESTORING)){
            var child=GhostVictimActors.find(p,s,ghost);
            if(child==null||child.level()!=home)return true;
            LoopProps.hold(home,door.pos);
            var state=home.getBlockState(door.pos);
            if(!state.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())
                    ||!(home.getBlockEntity(door.pos) instanceof com.coraline.otherworld.DoorBlockEntity tile)
                    ||!s.door.equals(tile.doorId))return true;
            var room=door.pos.relative(state.getValue(com.coraline.block.TinyDoorBlock.FACING),2);
            var at=e.contains("HomeLanding")?net.minecraft.core.BlockPos.of(e.getLong("HomeLanding")):room;
            if(at.distSqr(room)>64)at=room;
            var landing=net.minecraft.world.phys.Vec3.atBottomCenterOf(at);
            if(!RescueEpilogue.safeReturn(home,p,net.minecraft.world.entity.Pose.STANDING,landing)){
                landing=RescueEpilogue.nearReturn(home,p,net.minecraft.world.entity.Pose.STANDING,room,4);
                if(landing==null)return true;
            }
            p.setCamera(p);p.stopRiding();p.setForcedPose(null);
            p.teleportTo(home,landing.x,landing.y,landing.z,p.getYRot(),p.getXRot());
            if(p.serverLevel()!=home||p.position().distanceToSqr(landing)>.25)return true;
            p.setDeltaMovement(0,0,0);
        }
        if(p.serverLevel()!=home)return false;
        if(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.SAFE||GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.RESTORING){
            var snapshot=AcceptedButtonEyesProgress.data(s).getCompound("InventoryEscrow");
            if(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.SAFE){
                if(!GhostEscapeProgress.beginRestoration(ghost,snapshot,true))return true;
                e.putBoolean("RestorationStarted",true);
            }
            if(!AcceptedButtonEyesInventory.restoreAfterGhostEscape(p,s))return true;
            String saved=ghost.getString("BodyGameMode");if(saved.isBlank())saved=GhostVictimEntry.data(s).getString("BodyGameMode");
            GameType original=null;for(GameType type:GameType.values())if(type.getName().equals(saved)){original=type;break;}
            if(original==null){e.putString("Blocker","The original game mode is unavailable in this save; inventory remains safely restored.");return true;}
            if(p.gameMode.getGameModeForPlayer()!=original)p.setGameMode(original);p.setCamera(p);p.stopRiding();p.setForcedPose(null);p.fallDistance=0;
            if(!AcceptedButtonEyesInventory.restorationSaved(p,s)){
                e.putString("Blocker","The rescued player's original game mode is waiting for its saved-player checkpoint; the rescue remains recoverable.");return true;
            }
            if(!GhostEscapeProgress.restorationCommitted(ghost,snapshot.getUUID("Transaction"),true,true))return true;
            if(!GhostEscapeEyes.prepareFarewell(p,s))return true;
            OtherMotherLoop.changed(p);return true;
        }
        if(GhostEscapeProgress.phase(e)==GhostEscapeProgress.Phase.FAREWELL){
            if(!GhostEscapeEyes.prepareFarewell(p,s))return true;
            var child=GhostVictimActors.find(p,s,ghost);if(child!=null&&child.level()==home){
                if(e.getBoolean("ChildDepartureWitnessed")){
                    finishFarewell(p,s,ghost,child);
                    return true;
                }
                child.setNoAi(false);child.noPhysics=false;child.setNoGravity(false);
                if(!e.getBoolean("FarewellStarted")){
                    GhostVictimDirector.words(ghost,"We made it. I'll find my way home. You can find me at the agency if you want me to join your family.");
                    e.putBoolean("FarewellStarted",true);e.putLong("FarewellUntil",ghost.getLong("ChildTalkingUntil"));OtherMotherLoop.changed(p);
                }
                if(GhostVictimProgress.clock(ghost)<e.getLong("FarewellUntil")){child.getNavigation().stop();child.getLookControl().setLookAt(p,30,30);return true;}
                var source=com.coraline.otherworld.DoorData.get(p.server).doors.get(s.door);var doorState=home.getBlockState(source.pos);
                if(e.getInt("DepartureLayoutVersion")<1){e.remove("DepartureTarget");e.putInt("DepartureLayoutVersion",1);}
                if(!e.contains("DepartureTarget")){
                    if(!doorState.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())){e.putString("Blocker","The original Little Door is unavailable for the child's departure.");return true;}
                    var target=departureTarget(child,source.pos,doorState.getValue(com.coraline.block.TinyDoorBlock.FACING));
                    if(target==null){e.putString("Blocker","The home route away from the Little Door is obstructed.");return true;}
                    e.putLong("DepartureTarget",target.asLong());
                }
                var target=net.minecraft.core.BlockPos.of(e.getLong("DepartureTarget"));
                var departure=GhostVictimRoutines.travel(p,s,ghost,child,target,false);
                if(departure==GhostVictimNavigation.Result.BLOCKED){
                    e.remove("DepartureTarget");ghost.remove("ChildRoutine");
                    OtherMotherLoop.changed(p);
                    return true;
                }
                if(departure!=GhostVictimNavigation.Result.ARRIVED)return true;
                if(child.distanceToSqr(source.pos.getCenter())>36){
                    if(GhostEscapeProgress.childDeparture(ghost,true))finishFarewell(p,s,ghost,child);
                    return true;
                }
            }
            return true;
        }
        return false;
    }
    private static void finishFarewell(ServerPlayer p,VisitData.Scene scene,CompoundTag ghost,com.coraline.tycoon.TycoonNpc child){
        if(!com.coraline.tycoon.GhostRescueCandidates.register(p,scene))return;
        ShowTravel.forgetDiscarded(p,scene,child,"GhostEscapeChild");
        if(GhostEscapeProgress.finishFarewell(ghost,true,true))OtherMotherLoop.changed(p);
    }
    static net.minecraft.core.BlockPos departureTarget(com.coraline.tycoon.TycoonNpc child,net.minecraft.core.BlockPos door,net.minecraft.core.Direction facing){
        var side=facing.getClockWise();
        for(int distance:new int[]{8,10,12})for(int lateral:new int[]{0,3,-3,6,-6}){
            var desired=door.relative(facing,distance).relative(side,lateral);var at=GhostVictimNavigation.approach(child,desired);
            if(at!=null&&at.distSqr(door)>=64&&(at.getX()-door.getX())*facing.getStepX()+(at.getZ()-door.getZ())*facing.getStepZ()>=2)return at;
        }
        return null;
    }
    private GhostEscapeRestoration(){}
}
