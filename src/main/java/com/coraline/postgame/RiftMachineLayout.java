package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;

/** Canonical machinery/door coordinates derived from the saved facility. */
final class RiftMachineLayout {
    enum Kind { CORE, CONDUIT, GATE }
    record Part(Kind kind,BlockPos at){}
    record Decoded(InfiniteStructurePlan.Plan plan,int slot){}
    private record Key(UUID generation,long seed,int x,int z){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> entry){return size()>128;}};
    private static final Map<UUID,List<Part>> PARTS=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<UUID,List<Part>> entry){return size()>128;}};
    static List<Part> parts(InfiniteStructurePlan.Plan plan){
        if(plan.family()!=InfiniteStructurePlan.Family.RIFT_DRILL_CENTER)return List.of();
        synchronized(PARTS){var cached=PARTS.get(plan.site());if(cached!=null)return cached;}
        var chamber=plan.modules().stream().filter(m->m.role()==InfiniteStructurePlan.Role.DRILL_CHAMBER).findFirst().orElseThrow();var out=new ArrayList<Part>();
        out.add(new Part(Kind.CORE,new BlockPos(plan.x()+chamber.x()+chamber.width()/2,plan.y()+chamber.y()+1,plan.z()+chamber.z()+chamber.depth()/2)));
        for(var module:plan.modules())if(module.role()==InfiniteStructurePlan.Role.CONTROL)out.add(new Part(Kind.CONDUIT,new BlockPos(plan.x()+module.x()+module.width()/2,plan.y()+module.y()+1,plan.z()+module.z()+module.depth()/2)));
        for(int offset=-1;offset<=1;offset++)for(int height=1;height<=2;height++){
            int y=plan.y()+chamber.y()+height;
            out.add(new Part(Kind.GATE,new BlockPos(plan.x()+chamber.x(),y,plan.z()+chamber.z()+chamber.depth()/2+offset)));
            out.add(new Part(Kind.GATE,new BlockPos(plan.x()+chamber.maxX(),y,plan.z()+chamber.z()+chamber.depth()/2+offset)));
            out.add(new Part(Kind.GATE,new BlockPos(plan.x()+chamber.x()+chamber.width()/2+offset,y,plan.z()+chamber.z())));
            out.add(new Part(Kind.GATE,new BlockPos(plan.x()+chamber.x()+chamber.width()/2+offset,y,plan.z()+chamber.maxZ())));
        }
        var result=List.copyOf(out);synchronized(PARTS){PARTS.put(plan.site(),result);}return result;
    }
    static CompoundTag metadata(InfiniteStructurePlan.Plan plan,int slot){var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Site",plan.site());n.putUUID("Generation",plan.generation());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Slot",slot);return n;}
    static Optional<Decoded> read(CompoundTag n,BlockPos at){
        for(String key:List.of("Version","AnchorX","AnchorZ","Slot"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();
        if(n.getInt("Version")!=1||!n.hasUUID("Site")||!n.hasUUID("Generation")||!n.contains("Seed",Tag.TAG_LONG))return Optional.empty();
        var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"));Optional<InfiniteStructurePlan.Plan> found;
        synchronized(CACHE){found=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,InfiniteStructurePlan.Family.RIFT_DRILL_CENTER,k.x,k.z));}
        if(found.isEmpty())return Optional.empty();var plan=found.orElseThrow();int slot=n.getInt("Slot");var parts=parts(plan);
        return plan.site().equals(n.getUUID("Site"))&&slot>=0&&slot<parts.size()&&parts.get(slot).at().equals(at)?Optional.of(new Decoded(plan,slot)):Optional.empty();
    }
    private RiftMachineLayout(){}
}
