"""Import all source/quality-checked incidental speech, merging shared tables."""
import os
from pathlib import Path
import subprocess
import sys
from incidental_dialogue_catalog import MANIFESTS, recordings
from voice_recording_proof import validate

ROOT=Path(__file__).resolve().parents[1]


def main():
    cache={}
    records=recordings(ROOT)
    if len(records)!=78:raise ValueError('Incomplete incidental parent speech catalogue')
    # Both speakers must pass before the first shared manifest is edited.
    for record in records.values():validate(ROOT,record,cache)
    for manifest in MANIFESTS:
        subprocess.run([sys.executable,'tools/import_welcome_audio.py'],cwd=ROOT,
                       env=dict(os.environ,CORALINE_VOICE_MANIFEST=manifest),check=True,timeout=600)


if __name__=='__main__':main()
