package com.coraline.feast;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("coraline")
@PrefixGameTestTemplate(false)
public final class RescueSoftlockGameTests {
    @GameTest(template="empty", batch="story_softlocks")
    public static void ratEyeCannotBeClaimedBeforeReturnFromCircus(GameTestHelper helper) {
        helper.assertFalse(RescueEncounters.canClaimRatEye(0), "A rat still in the circus cannot award the final eye");
        helper.assertFalse(RescueEncounters.canClaimRatEye(1), "A rat whose owner has not returned to the attic cannot award the final eye");
        helper.assertTrue(RescueEncounters.canClaimRatEye(2), "The committed post-return chase leg must remain playable");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void unarmedWeakPointStrikesRemainPossibleButFair(GameTestHelper helper) {
        helper.assertFalse(RescueBosses.validStrike(true, 1.0, .79F), "Unarmed strikes still obey the normal swing cooldown");
        helper.assertTrue(RescueBosses.validStrike(true, 1.0, .8F), "A correctly timed unarmed weakpoint strike is valid");
        helper.assertFalse(RescueBosses.validStrike(false, 1.0, 1.0F), "An ineffective held item is not treated as an unarmed strike");
        helper.assertTrue(Math.abs(2.0F-RescueBosses.strikeDamage(true, 1.0, RescueChapter.Phase.MANTIS_FIGHT))<.001F, "Unarmed damage stays deliberately low");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void hiddenArrivalUsesStricterRadiusThanConcealment(GameTestHelper helper) {
        var target=new net.minecraft.core.BlockPos(0,0,0);
        helper.assertFalse(GhostVictimNavigation.arrived(new net.minecraft.world.phys.Vec3(1.16,0,.5),target), "Navigation must not stop outside the .65 concealment radius");
        helper.assertTrue(GhostVictimNavigation.arrived(new net.minecraft.world.phys.Vec3(.5,0,.5),target), "The center of the validated hiding position is a successful arrival");
        helper.assertTrue(PalaceTransport.MOVER!=CinematicPropEntity.TAFFY_BOWL, "Transport movers and the tea bowl must have separate kinds");
        helper.succeed();
    }

    @GameTest(template="empty", batch="story_softlocks")
    public static void rebirthResetsTheTeaModuleOwnedKey(GameTestHelper helper) {
        helper.assertTrue(MainStoryRebirth.STORY_KEYS.contains(MissingParentsTea.KEY),
            "A new run must retire the tea module's actual saved story state");
        helper.assertTrue(MainStoryRebirth.STORY_KEYS.contains(AcceptedButtonEyesProgress.KEY)
                && MainStoryRebirth.STORY_KEYS.contains("AlternativeEndingComplete"),
            "A new run must retire the completed alternative route's active state");
        helper.succeed();
    }
}
