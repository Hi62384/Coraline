"""Second, name-aware ASR pass of supplied masters. Draft evidence, never approval.

Uses the already cached small.en checkpoint. The prompt contains character names,
not expected dialogue. Segment timestamps and decoder confidence are retained for
review; this script never modifies subtitle text or the approval ledger.
"""
from pathlib import Path
import argparse, hashlib, json, subprocess, time, sys
from contextlib import nullcontext
from low_memory_voice_loader import release_own_loading_pages
with release_own_loading_pages() if '--low-memory-attempt' in sys.argv else nullcontext():
    import torch, whisper
from original_story_audio_catalog import originals as original_catalog

ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--turbo',action='store_true');parser.add_argument('--low-memory-attempt',action='store_true');args=parser.parse_args()
if args.low_memory_attempt and not args.turbo:raise RuntimeError('Low-memory draft check requires the pinned Turbo path')
engine='whisper large-v3-turbo local CUDA beam pass' if args.turbo else 'whisper small.en independent name-aware beam pass'
assets=ROOT/'src/main/resources/assets/coraline'
originals=original_catalog(ROOT)
cache=json.loads((ROOT/'tools/story_transcripts.json').read_text(encoding='utf-8'))
report_path=ROOT/'build-character-cast/original-transcription'/('comparison-turbo.json' if args.turbo else 'comparison-small.json')
report=json.loads(report_path.read_text()) if report_path.exists() else {}
torch.set_num_threads(4)
checkpoint=ROOT/'.voice-tools/whisper'/('large-v3-turbo.pt' if args.turbo else 'small.en.pt')
if args.turbo:
    with checkpoint.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
    if digest!='aff26ae408abcba5fbf8813c21e62b0941638c5f6eebfb145be0c9839262a19a':raise RuntimeError('Changed official ASR checkpoint')
    if not torch.cuda.is_available():raise RuntimeError('Turbo check requires the idle local GPU')
state=torch.load(str(checkpoint),map_location='cpu',mmap=True,weights_only=True)
if args.turbo:
    # Instantiate half-precision inference weights directly on the idle GPU,
    # avoiding a duplicate multi-gigabyte CPU model. Originals remain untouched.
    previous=torch.get_default_dtype();torch.set_default_dtype(torch.float16)
    try:
        with torch.device('cuda'):model=whisper.model.Whisper(whisper.model.ModelDimensions(**state['dims']))
    finally:torch.set_default_dtype(previous)
else:model=whisper.model.Whisper(whisper.model.ModelDimensions(**state['dims']))
if args.low_memory_attempt:
    from low_memory_voice_loader import install_state_copy_trimming
    install_state_copy_trimming(model,torch)
model.load_state_dict(state['model_state_dict']);del state
model.eval()
if args.turbo:
    # Whisper's LayerNorm explicitly evaluates float32 activations. Keep its
    # small affine parameters float32 too; convolutions/attention stay fp16.
    for module in model.modules():
        if isinstance(module,torch.nn.LayerNorm):module.float()
prompt='Coraline, Wybie, Miss Spink, Miss Forcible, Miriam, April, Mr. Bobinsky, Other Mother, Other Father.'
for name,entry in originals.items():
    if entry.get('music'):continue
    source=(ROOT/entry['source']).resolve()
    if not (source.is_relative_to(ROOT/'voicelines') or source.is_relative_to(ROOT/'music/theatre')):raise RuntimeError('Unexpected supplied master')
    digest=hashlib.sha256(source.read_bytes()).hexdigest()
    if digest!=entry['source_sha256']:raise RuntimeError('Changed supplied recording: '+name)
    if report.get(name,{}).get('source_sha256')==digest:continue
    result=model.transcribe(str(source),language='en',fp16=args.turbo,temperature=0,
        beam_size=5,condition_on_previous_text=False,initial_prompt=prompt)
    with source.open('rb') as stream:after=hashlib.file_digest(stream,'sha256').hexdigest()
    if after!=digest:raise RuntimeError('Supplied master changed during ASR; no draft published: '+name)
    report[name]={'source_sha256':digest,'engine':engine,
        'prompt':prompt,'prior_segments':cache.get(name,[]),'segments':result['segments'],
        'text':result['text'],'review_status':'pending','note':'No listening or approval claimed.'}
    report_path.parent.mkdir(parents=True,exist_ok=True)
    staged=report_path.with_suffix('.tmp')
    staged.write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    staged.replace(report_path)
    print(name+': '+result['text'],flush=True)
print('Comparison drafts complete. Explicit transcript review is still required.',flush=True)
