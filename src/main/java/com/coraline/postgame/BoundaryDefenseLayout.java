package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.Level;

final class BoundaryDefenseLayout {
    private record Key(UUID generation,long seed,int x,int z,InfiniteStructurePlan.Family family){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> entry){return size()>128;}};
    static BlockPos core(InfiniteStructurePlan.Plan plan){var hall=plan.modules().stream().filter(m->m.role()==InfiniteStructurePlan.Role.DEFENSE_HALL).findFirst().orElseThrow();return new BlockPos(plan.x()+hall.x()+hall.width()/2,plan.y()+hall.y()+1,plan.z()+hall.z()+hall.depth()/2);}
    static boolean family(InfiniteStructurePlan.Family family){return family==InfiniteStructurePlan.Family.ANCIENT_BOUNDARY_SHRINE||family==InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY||family==InfiniteStructurePlan.Family.ENTITY_HEART_CHAMBER;}
    static CompoundTag metadata(InfiniteStructurePlan.Plan plan){var n=new CompoundTag();n.putInt("Version",1);n.putString("Family",plan.family().name());n.putUUID("Generation",plan.generation());n.putUUID("Site",plan.site());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());return n;}
    static Optional<InfiniteStructurePlan.Plan> read(CompoundTag n){
        for(String key:List.of("Version","AnchorX","AnchorZ"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();if(n.getInt("Version")!=1||!n.hasUUID("Generation")||!n.hasUUID("Site")||!n.contains("Seed",Tag.TAG_LONG))return Optional.empty();
        InfiniteStructurePlan.Family family;try{if(n.contains("Family")&&!n.contains("Family",Tag.TAG_STRING))return Optional.empty();family=n.contains("Family")?InfiniteStructurePlan.Family.valueOf(n.getString("Family")):InfiniteStructurePlan.Family.ANCIENT_BOUNDARY_SHRINE;}catch(IllegalArgumentException unknown){return Optional.empty();}if(!family(family))return Optional.empty();
        var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"),family);Optional<InfiniteStructurePlan.Plan> found;synchronized(CACHE){found=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,k.family,k.x,k.z));}return found.filter(p->p.site().equals(n.getUUID("Site")));
    }
    static boolean cacheReady(Level level,CompoundTag receipt){
        if(!receipt.getString("Family").equals(InfiniteStructurePlan.Family.ANCIENT_BOUNDARY_SHRINE.name())&&!receipt.getString("Family").equals(InfiniteStructurePlan.Family.RESISTANCE_SANCTUARY.name())&&!receipt.getString("Family").equals(InfiniteStructurePlan.Family.ENTITY_HEART_CHAMBER.name()))return true;
        var plan=read(receipt).orElse(null);if(plan==null)return false;var at=core(plan);return level!=null&&level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof BoundaryDefenseEntity defense&&defense.authentic()&&defense.complete();
    }
    private BoundaryDefenseLayout(){}
}
