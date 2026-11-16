package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Native route modules carry genuine mechanical traces and one forged lure. */
final class WellScarLayout {
    static BlockPos clue(InfiniteStructurePlan.Plan p,int clue){return new BlockPos(p.x()+(clue==3?1:-1),p.y()+1,p.z()+new int[]{-20,-17,-14,-12}[clue]);}
    static void place(WorldGenLevel l,BoundingBox clip,InfiniteStructurePlan.Plan p,int module){if(p.family()!=InfiniteStructurePlan.Family.FALSE_PINK_PALACE)return;var room=p.modules().get(module);if(room.role()!=InfiniteStructurePlan.Role.APPROACH)return;
        for(int x:new int[]{room.x(),room.maxX()})for(int z=room.z();z<=room.maxZ();z+=3)for(int y=1;y<=2+Math.floorMod(z,3);y++){var at=new BlockPos(p.x()+x,p.y()+y,p.z()+z);if(clip.isInside(at)&&l.getBlockEntity(at)==null)l.setBlock(at,(y%2==0?Blocks.SMOOTH_QUARTZ:Blocks.OBSIDIAN).defaultBlockState(),2);}
        for(int i=0;i<4;i++){var at=clue(p,i);if(at.getZ()<p.z()+room.z()||at.getZ()>p.z()+room.maxZ()||!clip.isInside(at)||l.getBlockEntity(at)!=null)continue;l.setBlock(at,com.coraline.registry.ModBlocks.WELL_SCAR_CLUE.get().defaultBlockState().setValue(WellScarClueBlock.CLUE,i),2);if(l.getBlockEntity(at) instanceof WellScarClueEntity clue)clue.configure(p,i);}
    }
    private WellScarLayout(){}
}
