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
        if(clock<routine.getLong("PauseUntil")){child.getNavigation().stop();return;}
        BlockPos[] places={GhostVictimWorld.hall(s),GhostVictimWorld.kitchen(s),GhostVictimWorld.supper(s),GhostVictimWorld.bedroom(s)};
        boolean investigating=n.contains("Investigation")&&clock<n.getLong("InvestigateUntil");
        var target=investigating?BlockPos.of(n.getLong("Investigation")):places[Math.floorMod(routine.getInt("Index"),places.length)];
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
    public static GhostVictimNavigation.Result travel(ServerPlayer p,VisitData.Scene s,CompoundTag n,TycoonNpc child,BlockPos destination,boolean running){
        if(!GhostVictimActors.owns(child,s,n))return GhostVictimNavigation.Result.BLOCKED;
        var route=n.getCompound("ChildRoutine");if(!n.contains("ChildRoutine"))n.put("ChildRoutine",route);
        var result=GhostVictimNavigation.walk(child,route,destination,GhostVictimProgress.clock(n),running?1.4:1,600,door->GhostVictimWorld.ordinaryDoor(s,door));
        GhostVictimActors.remember(n,child);return result;
    }
    private GhostVictimRoutines(){}
}
