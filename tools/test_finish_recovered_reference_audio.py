import json,sys,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
import finish_recovered_reference_audio as recovery
import finish_verified_staged_audio as base

class RecoveredAudioAdmission(unittest.TestCase):
    def test_failed_unmeasured_or_lower_floor_probe_cannot_start_a_worker(self):
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary);path=root/'build-character-cast/compact-voice/probe/report.json';path.parent.mkdir(parents=True)
            valid=dict(status='passed',exit_code=0,stage='reference_retirement_component',reference_only_retirement=True,stop_free_mib=384,aggregate_job_limit_mib=12288,memory_measurement_available=True,requested_cuda_allocator='cudaMallocAsync',minimum_start_free_gib=2.75)
            argv=['finish_recovered_reference_audio.py','--failed-controller',str(root/'unused.json'),'--equivalence-probe',str(path),'--successful-probe',str(root/'unused.json'),'--quality-report',str(root/'unused.json')]
            for changes in ({'status':'low_memory_cutoff'},{'memory_measurement_available':False},{'minimum_start_free_gib':2.5},{'requested_cuda_allocator':'native'}):
                path.write_text(json.dumps(dict(valid,**changes)))
                with self.subTest(changes=changes),patch.object(base,'ROOT',root),patch.object(recovery,'ROOT',root),patch.object(sys,'argv',argv),patch.object(recovery.subprocess,'run') as model:
                    with self.assertRaises(ValueError):recovery.main()
                    model.assert_not_called()
if __name__=='__main__':unittest.main()
