package com.coraline.secrets;

import com.coraline.feast.OtherMotherLoop;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class JournalInspectionGameTests {
    private static ServerPlayer journalFixture(GameTestHelper h) {
        var p = SecretsGameTests.fixture(h);
        var s = SecretTransactions.state(p);
        s.hiding = 3;
        s.journal.addAll(SecretRegistry.ALL.keySet());
        s.invisibleUntil = OtherMotherLoop.now(p) + 1200;
        s.falseUntil = OtherMotherLoop.now(p) + 600;
        s.combatUntil = OtherMotherLoop.now(p) + 600;
        s.recalculate();
        OtherMotherLoop.relationship(p, .8F, .1F);
        return p;
    }

    private static void carry(GameTestHelper h, ServerPlayer p) {
        var s = SecretTransactions.state(p);
        var at = SecretWorld.hiding(p, s.hiding);
        h.assertTrue(at != null, "Journal hiding place missing from fixture");
        p.setPos(at.getCenter());
        JournalPlacement.begin(p);
        h.assertTrue(JournalPlacement.carrying(p), "Journal movement did not begin");
    }

    private static void assertDiscovered(GameTestHelper h, ServerPlayer p, float trust, float suspicion) {
        var s = SecretTransactions.state(p);
        var loop = OtherMotherLoop.scene(p).loop;
        h.assertTrue(s.journal.isEmpty(), "Discovered journal retained its pages");
        h.assertTrue(s.exposed.containsAll(SecretRegistry.ALL.keySet()), "Discovery lost permanent knowledge");
        h.assertTrue(s.world.getBoolean("Disturbed"), "Discovery did not mark the hiding place disturbed");
        h.assertTrue(s.invisibleUntil == 0 && s.falseUntil == 0 && s.combatUntil == 0,
                "Discovery left journal abilities active");
        h.assertTrue(loop.trust < trust && loop.suspicion > suspicion, "Discovery lost relationship consequences");
        for (int i = 0; i < 6; i++) {
            h.assertTrue(s.value(i) == 0, "Discovered journal still grants attribute " + i);
        }
    }

    @GameTest(template="empty")
    public static void carriedJournalIsNotFoundAtItsPreviousHidingPlace(GameTestHelper h) {
        var p = journalFixture(h);
        var s = SecretTransactions.state(p);
        carry(h, p);
        JournalPlacement.tick(p);
        var before = s.save();
        var loop = OtherMotherLoop.scene(p).loop;
        float trust = loop.trust, suspicion = loop.suspicion;

        SecretsRuntime.inspect(p, s.hiding);

        h.assertTrue(s.save().equals(before), "Inspecting the empty origin changed carried journal records or abilities");
        h.assertTrue(JournalPlacement.carrying(p), "Inspection ended journal movement");
        h.assertTrue(s.journal.size() == SecretRegistry.ALL.size(), "Inspection cleared carried journal pages");
        h.assertTrue(loop.trust == trust && loop.suspicion == suspicion,
                "Inspecting the empty origin changed the relationship");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void stationaryJournalIsFoundAtItsHidingPlace(GameTestHelper h) {
        var p = journalFixture(h);
        var s = SecretTransactions.state(p);
        var loop = OtherMotherLoop.scene(p).loop;
        float trust = loop.trust, suspicion = loop.suspicion;
        h.assertFalse(JournalPlacement.carrying(p), "Stationary fixture is carrying the journal");

        SecretsRuntime.inspect(p, s.hiding);

        assertDiscovered(h, p, trust, suspicion);
        h.succeed();
    }

    @GameTest(template="empty")
    public static void stationaryJournalIsNotFoundAtAnyOtherHidingPlace(GameTestHelper h) {
        var p = journalFixture(h);
        var s = SecretTransactions.state(p);
        var before = s.save();
        var loop = OtherMotherLoop.scene(p).loop;
        float trust = loop.trust, suspicion = loop.suspicion;
        for (int spot = 0; spot < SecretWorld.HIDING.length; spot++) {
            if (spot != s.hiding) SecretsRuntime.inspect(p, spot);
        }

        h.assertTrue(s.save().equals(before), "Wrong hiding place changed stationary journal records");
        h.assertTrue(loop.trust == trust && loop.suspicion == suspicion,
                "Wrong hiding place changed the relationship");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void cancellingMovementRestoresDiscoveryAtTheOriginalHidingPlace(GameTestHelper h) {
        var p = journalFixture(h);
        var s = SecretTransactions.state(p);
        int original = s.hiding;
        carry(h, p);
        SecretsRuntime.inspect(p, original);
        h.assertFalse(s.journal.isEmpty(), "Journal was found before movement was cancelled");

        JournalPlacement.cancel(p);

        h.assertFalse(JournalPlacement.carrying(p), "Cancellation left the journal carried");
        h.assertTrue(s.hiding == original, "Cancellation changed the journal return address");
        var loop = OtherMotherLoop.scene(p).loop;
        float trust = loop.trust, suspicion = loop.suspicion;
        SecretsRuntime.inspect(p, original);
        assertDiscovered(h, p, trust, suspicion);
        h.succeed();
    }

    @GameTest(template="empty")
    public static void placingJournalMovesDiscoveryToTheNewHidingPlace(GameTestHelper h) {
        var p = journalFixture(h);
        var s = SecretTransactions.state(p);
        int original = s.hiding, destination = 4;
        carry(h, p);
        var at = SecretWorld.hiding(p, destination);
        h.assertTrue(at != null, "Destination hiding place missing from fixture");
        p.setPos(at.getCenter());
        h.assertTrue(JournalPlacement.place(p, destination), "Journal could not be placed at the new spot");
        h.assertTrue(!JournalPlacement.carrying(p) && s.hiding == destination,
                "Placement did not commit the new hiding place");
        var before = s.save();

        SecretsRuntime.inspect(p, original);

        h.assertTrue(s.save().equals(before), "Old hiding place discovered the newly placed journal");
        var loop = OtherMotherLoop.scene(p).loop;
        float trust = loop.trust, suspicion = loop.suspicion;
        SecretsRuntime.inspect(p, destination);
        assertDiscovered(h, p, trust, suspicion);
        h.succeed();
    }

    private static void assertIndependentInspection(GameTestHelper h, String evidence) {
        var p = journalFixture(h);
        var s = SecretTransactions.state(p);
        carry(h, p);
        s.world.putBoolean(evidence, true);
        var pages = java.util.Set.copyOf(s.journal);
        var loop = OtherMotherLoop.scene(p).loop;
        float trust = loop.trust, suspicion = loop.suspicion;
        long invisible = s.invisibleUntil, falseSignal = s.falseUntil, combat = s.combatUntil;

        SecretsRuntime.inspect(p, s.hiding);

        h.assertTrue(s.journal.equals(pages) && s.exposed.isEmpty() && !s.world.getBoolean("Disturbed"),
                evidence + " inspection incorrectly discovered the carried journal");
        h.assertTrue(JournalPlacement.carrying(p) && s.invisibleUntil == invisible
                && s.falseUntil == falseSignal && s.combatUntil == combat,
                evidence + " inspection changed journal movement or abilities");
        h.assertTrue(s.suspected.contains("personal_keyring"), evidence + " no longer raises keyring suspicion");
        h.assertTrue(loop.trust == trust && Math.abs(loop.suspicion - suspicion - .025F) < .00001F,
                evidence + " inspection lost its independent relationship consequence");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void carriedJournalDoesNotSuppressMissingKeyInspection(GameTestHelper h) {
        assertIndependentInspection(h, "KeyMissing");
    }

    @GameTest(template="empty")
    public static void carriedJournalDoesNotSuppressOpenAtticInspection(GameTestHelper h) {
        assertIndependentInspection(h, "AtticOpen");
    }

    private JournalInspectionGameTests() {}
}
