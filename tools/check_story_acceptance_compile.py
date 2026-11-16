"""Full-source isolated compilation for serial disposable story QA.

Not a replacement for the normal production/Mixin/reobfuscation build. Includes
all actual story GameTests and existing visual fixtures, with source hashes for
the guarded native runner; no cached production classes are used as APIs.
"""
import check_missing_parents_cat_compile as compiler

compiler.HEAP_MIB=256
compiler.SOURCES=sorted(p.relative_to(compiler.ROOT/'src/main/java/com/coraline').as_posix()
                        for p in (compiler.ROOT/'src/main/java/com/coraline').rglob('*.java'))
compiler.TESTS=sorted(p.relative_to(compiler.ROOT/'src/wybietest/java/com/coraline').as_posix()
                      for p in (compiler.ROOT/'src/wybietest/java/com/coraline').rglob('*.java'))
compiler.EXTRA_SOURCES=sorted(p.relative_to(compiler.ROOT).as_posix()
                              for p in (compiler.ROOT/'src/rendercheck/java').rglob('*.java')
                              if p.name!='DollRenderCheck.java')
compiler.EXTRA_SOURCES.append('src/next-victim-test/java/com/coraline/feast/GhostVictimStateCheck.java')
compiler.EXTRA_SOURCES.append('src/visittest/java/com/coraline/feast/VisitGiftGameTests.java')

if __name__=='__main__':raise SystemExit(compiler.main())
