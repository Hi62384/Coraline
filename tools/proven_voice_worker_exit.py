"""Keep only this worker's page scope alive through verified completion.

Successful model wrappers have already closed their WAV/journal/proof files.
Terminate that isolated worker after flushing its output, rather than asking
Python/CUDA finalizers to refault several GiB of retired checkpoint pages. The
owning job still enforces RAM, commit and time limits and checks every proof.
Errors take the ordinary exception path; they never produce a success exit.
"""
import hashlib
import json
import os
from pathlib import Path
import sys
import time
from low_memory_voice_loader import release_own_loading_pages

ROOT=Path(__file__).resolve().parents[1]
ALLOWED=('generate_early_reference_voice.py','check_early_reference_voice.py')


def run(callback,source):
    source=Path(source).resolve(strict=True)
    if source.parent!=ROOT/'tools' or source.name not in ALLOWED or os.name!='nt':
        raise RuntimeError('Verified completion exit is confined to the original owned Windows speech workers')
    with release_own_loading_pages():
        # No exception interception: a failed model or comparison exits nonzero
        # through the original launcher and cannot create this completion proof.
        callback()
        folder=ROOT/'build-character-cast/worker-exit-proofs';folder.mkdir(parents=True,exist_ok=True)
        path=folder/(time.strftime('%Y%m%dT%H%M%SZ',time.gmtime())+'-'+str(os.getpid())+'.json')
        paths=(source,Path(__file__).resolve(),ROOT/'tools/low_memory_voice_loader.py')
        proof=dict(status='passed',callback_completed=True,original_speech_settings=True,
                   exit_scope='owned_worker_only',source_sha256={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths})
        with path.open('x',encoding='utf-8') as output:
            output.write(json.dumps(proof,indent=2)+'\n');output.flush();os.fsync(output.fileno())
        print('Verified owned speech completion exit: '+str(path),flush=True)
        sys.stdout.flush();sys.stderr.flush()
        # The operating system releases all owned mappings/threads/GPU handles.
        # No application, parent controller or unrelated process is touched.
        os._exit(0)


def verify(root,path):
    root=Path(root).resolve();path=Path(path).resolve(strict=True)
    if path.parent!=(root/'build-character-cast/worker-exit-proofs').resolve():raise ValueError('Unexpected worker exit receipt')
    proof=json.loads(path.read_text(encoding='utf-8'))
    if any(proof.get(k)!=v for k,v in dict(status='passed',callback_completed=True,original_speech_settings=True,exit_scope='owned_worker_only').items()):raise ValueError('Incomplete speech completion exit')
    sources=proof.get('source_sha256',{})
    base={'tools/proven_voice_worker_exit.py','tools/low_memory_voice_loader.py'}
    if len(sources)!=3 or not base.issubset(sources) or len(set(sources)-base)!=1:raise ValueError('Incomplete worker source pins')
    extra=next(iter(set(sources)-base))
    if extra not in {'tools/'+name for name in ALLOWED}:raise ValueError('Unexpected worker source')
    if any(hashlib.sha256((root/name).read_bytes()).hexdigest()!=digest for name,digest in sources.items()):raise ValueError('Worker completion sources changed')
    return proof,hashlib.sha256(path.read_bytes()).hexdigest()
