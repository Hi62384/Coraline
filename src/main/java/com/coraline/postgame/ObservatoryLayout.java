package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class ObservatoryLayout {
    private record Key(UUID generation,long seed,int x,int z){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> entry){return size()>128;}};
    static BlockPos core(InfiniteStructurePlan.Plan plan){var hall=plan.modules().stream().filter(m->m.role()==InfiniteStructurePlan.Role.OBSERVATORY_HALL).findFirst().orElseThrow();return new BlockPos(plan.x()+hall.x()+hall.width()/2,plan.y()+hall.y()+1,plan.z()+hall.z()+hall.depth()/2);}
    static List<BlockPos> stars(InfiniteStructurePlan.Plan plan){var core=core(plan);return List.of(core.offset(-4,9,-4),core.offset(4,11,-4),core.offset(4,9,4),core.offset(-4,11,4));}
    static CompoundTag metadata(InfiniteStructurePlan.Plan plan){var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Generation",plan.generation());n.putUUID("Site",plan.site());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Observed",0);return n;}
    static Optional<InfiniteStructurePlan.Plan> read(CompoundTag n){
        for(String field:List.of("Version","AnchorX","AnchorZ"))if(!n.contains(field,Tag.TAG_INT))return Optional.empty();if(n.getInt("Version")!=1||!n.hasUUID("Generation")||!n.hasUUID("Site")||!n.contains("Seed",Tag.TAG_LONG))return Optional.empty();
        var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"));Optional<InfiniteStructurePlan.Plan> found;synchronized(CACHE){found=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,InfiniteStructurePlan.Family.FORGOTTEN_OBSERVATORY,k.x,k.z));}return found.filter(p->p.site().equals(n.getUUID("Site")));
    }
    static boolean cacheReady(Level level,CompoundTag receipt){if(!receipt.getString("Family").equals(InfiniteStructurePlan.Family.FORGOTTEN_OBSERVATORY.name()))return true;var plan=read(receipt).orElse(null);if(plan==null)return false;var at=core(plan);return level!=null&&level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof ObservatoryEntity telescope&&telescope.complete();}
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan plan,int index){
        var room=plan.modules().get(index);if(room.role()!=InfiniteStructurePlan.Role.OBSERVATORY_HALL||plan.family()!=InfiniteStructurePlan.Family.FORGOTTEN_OBSERVATORY)return;
        var at=core(plan);if(clip.isInside(at)&&level.getBlockEntity(at)==null){level.setBlock(at,com.coraline.registry.ModBlocks.OBSERVATORY_TELESCOPE.get().defaultBlockState(),2);if(level.getBlockEntity(at) instanceof ObservatoryEntity telescope)telescope.configure(plan);}
        var stars=stars(plan);for(int i=0;i<stars.size();i++)put(level,clip,stars.get(i),com.coraline.registry.ModBlocks.OBSERVATORY_STAR.get().defaultBlockState().setValue(ObservatoryStarBlock.COLOR,i));
        for(int x=2;x<=10;x++)for(int z=2;z<=10;z++)if((x-6)*(x-6)+(z-6)*(z-6)<=20)put(level,clip,new BlockPos(plan.x()+room.x()+x,plan.y()+room.y()+15,plan.z()+room.z()+z),Blocks.AIR.defaultBlockState());
        for(int edge=3;edge<=9;edge++)for(int y=8;y<=10;y++)for(var pos:new BlockPos[]{new BlockPos(plan.x()+room.x(),plan.y()+room.y()+y,plan.z()+room.z()+edge),new BlockPos(plan.x()+room.maxX(),plan.y()+room.y()+y,plan.z()+room.z()+edge)})put(level,clip,pos,Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
    }
    private static void put(WorldGenLevel level,BoundingBox clip,BlockPos at,net.minecraft.world.level.block.state.BlockState state){if(clip.isInside(at)&&level.getBlockEntity(at)==null)level.setBlock(at,state,2);}
    private ObservatoryLayout(){}
}
