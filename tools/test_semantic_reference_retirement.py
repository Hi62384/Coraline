from types import SimpleNamespace
from unittest.mock import Mock
import unittest
from semantic_reference_retirement import install


class Base:
    def __init__(self):self.allowed=True;self.release=Mock()
    def install(self,inference):pass
    def admit(self):
        if not self.allowed:raise ValueError('cache not admitted')


class Model:
    def __init__(self,device='cpu',count=260_000_000):
        self.tensor=SimpleNamespace(device=SimpleNamespace(type=device),numel=lambda:count,element_size=lambda:4)
        self._modules={'weights':self.tensor};self._parameters={};self._buffers={}
    def parameters(self):return iter([self.tensor])
    def buffers(self):return iter([])
    def eval(self):return self


class SemanticRetirementTests(unittest.TestCase):
    def fixture(self,model):
        stage=install(Base)();loader=Mock(return_value=model)
        inference=SimpleNamespace(Wav2Vec2BertModel=SimpleNamespace(from_pretrained=loader));stage.install(inference)
        return stage,inference.Wav2Vec2BertModel.from_pretrained,loader
    def test_original_loader_runs_and_missing_cache_keeps_weights(self):
        model=Model();stage,load,original=self.fixture(model);stage.allowed=False
        self.assertIs(load('original-checkpoint'),model);original.assert_called_once_with('original-checkpoint')
        with self.assertRaisesRegex(ValueError,'cache not admitted'):model.eval()
        self.assertTrue(model._modules);stage.release.assert_not_called()
    def test_original_loaded_cpu_weights_retire_and_fallback_fails_closed(self):
        model=Model();stage,load,original=self.fixture(model);load('checkpoint');self.assertIs(model.eval(),model)
        self.assertEqual(stage.semantic_bytes,1_040_000_000);self.assertFalse(model._modules);stage.release.assert_called_once_with()
        with self.assertRaisesRegex(RuntimeError,'conditioning cache missing'):model.forward()
        self.assertEqual(stage.semantic_calls,1)
    def test_unexpected_gpu_or_small_module_is_preserved(self):
        for model in (Model('cuda'),Model(count=10)):
            stage,load,_=self.fixture(model);load('checkpoint')
            with self.assertRaises(ValueError):model.eval()
            self.assertTrue(model._modules);stage.release.assert_not_called()


if __name__=='__main__':unittest.main()
