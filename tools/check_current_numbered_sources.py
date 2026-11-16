"""Capped current-working-tree Java/API check; never applies any historical payload.

Reuses the established native memory monitor and exact input-handle guard. Outputs
remain separate from production classes. This is not Gradle/Mixin/native QA or a
release verification. Includes bounded tabletop, decoded-PCM wrapper and pure show
timeline regressions. One child runs at a time; only its handle may be stopped.
"""
from contextlib import ExitStack
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import threading
import time
import types
import uuid
import check_numbered_candidate as monitor

ROOT = Path(__file__).resolve().parents[1]
HELPER_SHA256 = "e4ba5abf54101e0caaf1eaa7533bc3a7a24a83ec9f4f6c36099fbef8176f5c65"
RECEIPTS = [
    "build-character-cast/numbered-apply-backups/20261014T034049Z-e74272fa69664dc481d57ecd164c115c/recovery-manifest.json",
    "build-character-cast/numbered-apply-backups/20261014T163218Z-86d424129a7c4360b47398f7d526d420/recovery-manifest.json",
    "build-character-cast/continuation-backups/20261014T165744Z/recovery-manifest.json",
    "build-character-cast/continuation-backups/20261014T170628Z-opening/recovery-manifest.json",
]

def main():
    if ROOT != monitor.ROOT or sys.platform != "win32":
        raise RuntimeError("Only this existing Windows Coraline working tree is supported")
    helper = ROOT / monitor.HELPER
    monitor.bootstrap_regular(helper)
    raw = helper.read_bytes()
    if hashlib.sha256(raw).hexdigest() != HELPER_SHA256:
        raise RuntimeError("Existing exact-input helper pin changed")
    guard = types.ModuleType("current_numbered_input_guard")
    guard.__file__ = str(helper)
    sys.modules[guard.__name__] = guard
    exec(compile(raw, str(helper), "exec"), guard.__dict__)
    output = ROOT / "build-character-cast/current-source-check" / (time.strftime("%Y%m%dT%H%M%SZ", time.gmtime()) + "-" + uuid.uuid4().hex[:8])
    report = {"scope": "Current declared Java/API source and real-NBT tabletop smoke only", "production_build": False,
              "native_qa": False, "source_application": False, "compiler_caps": monitor.JAVAC_CAPS,
              "start_free_gib": monitor.START_FREE_GIB, "stop_free_gib": monitor.STOP_FREE_GIB}
    if monitor.free_gib() < monitor.START_FREE_GIB:
        raise RuntimeError("Below unchanged 640 MiB free-memory start guard; no child started")
    with ExitStack() as stack:
        guard._mkdir_pinned(ROOT, output, stack, exclusive=True)
        classes, empty = output / "classes", output / "empty-sourcepath"
        for path in (classes, empty):
            guard._mkdir_pinned(ROOT, path, stack)
        held = monitor.HeldInputs(guard, stack)
        held.pin(ROOT, helper, limit=guard.LIMIT_FILE, expected=HELPER_SHA256)
        held.pin(ROOT, Path(__file__), limit=guard.LIMIT_FILE)
        held.pin(ROOT, Path(monitor.__file__), limit=guard.LIMIT_FILE)
        names = set()
        for receipt in RECEIPTS:
            held.pin(ROOT, ROOT / receipt, limit=guard.LIMIT_PAYLOAD)
            record = json.loads(guard._read(ROOT, ROOT / receipt, limit=guard.LIMIT_PAYLOAD))
            for item in record["files"] + record.get("created", []):
                name = item["path"]
                if name.startswith("src/main/java/") and name.endswith(".java"):
                    names.add(name)
        names.update(["src/main/java/com/coraline/feast/TheatreOpeningChoreography.java",
                      "src/wybietest/java/com/coraline/feast/ScriptedShowGameTests.java",
                      "src/wybietest/java/com/coraline/feast/VisitEventsGameTests.java", monitor.SMOKE,
                      "tools/ShowAudioStreamSmoke.java", "tools/ShowTimelineSmoke.java"])
        if not 1 <= len(names) <= 200:
            raise RuntimeError("Explicit source list exceeds established bounded compiler scope")
        for name in sorted(names):
            held.pin(ROOT, ROOT / name, limit=guard.LIMIT_FILE)
        cpfile = ROOT / monitor.CP
        held.pin(ROOT, cpfile, limit=guard.LIMIT_FILE)
        jars = [Path(line) for line in guard._read(ROOT, cpfile).decode("utf-8-sig").splitlines() if line]
        total = 0
        for path in jars:
            if not path.is_absolute() or not path.is_relative_to(monitor.GRADLE_CACHE) or ".." in path.parts or path.suffix.lower() not in (".jar", ".zip"):
                raise RuntimeError("Unexpected cached dependency path")
            total += path.stat().st_size
            if total > monitor.MAX_ALL_DEPENDENCIES:
                raise RuntimeError("Cached dependencies exceed existing bound")
            held.pin(monitor.GRADLE_CACHE, path, limit=monitor.MAX_DEPENDENCY_BYTES)
        compiled = ROOT / "build-character-cast/classes/java/main"
        api = monitor.lock_tree(guard, ROOT, compiled, held)
        report.update(source_files=sorted(names), source_sha256={name: hashlib.sha256(guard._read(ROOT, ROOT / name)).hexdigest() for name in sorted(names)},
                      compiled_api_files=len(api), dependency_bytes=total, free_gib=monitor.free_gib())
        def save():
            (output / "report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        def run(label, command, seconds):
            held.verify("before " + label)
            if monitor.free_gib() < monitor.START_FREE_GIB:
                raise RuntimeError("Unchanged start guard refused " + label)
            log = output / (label + ".log")
            reached, errors = threading.Event(), []
            report[label] = {"command": command, "status": "running", "log": str(log)}
            save()
            with log.open("xb") as stream:
                child = subprocess.Popen(command, cwd=ROOT, env=monitor.child_environment(), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
                reader = threading.Thread(target=monitor.drain_bounded, args=(child.stdout, stream, monitor.MAX_LOG_BYTES, reached, errors), daemon=True)
                reader.start()
                outcome = monitor.watch_child(child, seconds, monitor.free_gib, monitor.process_memory, log_limit=reached)
                reader.join(timeout=10)
                if reader.is_alive():
                    raise RuntimeError("Owned child output did not close")
                child.stdout.close()
            if reached.is_set() or errors:
                outcome.update(status="bounded_log_failure", errors=errors)
            report[label].update(outcome)
            save()
            print(monitor.tail_bytes(log), flush=True)
            held.verify("after " + label)
            if outcome["status"] != "passed" or outcome.get("exit_code") != 0:
                raise RuntimeError(label + " did not pass; no following child started")
        quote = lambda value: '"' + str(value).replace("\\", "/").replace('"', '\\"') + '"'
        compile_cp = os.pathsep.join([str(compiled), *map(str, jars)])
        argfile = output / "javac.args"
        arguments = ["-encoding", "UTF-8", "--release", "17", "-proc:none", "-implicit:none", "-sourcepath", quote(empty), "-classpath", quote(compile_cp), "-d", quote(classes), *[quote(ROOT / name) for name in sorted(names)]]
        argfile.write_text("\n".join(arguments), encoding="utf-8")
        held.pin(ROOT, argfile, limit=guard.LIMIT_FILE)
        save()
        print("Current-source check report: " + str(output / "report.json"), flush=True)
        try:
            run("javac", [str(monitor.JAVA / "javac.exe"), *monitor.JAVAC_CAPS, "@" + str(argfile)], 300)
            fresh_api = monitor.lock_tree(guard, ROOT, classes, held)
            report["fresh_class_files"] = len(fresh_api)
            report["fresh_class_tree_sha256"] = hashlib.sha256(json.dumps(fresh_api, sort_keys=True).encode()).hexdigest()
            save()
            run("tabletop-smoke", [str(monitor.JAVA / "java.exe"), *monitor.SMOKE_CAPS, "-cp", os.pathsep.join([str(classes), compile_cp]), "TabletopRulesSmoke"], 120)
            run("audio-stream-smoke", [str(monitor.JAVA / "java.exe"), *monitor.SMOKE_CAPS, "-cp", os.pathsep.join([str(classes), compile_cp]), "ShowAudioStreamSmoke"], 120)
            run("show-timeline-smoke", [str(monitor.JAVA / "java.exe"), *monitor.SMOKE_CAPS, "-cp", os.pathsep.join([str(classes), compile_cp]), "com.coraline.feast.ShowTimelineSmoke"], 120)
            report["status"] = "capped_source_tabletop_audio_stream_and_show_timeline_checks_passed"
            save()
            return 0
        except Exception as error:
            report.update(status="stopped", error=str(error))
            save()
            print("Stopped: " + str(error), flush=True)
            return 1

if __name__ == "__main__":
    raise SystemExit(main())
