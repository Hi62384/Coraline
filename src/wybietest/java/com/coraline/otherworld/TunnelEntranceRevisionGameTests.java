package com.coraline.otherworld;

import com.coraline.block.TinyDoorBlock;
import com.coraline.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TunnelEntranceRevisionGameTests {
    private static DoorBlockEntity door(GameTestHelper h,BlockPos at,Direction facing){var level=h.getLevel();level.setBlock(at,ModBlocks.TINY_DOOR.get().defaultBlockState().setValue(TinyDoorBlock.FACING,facing).setValue(TinyDoorBlock.OPEN,true).setValue(TinyDoorBlock.PORTAL,true),3);var d=(DoorBlockEntity)level.getBlockEntity(at);d.doorId=UUID.randomUUID();return d;}
    @GameTest(template="empty") public static void freestandingEntrancesNeverPaintTheSurroundingsBlue(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(12,5,12));
        for(var facing:Direction.Plane.HORIZONTAL){var at=center.relative(facing,8);var d=door(h,at,facing);h.assertTrue(TunnelPassage.ensure(level,d),"Entrance couldn't open");
            for(int depth=1;depth<=3;depth++)for(int side=-1;side<=1;side++)for(int y=-1;y<=1;y++)h.assertFalse(level.getBlockState(at.relative(facing,-depth).relative(facing.getClockWise(),side).offset(0,y,0)).is(ModBlocks.TUNNEL_FABRIC.get()),"Visible fabric cuboid created");
            TunnelPassage.restore(level,d);level.removeBlock(at,false);
        }h.succeed();
    }
    @GameTest(template="empty") public static void blueLiningRetainsCollisionAndChaseFabricRetainsItsModel(GameTestHelper h){
        var fabric=ModBlocks.TUNNEL_FABRIC.get().defaultBlockState();
        var lined=fabric.setValue(ModBlocks.TUNNEL_LINED,true);
        var at=h.absolutePos(new BlockPos(2,3,2));
        h.assertTrue(lined.getRenderShape()==net.minecraft.world.level.block.RenderShape.INVISIBLE,"A voxel model would intersect the curved blue lining");
        h.assertTrue(!lined.getCollisionShape(h.getLevel(),at).isEmpty(),"Hiding blue backing removed the crawl collision");
        h.assertTrue(fabric.getRenderShape()==net.minecraft.world.level.block.RenderShape.MODEL,"The later chase lost its visible fabric blocks");
        h.succeed();
    }
    @GameTest(template="empty") public static void legacyLiningMigratesButPlayerChangesAreNotOverwritten(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(5,5,5));var d=door(h,at,Direction.NORTH);var old=at.south().east().above();
        var entry=new CompoundTag();entry.putLong("Pos",old.asLong());entry.put("Original",NbtUtils.writeBlockState(Blocks.AIR.defaultBlockState()));entry.put("Placed",NbtUtils.writeBlockState(ModBlocks.TUNNEL_FABRIC.get().defaultBlockState()));d.passageOriginals.add(entry);d.passageBuilt=true;d.passageVersion=0;level.setBlock(old,ModBlocks.TUNNEL_FABRIC.get().defaultBlockState(),2);
        h.assertTrue(TunnelPassage.ensure(level,d)&&level.isEmptyBlock(old)&&d.passageVersion==2,"Saved blue lining wasn't repaired");
        var edit=at.south(2);level.setBlock(edit,Blocks.GOLD_BLOCK.defaultBlockState(),2);TunnelPassage.restore(level,d);h.assertTrue(level.getBlockState(edit).is(Blocks.GOLD_BLOCK),"Restoration overwrote a later player edit");h.succeed();
    }
}
