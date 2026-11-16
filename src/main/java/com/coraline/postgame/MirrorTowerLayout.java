package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Two complete turns of a walkable rising stair, three false mirror walls,
 * dead-end niches, and a real summit landing. Placement remains chunk-clipped. */
final class MirrorTowerLayout {
    private static final List<BlockPos> RING=ring();
    private static List<BlockPos> ring(){var result=new ArrayList<BlockPos>();for(int x=2;x<=8;x++)result.add(new BlockPos(x,0,2));for(int z=3;z<=8;z++)result.add(new BlockPos(8,0,z));for(int x=7;x>=2;x--)result.add(new BlockPos(x,0,8));for(int z=7;z>=3;z--)result.add(new BlockPos(2,0,z));return List.copyOf(result);}
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan plan,int index){
        var room=plan.modules().get(index);if(plan.family()!=InfiniteStructurePlan.Family.MIRROR_TOWER||room.role()!=InfiniteStructurePlan.Role.MIRROR_SHAFT)return;
        var origin=new BlockPos(plan.x()+room.x(),plan.y()+room.y(),plan.z()+room.z());var rock=InfiniteContent.block("mirrorstone").defaultBlockState();
        for(int y=1;y<=26;y++)for(int x=4;x<=6;x++)for(int z=4;z<=6;z++)put(level,clip,origin.offset(x,y,z),rock);
        for(int i=0;i<48;i++){
            var point=RING.get(i%RING.size());var next=RING.get((i+1)%RING.size());int height=i/2+1;
            var facing=Direction.fromDelta(next.getX()-point.getX(),0,next.getZ()-point.getZ());
            var step=i%2==0?Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing):Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            put(level,clip,origin.offset(point.getX(),height,point.getZ()),step);
            var outer=point.getZ()==2?point.north():point.getX()==8?point.east():point.getZ()==8?point.south():point.west();
            put(level,clip,origin.offset(outer.getX(),height,outer.getZ()),step);
        }
        // The final stair meets this 4x3 landing; the summit cache rests on it.
        for(int x=1;x<=4;x++)for(int z=1;z<=3;z++)put(level,clip,origin.offset(x,24,z),rock);
        for(int checkpoint=0;checkpoint<3;checkpoint++){
            int height=6+checkpoint*6;boolean east=checkpoint%2==0;int z=5;
            for(int x=east?8:1;x<=(east?9:2);x++)for(int y=height;y<height+2;y++)put(level,clip,origin.offset(x,y,z),InfiniteContent.ILLUSION_MIRROR.get().defaultBlockState());
            // Matching false alcoves lead only one block into the solid core.
            for(int y=height;y<height+2;y++)put(level,clip,origin.offset(3,y,5),InfiniteContent.ILLUSION_MIRROR.get().defaultBlockState());
            put(level,clip,origin.offset(5,height,0),Blocks.SEA_LANTERN.defaultBlockState());
        }
        put(level,clip,origin.offset(1,26,1),Blocks.SEA_LANTERN.defaultBlockState());
    }
    /** The expedition ends on the actual generated summit, not anywhere at
     * the same altitude or at a coordinate supplied by the client. */
    static boolean reachedSummit(net.minecraft.server.level.ServerPlayer player,InfiniteStructurePlan.Plan plan){
        if(plan.family()!=InfiniteStructurePlan.Family.MIRROR_TOWER||!player.onGround()||player.isPassenger())return false;
        var room=plan.modules().stream().filter(m->m.role()==InfiniteStructurePlan.Role.MIRROR_SHAFT).findFirst().orElse(null);if(room==null)return false;
        var origin=new BlockPos(plan.x()+room.x(),plan.y()+room.y(),plan.z()+room.z());var feet=player.blockPosition();int x=feet.getX()-origin.getX(),z=feet.getZ()-origin.getZ();
        return x>=1&&x<=4&&z>=1&&z<=3&&feet.getY()==origin.getY()+25&&player.level().hasChunkAt(feet.below())&&player.level().getBlockState(feet.below()).is(InfiniteContent.block("mirrorstone"));
    }
    private static void put(WorldGenLevel level,BoundingBox clip,BlockPos at,BlockState state){if(clip.isInside(at)&&level.getBlockEntity(at)==null)level.setBlock(at,state,2);}
    private MirrorTowerLayout(){}
}
