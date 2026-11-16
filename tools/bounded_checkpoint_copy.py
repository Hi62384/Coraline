"""Bound checkpoint staging allocations while preserving normal load_state_dict.

Every intercepted CPU-to-CUDA chunk is compared with the original source cast to
the unchanged destination dtype. Original module hooks/key/shape checks still run.
"""
from contextlib import contextmanager
from functools import wraps


def bound_own_inference_pages(engine, scope):
    """Page only this worker's clean mappings during the original inference.

    New kernels can fault large DLL/checkpoint mappings back into our working
    set after constructor trimming. Keep the existing process-local release
    scope active until inference returns; no tensors, RNG or arguments change.
    The outer job's emergency memory guard remains authoritative.
    """
    original=engine.infer
    @wraps(original)
    def infer(*args,**kwargs):
        with scope():return original(*args,**kwargs)
    engine.infer=infer


def spans(elements,source_width,target_width,limit):
    if min(elements,source_width,target_width,limit)<=0:raise ValueError('Invalid checkpoint copy dimensions')
    step=max(1,limit//max(source_width,target_width))
    for start in range(0,elements,step):yield start,min(elements,start+step)


@contextmanager
def bounded_copies(torch,trim,limit=2*1024*1024):
    original=torch.Tensor.copy_
    stats={'chunk_limit_bytes':limit,'copies':0,'chunks':0,'verified_target_bytes':0,'all_values_verified':True}
    def copy(target,source,non_blocking=False):
        if (not isinstance(source,torch.Tensor) or target.device.type!='cuda' or source.device.type!='cpu' or tuple(target.shape)!=tuple(source.shape)
                or target.numel()*max(target.element_size(),source.element_size())<=limit):
            return original(target,source,non_blocking=non_blocking)
        if not target.is_contiguous() or not source.is_contiguous():
            raise ValueError('Large noncontiguous checkpoint copy needs explicit review')
        stats['copies']+=1
        destination=target.reshape(-1);origin=source.reshape(-1)
        for start,end in spans(target.numel(),source.element_size(),target.element_size(),limit):
            actual_target=destination[start:end];actual_source=origin[start:end]
            original(actual_target,actual_source,non_blocking=False)
            torch.cuda.synchronize()
            expected=actual_source.to(device='cpu',dtype=target.dtype)
            actual=actual_target.cpu()
            if actual.dtype!=expected.dtype or not torch.equal(actual,expected):
                stats['all_values_verified']=False
                raise ValueError('Checkpoint chunk values changed during loading')
            stats['chunks']+=1;stats['verified_target_bytes']+=(end-start)*target.element_size()
            del expected,actual,actual_target,actual_source
            trim()
        return target
    torch.Tensor.copy_=copy
    try:yield stats
    finally:torch.Tensor.copy_=original
