package com.coraline.feast;

import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Actual near-player domestic navigation with persisted pauses/investigations.
 * An obstructed route chooses another public destination, never teleports or
 * marks the child as having reached a blocked bedroom or activity entrance. */
public final class GhostVictimRoutines {
    public static void explore(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child){
        if(!GhostVictimActors.owns(child,s,n))return;
        long clock=GhostVictimProgress.clock(n);var routine=n.getCompound("ChildRoutine");
        if(!n.contains("ChildRoutine"))n.put("ChildRoutine",routine);
        if(clock<routine.getLong("PauseUntil")){child.getNavigation().stop();lookAtMirror(n,child);return;}
        BlockPos[] places={GhostVictimWorld.hall(s),GhostVictimWorld.kitchen(s),GhostVictimWorld.supper(s),GhostVictimWorld.bedroom(s)};
        boolean investigating=n.contains("Investigation")&&clock<n.getLong("InvestigateUntil");
        var witnessed=BlockPos.of(n.getLong("Investigation"));
        var target=investigating?new BlockPos(witnessed.getX(),child.blockPosition().getY(),witnessed.getZ()):places[Math.floorMod(routine.getInt("Index")/2,places.length)];
        if(!investigating&&Math.floorMod(routine.getInt("Index"),2)==1){
            var mirror=mirrorStop(n,child,routine.getInt("Index")/2);if(mirror!=null)target=mirror;
        }
        if(investigating&&!GhostVictimNavigation.clear((net.minecraft.server.level.ServerLevel)child.level(),child,target)){
            var approach=GhostVictimNavigation.approach(child,target);if(approach!=null)target=approach;
        }
        var result=GhostVictimNavigation.walk(child,routine,target,clock,.9,400,door->GhostVictimWorld.ordinaryDoor(s,door));
        if(result==GhostVictimNavigation.Result.ARRIVED){
            routine.putLong("PauseUntil",clock+(investigating?80:100+Math.floorMod(child.resident.hashCode()+routine.getInt("Index")*73,201)));
            routine.putInt("Index",routine.getInt("Index")+1);GhostVictimNavigation.stop(child,routine);
            if(investigating){n.remove("Investigation");n.remove("InvestigateUntil");}
        }else if(result==GhostVictimNavigation.Result.BLOCKED){
            routine.putInt("Index",routine.getInt("Index")+1);routine.putLong("PauseUntil",clock+40);
            n.putString("LastRouteBlocker",routine.getString("Blocker"));GhostVictimNavigation.stop(child,routine);
            if(investigating){n.remove("Investigation");n.remove("InvestigateUntil");}
        }
        GhostVictimActors.remember(n,child);
    }
    public static void investigate(CompoundTag n,BlockPos actualWitnessedPoint){
        if(actualWitnessedPoint==null||!GhostVictimProgress.hasVictim(n))return;
        n.putLong("Investigation",actualWitnessedPoint.asLong());n.putLong("InvestigateUntil",GhostVictimProgress.clock(n)+200);
        var routine=n.getCompound("ChildRoutine");routine.remove("Goal");routine.remove("PauseUntil");n.put("ChildRoutine",routine);
    }
    /** Visit real scheduled mirrors from their visible side. Selecting a spot
     * requires a physical path and never counts as arrival or moves the actor. */
    private static BlockPos mirrorStop(CompoundTag n,TycoonNpc child,int index){
        var ids=n.getCompound("Mirrors").getAllKeys().stream()
            .filter(id->GhostVictimMirrors.available(n,id,child.level().dimension().location().toString())).sorted().toList();
        if(ids.isEmpty())return null;
        var mirror=n.getCompound("Mirrors").getCompound(ids.get(Math.floorMod(index,ids.size())));
        var center=net.minecraft.world.phys.Vec3.atBottomCenterOf(BlockPos.of(mirror.getLong("Position")));
        var normal=net.minecraft.world.phys.Vec3.directionFromRotation(0,mirror.getFloat("Yaw")).scale(-1);
        var destination=BlockPos.containing(center.add(normal.scale(2)));
        var at=GhostVictimNavigation.approach(child,destination);if(at==null)return null;
        var feet=net.minecraft.world.phys.Vec3.atBottomCenterOf(at);
        if(feet.subtract(center).dot(normal)<=.1||feet.distanceToSqr(center)>16)return null;
        var face=center.add(0,1.35,0);var eye=feet.add(0,child.getEyeHeight(),0);
        var end=face.subtract(face.subtract(eye).normalize().scale(.16));
        return child.level().clip(new net.minecraft.world.level.ClipContext(eye,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,child)).getType()==net.minecraft.world.phys.HitResult.Type.MISS?at:null;
    }
    public static void lookAtMirror(CompoundTag n,TycoonNpc child){
        for(String id:n.getCompound("Mirrors").getAllKeys()){
            if(!GhostVictimMirrors.available(n,id,child.level().dimension().location().toString()))continue;
            var mirror=n.getCompound("Mirrors").getCompound(id);var at=net.minecraft.world.phys.Vec3.atBottomCenterOf(BlockPos.of(mirror.getLong("Position"))).add(0,1.35,0);
            var normal=net.minecraft.world.phys.Vec3.directionFromRotation(0,mirror.getFloat("Yaw")).scale(-1);
            if(child.distanceToSqr(at)<25&&child.position().subtract(at).dot(normal)>.1){child.getLookControl().setLookAt(at.x,at.y,at.z,30,30);return;}
        }
    }
    public static GhostVictimNavigation.Result travel(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child,BlockPos destination,boolean running){
        return travelAtSpeed(p,s,n,child,destination,running?1.4:1);
    }
    public static GhostVictimNavigation.Result crawl(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child,BlockPos destination){
        return travelAtSpeed(p,s,n,child,destination,.35);
    }
    private static GhostVictimNavigation.Result travelAtSpeed(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child,BlockPos destination,double speed){
        if(!GhostVictimActors.owns(child,s,n))return GhostVictimNavigation.Result.BLOCKED;
        var route=n.getCompound("ChildRoutine");if(!n.contains("ChildRoutine"))n.put("ChildRoutine",route);
        long clock=GhostVictimProgress.clock(n);
        if(!route.contains("RequestedGoal")||route.getLong("RequestedGoal")!=destination.asLong()){
            route.putLong("RequestedGoal",destination.asLong());route.remove("ApproachGoal");route.remove("RecoveryAt");route.remove("Goal");
        }
        var actual=route.contains("ApproachGoal")?BlockPos.of(route.getLong("ApproachGoal")):destination;
        var result=GhostVictimNavigation.walk(child,route,actual,clock,speed,600,door->GhostVictimWorld.ordinaryDoor(s,door));
        if(result==GhostVictimNavigation.Result.BLOCKED&&(!route.contains("RecoveryAt")||clock-route.getLong("RecoveryAt")>=100)){
            route.putLong("RecoveryAt",clock);n.putString("LastRouteBlocker",route.getString("Blocker"));
            // Crawl endpoints must remain exact: an adjacent room is not the
            // far side of the Little Door. Standing props allow a safe approach.
            var approach=child.getPose()==net.minecraft.world.entity.Pose.STANDING?GhostVictimNavigation.approach(child,destination):destination;
            if(approach!=null){route.putLong("ApproachGoal",approach.asLong());GhostVictimNavigation.stop(child,route);}
        }
        GhostVictimActors.remember(n,child);return result;
    }
    private GhostVictimRoutines(){}
}
