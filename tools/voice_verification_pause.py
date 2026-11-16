"""Bounded cooperative pause before a synthesis worker loads any model.

Only a request addressed to this worker's parent controller is honored. Nothing
stops another application or an active model; removing the exact request resumes
the same worker with all normal memory, proof and deadline guards still enabled.
"""
import hashlib,json,math,os,time,uuid
from pathlib import Path

def wait_if_requested(root,*,parent_pid=None,clock=time.monotonic,sleep=time.sleep):
    root=Path(root).resolve();request=root/'build-character-cast/voice-verification-pause.json'
    if not request.exists():return None
    if request.is_symlink():raise ValueError('Linked voice verification request refused')
    raw=request.read_bytes();n=json.loads(raw);parent=os.getppid() if parent_pid is None else parent_pid
    if n.get('parent_pid')!=parent:return None
    token=str(uuid.UUID(n['token']))
    if token!=n['token'] or n.get('schema')!=1 or n.get('maximum_wait_seconds') not in (300,600,900):raise ValueError('Invalid bounded verification request')
    controller=Path(n['controller_report']).resolve(strict=True)
    if controller.name!='report.json' or controller.parent.parent!=(root/'build-character-cast/verified-staged-audio').resolve():raise ValueError('Verification controller escaped owned reports')
    controller_state=json.loads(controller.read_text())
    if controller_state.get('status')!='running':raise ValueError('Verification request is not for a running controller')
    resume_floor=controller_state.get('required_start_free_gib',640/1024)
    if type(resume_floor) not in (int,float) or not math.isfinite(resume_floor) or not 640/1024<=resume_floor<=4:raise ValueError('Invalid measured controller resume floor')
    folder=root/'build-character-cast/voice-verification-pauses';folder.mkdir(exist_ok=True)
    if folder.is_symlink() or getattr(folder.lstat(),'st_file_attributes',0)&0x400:raise ValueError('Linked verification receipt directory')
    proof=folder/(token+'.json')
    if proof.exists():raise ValueError('Verification token was already used')
    start=clock();deadline=start+n['maximum_wait_seconds'];state={'status':'waiting','token':token,'parent_pid':parent,'worker_pid':os.getpid(),'request_sha256':hashlib.sha256(raw).hexdigest(),'maximum_wait_seconds':n['maximum_wait_seconds'],'minimum_resume_free_gib':resume_floor,'model_started_before_release':False}
    def save():proof.write_text(json.dumps(state,indent=2)+'\n')
    save();print('Voice worker idle for bounded game verification: '+str(proof),flush=True)
    try:
        while request.exists():
            if request.is_symlink() or request.read_bytes()!=raw:raise ValueError('Verification request changed while waiting')
            if clock()>=deadline:raise TimeoutError('Finite game-verification pause expired; no voice model started')
            sleep(min(1,deadline-clock()))
        state.update(status='released',waited_seconds=round(clock()-start,3));save();return proof
    except BaseException as error:
        state.update(status='failed',waited_seconds=round(clock()-start,3),error=str(error));save();raise
