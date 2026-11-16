package com.coraline.feast;

import com.coraline.secrets.SecretsNetwork;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** A small recipient-only presentation snapshot, never a client-written ledger. */
public final class GhostVictimSnapshot {
    public static void send(ServerPlayer p,VisitData.Scene s){
        var n=GhostVictimEntry.data(s);if(!GhostVictimMirrors.owned(p,s,n))return;
        var packet=new CompoundTag();var ghost=new CompoundTag();packet.put("GhostVictim",ghost);
        ghost.putBoolean("Active",true);ghost.putString("Phase",GhostVictimProgress.phase(n).name());ghost.putUUID("Session",n.getUUID("Session"));
        ghost.putDouble("Energy",GhostVictimProgress.energy(n));ghost.putDouble("Attention",GhostVictimProgress.attention(n));
        ghost.putDouble("Deterioration",GhostVictimWorld.deterioration(n));ghost.putBoolean("InsideMirror",GhostVictimProgress.mirror(n));
        var occupied=n.getCompound("Mirrors").getCompound(n.getString("Mirror"));
        if(GhostVictimProgress.mirror(n)&&occupied.hasUUID("Entity"))ghost.putUUID("MirrorEntity",occupied.getUUID("Entity"));
        var child=GhostVictimActors.find(p,s,n);ghost.putInt("VictimEntity",child==null?-1:child.getId());
        if(n.hasUUID("VictimActor"))ghost.putUUID("VictimActor",n.getUUID("VictimActor"));
        ghost.putString("ChildName",GhostVictimProgress.child(n).getString("Name"));ghost.putString("ChildWords",n.getString("ChildWords"));
        ghost.putLong("Utterance",n.getLong("ChildTalkingUntil"));ghost.putInt("TalkingTicks",(int)Math.max(0,n.getLong("ChildTalkingUntil")-GhostVictimProgress.clock(n)));
        String objective=GhostVictimProgress.imprisoned(n)?"The Beldam has trapped you. Wait for your release.":switch(GhostVictimProgress.phase(n)){
            case ENTER_MIRROR->"Enter mirror";
            case EMPTY_WORLD->"The house is gone. Something is waiting.";
            case RECONSTRUCTION,NEW_VICTIM_RESET->"Watch the Other World rebuild.";
            case WAITING_FOR_VICTIM->"Watch the Little Door room.";
            case INTRODUCTION->"A new child has arrived.";
            case FIRST_SUPPER->"Follow the family to supper.";
            case EXPLORATION->"Find a way to warn the child.";
            case BEDTIME->"This may be your last chance to speak tonight.";
            case NIGHT_PREPARATION->"Explore tomorrow's opportunities.";
            case DAILY_ARRIVAL->"The child is returning through the Little Door.";
            case DAILY_GIFT->"Other Mother has prepared a gift.";
            case ACTIVITY,BETWEEN_ACTIVITIES->"Follow the child. Watch for cracks in the illusion.";
            case VICTIM_CAPTURED->"Another child is being claimed.";
            case VICTIM_ESCAPED->"The child is leaving. You are still trapped.";
        };
        ghost.putString("Objective",objective);
        long remaining=GhostVictimProgress.imprisoned(n)?n.getLong("PunishedUntil")-GhostVictimProgress.clock(n):
            GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.NIGHT_PREPARATION?GhostVictimConfig.PREPARATION_SECONDS.get()*20L-GhostVictimProgress.age(n):0;
        ghost.putInt("CountdownTicks",(int)Math.max(0,remaining));var points=new ListTag();
        if(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.ENTER_MIRROR)point(points,GhostVictimEntry.prisonMirror(s).getCenter(),"prison_exit");
        else if(!GhostVictimProgress.imprisoned(n))for(String key:n.getCompound("Mirrors").getAllKeys()){
            var mirror=n.getCompound("Mirrors").getCompound(key);point(points,Vec3.atBottomCenterOf(net.minecraft.core.BlockPos.of(mirror.getLong("Position"))).add(0,1.35,0),"mirror:"+key);
        }
        ghost.put("InteractionPoints",points);SecretsNetwork.send(p,packet);
    }
    private static void point(ListTag all,Vec3 at,String action){var point=new CompoundTag();point.putDouble("X",at.x);point.putDouble("Y",at.y);point.putDouble("Z",at.z);point.putString("Action",action);all.add(point);}
    private GhostVictimSnapshot(){}
}
