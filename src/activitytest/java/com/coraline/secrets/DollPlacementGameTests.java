package com.coraline.secrets;

import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class DollPlacementGameTests {
    @GameTest(template="empty") public static void legacySpySitsAboveFurniture(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var at=p.blockPosition();var level=p.serverLevel();
        level.setBlock(at.below(),Blocks.SPRUCE_PLANKS.defaultBlockState(),2);
        level.setBlock(at,Blocks.AIR.defaultBlockState(),2);
        var doll=EntityType.ITEM_DISPLAY.create(level);doll.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
        h.assertTrue(SecretSpies.repairDollSeat(p,doll),"Legacy seat not repaired");
        h.assertTrue(doll.getY()==at.getY()+.5,"Item display still clips into support");
        var pos=doll.position();SecretSpies.repairDollSeat(p,doll);
        h.assertTrue(doll.position().equals(pos),"Repeated repair drifts upwards");h.succeed();
    }
    @GameTest(template="empty") public static void furnitureOverlapMovesDollWithoutDeletingBlocks(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var at=p.blockPosition();var level=p.serverLevel();
        level.setBlock(at.below(),Blocks.STONE.defaultBlockState(),2);
        level.setBlock(at,Blocks.BARREL.defaultBlockState(),2);
        level.setBlock(at.above(),Blocks.AIR.defaultBlockState(),2);
        var doll=EntityType.ITEM_DISPLAY.create(level);doll.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
        h.assertTrue(SecretSpies.repairDollSeat(p,doll)&&doll.getY()==at.getY()+1.5,"Doll remained embedded in furniture");
        h.assertTrue(level.getBlockState(at).is(Blocks.BARREL),"Repair destroyed furniture");h.succeed();
    }
}
