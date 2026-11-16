"""Compile/run only the production terrain/ore field and its standalone checks.

Uses the existing Java RAM floors, job and output bounds. No Minecraft,
Gradle, registered-block, full-source or release acceptance is implied.
"""
from pathlib import Path
import ctypes
import hashlib
import json
import os
import subprocess
import time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job

ROOT = Path(__file__).resolve().parents[1]


def main(*, planner=False, component='ore'):
    if component not in ('ore', 'rapier', 'structures', 'locks') or planner and component != 'ore':
        raise ValueError('Invalid standalone component')
    rapier = component == 'rapier'
    structures = component == 'structures'
    locks = component == 'locks'
    names = ['RapierCadence.java'] if rapier else ['SiteLockRules.java'] if locks else ['InfiniteTerrain.java', 'InfiniteStructurePlan.java'] if structures else ['InfiniteTerrain.java', 'InfiniteVegetationPlan.java'] if planner else ['InfiniteTerrain.java', 'InfiniteOreVeins.java']
    check_name = 'RapierCadenceCheck' if rapier else 'SiteLockRulesCheck' if locks else 'InfiniteStructurePlanCheck' if structures else 'InfiniteVegetationPlanCheck' if planner else 'InfiniteOreDistributionCheck'
    marker = 'RAPIER_CADENCE_OK checks=' if rapier else 'SITE_LOCK_RULES_OK checks=' if locks else 'INFINITE_STRUCTURE_PLAN_OK checks=' if structures else 'INFINITE_VEGETATION_PLAN_OK checks=' if planner else 'INFINITE_ORE_DISTRIBUTION_OK checks='
    package = 'clothing' if rapier else 'postgame'
    sources = [ROOT / ('src/main/java/com/coraline/' + package) / name for name in
               names]
    sources.append(ROOT / ('tools/' + check_name + '.java'))
    hashes = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources}
    folder = ROOT / ('build-character-cast/rapier-cadence' if rapier else 'build-character-cast/site-lock-rules' if locks else 'build-character-cast/infinite-structure-plan' if structures else 'build-character-cast/infinite-vegetation-plan' if planner else 'build-character-cast/infinite-ore-distribution') / time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    folder.mkdir(parents=True, exist_ok=False)
    classes = folder / 'classes'
    classes.mkdir()
    report = {'scope': 'Production Java component checks only; not full compile, registered content, native gameplay, visual or release acceptance',
              'source_sha256': hashes, 'start_free_gib': monitor.free_gib()}
    status = 1
    try:
        commands = [
            [str(monitor.JAVA / 'javac.exe'), '-J-Xmx128m', '-J-XX:+UseSerialGC', '-J-XX:ActiveProcessorCount=2',
             '-J-XX:MaxMetaspaceSize=96m', '-J-XX:ReservedCodeCacheSize=32m', '-encoding', 'UTF-8', '--release', '17',
             '-proc:none', '-d', str(classes), *map(str, sources)],
            [str(monitor.JAVA / 'java.exe'), '-Xmx96m', '-XX:+UseSerialGC', '-XX:ActiveProcessorCount=2',
             '-XX:MaxMetaspaceSize=96m', '-XX:ReservedCodeCacheSize=32m', '-cp', str(classes),
             'com.coraline.' + package + '.' + check_name]]
        for stage, command in zip(['compile', 'distribution'], commands):
            if monitor.free_gib() < monitor.START_FREE_GIB:
                report['status'] = 'below_unchanged_start_floor'
                return 1
            api, handle = job(384)
            try:
                log = folder / (stage + '.log')
                with log.open('xb') as stream:
                    child = subprocess.Popen(command, cwd=ROOT, env=monitor.child_environment(), stdout=stream, stderr=subprocess.STDOUT)
                    if not api.AssignProcessToJobObject(handle, wintypes.HANDLE(int(child._handle))):
                        child.kill(); child.wait(); raise ctypes.WinError(ctypes.get_last_error())
                    result = monitor.watch_child(child, 120, monitor.free_gib, monitor.process_memory)
                report[stage] = result
                if log.stat().st_size > 1024 * 1024:
                    report['status'] = 'unexpected_output_size'
                    return 1
                output = log.read_text(encoding='utf-8', errors='replace')
                print(output)
                if result.get('status') != 'passed' or result.get('exit_code') != 0:
                    report['status'] = result.get('status', 'failed')
                    return 1
                if stage == 'distribution' and marker not in output:
                    report['status'] = 'missing_assertion_marker'
                    return 1
            finally:
                api.CloseHandle(handle)
        if any(hashlib.sha256((ROOT / p).read_bytes()).hexdigest() != digest for p, digest in hashes.items()):
            report['status'] = 'source_changed_during_check'
            return 1
        report['status'] = 'passed'
        status = 0
    finally:
        (folder / 'report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
        print(('Rapier cadence' if rapier else 'Site lock rules' if locks else 'Structure planner' if structures else 'Vegetation planner' if planner else 'Ore distribution') + ' report: ' + str(folder / 'report.json'))
    return status


if __name__ == '__main__':
    raise SystemExit(main())
