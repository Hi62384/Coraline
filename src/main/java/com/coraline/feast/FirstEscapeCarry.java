package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** A bounded, floor-supported cinematic route. Never walks through a failed path or a wall. */
final class FirstEscapeCarry {
    static Vec3 feet(ServerPlayer p,BlockPos cell){
        var shape=p.serverLevel().getBlockState(cell).getCollisionShape(p.serverLevel(),cell);
        double offset=shape.isEmpty()?0:shape.max(net.minecraft.core.Direction.Axis.Y);
        return Vec3.atBottomCenterOf(cell).add(0,offset<=.125?offset:0,0);
    }
    static boolean fits(ServerPlayer p,BlockPos cell){
        var l=p.serverLevel();if(!l.hasChunkAt(cell))return false;
        var at=feet(p,cell);var box=new AABB(at.x-.305,at.y+.002,at.z-.305,at.x+.305,at.y+1.84,at.z+.305);
        return l.getBlockCollisions(null,box).iterator().hasNext()==false
            &&l.getEntitiesOfClass(CinematicPropEntity.class,box,prop->prop.canBeCollidedWith()).isEmpty()
            &&l.getBlockState(cell.below()).isFaceSturdy(l,cell.below(),net.minecraft.core.Direction.UP);
    }
    static List<BlockPos> route(ServerPlayer p,BlockPos start,BlockPos goal){
        var pending=new ArrayDeque<BlockPos>();var previous=new HashMap<BlockPos,BlockPos>();
        pending.add(start);previous.put(start,start);BlockPos end=null;
        while(!pending.isEmpty()&&previous.size()<2048){
            var at=pending.removeFirst();if(at.equals(goal)){end=at;break;}
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL){var next=at.relative(direction);
                if(Math.abs(next.getX()-start.getX())>32||Math.abs(next.getZ()-start.getZ())>32||previous.containsKey(next)||!fits(p,next))continue;
                previous.put(next,at);pending.addLast(next);
            }
        }
        if(end==null)return List.of();var result=new ArrayList<BlockPos>();
        while(!end.equals(start)){result.add(end);end=previous.get(end);}Collections.reverse(result);return result;
    }
    static void clear(CompoundTag n){n.remove("CarryRoute");n.remove("CarryNode");n.remove("CarryGoal");n.remove("CarryStillTicks");}
    private static boolean obstructed(ServerPlayer p,CompoundTag n,String reason){
        int ticks=n.getInt("CarryStillTicks")+1;if(ticks>=20)clear(n);else n.putInt("CarryStillTicks",ticks);
        n.putString("Blocker","The route to the hall mirror is obstructed: "+reason);
        FirstEscapeStory.hint(p,"Other Mother cannot reach the mirror: the hallway is blocked.");return false;
    }
    static boolean move(ServerPlayer p,VisitData.Scene s,ParentEntity mother,BlockPos goal){
        var n=FirstEscapeStory.data(s);mother.stopRiding();mother.setNoAi(true);mother.setNoGravity(true);mother.noPhysics=false;mother.getNavigation().stop();mother.setDeltaMovement(Vec3.ZERO);
        if(mother.position().distanceToSqr(feet(p,goal))<.02){clear(n);return true;}
        if(!n.contains("CarryRoute")||n.getLong("CarryGoal")!=goal.asLong()){
            if(n.getLong("CarryRetryAt")>p.serverLevel().getGameTime()){FirstEscapeStory.hint(p,"Other Mother cannot reach the mirror: the hallway is blocked.");return false;}
            var path=route(p,mother.blockPosition(),goal);var saved=new ListTag();
            if(!path.isEmpty()&&fits(p,mother.blockPosition()))saved.add(LongTag.valueOf(mother.blockPosition().asLong()));
            for(var cell:path)saved.add(LongTag.valueOf(cell.asLong()));
            if(path.isEmpty()&&mother.blockPosition().equals(goal)&&fits(p,goal))saved.add(LongTag.valueOf(goal.asLong()));
            n.put("CarryRoute",saved);n.putLong("CarryGoal",goal.asLong());n.putInt("CarryNode",0);
        }
        var path=n.getList("CarryRoute",4);int index=n.getInt("CarryNode");
        if(index>=path.size()){
            // A failed search is retried, not mistaken for arrival. It cannot hang silently.
            n.remove("CarryRoute");n.putLong("CarryRetryAt",p.serverLevel().getGameTime()+20);n.putString("Blocker","The route to the hall mirror is obstructed.");
            FirstEscapeStory.hint(p,"Other Mother cannot reach the mirror: the hallway is blocked.");return false;
        }
        var targetCell=BlockPos.of(((LongTag)path.get(index)).getAsLong());
        // Loading block data alone does not make an entity section accessible.
        // Crossing into a hidden section removes the canonical UUID from lookup
        // until its chunk visibility catches up. Keep both sides ticketed and
        // wait for the real server tick instead of losing the carried actor.
        LoopProps.hold(p.serverLevel(),mother.blockPosition());LoopProps.hold(p.serverLevel(),targetCell);
        if(mother.isAddedToWorld()&&!p.serverLevel().areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(targetCell).toLong()))return false;
        var target=feet(p,targetCell);var delta=target.subtract(mother.position());
        var horizontal=CarryStepGeometry.horizontal(new CarryStepGeometry.Point(mother.getX(),mother.getY(),mother.getZ()),new CarryStepGeometry.Point(target.x,target.y,target.z));
        var floorShapes=new ArrayList<CarryStepGeometry.FloorBox>();
        // Resolve the thin floor covering under the complete footprint. A scripted
        // carry must not inherit navigation's step-up overshoot or gravity lag.
        for(double dx:new double[]{-.2999,.2999})for(double dz:new double[]{-.2999,.2999}){
            var cell=BlockPos.containing(horizontal.x()+dx,goal.getY(),horizontal.z()+dz);
            var shape=p.serverLevel().getBlockState(cell).getCollisionShape(p.serverLevel(),cell);
            for(var local:shape.toAabbs()){
                var covered=local.move(cell);
                floorShapes.add(new CarryStepGeometry.FloorBox(covered.minX,covered.minY,covered.minZ,covered.maxX,covered.maxY,covered.maxZ));
            }
        }
        var grounded=CarryStepGeometry.floor(horizontal,goal.getY(),floorShapes);if(grounded==null)return obstructed(p,n,"unsupported floor at "+horizontal);
        var next=new Vec3(grounded.x(),grounded.y(),grounded.z());var step=next.subtract(mother.position());
        // Test the whole movement, including the carried player's head clearance.
        // Existing saves may have her touching the wall from the old failed path.
        // Do not pad that contact into an overlap that prevents moving away.
        var swept=mother.getBoundingBox().expandTowards(step).deflate(.0001);
        var clearance=new AABB(swept.minX,swept.minY+.13,swept.minZ,swept.maxX,swept.maxY+.02,swept.maxZ);
        if(p.serverLevel().getBlockCollisions(mother,clearance).iterator().hasNext()){
            return obstructed(p,n,"head clearance at "+clearance);
        }
        var footprint=mother.getBoundingBox().move(step).deflate(.0001);
        if(p.serverLevel().getBlockCollisions(mother,footprint).iterator().hasNext())return obstructed(p,n,"body clearance at "+footprint);
        if(!p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,footprint,prop->prop.canBeCollidedWith()).isEmpty())return obstructed(p,n,"furniture clearance at "+footprint);
        n.remove("Blocker");var before=mother.position();
        mother.setPos(next);mother.setOnGround(true);
        if(mother.position().distanceToSqr(before)<.000001){int stalled=n.getInt("CarryStillTicks")+1;n.putInt("CarryStillTicks",stalled);if(stalled>=20){clear(n);n.putString("Blocker","The route to the hall mirror is obstructed.");}}else n.remove("CarryStillTicks");
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));mother.setYRot(yaw);mother.setYBodyRot(yaw);mother.setYHeadRot(yaw);mother.setXRot(0);mother.familyGesture(42,5);
        if(mother.position().distanceToSqr(target)<.0001)n.putInt("CarryNode",index+1);return false;
    }
    private FirstEscapeCarry(){}
}
