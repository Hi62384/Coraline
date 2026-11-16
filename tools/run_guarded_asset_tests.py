"""Small optimizer regression suite under the existing memory/job safeguards.

This is not Minecraft, gameplay, rendered-visual or audible-playback acceptance.
"""
import json
from pathlib import Path
import subprocess
import sys
import threading
import time
from ctypes import wintypes
import check_numbered_candidate as monitor
from run_low_memory_release import job


def main():
    root = Path(__file__).resolve().parents[1]
    free = monitor.free_gib()
    if free < monitor.START_FREE_GIB:
        raise RuntimeError('Optimizer tests require the unchanged 640 MiB admission floor')
    output = root / 'build-character-cast/asset-regression' / time.strftime('%Y%m%dT%H%M%SZ', time.gmtime())
    output.mkdir(parents=True, exist_ok=False)
    log = output / 'output.log'
    environment = monitor.child_environment()
    environment.update(PYTHONUTF8='1', PYTHONIOENCODING='utf-8')
    environment.update(OMP_NUM_THREADS='1', OPENBLAS_NUM_THREADS='1', MKL_NUM_THREADS='1')
    environment['PATH'] += ';C:\\ffmpeg'
    api, handle = job(960)
    report = {'scope': 'optimizer regressions only; not gameplay or perceptual QA',
              'start_free_gib': free, 'aggregate_job_limit_mib': 960, 'stop_free_mib': 384}
    try:
        with log.open('xb') as stream:
            child = subprocess.Popen([sys.executable, 'tools/test_optimize_runtime_assets.py'],
                cwd=root, env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, bufsize=0)
            if not api.AssignProcessToJobObject(handle, wintypes.HANDLE(int(child._handle))):
                child.kill()
                child.wait()
                raise RuntimeError('Could not attach optimizer tests to bounded job')
            limit, errors = threading.Event(), []
            reader = threading.Thread(target=monitor.drain_bounded,
                args=(child.stdout, stream, monitor.MAX_LOG_BYTES, limit, errors), daemon=True)
            reader.start()
            report.update(monitor.watch_child(child, 300, monitor.free_gib, monitor.process_memory, log_limit=limit))
            api.CloseHandle(handle)
            handle = None
            reader.join(timeout=10)
            if reader.is_alive() or errors or limit.is_set():
                report.update(status='output_capture_failed', errors=errors)
    finally:
        if handle:
            api.CloseHandle(handle)
        (output / 'report.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(log.read_text(encoding='utf-8', errors='replace'))
    print('Report: ' + str(output / 'report.json'))
    return 0 if report.get('status') == 'passed' and report.get('exit_code') == 0 else 1


if __name__ == '__main__':
    raise SystemExit(main())
