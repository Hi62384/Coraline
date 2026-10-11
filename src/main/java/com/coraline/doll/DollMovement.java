package com.coraline.doll;

import com.coraline.block.entity.DollBlockEntity;
import com.coraline.config.CoralineConfig;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

public final class DollMovement {
    public static boolean watched(ServerLevel level,BlockPos pos){
        Vec3 center=pos.getCenter();
        for(var player:level.players()){
            if(player.isSleeping())continue;
            Vec3 delta=center.subtract(player.getEyePosition());double distance=delta.lengthSqr();
            if(distance>64*64)continue;
            if(distance<.01)return true;
            // A conservative viewing cone; never move in peripheral vision or through visible windows.
            if(player.getLookAngle().dot(delta.normalize())<.25)continue;
            var hit=level.clip(new ClipContext(player.getEyePosition(),center,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
            if(hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(pos)||!level.getBlockState(hit.getBlockPos()).canOcclude())return true;
        }return false;
    }
    public static void tryMove(ServerLevel level,BlockPos origin,DollBlockEntity doll){
        tryMove(level,origin,doll,pos->DollHome.nearBed(level,pos,doll.getOwnerId()));
    }
    private static boolean indoors(ServerLevel level,BlockPos pos){return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ())>pos.getY()+1;}
    static void tryMove(ServerLevel level,BlockPos origin,DollBlockEntity doll,java.util.function.Predicate<BlockPos> withinHome){
        if(!withinHome.test(origin)||!indoors(level,origin)||watched(level,origin))return;
        int radius=CoralineConfig.DOLL_MOVE_RADIUS.get();
        Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();List<BlockPos> choices=new ArrayList<>();
        seen.add(origin);queue.add(origin);
        // Connected, supported air cells keep the doll in the room instead of teleporting through walls.
        while(!queue.isEmpty()&&seen.size()<256){
            var from=queue.remove();
            for(Direction direction:Direction.Plane.HORIZONTAL)for(int dy:new int[]{0,1,-1}){
                var next=from.relative(direction).offset(0,dy,0);
                if(!withinHome.test(next)||seen.contains(next)||Math.abs(next.getX()-origin.getX())>radius||Math.abs(next.getZ()-origin.getZ())>radius||Math.abs(next.getY()-origin.getY())>1
                        ||!level.hasChunkAt(next)||!level.getWorldBorder().isWithinBounds(next)||!level.isEmptyBlock(next)||!indoors(level,next)
                        ||!level.getBlockState(next.below()).isFaceSturdy(level,next.below(),Direction.UP))continue;
                // A raised step must also clear the horizontal transition above the old cell.
                if(dy>0&&!level.isEmptyBlock(from.above()))continue;
                if(dy<0&&!level.isEmptyBlock(next.above()))continue;
                seen.add(next);queue.add(next);
                if(next.distSqr(origin)>=4&&!watched(level,next)&&level.noCollision(new AABB(next).deflate(.15,0,.15)))choices.add(next);
            }
        }
        if(choices.isEmpty())return;
        BlockPos destination=choices.get(level.random.nextInt(choices.size()));var state=doll.getBlockState();var data=doll.saveWithoutMetadata();
        doll.moving=true;
        if(!level.setBlock(destination,state,3)){doll.moving=false;return;}
        if(level.getBlockEntity(destination) instanceof DollBlockEntity moved){
            moved.load(data);moved.setChanged();level.removeBlock(origin,false);
            level.sendBlockUpdated(destination,state,state,3);
            DollBondData.get(level.getServer()).placed(moved.createDollStack(),level,destination);
        }else{level.removeBlock(destination,false);doll.moving=false;}
    }
}
