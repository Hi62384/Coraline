package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** A distorted two-storey pink house, misleading mirrors and a real cage route.
 * This structure never creates a replacement for the original Wybie. */
final class FalsePalaceLayout {
    private record Key(UUID generation,long seed,int x,int z){}
    private static final Map<Key,Optional<InfiniteStructurePlan.Plan>> CACHE=new LinkedHashMap<>(128,.75F,true){@Override protected boolean removeEldestEntry(Map.Entry<Key,Optional<InfiniteStructurePlan.Plan>> e){return size()>128;}};
    static Optional<InfiniteStructurePlan.Plan> read(CompoundTag n){if(!n.hasUUID("Generation")||!n.hasUUID("Site")||!n.contains("Seed",net.minecraft.nbt.Tag.TAG_LONG)||!n.contains("AnchorX",net.minecraft.nbt.Tag.TAG_INT)||!n.contains("AnchorZ",net.minecraft.nbt.Tag.TAG_INT))return Optional.empty();var key=new Key(n.getUUID("Generation"),n.getLong("Seed"),n.getInt("AnchorX"),n.getInt("AnchorZ"));synchronized(CACHE){return CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,InfiniteStructurePlan.Family.FALSE_PINK_PALACE,k.x,k.z)).filter(p->p.site().equals(n.getUUID("Site")));}}
    static List<BlockPos> bindings(InfiniteStructurePlan.Plan p){var out=new ArrayList<BlockPos>();for(int x=-2;x<=2;x++)for(int z=4;z<=8;z++)for(int y=9;y<=12;y++)if(x==-2||x==2||z==4||z==8||y==12)out.add(new BlockPos(p.x()+x,p.y()+y,p.z()+z));return out;}
    static boolean open(Level level,InfiniteStructurePlan.Plan p){var at=SeamLayout.parts(p).get(0);return level!=null&&level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof SeamAnchorEntity root&&root.authentic()&&root.closed()&&p.site().equals(root.site());}
    static boolean cacheReady(Level level,CompoundTag receipt){if(!receipt.getString("Family").equals(InfiniteStructurePlan.Family.FALSE_PINK_PALACE.name()))return true;if(!receipt.hasUUID("Generation")||!receipt.hasUUID("Site")||!receipt.contains("Seed",net.minecraft.nbt.Tag.TAG_LONG)||!receipt.contains("AnchorX",net.minecraft.nbt.Tag.TAG_INT)||!receipt.contains("AnchorZ",net.minecraft.nbt.Tag.TAG_INT))return false;var key=new Key(receipt.getUUID("Generation"),receipt.getLong("Seed"),receipt.getInt("AnchorX"),receipt.getInt("AnchorZ"));Optional<InfiniteStructurePlan.Plan> found;synchronized(CACHE){found=CACHE.computeIfAbsent(key,k->InfiniteStructurePlan.at(k.generation,k.seed,InfiniteStructurePlan.Family.FALSE_PINK_PALACE,k.x,k.z));}return found.isPresent()&&found.get().site().equals(receipt.getUUID("Site"))&&open(level,found.get());}
    static void release(Level level,InfiniteStructurePlan.Plan p){if(!open(level,p))return;for(var at:bindings(p))if(level.hasChunkAt(at)&&level.getBlockState(at).is(com.coraline.registry.ModBlocks.ORPHANAGE_BINDING.get()))level.removeBlock(at,false);var at=SiteCacheReceipt.position(p,0);if(level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof SiteCacheBlockEntity cache)cache.unlock(p.site());}
    static void place(WorldGenLevel l,BoundingBox clip,InfiniteStructurePlan.Plan p,int module){
        if(p.family()!=InfiniteStructurePlan.Family.FALSE_PINK_PALACE||p.modules().get(module).role()!=InfiniteStructurePlan.Role.FALSE_PALACE_HALL)return;var wood=Blocks.DARK_OAK_PLANKS.defaultBlockState();var pink=Blocks.PINK_TERRACOTTA.defaultBlockState();
        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)if(!(x>=-1&&x<=1&&z>=-5&&z<=2))put(l,clip,new BlockPos(p.x()+x,p.y()+8,p.z()+z),wood);
        for(int floor:new int[]{0,8})for(int x:new int[]{-4,4})for(int z=-10;z<=10;z++)for(int y=1;y<=7;y++)if(!(Math.abs(z+6)<=1&&y<=2||Math.abs(z-6)<=1&&y<=2))put(l,clip,new BlockPos(p.x()+x,p.y()+floor+y,p.z()+z),pink);
        for(int step=0;step<8;step++)for(int x=-1;x<=1;x++)put(l,clip,new BlockPos(p.x()+x,p.y()+step+1,p.z()-5+step),Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH));
        for(int x=-11;x<=11;x++)for(int z=-11;z<=11;z++)put(l,clip,new BlockPos(p.x()+x,p.y()+17+Math.min(5,11-Math.abs(x)),p.z()+z),Blocks.PURPLE_TERRACOTTA.defaultBlockState());
        for(int x:new int[]{-11,11})for(int z:new int[]{-11,11})for(int y=1;y<=17;y++)put(l,clip,new BlockPos(p.x()+x,p.y()+y,p.z()+z),Blocks.QUARTZ_PILLAR.defaultBlockState());
        for(int x:new int[]{-8,-6,0,6,8})for(int floor:new int[]{0,8})for(int y=3;y<=5;y++)for(int z:new int[]{-11,11})put(l,clip,new BlockPos(p.x()+x,p.y()+floor+y,p.z()+z),Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
        for(int floor:new int[]{0,8})for(int x:new int[]{-7,7})for(int z:new int[]{-7,7})put(l,clip,new BlockPos(p.x()+x,p.y()+floor+6,p.z()+z),Blocks.SEA_LANTERN.defaultBlockState());
        // The upper east passage deliberately resembles the west hallway,
        // but its mirror is passable rather than a misleading solid lock.
        for(int y=9;y<=10;y++)put(l,clip,new BlockPos(p.x()+4,p.y()+y,p.z()+6),InfiniteContent.ILLUSION_MIRROR.get().defaultBlockState());
        for(var at:bindings(p))put(l,clip,at,com.coraline.registry.ModBlocks.ORPHANAGE_BINDING.get().defaultBlockState());
        for(int x=-2;x<=2;x++)for(int z=4;z<=8;z++)put(l,clip,new BlockPos(p.x()+x,p.y()+8,p.z()+z),InfiniteContent.block("buttonwood_log").defaultBlockState());
        SeamLayout.place(l,clip,p);
    }
    private static void put(WorldGenLevel l,BoundingBox clip,BlockPos at,BlockState state){if(clip.isInside(at)&&l.getBlockEntity(at)==null)l.setBlock(at,state,2);}
    private FalsePalaceLayout(){}
}
