package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

final class CarnivalLayout {
    private record Key(UUID generation,long seed,int x,int z){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> entry){return size()>128;}};
    static BlockPos core(InfiniteStructurePlan.Plan plan){return new BlockPos(plan.x()-7,plan.y()+1,plan.z()-2);}
    static int expected(InfiniteStructurePlan.Plan plan,int progress){return (new int[]{0,2,1,3,2,0,3,1}[progress]+plan.variation())&3;}
    static CompoundTag metadata(InfiniteStructurePlan.Plan plan){var n=new CompoundTag();n.putInt("Version",1);n.putUUID("Generation",plan.generation());n.putUUID("Site",plan.site());n.putLong("Seed",plan.seed());n.putInt("AnchorX",plan.x());n.putInt("AnchorZ",plan.z());n.putInt("Phase",0);n.putInt("Score",0);n.putInt("Ticks",0);n.putLong("LastTick",-1);n.putLong("RetryAfter",0);n.put("Targets",new ListTag());return n;}
    static Optional<InfiniteStructurePlan.Plan> read(CompoundTag n){for(String key:List.of("Version","AnchorX","AnchorZ"))if(!n.contains(key,Tag.TAG_INT))return Optional.empty();if(n.getInt("Version")!=1||!n.hasUUID("Generation")||!n.hasUUID("Site")||!n.contains("Seed",Tag.TAG_LONG))return Optional.empty();var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"));Optional<InfiniteStructurePlan.Plan> found;synchronized(CACHE){found=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,InfiniteStructurePlan.Family.ENDLESS_CARNIVAL,k.x,k.z));}return found.filter(p->p.site().equals(n.getUUID("Site")));}
    static boolean freed(Level level,CompoundTag record){var plan=read(record).orElse(null);if(plan==null||level==null)return false;var at=core(plan);return level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof CarnivalEntity game&&game.complete();}
    static boolean cacheReady(Level level,CompoundTag receipt){return !receipt.getString("Family").equals(InfiniteStructurePlan.Family.ENDLESS_CARNIVAL.name())||freed(level,receipt);}
    static void place(WorldGenLevel level,BoundingBox clip,InfiniteStructurePlan.Plan plan,int module){
        if(plan.family()!=InfiniteStructurePlan.Family.ENDLESS_CARNIVAL||plan.modules().get(module).role()!=InfiniteStructurePlan.Role.CARNIVAL_GALLERY)return;
        for(int x=-5;x<=5;x++)put(level,clip,new BlockPos(plan.x()+x,plan.y(),plan.z()-2),Blocks.YELLOW_CONCRETE.defaultBlockState());
        for(int x=-8;x<=8;x++)for(int z=-5;z<=11;z++)put(level,clip,new BlockPos(plan.x()+x,plan.y()+8,plan.z()+z),(Math.floorMod(x,4)<2?Blocks.PINK_WOOL:Blocks.WHITE_WOOL).defaultBlockState());
        for(int x:new int[]{-7,7})for(int z:new int[]{0,10})put(level,clip,new BlockPos(plan.x()+x,plan.y()+7,plan.z()+z),Blocks.SEA_LANTERN.defaultBlockState());
        var at=core(plan);if(clip.isInside(at)&&level.getBlockEntity(at)==null){level.setBlock(at,com.coraline.registry.ModBlocks.CARNIVAL_CONTROL.get().defaultBlockState(),2);if(level.getBlockEntity(at) instanceof CarnivalEntity game)game.configure(plan);}
    }
    private static void put(WorldGenLevel level,BoundingBox clip,BlockPos at,net.minecraft.world.level.block.state.BlockState state){if(clip.isInside(at)&&level.getBlockEntity(at)==null)level.setBlock(at,state,2);}
    private CarnivalLayout(){}
}
