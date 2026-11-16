"""Run the exported production javac specification without retaining Gradle.

This is a compiler checkpoint, not a release. Annotation processing is enabled;
publication still requires normal optimized resources, checks and Forge remapping.
"""
from pathlib import Path
import argparse
import ctypes
import hashlib
import json
import os
import subprocess
import threading
import time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import compact_memory_probe, job, ready_memory
from stream_srg_mappings import convert

ROOT = monitor.ROOT


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def files_at(directory):
    return {str(p.relative_to(directory)): sha(p) for p in sorted(directory.rglob('*')) if p.is_file()}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--graph', required=True)
    args = parser.parse_args()
    graph_path = Path(args.graph).resolve(strict=True)
    if not graph_path.is_relative_to(ROOT / 'build-character-cast'):
        raise RuntimeError('Graph must be in the owned build directory')
    graph = json.loads(graph_path.read_text(encoding='utf-8'))
    spec = graph['compile']
    sources = [Path(p) for p in spec['sources']]
    expected = set((ROOT / 'src/main/java').rglob('*.java'))
    if set(sources) != expected or not expected:
        raise RuntimeError('Export must contain every production source and no QA sources')
    cache = Path(r'C:/Users/User1/.gradle/caches')
    dependencies = [Path(p) for p in spec['classpath'] + spec['processors']]
    if not spec['processors'] or any(not p.resolve(strict=True).is_relative_to(cache) for p in dependencies):
        raise RuntimeError('Processor/dependency list must use existing cached libraries')
    if spec['sourceCompatibility'] != '17' or spec['targetCompatibility'] != '17':
        raise RuntimeError('Unexpected Java version')
    mapping_source = ROOT / 'build-character-cast/createMcpToSrg/output.tsrg'
    mapping = ROOT / 'build-character-cast/generatedMappings/clothing.srg'
    convert(mapping_source, mapping)
    ready_memory(640, 45)
    run = ROOT / 'build-character-cast/serial-compile' / time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    run.mkdir(parents=True)
    classes = run / 'classes'
    classes.mkdir()
    generated = run / 'generated'
    generated.mkdir()
    empty = run / 'empty-sourcepath'
    empty.mkdir()
    flags = []
    expected_options = {'-AdefaultObfuscationEnv=searge', '-AreobfSrgFile=' + str(mapping)}
    for value in spec['arguments']:
        if value.startswith('-AoutRefMapFile='):
            flags.append('-AoutRefMapFile=' + str(generated / 'coraline.refmap.json'))
        elif value.startswith('-AoutSrgFile='):
            flags.append('-AoutSrgFile=' + str(generated / 'mixin-shadow.srg'))
        elif value in expected_options:
            flags.append(value)
        else:
            raise RuntimeError('Unreviewed compiler option: ' + value)
    if len(flags) != 4:
        raise RuntimeError('Incomplete production annotation-processor options')
    quote = lambda value: '"' + str(value).replace('\\', '/') + '"'
    arguments = ['-encoding', 'UTF-8', '-source', '17', '-target', '17', '-implicit:none',
                 '-sourcepath', quote(empty), '-classpath', quote(os.pathsep.join(spec['classpath'])),
                 '-processorpath', quote(os.pathsep.join(spec['processors'])),
                 '-d', quote(classes), *map(quote, flags), *map(quote, sources)]
    argfile = run / 'javac.args'
    argfile.write_text('\n'.join(arguments) + '\n', encoding='utf-8')
    inputs = {str(p): sha(p) for p in [*sources, *dependencies, mapping_source, mapping, ROOT / 'build.gradle', ROOT / 'gradle.properties']}
    report = {'status': 'running', 'scope': 'Complete production javac with Mixin annotation processing; not gameplay or release acceptance',
              'graph': graph, 'inputs': inputs, 'heap_mib': 256, 'emergency_free_mib': 384}
    api, handle = job(768)
    print('Production compiler checkpoint: ' + str(run / 'report.json'), flush=True)
    try:
        with (run / 'output.log').open('xb', buffering=0) as log:
            command = [str(monitor.JAVA / 'javac.exe'), '-J-Xms16m', '-J-Xmx256m', '-J-XX:+UseSerialGC',
                       '-J-XX:ActiveProcessorCount=2', '-J-XX:MaxMetaspaceSize=128m', '-J-XX:ReservedCodeCacheSize=32m', '@' + str(argfile)]
            child = subprocess.Popen(command, cwd=ROOT, env=monitor.child_environment(), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle, wintypes.HANDLE(int(child._handle))):
                child.kill()
                child.wait()
                raise ctypes.WinError(ctypes.get_last_error())
            reached, errors = threading.Event(), []
            reader = threading.Thread(target=monitor.drain_bounded, args=(child.stdout, log, monitor.MAX_LOG_BYTES, reached, errors), daemon=True)
            reader.start()
            report.update(monitor.watch_child(child, 600, compact_memory_probe(child, report), monitor.process_memory, log_limit=reached))
            reader.join(timeout=10)
            if errors or reached.is_set() or reader.is_alive():
                report['status'] = 'output_capture_failed'
        if report.get('status') == 'passed':
            if any(sha(Path(p)) != h for p, h in inputs.items()):
                raise RuntimeError('Compiler inputs changed during compilation')
            if not (generated / 'coraline.refmap.json').is_file() or not (generated / 'mixin-shadow.srg').is_file():
                raise RuntimeError('Annotation processor outputs missing')
            report.update(classes=files_at(classes), generated=files_at(generated))
    except BaseException as error:
        report.update(status='failed', error=str(error))
        raise
    finally:
        api.CloseHandle(handle)
        (run / 'report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(monitor.tail_bytes(run / 'output.log'), flush=True)
    return 0 if report.get('status') == 'passed' and report.get('exit_code') == 0 else 1


if __name__ == '__main__':
    raise SystemExit(main())
