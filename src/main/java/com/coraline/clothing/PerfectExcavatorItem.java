package com.coraline.clothing;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Diamond-speed digging tool restricted to ordinary earth and stone. */
public final class PerfectExcavatorItem extends PickaxeItem {
    public PerfectExcavatorItem(){super(Tiers.DIAMOND,1,-2.8F,new Item.Properties());}
    public static boolean basic(BlockState state){
        var b=state.getBlock();
        return b==Blocks.DIRT||b==Blocks.GRASS_BLOCK||b==Blocks.COARSE_DIRT||b==Blocks.ROOTED_DIRT||b==Blocks.PODZOL||b==Blocks.MYCELIUM||
            b==Blocks.SAND||b==Blocks.RED_SAND||b==Blocks.GRAVEL||b==Blocks.CLAY||b==Blocks.MUD||
            b==Blocks.STONE||b==Blocks.DEEPSLATE||b==Blocks.COBBLESTONE||b==Blocks.COBBLED_DEEPSLATE||b==Blocks.ANDESITE||b==Blocks.DIORITE||b==Blocks.GRANITE||b==Blocks.TUFF;
    }
    @Override public boolean isCorrectToolForDrops(BlockState state){return basic(state);}
    @Override public float getDestroySpeed(ItemStack stack,BlockState state){return basic(state)?Tiers.DIAMOND.getSpeed():1F;}
    @Override public boolean canAttackBlock(BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player){return basic(state);}
}
