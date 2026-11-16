package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.state.BlockState;

/** Repair only canonical missing cells; never overwrite replacement builds or inventories. */
public final class TycoonMaintenance {
    public static boolean damaged(ServerLevel level,CompoundTag row){
        for(var e:TycoonLandscape.decode(level,row.getCompound("Generated")).entrySet())if(!e.getValue().isAir()&&level.hasChunkAt(e.getKey())&&!level.getBlockState(e.getKey()).equals(e.getValue()))return true;
        return false;
    }
    public static String repair(ServerPlayer p,UUID id,boolean machine){
        var h=TycoonWorld.home(p);var row=(machine?TycoonMachines.machines(h):TycoonAttractions.records(h)).getCompound(id.toString());var level=p.server.getLevel(TycoonWorld.KEY);
        if(level==null||row.isEmpty()||p.serverLevel()!=level&&!TycoonWorkers.constructing(p)||!machine&&TycoonAttractions.occupied(h,id)>0)return "Select an empty owned structure in the personal world.";
        var expected=TycoonLandscape.decode(level,row.getCompound("Generated"));if(expected.isEmpty())return "Missing generated provenance; repair refused.";
        boolean changed=false;for(var e:expected.entrySet()){level.getChunkAt(e.getKey());var actual=level.getBlockState(e.getKey());if(actual.equals(e.getValue()))continue;if(!actual.isAir()||e.getValue().isAir()||level.getBlockEntity(e.getKey())!=null)return "Restore changed non-generated blocks manually. No replacement build will be overwritten.";changed=true;}
        if(!changed)return "No missing structure cells.";
        var values=new CompoundTag();values.putBoolean(machine?"Verified":"Operational",true);var changes=new CompoundTag();changes.put(id.toString(),values);var patch=new CompoundTag();patch.put(machine?"MachineChanges":"AttractionChanges",changes);
        return TycoonBuildJob.begin(p,"attraction repair "+id,row.getCompound("Generated"),expected,row.getLong("Position"),patch,8*Math.max(1,row.getInt(machine?"Unlocked":"Tier")));
    }
    private TycoonMaintenance(){}
}
