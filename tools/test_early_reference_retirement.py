from types import SimpleNamespace
import unittest
from unittest.mock import Mock
from early_reference_retirement import EarlyRetirement


class EarlyReferenceGuards(unittest.TestCase):
    def test_unverified_cache_cannot_retire_any_model(self):
        torch=Mock();trim=Mock();stage=EarlyRetirement(torch,trim,lambda:False)
        for method in (stage.retire_codec,stage.retire_camp):
            with self.assertRaisesRegex(ValueError,'Verified fixed-reference'):method(object())
        torch.cuda.empty_cache.assert_not_called();trim.assert_not_called()

    def test_retirement_after_vocoder_start_is_rejected(self):
        stage=EarlyRetirement(Mock(),Mock(),lambda:True);stage.vocoder_started=True
        with self.assertRaisesRegex(ValueError,'after vocoder'):stage.retire_codec(object())

    def test_aliasing_original_speech_weights_are_never_freed(self):
        shared=object()
        class Module:
            def parameters(self):return iter([shared])
            def buffers(self):return iter([])
        codec=type('RepCodec',(),{})();codec.quantizer=Module();codec.encoder=Module();codec.decoder=Module()
        original=codec.encoder;stage=EarlyRetirement(Mock(),Mock(),lambda:True)
        with self.assertRaisesRegex(ValueError,'alias speech'):stage.retire_codec(codec)
        self.assertIs(codec.encoder,original)


if __name__=='__main__':unittest.main()
