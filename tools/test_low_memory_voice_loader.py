import types
import unittest
from unittest.mock import patch
from low_memory_voice_loader import retire_cached_reference_encoder,install_cached_reference_retirement,install_state_copy_trimming,release_own_loading_pages,gpu_module_constructor


class CachedReferenceTests(unittest.TestCase):
    def test_vocoder_constructor_keeps_class_and_fp32_and_restores_defaults(self):
        from contextlib import contextmanager
        state={'dtype':'previous','device':'cpu'};seen=[]
        @contextmanager
        def device(target):
            previous=state['device'];state['device']=target
            try:yield
            finally:state['device']=previous
        torch=types.SimpleNamespace(get_default_dtype=lambda:state['dtype'],
            set_default_dtype=lambda value:state.update(dtype=value),device=device)
        def initialize(module,value):seen.append((module,value,state.copy()))
        module=object()
        with patch('low_memory_voice_loader.install_state_copy_trimming') as trimming:
            constructor=gpu_module_constructor(initialize,torch,'cuda:0','float32')
            self.assertIsNone(constructor(module,7));trimming.assert_called_once_with(module,torch)
        self.assertEqual(seen,[(module,7,{'dtype':'float32','device':'cuda:0'})])
        self.assertEqual(state,{'dtype':'previous','device':'cpu'})

    def test_import_page_release_scope_cleans_up_on_success_and_error(self):
        with patch('low_memory_voice_loader.trim_own_mapped_pages') as trim:
            with release_own_loading_pages():pass
            with self.assertRaisesRegex(ValueError,'original failure'):
                with release_own_loading_pages():raise ValueError('original failure')
            if __import__('os').name=='nt':self.assertGreaterEqual(trim.call_count,2)

    def test_asr_copy_hooks_release_only_after_bounded_copies_or_root(self):
        hooks=[]
        class Module:
            def parameters(self,recurse=False):return [types.SimpleNamespace(numel=lambda:4*1024*1024,element_size=lambda:4)]
            def register_load_state_dict_post_hook(self,hook):hooks.append((self,hook))
        model=Module();child=Module();model.modules=lambda:[child,model]
        cuda=types.SimpleNamespace(synchronize=unittest.mock.Mock())
        with patch('low_memory_voice_loader.trim_own_mapped_pages') as trim:
            install_state_copy_trimming(model,types.SimpleNamespace(cuda=cuda))
            for module,hook in hooks:hook(module,None)
            self.assertEqual(cuda.synchronize.call_count,2);self.assertEqual(trim.call_count,2)

    def engine(self,ready=True):
        fields={name:object() if ready else None for name in
            ('cache_spk_cond','cache_s2mel_style','cache_s2mel_prompt','cache_mel','cache_emo_cond')}
        return types.SimpleNamespace(semantic_model=object(),cache_spk_audio_prompt='reference.wav',
            cache_emo_audio_prompt='reference.wav',get_emb=lambda *_:'original conditioning',
            _set_gr_progress=lambda *_:None,**fields)
    def test_incomplete_cache_preserves_encoder(self):
        engine=self.engine(False);encoder=engine.semantic_model
        with self.assertRaisesRegex(RuntimeError,'incomplete'):retire_cached_reference_encoder(engine,'reference.wav')
        self.assertIs(engine.semantic_model,encoder)
    def test_complete_cache_keeps_every_original_conditioning_tensor(self):
        engine=self.engine();before=dict(vars(engine))
        self.assertTrue(retire_cached_reference_encoder(engine,'reference.wav'))
        self.assertIsNone(engine.semantic_model)
        for name,value in before.items():
            if name.startswith('cache_'):self.assertIs(getattr(engine,name),value)
        self.assertFalse(retire_cached_reference_encoder(engine,'reference.wav'))
        engine.cache_emo_audio_prompt='changed.wav'
        with self.assertRaisesRegex(RuntimeError,'changed'):retire_cached_reference_encoder(engine,'reference.wav')
    def test_hook_waits_for_conditioning_and_rejects_any_new_encoding(self):
        engine=self.engine()
        with patch('low_memory_voice_loader.trim_own_mapped_pages') as trim:
            install_cached_reference_retirement(engine,'reference.wav')
            engine._set_gr_progress(0,'starting inference...')
            self.assertIsNotNone(engine.semantic_model)
            self.assertEqual(engine.get_emb(None),'original conditioning')
            engine._set_gr_progress(.1,'text processing...')
            self.assertIsNone(engine.semantic_model);trim.assert_called_once()
            with self.assertRaisesRegex(RuntimeError,'invalidated'):engine.get_emb(None)


if __name__=='__main__':unittest.main()
