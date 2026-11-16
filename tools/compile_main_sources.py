"""Compile actual production sources against cached Minecraft/Forge APIs.

No verification fixtures, native game, mixin processing, assets or release
packaging. Reuses the existing 640/384 MiB admission/cutoff and job memory cap.
Every main source is explicit; stale compiled project classes are not APIs.
"""
import check_missing_parents_cat_compile as compiler

compiler.HEAP_MIB=256
compiler.SOURCES=sorted(p.relative_to(compiler.ROOT/'src/main/java/com/coraline').as_posix()
                        for p in (compiler.ROOT/'src/main/java/com/coraline').rglob('*.java'))
compiler.TESTS=[]
compiler.EXTRA_SOURCES=[]

if __name__=='__main__':
    raise SystemExit(compiler.main())
