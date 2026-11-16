"""Capped full-source compile for the disposable finale/mixer checks; not release acceptance."""
import check_missing_parents_cat_compile as compiler
compiler.HEAP_MIB=192

compiler.SOURCES=sorted(str(path.relative_to(compiler.ROOT/'src/main/java/com/coraline')).replace('\\','/') for path in (compiler.ROOT/'src/main/java/com/coraline').rglob('*.java'))
compiler.TESTS=['feast/RescueFarewellGateGameTests.java','feast/FamilyGameNightGameTests.java']
compiler.EXTRA_SOURCES=[
    'src/rendercheck/java/com/coraline/qa/LeanWorldVisualCheck.java',
    'src/rendercheck/java/com/coraline/client/RescueRigVisualCheck.java',
    'src/rendercheck/java/com/coraline/client/RescueReunionVisualCheck.java',
    'src/rendercheck/java/com/coraline/client/StoryAudioPlaybackVisualCheck.java',
    'src/rendercheck/java/com/coraline/client/renderer/FirstEscapeGeometryCheck.java',
    'src/rendercheck/java/com/coraline/otherworld/FirstEscapeVisualCheck.java',
    'src/rendercheck/java/com/coraline/otherworld/RescueFinaleVisualCheck.java',
    'src/rendercheck/java/com/coraline/otherworld/RescueFinaleParlor.java',
    'src/rendercheck/java/com/coraline/otherworld/RescueWebVisualCheck.java',
    'src/rendercheck/java/com/coraline/feast/FamilyGameNightNaturalVisualCheck.java',
]
if __name__=='__main__':raise SystemExit(compiler.main())
