"""Losslessly recompress an already optimized, Forge-mapped release archive."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import zipfile
import check_numbered_candidate as monitor
from verify_packaged_mixins import verify
from verify_release_jar_size import audit


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--sha256', required=True)
    args = parser.parse_args()
    source = args.jar.resolve(strict=True)
    if source.parent != monitor.ROOT / 'build-character-cast/libs' or sha(source) != args.sha256:
        raise RuntimeError('Unexpected release input')
    if monitor.free_gib() < monitor.START_FREE_GIB:
        raise RuntimeError('Below unchanged start floor')
    verify(source)
    pending = source.with_suffix('.pending.jar')
    report_path = monitor.ROOT / 'build-character-cast/reports/lossless-release-compression.json'
    before = audit(source)
    try:
        with zipfile.ZipFile(source) as old, zipfile.ZipFile(pending, 'x', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as new:
            for entry in old.infolist():
                if monitor.free_gib() < monitor.STOP_FREE_GIB:
                    raise RuntimeError('Emergency free-memory floor reached')
                # Preserve entry metadata; replace only the lossless encoding.
                entry.compress_type = zipfile.ZIP_DEFLATED
                entry._compresslevel = 9
                with old.open(entry.filename) as incoming, new.open(entry, 'w') as outgoing:
                    while block := incoming.read(1024 * 1024):
                        if monitor.free_gib() < monitor.STOP_FREE_GIB:
                            raise RuntimeError('Emergency free-memory floor reached')
                        outgoing.write(block)
        with zipfile.ZipFile(source) as old, zipfile.ZipFile(pending) as new:
            if new.testzip() is not None or old.namelist() != new.namelist():
                raise RuntimeError('Archive integrity/membership changed')
            for name in old.namelist():
                with old.open(name) as a, new.open(name) as b:
                    if hashlib.file_digest(a, 'sha256').digest() != hashlib.file_digest(b, 'sha256').digest():
                        raise RuntimeError('Entry bytes changed: ' + name)
        verify(pending)
        after = audit(pending)
        if after['bytes'] >= before['bytes'] or sha(source) != args.sha256:
            raise RuntimeError('No size improvement or release input changed')
        backup = report_path.parent / (source.stem + '-before-lossless-compression.jar')
        if backup.exists():
            raise RuntimeError('Refusing to replace preserved input')
        shutil.copyfile(source, backup)
        pending.replace(source)
        after = audit(source)
        report_path.write_text(json.dumps({'before': before, 'after': after, 'all_entry_bytes_identical': True,
            'emergency_free_mib': 384}, indent=2) + '\n', encoding='utf-8')
        (report_path.parent / 'release-jar-size.json').write_text(json.dumps(after, indent=2) + '\n', encoding='utf-8')
        print(json.dumps({key: after[key] for key in ('jar', 'bytes', 'sha256', 'size_status')}))
        return 0 if after['bytes'] < 100_000_000 else 1
    finally:
        pending.unlink(missing_ok=True)


if __name__ == '__main__':
    raise SystemExit(main())
