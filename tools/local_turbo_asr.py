"""Checksum-pinned local Whisper inference; recordings are never uploaded."""
import hashlib
def load(root,memory_efficient=False):
    import torch,whisper
    checkpoint=root/'.voice-tools/whisper/large-v3-turbo.pt'
    with checkpoint.open('rb') as stream:digest=hashlib.file_digest(stream,'sha256').hexdigest()
    if digest!='aff26ae408abcba5fbf8813c21e62b0941638c5f6eebfb145be0c9839262a19a':raise RuntimeError('Official ASR model identity changed')
    if not torch.cuda.is_available():raise RuntimeError('Local ASR GPU is unavailable')
    state=torch.load(str(checkpoint),map_location='cpu',mmap=True,weights_only=True)
    previous=torch.get_default_dtype();torch.set_default_dtype(torch.float16)
    try:
        with torch.device('cuda'):model=whisper.model.Whisper(whisper.model.ModelDimensions(**state['dims']))
    finally:torch.set_default_dtype(previous)
    if memory_efficient:
        from low_memory_voice_loader import install_state_copy_trimming
        install_state_copy_trimming(model,torch)
    model.load_state_dict(state['model_state_dict']);del state
    for module in model.modules():
        if isinstance(module,torch.nn.LayerNorm):module.float()
    return model.eval()
