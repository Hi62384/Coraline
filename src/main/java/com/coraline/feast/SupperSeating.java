package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import net.minecraft.world.phys.Vec3;

/** Walk to an assigned chair, recovering only when the walk stalls or takes too long. */
final class SupperSeating {
    private static final String KEY="CoralineSupperApproach";

    static void approach(ParentEntity parent,ScenePropEntity chair){
        if(parent.getVehicle()==chair){parent.getNavigation().stop();parent.getPersistentData().remove(KEY);return;}
        if(chair.isVehicle())return;
        if(parent.isPassenger())parent.stopRiding(); // Also repairs a saved father still on his piano.
        long now=parent.level().getGameTime();double distance=parent.distanceToSqr(chair);
        var data=parent.getPersistentData().getCompound(KEY);
        if(!data.hasUUID("Chair")||!data.getUUID("Chair").equals(chair.getUUID())){
            data.putUUID("Chair",chair.getUUID());data.putLong("Started",now);data.putLong("Progress",now);data.putDouble("Distance",distance);
        }
        if(distance<data.getDouble("Distance")-.25){data.putDouble("Distance",distance);data.putLong("Progress",now);}
        parent.getPersistentData().put(KEY,data);
        boolean stalled=now-data.getLong("Progress")>=VisitConfig.SEAT_STALL_TICKS.get();
        boolean overdue=now-data.getLong("Started")>=VisitConfig.SEAT_TRAVEL_TICKS.get();
        if(distance<2.25||stalled||overdue){
            // Never recover into solid blocks, fluid, another occupant, or a different player's seat.
            Vec3 destination=new Vec3(chair.getX(),chair.getY()+.1,chair.getZ());
            var bounds=parent.getBoundingBox().move(destination.subtract(parent.position()));
            if(parent.level().noCollision(parent,bounds)&&!parent.level().containsAnyLiquid(bounds)){
                if(parent.startRiding(chair,true)){
                    parent.setPos(destination);parent.setDeltaMovement(Vec3.ZERO);parent.getNavigation().stop();
                    parent.getPersistentData().remove(KEY);return;
                }
            }
        }
        // Let navigation make progress between retries instead of resetting its path every half second.
        if(parent.getNavigation().isDone()||parent.tickCount%40==0)
            parent.getNavigation().moveTo(chair.getX(),chair.getY(),chair.getZ(),1);
    }
}
