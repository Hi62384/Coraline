package com.coraline.feast;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

/** Focused rerun of a failed native case; unchanged passing families remain separate. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GhostFurnitureApproachGameTests {
    @GameTest(template="empty") public static void furnitureUsesAReachableApproachWithoutInventingArrival(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(80,30,80));
        var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(int x=-2;x<=7;x++)for(int z=-3;z<=3;z++)for(int y=-1;y<=2;y++){
            var at=base.offset(x,y,z);level.getChunkAt(at);saved.put(at,level.getBlockState(at));level.setBlock(at,(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        }
        var pig=EntityType.PIG.create(level);pig.setNoAi(true);pig.setPos(Vec3.atBottomCenterOf(base));level.addFreshEntity(pig);
        // NoAI freezes idle physics. Establish contact through Minecraft's
        // collision engine, never by assigning an invented onGround flag.
        pig.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,-.05,0));
        try{
            h.assertTrue(pig.onGround(),"Furniture fixture did not settle on its real floor");
            var target=base.east(5);level.setBlock(target,Blocks.OAK_PLANKS.defaultBlockState(),2);level.setBlock(target.above(),Blocks.OAK_PLANKS.defaultBlockState(),2);
            var start=pig.position();var approach=GhostVictimNavigation.approach(pig,target);
            h.assertTrue(approach!=null&&!approach.equals(target)&&approach.distSqr(target)<=4,"No actual standable approach to furniture");
            h.assertTrue(GhostVictimNavigation.clear(level,pig,approach),"Approach overlaps furniture");
            h.assertTrue(pig.position().equals(start),"Approach selection teleported the child");
            var journal=new CompoundTag();var result=GhostVictimNavigation.walk(pig,journal,approach,1,1,20,door->false);
            h.assertTrue(result==GhostVictimNavigation.Result.MOVING,"Planning a path counted as reaching the prop");
            result=GhostVictimNavigation.walk(pig,journal,approach,22,1,20,door->false);
            h.assertTrue(result==GhostVictimNavigation.Result.BLOCKED&&pig.position().equals(start),"Expired route fabricated arrival or warped");
        }finally{pig.discard();saved.forEach((at,state)->level.setBlock(at,state,2));}
        h.succeed();
    }
    private GhostFurnitureApproachGameTests(){}
}
