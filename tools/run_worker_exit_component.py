"""One detached PCM comparison after the recorded finalization RAM failure.
No catalogue generation, retry loop or changed emergency cutoff.
"""
import json
from pathlib import Path
import subprocess
import sys
import time
from run_parent_audio_chunks import wait_ready

ROOT=Path(__file__).resolve().parents[1]


def main():
    failure=ROOT/'build-character-cast/compact-voice/20261029T013849Z/report.json'
    n=json.loads(failure.read_text());log=(failure.parent/'output.log').read_text(encoding='utf-8',errors='replace')
    if n.get('status')!='low_memory_cutoff' or n.get('stop_free_mib')!=384 or not n.get('own_child_stopped'):raise ValueError('Expected preserved failed original worker')
    required=('Completed bounded generation batch; full coverage remains a separate gate.','Verified staged flow weights: ','Verified staged vocoder weights: ','Verified early reference retirement: ')
    if any(marker not in log for marker in required):raise ValueError('Failure was not after completed generation and model proofs')
    print('Detached changed-exit component only; original failed worker remains failed; start floor 3.25 GiB.',flush=True)
    wait_ready(time.monotonic()+3600,minimum_gib=3.25)
    command=[sys.executable,'tools/run_compact_voice_batch.py','--speaker','mother','--full-dialogue',
        '--state','other_mother_events_wybie_adventure_caught_normal_low_high_01',
        '--memory-efficient','--low-memory-attempt','--require-reference-cache','--stage-flow-model',
        '--stage-vocoder','--cuda-async-allocator','--early-reference-retirement','--early-reference-probe',
        '--timeout-seconds','1800']
    return subprocess.run(command,cwd=ROOT,timeout=1860).returncode


if __name__=='__main__':raise SystemExit(main())
