package com.coraline.secrets;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class LocalSearchGameTests {
    @GameTest(template="empty") public static void patrolFollowsPlayerWithin120Blocks(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);
        h.assertTrue(MotherSearch.inSearchRadius(BlockPos.ZERO,new BlockPos(120,50,0)),"Boundary rejected");
        h.assertFalse(MotherSearch.inSearchRadius(BlockPos.ZERO,new BlockPos(120,0,1)),"Outside circular radius accepted");
        for(int step=0;step<8;step++){
            p.setPos(p.position().add(137,0,83));
            for(int i=0;i<16;i++)h.assertTrue(MotherSearch.inSearchRadius(p.blockPosition(),MotherSearch.patrolTarget(p)),"Patrol left moving search area");
        }
        var s=SecretTransactions.state(p);s.search=SecretsState.Search.MISSED_ACTIVITY_PENDING;s.pendingTicks=1;MotherSearch.tick(p);
        h.assertTrue(MotherSearch.inSearchRadius(p.blockPosition(),s.target),"Search starts at distant house");h.succeed();
    }
}
