import copy
from datetime import datetime
from pathlib import Path
import unittest
from unittest.mock import patch

import finish_positive_audio_checkpoint as finish


class PositiveCheckpointAdmission(unittest.TestCase):
    def test_changed_profile_does_not_reset_the_original_final_window(self):
        prior=dict(completion_window_started_utc='2026-10-28T21:34:49Z',completion_window_seconds=21600)
        after=datetime.fromisoformat('2026-10-29T03:34:50+00:00').timestamp()
        with patch.object(finish.time,'time',return_value=after),patch.object(finish.subprocess,'run') as model:
            with self.assertRaisesRegex(TimeoutError,'Existing finite completion window'):
                finish.run_final(Path('old'),prior,10,3.25,generation_profile='early',preserve_window=True,component_admission=Path('component'))
            model.assert_not_called()

    def checkpoint(self):
        return dict(status='failed',
                    error='Preserved completion deadline does not permit another worker',
                    historical_speech_ram_stops=16, worker_max_new_takes=1,
                    retry_failed_models=False, recovery_from_speech_cutoff={'report': 'old'},
                    completion_window_started_utc='2026-10-28T03:34:12Z',
                    completion_window_seconds=64800, anchors={'original': 'pin'},
                    manifest_sha256={name: 'pin' for name in finish.MANIFESTS},
                    required_start_free_gib=3.125,
                    steps=[dict(report='worker', sha256='pin', status='passed', exit_code=0)])

    def attempt(self, prior, *, now=1793223253, failed_worker=False):
        worker = dict(status='low_memory_cutoff' if failed_worker else 'passed',
                      exit_code=0, stop_free_mib=384, memory_measurement_available=True,
                      stage='synthesis', max_new_takes=1)
        def receipt(path, kind):
            return Path(path), prior if kind == 'verified-staged-audio' else worker
        with patch.object(finish, 'receipt', side_effect=receipt), \
                patch.object(finish, 'pin', return_value='pin'), \
                patch.object(finish, 'checked'), \
                patch.object(Path, 'read_text', return_value='{}'), \
                patch.object(finish, 'generation_state', return_value={'expected': 10, 'completed': 8}):
            return finish.admit(Path('checkpoint'), now=now)

    def test_only_successful_expired_bounded_checkpoint_is_admitted(self):
        admitted = self.attempt(self.checkpoint())
        self.assertEqual(admitted[2:], (6, 3.125))
        cases = [dict(status='running'), dict(error='Worker failed; no automatic retry'),
                 dict(historical_speech_ram_stops=17), dict(worker_max_new_takes=5),
                 dict(retry_failed_models=True), dict(required_start_free_gib=2.75),
                 dict(positive_checkpoint_from={'report': 'another-final-window'})]
        for changes in cases:
            prior = copy.deepcopy(self.checkpoint())
            prior.update(changes)
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                self.attempt(prior)
        with self.assertRaises(ValueError):
            self.attempt(self.checkpoint(), now=0)
        with self.assertRaises(ValueError):
            self.attempt(self.checkpoint(), failed_worker=True)


if __name__ == '__main__':
    unittest.main()
