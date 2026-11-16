package com.coraline.otherworld;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class GenerationDebrisGameTests {
    @GameTest(template="empty",timeoutTicks=30) public static void structureWritesDoNotCreatePlantDropsOrDeletePlayerItems(GameTestHelper h){
        var l=h.getLevel();var at=h.absolutePos(new net.minecraft.core.BlockPos(1,2,1));l.setBlock(at.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);l.setBlock(at,Blocks.ALLIUM.defaultBlockState(),2);
        var item=new ItemEntity(l,at.getX()+.5,at.getY()+1,at.getZ()+.5,new ItemStack(Items.WHEAT_SEEDS,3));l.addFreshEntity(item);
        GenerationBlocks.set(l,at.below(),Blocks.OAK_PLANKS.defaultBlockState(),2);GenerationBlocks.set(l,at,Blocks.AIR.defaultBlockState(),2);
        h.runAfterDelay(5,()->{var drops=l.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(at).inflate(2));h.assertTrue(drops.size()==1&&drops.get(0)==item&&item.getItem().getCount()==3,"Generation spawned garbage or swept up a legitimate dropped stack");item.discard();h.succeed();});
    }
}
