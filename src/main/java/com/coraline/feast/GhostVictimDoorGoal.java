package com.coraline.feast;

import com.coraline.tycoon.TycoonNpc;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;

/** Ordinary care retains vanilla door use. The leased story participant may
 * open ordinary doors only in its registered house, venue or real-home region;
 * locked secret rooms need explicit
 * lockpicking authorization. Pathfinding's open-door flag alone grants none. */
public final class GhostVictimDoorGoal extends OpenDoorGoal {
    private final TycoonNpc actor;
    public GhostVictimDoorGoal(TycoonNpc actor){super(actor,true);this.actor=actor;}
    @Override public boolean canUse(){return super.canUse()&&authorized();}
    @Override public boolean canContinueToUse(){return authorized()&&super.canContinueToUse();}
    private boolean authorized(){
        if(!actor.getPersistentData().contains(GhostVictimActors.LEASE))return true;
        var lease=actor.getPersistentData().getCompound(GhostVictimActors.LEASE);
        if(actor.getServer()==null||!lease.hasUUID("Owner"))return false;
        var s=VisitData.get(actor.getServer()).scenes.get(lease.getUUID("Owner"));
        if(s==null)return false;var n=AcceptedButtonEyes.data(s).getCompound(GhostVictimProgress.KEY);
        if(!GhostVictimActors.owns(actor,s,n))return false;
        if(com.coraline.otherworld.OtherWorld.isOther(actor.level()))return GhostVictimWorld.ordinaryDoor(s,doorPos);
        if(ActivityWorld.isActivity(actor.level())){
            var outing=n.getCompound("ParticipantActivity");return outing.contains("Center")&&doorPos.distSqr(net.minecraft.core.BlockPos.of(outing.getLong("Center")))<=200*200;
        }
        var source=com.coraline.otherworld.DoorData.get(actor.getServer()).doors.get(s.door);
        return source!=null&&s.owner.equals(source.owner)&&actor.level().dimension().location().toString().equals(source.dimension)&&doorPos.distSqr(source.pos)<=64*64;
    }
}
