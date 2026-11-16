package com.coraline.otherworld;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Local, rounded grading around actual foundations, never a rectangular plot fill. */
final class RosewoodTerrain {
    private RosewoodTerrain(){}
    static Map<BlockPos,Integer> prepare(ServerLevel source,ServerLevel level,BlockPos house,BlockPos previous,RealmData.Realm realm,Set<BlockPos> footprint,Set<BlockPos> columns){
        if(source!=level&&previous!=null){
            // Remove the previous platform using the original copied landscape,
            // including its buried pedestal and old roof, before local grading.
            int bottom=Math.max(level.getMinBuildHeight(),Math.min(previous.getY(),house.getY())-48),top=Math.min(level.getMaxBuildHeight()-1,Math.max(previous.getY(),house.getY())+45);
            for(int x=-33;x<=31;x++)for(int z=-43;z<=41;z++)for(int y=bottom;y<=top;y++){
                var at=new BlockPos(previous.getX()+x,y,previous.getZ()+z);var original=source.getBlockState(at.offset(-realm.dx,0,-realm.dz));
                // Never duplicate source-world inventories or linked mod portals.
                if(original.hasBlockEntity()||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(original.getBlock()).getNamespace().equals("coraline"))original=Blocks.AIR.defaultBlockState();
                if(!level.getBlockState(at).equals(original)){level.removeBlockEntity(at);GenerationBlocks.set(level,at,original,2);}
            }
        }
        var distances=new HashMap<BlockPos,Double>();
        for(var root:footprint)for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++){
            double distance=Math.hypot(dx,dz);if(distance>8)continue;
            distances.merge(root.offset(dx,0,dz),distance,Math::min);
        }
        var originals=new HashMap<BlockPos,Integer>();var all=new HashSet<>(columns);all.addAll(distances.keySet());
        for(var column:all)originals.put(column,OtherWorldEstate.naturalSurface(source,column.getX()-realm.dx,column.getZ()-realm.dz)-1);
        var grades=new HashMap<>(originals);int porchGrade=house.getY()-2;
        for(var entry:distances.entrySet()){
            var column=entry.getKey();int natural=originals.get(column);
            // A slight edge variation avoids a ruler-straight terrace boundary.
            double wave=.45*Math.sin(column.getX()*.37)+.45*Math.cos(column.getZ()*.29);
            double t=Math.max(0,Math.min(1,(entry.getValue()+wave)/8));if(entry.getValue()==0)t=0;
            double blend=t*t*(3-2*t);int desired=(int)Math.round(porchGrade+(natural-porchGrade)*blend);grades.put(column,desired);
            // Clear natural vegetation through its full height, not just the
            // bottom of trunks: grading must not leave suspended tree fragments.
            for(int y=natural+1;y<=Math.min(level.getMaxBuildHeight()-1,natural+40);y++){
                var at=new BlockPos(column.getX(),y,column.getZ());var state=level.getBlockState(at);
                if(state.is(net.minecraft.tags.BlockTags.LOGS)||state.is(net.minecraft.tags.BlockTags.LEAVES))GenerationBlocks.set(level,at,Blocks.AIR.defaultBlockState(),2);
            }
            if(desired==natural)continue;
            for(int y=desired+1;y<=Math.max(natural+3,desired+3);y++)GenerationBlocks.set(level,new BlockPos(column.getX(),y,column.getZ()),Blocks.AIR.defaultBlockState(),2);
            for(int y=Math.min(natural,desired)-3;y<=desired;y++)GenerationBlocks.set(level,new BlockPos(column.getX(),y,column.getZ()),(y==desired?Blocks.GRASS_BLOCK:Blocks.DIRT).defaultBlockState(),2);
        }
        return grades;
    }
}
