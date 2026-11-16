"""Compile staged continuation sources without changing accepted release outputs.

This is API compilation only, never native/gameplay acceptance or activation.
The normal focused compiler retains its owned-job and emergency-memory guards.
"""
import check_missing_parents_cat_compile as compiler

compiler.SOURCES=[]
compiler.TESTS=[]
compiler.HEAP_MIB=192
compiler.EXTRA_SOURCES=sorted(str(p.relative_to(compiler.ROOT)).replace('\\','/')
    for folder in ('src/next-victim/java','src/next-victim-test/java')
    for p in (compiler.ROOT/folder).rglob('*.java'))

if __name__=='__main__':
    raise SystemExit(compiler.main())
