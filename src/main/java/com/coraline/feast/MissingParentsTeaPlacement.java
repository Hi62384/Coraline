package com.coraline.feast;

import com.coraline.bobinsky.*;
import com.coraline.tea.TeaHouse;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** A temporary house supplies its own floor; terrain underneath is optional. */
public final class MissingParentsTeaPlacement {
    public static final int CLEAR_RX=TeaHouse.RX+2,CLEAR_RZ=TeaHouse.RZ+3;
    public static BlockPos find(ServerPlayer player){
        var level=player.serverLevel();var base=player.blockPosition();var candidates=new java.util.ArrayList<BlockPos>();
        for(int radius:new int[]{24,40,56})for(int side=0;side<8;side++){
            double angle=side*Math.PI/4;
            candidates.add(new BlockPos(base.getX()+(int)Math.round(Math.cos(angle)*radius),base.getY(),base.getZ()+(int)Math.round(Math.sin(angle)*radius)));
        }
        // Try every nearby ground site before falling back to a floating house.
        // Median footing fits slopes without lifting the whole house above one hill.
        for(var at:candidates){var ground=grounded(level,at);if(ground!=null&&safe(player,ground))return ground;}
        for(var at:candidates){
            if(safe(player,at))return at;
            int top=at.getY();
            for(int x=-CLEAR_RX;x<=CLEAR_RX;x+=2)for(int z=-CLEAR_RZ;z<=CLEAR_RZ;z+=2)
                top=Math.max(top,level.getHeight(Heightmap.Types.MOTION_BLOCKING,at.getX()+x,at.getZ()+z)+2);
            for(int y=top;y<level.getMaxBuildHeight()-10;y+=12)if(safe(player,at.atY(y)))return at.atY(y);
        }
        return null;
    }
    static BlockPos grounded(ServerLevel level,BlockPos near){
        var heights=new java.util.ArrayList<Integer>();
        for(int x:new int[]{-TeaHouse.RX+1,0,TeaHouse.RX-1})for(int z:new int[]{-TeaHouse.RZ,0,TeaHouse.RZ-1}){
            var column=near.offset(x,0,z);level.getChunkAt(column);
            int top=Math.min(near.getY()+24,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column.getX(),column.getZ()));
            int bottom=Math.max(level.getMinBuildHeight()+2,near.getY()-64);
            for(int y=top;y>=bottom;y--){var floor=column.atY(y-1);var state=level.getBlockState(floor);
                if(state.is(net.minecraft.tags.BlockTags.LOGS)||state.is(net.minecraft.tags.BlockTags.LEAVES))continue;
                if(state.isFaceSturdy(level,floor,net.minecraft.core.Direction.UP)&&state.getFluidState().isEmpty()){heights.add(y);break;}
            }
        }
        if(heights.isEmpty())return null;heights.sort(Integer::compareTo);return near.atY(heights.get(heights.size()/2));
    }
    static boolean safe(ServerPlayer player,BlockPos center){
        var level=player.serverLevel();int rx=CLEAR_RX,rz=CLEAR_RZ;
        if(center.getY()-1<level.getMinBuildHeight()||center.getY()+10>=level.getMaxBuildHeight())return false;
        if(!level.getWorldBorder().isWithinBounds(center.offset(-rx,0,-rz))||!level.getWorldBorder().isWithinBounds(center.offset(rx,0,rz)))return false;
        var box=new AABB(center.offset(-rx,-1,-rz),center.offset(rx+1,10,rz+1));
        for(var visit:BobinskyHouseProtection.houses(level))if(visit.getBoolean("Active")&&visit.contains("Originals")&&BobinskyHouse.bounds(visit).intersects(box))return false;
        if(!level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,box).isEmpty())return false;
        for(int cx=(center.getX()-rx)>>4;cx<=(center.getX()+rx)>>4;cx++)for(int cz=(center.getZ()-rz)>>4;cz<=(center.getZ()+rz)>>4;cz++)level.getChunk(cx,cz);
        for(var pos:BlockPos.betweenClosed(center.offset(-rx,-1,-rz),center.offset(rx,9,rz))){
            var state=level.getBlockState(pos);
            if(state.hasBlockEntity()||state.getBlock() instanceof BedBlock||state.getDestroySpeed(level,pos)<0)return false;
        }
        return true;
    }
    private MissingParentsTeaPlacement(){}
}
