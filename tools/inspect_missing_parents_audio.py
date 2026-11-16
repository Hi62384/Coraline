"""Offline source-content inspection only. No synthesis or recording edits; ASR is not an approved subtitle master."""
from pathlib import Path
import hashlib,json,subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
def worker():
    import torch,whisper
    torch.set_num_threads(2)
    review='--review' in sys.argv or '--cpu-review' in sys.argv
    gpu=review and '--cpu-review' not in sys.argv
    # Decode before CUDA initialization: the Windows driver reserves substantial
    # commit, which otherwise prevents ffmpeg from starting inside the job cap.
    decoded={path:whisper.load_audio(str(path)) for folder in ('MissingParentsSpinkAndForcible','MissingParentsCat','EnterOtherWorldParentRescue') for path in sorted((ROOT/'voicelines/story'/folder).glob('*.wav'))} if review else {}
    checkpoint=ROOT/'.voice-tools/whisper'/('small.en.pt' if review else 'base.en.pt')
    state=torch.load(str(checkpoint),map_location='cpu',mmap=True,weights_only=True)
    # Whisper constructs a sparse alignment-head buffer; Torch cannot construct
    # that buffer on meta. Build on CPU, then assign the mmap-backed weights.
    if gpu:
        if not torch.cuda.is_available() or torch.cuda.mem_get_info()[0]<2500*1024**2:raise RuntimeError('Independent GPU review needs 2500 MiB free VRAM')
        dtype=torch.get_default_dtype();torch.set_default_dtype(torch.float16)
        try:
            with torch.device('cuda'):model=whisper.model.Whisper(whisper.model.ModelDimensions(**state['dims']))
        finally:torch.set_default_dtype(dtype)
        model.half().load_state_dict(state['model_state_dict'])
        for module in model.modules():
            if isinstance(module,torch.nn.LayerNorm):module.float()
    else:
        model=whisper.model.Whisper(whisper.model.ModelDimensions(**state['dims']))
        # Copy into the CPU float32 model directly. assign-then-float retains
        # extra private half tensors during conversion on memory-limited hosts.
        model.load_state_dict(state['model_state_dict'])
    del state;model.eval()
    output=ROOT/('artwork/missing_parents_audio_review.json' if review else 'artwork/missing_parents_audio_inspection.json')
    report=json.loads(output.read_text()) if output.exists() else {}
    for folder in ('MissingParentsSpinkAndForcible','MissingParentsCat','EnterOtherWorldParentRescue'):
        for path in sorted((ROOT/'voicelines/story'/folder).glob('*.wav')):
            key=folder+'/'+path.name;digest=hashlib.sha256(path.read_bytes()).hexdigest()
            if key in report and report[key].get('source_sha256')==digest:continue
            result=model.transcribe(decoded[path] if review else str(path),language='en',fp16=gpu,verbose=False,beam_size=5 if review else 1,best_of=1,condition_on_previous_text=False,temperature=0)
            report[key]={'source_sha256':digest,'review_status':'automatic_inspection_not_approved_subtitles','text':result['text'],'segments':[{'start':s['start'],'end':s['end'],'text':s['text'],'avg_logprob':s.get('avg_logprob'),'no_speech_prob':s.get('no_speech_prob'),'compression_ratio':s.get('compression_ratio')} for s in result['segments']]}
            output.write_text(json.dumps(report,indent=2)+'\n')
            print(key+': '+result['text'],flush=True)
    output.write_text(json.dumps(report,indent=2)+'\n')
def main():
    if '--worker' in sys.argv:worker();return
    import ctypes
    from ctypes import wintypes
    from run_low_memory_release import job
    import check_numbered_candidate as monitor
    if monitor.free_gib()<monitor.START_FREE_GIB:raise RuntimeError('Below unchanged memory start floor')
    review='--review' in sys.argv or '--cpu-review' in sys.argv
    output=ROOT/('artwork/missing_parents_audio_review.log' if review else 'artwork/missing_parents_audio_inspection.log');api,handle=job(3072 if review else 2048)
    try:
        with output.open('wb') as log:
            mode=['--cpu-review'] if '--cpu-review' in sys.argv else ['--review'] if review else []
            child=subprocess.Popen([str(ROOT/'.voice-tools/index-tts/.venv/Scripts/python.exe'),str(Path(__file__)),'--worker']+mode,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT)
            if not api.AssignProcessToJobObject(handle,wintypes.HANDLE(int(child._handle))):child.kill();raise ctypes.WinError(ctypes.get_last_error())
            report=monitor.watch_child(child,600,monitor.free_gib,monitor.process_memory)
        report.update(mode=mode or ['--inspection'],voice_generation=False)
        (ROOT/('artwork/missing_parents_audio_review_run.json' if review else 'artwork/missing_parents_audio_inspection_run.json')).write_text(json.dumps(report,indent=2)+'\n');print(monitor.tail_bytes(output));raise SystemExit(0 if report.get('status')=='passed' else 1)
    finally:api.CloseHandle(handle)
if __name__=='__main__':main()
