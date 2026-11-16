package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Exact generated workshop doorway cells; metadata is not live authority. */
final class SiteBindingReceipt {
    record View(UUID site,UUID generation,long seed,boolean cut){}
    static List<BlockPos> cells(InfiniteStructurePlan.Plan plan,int module){
        if(module<0||module>=plan.modules().size()||plan.family()!=InfiniteStructurePlan.Family.DOLL_WORKSHOP)return List.of();
        var room=plan.modules().get(module);if(room.role()!=InfiniteStructurePlan.Role.WORKROOM)return List.of();
        var result=new ArrayList<BlockPos>();
        for(int offset=-1;offset<=1;offset++)for(int height=1;height<=2;height++){
            int y=plan.y()+room.y()+height;
            result.add(new BlockPos(plan.x()+room.x(),y,plan.z()+room.z()+room.depth()/2+offset));
            result.add(new BlockPos(plan.x()+room.maxX(),y,plan.z()+room.z()+room.depth()/2+offset));
            result.add(new BlockPos(plan.x()+room.x()+room.width()/2+offset,y,plan.z()+room.z()));
            result.add(new BlockPos(plan.x()+room.x()+room.width()/2+offset,y,plan.z()+room.maxZ()));
        }
        return List.copyOf(result);
    }
    static Optional<CompoundTag> create(InfiniteStructurePlan.Plan plan,int module,BlockPos at){
        int cell=cells(plan,module).indexOf(at);if(cell<0)return Optional.empty();
        var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Site",plan.site());n.putUUID("Generation",plan.generation());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Module",module);n.putInt("Cell",cell);n.putBoolean("Cut",false);return Optional.of(n);
    }
    static Optional<View> read(CompoundTag n,BlockPos at){
        for(String key:List.of("Version","AnchorX","AnchorZ","Module","Cell"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();
        if(n.getInt("Version")!=1||!n.hasUUID("Site")||!n.hasUUID("Generation")||!n.contains("Seed",Tag.TAG_LONG)||!n.contains("Cut",Tag.TAG_BYTE)||n.getByte("Cut")<0||n.getByte("Cut")>1)return Optional.empty();
        var plan=InfiniteStructurePlan.at(n.getUUID("Generation"),n.getLong("Seed"),InfiniteStructurePlan.Family.DOLL_WORKSHOP,n.getInt("AnchorX"),n.getInt("AnchorZ")).orElse(null);
        if(plan==null||!plan.site().equals(n.getUUID("Site")))return Optional.empty();
        var cells=cells(plan,n.getInt("Module"));int index=n.getInt("Cell");
        if(index<0||index>=cells.size()||!cells.get(index).equals(at))return Optional.empty();
        return Optional.of(new View(plan.site(),plan.generation(),plan.seed(),n.getBoolean("Cut")));
    }
    private SiteBindingReceipt(){}
}
