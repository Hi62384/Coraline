from types import SimpleNamespace
import unittest
import numpy as np
from contextlib import contextmanager
from bounded_checkpoint_copy import bounded_copies,spans,bound_own_inference_pages


class Tensor:
    def __init__(self,array,device):self.array=array;self.device=SimpleNamespace(type=device);self.dtype=array.dtype;self.shape=array.shape
    def numel(self):return self.array.size
    def element_size(self):return self.array.dtype.itemsize
    def is_contiguous(self):return self.array.flags.c_contiguous
    def reshape(self,*shape):return Tensor(self.array.reshape(*shape),self.device.type)
    def __getitem__(self,key):return Tensor(self.array[key],self.device.type)
    def cpu(self):return Tensor(self.array.copy(),'cpu')
    def to(self,*,device,dtype):return Tensor(self.array.astype(dtype),device)
    def copy_(self,source,non_blocking=False):self.array[...]=source.array;return self


class BoundedCheckpointCopyTests(unittest.TestCase):
    def test_inference_page_scope_preserves_arguments_result_and_exception(self):
        events=[];result=object();argument=object()
        @contextmanager
        def scope():
            events.append('enter')
            try:yield
            finally:events.append('exit')
        def infer(value,*,fail=False):
            self.assertIs(value,argument);events.append('infer')
            if fail:raise ValueError('original inference failure')
            return result
        engine=SimpleNamespace(infer=infer)
        bound_own_inference_pages(engine,scope)
        self.assertIs(engine.infer(argument),result)
        self.assertEqual(events,['enter','infer','exit'])
        with self.assertRaisesRegex(ValueError,'original inference failure'):engine.infer(argument,fail=True)
        self.assertEqual(events,['enter','infer','exit','enter','infer','exit'])

    def torch(self):return SimpleNamespace(Tensor=Tensor,cuda=SimpleNamespace(synchronize=lambda:None),
                                           equal=lambda a,b:np.array_equal(a.array,b.array))
    def test_chunks_cover_every_element_and_dtype_conversion_exactly(self):
        source=Tensor(np.linspace(-.01,.01,10000,dtype=np.float32),'cpu')
        target=Tensor(np.zeros(10000,dtype=np.float16),'cuda');original=Tensor.copy_;trim=[]
        with bounded_copies(self.torch(),lambda:trim.append(1),limit=1024) as report:
            self.assertIs(target.copy_(source),target)
        self.assertIs(Tensor.copy_,original)
        self.assertTrue(np.array_equal(target.array,source.array.astype(np.float16)))
        self.assertGreater(report['chunks'],1);self.assertEqual(report['chunks'],len(trim))
        self.assertEqual(report['verified_target_bytes'],target.array.nbytes)
        boundaries=list(spans(10000,4,2,1024))
        self.assertEqual(sum(end-start for start,end in boundaries),10000)
        self.assertTrue(all((end-start)*4<=1024 for start,end in boundaries))

    def test_changed_values_fail_before_acceptance_and_restore_original_copy(self):
        source=Tensor(np.ones(1000,dtype=np.float32),'cpu');target=Tensor(np.zeros(1000,dtype=np.float32),'cuda')
        original=Tensor.copy_
        def broken(destination,origin,non_blocking=False):destination.array[...]=origin.array+1;return destination
        Tensor.copy_=broken
        try:
            with self.assertRaisesRegex(ValueError,'values changed'):
                with bounded_copies(self.torch(),lambda:None,limit=1024):target.copy_(source)
            self.assertIs(Tensor.copy_,broken)
        finally:Tensor.copy_=original

    def test_native_small_or_same_device_copy_remains_unchanged(self):
        source=Tensor(np.arange(8,dtype=np.float32),'cpu');target=Tensor(np.zeros(8,dtype=np.float32),'cuda')
        with bounded_copies(self.torch(),lambda:None,limit=1024) as report:target.copy_(source)
        self.assertEqual(report['chunks'],0);self.assertTrue(np.array_equal(source.array,target.array))


if __name__=='__main__':unittest.main()
