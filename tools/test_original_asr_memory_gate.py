import ast
from pathlib import Path
import types
import unittest


class OriginalASRMemoryGateTests(unittest.TestCase):
    def test_voice_quality_uses_same_bounded_import_and_copy_loading(self):
        tools=Path(__file__).parent
        source=(tools/'check_welcome_audio.py').read_text()
        self.assertIn('with release_own_loading_pages() if args.memory_efficient_loading',source)
        self.assertIn('memory_efficient=args.memory_efficient_loading',source)
        self.assertIn("if args.asr_turbo and args.low_memory_attempt:command.append('--memory-efficient-loading')",(tools/'run_compact_voice_batch.py').read_text())
        self.assertIn('install_state_copy_trimming(model,torch)',(tools/'local_turbo_asr.py').read_text())
    def test_asr_uses_the_established_environment_not_system_python(self):
        source=(Path(__file__).parent/'run_original_transcription.py').read_text()
        self.assertIn("voice_python=root/'.voice-tools/index-tts/.venv/Scripts/python.exe'",source)
        self.assertIn('command=[str(voice_python)',source)
        self.assertNotIn('command=[sys.executable',source)
    def floor(self):
        source=ast.parse((Path(__file__).parent/'run_original_transcription.py').read_text())
        method=next(n for n in source.body if isinstance(n,ast.FunctionDef) and n.name=='start_floor')
        scope={'monitor':types.SimpleNamespace(START_FREE_GIB=640/1024)}
        exec(compile(ast.Module(body=[method],type_ignores=[]),'asr_gate','exec'),scope)
        return scope['start_floor']

    def test_default_turbo_floor_is_unchanged(self):self.assertEqual(self.floor()(True,False,2048),4)
    def test_approved_voice_mode_has_same640mib_start_floor(self):self.assertEqual(self.floor()(True,True,2048),640/1024)
    def test_other_asr_modes_cannot_silently_inherit_the_exception(self):
        with self.assertRaises(RuntimeError):self.floor()(False,True,2048)
        self.assertEqual(self.floor()(False,False,2048),2)


if __name__=='__main__':unittest.main()
