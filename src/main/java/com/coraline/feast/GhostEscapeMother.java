package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Physical patrol/search controller. Suspicion changes where she looks;
 * detection still requires actual sight and capture still requires proximity. */
public final class GhostEscapeMother {
    public static boolean hunting(CompoundTag escape){return GhostEscapeProgress.hunting(escape);}
    public static boolean control(ParentEntity mother,ServerPlayer p,VisitData.Scene s,CompoundTag ghost){
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);if(!mother.isMom()||!mother.getUUID().equals(s.mom)||!GhostVictimMirrors.owned(p,s,ghost)||!GhostEscapeProgress.owns(ghost,escape)
            ||!GhostEscapeProgress.active(escape)||mother.level()!=AcceptedButtonEyesWorld.level(p))return false;
        var phase=GhostEscapeProgress.phase(escape);long clock=GhostVictimProgress.clock(ghost);var child=GhostVictimActors.find(p,s,ghost);
        mother.setNoAi(false);mother.noPhysics=false;mother.setNoGravity(false);mother.eventGaze(true);
        if(java.util.Set.of(GhostEscapeProgress.Phase.SAFE,GhostEscapeProgress.Phase.RESTORING,GhostEscapeProgress.Phase.FAREWELL).contains(phase)){mother.getNavigation().stop();mother.setSprinting(false);return true;}
        if(phase==GhostEscapeProgress.Phase.APPROACH_EXIT){
            var realm=AcceptedButtonEyesWorld.realm(p,s);if(realm==null||realm.doorPos==null)return true;
            var route=escape.getCompound("MotherInterceptionRoute");escape.put("MotherInterceptionRoute",route);
            BlockPos target=route.contains("InterceptionAt")?BlockPos.of(route.getLong("InterceptionAt")):null;
            if(target==null||!GhostVictimNavigation.clear((net.minecraft.server.level.ServerLevel)mother.level(),mother,target)){
                target=interceptionApproach(mother,realm.doorPos);
                if(target==null){escape.putString("Blocker","Other Mother needs a clear physical approach to the Little Door confrontation.");mother.getNavigation().stop();return true;}
                route.putLong("InterceptionAt",target.asLong());GhostVictimNavigation.stop(mother,route);
            }
            var result=walk(mother,s,route,target,clock,1.1);mother.familyGesture(1,5);
            if(child!=null&&child.level()==mother.level())mother.getLookControl().setLookAt(child,30,30);
            if(result==GhostVictimNavigation.Result.BLOCKED){
                escape.putString("Blocker","Other Mother's route to the confrontation is obstructed.");
                if(!route.contains("RetryAt")||clock>=route.getLong("RetryAt")){route.putLong("RetryAt",clock+100);route.remove("InterceptionAt");GhostVictimNavigation.stop(mother,route);}
            }
            return true;
        }
        if(phase==GhostEscapeProgress.Phase.INTERCEPT||phase==GhostEscapeProgress.Phase.WAGER){
            // Give the child the agreed physical head start. Falling through
            // to the bedtime/follow routine let Mother follow it into cover.
            mother.getNavigation().stop();mother.setSprinting(false);mother.familyGesture(1,5);
            if(phase==GhostEscapeProgress.Phase.INTERCEPT)mother.getLookControl().setLookAt(p,30,30);
            else{var realm=AcceptedButtonEyesWorld.realm(p,s);if(realm!=null&&realm.doorPos!=null){var at=realm.doorPos.getCenter();mother.getLookControl().setLookAt(at.x,at.y,at.z,30,30);}}
            return true;
        }
        boolean hunt=hunting(escape);mother.familyGesture(hunt?82:1,5);
        if(hunt&&child!=null&&child.level()==mother.level()&&GhostVictimNavigation.canSee(mother,child,24,150)){
            escape.putLong("LastKnownChild",child.blockPosition().asLong());escape.putLong("PursuitUntil",clock+100);mother.getLookControl().setLookAt(child,40,40);
        }
        var route=escape.getCompound("MotherPatrol");escape.put("MotherPatrol",route);
        if(hunt&&escape.contains("LastKnownChild")&&clock<escape.getLong("PursuitUntil")){
            walk(mother,s,route,BlockPos.of(escape.getLong("LastKnownChild")),clock,1.5);return true;
        }
        if(ghost.contains("MotherInvestigation")&&clock<ghost.getLong("MotherInvestigateUntil")){
            var fixture=BlockPos.of(ghost.getLong("MotherInvestigation"));
            var target=escape.contains("AmbushLamp",Tag.TAG_LONG)&&escape.getLong("AmbushLamp")==fixture.asLong()
                    &&escape.contains("AmbushInvestigation",Tag.TAG_LONG)
                    ?BlockPos.of(escape.getLong("AmbushInvestigation")):standingNear(mother,s,fixture);
            if(target!=null){
                walk(mother,s,route,target,clock,hunt?1.3:1);
                var focus=fixture.getCenter();
                mother.getLookControl().setLookAt(focus.x,focus.y,focus.z,25,25);
                return true;
            }
        }
        if(clock<route.getLong("PauseUntil")){mother.getNavigation().stop();mother.setSprinting(false);return true;}
        if(!route.contains("PatrolTarget")){
            long seed=escape.getUUID("Attempt").getLeastSignificantBits()^route.getInt("Sequence")*0x9E3779B97F4A7C15L;
            var choices=new java.util.ArrayList<BlockPos>(java.util.List.of(GhostVictimWorld.hall(s),GhostVictimWorld.kitchen(s),GhostVictimWorld.supper(s),GhostVictimWorld.bedroom(s)));
            if(GhostEscapeProgress.suspicion(escape)>=.5)for(var room:com.coraline.secrets.HouseCabinets.ROOMS){var at=GhostEscapeRooms.outside(s,room.id(),(net.minecraft.server.level.ServerLevel)mother.level());if(at!=null)choices.add(at);}
            if(hunt)for(Tag entry:escape.getList("HidingNodes",Tag.TAG_COMPOUND)){var node=(CompoundTag)entry;if(!node.getBoolean("Compromised"))choices.add(BlockPos.of(node.getLong("Position")));}
            choices.removeIf(at->route.contains("PreviousTarget")&&at.asLong()==route.getLong("PreviousTarget"));
            if(choices.isEmpty())return true;var target=choices.get(Math.floorMod(Long.hashCode(seed),choices.size()));route.putLong("PatrolTarget",target.asLong());route.putInt("Sequence",route.getInt("Sequence")+1);
        }
        var target=BlockPos.of(route.getLong("PatrolTarget"));var result=walk(mother,s,route,target,clock,hunt?1.35:.9);
        if(result!=GhostVictimNavigation.Result.MOVING){
            if(hunt&&result==GhostVictimNavigation.Result.ARRIVED)for(Tag entry:escape.getList("HidingNodes",Tag.TAG_COMPOUND)){var node=(CompoundTag)entry;if(BlockPos.of(node.getLong("Position")).distSqr(mother.blockPosition())<3){node.putBoolean("Compromised",true);node.putLong("CompromisedAt",clock);escape.putString("SearchedHide",node.getString("Id"));escape.putLong("SearchedAt",clock);mother.getLookControl().setLookAt(target.getX()+.5,target.getY()+1,target.getZ()+.5,40,40);}}
            route.putLong("PreviousTarget",target.asLong());route.remove("PatrolTarget");route.putLong("PauseUntil",clock+(hunt?30:100));GhostVictimNavigation.stop(mother,route);
        }
        if(!hunt&&phase==GhostEscapeProgress.Phase.READY&&mother.tickCount%20==0)GhostEscapeProgress.ordinarySecond(escape);
        return true;
    }
    private static GhostVictimNavigation.Result walk(ParentEntity mother,VisitData.Scene s,CompoundTag route,BlockPos at,long clock,double speed){return GhostVictimNavigation.walk(mother,route,at,clock,speed,400,door->GhostVictimWorld.ordinaryDoor(s,door));}
    /** Stay on the room side, beside the child's crawl line. Only an actual
     * reachable standing position may stage the mandatory interception. */
    static BlockPos interceptionApproach(ParentEntity mother,BlockPos door){
        if(!(mother.level() instanceof net.minecraft.server.level.ServerLevel level))return null;
        var state=level.getBlockState(door);if(!state.hasProperty(com.coraline.block.TinyDoorBlock.FACING))return null;
        var facing=state.getValue(com.coraline.block.TinyDoorBlock.FACING);var side=facing.getClockWise();
        for(int distance:new int[]{2,3,1})for(int lateral:new int[]{1,-1,2,-2}){
            var candidate=door.relative(facing,distance).relative(side,lateral);
            if(!GhostVictimNavigation.clear(level,mother,candidate))continue;
            var path=mother.getNavigation().createPath(candidate,0);if(path!=null&&path.canReach())return candidate;
        }
        return null;
    }
    static BlockPos standingNear(ParentEntity mother,VisitData.Scene s,BlockPos point){
        int floor=GhostVictimWorld.at(s,26,-1,20).getY();for(int y:new int[]{6,15,24}){int candidate=GhostVictimWorld.at(s,34,y,35).getY();if(candidate<=point.getY()&&candidate>floor)floor=candidate;}
        return GhostVictimNavigation.approach(mother,new BlockPos(point.getX(),floor,point.getZ()));
    }
    private GhostEscapeMother(){}
}
