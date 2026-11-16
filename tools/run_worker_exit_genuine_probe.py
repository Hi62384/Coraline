"""One genuine new take after the changed shutdown profile's PCM comparison.
Preserves the failed worker, original completion deadline and all master files.
"""
from datetime import datetime
import json
from pathlib import Path
import subprocess
import sys
import time
from run_early_reference_genuine_probe import admit,measured_floor
from run_parent_audio_chunks import wait_ready
from voice_batch_scope import generation_state
from proven_voice_worker_exit import verify

ROOT=Path(__file__).resolve().parents[1]
COMPONENT=ROOT/'build-character-cast/compact-voice/20261029T014715Z/report.json'


def completed_exit(report):
    log=(report.parent/'output.log').read_text(encoding='utf-8',errors='replace')
    markers=[line.split(': ',1)[1] for line in log.splitlines() if line.startswith('Verified owned speech completion exit: ')]
    if len(markers)!=1:raise ValueError('One actual worker completion receipt required')
    return verify(ROOT,Path(markers[0]))


def main():
    path,component=admit(COMPONENT);completed_exit(path);floor=measured_floor(component)
    manifest=ROOT/'tools/incidental_mother_voice_manifest.json'
    if generation_state(ROOT,json.loads(manifest.read_text(encoding='utf-8')))['completed']!=38:raise ValueError('Expected preserved 38 saved Mother takes')
    original=json.loads((ROOT/'build-character-cast/verified-staged-audio/20261028T213449Z/report.json').read_text())
    # Match the already committed six-hour completion window; never reset it.
    if original.get('completion_window_seconds')!=21600:raise ValueError('Original finite completion window changed')
    end=datetime.fromisoformat(original['completion_window_started_utc'].replace('Z','+00:00')).timestamp()+21600
    remaining=min(3600,end-time.time()-1860)
    if remaining<=0:raise TimeoutError('Original generation admission window expired')
    print('One genuine take after successful original PCM and durable-exit comparison; floor '+str(floor)+' GiB; no automatic retries.',flush=True)
    wait_ready(time.monotonic()+remaining,minimum_gib=floor)
    admit(path);completed_exit(path)
    command=[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue',
        '--memory-efficient','--low-memory-attempt','--require-reference-cache','--stage-flow-model',
        '--stage-vocoder','--cuda-async-allocator','--early-reference-retirement','--early-reference-admission',str(path),
        '--max-new-takes','1','--timeout-seconds','1800']
    return subprocess.run(command,cwd=ROOT,timeout=1860).returncode


if __name__=='__main__':raise SystemExit(main())
