"""Source/asset regressions, supplementary to native and rendered checks."""
import hashlib,json,unittest
from pathlib import Path
from import_numbered_original_audio import ogg_measure

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/com/coraline'
ASSETS=ROOT/'src/main/resources/assets/coraline'
def source(name):return (JAVA/name).read_text(encoding='utf-8-sig')

class CurrentStoryRepairs(unittest.TestCase):
    def test_saved_supper_table_reconciles_missing_seats_and_live_entity_ticks(self):
        visit=source('feast/FirstVisit.java')
        table=visit.split('static void table(ServerPlayer p,VisitData.Scene s)',1)[1].split('static void ensureChairs(',1)[0]
        self.assertIn('if(existingTable!=null){',table)
        self.assertIn('existingTable.longDining(true);',table)
        self.assertIn('ensureChairs(p,s);',table)
        self.assertLess(table.index('existingTable.longDining(true);'),table.index('ensureChairs(p,s);'))
        native=(ROOT/'src/gametest/java/com/coraline/feast/FirstVisitGameTests.java').read_text()
        self.assertIn('h.runAfterDelay(8,()->{',native)
        self.assertIn('p.tickCount=40;FirstVisit.tick(tick);',native)
        self.assertIn('Supper needs all nine chairs',native)

    def test_first_escape_environment_restoration_rejects_corrupt_journal_rows(self):
        room=source('feast/FirstEscapeScene.java')
        cleanup=room.split('public static void restore(ServerPlayer p,VisitData.Scene s)',1)[1].split('private FirstEscapeScene()',1)[0]
        self.assertIn('try{packed=Long.parseLong(key);}',cleanup)
        self.assertIn('catch(NumberFormatException invalid)',cleanup)
        self.assertIn('l.isOutsideBuildHeight(at)',cleanup)
        self.assertIn('!l.getWorldBorder().isWithinBounds(at)',cleanup)
        self.assertIn('!cell.contains("Original",Tag.TAG_COMPOUND)',cleanup)
        self.assertIn('!cell.contains("Placed",Tag.TAG_COMPOUND)',cleanup)
        self.assertIn('l.getBlockState(at).equals(placed)',cleanup)
        self.assertIn('l.getBlockEntity(at)==null',cleanup)
        native=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('invalidEscapeRoomCleanupJournalCannotCrashOrEraseUnownedBlocks',native)

    def test_partial_collapse_checkpoint_must_restart_the_real_exterior_gate(self):
        encounter=source('feast/RescueEncounters.java')
        validation=encounter.split('static boolean collapseCheckpointValid(CompoundTag n)',1)[1].split('static boolean restoreIncompleteCollapse(',1)[0]
        self.assertIn('n.contains("CollapseStart",Tag.TAG_LONG)',validation)
        self.assertIn('n.getLong("CollapseStart")!=0L',validation)
        self.assertIn('n.getInt("CollapseDuration")>0',validation)
        self.assertIn('Double.isFinite(radius)',validation)
        rollback=encounter.split('static boolean restoreIncompleteCollapse(',1)[1].split('public static BlockPos garden(',1)[0]
        self.assertIn('RescueChapter.go(p,s,EXTERIOR)',rollback)
        self.assertIn('n.putBoolean("CatRescued",false)',rollback)
        self.assertIn('n.remove("HouseSafe")',rollback)
        self.assertIn('if(restoreIncompleteCollapse(p,s))return;',encounter)
        self.assertIn('if(RescueEncounters.restoreIncompleteCollapse(p,s))return;',source('feast/RescueChapter.java'))
        self.assertIn('RescueEncounters.collapseCheckpointValid(RescueChapter.data(scene))',source('feast/StoryTunnel.java'))
        native=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('malformedCollapseCheckpointRestartsTheRealExteriorObjective',native)

    def test_interrupted_wybie_shoe_miss_recovers_saved_cue_without_an_automatic_hit(self):
        story=source('feast/FirstEscapeStory.java')
        shoe=story.split('static void shoe(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static void finishShoeThrow(',1)[0]
        missed=shoe.split('if(e.cue()==1){',1)[1].split('if(e.time()>=80',1)[0]
        self.assertIn('if(!n.getBoolean("ShoeMissed"))',missed)
        self.assertIn('n.putBoolean("ShoeMissed",true)',missed)
        self.assertNotIn('finishShoeThrow(p,s,e)',missed)
        self.assertIn('if(n.hasUUID("ShoeFlight")){',shoe)
        self.assertIn('if(n.contains("ShoeFlightPosition",Tag.TAG_LONG))',shoe)
        lost=story.split('private static CinematicPropEntity recoverMissingShoe(',1)[1].split('private static boolean relaunchShoe(',1)[0]
        self.assertNotIn('||!n.contains("ShoeFlightPosition",Tag.TAG_LONG)',lost)
        self.assertIn('LoopProps.hold(p.server.overworld(),p.blockPosition())',lost)
        self.assertLess(lost.index('LegacyActorRecovery.Result.ABSENT'),lost.index('recreated.setUUID(id)'))
        timeline=source('feast/FirstEscapeTimeline.java')
        self.assertIn('case MISSING_D->Phase.THROW_SHOE',timeline)
        self.assertNotIn('case THROW_SHOE->',timeline)
        gametests=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('interruptedWybieShoeMissKeepsSameActorAndNeverAutoCompletes',gametests)

    def test_malformed_web_and_crawl_receipts_never_crash_cinematic_tick(self):
        web=source('feast/RescueWebSite.java')
        sag=web.split('public static void sag(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static void restore(',1)[0]
        self.assertIn('try{packed=Long.parseLong(key);}',sag)
        self.assertIn('catch(NumberFormatException invalid)',sag)
        self.assertIn('Invalid saved web cell reference.',sag)
        passage=source('feast/RescuePassage.java')
        cleanup=passage.split('public static void restore(ServerPlayer p,VisitData.Scene s)',1)[1]
        self.assertIn('try{packed=Long.parseLong(key);}',cleanup)
        self.assertIn('catch(NumberFormatException invalid)',cleanup)
        self.assertIn('Invalid saved passage cleanup cell reference.',cleanup)
        self.assertIn('continue;',cleanup)
        self.assertIn('level.isOutsideBuildHeight(at)',cleanup)
        self.assertIn('!level.getWorldBorder().isWithinBounds(at)',cleanup)
        postboss=source('feast/RescueSiteCleanup.java')
        self.assertIn('try{packed=Long.parseLong(position);}',postboss)
        self.assertIn('catch(NumberFormatException invalid)',postboss)
        self.assertIn('Invalid saved rescue cleanup receipt.',postboss)
        self.assertIn('if(level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at))continue',postboss)

    def test_original_little_door_return_rechecks_physical_web_and_crawl_before_transfers(self):
        tunnel=source('feast/StoryTunnel.java')
        return_body=tunnel.split('private static void resumeChapter(ServerPlayer p,VisitData.Scene scene)',1)[1].split('public static boolean controls(',1)[0]
        self.assertIn('static boolean webReturnPhase(RescueChapter.Phase phase)',tunnel)
        self.assertIn('phase==RescueChapter.Phase.FINAL_P||phase==RescueChapter.Phase.FINAL_Q',tunnel)
        self.assertIn('phase.ordinal()>=RescueChapter.Phase.WEB_STEALTH.ordinal()',tunnel)
        self.assertIn('phase.ordinal()<=RescueChapter.Phase.HAND_FALL.ordinal()',tunnel)
        self.assertIn('n.remove("PassageEntered");n.remove("PassageEntryRecheck")',return_body)
        self.assertLess(return_body.index('n.remove("PassageEntered")'),return_body.index('if(!RescuePassage.enter(p,scene))'))
        self.assertIn('RescueChapter.go(p,scene,RescueChapter.Phase.TUNNEL_CHASE)',return_body)
        self.assertLess(return_body.index('if(!RescuePassage.enter(p,scene))'),return_body.index('RescueChapter.go(p,scene,RescueChapter.Phase.TUNNEL_CHASE)'))
        self.assertIn('n.remove("Recover")',return_body)
        self.assertIn('n.remove("WebVerified");n.putInt("WebVerify",0)',return_body)
        self.assertLess(return_body.index('n.remove("WebVerified")'),return_body.index('if(!RescueWebSite.prepare(p,scene))'))
        self.assertLess(return_body.index('if(!RescueWebSite.prepare(p,scene))'),return_body.index('if(!RescueChapter.transfer(p,level,landing))'))
        self.assertIn('n.putBoolean("StoryReturnWebPending",true)',return_body)
        self.assertIn('n.putBoolean("StoryReturnCrawlPending",true)',return_body)
        self.assertIn('n.remove("StoryReturnWebPending");n.remove("StoryReturnCrawlPending")',return_body)
        self.assertIn('n.putBoolean("Recover",true)',return_body)
        source_tick=tunnel.split('public static void tickReturnSource(ServerPlayer player)',1)[1].split('public static void tickSource(',1)[0]
        self.assertIn('if(player.tickCount%5==0)resumeChapter(player,scene)',source_tick)
        self.assertIn('n.remove("StoryReturnWebPending");n.remove("StoryReturnCrawlPending")',source_tick)
        self.assertLess(source_tick.index('resumeChapter(player,scene)'),source_tick.index('TunnelPassage.tick(player)'))
        tests=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('littleDoorReentryUsesTheRealFinaleDimensionAndFreshReceipts',tests)

    def test_postboss_dream_return_never_restores_missing_or_nonfinite_vitals(self):
        rules=source('feast/RescueVisionRules.java')
        self.assertIn('public static float restoredHealth(boolean hasSaved,float saved,float current,float maximum)',rules)
        self.assertIn('hasSaved&&Float.isFinite(saved)&&saved>0',rules)
        self.assertIn('Float.isFinite(current)&&current>0',rules)
        self.assertIn('public static int restoredFood(',rules)
        self.assertIn('public static float restoredSaturation(',rules)
        epilogue=source('feast/RescueEpilogue.java')
        recovery=epilogue.split('private static boolean restoreVision(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static boolean verifyVisionLanding(',1)[0]
        self.assertIn('saved.contains("Health",Tag.TAG_ANY_NUMERIC)',recovery)
        self.assertIn('RescueVisionRules.restoredHealth(healthSaved,savedHealth,p.getHealth(),p.getMaxHealth())',recovery)
        self.assertIn('RescueVisionRules.restoredFood(',recovery)
        self.assertIn('RescueVisionRules.restoredSaturation(',recovery)
        self.assertIn('VisionReturnUsedVitalsFallback',recovery)
        self.assertIn('Float.isFinite(saved.getFloat("Yaw"))',recovery)
        self.assertIn('Float.isFinite(saved.getFloat("Pitch"))',recovery)
        self.assertNotIn('p.setHealth(Math.min(p.getMaxHealth(),saved.getFloat("Health")))',recovery)
        self.assertLess(recovery.index('p.setHealth(health)'),recovery.index('n.putBoolean("VisionLandingPending",true)'))
        tests=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('postBossDreamCannotRestoreDeathOrInvalidVitals',tests)

    def test_web_journal_recovers_interrupted_placement_and_sag(self):
        web=source('feast/RescueWebSite.java')
        floor=web.split('private static boolean floor(ServerLevel l,CompoundTag journal,Vec3 point)',1)[1].split('public static void sag(',1)[0]
        self.assertIn('receipt.put("PreviousPlaced",NbtUtils.writeBlockState(old))',floor)
        self.assertIn('if(!l.getBlockState(pos).equals(state))return false',floor)
        self.assertIn('receipt.remove("PreviousPlaced")',floor)
        build=web.split('public static boolean prepare(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static java.util.Set<Long> expectedCells(',1)[0]
        self.assertIn('n.putInt("WebBuild",cursor)',build)
        self.assertLess(build.index('n.putInt("WebBuild",cursor)'),build.index('OtherMotherLoop.changed(p)'))
        self.assertIn('if(!recorded&&!old.isAir())return false',floor)
        self.assertIn('old.equals(NbtUtils.readBlockState(lookup,receipt.getCompound("PreviousPlaced")))',floor)
        sag=web.split('public static void sag(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static void restore(',1)[0]
        self.assertIn('cell.put("PreviousPlaced",NbtUtils.writeBlockState(old))',sag)
        self.assertIn('if(l.getBlockState(at).equals(state))cell.remove("PreviousPlaced")',sag)
        self.assertIn('if(l.getBlockState(at).equals(saved))cell.remove("PreviousPlaced")',sag)
        self.assertIn('boolean updated=false',sag)
        self.assertIn('if(updated)OtherMotherLoop.changed(p)',sag)
        self.assertLess(sag.index('cell.put("Placed",NbtUtils.writeBlockState(state))'),sag.index('if(updated)OtherMotherLoop.changed(p)'))
        restore=web.split('public static void restore(ServerPlayer p,VisitData.Scene s)',1)[1].split('private RescueWebSite()',1)[0]
        self.assertIn('for(String key:new java.util.HashSet<>(cells.getAllKeys()))',restore)
        self.assertIn('previous==null||!actual.equals(previous)',restore)
        self.assertLess(restore.index('if(!l.getBlockState(pos).equals(original))continue'),restore.index('cells.remove(key)'))
        self.assertIn('if(cells.isEmpty())',restore)
        self.assertLess(restore.index('if(cells.isEmpty())'),restore.index('n.remove("WebCells")'))
        cleanup=source('feast/RescueSiteCleanup.java')
        self.assertIn('entry.contains("PreviousPlaced",net.minecraft.nbt.Tag.TAG_COMPOUND)',cleanup)
        self.assertIn('previous==null||!current.equals(previous)',cleanup)
        self.assertIn('data.remove("WebVerified");data.remove("WebVerify");data.remove("WebEntryRecheck")',cleanup)

    def test_final_web_entry_revalidates_actual_journal_and_persists_retry(self):
        web=source('feast/RescueWebSite.java')
        self.assertIn('static boolean webVerificationRequired(boolean ready,boolean verified,int receipts)',web)
        self.assertIn('if(n.getBoolean("WebReady"))return verify(p,s)',web)
        verify=web.split('private static boolean verify(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static boolean blocked(',1)[0]
        self.assertIn('keys.sort(String::compareTo)',verify)
        self.assertIn('expectedCells(p,s)',verify)
        self.assertIn('!cells.contains(Long.toString(packed))',verify)
        expected=web.split('private static java.util.Set<Long> expectedCells(',1)[1].split('private static boolean verify(',1)[0]
        self.assertIn('for(int lane=0;lane<RescueWebLayout.LANES;lane++)',expected)
        self.assertIn('for(int sample=0;sample<=samples;sample++)',expected)
        self.assertIn('point.lerp(adjacent,i/12.0)',expected)
        self.assertIn('for(double u:new double[]{0,1})',expected)
        self.assertIn('int end=Math.min(keys.size(),cursor+96)',verify)
        self.assertIn('receipt.contains("Original",Tag.TAG_COMPOUND)',verify)
        self.assertIn('receipt.contains("Placed",Tag.TAG_COMPOUND)',verify)
        self.assertIn('!actual.equals(placed)',verify)
        self.assertIn('&&!actual.equals(original)',verify)
        self.assertIn('if(!actual.equals(placed))',verify)
        self.assertIn('previous==null||!actual.equals(previous)',verify)
        self.assertIn('receipt.remove("PreviousPlaced")',verify)
        self.assertIn('if(!level.getBlockState(pos).equals(placed))',verify)
        finale=source('feast/BeldamFinale.java')
        entry=finale.split('static boolean enterWeb(ServerPlayer p,VisitData.Scene s,ParentEntity mother)',1)[1].split('private static void rememberPosition(',1)[0]
        self.assertLess(entry.index('n.remove("WebVerified")'),entry.index('var landing=webLanding(p,s)'))
        self.assertIn('n.putBoolean("WebEntryRecheck",true)',entry)
        self.assertIn('p.position().distanceToSqr(landing)>.25',entry)
        self.assertIn('n.remove("WebEntryRecheck")',entry)
        self.assertIn('RescueWebHearing.reset(p)',entry)
        retry=finale.split('public static void retry(ServerPlayer p,VisitData.Scene s',1)[1]
        self.assertIn('n.remove("WebEntryReady");n.remove("WebEntered");n.remove("WebEntryRecheck")',retry)
        self.assertIn('n.remove("WebVerified");n.putInt("WebVerify",0)',retry)
        self.assertNotIn('RescueChapter.transfer(p,p.server.getLevel(ActivityWorld.KEY)',retry.split('}else if(state==WEB_STEALTH){',1)[1].split('}else if(state==DOOR_STRUGGLE){',1)[0])
        tests=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('webReadyCannotSkipItsPhysicalReceipts',tests)

    def test_first_escape_cutscene_and_main_tick_require_original_cast(self):
        rules=source('feast/FirstEscapeTimeline.java')
        for key in ('needsOtherMother','needsOtherWybie','needsRealWybie'):
            self.assertIn('public static boolean '+key+'(Phase p)',rules)
        story=source('feast/FirstEscapeStory.java')
        tick=story.split('public static boolean tick(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static boolean recording(',1)[0]
        self.assertLess(tick.index('pauseForMissingActor(p,s,state)'),tick.index('int ticks=n.getInt("Ticks");String sound='))
        self.assertIn('if(n.getBoolean("AudioStarted"))',story.split('private static boolean pauseForMissingActor(',1)[1].split('public static boolean tick(',1)[0])
        self.assertIn('ShowPlayback.stop(p,s)',story.split('private static boolean pauseForMissingActor(',1)[1].split('public static boolean tick(',1)[0])
        safety=story.split('static void cutsceneSafety(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static boolean readyForVoice(',1)[0]
        self.assertLess(safety.index('pauseForMissingActor(p,s,state)'),safety.index('StoryCutsceneWatchdog.overdue('))
        self.assertIn('!voice.isEmpty()&&!readyForVoice(p,s,state)',safety)
        self.assertIn('FirstEscapeTimeline.needsRealWybie(state)',story)

    def test_original_cat_absence_pauses_mirror_watchdog_before_reward(self):
        cat=source('feast/MissingParentsCat.java')
        self.assertIn('static boolean catWatchdogReady(boolean started,Phase state,boolean originalPresent)',cat)
        safety=cat.split('static void cutsceneSafety(ServerPlayer p,VisitData.Scene s)',1)[1].split('static ',1)[0]
        self.assertIn('!catWatchdogReady(n.getBoolean("Started"),state,cat(p,s)!=null)',safety)
        self.assertLess(safety.index('!catWatchdogReady('),safety.index('StoryCutsceneWatchdog.overdue('))
        self.assertLess(safety.index('!catWatchdogReady('),safety.index('ShowPlayback.safetyFinish('))
        self.assertIn('StoryCutsceneWatchdog.retry(s,"post_escape_cat")',safety)
        gametest=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('catWatchdogRequiresOriginalActorThroughoutMirrorAndDolls',gametest)

    def test_parent_rescue_watchdog_requires_original_mother_before_completing(self):
        parent=source('feast/ParentRescueStory.java')
        self.assertIn('static boolean rescueMotherReadyForWatchdog(Phase state,boolean originalPresent)',parent)
        safety=parent.split('static void cutsceneSafety(ServerPlayer p,VisitData.Scene s)',1)[1].split('static void prepareReturnRoom(',1)[0]
        self.assertIn('!rescueMotherReadyForWatchdog(state,mother(p,s)!=null)',safety)
        self.assertLess(safety.index('!rescueMotherReadyForWatchdog('),safety.index('StoryCutsceneWatchdog.overdue('))
        self.assertLess(safety.index('!rescueMotherReadyForWatchdog('),safety.index('case N->{release(p,s)'))
        self.assertIn('StoryCutsceneWatchdog.retry(s,"parent_rescue")',safety)
        gametest=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('parentRescueWatchdogNeverFinishesMissingMother',gametest)

    def test_saved_final_exit_recovers_original_world_and_never_skips_key_or_crawl(self):
        finale=source('feast/BeldamFinale.java')
        recovery=finale.split('static boolean originalExitReady(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static boolean preflight(',1)[0]
        self.assertIn('!s.owner.equals(source.owner)',recovery)
        self.assertIn('home!=p.server.overworld()',recovery)
        self.assertIn('state==LOCK_DOOR&&!n.getBoolean("CrawlReachedOverworld")',recovery)
        self.assertIn('n.getBoolean("HandSevered")&&n.getBoolean("KeyRecovered")',recovery)
        self.assertIn('RescuePassage.enter(p,s)',recovery)
        self.assertIn('RescueChapter.go(p,s,TUNNEL_CHASE)',recovery)
        self.assertIn('p.serverLevel()!=home&&!RescueChapter.transfer(p,home,source.pos)',recovery)
        self.assertIn('state==BANGING&&!n.getBoolean("PermanentlySealed")',recovery)
        self.assertIn('RescueChapter.go(p,s,LOCK_DOOR)',recovery)
        chapter=source('feast/RescueChapter.java')
        self.assertIn('if(!BeldamFinale.originalExitReady(p,s))',chapter)
        safety=chapter.split('static void cutsceneSafety(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static void recover(',1)[0]
        self.assertLess(safety.index('!BeldamFinale.originalExitReady(p,s)'),safety.index('StoryCutsceneWatchdog.overdue('))
        tests=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('finalDoorAndBangingCannotResumeInTheWebWorld',tests)

    def test_final_tunnel_requires_fresh_bounded_readback_on_reentry(self):
        passage=source('feast/RescuePassage.java')
        enter=passage.split('public static boolean enter(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static double progress(',1)[0]
        self.assertIn('entryRecheckNeeded(n.getBoolean("PassageEntered"),n.getBoolean("PassageEntryRecheck"))',enter)
        self.assertIn('n.remove("PassageVerified")',enter)
        self.assertIn('n.putInt("PassageVerify",0)',enter)
        self.assertLess(enter.index('n.remove("PassageVerified")'),enter.index('if(!prepare(p,s))return false'))
        self.assertLess(enter.index('if(!prepare(p,s))return false'),enter.index('n.putBoolean("PassageEntered",true)'))
        self.assertIn('n.remove("PassageEntryRecheck")',enter)
        verify=passage.split('private static boolean verify(',1)[1].split('public static boolean enter(',1)[0]
        self.assertIn('missing original or placement receipt',verify)
        self.assertIn('the physical tunnel differs from the saved layout',verify)
        game=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('savedEscapeCrawlRechecksEveryNewAttempt',game)

    def test_postboss_reunion_cat_and_watchdog_require_physical_stage(self):
        chapter=source('feast/RescueChapter.java')
        safety=chapter.split('static void cutsceneSafety(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static void recover(',1)[0]
        self.assertIn('!RescueEncounters.presentationReady(p,s,state)||!RescueEpilogue.presentationReady(p,s)',safety)
        self.assertLess(safety.index('!RescueEpilogue.presentationReady(p,s)'),safety.index('StoryCutsceneWatchdog.overdue('))
        epilogue=source('feast/RescueEpilogue.java')
        ready=epilogue.split('static boolean presentationReady(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static boolean begin(',1)[0]
        self.assertIn('!requiresEpilogueStage(state)',ready)
        self.assertIn('p.serverLevel()!=world&&!RescueChapter.transfer(p,world,door.pos)',ready)
        self.assertIn('state==REUNION_A||state==REUNION_B',ready)
        self.assertIn('stageFarewellCat(p,s,door,world)',ready)
        self.assertIn('!RescueChapter.data(s).getBoolean("ParentsReturned")',ready)
        self.assertIn('world!=p.server.overworld()',ready)
        cat=epilogue.split('private static boolean stageFarewellCat(',1)[1].split('static boolean presentationReady(',1)[0]
        self.assertIn('ShowActors.recoverCat(p,s,world,candidate)',cat)
        self.assertIn('cat.setPos(candidate)',cat)
        self.assertIn('n.putBoolean("FarewellCatPlaced",true)',cat)

    def test_house_safe_requires_real_world_and_reopens_saved_doors(self):
        encounter=source('feast/RescueEncounters.java')
        safe=encounter.split('case HOUSE_SAFE->{',1)[1].split('case ROOM_LOCKED->',1)[0]
        self.assertLess(safe.index('!OtherWorld.isOther(p.level())'),safe.index('greenHouse(p,s)'))
        self.assertLess(safe.index('!OtherWorld.isOther(p.level())'),safe.index('carryCat(p,s)'))
        self.assertIn('n.putString("Blocker","Return through your original Little Door',safe)
        green=encounter.split('private static void greenHouse(',1)[1].split('static void closeRoom(',1)[0]
        self.assertIn('restoreHouseEntry(n.getBoolean("GreenHouse")',green)
        self.assertLess(green.index('FirstEscapeStory.setDoor(p,entrance,true)'),green.index('if(n.getBoolean("GreenHouse"))return'))
        tests=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('postBossScenesMustPreserveOriginalWorldAndCast',tests)

    def test_legacy_offworld_ghost_dream_has_authenticated_return_anchor(self):
        epilogue=source('feast/RescueEpilogue.java')
        restore=epilogue.split('private static boolean restoreVision(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static boolean verifyVisionLanding(',1)[0]
        self.assertIn('originalOverworld&&finiteOriginal',restore)
        self.assertIn('door.placed&&s.owner.equals(door.owner)',restore)
        self.assertIn('FamilyData.level(p.server,door.dimension)==l',restore)
        self.assertIn('VisionReturnUsedDoorFallback',restore)
        self.assertLess(restore.index('door.placed&&s.owner.equals(door.owner)'),restore.index('n.putBoolean("VisionReturnUsedDoorFallback",true)'))
        self.assertIn('return verifyVisionLanding(p,n)',restore)

    def test_tea_watchdog_does_not_outpace_canonical_visitors(self):
        tea=source('feast/MissingParentsTea.java')
        self.assertIn('static boolean visitorsReadyForScene(',tea)
        self.assertIn('if(!visitorsReadyForScene(state,spink!=null,forcible!=null))',tea)
        watchdog=tea.split('static void cutsceneSafety(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static float crush(',1)[0]
        self.assertIn('visitorsReadyForScene(state,actor(p,n,"Spink")!=null,actor(p,n,"Forcible")!=null)',watchdog)
        self.assertIn('StoryCutsceneWatchdog.retry(s,"post_escape_tea")',watchdog)
        self.assertLess(watchdog.index('visitorsReadyForScene('),watchdog.index('StoryCutsceneWatchdog.overdue('))
        self.assertLess(watchdog.index('ShowPlayback.stop(p,s)'),watchdog.index('StoryCutsceneWatchdog.overdue('))
        tests=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('teaCutsceneNeverSkipsUnloadedOriginalVisitors',tests)

    def test_reunion_actor_lease_gates_recording_after_reload(self):
        epilogue=source('feast/RescueEpilogue.java')
        self.assertIn('static boolean parentsOnReunionStage(',epilogue)
        self.assertIn('static boolean presentationReady(ServerPlayer p,VisitData.Scene s)',epilogue)
        self.assertIn('parentsOnReunionStage(n.getBoolean("ParentsStaged")',epilogue)
        self.assertIn('n.remove("ParentsStaged")',epilogue)
        self.assertIn('if(!reunionViewerNear(p.serverLevel()==world,p.distanceToSqr(door.pos.getCenter())))',epilogue)
        self.assertIn('RescueChapter.transfer(p,world,door.pos)',epilogue)
        self.assertIn('if(n.getBoolean("Audio")){ShowPlayback.stop(p,s)',epilogue)
        chapter=source('feast/RescueChapter.java')
        self.assertIn('!RescueEpilogue.presentationReady(p,s)',chapter)
        self.assertLess(chapter.index('!RescueEpilogue.presentationReady(p,s)'),chapter.index('String recording=sound(state);boolean done=false;'))
        tests=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('reunionAudioWaitsForOriginalParentsAtSavedStage',tests)

    def test_proposal_dismount_is_stopped_on_server_and_before_client_prediction(self):
        text=source('feast/BlackIsTraditional.java')
        self.assertIn('EntityMountEvent event',text);self.assertIn('ClientboundSetPassengersPacket(chair)',text)
        self.assertIn('tag.getInt("VisitEventSerial")==s.loop.event.getInt("Serial")',text)
        self.assertIn('Phase.AWAITING_CHOICE.ordinal()',text);self.assertIn('releaseSeat(p,s);BedroomBarricade.clear',text)
        self.assertIn('event.getInput().shiftKeyDown=false',source('client/TraditionalClient.java'))
        prop=source('feast/ScenePropEntity.java')
        self.assertLess(prop.index('refreshProposalSeatLock();\n'),prop.index('if(VisitEvents.propTick(this))return;'))
    def test_no_old_dark_proposal_packet_on_mother_click(self):
        text=source('feast/VisitEvents.java').split('public static boolean interact(ServerPlayer p,ParentEntity parent)',1)[1]
        self.assertLess(text.index('FirstEscapeStory.engaged(major)||ParentRescueStory.started(major)'),text.index('BlackIsTraditional.sync'))
        self.assertIn('!ParentRescueClient.active()&&!RescueClient.active()',source('client/OtherWorldLighting.java'))
    def test_temporary_arm_and_dolls_have_scoped_retirement(self):
        text=source('feast/CinematicPropEntity.java')
        self.assertIn('retiredCanonicalGhost()||retireFinishedStoryProp()',text)
        self.assertIn('FirstEscapeStory.retiredGrab(this,s)',text);self.assertIn('MissingParentsCat.retiredDolls(this,s)',text)
        self.assertIn('FirstEscapeVisualRules.rescueHand(',source('client/renderer/FirstEscapePresentation.java'))
        self.assertIn('!dollsFinished(s)&&n.contains("BedFoot")',source('feast/MissingParentsCat.java'))
    def test_bike_uses_actual_story_tick_not_only_riding_pose(self):
        text=source('feast/FirstEscapeStory.java')
        self.assertIn('driveStoryBike(bike,p,s,w);',text);self.assertIn('bike.storyDrivenAt==now',text)
        self.assertIn('expanded==0||System.nanoTime()<deadline',source('feast/StoryBikeRoute.java'))
        self.assertIn('best=next;',source('feast/StoryBikeRoute.java'))
    def test_cat_tunnel_is_nonblocking_and_follows_behind(self):
        text=source('entity/CharacterEntity.java')
        self.assertIn('boolean canCollideWith(Entity other)',text);self.assertIn('void push(Entity other)',text)
        guide=source('feast/ParentRescueStory.java').split('static void guideTunnelCat(',1)[1].split('public static void tick(',1)[0]
        self.assertIn('playerProgress-2.5',guide);self.assertIn('cat.storyCatCrawl(true)',guide);self.assertIn('getBlockCollisions(cat',guide)
    def test_first_escape_retries_completion_and_requires_physical_transfers(self):
        text=source('feast/FirstEscapeStory.java')
        tick=text.split('public static boolean tick(',1)[1].split('private static boolean recording(',1)[0]
        self.assertLess(tick.index('finishFirstEscape(p,s,false)'),tick.index('ParentRescueStory.tick(p,s)'))
        self.assertIn('frame>=30&&enterPrison(p,s)',tick)
        push=text.split('private static void pushIntoTunnel(',1)[1].split('private static void tunnel(',1)[0]
        self.assertLess(push.index('if(spawnInEscapeTunnel(p,s,1))'),push.index('go(p,s,ESCAPE_TUNNEL)'))
        finish=text.split('public static boolean finishFirstEscape(',1)[1].split('private static void brickDoor(',1)[0]
        self.assertLess(finish.index('!verifiedTransfer(p,s,p.server.overworld(),d.pos)'),finish.index('n.putBoolean("Complete",true)'))

    def test_reloaded_audio_retries_same_checkpoint_instead_of_silent_wait(self):
        playback=source('feast/ShowPlayback.java')
        start=playback.split('public static boolean start(',1)[1].split('public static void signal(',1)[0]
        tick=playback.split('public static boolean tick(',1)[1].split('public static boolean ownsSession(',1)[0]
        self.assertIn('n.hasUUID("Nonce")&&REQUESTED_AT.containsKey(n.getUUID("Nonce"))',start)
        self.assertIn('if(!n.hasUUID("Nonce")||!REQUESTED_AT.containsKey(n.getUUID("Nonce")))',tick)
        self.assertIn('if(!start(p,s,sound,music)&&n.getString("Blocker").isEmpty())',tick)
        self.assertIn('String sound=n.getString("Sound");boolean music=n.getBoolean("Music");',tick)
        self.assertLess(tick.index('if(!n.hasUUID("Nonce")||!REQUESTED_AT.containsKey('),
                        tick.index('if(!n.getBoolean("Started"))'))
        fixture=(ROOT/'src/wybietest/java/com/coraline/feast/RescuePlaybackGameTests.java').read_text()
        self.assertIn('savedPlaybackCannotStallSilentlyAfterRestart',fixture)
        self.assertIn('for(boolean acknowledged:new boolean[]{false,true})',fixture)

    def test_wybie_shoe_miss_requires_real_hit_and_reuses_original(self):
        story=source('feast/FirstEscapeStory.java')
        shoe=story.split('static void shoe(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static Vec3 escapeTarget(',1)[0]
        self.assertIn('if(e.cue()==1){',shoe)
        self.assertIn('if(e.time()>=80&&p.distanceToSqr(e)>16)',shoe)
        self.assertIn('n.putBoolean("ShoeMissed",true)',shoe)
        self.assertIn('n.putLong("ShoeFlightPosition",e.blockPosition().asLong())',shoe)
        self.assertIn('e.setDeltaMovement(Vec3.ZERO);e.setPos(missedShoePosition(p,e));e.frame(1,0,0);',shoe)
        self.assertIn('w.gameGesture(49,40);finishShoeThrow(p,s,e);return;',shoe)
        self.assertEqual(shoe.count('finishShoeThrow(p,s,e)'),1)
        self.assertLess(shoe.index('if(hit.isPresent()'),shoe.index('finishShoeThrow(p,s,e)'))
        self.assertNotIn('if(e.cue()==1){finishShoeThrow',shoe)
        interaction=story.split('public static boolean interact(ServerPlayer p,CinematicPropEntity prop)',1)[1].split('public static boolean block(',1)[0]
        self.assertIn('relaunchShoe(p,s,prop);',interaction)
        self.assertIn('prop.setDeltaMovement(p.getLookAngle().scale(1.05))',story)
        self.assertNotIn('if(giveTool(p,s,true)){prop.discard()',interaction)
        rendering=source('client/renderer/FirstEscapePresentation.java')
        self.assertIn('case CinematicPropEntity.STORY_SHOE->{if(e.cue()!=1)',rendering)

    def test_missing_story_shoe_requires_native_storage_absence_before_same_uuid_recovery(self):
        story=source('feast/FirstEscapeStory.java')
        lost=story.split('private static CinematicPropEntity recoverMissingShoe(',1)[1].split('private static boolean relaunchShoe(',1)[0]
        self.assertIn('n.getBoolean("ShoeMissed")',lost)
        self.assertIn('n.hasUUID("ShoeFlight")',lost)
        self.assertIn('n.contains("ShoeFlightPosition",Tag.TAG_LONG)',lost)
        self.assertIn('LoopProps.hold(p.server.overworld(),oldAt)',lost)
        self.assertIn('for(var level:p.server.getAllLevels())if(level.getEntity(id)!=null)',lost)
        self.assertIn('LegacyActorRecovery.check(p.server,id)',lost)
        self.assertIn('LegacyActorRecovery.Result.ABSENT',lost)
        self.assertLess(lost.index('LegacyActorRecovery.Result.ABSENT'),lost.index('recreated.setUUID(id)'))
        self.assertIn('recreated.configure(s,CinematicPropEntity.STORY_SHOE,0)',lost)
        self.assertIn('recreated.setUUID(id)',lost)
        self.assertIn('if(!p.server.overworld().addFreshEntity(recreated))return null',lost)
        self.assertNotIn('data(s).putUUID("ShoeFlight"',lost)
        shoe=story.split('static void shoe(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static void finishShoeThrow(',1)[0]
        self.assertIn('int missing=Math.min(240,n.getInt("ShoeMissingTicks")+1)',shoe)
        self.assertIn('if(missing>=60)',shoe)
        self.assertIn('if(p.tickCount%20==0)recoverMissingShoe(p,s)',shoe)
        self.assertIn('n.remove("ShoeMissingTicks")',shoe)
        self.assertIn('data(s).remove("ShoeMissingTicks");data(s).remove("ShoeMissed")',story)
        restart=source('client/FirstEscapeClient.java')
        self.assertIn('SecretsNetwork.request(data.getUUID("Choice"),"escape_shoe","retry")',restart)

    def test_post_escape_tea_visitor_recovery_preserves_original_uuid_and_refuses_unverified_absence(self):
        tea=source('feast/MissingParentsTea.java')
        actor=tea.split('private static TeaVisitorEntity actor(',1)[1].split('private static void spawn(',1)[0]
        self.assertIn('e.getType()==type',actor)
        self.assertIn('n.getUUID("Owner").equals(e.getPersistentData().getUUID("TeaOwner"))',actor)
        self.assertIn('n.getUUID("Token").equals(e.getPersistentData().getUUID("TeaToken"))',actor)
        proof=tea.split('static boolean teaActorMissingProof(',1)[1].split('private static boolean recoverAbsentVisitor(',1)[0]
        self.assertIn('LegacyActorRecovery.Result.ABSENT',proof)
        recover=tea.split('private static boolean recoverAbsentVisitor(',1)[1].split('private static void spawn(',1)[0]
        for token in (
                'n.getBoolean("Completed")','n.hasUUID("Token")','n.hasUUID(key)',
                'for(var world:p.server.getAllLevels())if(world.getEntity(original)!=null)',
                'LegacyActorRecovery.check(p.server,original)',
                'if(!teaActorMissingProof(result))',
                'actor.setUUID(original)', 'actor.getPersistentData().putUUID("TeaOwner",s.owner)',
                'actor.getPersistentData().putUUID("TeaToken",n.getUUID("Token"))',
                'level.addFreshEntity(actor)'):
            self.assertIn(token,recover)
        self.assertLess(recover.index('if(!teaActorMissingProof(result))'),
                        recover.index('actor.setUUID(original)'))
        spawned=tea.split('private static void spawn(ServerPlayer p,VisitData.Scene s)',1)[1].split('static boolean walk(',1)[0]
        self.assertIn('if(actor(p,n,key)==null&&p.tickCount%20==0)recoverAbsentVisitor(p,s,n,key)',spawned)
        self.assertIn('if(created)save(p,s,true);',spawned)
        self.assertNotIn('        save(p,s,true);\n    }',spawned)
        native=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('missingTeaVisitorsRequireNativeProofOfAbsenceBeforeOriginalUuidRecovery',native)

    def test_parent_rescue_kitchen_accepts_original_smoker_and_null_safe_registry_id(self):
        story=source('feast/ParentRescueStory.java')
        helper=story.split('static boolean kitchenStoveId(String id)',1)[1].split('private static boolean kitchen(',1)[0]
        for token in ('id!=null','id.contains("smoker")','id.contains("furnace")',
                      'id.contains("oven")','id.contains("stove")'):
            self.assertIn(token,helper)
        lookup=story.split('private static boolean kitchen(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static boolean block(',1)[0]
        self.assertIn('if(id!=null&&kitchenStoveId(id.getPath()))',lookup)
        game=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('canonicalKitchenSmokerCanStartParentRescueDinner',game)
        self.assertIn('blast_furnace',game)

    def test_kitchen_partial_save_waits_for_rebuilt_original_appliance_before_audio(self):
        story=source('feast/ParentRescueStory.java')
        readiness=story.split('static boolean kitchenCheckpointComplete(CompoundTag n)',1)[1].split('private static boolean kitchen(',1)[0]
        for key in ('n.contains("Kitchen",Tag.TAG_LONG)',
                    'n.contains("Stove",Tag.TAG_LONG)',
                    'n.contains("KitchenTable",Tag.TAG_LONG)',
                    'n.getLong("Kitchen")!=n.getLong("KitchenTable")'):
            self.assertIn(key,readiness)
        tick=story.split('public static void tick(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static boolean recording(',1)[0]
        self.assertIn('state==Phase.WAIT_KITCHEN_SEAT',tick)
        self.assertIn('state==Phase.H||state==Phase.I||state==Phase.J||state==Phase.K',tick)
        self.assertIn('state==Phase.SERVE)&&!kitchen(p,s)',tick)
        self.assertLess(tick.index('state==Phase.SERVE)&&!kitchen(p,s)'),
                        tick.index('boolean done=recording(p,s,n,sound)'))
        paused=tick.split('state==Phase.SERVE)&&!kitchen(p,s)',1)[1].split('if(!StoryTunnel.entryPhase(s)',1)[0]
        self.assertIn('ShowPlayback.stop(p,s)',paused)
        self.assertIn('n.remove("ReturnRecording")',paused)
        self.assertIn('n.putInt("Ticks",0)',paused)
        self.assertLess(paused.index('ShowPlayback.stop(p,s)'),paused.index('return;'))
        rebuild=story.split('private static boolean kitchen(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static boolean block(',1)[0]
        self.assertIn('if(kitchenCheckpointComplete(n)){',rebuild)
        self.assertIn('if(savedId!=null&&kitchenStoveId(savedId.getPath()))return true;',rebuild)
        self.assertLess(rebuild.index('if(kitchenCheckpointComplete(n)){'),
                        rebuild.index('var realm=RealmData.get(p.server)'))
        self.assertLess(rebuild.index('if(stove==null)'),rebuild.index('n.putLong("Kitchen",center.asLong())'))
        self.assertLess(rebuild.index('n.putLong("Stove",stove.asLong())'),rebuild.rindex('return true;'))
        game=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('interruptedKitchenSaveCannotAdvanceWithMissingStoveOrTable',game)

    def test_parent_rescue_kitchen_cannot_stall_forever_on_actor_navigation(self):
        story=source('feast/ParentRescueStory.java')
        serve=story.split('case SERVE->{',1)[1].split('case L->{',1)[0]
        self.assertIn('MissingParentsCat.walk(m,Vec3.atBottomCenterOf(target.west()),.65)',serve)
        self.assertIn('!arrived&&plate!=null&&serveRecoveryDue(t)&&t%20==0',serve)
        self.assertIn('arrived=recoverServingActor(p,s,m,target)',serve)
        self.assertIn('plate.setPos(Vec3.atBottomCenterOf(target).add(0,.9,0))',serve)
        self.assertLess(serve.index('arrived=recoverServingActor('),serve.index('go(p,s,Phase.L)'))
        method=story.split('private static boolean recoverServingActor(',1)[1].split('public static BlockPos cookingStand(',1)[0]
        self.assertIn('s.owner.equals(p.getUUID())',method)
        self.assertIn('mother.level()!=p.level()||!mother.isOtherParent()',method)
        self.assertIn('!level.noCollision(mother,box)',method)
        self.assertIn('level.getWorldBorder().isWithinBounds(box)',method)
        self.assertIn('MissingParentsCat.arrived(mother,Vec3.atBottomCenterOf(wanted))',method)
        self.assertIn('Clear a reachable serving space beside the original kitchen table.',method)
        game=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('parentRescueKitchenServingWaitsBeforeRecoveringOriginalActor',game)

    def test_finale_open_floor_has_delayed_safe_return_to_actual_hole(self):
        finale=source('feast/BeldamFinale.java')
        floor=finale.split('private static boolean guideOpenedFloorFall(',1)[1].split('public static boolean preflight(',1)[0]
        self.assertIn('n.getBoolean("FloorOpened")',floor)
        self.assertIn('openedFloorFallRecoveryDue(t,duration)',floor)
        self.assertIn('level.getBlockState(room.below()).isAir()',floor)
        self.assertIn('level.getBlockCollisions(p,body).iterator().hasNext()',floor)
        self.assertIn('level.getWorldBorder().isWithinBounds(body)',floor)
        self.assertIn('now=level.getGameTime()',floor)
        self.assertIn('n.putLong("FallGuideRetryAt",now+80)',floor)
        self.assertIn('p.teleportTo(level,landing.x,landing.y,landing.z',floor)
        final_o=finale.split('case FINAL_O->{',1)[1].split('case FINAL_P->',1)[0]
        self.assertLess(final_o.index('!prepareFall(p,s)'),final_o.index('guideOpenedFloorFall('))
        self.assertLess(final_o.index('guideOpenedFloorFall('),final_o.index('&&enterWeb(p,s,m)&&done'))
        native=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('finaleOpenedFloorRecoversOnlyAfterRecordedFallCueAndGrace',native)

    def test_stranded_post_escape_checkpoint_recovers_original_world_not_parent_rescue(self):
        story=source('feast/FirstEscapeStory.java')
        tick=story.split('public static boolean tick(ServerPlayer p,VisitData.Scene s)',1)[1].split('private static boolean recording(',1)[0]
        self.assertIn('if(FirstEscapeTimeline.overworld(state)&&state!=ESCAPE_COMPLETE)',tick)
        self.assertIn('var real=source==null?null:FamilyData.level(p.server,source.dimension);',tick)
        self.assertIn('n.putBoolean("Recover",true)',tick)
        recover=story.split('private static boolean recoverCheckpoint(',1)[1].split('private static void prepareSealedRoom(',1)[0]
        self.assertIn('if(FirstEscapeTimeline.overworld(state)&&!ParentRescueStory.started(s))',recover)
        self.assertIn('real==null||p.serverLevel()!=real&&!verifiedTransfer(p,s,real,door.pos)',recover)
        self.assertLess(recover.index('if(FirstEscapeTimeline.overworld(state)&&!ParentRescueStory.started(s))'),recover.index('n.remove("Recover")'))

    def test_original_shoe_retry_works_without_clicking_ground_prop(self):
        story=source('feast/FirstEscapeStory.java')
        retry=story.split('public static boolean retryShoe(ServerPlayer p,UUID nonce)',1)[1].split('private static boolean relaunchShoe(',1)[0]
        self.assertIn('n.getUUID("Choice").equals(nonce)',retry)
        self.assertIn('!n.getBoolean("ShoeMissed")',retry)
        self.assertIn('return relaunchShoe(p,s,shoe);',retry)
        self.assertIn('LoopProps.hold(p.server.overworld(),oldAt)',retry)
        self.assertIn('LegacyActorRecovery.Result.ABSENT',retry)
        self.assertIn('recreated.setUUID(id)',retry)
        flight=story.split('private static boolean relaunchShoe(',1)[1].split('private static Vec3 missedShoePosition(',1)[0]
        self.assertIn('prop.kind()!=CinematicPropEntity.STORY_SHOE||prop.cue()!=1',flight)
        self.assertIn('n.getUUID("ShoeFlight").equals(prop.getUUID())',flight)
        self.assertIn('prop.setDeltaMovement(p.getLookAngle().scale(1.05))',flight)
        self.assertIn('n.remove("ShoeMissed")',flight)
        position=story.split('private static Vec3 missedShoePosition(',1)[1].split('public static void throwShoe(',1)[0]
        self.assertIn('new double[]{1.45,1.15,1.85}',position)
        self.assertIn('getBlockCollisions(shoe,box)',position)
        self.assertIn('ClipContext.Block.COLLIDER',position)
        self.assertIn('return p.position().add(0,.2,0);',position)
        client=source('client/FirstEscapeClient.java')
        self.assertIn('flag("ShoeMissed")',client)
        self.assertIn('use&&!shoeUseDown',client)
        self.assertIn('SecretsNetwork.request(data.getUUID("Choice"),"escape_shoe","retry")',client)
        network=source('secrets/SecretsNetwork.java')
        self.assertIn('FirstEscapeStory.retryShoe(p,r.nonce)',network)
        self.assertIn('packet.putBoolean("ShoeMissed"',story)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/FirstEscapeStoryGameTests.java').read_text()
        self.assertIn('shoeCanBeRetriedWithoutClickingTinyGroundHitbox',native)

    def test_missing_bed_cat_handoff_requires_the_original_actor_before_completion(self):
        cat=source('feast/MissingParentsCat.java')
        recovery=cat.split('if(family.bed==null){',1)[1].split('if(n.contains("MissingBedTicks"))',1)[0]
        self.assertIn('if(!n.getBoolean("Started"))',recovery)
        self.assertIn('Place your family',recovery)
        self.assertIn('var originalCat=cat(p,s);',recovery)
        self.assertIn('if(originalCat==null)',recovery)
        self.assertIn('missingBedRecoveryReady(n.getBoolean("Started"),originalCat!=null,missing)',recovery)
        self.assertIn('if(!StoryTunnel.restore(p,s))',recovery)
        self.assertLess(recovery.index('if(originalCat==null)'),
                        recovery.index('n.putBoolean("DollsRetrieved",true)'))
        self.assertLess(recovery.index('if(!StoryTunnel.restore(p,s))'),
                        recovery.index('n.putBoolean("Complete",true)'))
        self.assertIn('n.remove("MissingBedTicks")',cat)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/MissingParentsCatGameTests.java').read_text()
        self.assertIn('missingBedBeforeCatStartsPreservesTheMirrorClue',native)
        self.assertIn('missingBedFallbackCannotLoseTheOriginalCatForTheBoss',native)
        self.assertIn('catWatchdogCannotAwardDollsWhileOriginalCatIsUnloaded',native)
        continuation=(ROOT/'src/wybietest/java/com/coraline/feast/StoryContinuationGameTests.java').read_text()
        scenario=continuation.split('skippedEscapeQueuesMissingRealmAndResumesBlockedCatClue(',1)[1].split('catDepartureCannotCompleteAnUnrestoredPassage',1)[0]
        self.assertIn('ShowActors.acquire(p,s,"cat",level,',scenario)
        self.assertLess(scenario.index('ShowActors.acquire(p,s,"cat",level,'),
                        scenario.index('MissingParentsCat.cutsceneSafety(p,s);'))
        safety=cat.split('static void cutsceneSafety(ServerPlayer p,VisitData.Scene s)',1)[1].split('static boolean parkInTunnel(',1)[0]
        self.assertIn('if(state==Phase.F&&cat(p,s)==null)',safety)
        self.assertLess(safety.index('if(state==Phase.F&&cat(p,s)==null)'),
                        safety.index('if(state==Phase.F&&!StoryTunnel.restore(p,s))'))
        follow=safety.split('case FOLLOW_BED->{',1)[1].split('case E->{',1)[0]
        self.assertIn('if(actor==null)',follow)
        self.assertLess(follow.index('if(actor==null)'),
                        follow.index('n.putBoolean("DollsRetrieved",true)'))
        self.assertIn('StoryCutsceneWatchdog.retry(s,"post_escape_cat")',follow)
        performance=safety.split('case E->{',1)[1].split('case F->{',1)[0]
        self.assertIn('if(cat(p,s)==null)',performance)
        self.assertLess(performance.index('if(cat(p,s)==null)'),
                        performance.index('n.putBoolean("DollsRetrieved",true)'))
        finish=safety.split('case F->{',1)[1].split('default->{}',1)[0]
        self.assertIn('if(cat(p,s)==null)',finish)
        self.assertLess(finish.index('if(cat(p,s)==null)'),
                        finish.index('n.putBoolean("TunnelRestored",true)'))

    def test_first_escape_wybie_tea_cat_and_return_tunnel_handoffs(self):
        # Source dispatch checks supplement, but do not replace, an in-game run.
        escape=source('feast/FirstEscapeStory.java')
        self.assertIn('case THROW_SHOE->{ensureTool(p,s,true);shoe(p,s);',escape)
        self.assertIn('if(wall.getType()!=HitResult.Type.MISS||e.time()>45||e.getY()<p.getY()-12||e.distanceToSqr(p)>24*24',escape)
        self.assertIn('case MISSING_G,BICYCLE_DEPARTURE->departure(p,s);',escape)
        self.assertIn('MissingParentsTea.select(p,s,false)',escape)
        tea=source('feast/MissingParentsTea.java')
        self.assertIn('n.putBoolean("Completed",true);n.putBoolean("Active",false)',tea)
        self.assertIn('s.loop.story.putBoolean("SeeingStoneReceipt",true)',tea)
        cat=source('feast/MissingParentsCat.java')
        self.assertIn('tea.getBoolean("Completed")&&!tea.getBoolean("Active")',cat)
        self.assertIn('!s.loop.story.getBoolean("SeeingStonePending")',cat)
        self.assertIn('StoryTunnel.restore(p,s)',cat)
        tunnel=source('feast/StoryTunnel.java')
        self.assertIn('else ParentRescueStory.begin(p,scene);',tunnel)
        self.assertIn('return StoryReturnContext.inCorridor',tunnel)
        supper=source('feast/ParentRescueStory.java')
        self.assertIn('n.putBoolean("KeySwallowed",true)',supper)
        self.assertIn('StoryItemCustody.capture(p,s,"Key"',supper)

    def test_final_room_waits_for_original_cat_and_recovers_old_locked_saves(self):
        story=source('feast/RescueEncounters.java')
        house=story.split('case HOUSE_SAFE->{',1)[1].split('case ROOM_LOCKED->{',1)[0]
        self.assertIn('if(BeldamFinale.catAtMantel(p,s)==null)',house)
        self.assertLess(house.index('if(BeldamFinale.catAtMantel(p,s)==null)'),
                        house.index('closeRoom(p,s)'))
        locked=story.split('case ROOM_LOCKED->{',1)[1].split('default->{}',1)[0]
        for required in ('!OtherWorld.isOther(p.level())',
                         'RescueEyes.all(s)&&n.getBoolean("CatRescued")&&n.getBoolean("HouseSafe")',
                         'objectives?BeldamFinale.catAtMantel(p,s):null',
                         'if(!objectives||cat==null)',
                         'n.putBoolean("RoomLocked",false)',
                         'FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s),true)',
                         'FirstEscapeStory.setDoor(p,FirstEscapeScene.entrance(s).south(),true)',
                         '!FirstEscapeScene.fullyInRoom(s,p.getBoundingBox())',
                         'closeRoom(p,s)',
                         'RescueChapter.go(p,s,FINAL_A)'):
            self.assertIn(required,locked)
        self.assertLess(locked.index('if(!objectives||cat==null)'),
                        locked.index('RescueChapter.go(p,s,FINAL_A)'))
        self.assertLess(locked.index('if(!FirstEscapeScene.fullyInRoom(s,p.getBoundingBox()))'),
                        locked.index('closeRoom(p,s)'))
        self.assertLess(locked.index('closeRoom(p,s)'),
                        locked.index('RescueChapter.go(p,s,FINAL_A)'))
        chapter=source('feast/RescueChapter.java')
        tick=chapter.split('public static void tick(ServerPlayer p,VisitData.Scene s)',1)[1].split('static final class RecordingRecovery',1)[0]
        self.assertNotIn('state==Phase.ROOM_LOCKED&&!BeldamFinale.keepPlayerInRoom(p,s)',tick)
        self.assertIn('!BeldamFinale.preflight(p,s)',tick)
        self.assertIn('RescueEncounters.tick(p,s,state,t,done)',tick)
        finale=source('feast/BeldamFinale.java')
        mantel=finale.split('static Mob catAtMantel(',1)[1].split('private static void catFlight(',1)[0]
        self.assertIn('boolean preFight=state==HOUSE_SAFE||state==ROOM_LOCKED;',mantel)
        self.assertIn('if(!n.getBoolean("CatMantel")||preFight&&cat.distanceToSqr(at)>.25)',mantel)
        self.assertIn('cat.setPos(at);n.putBoolean("CatMantel",true)',mantel)
        self.assertLess(mantel.index('var cat=ShowActors.recoverCat('),
                        mantel.index('cat.setPos(at)'))
        self.assertNotIn('state==CAT_THROW',mantel)

    def test_collapse_stale_cat_pickup_recovers_only_original_unrescued_actor(self):
        encounter=source('feast/RescueEncounters.java')
        collapse=encounter.split('private static void collapse(',1)[1].split('private static void carryCat(',1)[0]
        self.assertIn('ShowActors.recoverCat(p,s,level,',collapse)
        self.assertIn('if(cat==null)',collapse)
        self.assertIn('if(!n.getBoolean("CatRescued"))',collapse)
        self.assertIn('RescueEpilogue.safeReturn(level,p,Pose.STANDING,',collapse)
        self.assertIn('!level.noCollision(cat,cat.getBoundingBox())',collapse)
        self.assertIn('RescueChallengeMath.restageUnrescuedCat(n.getBoolean("CatCollapsePlaced"),false,',collapse)
        self.assertIn('cat.setPos(at);',collapse)
        self.assertIn('n.putBoolean("CatCollapsePlaced",true)',collapse)
        self.assertLess(collapse.index('if(cat==null)'),collapse.index('cat.setPos(at);'))
        self.assertLess(collapse.index('if(!n.getBoolean("CatRescued"))'),collapse.index('cat.setPos(at);'))
        self.assertLess(collapse.index('RescueEpilogue.safeReturn('),collapse.index('cat.setPos(at);'))
        self.assertIn('n.remove("CatCollapsePosition")',encounter)
        math=source('feast/RescueChallengeMath.java')
        self.assertIn('!rescued&&(!placed||obstructed||!Double.isFinite(distanceSquared)||distanceSquared>64)',math)
        gametest=(ROOT/'src/gametest/java/com/coraline/feast/RescueSoftlockGameTests.java').read_text()
        self.assertIn('savedUnrescuedCatCannotDisappearFromCollapsePickup',gametest)

    def test_late_rescue_cutscenes_keep_safety_but_bosses_and_crawl_do_not_autocomplete(self):
        chapter=source('feast/RescueChapter.java')
        safety=chapter.split('static void cutsceneSafety(',1)[1].split('public static void recover(',1)[0]
        # A former ordinal check silently disabled every Bobinsky, finale and
        # postboss cinematic watchdog as soon as the Bobinsky chapter began.
        self.assertNotIn('state.ordinal()>=Phase.BOB_A.ordinal()',safety)
        self.assertIn('RescueBosses.fighting(state)||state==Phase.TUNNEL_CHASE',safety)
        for name in ('Phase.FURNACE','Phase.BOB_TENT','Phase.SWARM_TRANSFORM',
                     'Phase.WARDROBE_ASIDE','Phase.HAND_FALL','Phase.BANGING',
                     'Phase.EYES_CLOSE','Phase.CELESTIAL_OPEN','Phase.ORBS'):
            self.assertIn(name,safety)
        self.assertIn('if(voice.isEmpty()&&!motion)return;',safety)
        self.assertIn('RescueEncounters.tick(p,s,state,tail,true)',safety)
        self.assertIn('BeldamFinale.tick(p,s,state,tail,true)',safety)
        self.assertIn('RescueEpilogue.tick(p,s,state,tail,true)',safety)
        self.assertIn('!BeldamFinale.preflight(p,s)',safety)
        self.assertIn('case REUNION_A->"reunion_a"',chapter)
        self.assertIn('case FAREWELL_C->"farewell_c"',chapter)

    def test_old_proposal_cannot_consume_rescue_interactions(self):
        proposal=source('feast/BlackIsTraditional.java')
        interaction=proposal.split('public static boolean interact(ServerPlayer p,VisitData.Scene s,CinematicPropEntity prop)',1)[1].split('public static boolean choice(',1)[0]
        self.assertIn('!active(s)||FirstEscapeStory.engaged(s)||!prop.owned(s)',interaction)
        prop=source('feast/CinematicPropEntity.java')
        general=prop.split('InteractionResult interact(Player actor,InteractionHand hand)',1)[1].split('AABB getBoundingBoxForCulling()',1)[0]
        self.assertLess(general.index('RescueChapter.interact(p,this)'),general.index('BlackIsTraditional.interact(p,s,this)'))

    def test_late_rescue_props_are_not_stolen_by_first_escape_interactions(self):
        first=source('feast/FirstEscapeStory.java')
        handler=first.split('public static boolean interact(ServerPlayer p,CinematicPropEntity prop)',1)[1].split('public static boolean block(',1)[0]
        self.assertIn('if(MissingParentsCat.interact(p,prop))return true;',handler)
        self.assertIn('!ParentRescueStory.started(s)&&phase!=ESCAPE_COMPLETE',handler)
        self.assertIn('prop.kind()<=CinematicPropEntity.STORY_SHOE;',handler)
        self.assertNotIn('return prop.owner.equals(p.getUUID())&&prop.kind()>=CinematicPropEntity.STORY_CANE;',handler)
        prop=source('feast/CinematicPropEntity.java')
        specific=prop.split('InteractionResult interactAt(',1)[1].split('boolean canBeCollidedWith()',1)[0]
        self.assertLess(specific.index('FirstEscapeStory.interact(p,this)'),specific.index('return interact(player,hand);'))
        generic=prop.split('InteractionResult interact(Player actor,InteractionHand hand)',1)[1].split('AABB getBoundingBoxForCulling()',1)[0]
        self.assertIn('if(RescueChapter.interact(p,this))return InteractionResult.CONSUME;',generic)

    def test_boss_retry_never_uses_wrong_player_dimension_after_reload(self):
        bosses=source('feast/RescueBosses.java')
        retry=bosses.split('public static void retry(ServerPlayer p,VisitData.Scene s,String reason)',1)[1].split('private RescueBosses()',1)[0]
        self.assertIn('var level=fighting(state)?arena(p,state):p.serverLevel();',retry)
        self.assertIn('RescueChapter.transfer(p,level,at)',retry)
        self.assertNotIn('RescueChapter.transfer(p,p.serverLevel(),at)',retry)
        self.assertIn('bossRetryUsesOriginalArenaDimensionEvenWhenPlayerWasStrandedElsewhere',
                      (ROOT/'src/wybietest/java/com/coraline/feast/RescueBossGameTests.java').read_text())

    def test_rat_return_and_postboss_seal_recheck_after_reload(self):
        encounter=source('feast/RescueEncounters.java')
        handoff=encounter.split('else if(leg==1)',1)[1].split('else {ShowHouseAccess',1)[0]
        self.assertIn('rat=returnedRat;',handoff)
        self.assertIn('recordChaseRat(n,rat);',encounter)
        self.assertIn('n.getInt("RatLeg")>=2',encounter)
        self.assertIn('for(var waypoint:chaseHouseRoute(s))LoopProps.hold(savedWorld,waypoint)',encounter)
        self.assertIn('ratReturnPersistsTransferredOriginalAndRecoversLegacyCircusCheckpoint',
                      (ROOT/'src/wybietest/java/com/coraline/feast/RescueRatGameTests.java').read_text())
        finale=source('feast/BeldamFinale.java')
        recovery=finale.split('static boolean originalExitReady(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static boolean preflight(',1)[0]
        self.assertIn('!originalDoorSealed(home,source,s.door)',recovery)
        self.assertIn('n.putBoolean("PermanentlySealed",false);',recovery)
        self.assertIn('RescueChapter.go(p,s,LOCK_DOOR);',recovery)
        self.assertIn('originalExitReady(p,s),"A reopened saved door',
                      (ROOT/'src/wybietest/java/com/coraline/feast/RescueFinaleInputGameTests.java').read_text())

    def test_ratmaster_clickable_ball_reaches_exact_saved_rat_and_eye_gate(self):
        prop=source('feast/CinematicPropEntity.java')
        # The pick box covers the tiny canonical passenger. Direct rat clicks
        # cannot be the sole way to obtain the required final ghost eye.
        self.assertIn('RescueProps.RAT_BALL)return new AABB(getX()-.30,getY(),getZ()-.30,getX()+.30,getY()+1.35,getZ()+.30)',prop)
        self.assertIn('kind()==RescueProps.RAT_BALL?.45',prop)
        self.assertIn('return RescueProps.kind(kind())',prop)
        self.assertIn('.sized(.3F,.25F)',source('registry/ModEntities.java'))
        encounter=source('feast/RescueEncounters.java')
        interaction=encounter.split('public static boolean interact(ServerPlayer p,VisitData.Scene s,Entity target)',1)[1].split('public static void retry(',1)[0]
        self.assertIn('ball.kind()==RescueProps.RAT_BALL&&ball.owned(s)',interaction)
        self.assertIn('n.hasUUID("ChaseRat")&&n.getUUID("ChaseRat").equals(rider.getUUID())',interaction)
        self.assertIn('s.owner.equals(rider.getPersistentData().getUUID("RescueOwner"))',interaction)
        self.assertLess(interaction.index('return interact(p,s,passenger);'),
                        interaction.index('if(phase==RAT_CHASE&&n.hasUUID("ChaseRat")'))
        self.assertIn('!canClaimRatEye(n.getInt("RatLeg"))||!n.getBoolean("RatHouseMoved")',interaction)
        self.assertIn('RescueEyes.claim(p,s,2)',interaction)

    def test_hidden_rescue_props_do_not_steal_scene_clicks(self):
        source_text=source('feast/CinematicPropEntity.java')
        pick=source_text.split('@Override public boolean isPickable()',1)[1].split('@Override public boolean hurt(',1)[0]
        self.assertIn('if(isInvisible()&&RescueProps.kind(kind()))return false;',pick)
        self.assertLess(pick.index('if(isInvisible()&&RescueProps.kind(kind()))return false;'),
                        pick.index('return RescueProps.kind(kind())'))
        encounter=source('feast/RescueEncounters.java')
        self.assertIn('RescueProps.hide(p,s,"GardenEye")',encounter)
        self.assertIn('RescueProps.hide(p,s,"Swarm")',source('feast/RescueBosses.java'))

    def test_final_rescue_crawl_and_key_lock_require_verified_physical_exit(self):
        finale=source('feast/BeldamFinale.java')
        crawl=finale.split('private static void tunnel(',1)[1].split('public static void input(',1)[0]
        self.assertLess(crawl.index('if(progress<RescuePassage.LENGTH||!done)return;'),
                        crawl.index('if(!RescueChapter.transfer(p,level,source.pos))return;'))
        self.assertLess(crawl.index('if(!RescueChapter.transfer(p,level,source.pos))return;'),
                        crawl.index('RescueChapter.go(p,s,LOCK_DOOR)'))
        lock=finale.split('public static boolean block(ServerPlayer p,BlockPos at)',1)[1].split('public static void retry(',1)[0]
        self.assertIn('!n.getBoolean("CrawlReachedOverworld")||!n.getBoolean("KeyRecovered")',lock)
        self.assertLess(lock.index('if(!p.serverLevel().getBlockState(at).equals(closed))return true;'),
                        lock.index('n.putBoolean("PermanentlySealed",true)'))
        epilogue=source('feast/RescueEpilogue.java')
        self.assertIn('if(!reunionReady(s))return;',epilogue)
        self.assertIn('if(!restoreVision(p,s))return;',epilogue)
        self.assertIn('if(RescueEyes.release(p,s))completeFarewell(p,s)',epilogue)

    def test_accepted_child_crosses_before_escape_is_archived(self):
        text=source('feast/GhostVictimDirector.java')
        escaped=text.rsplit('case VICTIM_ESCAPED->{',1)[1].split('case BETWEEN_ACTIVITIES',1)[0]
        self.assertLess(escaped.index('GhostEscapeHunt.openPassage'),escaped.index('n.putBoolean("VictimReachedDoor",true)'))
        self.assertLess(escaped.index('same.level()!=source'),escaped.index('archiveVictim(n,"ESCAPED")'))

    def test_later_parent_rescue_has_original_door_reentry(self):
        door=source('feast/StoryTunnel.java')
        self.assertIn('else resumeChapter(p,scene);',door)
        resume=door.split('private static void resumeChapter(',1)[1].split('public static boolean controls(',1)[0]
        self.assertIn('SafeReturnPhase',resume)
        self.assertIn('TheatreSite.entrance',resume)
        self.assertIn('MouseCircusSite.audience',resume)
        self.assertIn('RescuePassage.enter',resume)
        self.assertNotIn('ParentRescueStory.begin',resume)

    def test_registration_uses_map_proof_and_supper_recovery_preserves_saved_actors(self):
        transport=source('feast/PalaceTransport.java')
        self.assertIn('PalaceJourney.ownsTransport(p,s)',transport)
        self.assertNotIn('PalaceJourney.owns(p,s)',transport)
        self.assertIn('!sites.palaceArrivalReady()',transport)
        self.assertIn('PalaceJourney.owns(p,s)',source('feast/PalaceWellEncounter.java'))
        for path,method,admit in (
            ('feast/AcceptedButtonEyesWorld.java','recoverSupperParent','level.addFreshEntity(actor)'),
            ('feast/GhostVictimActors.java','recoverSupper','var actor=arrive('),
        ):
            text=source(path).split(method+'(',1)[1]
            self.assertLess(text.index('LegacyActorRecovery.Result.ABSENT'),text.index(admit))

    def test_rescue_reentry_preserves_actual_venue_exits_and_rat_leg(self):
        tunnel=source('feast/StoryTunnel.java')
        recover=tunnel.split('private static void resumeChapter(',1)[1].split('public static boolean controls(',1)[0]
        self.assertIn('ShowTravel.recoverRescueReturn(p,scene,"TheatreReturn"',recover)
        self.assertIn('ShowTravel.recoverRescueReturn(p,scene,"CircusReturn"',recover)
        self.assertIn('chapter.getInt("RatLeg")>=2',recover)
        self.assertIn('level=p.server.getLevel(OtherWorld.KEY);',recover)
        self.assertLess(recover.index('recoverRescueReturn(p,scene,"TheatreReturn"'),
                        recover.index('level=p.server.getLevel(ActivityWorld.KEY);landing=TheatreSite.entrance'))
        travel=source('feast/ShowTravel.java')
        lease=travel.split('static boolean recoverRescueReturn(',1)[1].split('private static CompoundTag ledger(',1)[0]
        self.assertIn('data/coraline_first_visits.dat',lease)
        self.assertIn('record.equals(disk.loop.event.getCompound(key))',lease)
        self.assertIn('destination.distanceToSqr(Vec3.atBottomCenterOf(returnNear))<=64',lease)

    def test_rescue_passage_requires_readback_and_cleanup_retains_failures(self):
        passage=source('feast/RescuePassage.java')
        self.assertIn('PassageVerified',passage)
        self.assertIn('if(!level.getBlockState(at).equals(expected))',passage)
        self.assertIn('if(!expected.equals(NbtUtils.readBlockState(lookup,row.getCompound("Placed"))))',passage)
        self.assertIn('p.server.overworld().getDataStorage().save();',passage)
        cleanup=source('feast/RescueSiteCleanup.java')
        self.assertLess(cleanup.index('if(!level.getBlockState(at).equals(original))continue;'),
                        cleanup.index('cells.remove(position);budget--;changed=true;'))
        self.assertIn('CleanupOffset',cleanup)

    def test_supper_parent_recovery_loads_original_dimension_first(self):
        world=source('feast/AcceptedButtonEyesWorld.java')
        recovery=world.split('static ParentEntity recoverSupperParent(',1)[1].split('private static ParentEntity parent(',1)[0]
        self.assertIn('SupperMotherDimension',recovery)
        self.assertIn('SupperFatherDimension',recovery)
        self.assertIn('FamilyData.level(p.server,dimension)',recovery)
        self.assertLess(recovery.index('LoopProps.hold(recordedWorld,origin)'),
                        recovery.index('LegacyActorRecovery.check(p.server,id)'))
        director=source('feast/GhostVictimDirector.java')
        self.assertIn('n.putString("SupperMotherDimension"',director)
        self.assertIn('n.putString("SupperFatherDimension"',director)

    def test_postboss_company_grade_works_without_palace_well(self):
        generation=source('palace/PalaceWorldGeneration.java')
        grade=generation.split('private static boolean column(',1)[1].split('private static void terrain(',1)[0]
        self.assertIn('PalaceGenerationRules.stage(n)==COMPANY_PREPARE',grade)
        self.assertIn('PalaceGenerationRules.companyFoundationInner(',grade)
        self.assertLess(grade.index('PalaceGenerationRules.stage(n)==COMPANY_PREPARE'),
                        grade.index('d.position(Kind.PALACE).orElseThrow()'))
        sites=source('palace/PalaceSiteData.java')
        recover=sites.split('void retryLegacyPause()',1)[1].split('boolean prioritizeCompany()',1)[0]
        self.assertIn('earlyCompanyMissingPalace',recover)
        self.assertIn('COMPANY_PREPARE cursor',recover)
        self.assertIn('generation.contains("DeferredPalace"',recover)

    def test_well_reconnect_reuses_original_bicycle(self):
        bike=source('feast/WybieBicycleEntity.java')
        rebind=bike.split('boolean reconcileWellSession(',1)[1].split('public boolean ownedBy(',1)[0]
        for identity in ('expectedOwner.equals(owner)','expectedDoor.equals(door)',
                         'originalWybie.equals(rider)','PalaceWellOwner','entityData.set(SESSION,'):
            self.assertIn(identity,rebind)
        well=source('feast/PalaceWellEncounter.java')
        rider=well.split('private static WybieBicycleEntity bike(',1)[1].split('private static Vec3 entryTarget(',1)[0]
        self.assertIn('b.reconcileWellSession(s.owner,s.door,progress.originalWybie().orElseThrow(),progress.session())',rider)
        self.assertLess(rider.index('reconcileWellSession('),rider.index('LegacyActorRecovery.check(p.server,original)'))

    def test_lost_map_is_recoverable_under_original_map_prerequisites(self):
        journey=source('feast/PalaceJourney.java')
        recovery=journey.split('public static void recoverMap(',1)[1]
        self.assertIn('mapEligible(p,scene)',recovery)
        self.assertNotIn('!owns(p,scene)',recovery)

    def test_well_resume_reconciles_authenticated_finished_combat(self):
        encounter=source('feast/PalaceWellEncounter.java')
        resume=encounter.split('public static int resume(',1)[1].split('@SubscribeEvent public static void tick(',1)[0]
        self.assertIn('PalaceHandCombat.load(s.owner,s.door,h.getUUID()',resume)
        self.assertIn('progress.session().equals(fight.session())',resume)
        self.assertIn('fight.complete()&&progress.phase()==BOSS',resume)
        self.assertIn('fight.complete()&&progress.phase().ordinal()>BOSS.ordinal()',resume)
        self.assertIn('!oldSession&&!interruptedVictory&&!completedEarlier',resume)
        self.assertIn('progress.defeated(s.owner,progress.session(),h.getUUID())',resume)
        self.assertIn('fight.resume(s.owner,progress.session())',resume)
        self.assertLess(resume.index('PalaceHandCombat.load('),
                        resume.index('GlobalHandEncounter.claim('))
        self.assertLess(resume.index('GlobalHandEncounter.claim('),
                        resume.index('progress.resume(s.owner)'))

    def test_accepted_escape_dialogue_and_room_exit_recovery(self):
        mirrors=source('feast/GhostVictimMirrors.java')
        dispatch=mirrors.split('public static boolean choice(',1)[1].split('private static boolean escapeChoice(',1)[0]
        self.assertIn('String extra=indexedEscapeAction(n,selected);',dispatch)
        self.assertIn('return extra!=null&&escapeChoice(p,s,n,child,nonce,extra);',dispatch)
        self.assertIn('wireAction.startsWith("escape:")',dispatch)
        self.assertIn('return wireAction.substring(7);',dispatch)
        rooms=source('feast/GhostEscapeRooms.java')
        self.assertIn('!authoredExit(s,id,door)',rooms)
        self.assertIn('if(at.equals(excluded))continue;',rooms)
        covert=source('feast/GhostEscapeCovert.java')
        leaving=covert.split('case LEAVE_ROOM->{',1)[1].split('case APPROACH_EXIT->',1)[0]
        self.assertIn('travel==GhostVictimNavigation.Result.BLOCKED',leaving)
        self.assertIn('GhostEscapeRooms.reachableExit(s,roomId,level,child,exit)',leaving)
        self.assertLess(leaving.index('if(travel==GhostVictimNavigation.Result.BLOCKED)'),
                        leaving.index('GhostEscapeProgress.leftRoom(ghost,true)'))
        director=source('feast/GhostVictimDirector.java')
        requests=director.split('public static void request(',1)[1].split('public static boolean parent(',1)[0]
        self.assertIn('||value.equals("escape_door")||value.equals("escape_go")',requests)
        self.assertIn('if(!GhostEscapePerception.destinationSignalPoint(p,s,n,child,at))return;',requests)

    def test_accepted_escape_retries_physically_return_to_the_bedroom_mirror(self):
        # These are source-dispatch regressions, NOT proof of a Minecraft playthrough.
        progress=source('feast/GhostEscapeProgress.java')
        self.assertIn('phase==Phase.WAIT_GO||phase==Phase.WAIT_LURE||phase==Phase.STEAL_KEY?Phase.READY',progress)
        self.assertIn('move(n,Phase.READY,GhostVictimProgress.clock(ghost));return true;',progress)
        covert=source('feast/GhostEscapeCovert.java')
        planning=covert.split('switch(GhostEscapeProgress.phase(e)){',1)[1].split('case APPROACH_ROOM->{',1)[0]
        self.assertIn('case READY,STONE_REPORT,ROOM_REPORT,KEY_REPORT->{',planning)
        self.assertIn('GhostVictimRoutines.travel(p,s,ghost,child,destination,false)',planning)
        self.assertIn('GhostVictimMirrors.childAtMirror(ghost,"bedroom",child,true)',planning)
        self.assertIn('case READY->',planning)
        self.assertLess(planning.index('GhostVictimRoutines.travel(p,s,ghost,child,destination,false)'),
                        planning.index('GhostVictimMirrors.childAtMirror(ghost,"bedroom",child,true)'))
        mirrors=source('feast/GhostVictimMirrors.java')
        self.assertIn('!childPresent(p,n,n.getString("Mirror"),child)',mirrors)

    def test_accepted_activity_parent_landing_can_be_obstructed_without_crashing(self):
        world=source('feast/ActivityWorld.java')
        entry=world.split('public static void enter(ServerPlayer p,VisitData.Scene s)',1)[1].split('public static String enterDebug(',1)[0]
        self.assertIn('if(at!=null){mom.getNavigation().stop();mom.teleportTo(',entry)
        returned=world.split('private static void finishActivityReturn(',1)[1].split('static boolean recoverCompletedReturn(',1)[0]
        self.assertIn('if(safe!=null)mom.teleportTo(',returned)

    def test_pending_stone_errand_cannot_be_skipped_by_night_watchdog(self):
        director=source('feast/GhostVictimDirector.java')
        self.assertGreaterEqual(director.count('if(GhostEscapeProgress.homeErrandPending(n))'),2)
        progress=source('feast/GhostEscapeProgress.java')
        self.assertIn('static boolean homeErrandPending(CompoundTag ghost)',progress)
        self.assertIn('owns(ghost,escape)&&phase(escape)==Phase.HOME_ERRAND',progress)
        covert=source('feast/GhostEscapeCovert.java')
        self.assertIn('visitorLanding(home,front,1,null)',covert)
        self.assertIn('visitorLanding(home,front,-1,first)',covert)
        self.assertIn('first==null||second==null',covert)

    def test_accepted_escape_options_match_actual_server_gates(self):
        mirror=source('feast/GhostVictimMirrors.java')
        display=mirror.split('private static void screen(',1)[1].split('private static void addOption(',1)[0]
        self.assertIn('escapePlanOfferReady(n)',display)
        self.assertIn('ambushOfferReady(escape)',display)
        self.assertIn('ghost.contains("BedtimeWindowStarted",Tag.TAG_LONG)',display)
        self.assertIn('GhostVictimProgress.readyToHelp(ghost)',display)
        progress=source('feast/GhostEscapeProgress.java')
        self.assertIn('static boolean keyAmbushReady(CompoundTag n)',progress)
        self.assertIn('(phase(n)==Phase.ROOM_REPORT||phase(n)==Phase.READY)',progress)
        self.assertIn('!keyAmbushReady(n)',progress)
        self.assertIn('return GhostEscapeProgress.keyAmbushReady(escape)',display)
        native=ROOT/'src/wybietest/java/com/coraline/feast/GhostVictimProgressGameTests.java'
        self.assertIn('acceptedEscapeChoiceVisibilityMatchesProgressPrerequisites',native.read_text(encoding='utf-8'))

    def test_original_room_door_fallback_does_not_issue_fake_lockpick_receipt(self):
        rooms=source('feast/GhostEscapeRooms.java')
        self.assertIn('static BlockPos liveDoor(VisitData.Scene s,String id,ServerLevel level)',rooms)
        self.assertIn('var door=liveDoor(s,room.id(),level)',rooms)
        self.assertIn('var at=liveDoor(s,id,level)',rooms)
        self.assertIn('var at=liveDoor(s,id,level);if(at==null||child.distanceToSqr',rooms)
        self.assertIn('boolean selectedOpened=false;',rooms)
        self.assertIn('if(!selectedOpened)return false;',rooms)
        self.assertLess(rooms.index('if(!selectedOpened)return false;'),rooms.index('permits.putBoolean(id,true)'))
        covert=source('feast/GhostEscapeCovert.java')
        self.assertIn('GhostEscapeRooms.liveDoor(s,id,level)',covert)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/GhostEscapeRecoveryGameTests.java').read_text(encoding='utf-8')
        self.assertIn('basementCanUseItsSecondOriginalDoorWhenFirstMissing',native)

    def test_chase_spots_are_not_permanently_exhausted(self):
        hiding=source('feast/GhostEscapeHiding.java')
        mother=source('feast/GhostEscapeMother.java')
        self.assertIn('row.putLong("CompromisedAt",marked)',hiding)
        self.assertIn('stillCompromised(now,marked,nearby)',hiding)
        self.assertIn('now-markedAt<1200||motherNearby',hiding)
        self.assertIn('node.putLong("CompromisedAt",clock)',mother)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/GhostEscapeRecoveryGameTests.java').read_text(encoding='utf-8')
        self.assertIn('compromisedHidingSpotsEventuallyBecomeReusable',native)

    def test_complete_music_master_timing_and_one_shot_playback(self):
        receipt=json.loads((ASSETS/'missing_parents_music_audio.json').read_text())
        self.assertEqual(receipt['source_sha256'],hashlib.sha256((ROOT/receipt['source']).read_bytes()).hexdigest())
        ogg=ASSETS/'sounds/scenes/missing_parents_music.ogg'
        self.assertEqual(receipt['ogg_sha256'],hashlib.sha256(ogg.read_bytes()).hexdigest())
        self.assertEqual(receipt['export'],ogg_measure(ogg.read_bytes()));self.assertGreater(receipt['export']['ticks'],100)
        sounds=json.loads((ASSETS/'sounds.json').read_text());self.assertTrue(sounds['missing_parents_music']['sounds'][0]['stream'])
        for table in ('visit_audio_lengths.json','scene_audio_lengths.json'):
            self.assertEqual(json.loads((ASSETS/table).read_text())['missing_parents_music'],receipt['export']['ticks'])
        track=source('client/MissingParentsMusic.java');self.assertIn('looping=false',track);self.assertIn('ShowPlaybackClient.observed',track)
        self.assertIn('return 45*20',source('feast/MissingParentsCat.java'))

    def test_all_rescue_bosses_are_dimension_bound_and_offworld_fights_pause(self):
        bosses=source('feast/RescueBosses.java')
        chapter=source('feast/RescueChapter.java')
        self.assertIn('static boolean inArena(ServerPlayer p,RescueChapter.Phase phase)',bosses)
        self.assertIn('inArena(p,RescueChapter.phase(scene))',bosses)
        self.assertIn('||!inArena(p,RescueChapter.phase(s)))return false',bosses)
        self.assertIn('if(RescueBosses.fighting(state)&&!RescueBosses.inArena(p,state))',chapter)
        self.assertIn('n.putBoolean("Recover",true)',chapter)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/RescueBossGameTests.java').read_text()
        self.assertIn('bossCannotAdvanceAcrossMatchingCoordinatesInWrongDimension',native)

    def test_finale_actor_absence_pauses_voice_without_faking_the_cat_swat(self):
        finale=source('feast/BeldamFinale.java')
        self.assertIn('static boolean finaleCastReady(',finale)
        self.assertIn('state!=FINAL_N||catEscaped||catPresent',finale)
        self.assertIn('ShowActors.recoverCat(p,s,world,at)',finale)
        self.assertIn('ShowActors.storyCat(original)',finale)
        self.assertIn('if(!keepPlayerInRoom(p,s)||!originalFinaleCastReady(p,s,state))return false',finale)
        self.assertIn('n.remove("Audio");n.remove("AudioTail");n.putInt("Ticks",0)',finale)
        self.assertIn('if(done&&n.getBoolean("CatEscaped")&&n.getBoolean("KeyRecovered"))',finale)
        native=(ROOT/'src/wybietest/java/com/coraline/feast/RescueBossGameTests.java').read_text()
        self.assertIn('finaleSwatWaitsForOriginalActorsAndRestartsInterruptedVoice',native)

    def test_wybie_bicycle_chase_handles_a_completely_obstructed_navigation_search(self):
        story=source('feast/FirstEscapeStory.java')
        chase=story.split('private static Vec3 escapeTarget(ServerPlayer p,VisitData.Scene s,CharacterEntity w)',1)[1].split('private static void chase(',1)[0]
        self.assertIn('var at=OtherWorld.safePlayer(',chase)
        self.assertIn('if(at==null)continue;var path=w.getNavigation().createPath(at,0)',chase)
        self.assertIn('return w.position()',chase)

if __name__=='__main__':unittest.main()
