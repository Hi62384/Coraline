package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class PerformanceLayout {
    private record Key(UUID generation,long seed,int x,int z){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> entry){return size()>128;}};
    static BlockPos core(InfiniteStructurePlan.Plan plan){return new BlockPos(plan.x()-8,plan.y()+1,plan.z());}
    static List<BlockPos> tiles(InfiniteStructurePlan.Plan plan){return List.of(new BlockPos(plan.x()-2,plan.y()+1,plan.z()+4),new BlockPos(plan.x()+2,plan.y()+1,plan.z()+4),new BlockPos(plan.x()+2,plan.y()+1,plan.z()+8),new BlockPos(plan.x()-2,plan.y()+1,plan.z()+8));}
    static int cue(InfiniteStructurePlan.Plan plan,int index){return (new int[]{0,2,1,3,2,0,3,1}[index]+plan.variation())&3;}
    static CompoundTag metadata(InfiniteStructurePlan.Plan plan){var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Generation",plan.generation());n.putUUID("Site",plan.site());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Phase",0);n.putInt("Cue",0);n.putInt("Cursor",0);n.putLong("LastTick",-1);return n;}
    static Optional<InfiniteStructurePlan.Plan> read(CompoundTag n){for(String key:List.of("Version","AnchorX","AnchorZ"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();if(n.getInt("Version")!=1||!n.hasUUID("Generation")||!n.hasUUID("Site")||!n.contains("Seed",Tag.TAG_LONG))return Optional.empty();var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"));Optional<InfiniteStructurePlan.Plan> found;synchronized(CACHE){found=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,InfiniteStructurePlan.Family.GRAND_THEATRE,k.x,k.z));}return found.filter(p->p.site().equals(n.getUUID("Site")));}
    static boolean freed(Level level,CompoundTag record){var plan=read(record).orElse(null);if(plan==null||level==null)return false;var at=core(plan);return level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof PerformanceEntity stage&&stage.complete();}
    static boolean cacheReady(Level level,CompoundTag receipt){return !receipt.getString("Family").equals(InfiniteStructurePlan.Family.GRAND_THEATRE.name())||freed(level,receipt);}
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan plan,int index){
        var room=plan.modules().get(index);if(room.role()!=InfiniteStructurePlan.Role.THEATRE_HALL||plan.family()!=InfiniteStructurePlan.Family.GRAND_THEATRE)return;
        for(int x=-9;x<=9;x++)for(int z=2;z<=9;z++)put(level,clip,new BlockPos(plan.x()+x,plan.y()+1,plan.z()+z),Blocks.SMOOTH_QUARTZ.defaultBlockState());
        for(int x=-1;x<=1;x++)put(level,clip,new BlockPos(plan.x()+x,plan.y()+1,plan.z()+1),Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH));
        for(int z=-9;z<=-3;z+=2)for(int x:new int[]{-7,-5,-3,3,5,7})put(level,clip,new BlockPos(plan.x()+x,plan.y()+1,plan.z()+z),Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH));
        for(int x:new int[]{-9,9})for(int y=2;y<=8;y++)for(int z=2;z<=9;z++)put(level,clip,new BlockPos(plan.x()+x,plan.y()+y,plan.z()+z),Blocks.PURPLE_WOOL.defaultBlockState());
        for(int z:new int[]{3,8}){for(int x=-8;x<=8;x++)put(level,clip,new BlockPos(plan.x()+x,plan.y()+10,plan.z()+z),Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X));for(int x:new int[]{-7,7}){put(level,clip,new BlockPos(plan.x()+x,plan.y()+9,plan.z()+z),Blocks.CHAIN.defaultBlockState());put(level,clip,new BlockPos(plan.x()+x,plan.y()+8,plan.z()+z),Blocks.SEA_LANTERN.defaultBlockState());}}
        var tiles=tiles(plan);for(int i=0;i<tiles.size();i++)put(level,clip,tiles.get(i),com.coraline.registry.ModBlocks.PERFORMANCE_TILE.get().defaultBlockState().setValue(PerformanceTileBlock.COLOR,i));
        var at=core(plan);if(clip.isInside(at)&&level.getBlockEntity(at)==null){level.setBlock(at,com.coraline.registry.ModBlocks.PERFORMANCE_CONTROL.get().defaultBlockState(),2);if(level.getBlockEntity(at) instanceof PerformanceEntity stage)stage.configure(plan);}
    }
    private static void put(WorldGenLevel level,BoundingBox clip,BlockPos at,net.minecraft.world.level.block.state.BlockState state){if(clip.isInside(at)&&level.getBlockEntity(at)==null)level.setBlock(at,state,2);}
    private PerformanceLayout(){}
}
