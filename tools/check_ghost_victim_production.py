"""Full current production API compile plus continuation fixtures, under the existing memory guard.
Normal Gradle/Mixin and actual game acceptance remain separate checks.
"""
import check_missing_parents_cat_compile as compiler
compiler.SOURCES=sorted(str(p.relative_to(compiler.ROOT/'src/main/java/com/coraline')).replace('\\','/') for p in (compiler.ROOT/'src/main/java/com/coraline').rglob('*.java'))
compiler.TESTS=['feast/GhostVictimProgressGameTests.java','feast/AcceptedButtonEyesProgressGameTests.java','feast/GhostVictimNavigationGameTests.java','feast/AcceptedButtonEyesInventoryGameTests.java']
compiler.EXTRA_SOURCES=['src/rendercheck/java/com/coraline/qa/LeanWorldVisualCheck.java','src/rendercheck/java/com/coraline/qa/PalaceBackgroundVisualCheck.java','src/wybietest/java/com/coraline/feast/AcceptedBranchPlayers.java','src/wybietest/java/com/coraline/feast/GhostEscapeStateCheck.java']
compiler.HEAP_MIB=192
if __name__=='__main__':raise SystemExit(compiler.main())
