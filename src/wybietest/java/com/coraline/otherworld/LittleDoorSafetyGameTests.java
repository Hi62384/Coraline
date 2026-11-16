package com.coraline.otherworld;

import com.coraline.block.TinyDoorBlock;
import com.coraline.registry.ModBlocks;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class LittleDoorSafetyGameTests {
    @GameTest(template="empty",timeoutTicks=110)
    public static void flowingWaterCannotWashAwayAnyDoorFacingOrOpenState(GameTestHelper h){
        var l=h.getLevel();var doors=new LinkedHashMap<BlockPos,UUID>();int index=0;
        for(var facing:Direction.Plane.HORIZONTAL)for(boolean open:new boolean[]{false,true}){
            var at=h.absolutePos(new BlockPos(2+(index%4)*6,3,2+(index/4)*6));index++;
            l.setBlock(at.below(),Blocks.STONE.defaultBlockState(),3);
            var state=ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,facing).setValue(TinyDoorBlock.OPEN,open).setValue(TinyDoorBlock.PORTAL,open);
            l.setBlock(at,state,3);var door=(DoorBlockEntity)l.getBlockEntity(at);door.doorId=UUID.randomUUID();doors.put(at,door.doorId);
            var block=(LiquidBlockContainer)state.getBlock();
            h.assertFalse(block.canPlaceLiquid(l,at,state,Fluids.WATER)||block.canPlaceLiquid(l,at,state,Fluids.LAVA),"Door accepts bucket/flow replacement");
            h.assertFalse(block.placeLiquid(l,at,state,Fluids.WATER.defaultFluidState()),"Water replaced the door directly");
            l.setBlock(at.above(),Blocks.WATER.defaultBlockState(),3);
        }
        h.runAtTickTime(90,()->{
            for(var entry:doors.entrySet()){
                var state=l.getBlockState(entry.getKey());
                h.assertTrue(state.is(ModBlocks.TINY_DOOR.get()),"Flowing water destroyed a Little Door at "+entry.getKey());
                h.assertTrue(l.getBlockEntity(entry.getKey()) instanceof DoorBlockEntity door&&entry.getValue().equals(door.doorId),"Water reset the persistent door identity");
                l.removeBlock(entry.getKey().above(),false);l.removeBlock(entry.getKey(),false);
            }
            h.succeed();
        });
    }
}
