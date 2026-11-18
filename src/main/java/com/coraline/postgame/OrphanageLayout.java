package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Three physical institutional records attest the release, not a timer. */
final class OrphanageLayout {
    private record Key(UUID generation,long seed,int x,int z){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> e){return size()>128;}};
    static BlockPos register(InfiniteStructurePlan.Plan p,int slot){return new BlockPos(p.x()+(slot==0?-8:slot==1?8:0),p.y()+1,p.z()+(slot==2?17:slot==3?0:7));}
    static CompoundTag metadata(InfiniteStructurePlan.Plan p,int slot){var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Generation",p.generation());n.putUUID("Site",p.site());n.putLong("Seed",p.seed());n.putInt("AnchorX",p.x());n.putInt("AnchorZ",p.z());n.putInt("Slot",slot);n.putInt("Evidence",0);n.putBoolean("Released",false);return n;}
    static Optional<InfiniteStructurePlan.Plan> read(CompoundTag n){for(String key:List.of("Version","AnchorX","AnchorZ"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();if(n.getInt("Version")!=1||!n.hasUUID("Generation")||!n.hasUUID("Site")||!n.contains("Seed",Tag.TAG_LONG))return Optional.empty();var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"));Optional<InfiniteStructurePlan.Plan> found;synchronized(CACHE){found=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE,k.x,k.z));}return found.filter(p->p.site().equals(n.getUUID("Site")));}
    static boolean freed(Level level,CompoundTag n){var p=read(n).orElse(null);if(level==null||p==null)return false;var at=register(p,3);return level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof OrphanageEntity root&&root.complete();}
    static boolean cacheReady(Level level,CompoundTag n){
        if(!n.getString("Family").equals(InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE.name()))return true;
        var plan=read(n).orElse(null);
        if(plan==null||level==null)return false;
        var at=register(plan,3);
        return level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof OrphanageEntity root&&root.bindingsCleared();
    }
    static List<BlockPos> bars(InfiniteStructurePlan.Plan p){var out=new ArrayList<BlockPos>();for(var room:p.modules())if(room.role()==InfiniteStructurePlan.Role.ORPHAN_DAIS)for(int y=1;y<=3;y++)for(int x=0;x<5;x++)for(int z=0;z<7;z++)if(x==0||x==4||z==0||z==6)out.add(new BlockPos(p.x()+room.x()+x,p.y()+y,p.z()+room.z()+z));return out;}
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan p,int module){
        if(p.family()!=InfiniteStructurePlan.Family.ABANDONED_ORPHANAGE)return;var room=p.modules().get(module);int slot=room.role()==InfiniteStructurePlan.Role.ORPHAN_OFFICE?3:room.role()==InfiniteStructurePlan.Role.ORPHAN_RECORD?module-1:-1;
        if(slot>=0&&slot<=3){var at=register(p,slot);if(clip.isInside(at)&&level.getBlockEntity(at)==null){level.setBlock(at,com.coraline.registry.ModBlocks.ORPHANAGE_REGISTER.get().defaultBlockState().setValue(OrphanageBlock.SLOT,slot),2);if(level.getBlockEntity(at) instanceof OrphanageEntity record)record.configure(p,slot);}}
        if(room.role()==InfiniteStructurePlan.Role.ORPHAN_DAIS){
            for(var at:bars(p))if(clip.isInside(at)&&at.getX()>=p.x()+room.x()&&at.getX()<p.x()+room.x()+5)level.setBlock(at,com.coraline.registry.ModBlocks.ORPHANAGE_BINDING.get().defaultBlockState(),2);
            var bed=new BlockPos(p.x()+room.x()+1,p.y()+1,p.z()+room.z()+2);put(level,clip,bed,Blocks.GRAY_BED.defaultBlockState().setValue(BedBlock.FACING,net.minecraft.core.Direction.SOUTH));put(level,clip,bed.south(),Blocks.GRAY_BED.defaultBlockState().setValue(BedBlock.FACING,net.minecraft.core.Direction.SOUTH).setValue(BedBlock.PART,net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        }
    }
    private static void put(WorldGenLevel level,BoundingBox clip,BlockPos at,net.minecraft.world.level.block.state.BlockState state){if(clip.isInside(at)&&level.getBlockEntity(at)==null)level.setBlock(at,state,2);}
    private OrphanageLayout(){}
}
