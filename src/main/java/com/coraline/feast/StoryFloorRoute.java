package com.coraline.feast;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.*;

/** Short, cached, floor-supported stage routes. A failed search is never arrival. */
final class StoryFloorRoute {
    static Vec3 feet(ServerLevel level,BlockPos cell){
        var shape=level.getBlockState(cell).getCollisionShape(level,cell);
        double cover=shape.isEmpty()?0:shape.max(Direction.Axis.Y);
        return Vec3.atBottomCenterOf(cell).add(0,cover<=.125?cover:0,0);
    }
    static boolean fits(Mob actor,BlockPos cell){
        var level=(ServerLevel)actor.level();if(!level.hasChunkAt(cell)||!level.getBlockState(cell).getFluidState().isEmpty())return false;
        var at=feet(level,cell);var box=actor.getDimensions(actor.getPose()).makeBoundingBox(at).deflate(.001);
        return level.getBlockState(cell.below()).isFaceSturdy(level,cell.below(),Direction.UP)
            &&!level.getBlockCollisions(actor,box).iterator().hasNext()
            &&level.getEntitiesOfClass(CinematicPropEntity.class,box,prop->prop.canBeCollidedWith()).isEmpty();
    }
    static BlockPos beside(Mob actor,Vec3 point,double minimum,double maximum){
        BlockPos best=null;double score=Double.MAX_VALUE;var center=BlockPos.containing(point);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            var cell=center.offset(x,0,z);var at=feet((ServerLevel)actor.level(),cell);double distance=at.subtract(point).multiply(1,0,1).length();
            if(distance<minimum||distance>maximum||!fits(actor,cell))continue;
            double cost=actor.distanceToSqr(at)+distance*distance;
            if(cost<score){score=cost;best=cell;}
        }return best;
    }
    static List<BlockPos> plan(Mob actor,BlockPos goal){
        var start=actor.blockPosition();var pending=new ArrayDeque<BlockPos>();var previous=new HashMap<BlockPos,BlockPos>();
        pending.add(start);previous.put(start,start);
        while(!pending.isEmpty()&&previous.size()<1536){
            var at=pending.removeFirst();if(at.equals(goal)){
                var result=new ArrayList<BlockPos>();while(!at.equals(start)){result.add(at);at=previous.get(at);}Collections.reverse(result);return result;
            }
            for(var direction:Direction.Plane.HORIZONTAL){var next=at.relative(direction);
                if(Math.abs(next.getX()-start.getX())>24||Math.abs(next.getZ()-start.getZ())>24||previous.containsKey(next)||!fits(actor,next))continue;
                previous.put(next,at);pending.addLast(next);
            }
        }return List.of();
    }
    static boolean move(Mob actor,CompoundTag state,String key,BlockPos goal,double speed){
        var level=(ServerLevel)actor.level();var route=state.getCompound(key);state.put(key,route);
        actor.getNavigation().stop();actor.setJumping(false);actor.setDeltaMovement(Vec3.ZERO);actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=false;
        var destination=feet(level,goal);if(actor.distanceToSqr(destination)<.025){route.remove("Nodes");return true;}
        if(!route.contains("Nodes")||route.getLong("Goal")!=goal.asLong()){
            if(route.getLong("Retry")>level.getGameTime())return false;
            var planned=plan(actor,goal);if(planned.isEmpty()){route.putLong("Retry",level.getGameTime()+20);return false;}
            // Center within the starting cell before turning through a narrow doorway.
            if(fits(actor,actor.blockPosition()))planned.add(0,actor.blockPosition());
            route.putLongArray("Nodes",planned.stream().mapToLong(BlockPos::asLong).toArray());route.putInt("Node",0);route.putLong("Goal",goal.asLong());
        }
        var nodes=route.getLongArray("Nodes");int index=route.getInt("Node");if(index>=nodes.length){route.remove("Nodes");return false;}
        var cell=BlockPos.of(nodes[index]);var delta=feet(level,cell).subtract(actor.position());
        if(delta.lengthSqr()<.0001){route.putInt("Node",index+1);return false;}
        var step=delta.normalize().scale(Math.min(speed,delta.length()));
        var raw=actor.getBoundingBox().expandTowards(step).deflate(.001);
        // Floor coverings are handled by feet(); they are not chest-high obstacles.
        var swept=new AABB(raw.minX,raw.minY+.126,raw.minZ,raw.maxX,raw.maxY,raw.maxZ);
        if(level.getBlockCollisions(actor,swept).iterator().hasNext()||!level.getEntitiesOfClass(CinematicPropEntity.class,swept,prop->prop.canBeCollidedWith()).isEmpty()){
            route.remove("Nodes");route.putLong("Retry",level.getGameTime()+20);return false;
        }
        LoopProps.hold(level,cell);actor.setPos(actor.position().add(step));actor.setOnGround(true);state.putInt("StageStall",0);state.remove("Blocker");
        float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));actor.setYRot(yaw);actor.setYBodyRot(yaw);actor.setYHeadRot(yaw);
        return actor.distanceToSqr(destination)<.025;
    }
    private StoryFloorRoute(){}
}
