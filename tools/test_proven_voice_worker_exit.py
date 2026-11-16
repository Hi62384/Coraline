import json
from contextlib import nullcontext
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch,Mock
import proven_voice_worker_exit as worker


class CompletedExit(BaseException):pass


class WorkerExitTests(unittest.TestCase):
    def test_failed_model_never_gets_success_receipt_or_exit(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'tools').mkdir();source=root/'tools/generate_early_reference_voice.py';source.write_text('source')
            callback=Mock(side_effect=ValueError('model failed'))
            with patch.object(worker,'ROOT',root),patch.object(worker.os,'name','nt'),patch.object(worker,'release_own_loading_pages',return_value=nullcontext()),patch.object(worker.os,'_exit') as terminate:
                with self.assertRaisesRegex(ValueError,'model failed'):worker.run(callback,source)
                terminate.assert_not_called()
            self.assertFalse((root/'build-character-cast/worker-exit-proofs').exists())

    def test_success_flushes_durable_receipt_before_owned_exit(self):
        # Keep the actual helper/module files in the source pin set.
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'tools').mkdir();source=root/'tools/check_early_reference_voice.py';source.write_text('source')
            (root/'tools/low_memory_voice_loader.py').write_text('loader')
            original_module=worker.__file__;module=root/'tools/proven_voice_worker_exit.py';module.write_text('wrapper')
            callback=Mock()
            with patch.object(worker,'ROOT',root),patch.object(worker,'__file__',str(module)),patch.object(worker.os,'name','nt'),patch.object(worker,'release_own_loading_pages',return_value=nullcontext()),patch.object(worker.os,'_exit',side_effect=CompletedExit) as terminate:
                with self.assertRaises(CompletedExit):worker.run(callback,source)
                callback.assert_called_once_with();terminate.assert_called_once_with(0)
            receipts=list((root/'build-character-cast/worker-exit-proofs').glob('*.json'));self.assertEqual(len(receipts),1)
            result,_=worker.verify(root,receipts[0]);self.assertTrue(result['callback_completed'])
            source.write_text('changed')
            with self.assertRaisesRegex(ValueError,'sources changed'):worker.verify(root,receipts[0])

    def test_unknown_worker_is_rejected_before_callback(self):
        callback=Mock()
        with self.assertRaises(RuntimeError):worker.run(callback,__file__)
        callback.assert_not_called()


if __name__=='__main__':unittest.main()
