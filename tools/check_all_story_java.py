"""Full production Java compile plus explicitly selected isolated story regression fixtures."""
import argparse,re
import check_missing_parents_cat_compile as compiler
parser=argparse.ArgumentParser();parser.add_argument('--test-classes');parser.add_argument('--rescue-rig',action='store_true');parser.add_argument('--first-escape',action='store_true');args=parser.parse_args()
root=compiler.ROOT/'src/main/java/com/coraline'
compiler.SOURCES=sorted(str(path.relative_to(root)).replace('\\','/') for path in root.rglob('*.java'))
compiler.TESTS=['feast/FirstEscapeCarryGameTests.java','feast/DinnerPartyRepairGameTests.java','feast/StoryBacklogGameTests.java','feast/RescueFoundationsGameTests.java','feast/StoryContinuationGameTests.java','feast/MissingParentsTeaGameTests.java','otherworld/RescueKitchenGameTests.java','feast/RescueCleanupGameTests.java','feast/RescueVisionGameTests.java','feast/RescueRatGameTests.java','feast/RescueKeyMigrationGameTests.java','feast/RescueAuthoredRatRouteGameTests.java','feast/RescueTraversalGameTests.java','feast/RescueBossGameTests.java','feast/RescueGhostContinuityGameTests.java']
compiler.TESTS+=['feast/FamilyGameNightGameTests.java','feast/EventRevisionGameTests.java','feast/RescueFinaleInputGameTests.java','feast/RescueReunionGameTests.java','feast/RescueCatThrowGameTests.java']
compiler.TESTS+=['feast/RescueHearingGameTests.java','feast/RescuePlaybackGameTests.java']
if args.test_classes:
    names=args.test_classes.split(',')
    test_root=compiler.ROOT/'src/wybietest/java/com/coraline'
    selected=[]
    for name in names:
        matches=list(test_root.rglob(name)) if re.fullmatch(r'[A-Za-z0-9_]+GameTests\.java',name) else []
        if len(matches)!=1:raise RuntimeError('Unknown or ambiguous explicit QA class selection: '+name)
        selected.append(str(matches[0].relative_to(test_root)).replace('\\','/'))
    compiler.TESTS=selected
compiler.EXTRA_SOURCES=[str(path.relative_to(compiler.ROOT)).replace('\\','/') for path in (compiler.ROOT/'src/rescuedevelopment/java').rglob('*.java')]+['src/rendercheck/java/com/coraline/qa/LeanWorldVisualCheck.java','src/wybietest/java/com/coraline/feast/PlaybackFixtureClock.java']
if any(path.startswith('tycoon/') for path in compiler.TESTS):compiler.EXTRA_SOURCES+=['src/wybietest/java/com/coraline/tycoon/TycoonNativePlayers.java']
if any(path.startswith('feast/AcceptedButtonEyes') for path in compiler.TESTS):compiler.EXTRA_SOURCES+=['src/wybietest/java/com/coraline/feast/AcceptedBranchPlayers.java']
if args.rescue_rig:compiler.EXTRA_SOURCES+=['src/rendercheck/java/com/coraline/client/RescueRigVisualCheck.java','src/rendercheck/java/com/coraline/client/RescueReunionVisualCheck.java','src/rendercheck/java/com/coraline/client/renderer/FirstEscapeGeometryCheck.java','src/rendercheck/java/com/coraline/client/StoryAudioPlaybackVisualCheck.java']
if args.first_escape:compiler.EXTRA_SOURCES+=['src/rendercheck/java/com/coraline/otherworld/FirstEscapeVisualCheck.java','src/rendercheck/java/com/coraline/otherworld/RescueFinaleVisualCheck.java','src/rendercheck/java/com/coraline/otherworld/RescueFinaleParlor.java','src/rendercheck/java/com/coraline/otherworld/RescueWebVisualCheck.java','src/rendercheck/java/com/coraline/feast/FirstEscapeGrabVisualCheck.java','src/rendercheck/java/com/coraline/otherworld/SeamlessTunnelVisualCheck.java']
if __name__=='__main__':raise SystemExit(compiler.main())
