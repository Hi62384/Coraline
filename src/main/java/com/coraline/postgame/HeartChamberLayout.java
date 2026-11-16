package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Suspended organic core and four physical containment pylons above the arena. */
final class HeartChamberLayout {
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan p,int module){
        if(p.family()!=InfiniteStructurePlan.Family.ENTITY_HEART_CHAMBER||p.modules().get(module).role()!=InfiniteStructurePlan.Role.DEFENSE_HALL)return;
        for(int dx=-3;dx<=3;dx++)for(int dy=5;dy<=11;dy++)for(int dz=-3;dz<=3;dz++)if(dx*dx+dz*dz+(dy-8)*(dy-8)<=11){var state=(dx*dx+dz*dz+(dy-8)*(dy-8)<5?Blocks.REDSTONE_BLOCK:Blocks.RED_STAINED_GLASS).defaultBlockState();put(level,clip,new BlockPos(p.x()+dx,p.y()+dy,p.z()+dz),state);}
        for(int x:new int[]{-9,9})for(int z:new int[]{-9,9}){for(int y=1;y<=10;y++)put(level,clip,new BlockPos(p.x()+x,p.y()+y,p.z()+z),Blocks.POLISHED_DEEPSLATE.defaultBlockState());put(level,clip,new BlockPos(p.x()+x,p.y()+11,p.z()+z),Blocks.SEA_LANTERN.defaultBlockState());}
        for(int y=12;y<=16;y++)put(level,clip,new BlockPos(p.x(),p.y()+y,p.z()),Blocks.CHAIN.defaultBlockState());
    }
    private static void put(WorldGenLevel l,BoundingBox clip,BlockPos at,net.minecraft.world.level.block.state.BlockState s){if(clip.isInside(at)&&l.getBlockEntity(at)==null)l.setBlock(at,s,2);}
    private HeartChamberLayout(){}
}
