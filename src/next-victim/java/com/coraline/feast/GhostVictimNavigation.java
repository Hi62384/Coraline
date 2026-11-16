package com.coraline.feast;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bounded physical navigation shared by victim routines and the later escape.
 * Stalls report failure to the owning director; they never teleport an actor
 * through a wall, invent arrival or silently unlock a restricted door. */
public final class GhostVictimNavigation {
    public enum Result { MOVING, ARRIVED, BLOCKED }
    public static boolean clear(ServerLevel level,PathfinderMob actor,BlockPos feet){
        var at=Vec3.atBottomCenterOf(feet);var box=actor.getBoundingBox().move(at.subtract(actor.position()));
        return level.getWorldBorder().isWithinBounds(feet)&&level.getFluidState(feet).isEmpty()
            &&level.getFluidState(feet.above()).isEmpty()&&level.noCollision(actor,box)
            &&!level.getBlockState(feet.below()).getCollisionShape(level,feet.below()).isEmpty();
    }
    public static Result walk(PathfinderMob actor,CompoundTag journal,BlockPos target,long clock,double speed,
                              int limitTicks,Predicate<BlockPos> mayOpenDoor){
        if(!(actor.level() instanceof ServerLevel level)||target==null||limitTicks<20||!Double.isFinite(speed))return Result.BLOCKED;
        if(!journal.contains("Goal")||journal.getLong("Goal")!=target.asLong()){
            journal.putLong("Goal",target.asLong());journal.putLong("Started",clock);journal.putLong("ProgressAt",clock);
            journal.putDouble("PreviousX",actor.getX());journal.putDouble("PreviousZ",actor.getZ());journal.remove("Blocker");
        }
        if(actor.distanceToSqr(Vec3.atBottomCenterOf(target))<=.8*.8){
            actor.getNavigation().stop();actor.setSprinting(false);journal.remove("Blocker");return Result.ARRIVED;
        }
        if(clock-journal.getLong("Started")>=limitTicks){
            actor.getNavigation().stop();actor.setSprinting(false);journal.putString("Blocker","The route is obstructed.");return Result.BLOCKED;
        }
        if(actor.getNavigation() instanceof GroundPathNavigation navigation)navigation.setCanOpenDoors(true);
        actor.setNoAi(false);actor.noPhysics=false;actor.setNoGravity(false);actor.setSprinting(speed>1.1);
        // A normal open-door path is not enough: the actor must actually use the
        // nearby door. The director authenticates ownership and current locks.
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            var at=actor.blockPosition().offset(dx,0,dz);var state=level.getBlockState(at);
            if(state.getBlock() instanceof DoorBlock door&&!state.getValue(DoorBlock.OPEN)
                    &&actor.distanceToSqr(at.getCenter())<3&&mayOpenDoor!=null&&mayOpenDoor.test(at))door.setOpen(actor,level,state,at,true);
        }
        double dx=actor.getX()-journal.getDouble("PreviousX"),dz=actor.getZ()-journal.getDouble("PreviousZ");
        if(dx*dx+dz*dz>.16){journal.putLong("ProgressAt",clock);journal.putDouble("PreviousX",actor.getX());journal.putDouble("PreviousZ",actor.getZ());}
        if(clock%10==0||actor.getNavigation().isDone()){
            var path=actor.getNavigation().createPath(target,0);
            if(path!=null)actor.getNavigation().moveTo(path,Math.max(.25,Math.min(1.6,speed)));
            else if(clock-journal.getLong("ProgressAt")>100){actor.getNavigation().stop();actor.setSprinting(false);journal.putString("Blocker","No safe path is available.");return Result.BLOCKED;}
        }
        return Result.MOVING;
    }
    public static void stop(PathfinderMob actor,CompoundTag route){
        actor.getNavigation().stop();actor.setSprinting(false);route.remove("Goal");
    }
    public static boolean canSee(LivingEntity observer,LivingEntity target,double distance,double fieldOfView){
        if(observer.level()!=target.level()||!observer.isAlive()||!target.isAlive()||!Double.isFinite(distance)||!Double.isFinite(fieldOfView)||distance<=0||fieldOfView<=0)return false;
        Vec3 eye=observer.getEyePosition(),destination=target.getEyePosition(),delta=destination.subtract(eye);
        if(delta.lengthSqr()>distance*distance)return false;
        if(delta.lengthSqr()>.001&&observer.getLookAngle().dot(delta.normalize())<Math.cos(Math.toRadians(Math.min(360,fieldOfView)/2)))return false;
        return observer.level().clip(new ClipContext(eye,destination,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,observer)).getType()==HitResult.Type.MISS;
    }
    private GhostVictimNavigation(){}
}
