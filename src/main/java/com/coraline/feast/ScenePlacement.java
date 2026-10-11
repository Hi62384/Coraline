package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.phys.AABB;

/** Searches real furniture footprints, not an arbitrary perfectly flat 5x5 clearing. */
final class ScenePlacement {
    static void prepareWalker(ParentEntity parent){
        if(parent.getNavigation() instanceof GroundPathNavigation navigation){
            navigation.setCanOpenDoors(true);navigation.setCanPassDoors(true);
        }
        var range=parent.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        if(range!=null)range.setBaseValue(Math.max(range.getBaseValue(),VisitConfig.PIANO_MAX.get()*2));
    }
    static BlockPos find(ServerLevel level,BlockPos origin,int min,int max,boolean piano,ParentEntity walker,BlockPos avoid){
        max=Math.max(min,max);
        for(int attempt=0;attempt<VisitConfig.PLACEMENT_ATTEMPTS.get();attempt++){
            double angle=level.random.nextDouble()*Math.PI*2;
            int distance=min+level.random.nextInt(max-min+1);
            var column=origin.offset((int)Math.round(Math.cos(angle)*distance),0,(int)Math.round(Math.sin(angle)*distance));
            if(!level.hasChunkAt(column))continue;
            for(int offset=0;offset<=VisitConfig.PLACEMENT_HEIGHT.get()*2;offset++){
                int dy=offset==0?0:(offset+1)/2*(offset%2==1?1:-1);
                var candidate=column.above(dy);
                if(avoid!=null&&candidate.distSqr(avoid)<64)continue;
                if(!fits(level,candidate,piano))continue;
                // Approach from the chair/keyboard side. Wooden house doors are traversable.
                var path=walker.getNavigation().createPath(candidate.north(2),1);
                if(path!=null&&path.canReach())return candidate;
            }
        }
        return null;
    }
    /** Last resort for cramped cave homes: build only in verified empty air above the
     * copied terrain. Never excavate or replace a player's structure or inventory. */
    static BlockPos[] terrace(ServerLevel level,BlockPos origin,int radius){
        int length=Math.min(38,Math.max(18,radius/2));
        int x0=origin.getX()-4,z0=origin.getZ()-4;
        int y=Math.max(origin.getY()+6,level.getMinBuildHeight()+8);
        for(int x=x0;x<=x0+length;x++)for(int z=z0;z<=z0+8;z++){
            var p=new BlockPos(x,y,z);level.getChunkAt(p);
            y=Math.max(y,level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,x,z)+4);
        }
        if(y+5>=level.getMaxBuildHeight())return null;
        for(int x=x0;x<=x0+length;x++)for(int z=z0;z<=z0+8;z++)for(int dy=0;dy<5;dy++)
            if(!level.getBlockState(new BlockPos(x,y+dy,z)).isAir())return null;
        // Connected, fenced terrace; the guide carries cave visitors here when no
        // walkable route exists. Coordinates remain within the owning copied island.
        for(int x=x0;x<=x0+length;x++)for(int z=z0;z<=z0+8;z++){
            var floor=new BlockPos(x,y,z);
            level.setBlock(floor,net.minecraft.world.level.block.Blocks.DARK_OAK_PLANKS.defaultBlockState(),3);
            if(x==x0||x==x0+length||z==z0||z==z0+8)
                level.setBlock(floor.above(),net.minecraft.world.level.block.Blocks.DARK_OAK_FENCE.defaultBlockState(),3);
        }
        return new BlockPos[]{new BlockPos(x0+5,y+1,z0+4),new BlockPos(x0+length-5,y+1,z0+4)};
    }
    static boolean fits(ServerLevel level,BlockPos pos,boolean piano){
        if(!supported(level,pos)||!supported(level,pos.north(2)))return false;
        double halfWidth=piano?1.3:1.85,back=piano?1.95:2.15;
        var space=new AABB(pos.getX()+.5-halfWidth,pos.getY(),pos.getZ()-2.45,
                pos.getX()+.5+halfWidth,pos.getY()+2.3,pos.getZ()+.5+back);
        if(space.minY<level.getMinBuildHeight()||space.maxY>=level.getMaxBuildHeight())return false;
        if(!level.getWorldBorder().isWithinBounds(space)||level.getBlockCollisions(null,space).iterator().hasNext()||level.containsAnyLiquid(space))return false;
        // Small terrain steps beneath the outer legs are fine; reject cliff/void overhangs.
        for(int x=(int)Math.floor(space.minX);x<Math.ceil(space.maxX);x++)for(int z=(int)Math.floor(space.minZ);z<Math.ceil(space.maxZ);z++){
            boolean ground=false;
            for(int drop=0;drop<=2;drop++)if(supported(level,new BlockPos(x,pos.getY()-drop,z))){ground=true;break;}
            if(!ground)return false;
        }
        return true;
    }
    private static boolean supported(ServerLevel level,BlockPos pos){
        return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);
    }
}
