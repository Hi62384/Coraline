package com.coraline.feast;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;

/** Cached floor route for moving scenery, including a chair's carried player.
 * Never uses a direct line through a wall or a roof-height terrain fallback. */
final class CinematicFloorRoute {
    static boolean fits(ServerPlayer p,VisitData.Scene s,CinematicPropEntity actor,BlockPos cell){
        if(!FirstEscapeScene.inRoom(s,cell))return false;
        var feet=PartyFloor.at(p,cell);if(feet==null)return false;
        double radius=actor.isVehicle()?.95:.42;var box=new AABB(feet.x-radius,feet.y+.13,feet.z-radius,feet.x+radius,feet.y+2.1,feet.z+radius);
        if(p.serverLevel().getBlockCollisions(actor,box).iterator().hasNext())return false;
        // Only solid furniture is an obstacle; decorative lamps and light
        // entities must not turn every square of the set into an impassable cell.
        return p.serverLevel().getEntitiesOfClass(CinematicPropEntity.class,box,e->e!=actor&&e.canBeCollidedWith()).isEmpty();
    }
    static List<BlockPos> plan(ServerPlayer p,VisitData.Scene s,CinematicPropEntity actor,BlockPos goal){
        var start=actor.blockPosition();var queue=new ArrayDeque<BlockPos>();var previous=new HashMap<BlockPos,BlockPos>();queue.add(start);previous.put(start,start);
        while(!queue.isEmpty()&&previous.size()<512){var cell=queue.removeFirst();if(cell.equals(goal)){var path=new ArrayList<BlockPos>();while(!cell.equals(start)){path.add(cell);cell=previous.get(cell);}Collections.reverse(path);return path;}
            for(var dir:Direction.Plane.HORIZONTAL){var next=cell.relative(dir);if(!previous.containsKey(next)&&fits(p,s,actor,next)){previous.put(next,cell);queue.addLast(next);}}}
        return List.of();
    }
    static BlockPos nearest(ServerPlayer p,VisitData.Scene s,CinematicPropEntity actor,Vec3 target){
        BlockPos best=null;double score=Double.MAX_VALUE;var center=BlockPos.containing(target);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var at=center.offset(x,0,z);if(!fits(p,s,actor,at))continue;var path=plan(p,s,actor,at);if(path.isEmpty()&&!at.equals(actor.blockPosition()))continue;double cost=at.getCenter().distanceToSqr(target)+path.size()*.025;if(cost<score){score=cost;best=at;}}
        return best;
    }
    static boolean move(ServerPlayer p,VisitData.Scene s,CinematicPropEntity actor,CompoundTag state,BlockPos goal,double speed){
        if(goal==null)return false;var feet=PartyFloor.at(p,goal);if(feet==null)return false;
        if(actor.position().distanceToSqr(feet)<.025)return true;
        if(!state.contains("Nodes")||state.getLong("Goal")!=goal.asLong()){
            if(state.getLong("Retry")>p.serverLevel().getGameTime())return false;var path=plan(p,s,actor,goal);
            if(path.isEmpty()){state.putLong("Retry",p.serverLevel().getGameTime()+20);return false;}
            if(fits(p,s,actor,actor.blockPosition()))path.add(0,actor.blockPosition());state.putLongArray("Nodes",path.stream().mapToLong(BlockPos::asLong).toArray());state.putLong("Goal",goal.asLong());state.putInt("Node",0);
        }
        var nodes=state.getLongArray("Nodes");int index=state.getInt("Node");if(index>=nodes.length){state.remove("Nodes");return false;}
        var at=PartyFloor.at(p,BlockPos.of(nodes[index]));if(at==null){state.remove("Nodes");return false;}
        var delta=at.subtract(actor.position());if(delta.lengthSqr()<.0001){state.putInt("Node",index+1);return false;}
        var step=delta.normalize().scale(Math.min(speed,delta.length()));var swept=actor.getBoundingBox().expandTowards(step).deflate(.001);
        double padding=actor.isVehicle()?.55:0;var clearance=new AABB(swept.minX-padding,swept.minY+.13,swept.minZ-padding,swept.maxX+padding,swept.minY+2.1,swept.maxZ+padding);
        if(p.serverLevel().getBlockCollisions(actor,clearance).iterator().hasNext()){state.remove("Nodes");state.putLong("Retry",p.serverLevel().getGameTime()+20);return false;}
        LoopProps.hold(p.serverLevel(),BlockPos.of(nodes[index]));actor.setPos(actor.position().add(step));actor.setDeltaMovement(Vec3.ZERO);
        return actor.position().distanceToSqr(feet)<.025;
    }
    private CinematicFloorRoute(){}
}
