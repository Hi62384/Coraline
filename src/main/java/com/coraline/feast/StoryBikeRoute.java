package com.coraline.feast;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;

/** Short, loaded-terrain routes for the full bicycle and mounted rider, not a walker. */
final class StoryBikeRoute {
    private record Node(BlockPos cell,Vec3 feet,Node previous,int distance){}
    static float yaw(Vec3 delta){return (float)Math.toDegrees(Math.atan2(-delta.x,delta.z));}
    static AABB bounds(Vec3 at,float yaw){double a=Math.toRadians(yaw),x=Math.abs(Math.cos(a))*.45+Math.abs(Math.sin(a))*1.03,z=Math.abs(Math.sin(a))*.45+Math.abs(Math.cos(a))*1.03;return new AABB(at.x-x,at.y+.002,at.z-z,at.x+x,at.y+2.25,at.z+z);}
    static Vec3 floor(WybieBicycleEntity bike,double x,double z,double around,float yaw){
        var l=bike.level();var column=BlockPos.containing(x,around,z);if(!l.hasChunkAt(column))return null;
        var footprint=bounds(new Vec3(x,around,z),yaw);var heights=new TreeSet<Double>(Comparator.reverseOrder());
        // A front wheel reaches a one-block bank BEFORE the bike's centre does.
        // Testing only the centre's floor makes a perfectly rideable step an
        // impassable wall for a two-block-long bicycle.
        for(int bx=(int)Math.floor(footprint.minX);bx<=(int)Math.floor(footprint.maxX);bx++)for(int bz=(int)Math.floor(footprint.minZ);bz<=(int)Math.floor(footprint.maxZ);bz++){
            var cell=new BlockPos(bx,(int)Math.floor(around),bz);if(!l.hasChunkAt(cell)||!l.getWorldBorder().isWithinBounds(cell))return null;
            for(int y=(int)Math.floor(around)+1;y>=(int)Math.floor(around)-2;y--){
                var support=new BlockPos(bx,y,bz);var state=l.getBlockState(support);if(state.is(net.minecraft.tags.BlockTags.LEAVES)||!l.getFluidState(support).isEmpty())continue;
                var shape=state.getCollisionShape(l,support);if(shape.isEmpty())continue;double top=y+shape.max(Direction.Axis.Y);
                if(top<=around+1.05&&top>=around-1.05)heights.add(top);
            }
        }
        for(double top:heights){var feet=new Vec3(x,top,z);var body=bounds(feet,yaw);
            if(!l.getFluidState(BlockPos.containing(feet)).isEmpty()||l.containsAnyLiquid(body)||l.getBlockCollisions(bike,body).iterator().hasNext())continue;
            // Do not turn a nearby high surface into permission to fly over a
            // hole. At least one wheel must have real, dry support underneath.
            boolean supported=false;double angle=Math.toRadians(yaw);
            for(double wheel:new double[]{-1.02,-.72,0,.72,1.02}){
                var at=BlockPos.containing(x-Math.sin(angle)*wheel,top-.01,z+Math.cos(angle)*wheel);var state=l.getBlockState(at);
                var shape=state.getCollisionShape(l,at);
                if(l.getFluidState(at).isEmpty()&&!state.is(net.minecraft.tags.BlockTags.LEAVES)&&!shape.isEmpty()&&Math.abs(at.getY()+shape.max(Direction.Axis.Y)-top)<.02){supported=true;break;}
            }
            if(supported)return feet;
        }return null;
    }
    static Vec3 step(WybieBicycleEntity bike,Vec3 from,double x,double z,float heading){
        var to=floor(bike,x,z,from.y,heading);if(to==null)return null;double high=Math.max(from.y,to.y);
        // Step up, sweep horizontally, then settle down. Test the entire rider,
        // including turns, instead of teleporting through a trunk or low roof.
        for(int i=0;i<=8;i++){double t=i/8.;var at=new Vec3(from.x+(to.x-from.x)*t,high,from.z+(to.z-from.z)*t);if(bike.level().getBlockCollisions(bike,bounds(at,heading)).iterator().hasNext())return null;}
        for(int i=1;i<=4;i++){double t=i/4.;var at=to.y>=from.y?new Vec3(from.x,from.y+(to.y-from.y)*t,from.z):new Vec3(to.x,from.y+(to.y-from.y)*t,to.z);if(bike.level().getBlockCollisions(bike,bounds(at,heading)).iterator().hasNext())return null;}
        return to;
    }
    static Vec3 spawn(WybieBicycleEntity bike,Vec3 rider,ServerPlayer viewer){
        var away=rider.subtract(viewer.position());float heading=yaw(away);
        for(int radius=0;radius<=6;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int turn:new int[]{0,1,-1,2}){float angle=heading+turn*90;var at=floor(bike,rider.x+dx,rider.z+dz,rider.y,angle);if(at==null)continue;
                var forward=new Vec3(-Math.sin(Math.toRadians(angle)),0,Math.cos(Math.toRadians(angle)));
                if(step(bike,at,at.x+forward.x,at.z+forward.z,angle)!=null){bike.setYRot(angle);return at;}
            }
        }
        return rider; // Bounded stage fallback handles a genuinely enclosed site.
    }
    static long[] plan(WybieBicycleEntity bike,ServerPlayer viewer){
        var start=new Node(bike.blockPosition(),bike.position(),null,0);var best=start;
        var open=new PriorityQueue<Node>(Comparator.comparingDouble(n->n.distance-2*Math.sqrt(n.feet.distanceToSqr(viewer.position()))));open.add(start);
        var seen=new HashSet<BlockPos>();seen.add(start.cell);long deadline=System.nanoTime()+4_000_000L;int expanded=0;
        // Always finish at least the starting expansion. On a cold/slow client
        // its collision queries alone can consume the entire time budget. The
        // old loop then threw away every valid neighbour without ever riding.
        while(!open.isEmpty()&&expanded<64&&(expanded==0||System.nanoTime()<deadline)){
            expanded++;
            var node=open.remove();if(node.feet.distanceToSqr(viewer.position())>best.feet.distanceToSqr(viewer.position()))best=node;
            if(node.feet.distanceToSqr(viewer.position())>40*40)break;
            for(var direction:Direction.Plane.HORIZONTAL){var cell=node.cell.relative(direction);if(seen.contains(cell)||cell.distSqr(start.cell)>32*32)continue;
                float yaw=direction.toYRot();var feet=step(bike,node.feet,cell.getX()+.5,cell.getZ()+.5,yaw);if(feet==null)continue;
                cell=BlockPos.containing(feet);if(!seen.add(cell))continue;var next=new Node(cell,feet,node,node.distance+1);open.add(next);
                // A discovered collision-checked leg is usable even when the
                // budget expires before its queue entry can be popped.
                if(next.feet.distanceToSqr(viewer.position())>best.feet.distanceToSqr(viewer.position()))best=next;
            }
        }
        if(best==start)return new long[0];var path=new ArrayList<Long>();for(var n=best;n.previous!=null;n=n.previous)path.add(n.cell.asLong());Collections.reverse(path);return path.stream().mapToLong(Long::longValue).toArray();
    }
    /** Bounded search toward an actual staging position. Each edge uses the
     * same mounted clearance and step rules as the physical ride. */
    static List<Vec3> toward(WybieBicycleEntity bike,Vec3 goal){
        var start=new Node(bike.blockPosition(),bike.position(),null,0);
        var open=new PriorityQueue<Node>(Comparator.comparingDouble(
                node->node.distance+1.4*node.feet.distanceTo(goal)));
        var seen=new HashSet<BlockPos>();
        open.add(start);
        int expanded=0;
        while(!open.isEmpty()&&expanded<96){
            var node=open.remove();
            if(!seen.add(node.cell))continue;
            expanded++;
            if(node.feet.distanceToSqr(goal)<.36){
                var route=new ArrayList<Vec3>();
                for(var leg=node;leg.previous!=null;leg=leg.previous)route.add(leg.feet);
                Collections.reverse(route);
                return route;
            }
            if(node.distance>=64)continue;
            for(var direction:Direction.Plane.HORIZONTAL){
                var cell=node.cell.relative(direction);
                var feet=step(bike,node.feet,cell.getX()+.5,cell.getZ()+.5,direction.toYRot());
                if(feet==null)continue;
                var feetCell=BlockPos.containing(feet);
                if(!seen.contains(feetCell))open.add(new Node(feetCell,feet,node,node.distance+1));
            }
        }
        return null;
    }
    private StoryBikeRoute(){}
}
