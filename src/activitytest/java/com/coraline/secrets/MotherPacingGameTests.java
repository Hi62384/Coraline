package com.coraline.secrets;
import com.coraline.feast.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class MotherPacingGameTests {
    @GameTest(template="empty") public static void activityWaitOnlyAtVisitOpening(GameTestHelper h) throws Exception {
        var p=SecretsGameTests.fixture(h);var scene=OtherMotherLoop.scene(p);
        var advance=OtherMotherLoop.class.getDeclaredMethod("advance",net.minecraft.server.level.ServerPlayer.class,VisitData.Scene.class);advance.setAccessible(true);
        scene.loop.phase=OtherMotherLoopState.Phase.PROPOSAL;scene.loop.secrets.dialogue.putBoolean("OpeningActivityWait",true);
        advance.invoke(null,p,scene);h.assertTrue(scene.loop.phase==OtherMotherLoopState.Phase.WARNING,"Opening wait omitted");
        scene.loop.phase=OtherMotherLoopState.Phase.PROPOSAL;scene.loop.secrets.dialogue.remove("OpeningActivityWait");
        advance.invoke(null,p,scene);h.assertFalse(scene.loop.phase==OtherMotherLoopState.Phase.WARNING,"Later proposal replays activity_wait");h.succeed();
    }
    @GameTest(template="empty") public static void bellDialogueDoesNotWaitForMother(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);
        Reconciliation.begin(p,Reconciliation.Category.MINOR,SecretDialogue.state(p),true);
        Reconciliation.advance(p);
        h.assertTrue(Reconciliation.stage(p).equals("SCOLDING"),"Unloaded/distant Mother blocked dialogue progression");h.succeed();
    }
    @GameTest(template="empty") public static void ordinaryOpeningHasNoSecondScold(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var n=SecretTransactions.state(p).dialogue;
        Reconciliation.begin(p,Reconciliation.Category.MINOR,SecretDialogue.state(p),false);
        Reconciliation.advance(p);
        h.assertFalse(n.getString("Queue").contains("scolding"),"Ordinary-area encounter queued another scolding");
        h.assertFalse(Reconciliation.active(p),"Ordinary-area encounter did not finish");h.succeed();
    }
    @GameTest(template="empty") public static void chaseInterruptsSpeechWithoutCatching(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var s=SecretTransactions.state(p);
        s.dialogue.putString("Queue","test");MotherSearch.startChase(p);
        h.assertTrue(s.search==SecretsState.Search.DIRECT_CHASE&&s.target.equals(p.blockPosition()),"No chase target");
        h.assertFalse(Reconciliation.active(p)||s.world.getBoolean("CaughtEncounter")||s.dialogue.contains("Queue"),"Chase caught player or retained old speech");h.succeed();
    }
    @GameTest(template="empty") public static void interruptedVoiceReleasesItsCooldown(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var scene=OtherMotherLoop.scene(p);scene.welcomeAfter=OtherMotherLoop.now(p)+900;
        MotherInterrupts.interrupt(p);h.assertTrue(scene.welcomeAfter<=OtherMotherLoop.now(p),"Cancelled recording still blocks next line");h.succeed();
    }
    @GameTest(template="empty") public static void missingAckAddsOnlyOneSecond(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var n=SecretTransactions.state(p).dialogue;n.putString("Playing","test");n.putBoolean("Finished",false);
        n.putLong("Until",OtherMotherLoop.now(p)-19);h.assertFalse(SecretDialogue.completed(p),"Fallback grace skipped");
        n.putLong("Until",OtherMotherLoop.now(p)-20);h.assertTrue(SecretDialogue.completed(p),"Old ten-second silence remains");h.succeed();
    }
    @GameTest(template="empty") public static void realPlaybackFinishHandlesSlowServerTicks(GameTestHelper h){
        var p=SecretsGameTests.fixture(h);var scene=OtherMotherLoop.scene(p);var n=scene.loop.secrets.dialogue;long now=OtherMotherLoop.now(p);
        n.putString("Playing","test");n.putString("VoiceClockSound","test");n.putLong("VoiceClockEnd",System.currentTimeMillis()+10000);n.putLong("Until",now+500);scene.welcomeAfter=now+500;
        SecretDialogue.finished(p,"test");h.assertTrue(n.getLong("Until")==now+500,"Premature ack shortened a recording");
        n.putLong("VoiceClockEnd",System.currentTimeMillis()-1);SecretDialogue.finished(p,"test");
        h.assertTrue(n.getLong("Until")<=now+2&&scene.welcomeAfter<=now+2,"Finished audio still waits on lagged tick clock");h.succeed();
    }
}
