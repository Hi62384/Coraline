package com.coraline.feast;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class CraftedShoeGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        for(int x=0;x<=6;x++)for(int y=1;y<=5;y++)h.setBlock(new BlockPos(x,y,6),Blocks.STONE);
        var p=FakePlayerFactory.get(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ShoeThrowQA"));
        var at=h.absolutePos(new BlockPos(2,1,2));p.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);p.setYRot(0);p.setXRot(0);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(FirstEscapeItems.SHOE.get()));return p;
    }
    private static List<Snowball> flights(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(Snowball.class,new AABB(p.blockPosition()).inflate(16),ball->ball.getPersistentData().hasUUID("CoralineCraftedShoeOwner")&&ball.getPersistentData().getUUID("CoralineCraftedShoeOwner").equals(p.getUUID()));
    }
    private static List<ItemEntity> recovered(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p.blockPosition()).inflate(16),item->item.getPersistentData().hasUUID("CoralineCraftedShoeOwner")&&item.getPersistentData().getUUID("CoralineCraftedShoeOwner").equals(p.getUUID()));
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void ordinaryShoeReallyFliesCollidesAndReturnsOneItem(GameTestHelper h) {
        var p=player(h);var stack=p.getMainHandItem();
        FirstEscapeItems.SHOE.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(stack.isEmpty(),"Successful throw did not consume its inventory copy");
        var balls=flights(p);h.assertTrue(balls.size()==1&&balls.get(0).getDeltaMovement().length()>.5,"Shoe disappeared without a real moving projectile");
        h.assertTrue(balls.get(0).getItem().is(FirstEscapeItems.SHOE.get())&&!CraftedShoeThrow.bound(balls.get(0).getItem()),"Ordinary flight acquired a story credential");
        h.runAfterDelay(40,()->{
            h.assertTrue(flights(p).isEmpty(),"Shoe did not hit the real wall");
            var drops=recovered(p);h.assertTrue(drops.size()==1&&drops.get(0).getItem().is(FirstEscapeItems.SHOE.get())&&drops.get(0).getItem().getCount()==1,"Collision lost or duplicated its recovery item");
            h.assertFalse(CraftedShoeThrow.bound(drops.get(0).getItem()),"Recovery minted story authority");h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=150)
    public static void creativeThrowCannotFarmRecoveredShoes(GameTestHelper h) {
        var p=player(h);p.getAbilities().instabuild=true;
        FirstEscapeItems.SHOE.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getMainHandItem().getCount()==1&&flights(p).size()==1,"Creative throw consumed or failed to throw");
        h.runAfterDelay(40,()->{h.assertTrue(flights(p).isEmpty()&&recovered(p).isEmpty(),"Creative free throw created farmable shoes");h.succeed();});
    }
    @GameTest(template="empty")
    public static void foreignStoryShoeDoesNotFallBackToOrdinaryThrow(GameTestHelper h) {
        var p=player(h);var stack=p.getMainHandItem();stack.getOrCreateTag().putUUID("StoryOwner",UUID.randomUUID());
        stack.getTag().putUUID("StoryToken",UUID.randomUUID());
        var result=FirstEscapeItems.SHOE.get().use(p.level(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(!result.getResult().consumesAction()&&stack.getCount()==1&&flights(p).isEmpty(),"Foreign/inactive story tool bypassed custody through crafted throwing");
        h.assertFalse(FirstEscapeItems.SHOE.get().onDroppedByPlayer(stack,p),"Bound story shoe became droppable");
        h.assertTrue(FirstEscapeItems.SHOE.get().onDroppedByPlayer(new ItemStack(FirstEscapeItems.SHOE.get()),p),"Ordinary crafted shoe cannot be dropped");h.succeed();
    }
    private CraftedShoeGameTests() {}
}
