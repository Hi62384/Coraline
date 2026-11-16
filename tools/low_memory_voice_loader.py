"""Process-local IndexTTS loading adapters, without editing model checkpoints.

Avoid the simultaneous CPU random parameters + deserialized CPU checkpoint +
GPU parameters. Checkpoint pages are mapped read-only and copied into the final
GPU modules. The GPT uses exactly the fp16 inference precision of the old path;
other modules retain fp32. No quantization, reference substitution or truncation.
"""
import functools
import ctypes
import os
import threading
from contextlib import contextmanager
from pathlib import Path
import zipfile


@contextmanager
def release_own_loading_pages():
    """Bound this process's clean DLL/checkpoint pages during import/loading only.

    Does not change models, system/pagefile settings or other working sets. The
    thread always stops on leaving this small loading scope, including errors.
    The external384MiB emergency guard still bounds the whole job.
    """
    if os.name!='nt':
        yield
        return
    done=threading.Event();errors=[]
    def release_pages():
        while not done.wait(.05):
            try:trim_own_mapped_pages()
            except OSError as error:errors.append(str(error));return
    worker=threading.Thread(target=release_pages,daemon=True);worker.start()
    try:yield
    finally:
        done.set();worker.join(timeout=2);trim_own_mapped_pages()
        if worker.is_alive() or errors:raise RuntimeError('Own loading-page release failed: '+str(errors))


def retire_cached_reference_encoder(engine, reference):
    """Release the CPU encoder only after its unchanged conditioning is cached.

    IndexTTS caches both passes for a fixed speaker/emotion reference before
    announcing text processing. The encoder is not part of speech generation.
    Keep all conditioning tensors; any cache/reference drift fails closed.
    """
    if (engine.cache_spk_audio_prompt!=reference or engine.cache_emo_audio_prompt!=reference
            or any(getattr(engine,name,None) is None for name in
                ('cache_spk_cond','cache_s2mel_style','cache_s2mel_prompt','cache_mel','cache_emo_cond'))):
        raise RuntimeError('Fixed reference conditioning cache is incomplete or changed')
    if getattr(engine,'_coraline_reference_retired',False):return False
    if engine.semantic_model is None:raise RuntimeError('Reference encoder missing before conditioning completed')
    engine.semantic_model=None
    engine._coraline_reference_retired=True
    return True


def install_cached_reference_retirement(engine, reference):
    """Use the engine's post-conditioning hook, before the first speech pass."""
    import gc
    original_progress=engine._set_gr_progress
    original_embeddings=engine.get_emb
    def embeddings(*args,**kwargs):
        if getattr(engine,'_coraline_reference_retired',False):
            raise RuntimeError('Fixed reference cache was invalidated; refusing unconditioned speech')
        return original_embeddings(*args,**kwargs)
    def progress(value,description):
        if value==.1 and description=='text processing...':
            if retire_cached_reference_encoder(engine,reference):
                gc.collect();trim_own_mapped_pages()
                print('Retired CPU reference encoder; original speaker/style/emotion conditioning retained.',flush=True)
        return original_progress(value,description)
    engine.get_emb=embeddings
    engine._set_gr_progress=progress


def trim_own_mapped_pages():
    """Release only this process's clean mapped pages, not another app's RAM.

    Windows otherwise lets sequential checkpoint reads accumulate in our active
    working set until after the model is loaded. The weights remain in the file
    mapping/GPU, and are faulted back in normally if needed. No system settings
    or pagefile changes are made.
    """
    if os.name != 'nt':return
    from ctypes import wintypes
    kernel=ctypes.WinDLL('kernel32',use_last_error=True)
    kernel.GetCurrentProcess.restype=wintypes.HANDLE
    empty=ctypes.WinDLL('psapi',use_last_error=True).EmptyWorkingSet
    empty.argtypes=[wintypes.HANDLE];empty.restype=wintypes.BOOL
    if not empty(kernel.GetCurrentProcess()):raise ctypes.WinError(ctypes.get_last_error())


def install_state_copy_trimming(model,torch):
    """Bound clean CPU checkpoint pages during direct-to-GPU ASR weight copies."""
    copied=[0]
    def loaded(module,incompatible):
        copied[0]+=sum(p.numel()*p.element_size() for p in module.parameters(recurse=False))
        if copied[0]>=16*1024*1024 or module is model:
            torch.cuda.synchronize();trim_own_mapped_pages();copied[0]=0
    for module in model.modules():module.register_load_state_dict_post_hook(loaded)


def gpu_module_constructor(original,torch,device,dtype):
    """Preserve a Hub model's class methods while avoiding CPU initialization."""
    @functools.wraps(original)
    def initialize(module,*args,**kwargs):
        previous=torch.get_default_dtype()
        try:
            torch.set_default_dtype(dtype)
            with torch.device(device):original(module,*args,**kwargs)
        finally:torch.set_default_dtype(previous)
        install_state_copy_trimming(module,torch)
        print('Constructed vocoder directly on '+device+' ('+str(dtype)+')',flush=True)
    return initialize


def install(infer, torch, device, model_dir):
    if not device.startswith('cuda'):
        raise ValueError('Memory-efficient voice loading requires the CUDA inference path')
    model_dir = Path(model_dir).resolve()
    original_load = torch.load

    @functools.wraps(original_load)
    def mapped_load(filename, *args, **kwargs):
        mapped=False
        if isinstance(filename, (str, Path)):
            path = Path(filename).resolve()
            if path.is_relative_to(model_dir) and zipfile.is_zipfile(path):
                kwargs.setdefault('mmap', True)
                mapped=True
                print('Memory-mapped checkpoint: '+path.name,flush=True)
        if not mapped:return original_load(filename,*args,**kwargs)
        # Deserialization itself can touch many clean file-backed pages before
        # load_state_dict's per-layer hooks get a chance to release them.
        done=threading.Event();errors=[]
        def release_pages():
            while not done.wait(.05):
                try:trim_own_mapped_pages()
                except OSError as error:errors.append(str(error));return
        worker=threading.Thread(target=release_pages,daemon=True);worker.start()
        try:return original_load(filename,*args,**kwargs)
        finally:
            done.set();worker.join(timeout=2);trim_own_mapped_pages()
            if errors:raise RuntimeError('Own-process mapped-page release failed: '+errors[0])

    torch.load = mapped_load

    def gpu_factory(original, dtype):
        @functools.wraps(original)
        def create(*args, **kwargs):
            print('Constructing '+original.__name__+' on '+device+' ('+str(dtype)+')',flush=True)
            previous = torch.get_default_dtype()
            try:
                torch.set_default_dtype(dtype)
                with torch.device(device):
                    result = original(*args, **kwargs)
                copied=[0]
                def loaded(module,incompatible):
                    copied[0]+=sum(p.numel()*p.element_size() for p in module.parameters(recurse=False))
                    if copied[0]>=16*1024*1024 or module is result:
                        torch.cuda.synchronize();trim_own_mapped_pages();copied[0]=0
                for module in result.modules():module.register_load_state_dict_post_hook(loaded)
                print('Constructed '+original.__name__,flush=True)
                return result
            finally:
                torch.set_default_dtype(previous)
        return create

    # GPT no longer allocates 3.5 GB of temporary initialized CPU parameters,
    # nor a temporary fp32 GPU copy before half() conversion.
    infer.UnifiedVoice = gpu_factory(infer.UnifiedVoice, torch.float16)
    infer.MyModel = gpu_factory(infer.MyModel, torch.float32)
    infer.build_semantic_codec = gpu_factory(infer.build_semantic_codec, torch.float32)
    infer.CAMPPlus = gpu_factory(infer.CAMPPlus, torch.float32)
    # BigVGAN's custom Hub loader first calls cls(h), then loads its checkpoint.
    # Keep that original class/loader, checkpoint and FP32 precision, but avoid
    # its additional ~1GiB initialized CPU copy before the later .to(cuda).
    vocoder=infer.bigvgan.BigVGAN
    vocoder.__init__=gpu_module_constructor(vocoder.__init__,torch,device,torch.float32)

    original_reference_loader = infer.Wav2Vec2BertModel.from_pretrained

    class CPUReference:
        @staticmethod
        def from_pretrained(*args, **kwargs):
            # Safetensors/Transformers load CPU tensors from mapped storage.
            # Keep the reference encoder there from the outset; the established
            # 6 GB-card get_emb override in the generator runs it on CPU.
            kwargs.update(low_cpu_mem_usage=True, device_map={'': 'cpu'}, torch_dtype=torch.float32)
            model = original_reference_loader(*args, **kwargs)
            original_to = model.to

            def stay_on_cpu(*to_args, **to_kwargs):
                target = to_args[0] if to_args else to_kwargs.get('device')
                if target is not None and str(target).startswith('cuda'):
                    return model
                return original_to(*to_args, **to_kwargs)

            model.to = stay_on_cpu
            # Reference inference is sequential: release clean file-backed
            # encoder pages between transformer layers rather than retaining
            # the entire 2.3 GB reference model in physical RAM.
            for layer in model.encoder.layers:
                layer.register_forward_hook(lambda module,inputs,output: trim_own_mapped_pages())
            trim_own_mapped_pages()
            return model

    infer.Wav2Vec2BertModel = CPUReference
    print('Low-memory loader: mapped weights; direct GPU modules; CPU reference encoder.', flush=True)
