package com.coraline.feast;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Loaded-chunk-only collision/support tests. No carving, flight through walls,
 * instant navigation success or straight-line rescue-distance substitution. */
final class PalaceWellMotion {
    static Vec3 ground(ServerLevel level,Entity actor,Vec3 column,double range){
        if(!level.hasChunkAt(BlockPos.containing(column)))return null;
        var hit=level.clip(new ClipContext(column.add(0,range,0),column.add(0,-range,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor));
        if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection()!=net.minecraft.core.Direction.UP)return null;
        var at=new Vec3(column.x,hit.getLocation().y+actor.getY()-actor.getBoundingBox().minY+.02,column.z);return clear(level,actor,at)?at:null;
    }
    static boolean clear(ServerLevel level,Entity actor,Vec3 at){
        var box=actor.getBoundingBox().move(at.subtract(actor.position())).deflate(.015);
        for(int x:new int[]{(int)Math.floor(box.minX),(int)Math.floor(box.maxX)})for(int z:new int[]{(int)Math.floor(box.minZ),(int)Math.floor(box.maxZ)})if(!level.hasChunkAt(new BlockPos(x,(int)Math.floor(at.y),z)))return false;
        return !level.getBlockCollisions(actor,box).iterator().hasNext()&&!level.containsAnyLiquid(box);
    }
    static boolean move(ServerLevel level,Entity actor,Vec3 target,double speed){
        var delta=target.subtract(actor.position());var end=actor.position().add(delta.length()>speed?delta.normalize().scale(speed):delta);
        int steps=Math.max(1,(int)Math.ceil(actor.position().distanceTo(end)*8));
        for(int i=1;i<=steps;i++)if(!clear(level,actor,actor.position().lerp(end,i/(double)steps))){
            // Walk to the lip before descending. A diagonal drop whose collider
            // still overlaps the higher tile must not become a permanent ledge lock.
            var horizontal=new Vec3(delta.x,0,delta.z);
            if(delta.y<0&&horizontal.lengthSqr()>.0001){
                var edge=actor.position().add(horizontal.length()>speed?horizontal.normalize().scale(speed):horizontal);
                boolean open=true;for(int j=1;j<=4;j++)if(!clear(level,actor,actor.position().lerp(edge,j/4.0))){open=false;break;}
                if(open){actor.setPos(edge);actor.setDeltaMovement(Vec3.ZERO);actor.fallDistance=0;return true;}
            }return false;
        }
        actor.setPos(end);actor.setDeltaMovement(Vec3.ZERO);actor.fallDistance=0;return true;
    }
    /** Raise, swing over the rim, then lower the original bundle into the shaft. */
    static int throwBundle(ServerLevel level,CinematicPropEntity bundle,Vec3 well,int leg){
        double height=well.y+3.5;
        double horizontal=bundle.position().subtract(well).horizontalDistanceSqr();
        if(leg==2&&horizontal>.04||leg==1&&Math.abs(bundle.getY()-height)>.35)leg=0;
        var target=switch(leg){
            case 0->new Vec3(bundle.getX(),height,bundle.getZ());
            case 1->well.add(0,3.5,0);
            default->new Vec3(well.x,Math.max(well.y-16,bundle.getY()-.45),well.z);
        };
        if(move(level,bundle,target,.30)&&bundle.position().distanceToSqr(target)<.035&&leg<2)return leg+1;
        return leg;
    }
    static boolean seen(net.minecraft.server.level.ServerPlayer owner,Entity actor){
        for(var viewer:owner.server.getPlayerList().getPlayers())if(viewer.level()==actor.level()&&viewer.distanceToSqr(actor)<128*128&&viewer.hasLineOfSight(actor)
                &&viewer.getLookAngle().dot(actor.getEyePosition().subtract(viewer.getEyePosition()).normalize())>.15)return true;return false;
    }
    /** Walk off the upper tile before descending; a diagonal sweep into its
     * solid side incorrectly rejects ordinary one-block downhill steps. Upward
     * steps instead rise first, then cross, with both legs collision checked. */
    static boolean walkStep(ServerLevel level,Entity actor,Vec3 from,Vec3 to){
        var delta=to.subtract(from);
        if(Math.abs(delta.y)>1.001||Math.abs(delta.x)+Math.abs(delta.z)>1.001
                ||Math.abs(delta.x)>.001&&Math.abs(delta.z)>.001)return false;
        var corner=delta.y>0?new Vec3(from.x,to.y,from.z):new Vec3(to.x,from.y,to.z);
        for(var leg:new Vec3[][]{{from,corner},{corner,to}})
            for(int i=0;i<=4;i++)if(!clear(level,actor,leg[0].lerp(leg[1],i/4.0)))return false;
        return true;
    }
    /** Shortest bounded walkable approach, including one-block steps. Goal also
     * requires actual melee LOS. Missing chunks/unsafe tiles fail closed. */
    static double rescueRoute(net.minecraft.server.level.ServerPlayer p,Entity hand){
        record Node(BlockPos pos,int cost){}
        var queue=new ArrayDeque<Node>();var seen=new HashSet<BlockPos>();var start=p.blockPosition();queue.add(new Node(start,0));seen.add(start);
        for(int visited=0;!queue.isEmpty()&&visited<2048;visited++){
            var node=queue.removeFirst();var at=Vec3.atBottomCenterOf(node.pos());var eye=at.add(0,p.getEyeHeight(),0);
            if(at.distanceToSqr(hand.position())<=2.8*2.8&&p.serverLevel().clip(new ClipContext(eye,hand.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS)return node.cost();
            if(node.cost()>=24)continue;
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)for(int dy:new int[]{0,1,-1}){
                var next=node.pos().relative(direction).above(dy);if(seen.contains(next)||!p.serverLevel().hasChunkAt(next))continue;
                var feet=Vec3.atBottomCenterOf(next);var support=next.below();
                if(!p.serverLevel().getBlockState(support).isFaceSturdy(p.serverLevel(),support,net.minecraft.core.Direction.UP)||!clear(p.serverLevel(),p,feet))continue;
                if(!walkStep(p.serverLevel(),p,at,feet))continue;
                var high=at.add(0,Math.max(0,dy),0);
                if(p.serverLevel().clip(new ClipContext(high.add(0,1,0),feet.add(0,1,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)continue;
                seen.add(next);queue.addLast(new Node(next,node.cost()+1));break;
            }
        }return Double.NaN;
    }
    private PalaceWellMotion(){}
}
