package com.coraline.tycoon;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Walk-in industrial containment, with a distinct visible chamber per tier. */
public final class TycoonSoulFarmPlan {
    public static Map<BlockPos,BlockState> create(BlockPos origin,int rotation,int tier){
        var plan=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=0;y<=5;y++){
            Block material=y==0?Blocks.DEEPSLATE_TILES:y==5?Blocks.DARK_PRISMARINE:Blocks.AIR;
            if(y>0&&y<5&&(Math.abs(x)==6||Math.abs(z)==6))material=y==2?Blocks.TINTED_GLASS:Blocks.DEEPSLATE_BRICKS;
            if(z==-6&&Math.abs(x)<=1&&y>=1&&y<=3)material=Blocks.AIR;
            plan.put(TycoonAttractionPlan.at(origin,rotation,x,y,z),material.defaultBlockState());
        }
        for(int chamber=0;chamber<tier;chamber++){
            int x=-4+chamber*2;
            for(int y=1;y<=3;y++)plan.put(TycoonAttractionPlan.at(origin,rotation,x,y,4),(y==2?Blocks.GLASS:Blocks.IRON_BLOCK).defaultBlockState());
            plan.put(TycoonAttractionPlan.at(origin,rotation,x,4,4),Blocks.SEA_LANTERN.defaultBlockState());
            plan.put(TycoonAttractionPlan.at(origin,rotation,x,4,3),Blocks.COPPER_BLOCK.defaultBlockState());
        }
        for(int z=-3;z<=3;z++)for(int x:new int[]{-5,5})plan.put(TycoonAttractionPlan.at(origin,rotation,x,4,z),Blocks.WAXED_CUT_COPPER.defaultBlockState());
        for(int y=1;y<=2+tier/2;y++)plan.put(TycoonAttractionPlan.at(origin,rotation,0,y,1),(y%2==0?Blocks.CRYING_OBSIDIAN:Blocks.IRON_BARS).defaultBlockState());
        plan.put(TycoonAttractionPlan.at(origin,rotation,-4,1,-2),Blocks.LODESTONE.defaultBlockState());
        plan.put(TycoonAttractionPlan.at(origin,rotation,4,1,-2),Blocks.RESPAWN_ANCHOR.defaultBlockState());
        if(tier>=3)plan.put(TycoonAttractionPlan.at(origin,rotation,0,4,-4),Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT,true));
        if(tier>=4)for(int x:new int[]{-3,3})plan.put(TycoonAttractionPlan.at(origin,rotation,x,1,-4),Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
        if(tier>=5)plan.put(TycoonAttractionPlan.at(origin,rotation,0,4,1),Blocks.BEACON.defaultBlockState());
        return plan;
    }
    private TycoonSoulFarmPlan(){}
}
