"""Bounded Coraline source application; the default operation is a read-only dry run.

Production is fixed to PROJECT/PAYLOAD and requires the normal local command
approval plus an exact SHA-256 payload pin. No downloads, builds, release claims,
permission changes, automatic rollback, or writes to held targets are performed.

Existing sources are written through a Windows handle that denies other writers
and deletion, after a fresh raw-byte comparison. Writes are intentionally not a
rename-based transaction: interruption can leave a partial file, with its exact
prior bytes and a write-started record in the recovery directory. Recovery is a
separate, explicitly reviewed action; later user edits are never rolled back.
The portable backend exists only for isolated tests and uses advisory locks.
"""
from contextlib import ExitStack, contextmanager
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
import argparse
import base64
import datetime
import hashlib
import json
import os
import re
import stat
import uuid

PROJECT = Path(r'C:\Users\User1\Documents\Downloads\Coraline')
PAYLOAD = 'tools/numbered_specs_apply_payload.json'
HELD = frozenset({
    'numbered_specs_implementation_plan.md',
    'src/wybietest/java/com/coraline/feast/familydinnerresumegametests.java',
})
SELF = frozenset({'tools/apply_numbered_specs.py', 'tools/check_numbered_candidate.py',
                  'tools/verify_and_apply_numbered_specs.py', PAYLOAD})
SOURCE_TYPES = {
    'src/main/java/com/coraline/': frozenset({'.java'}),
    'src/wybietest/java/com/coraline/': frozenset({'.java'}),
    'src/activitytest/java/com/coraline/': frozenset({'.java'}),
    'src/main/resources/assets/coraline/': frozenset({'.json', '.png'}),
    'tools/': frozenset({'.py', '.ps1', '.md', '.json'}),
    'docs/': frozenset({'.md'}),
}
EXACT = frozenset({'build.gradle', 'src/main/resources/coraline.mixins.json', 'src/main/resources/coraline.party.mixins.json'})
LIMIT_FILES = 250
LIMIT_PAYLOAD = 12 * 1024 * 1024
LIMIT_FILE = 1024 * 1024
HASH = re.compile(r'[0-9a-f]{64}\Z')
RESERVED = {'CON', 'PRN', 'AUX', 'NUL', *(f'COM{i}' for i in range(1, 10)),
            *(f'LPT{i}' for i in range(1, 10))}


@dataclass(frozen=True)
class Entry:
    name: str
    path: Path
    data: bytes
    mode: str
    base: str | None
    candidate: str


def digest(data):
    return hashlib.sha256(data).hexdigest()


def normalized(data, mode):
    if mode == 'utf8-lf':
        text = data.decode('utf-8')
        if '\x00' in text or '\r' in text.replace('\r\n', ''):
            raise ValueError('Text contains NUL or unsupported bare-CR newlines')
        return text.replace('\r\n', '\n').encode('utf-8')
    if mode == 'binary':
        return data
    raise ValueError('Unknown fingerprint encoding')


def valid_hash(value):
    return isinstance(value, str) and HASH.fullmatch(value) is not None


def target_path(root, name):
    if not isinstance(name, str) or not re.fullmatch(r'[A-Za-z0-9_.\-/]+', name):
        raise ValueError('Invalid target spelling')
    path = PurePosixPath(name)
    if (path.is_absolute() or name != path.as_posix() or not path.parts
            or len(name) > 200 or any(len(part) > 120 or part in ('.', '..')
                                     or part.endswith(('.', ' ')) for part in path.parts)):
        raise ValueError('Unsafe target path: ' + name)
    for part in path.parts:
        if part.split('.')[0].upper() in RESERVED:
            raise ValueError('Reserved Windows target: ' + name)
    low = name.lower()
    if path.suffix.lower() == '.java' and path.suffix != '.java':
        raise ValueError('Java source suffix must be canonical .java: ' + name)
    if low in HELD:
        raise ValueError('Explicitly held target remains excluded: ' + name)
    allowed = low in EXACT or any(low.startswith(prefix) and path.suffix.lower() in extensions
                                  for prefix, extensions in SOURCE_TYPES.items())
    if low in SELF or not allowed or any(part.startswith('.') for part in path.parts):
        raise ValueError('Target is outside the bounded source scope: ' + name)
    if low.startswith('src/main/resources/assets/coraline/sounds/'):
        raise ValueError('Audio conversion is a separate validated operation')
    return root.joinpath(*path.parts)


def _absolute(path):
    if '..' in path.parts:
        raise ValueError('Parent traversal is forbidden')
    return path.absolute()


def _is_reparse(info):
    return stat.S_ISLNK(info.st_mode) or bool(getattr(info, 'st_file_attributes', 0) & 0x400)


def no_reparse(root, path, *, kind='file', required=False):
    """Inspect only the bounded ancestor chain, including ancestors above root.

    Do not resolve links first: that would silently authorize their destinations.
    Directory callers must explicitly select kind='directory'.
    """
    root, path = _absolute(root), _absolute(path)
    if not path.is_relative_to(root):
        raise ValueError('Target leaves the project')
    if kind not in {'file', 'directory'}:
        raise ValueError('Invalid path kind')
    for current in (*reversed(path.parents), path):
        try:
            info = current.lstat()
        except FileNotFoundError:
            if current == root or (current == path and required):
                raise ValueError('Required path is missing: ' + str(current))
            continue
        if _is_reparse(info):
            raise ValueError('Reparse/link target requires separate inspection: ' + str(current))
        wants_directory = current != path or kind == 'directory'
        if wants_directory and not stat.S_ISDIR(info.st_mode):
            raise ValueError('Target parent is not a directory: ' + str(current))
        if not wants_directory:
            if not stat.S_ISREG(info.st_mode):
                raise ValueError('Target is not a regular file: ' + str(current))
            if info.st_nlink != 1:
                raise ValueError('Hard-linked targets are excluded: ' + str(current))


def _windows_handle(path, *, write=False, create=False, directory=False):
    """Open the named object itself, excluding reparses and concurrent mutation.

    The no-share-write/no-share-delete handle is held through the source write.
    Directory handles also prevent ancestor rename/reparse changes while held.
    """
    import ctypes
    from ctypes import wintypes
    kernel = ctypes.WinDLL('kernel32', use_last_error=True)
    kernel.CreateFileW.argtypes = [wintypes.LPCWSTR, wintypes.DWORD, wintypes.DWORD,
                                  wintypes.LPVOID, wintypes.DWORD, wintypes.DWORD,
                                  wintypes.HANDLE]
    kernel.CreateFileW.restype = wintypes.HANDLE
    kernel.CloseHandle.argtypes = [wintypes.HANDLE]
    kernel.CloseHandle.restype = wintypes.BOOL
    kernel.GetFileInformationByHandleEx.argtypes = [wintypes.HANDLE, ctypes.c_int,
                                                   wintypes.LPVOID, wintypes.DWORD]
    kernel.GetFileInformationByHandleEx.restype = wintypes.BOOL
    access = 0x80 if directory else 0x80000000 | (0x40000000 if write else 0)
    flags = 0x00200000 | (0x02000000 if directory else 0)  # OPEN_REPARSE_POINT / BACKUP_SEMANTICS
    handle = kernel.CreateFileW(str(path), access, 1, None, 1 if create else 3, flags, None)
    if handle == ctypes.c_void_p(-1).value:
        raise ctypes.WinError(ctypes.get_last_error())
    try:
        class AttributeTag(ctypes.Structure):
            _fields_ = [('attributes', wintypes.DWORD), ('tag', wintypes.DWORD)]
        info = AttributeTag()
        if not kernel.GetFileInformationByHandleEx(handle, 9, ctypes.byref(info), ctypes.sizeof(info)):
            raise ctypes.WinError(ctypes.get_last_error())
        if info.attributes & 0x400 or bool(info.attributes & 0x10) != directory:
            raise ValueError('Opened object is a reparse point or has the wrong type: ' + str(path))
        return kernel, handle
    except BaseException:
        kernel.CloseHandle(handle)
        raise


@contextmanager
def _file(root, path, *, write=False, create=False, limit=LIMIT_FILE):
    no_reparse(root, path, required=not create)
    if os.name == 'nt':
        import msvcrt
        kernel, handle = _windows_handle(path, write=write, create=create)
        try:
            fd = msvcrt.open_osfhandle(handle, os.O_BINARY | (os.O_RDWR if write else os.O_RDONLY))
        except BaseException:
            kernel.CloseHandle(handle)
            raise
    else:
        # Cloud fixture backend only. POSIX locks cannot exclude non-cooperating writers.
        flags = (os.O_RDWR if write else os.O_RDONLY) | getattr(os, 'O_NOFOLLOW', 0)
        if create:
            flags |= os.O_CREAT | os.O_EXCL
        fd = os.open(path, flags, 0o600)
    try:
        if os.name != 'nt':
            import fcntl
            fcntl.flock(fd, (fcntl.LOCK_EX if write else fcntl.LOCK_SH) | fcntl.LOCK_NB)
        info = os.fstat(fd)
        if not stat.S_ISREG(info.st_mode) or info.st_nlink != 1 or info.st_size > limit:
            raise ValueError('Opened file is not a bounded, unaliased regular file: ' + str(path))
        no_reparse(root, path, required=True)
        named = path.lstat()
        if (named.st_dev, named.st_ino) != (info.st_dev, info.st_ino):
            raise RuntimeError('Source identity changed while opening: ' + str(path))
        stream = os.fdopen(fd, 'r+b' if write else 'rb')
        fd = None
        with stream:
            yield stream
    finally:
        if fd is not None:
            os.close(fd)


@contextmanager
def _pin_directories(root, directories):
    """Pin only listed existing ancestor chains; do not scan any directory."""
    paths = set()
    for directory in directories:
        no_reparse(root, directory, kind='directory')
        paths.update((*directory.absolute().parents, directory.absolute()))
    held = []
    try:
        for path in sorted(paths, key=lambda value: (len(value.parts), str(value).lower())):
            if not path.exists():
                continue
            if os.name == 'nt':
                kernel, handle = _windows_handle(path, directory=True)
                held.append((kernel, handle))
        yield
    finally:
        for kernel, handle in reversed(held):
            kernel.CloseHandle(handle)


def _read(root, path, *, limit=LIMIT_FILE):
    with _pin_directories(root, [path.parent]), _file(root, path, limit=limit) as stream:
        before = os.fstat(stream.fileno())
        data = stream.read(limit + 1)
        after = os.fstat(stream.fileno())
        if len(data) > limit:
            raise ValueError('Source exceeds bounded file size: ' + str(path))
        if (before.st_size, before.st_mtime_ns, before.st_ctime_ns) != (after.st_size, after.st_mtime_ns, after.st_ctime_ns):
            raise RuntimeError('Source changed during read: ' + str(path))
        return data


def _mkdir_pinned(root, directory, guards, *, exclusive=False):
    """Create one component at a time and pin it before descending any further."""
    root, directory = _absolute(root), _absolute(directory)
    no_reparse(root, directory, kind='directory')
    current = root
    for part in directory.relative_to(root).parts:
        current = current / part
        try:
            current.mkdir()
        except FileExistsError:
            if exclusive and current == directory:
                raise
        no_reparse(root, current, kind='directory', required=True)
        guards.enter_context(_pin_directories(root, [current]))


def _reject_duplicate_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate JSON key: ' + key)
        result[key] = value
    return result


def validate_payload(root, document):
    if (not isinstance(document, dict) or set(document) != {'format', 'project', 'scope', 'files'}
            or type(document['format']) is not int or document['format'] != 1
            or document['project'] != 'Coraline' or document['scope'] != 'numbered-spec-source-only'):
        raise ValueError('Unexpected payload identity or schema')
    files = document['files']
    if not isinstance(files, list) or not 0 < len(files) <= LIMIT_FILES:
        raise ValueError('Invalid target count')
    result, seen = [], set()
    required = {'path', 'content_base64', 'candidate_sha256', 'base_sha256'}
    for item in files:
        if (not isinstance(item, dict) or not required <= set(item)
                or set(item) - required - {'fingerprint_mode'}):
            raise ValueError('Unexpected target schema')
        name = item['path']
        path = target_path(root, name)
        low = name.lower()
        if low in seen or any(low.startswith(other + '/') or other.startswith(low + '/') for other in seen):
            raise ValueError('Duplicate/case-aliased or file-parent target: ' + name)
        seen.add(low)
        no_reparse(root, path)
        encoded = item['content_base64']
        if not isinstance(encoded, str) or len(encoded) > 4 * ((LIMIT_FILE + 2) // 3):
            raise ValueError('Invalid or oversized encoded candidate: ' + name)
        data = base64.b64decode(encoded, validate=True)
        if len(data) > LIMIT_FILE or base64.b64encode(data).decode('ascii') != encoded:
            raise ValueError('Noncanonical or oversized candidate encoding: ' + name)
        mode = item.get('fingerprint_mode', 'utf8-lf')
        if mode not in {'utf8-lf', 'binary'} or (path.suffix.lower() == '.png') != (mode == 'binary'):
            raise ValueError('Fingerprint mode does not match source type: ' + name)
        if mode == 'binary' and not data.startswith(b'\x89PNG\r\n\x1a\n'):
            raise ValueError('Binary candidate is not a PNG source asset: ' + name)
        candidate = digest(normalized(data, mode))
        if not valid_hash(item['candidate_sha256']) or item['candidate_sha256'] != candidate:
            raise ValueError('Candidate integrity mismatch: ' + name)
        base = item['base_sha256']
        if base is not None and not valid_hash(base):
            raise ValueError('Invalid baseline identity: ' + name)
        result.append(Entry(name, path, data, mode, base, candidate))
    return tuple(result)


def load_payload(root, expected_hash):
    if not valid_hash(expected_hash):
        raise ValueError('A lowercase SHA-256 payload pin is required')
    path = root / PAYLOAD
    raw = _read(root, path, limit=LIMIT_PAYLOAD)
    if digest(raw) != expected_hash:
        raise RuntimeError('Approved payload identity mismatch')
    document = json.loads(raw.decode('utf-8'), object_pairs_hook=_reject_duplicate_keys)
    return validate_payload(root, document)


def inspect_targets(root, entries):
    report = []
    for entry in entries:
        no_reparse(root, entry.path)
        current = _read(root, entry.path) if entry.path.exists() else None
        actual = digest(normalized(current, entry.mode)) if current is not None else None
        if current is not None and actual == entry.candidate:
            state = 'already_present_identical'
        elif actual == entry.base:
            state = 'ready_new' if current is None else 'ready_update'
        else:
            state = 'CONFLICT'
        if state != 'CONFLICT':
            # Validate the final encoded size in the all-target read-only preflight.
            _output_bytes(entry, current)
        report.append({'path': entry.name, 'state': state, 'expected_base_sha256': entry.base,
                       'current_sha256': actual, 'current_raw_sha256': None if current is None else digest(current),
                       'candidate_sha256': entry.candidate})
    return report


def _output_bytes(entry, current):
    if entry.mode == 'binary':
        if len(entry.data) > LIMIT_FILE:
            raise ValueError('Formatted output exceeds bounded file size: ' + entry.name)
        return entry.data
    output = normalized(entry.data, entry.mode)
    if current is not None and b'\r\n' in current:
        if b'\n' in current.replace(b'\r\n', b''):
            raise ValueError('Mixed source newlines require explicit review: ' + entry.name)
        output = output.replace(b'\n', b'\r\n')
    if len(output) > LIMIT_FILE:
        raise ValueError('Formatted output exceeds bounded file size: ' + entry.name)
    return output


def _write_all(stream, data):
    stream.seek(0)
    written = stream.write(data)
    if written != len(data):
        raise OSError('Incomplete source write')
    stream.truncate()
    stream.flush()
    os.fsync(stream.fileno())


def apply(root, entries, preflight, payload_hash):
    if not valid_hash(payload_hash):
        raise ValueError('Invalid payload pin')
    # Never trust a supplied preflight; perform a complete fresh scan before any write.
    fresh = inspect_targets(root, entries)
    if fresh != preflight or any(row['state'] == 'CONFLICT' for row in fresh):
        raise RuntimeError('All-target dry run changed or found conflicts; no files changed')
    if not any(row['state'] != 'already_present_identical' for row in fresh):
        return {'status': 'already_present_source_only', 'applied': 0, 'already_identical': len(fresh),
                'recovery_manifest': None, 'compile_or_release_verified': False}

    destination = root / 'build-character-cast' / 'numbered-apply-backups'
    no_reparse(root, destination, kind='directory')
    records = []
    # Determine formatting for all files before making a backup/report/source directory.
    outputs = []
    for entry, row in zip(entries, fresh):
        current = _read(root, entry.path) if row['current_raw_sha256'] is not None else None
        if (None if current is None else digest(current)) != row['current_raw_sha256']:
            raise RuntimeError('Source changed during output preparation; no files changed: ' + entry.name)
        outputs.append(current if row['state'] == 'already_present_identical' else _output_bytes(entry, current))
        records.append(dict(row, fingerprint_mode=entry.mode, applied=False, write_started=False,
                            creation_intent=False, creation_status='not_requested',
                            output_raw_sha256=digest(outputs[-1])))

    with ExitStack() as guards:
        guards.enter_context(_pin_directories(root, [entry.path.parent for entry in entries] + [destination]))
        if inspect_targets(root, entries) != fresh:
            raise RuntimeError('Source changed before backup creation; no files changed')
        run = destination / (datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + uuid.uuid4().hex)
        _mkdir_pinned(root, run, guards, exclusive=True)
        manifest = run / 'recovery-manifest.json'
        audit = {'payload_sha256': payload_hash, 'status': 'preparing_backups', 'files': records,
                 'compile_or_release_verified': False,
                 'policy': 'No automatic rollback. Exact backups are retained; interrupted writes may be partial. Creation intent records an attempted exclusive open, not proof of creation or a content write. Inspect every creation-intent target even when write_started is false. Review current bytes before any manual recovery.'}

        def save_audit():
            # This run is exclusive and never reused; only its private manifest is replaced.
            no_reparse(root, run, kind='directory', required=True)
            temp = run / ('manifest-' + uuid.uuid4().hex + '.tmp')
            data = (json.dumps(audit, indent=2) + '\n').encode('utf-8')
            with _file(root, temp, write=True, create=True, limit=LIMIT_PAYLOAD) as stream:
                _write_all(stream, data)
            no_reparse(root, manifest)
            os.replace(temp, manifest)

        backup_handles = []

        def verify_backups():
            for backup, stream, expected in backup_handles:
                no_reparse(root, backup, required=True)
                opened = os.fstat(stream.fileno()); named = backup.lstat()
                if (opened.st_dev, opened.st_ino) != (named.st_dev, named.st_ino):
                    raise RuntimeError('Backup identity changed: ' + str(backup))
                stream.seek(0)
                raw = stream.read(LIMIT_FILE + 1)
                if len(raw) > LIMIT_FILE or digest(raw) != expected:
                    raise RuntimeError('Backup changed while held: ' + str(backup))

        try:
            save_audit()
            # Exact raw backups for every existing changed file precede every source write.
            for index, (entry, row) in enumerate(zip(entries, fresh)):
                if row['state'] != 'ready_update':
                    continue
                old = _read(root, entry.path)
                if digest(old) != row['current_raw_sha256']:
                    raise RuntimeError('Source changed before backup: ' + entry.name)
                backup = run / (f'before-{index:04d}.bin')
                with _file(root, backup, write=True, create=True) as stream:
                    _write_all(stream, old)
                # The readonly handle denies write/delete on Windows until apply exits.
                # Compare after acquiring it, closing the create-to-read-lock race.
                locked_backup = guards.enter_context(_file(root, backup))
                if locked_backup.read(LIMIT_FILE + 1) != old:
                    raise RuntimeError('Backup readback mismatch: ' + entry.name)
                backup_handles.append((backup, locked_backup, digest(old)))
                records[index]['backup'] = backup.name
                records[index]['backup_raw_sha256'] = digest(old)
                save_audit()
            verify_backups()
            if inspect_targets(root, entries) != fresh:
                raise RuntimeError('Source changed after backups; no source writes attempted')
            audit['status'] = 'applying'
            save_audit()
            for index, entry in enumerate(entries):
                verify_backups()
                record, output = records[index], outputs[index]
                if record['state'] == 'already_present_identical':
                    if digest(_read(root, entry.path)) != record['current_raw_sha256']:
                        raise RuntimeError('Previously identical source changed: ' + entry.name)
                    continue
                no_reparse(root, entry.path)
                with ExitStack() as source_guards:
                    _mkdir_pinned(root, entry.path.parent, source_guards)
                    no_reparse(root, entry.path)
                    # CREATE_NEW never overwrites a file that appeared after the dry run.
                    create = record['state'] == 'ready_new'
                    if create:
                        # CREATE_NEW can mutate the filesystem before _file yields.
                        # Persist the attempted creation, including uncertain failures.
                        record['creation_intent'] = True
                        record['creation_status'] = 'attempting_exclusive_create'
                        save_audit()
                    with _file(root, entry.path, write=True, create=create) as stream:
                        current = None if create else stream.read(LIMIT_FILE + 1)
                        if (None if current is None else digest(current)) != record['current_raw_sha256']:
                            raise RuntimeError('Source changed immediately before locked write: ' + entry.name)
                        if create:
                            record['creation_status'] = 'created_verified'
                        record['write_started'] = True
                        save_audit()
                        _write_all(stream, output)
                        stream.seek(0)
                        actual = stream.read(LIMIT_FILE + 1)
                        if actual != output:
                            raise RuntimeError('Applied readback mismatch: ' + entry.name)
                        verify_backups()
                        record['applied'] = True
                        record['applied_raw_sha256'] = digest(actual)
                        save_audit()
            # Recheck all targets, including those skipped as already identical.
            for entry, record in zip(entries, records):
                expected = record.get('applied_raw_sha256', record['current_raw_sha256'])
                if digest(_read(root, entry.path)) != expected:
                    raise RuntimeError('Source changed before final readback: ' + entry.name)
            verify_backups()
            audit['status'] = 'applied_source_only'
            save_audit()
        except BaseException as error:
            for record in records:
                if record['creation_status'] == 'attempting_exclusive_create':
                    record['creation_status'] = 'failed_or_uncertain_before_write'
            audit['status'] = 'stopped_without_rollback'
            audit['error'] = f'{type(error).__name__}: {error}'
            try:
                save_audit()
            except BaseException as audit_error:
                # Do not hide the original source failure. Earlier write-started records remain.
                raise RuntimeError(f'Apply stopped; recovery audit could not be updated: {manifest}; '
                                   f'audit error: {audit_error}; original error: {error}') from error
            raise
    return {'status': audit['status'], 'applied': sum(row['applied'] for row in records),
            'already_identical': sum(row['state'] == 'already_present_identical' for row in records),
            'recovery_manifest': str(manifest), 'compile_or_release_verified': False}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--payload-sha256', required=True)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args(argv)
    if os.name != 'nt':
        raise RuntimeError('Production entry point is Windows-only; tests use isolated helpers')
    entries = load_payload(PROJECT, args.payload_sha256)
    report = inspect_targets(PROJECT, entries)
    print(json.dumps({'mode': 'all-target dry run', 'files': report,
                      'compile_or_release_verified': False}, indent=2), flush=True)
    if any(row['state'] == 'CONFLICT' for row in report):
        raise RuntimeError('Conflicts found; no writes attempted')
    if args.apply:
        print(json.dumps(apply(PROJECT, entries, report, args.payload_sha256), indent=2), flush=True)
    else:
        print('Read-only dry run complete; no files changed.', flush=True)


if __name__ == '__main__':
    main()
