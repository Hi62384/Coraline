package com.coraline.clothing;

import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

/** Actual item raycast, fluid placement, durability and adventure permission. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class BottomlessBucketGameTests {
    @GameTest(template="empty")
    public static void craftedBucketPlacesItsFluidWithWearAndHonorsUsePermission(GameTestHelper h){
        for(int x=0;x<6;x++)for(int z=0;z<7;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        var player=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"BucketUseQA"));
        var at=h.absolutePos(new BlockPos(2,1,5));player.setPos(Vec3.atBottomCenterOf(at));
        var floor=h.absolutePos(new BlockPos(2,0,2));var output=floor.above();
        for(boolean lava:new boolean[]{false,true}){
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            h.getLevel().setBlock(output,Blocks.AIR.defaultBlockState(),3);
            player.lookAt(EntityAnchorArgument.Anchor.EYES,new Vec3(floor.getX()+.5,floor.getY()+.99,floor.getZ()+.5));
            var item=lava?ArtifactItems.BOTTOMLESS_LAVA_BUCKET.get():ArtifactItems.BOTTOMLESS_WATER_BUCKET.get();
            var stack=new ItemStack(item);player.setItemSlot(EquipmentSlot.MAINHAND,stack);
            var result=item.use(h.getLevel(),player,InteractionHand.MAIN_HAND);
            h.assertTrue(result.getResult().consumesAction()&&h.getLevel().getBlockState(output).is(lava?Blocks.LAVA:Blocks.WATER),"Real bucket use failed to place the correct fluid");
            h.assertTrue(stack.getCount()==1&&stack.is(item)&&stack.getDamageValue()==1,"Bucket use consumed/replaced its item or ignored durability");
            h.getLevel().setBlock(output,Blocks.AIR.defaultBlockState(),3);
            player.gameMode.changeGameModeForPlayer(GameType.ADVENTURE);
            result=item.use(h.getLevel(),player,InteractionHand.MAIN_HAND);
            h.assertTrue(!result.getResult().consumesAction()&&h.getLevel().getBlockState(output).isAir()&&stack.getDamageValue()==1,"Adventure use restriction was bypassed or charged durability");
        }
        player.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);h.succeed();
    }
    private BottomlessBucketGameTests(){}
}
