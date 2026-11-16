package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Machinery occupies the existing immutable factory footprint. */
final class FactoryLayout {
    record Decoded(InfiniteStructurePlan.Plan plan,int slot){}
    private record Key(UUID generation,long seed,int x,int z){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(64,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> row){return size()>64;}};
    static BlockPos part(InfiniteStructurePlan.Plan plan,int slot){
        var role=slot==1?InfiniteStructurePlan.Role.WORKROOM:slot==3?InfiniteStructurePlan.Role.CONTROL:InfiniteStructurePlan.Role.HALL;
        var room=plan.modules().stream().filter(m->m.role()==role).findFirst().orElseThrow();
        return new BlockPos(plan.x()+room.x()+room.width()/2+(slot==2?4:0),plan.y()+room.y()+1,plan.z()+room.z()+room.depth()/2+(slot==0?-3:slot==2?3:0));
    }
    static BlockPos archive(InfiniteStructurePlan.Plan plan){for(int i=0;i<plan.modules().size();i++)if(plan.modules().get(i).role()==InfiniteStructurePlan.Role.CONTROL)return SiteCacheReceipt.position(plan,i);throw new IllegalArgumentException("Factory control room missing");}
    static CompoundTag metadata(InfiniteStructurePlan.Plan plan,int slot){var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Site",plan.site());n.putUUID("Generation",plan.generation());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Slot",slot);n.putInt("Clutches",0);return n;}
    private static Optional<InfiniteStructurePlan.Plan> resolve(CompoundTag n){var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"));synchronized(CACHE){return CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,InfiniteStructurePlan.Family.CLOCKWORK_FACTORY,k.x,k.z));}}
    static Optional<Decoded> read(CompoundTag n,BlockPos at){
        for(String key:List.of("Version","AnchorX","AnchorZ","Slot","Clutches"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();
        if(n.getInt("Version")!=1||!n.hasUUID("Site")||!n.hasUUID("Generation")||!n.contains("Seed",Tag.TAG_LONG)||n.getInt("Slot")<0||n.getInt("Slot")>3||!Set.of(0,1,3,7).contains(n.getInt("Clutches"))||n.contains("NextBuild")&&(!n.contains("NextBuild",Tag.TAG_LONG)||n.getLong("NextBuild")<0))return Optional.empty();
        if(n.contains("PulseUntil")&&(!n.contains("PulseUntil",Tag.TAG_LONG)||n.getLong("PulseUntil")<0))return Optional.empty();
        var candidate=resolve(n);
        return candidate.filter(p->p.site().equals(n.getUUID("Site"))&&part(p,n.getInt("Slot")).equals(at)).map(p->new Decoded(p,n.getInt("Slot")));
    }
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan plan,int module){
        if(plan.family()!=InfiniteStructurePlan.Family.CLOCKWORK_FACTORY)return;var room=plan.modules().get(module);
        for(int slot=0;slot<4;slot++){var at=part(plan,slot);if(!clip.isInside(at)||at.getX()<plan.x()+room.x()||at.getX()>plan.x()+room.maxX()||at.getZ()<plan.z()+room.z()||at.getZ()>plan.z()+room.maxZ()||at.getY()!=plan.y()+room.y()+1||level.getBlockEntity(at)!=null)continue;
            level.setBlock(at,com.coraline.registry.ModBlocks.FACTORY_CONTROL.get().defaultBlockState().setValue(FactoryControlBlock.PART,slot),2);
            if(level.getBlockEntity(at) instanceof FactoryControlEntity control)control.configure(plan,slot);
        }
    }
    static FactoryControlEntity core(Level level,InfiniteStructurePlan.Plan plan){var at=part(plan,0);return level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof FactoryControlEntity core&&core.authentic()&&core.site().equals(plan.site())?core:null;}
    static boolean cacheReady(Level level,CompoundTag receipt){if(!receipt.getString("Family").equals(InfiniteStructurePlan.Family.CLOCKWORK_FACTORY.name())||!receipt.contains("FactoryProductionVersion"))return true;if(level==null)return false;var plan=resolve(receipt).orElse(null);if(plan==null)return false;var core=core(level,plan);return core!=null&&core.complete();}
    private FactoryLayout(){}
}
