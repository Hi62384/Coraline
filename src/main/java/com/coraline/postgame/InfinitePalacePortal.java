package com.coraline.postgame;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;
import com.coraline.block.TinyDoorBlock;
import com.coraline.otherworld.DoorBlockEntity;

/** Convert only the verified master's decorative recess, never an unrelated
 * placed door or a player's replacement blocks. The receipt precedes edits. */
final class InfinitePalacePortal {
    private static Map<BlockPos,BlockState> originals;
    private static Map<BlockPos,BlockState> originals(){
        if(originals==null){var at=com.coraline.otherworld.RosewoodHouse.palaceLittleDoor(BlockPos.ZERO);var wanted=Set.of(at,at.above(),at.east(),at.east().above());var out=new HashMap<BlockPos,BlockState>();for(var cell:com.coraline.otherworld.RosewoodHouse.palaceCells())if(wanted.contains(cell.offset()))out.put(cell.offset(),cell.state());originals=Map.copyOf(out);}return originals;
    }
    static void install(MinecraftServer server,InfiniteTransitData data,UUID generation){
        var target=InfiniteRealm.palaceDoor(server);if(target.isEmpty())return;var at=target.orElseThrow();var level=server.overworld();if(!level.hasChunkAt(at))return;
        if(level.getBlockEntity(at) instanceof DoorBlockEntity door&&door.hasPostgameBinding()){
            if(!generation.equals(door.postgameGeneration())&&door.doorId==null&&InfiniteWorldData.get(server).state().retired(door.postgameGeneration())&&data.palaceDoor(server,at)&&door.bindPostgame(generation)){var state=door.getBlockState();var next=state.setValue(TinyDoorBlock.PORTAL,true).setValue(TinyDoorBlock.OPEN,false);level.setBlock(at,next,3);level.sendBlockUpdated(at,state,next,3);}return;
        }
        var origin=com.coraline.palace.PalaceSiteData.get(server).position(com.coraline.palace.PalaceSitePlan.Kind.PALACE).orElseThrow();
        var cells=List.of(at,at.above(),at.east(),at.east().above());boolean recovery=data.ownsPalaceDoor(at);
        for(var cell:cells){
            if(!level.hasChunkAt(cell))return;var current=level.getBlockState(cell);var original=originals().getOrDefault(cell.subtract(origin),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            if(cell.equals(at)&&recovery&&current.is(com.coraline.registry.ModBlocks.TINY_DOOR.get())&&level.getBlockEntity(cell) instanceof DoorBlockEntity be&&be.doorId==null&&!be.hasPostgameBinding())continue;
            if(!cell.equals(at)&&recovery&&current.isAir()&&level.getBlockEntity(cell)==null)continue;
            if(current.isAir()&&level.getBlockEntity(cell)==null)continue;
            if(original==null||current.getBlock()!=original.getBlock()||level.getBlockEntity(cell)!=null)return;
        }
        if(!data.palaceDoor(server,at))return;
        for(var cell:cells)if(!cell.equals(at))level.setBlock(cell,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        var state=com.coraline.registry.ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,Direction.EAST).setValue(TinyDoorBlock.PORTAL,true).setValue(TinyDoorBlock.OPEN,false);
        level.setBlock(at,state,3);if(level.getBlockEntity(at) instanceof DoorBlockEntity door&&door.bindPostgame(generation))level.sendBlockUpdated(at,state,state,3);
    }
    static void extinguish(MinecraftServer server,UUID generation){var at=InfiniteRealm.palaceDoor(server).orElse(null);if(at!=null&&server.overworld().hasChunkAt(at)&&server.overworld().getBlockEntity(at) instanceof DoorBlockEntity door&&generation.equals(door.postgameGeneration()))door.extinguishRetiredPostgame();}
    private InfinitePalacePortal(){}
}
