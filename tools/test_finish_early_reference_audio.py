from pathlib import Path
import unittest
from unittest.mock import patch
import finish_early_reference_audio as finish


class EarlyFinalAdmission(unittest.TestCase):
    def test_failed_partial_or_unmeasured_take_cannot_admit_later_models(self):
        valid=dict(status='passed',exit_code=0,stage='synthesis',manifest=finish.MANIFESTS[2],
                   completed=34,max_new_takes=1,early_reference_retirement=True,
                   stop_free_mib=384,aggregate_job_limit_mib=12288,memory_measurement_available=True,
                   requested_cuda_allocator='cudaMallocAsync',minimum_start_free_gib=3.25)
        for changes in (dict(status='low_memory_cutoff'),dict(completed=33),dict(memory_measurement_available=False),
                        dict(minimum_start_free_gib=2.5),dict(stage='early_reference_component')):
            with self.subTest(changes=changes),patch.object(finish,'receipt',return_value=(Path('probe'),dict(valid,**changes))),patch.object(finish,'verify_early') as weights:
                with self.assertRaises(ValueError):finish.genuine(Path('probe'))
                weights.assert_not_called()


if __name__=='__main__':unittest.main()
