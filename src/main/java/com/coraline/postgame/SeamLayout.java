package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class SeamLayout {
    record Decoded(InfiniteStructurePlan.Plan plan,int slot){}
    private record Key(UUID generation,long seed,int x,int z,InfiniteStructurePlan.Family family){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> entry){return size()>128;}};
    static boolean family(InfiniteStructurePlan.Family family){return family==InfiniteStructurePlan.Family.DIMENSIONAL_SEAM||family==InfiniteStructurePlan.Family.FALSE_PINK_PALACE;}
    static List<BlockPos> parts(InfiniteStructurePlan.Plan plan){if(plan.family()==InfiniteStructurePlan.Family.FALSE_PINK_PALACE)return List.of(new BlockPos(plan.x()-4,plan.y()+9,plan.z()+6),new BlockPos(plan.x()-6,plan.y()+9,plan.z()-6),new BlockPos(plan.x()+6,plan.y()+9,plan.z()-6),new BlockPos(plan.x(),plan.y()+1,plan.z()+6));if(plan.family()!=InfiniteStructurePlan.Family.DIMENSIONAL_SEAM)return List.of();var room=plan.modules().stream().filter(m->m.role()==InfiniteStructurePlan.Role.SEAM_CHAMBER).findFirst().orElseThrow();var core=new BlockPos(plan.x()+room.x()+room.width()/2,plan.y()+room.y()+1,plan.z()+room.z()+room.depth()/2);return List.of(core,core.west(4),core.east(4),core.north(4));}
    static int expected(InfiniteStructurePlan.Plan plan,int slot){return (plan.variation()+slot)&3;}
    static CompoundTag metadata(InfiniteStructurePlan.Plan plan,int slot){var n=new CompoundTag();n.putInt("Version",1);n.putString("Family",plan.family().name());n.putUUID("Generation",plan.generation());n.putUUID("Site",plan.site());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Slot",slot);n.putInt("Rotation",slot==0?0:(expected(plan,slot)+1+(slot%3))&3);if(slot==0)n.putBoolean("Closed",false);return n;}
    static Optional<Decoded> read(CompoundTag n,BlockPos at){
        for(String key:List.of("Version","AnchorX","AnchorZ","Slot","Rotation"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();
        if(n.getInt("Version")!=1||!n.hasUUID("Generation")||!n.hasUUID("Site")||!n.contains("Seed",Tag.TAG_LONG)||n.getInt("Rotation")<0||n.getInt("Rotation")>3)return Optional.empty();
        InfiniteStructurePlan.Family family;try{if(n.contains("Family")&&!n.contains("Family",Tag.TAG_STRING))return Optional.empty();family=n.contains("Family")?InfiniteStructurePlan.Family.valueOf(n.getString("Family")):InfiniteStructurePlan.Family.DIMENSIONAL_SEAM;}catch(IllegalArgumentException bad){return Optional.empty();}if(!family(family))return Optional.empty();
        var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"),family);Optional<InfiniteStructurePlan.Plan> found;
        synchronized(CACHE){found=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,k.family,k.x,k.z));}
        if(found.isEmpty())return Optional.empty();var plan=found.orElseThrow();int slot=n.getInt("Slot");var parts=parts(plan);
        return plan.site().equals(n.getUUID("Site"))&&slot>=0&&slot<parts.size()&&parts.get(slot).equals(at)?Optional.of(new Decoded(plan,slot)):Optional.empty();
    }
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan plan){
        var parts=parts(plan);for(int slot=0;slot<parts.size();slot++){
            var at=parts.get(slot);if(clip.isInside(at)&&level.getBlockEntity(at)==null){
                level.setBlock(at,(slot==0?com.coraline.registry.ModBlocks.SEAM_CORE:com.coraline.registry.ModBlocks.SEAM_DIAL).get().defaultBlockState().setValue(SeamAnchorBlock.ROTATION,metadata(plan,slot).getInt("Rotation")),2);
                if(level.getBlockEntity(at) instanceof SeamAnchorEntity entity)entity.configure(plan,slot);
            }
            if(slot>0){var marker=at.below().relative(Direction.fromYRot(expected(plan,slot)*90));if(clip.isInside(marker)&&level.getBlockEntity(marker)==null)level.setBlock(marker,Blocks.SEA_LANTERN.defaultBlockState(),2);}
        }
    }
    private SeamLayout(){}
}
