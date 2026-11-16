import sys
import unittest
from unittest.mock import patch
import run_compact_voice_batch as batch


class EarlyBatchGuards(unittest.TestCase):
    def arguments(self):
        return ['run_compact_voice_batch.py','--speaker','mother','--full-dialogue',
                '--memory-efficient','--low-memory-attempt','--require-reference-cache',
                '--stage-flow-model','--stage-vocoder','--cuda-async-allocator',
                '--early-reference-retirement','--early-reference-probe',
                '--state','other_mother_events_wybie_adventure_caught_normal_low_high_01']

    def test_component_requires_full_isolated_original_profile(self):
        original=self.arguments()
        cases=[list(a for a in original if a!='--require-reference-cache'),
               original+['--retire-reference-modules'],original+['--max-new-takes','1']]
        for arguments in cases:
            with self.subTest(arguments=arguments),patch.object(sys,'argv',arguments),patch.object(batch.subprocess,'Popen') as child:
                with self.assertRaises(RuntimeError):batch.main()
                child.assert_not_called()

    def test_valid_profile_retains_stronger_start_floors_without_loading_a_model(self):
        for low,floor in ((True,'3.25'),(False,'4.00')):
            arguments=self.arguments()
            if not low:arguments.remove('--low-memory-attempt')
            with self.subTest(low=low),patch.object(sys,'argv',arguments),patch.object(batch.monitor,'free_gib',return_value=0),patch.object(batch.subprocess,'Popen') as child:
                with self.assertRaisesRegex(RuntimeError,'below '+floor):batch.main()
                child.assert_not_called()


if __name__=='__main__':unittest.main()
