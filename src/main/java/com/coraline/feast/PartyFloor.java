package com.coraline.feast;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;

/** Indoor standing clearance includes thin carpet; no house blocks are removed. */
public final class PartyFloor {
    public static Vec3 at(ServerPlayer p,BlockPos cell){
        var level=p.serverLevel();if(!level.hasChunkAt(cell)||!level.getFluidState(cell).isEmpty()||!level.getFluidState(cell.above()).isEmpty()||!level.getBlockState(cell.below()).isFaceSturdy(level,cell.below(),Direction.UP))return null;
        var shape=level.getBlockState(cell).getCollisionShape(level,cell);double covering=shape.isEmpty()?0:shape.max(Direction.Axis.Y);if(covering>.125)return null;
        var at=Vec3.atBottomCenterOf(cell).add(0,covering+.001,0);var box=new AABB(at.x-.31,at.y,at.z-.31,at.x+.31,at.y+1.95,at.z+.31);
        return level.getBlockCollisions(null,box).iterator().hasNext()?null:at;
    }
    public static boolean clear(ServerPlayer p,AABB volume){return !p.serverLevel().getBlockCollisions(null,volume).iterator().hasNext()&&!p.serverLevel().containsAnyLiquid(volume);}
    public static Vec3 rectangle(ServerPlayer p,BlockPos center,int halfX,int halfZ){
        double y=center.getY();for(int x=-halfX;x<=halfX;x++)for(int z=-halfZ;z<=halfZ;z++){var at=at(p,center.offset(x,0,z));if(at==null||!PartyRegions.allowed(p,at))return null;y=Math.max(y,at.y);}
        var result=new Vec3(center.getX()+.5,y,center.getZ()+.5);return clear(p,new AABB(result.x-halfX-.35,result.y,result.z-halfZ-.35,result.x+halfX+.35,result.y+2,result.z+halfZ+.35))?result:null;
    }
    private PartyFloor(){}
}
