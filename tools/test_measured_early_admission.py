import sys
import unittest
from pathlib import Path
from unittest.mock import patch
import run_compact_voice_batch as batch
from run_early_reference_genuine_probe import measured_floor


class MeasuredEarlyAdmission(unittest.TestCase):
    def test_measured_peak_gets_one_gib_margin_without_lowering_profile_minimum(self):
        self.assertEqual(measured_floor(dict(start_free_gib=3.258758544921875,min_free_gib=2.205810546875)),2.75)
        self.assertEqual(measured_floor(dict(start_free_gib=4,min_free_gib=2)),3)
        self.assertEqual(measured_floor(dict(start_free_gib=4,min_free_gib=1)),4)
        for n in (dict(start_free_gib=5,min_free_gib=.5),dict(start_free_gib=float('nan'),min_free_gib=1),dict(start_free_gib=2,min_free_gib=3)):
            with self.subTest(n=n),self.assertRaises(ValueError):measured_floor(n)

    def test_genuine_profile_needs_actual_component_and_preserves_normal_four_gib_gate(self):
        arguments=['run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue','--memory-efficient',
                   '--require-reference-cache','--stage-flow-model','--stage-vocoder','--cuda-async-allocator',
                   '--early-reference-retirement','--max-new-takes','1']
        with patch.object(sys,'argv',arguments),patch.object(batch.subprocess,'Popen') as child:
            with self.assertRaisesRegex(RuntimeError,'actual exact-PCM'):batch.main()
            child.assert_not_called()
        component=dict(start_free_gib=3.258758544921875,min_free_gib=2.205810546875)
        for low,floor in ((True,'2.75'),(False,'4.00')):
            argv=arguments+['--early-reference-admission','component.json']+(['--low-memory-attempt'] if low else [])
            with self.subTest(low=low),patch.object(sys,'argv',argv),patch('run_early_reference_genuine_probe.admit',return_value=(Path('component.json'),component)),patch.object(batch.monitor,'free_gib',return_value=0),patch.object(batch.subprocess,'Popen') as child:
                with self.assertRaisesRegex(RuntimeError,'below '+floor):batch.main()
                child.assert_not_called()


if __name__=='__main__':unittest.main()
