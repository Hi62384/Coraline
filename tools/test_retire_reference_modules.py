import unittest
from types import SimpleNamespace
from unittest.mock import Mock
from retire_reference_modules import retire

class RetiredReferenceGuards(unittest.TestCase):
    def engine(self):
        return SimpleNamespace(cache_spk_audio_prompt='reference.wav',cache_emo_audio_prompt='reference.wav',
            **{name:object() for name in ('cache_spk_cond','cache_s2mel_style','cache_s2mel_prompt','cache_mel','cache_emo_cond')})
    def test_foreign_or_incomplete_conditioning_never_touches_a_model(self):
        for change in ({'cache_spk_audio_prompt':'other.wav'},{'cache_emo_audio_prompt':'other.wav'},{'cache_mel':None}):
            engine=self.engine();engine.__dict__.update(change);torch=Mock();trim=Mock()
            with self.subTest(change=change),self.assertRaisesRegex(ValueError,'Complete restored'):retire(engine,'reference.wav',torch,trim)
            torch.cuda.empty_cache.assert_not_called();trim.assert_not_called()
    def test_unrecognized_architecture_is_preserved(self):
        engine=self.engine();engine.semantic_codec=SimpleNamespace();engine.campplus_model=object();before=engine.semantic_codec
        with self.assertRaisesRegex(ValueError,'architecture'):retire(engine,'reference.wav',Mock(),Mock())
        self.assertIs(engine.semantic_codec,before)
    def test_aliasing_speech_weights_cannot_be_retired(self):
        shared=object()
        class Module:
            def parameters(self):return iter([shared])
            def buffers(self):return iter([])
        RepCodec=type('RepCodec',(),{});CAMPPlus=type('CAMPPlus',(Module,),{})
        engine=self.engine();codec=RepCodec();codec.quantizer=Module();codec.encoder=Module();codec.decoder=Module();engine.semantic_codec=codec;engine.campplus_model=CAMPPlus();before=codec.encoder
        with self.assertRaisesRegex(ValueError,'alias'):retire(engine,'reference.wav',Mock(),Mock())
        self.assertIs(codec.encoder,before)
if __name__=='__main__':unittest.main()
