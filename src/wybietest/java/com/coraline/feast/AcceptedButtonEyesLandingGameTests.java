package com.coraline.feast;

import com.coraline.otherworld.OtherWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

/** Carpeted hall over a basement: keep the arrival on the same safe floor. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class AcceptedButtonEyesLandingGameTests {
    @GameTest(template="empty") public static void carpetedArrivalFindsNearbyDryFloorRatherThanBasement(GameTestHelper h){
        var l=h.getLevel();var near=h.absolutePos(new BlockPos(60,30,60));
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)for(int y=-9;y<=9;y++){
            var at=near.offset(x,y,z);l.getChunkAt(at);saved.put(at,l.getBlockState(at));l.setBlock(at,Blocks.AIR.defaultBlockState(),2);
        }
        try{
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
                l.setBlock(near.offset(x,-1,z),Blocks.OAK_PLANKS.defaultBlockState(),2);
                if(Math.abs(x)<=1&&Math.abs(z)<=1)l.setBlock(near.offset(x,0,z),Blocks.GRAY_CARPET.defaultBlockState(),2);
            }
            l.setBlock(near.below(7),Blocks.STONE_BRICKS.defaultBlockState(),2);
            h.assertTrue(OtherWorld.safePlayer(l,near).equals(near.below(6)),"Fixture did not reproduce column-first basement selection");
            var result=AcceptedButtonEyesWorld.landing(l,near);
            h.assertTrue(result!=null&&result.getY()==near.getY()&&result.distSqr(near)<=5,"Carpeted landing did not retain a nearby clear same-floor arrival");
            h.assertTrue(l.isEmptyBlock(result)&&l.isEmptyBlock(result.above())&&l.getFluidState(result).isEmpty(),"Arrival overlaps blocks or fluid");
            h.assertTrue(l.getBlockState(near).is(Blocks.GRAY_CARPET)&&l.getBlockState(near.below(7)).is(Blocks.STONE_BRICKS),"Arrival selection modified house geometry");
            for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)for(int y=-9;y<=9;y++)l.setBlock(near.offset(x,y,z),Blocks.STONE_BRICKS.defaultBlockState(),2);
            h.assertTrue(AcceptedButtonEyesWorld.landing(l,near)==null,"Fully blocked landing escaped the bounded arrival area");
        }finally{saved.forEach((at,state)->l.setBlock(at,state,2));}
        h.succeed();
    }
    private AcceptedButtonEyesLandingGameTests(){}
}
