package com.coraline.feast;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class MantisTerrainGameTests {
    @GameTest(template="empty") public static void stepOntoBottomSlab(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(2,2,2));var level=h.getLevel();
        level.setBlock(at,Blocks.STONE_SLAB.defaultBlockState(),2);
        var feet=RescueBosses.mantisSurface(level,Vec3.atBottomCenterOf(at),at.getY());
        h.assertTrue(feet!=null&&Math.abs(feet.y-(at.getY()+.5))<.001,"Mantis rejected the bottom slab collision top");h.succeed();
    }
    @GameTest(template="empty") public static void crossWaterWithoutSolidLakeBottom(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(2,2,2));var level=h.getLevel();
        level.setBlock(at,Blocks.WATER.defaultBlockState(),2);
        var feet=RescueBosses.mantisSurface(level,Vec3.atBottomCenterOf(at),at.getY()+1);
        h.assertTrue(feet!=null&&feet.y>at.getY()+.8,"Mantis refused a water surface without a sturdy block underneath");h.succeed();
    }
    @GameTest(template="empty") public static void doNotStepOffUnsupportedCliff(GameTestHelper h){
        var at=h.absolutePos(new BlockPos(2,5,2));var level=h.getLevel();
        for(int y=-2;y<=1;y++)level.setBlock(at.above(y),Blocks.AIR.defaultBlockState(),2);
        h.assertTrue(RescueBosses.mantisSurface(level,Vec3.atBottomCenterOf(at),at.getY())==null,"Mantis accepted unsupported air");h.succeed();
    }
}