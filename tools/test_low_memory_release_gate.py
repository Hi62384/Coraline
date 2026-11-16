import unittest
from unittest.mock import patch
from run_low_memory_release import ready_memory,compact_memory_probe
from unittest.mock import Mock
import check_numbered_candidate as monitor

class BuildMemoryGateTests(unittest.TestCase):
    def test_compact_probe_never_masks_emergency_measurement(self):
        child=Mock();child.poll.return_value=None
        with patch('run_low_memory_release.monitor.free_gib',return_value=.35),patch('run_low_memory_release.trim_owned_working_set') as trim:
            report={};self.assertEqual(compact_memory_probe(child,report)(),.35);trim.assert_not_called()
            self.assertEqual(report['minimum_free_before_trim_gib'],.35)
    def test_compact_probe_trims_only_its_owned_child_and_remeasures(self):
        child=Mock();child.poll.return_value=None;report={}
        with patch('run_low_memory_release.monitor.free_gib',side_effect=[.6,.8,.6]),patch('run_low_memory_release.monitor.process_memory',return_value={'working_set_bytes':200*1024**2}),patch('run_low_memory_release.time.monotonic',return_value=10),patch('run_low_memory_release.trim_owned_working_set') as trim:
            probe=compact_memory_probe(child,report);self.assertEqual(probe(),.8);self.assertEqual(probe(),.6)
            trim.assert_called_once_with(child);self.assertEqual(report['owned_working_set_trims'],1)
    def test_higher_floor_does_not_relax_existing_guards(self):
        before=(monitor.START_FREE_GIB,monitor.STOP_FREE_GIB)
        with patch('run_low_memory_release.monitor.free_gib',return_value=1):
            with self.assertRaisesRegex(RuntimeError,'no build child'):ready_memory(1536)
        self.assertEqual(before,(monitor.START_FREE_GIB,monitor.STOP_FREE_GIB))
    def test_sufficient_memory_returns_without_waiting(self):
        with patch('run_low_memory_release.monitor.free_gib',return_value=2.1),patch('run_low_memory_release.time.sleep') as sleep:
            self.assertEqual(ready_memory(2048,45),2.1);sleep.assert_not_called()
    def test_one_gib_floor_rejects_old_minimum_and_accepts_headroom(self):
        before=(monitor.START_FREE_GIB,monitor.STOP_FREE_GIB)
        with patch('run_low_memory_release.monitor.free_gib',return_value=.9):
            with self.assertRaisesRegex(RuntimeError,'no build child'):ready_memory(1024)
        with patch('run_low_memory_release.monitor.free_gib',return_value=1.2):
            self.assertEqual(ready_memory(1024),1.2)
        self.assertEqual(before,(monitor.START_FREE_GIB,monitor.STOP_FREE_GIB))
    def test_bounded_wait_rechecks_actual_memory(self):
        with patch('run_low_memory_release.monitor.free_gib',side_effect=[1,1.6]),patch('run_low_memory_release.time.monotonic',return_value=0),patch('run_low_memory_release.time.sleep') as sleep:
            self.assertEqual(ready_memory(1536,30),1.6);sleep.assert_called_once_with(1)
    def test_timeout_and_unsupported_floors_fail_closed(self):
        with patch('run_low_memory_release.monitor.free_gib',return_value=1),patch('run_low_memory_release.time.monotonic',side_effect=[0,31]):
            with self.assertRaisesRegex(RuntimeError,'no build child'):ready_memory(1536,30)
        for floor,wait in ((384,0),(640,600)):
            with self.subTest(floor=floor,wait=wait),self.assertRaises(RuntimeError):ready_memory(floor,wait)

if __name__=='__main__':unittest.main()
