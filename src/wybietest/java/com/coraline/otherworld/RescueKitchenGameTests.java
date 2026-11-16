package com.coraline.otherworld;

import com.coraline.feast.ParentRescueStory;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class RescueKitchenGameTests {
    @GameTest(template="empty",timeoutTicks=200) public static void originalKitchenHasReachableCookingAndServingTargets(GameTestHelper h){
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(200,60,200));
        var realm=new RealmData.Realm();RosewoodHouse.build(level,center,new DoorData.Door(),realm);
        var stove=HouseCabinets.at(center,40,6,67);var cook=ParentRescueStory.cookingStand(level,stove);
        h.assertTrue(cook.equals(stove.north()),"Cook target is behind the authored north-facing stove");
        h.assertTrue(level.noCollision(new net.minecraft.world.phys.AABB(cook).deflate(.2,0,.2).expandTowards(0,.8,0)),"Cook target is occupied");
        var table=HouseCabinets.at(center,43,6,60);
        h.assertFalse(level.isEmptyBlock(table.east()),"Fixture no longer reproduces the second table block");
        h.assertTrue(level.noCollision(new net.minecraft.world.phys.AABB(table.west()).deflate(.2,0,.2).expandTowards(0,.8,0)),"Serving target is occupied");
        h.succeed();
    }
}
