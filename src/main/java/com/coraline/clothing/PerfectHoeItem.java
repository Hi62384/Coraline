package com.coraline.clothing;

import com.coraline.registry.ModBlocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;

public final class PerfectHoeItem extends HoeItem {
    public PerfectHoeItem(){super(OtherMaterials.PERFECT,-3,0,new Item.Properties());}
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();
        if(level.getBlockState(pos).is(Blocks.FARMLAND)){
            if(!level.isClientSide)level.setBlock(pos,ModBlocks.PERFECT_FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,level.getBlockState(pos).getValue(FarmBlock.MOISTURE)),3);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        var result=super.useOn(context);
        if(result.consumesAction()&&!level.isClientSide&&level.getBlockState(pos).is(Blocks.FARMLAND)){
            level.setBlock(pos,ModBlocks.PERFECT_FARMLAND.get().defaultBlockState().setValue(FarmBlock.MOISTURE,level.getBlockState(pos).getValue(FarmBlock.MOISTURE)),3);
        }
        return result;
    }
}
