from pathlib import Path
import sys
import unittest
from unittest.mock import patch
import run_compact_voice_batch as batch
import run_scoped_quality_admission as admission


class ScopedQualityGuards(unittest.TestCase):
    def arguments(self):
        return ['run_compact_voice_batch.py','--speaker','mother','--incidental-dialogue',
                '--low-memory-attempt','--validate','--asr-turbo','--scope-quality-pages',
                '--quality-page-scope-probe','--state','mother_53b786200560bb87']

    def test_probe_cannot_reuse_old_results_and_full_validation_needs_admission(self):
        for arguments in (self.arguments()+['--resume-checks'],self.arguments()+['--existing-only'],
                          [a for a in self.arguments() if a!='--quality-page-scope-probe']):
            with self.subTest(arguments=arguments),patch.object(sys,'argv',arguments),patch.object(batch.subprocess,'Popen') as child:
                with self.assertRaises(RuntimeError):batch.main()
                child.assert_not_called()
        with patch.object(sys,'argv',self.arguments()),patch.object(batch.monitor,'free_gib',return_value=0),patch.object(batch.subprocess,'Popen') as child:
            with self.assertRaisesRegex(RuntimeError,'below 2.50'):batch.main()
            child.assert_not_called()

    def test_failed_component_cannot_unlock_full_quality(self):
        valid=dict(status='passed',exit_code=0,stage='quality_page_scope_component',
                   manifest='tools/incidental_mother_voice_manifest.json',quality_own_page_scope=True,
                   stop_free_mib=384,memory_measurement_available=True,minimum_start_free_gib=2.5)
        for changes in (dict(status='low_memory_cutoff'),dict(stage='validation'),dict(memory_measurement_available=False),dict(minimum_start_free_gib=1.5)):
            with self.subTest(changes=changes),patch.object(admission,'receipt',return_value=(Path('component'),dict(valid,**changes))),patch.object(admission,'verify') as proof:
                with self.assertRaises(ValueError):admission.admit(Path('component'))
                proof.assert_not_called()

    def test_quality_headroom_uses_measurement_without_reducing_minimum(self):
        self.assertEqual(admission.measured_floor(dict(start_free_gib=3.577106475830078,min_free_gib=3.14404296875)),2)
        self.assertEqual(admission.measured_floor(dict(start_free_gib=3.5,min_free_gib=1.5)),2.75)
        for n in (dict(start_free_gib=5,min_free_gib=.1),dict(start_free_gib=float('nan'),min_free_gib=0),dict(start_free_gib=1,min_free_gib=2)):
            with self.subTest(n=n),self.assertRaises(ValueError):admission.measured_floor(n)


if __name__=='__main__':unittest.main()
