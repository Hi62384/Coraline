"""Regression guards for the 2026-11-22 Coraline softlock audit fixes.

Source-level tests complement native Forge GameTests; they do not constitute
a completed Minecraft survival playthrough.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/com/coraline"

def src(path):
    return (JAVA / path).read_text(encoding="utf-8")

class StorySoftlockFixesSourceTests(unittest.TestCase):
    def test_post_escape_watchdog_requires_real_overworld(self):
        s=src("feast/FirstEscapeStory.java")
        guard=s.split("static void cutsceneSafety(ServerPlayer p,VisitData.Scene s)",1)[1].split("int budget=",1)[0]
        self.assertIn("FamilyData.level(p.server,source.dimension)!=p.server.overworld()",guard)
        self.assertIn("p.serverLevel()!=p.server.overworld()",guard)
        self.assertIn("else if(!OtherWorld.isOther(p.level()))return",guard)

    def test_stationary_player_gets_opt_in_recovery(self):
        s=src("feast/QuestRecovery.java")
        self.assertIn("travel>=8||travel<.05",s)
        self.assertIn("watch.travel<.05",s)
        self.assertIn("if(watch.failed<FAILED_CHECKS&&!stalled)return",s)
        self.assertIn("/coraline recover",s)

    def test_tunnel_exit_has_a_lateral_and_vertical_gate(self):
        s=src("feast/FirstEscapeStory.java")
        self.assertIn("FirstEscapeTimeline.tunnelExitValid(progress,side,delta.y)",s)
        self.assertLess(s.index("tunnelExitValid(progress,side,delta.y)"),
                        s.index("var d=sourceDoor(p,s);",s.index("private static void tunnel(")))
        self.assertIn("Double.isFinite(sideways)",src("feast/FirstEscapeTimeline.java"))

    def test_exact_uuid_absence_before_resurrection(self):
        for name,marker in [
            ("feast/ShowActors.java","actor.setUUID(id)"),
            ("feast/RescueProps.java","restored.setUUID(original)"),
            ("feast/RescueEncounters.java","restored.setUUID(id)")]:
            s=src(name)
            self.assertIn("LegacyActorRecovery.Result.ABSENT",s,name)
            self.assertLess(s.index("LegacyActorRecovery.Result.ABSENT"),s.index(marker),name)
            self.assertIn("addFreshEntity(",s,name)
        s=src("feast/ShowActors.java")
        self.assertIn("canonical.getBoolean(\"FarewellDeparted\")",s)
        self.assertIn("canonical.getUUID(\"Entity\")",s)

    def test_post_boss_return_and_key_custody_fail_safely(self):
        s=src("feast/RescueEpilogue.java")
        self.assertIn("VisionLandingFailures",s)
        self.assertIn("VisionReturnUsedSpawnFallback",s)
        self.assertIn("nearReturn(l,p,Pose.STANDING,spawn",s)
        self.assertIn("FamilyData.level(p.server,door.dimension)==l",s)
        self.assertLess(s.index("nearReturn(l,p,Pose.STANDING,spawn"),s.index("p.teleportTo(l,target.x"))
        lock=src("feast/BeldamFinale.java")
        self.assertIn("StoryItemCustody.returnedOriginalKey(s)",lock)
        self.assertIn("n.getBoolean(\"CrawlReachedOverworld\")",lock)
        self.assertIn("n.getBoolean(\"KeyRecovered\")",lock)
        self.assertIn("LockUsedOriginalKeyReceipt",lock)
        custody=src("feast/StoryItemCustody.java")
        self.assertIn("ParentRescueStory.key(ItemStack.of(record.getCompound(\"Stack\")),s)",custody)

    def test_alternate_sites_preserve_foreign_blocks_and_original_receipts(self):
        for name,attempt in [
            ("feast/RescueWebSite.java","WebSiteAttempt"),
            ("feast/RescuePassage.java","PassageSiteAttempt")]:
            s=src(name)
            self.assertIn(attempt,s)
            self.assertIn("restore(p,s)",s if name.endswith("RescueWebSite.java") else s.replace("restore(p,scene)","restore(p,s)"))
            self.assertIn("LegacyActorRecovery",src("feast/RescueProps.java"))
        web=src("feast/RescueWebSite.java")
        self.assertIn("!recorded&&!old.isAir()",web)
        self.assertIn('n.put("AbandonedWebCells",abandoned)',web)
        self.assertIn('n.remove("WebSagIndexReady")',web)
        passage=src("feast/RescuePassage.java")
        self.assertIn("unowned block obstructs the original crawl",passage)
        self.assertIn('n.put("AbandonedPassageCells",abandoned)',passage)
        self.assertIn('drain(player,scene,"AbandonedWebCells"',src("feast/RescueSiteCleanup.java"))
        self.assertIn('drain(player,scene,"AbandonedPassageCells"',src("feast/RescueSiteCleanup.java"))

    def test_web_sag_uses_locality_after_one_time_migration(self):
        web=src("feast/RescueWebSite.java")
        body=web.split("public static void sag(ServerPlayer p,VisitData.Scene s)",1)[1].split("public static void restore(",1)[0]
        self.assertIn("if(!n.getBoolean(\"WebSagIndexReady\"))",body)
        self.assertIn("for(long packed:candidates)",body)
        self.assertIn("tracked.stream().mapToLong",body)
        self.assertIn("receipt.put(\"PreviousPlaced\",NbtUtils.writeBlockState(old))",body)
        self.assertIn("Invalid saved web cell reference.",body)
        verify=web.split("private static boolean verify(ServerPlayer p,VisitData.Scene s)",1)[1]
        self.assertLess(verify.index("if(n.getBoolean(\"WebVerified\")&&!cells.isEmpty())return true;"),
                        verify.index("var keys=new java.util.ArrayList<>(cells.getAllKeys())"))

    def test_audio_logout_and_server_stop_clear_tracking(self):
        s=src("feast/ShowPlayback.java")
        self.assertIn("PlayerLoggedOutEvent",s)
        self.assertIn("AUDIBLE_START.remove(nonce)",s)
        self.assertIn("REQUESTED_AT.remove(nonce)",s)
        self.assertIn("AUDIBLE_START.clear()",s)
        self.assertIn("REQUESTED_AT.clear()",s)

if __name__ == "__main__":
    unittest.main()
