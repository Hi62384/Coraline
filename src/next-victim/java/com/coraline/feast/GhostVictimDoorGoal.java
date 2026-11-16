package com.coraline.feast;

import com.coraline.tycoon.TycoonNpc;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;

/** Ordinary care retains vanilla door use. The leased story participant may
 * open only ordinary house doors; locked secret rooms need the later explicit
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
        return GhostVictimActors.owns(actor,s,n)&&GhostVictimWorld.ordinaryDoor(s,doorPos);
    }
}
