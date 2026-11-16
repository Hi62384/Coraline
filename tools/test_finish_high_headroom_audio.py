from pathlib import Path
import unittest
from unittest.mock import patch

import finish_high_headroom_audio as finish


class HighHeadroomAdmission(unittest.TestCase):
    def test_failed_probe_lower_floor_and_unmeasured_margin_cannot_continue(self):
        prior = dict(status='failed', error='Worker failed; no retry or later model permitted: mother_incidental_generate_8',
                     historical_speech_ram_stops=16, retry_failed_models=False,
                     positive_checkpoint_from=dict(report='old', sha256='pin'),
                     steps=[dict(report='failed', sha256='pin', status='low_memory_cutoff', exit_code=1)])
        stopped = dict(status='low_memory_cutoff', stop_free_mib=384,
                       memory_measurement_available=True, own_child_stopped=True,
                       stage='synthesis', manifest=finish.MANIFESTS[2])
        base = dict(manifest=finish.MANIFESTS[2], completed=34, max_new_takes=1,
                    minimum_start_free_gib=4, start_free_gib=4.1, min_free_gib=1.1)
        def receipt(path, kind):
            return Path(path), {'anchors': {}} if str(path) == 'old' else stopped if str(path) == 'failed' else prior
        for changes in (dict(minimum_start_free_gib=2.75), dict(start_free_gib=3.9),
                        dict(min_free_gib=.5), dict(max_new_takes=5), dict(completed=33)):
            with self.subTest(changes=changes), patch.object(finish, 'receipt', side_effect=receipt), \
                    patch.object(finish, 'pin', return_value='pin'), \
                    patch.object(finish, 'checked', return_value=(Path('probe'), dict(base, **changes))), \
                    self.assertRaises(ValueError):
                finish.admit(Path('checkpoint'), Path('probe'))
        with patch.object(finish, 'receipt', side_effect=receipt), \
                patch.object(finish, 'pin', return_value='pin'), \
                patch.object(finish, 'checked', side_effect=ValueError('failed actual probe')), \
                self.assertRaises(ValueError):
            finish.admit(Path('checkpoint'), Path('probe'))


if __name__ == '__main__':
    unittest.main()
