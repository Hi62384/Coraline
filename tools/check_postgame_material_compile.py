"""Small cached-API compile for changed postgame material adapters/fixtures.

Not a full current-source compile and cannot admit native or release testing.
The unchanged source-compile memory floor/cutoff and owned job guard apply.
"""
import check_missing_parents_cat_compile as compiler

compiler.HEAP_MIB = 192
compiler.SOURCES = ['CoralineMod.java', 'registry/ModBlocks.java', 'registry/ModItems.java'] + sorted(
    p.relative_to(compiler.ROOT / 'src/main/java/com/coraline').as_posix()
    for p in (compiler.ROOT / 'src/main/java/com/coraline/postgame').glob('*.java'))
compiler.TESTS = ['postgame/InfiniteChunkTerrainGameTests.java',
                  'postgame/InfiniteRareOreGameTests.java', 'postgame/InfiniteContentGameTests.java',
                  'postgame/InfiniteVegetationGameTests.java']
compiler.EXTRA_SOURCES = []

if __name__ == '__main__':
    raise SystemExit(compiler.main())
