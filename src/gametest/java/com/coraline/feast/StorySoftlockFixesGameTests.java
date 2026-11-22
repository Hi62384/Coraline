package com.coraline.feast;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Pure/identity acceptance tests for the read-only story audit repairs.
 * The production classes are invoked; no debug win or reward is granted. */
@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class StorySoftlockFixesGameTests {
    @GameTest(template="empty",batch="story_softlocks")
    public static void firstEscapeRequiresPhysicalTunnelAlignment(GameTestHelper h){
        h.assertTrue(FirstEscapeTimeline.tunnelExitValid(20,0,0),
                "The real center of the 20-block exit was rejected");
        for(double[] bad:new double[][]{{19.99,0,0},{23.01,0,0},
                {20,1.36,0},{20,-1.36,0},{20,0,-.51},{20,0,1.46},
                {Double.NaN,0,0},{20,Double.POSITIVE_INFINITY,0},
                {20,0,Double.NaN}})
            h.assertFalse(FirstEscapeTimeline.tunnelExitValid(bad[0],bad[1],bad[2]),
                    "Foreign or off-corridor endpoint completed the original crawl");
        h.succeed();
    }
    @GameTest(template="empty",batch="story_softlocks")
    public static void alternateWebAndPassageSitesAreBounded(GameTestHelper h){
        h.assertTrue(RescueWebSite.siteAttempt(-10)==0
                        &&RescueWebSite.siteAttempt(0)==0
                        &&RescueWebSite.siteAttempt(8)==8
                        &&RescueWebSite.siteAttempt(100)==8,
                "Web alternative slot can escape its owner allocation");
        h.assertTrue(RescuePassage.siteAttempt(-10)==0
                        &&RescuePassage.siteAttempt(0)==0
                        &&RescuePassage.siteAttempt(8)==8
                        &&RescuePassage.siteAttempt(100)==8,
                "Crawl alternative slot can escape its owner allocation");
        h.succeed();
    }
    @GameTest(template="empty",batch="story_softlocks")
    public static void onlyOriginalReturnedKeyReceiptMayRecoverPhysicalLock(GameTestHelper h){
        var scene=new VisitData.Scene();
        scene.owner=UUID.randomUUID();
        scene.door=UUID.randomUUID();
        var root=ParentRescueStory.data(scene);
        var custody=new CompoundTag();
        root.put("Custody",custody);
        var record=new CompoundTag();
        custody.put("Key",record);
        var original=ParentRescueStory.boundKey(scene);
        record.put("Stack",original.save(new CompoundTag()));
        h.assertFalse(StoryItemCustody.returnedOriginalKey(scene),
                "A key only in custody must not authorize locking without its return");
        record.putBoolean("Returned",true);
        h.assertTrue(StoryItemCustody.returnedOriginalKey(scene),
                "The authentic returned original key was rejected");
        var foreign=ParentRescueStory.boundKey(scene);
        foreign.getOrCreateTag().putUUID("StoryKeyDoor",UUID.randomUUID());
        record.put("Stack",foreign.save(new CompoundTag()));
        h.assertFalse(StoryItemCustody.returnedOriginalKey(scene),
                "An unrelated key could authorize the final original door");
        record.put("Stack",original.save(new CompoundTag()));
        record.putBoolean("Destroyed",true);
        h.assertFalse(StoryItemCustody.returnedOriginalKey(scene),
                "A destroyed custody record authorized an irreversible lock");
        h.succeed();
    }
    @GameTest(template="empty",batch="story_softlocks")
    public static void completelyIdleMovementIsDetectedWithoutForcedAfkTeleport(GameTestHelper h){
        var watch=new QuestRecovery.Watch(net.minecraft.core.BlockPos.ZERO,
                net.minecraft.world.phys.Vec3.ZERO,0);
        h.assertFalse(watch.stalled(net.minecraft.world.phys.Vec3.ZERO,599),
                "AFK watchdog fired before its grace period");
        h.assertTrue(watch.stalled(net.minecraft.world.phys.Vec3.ZERO,600),
                "Completely idle movement was never classified as stalled");
        h.succeed();
    }
    private StorySoftlockFixesGameTests(){}
}
