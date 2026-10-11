package com.coraline.feast;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Scripted removal is not player mining. Keep client updates, but do not recursively
 * break carpets, beds, hanging lights or flowers when their support is cleared first.
 * Placement keeps normal shape updates so fences, walls and stairs still connect. */
public final class ActivityBlocks {
    public static net.minecraft.world.level.block.entity.SignText readable(net.minecraft.world.level.block.entity.SignBlockEntity sign,net.minecraft.world.level.block.entity.SignText text){
        String wood=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(sign.getBlockState().getBlock()).getPath();
        boolean dark=wood.contains("dark_oak")||wood.contains("spruce")||wood.contains("mangrove")||wood.contains("crimson")||wood.contains("warped");
        var color=dark?net.minecraft.world.item.DyeColor.WHITE:net.minecraft.world.item.DyeColor.BLACK;
        text=text.setColor(color).setHasGlowingText(true);
        for(int i=0;i<4;i++)text=text.setMessage(i,text.getMessage(i,false).copy().withStyle(style->style.withColor(dark?0xFFFFFF:0x111111)));
        return text;
    }
    public static boolean set(Level level,BlockPos at,BlockState state,int flags){
        if(state.isAir())flags=(flags & ~Block.UPDATE_NEIGHBORS)|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS;
        return level.setBlock(at,state,flags);
    }
    private ActivityBlocks(){}
}
