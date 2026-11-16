import hashlib
from pathlib import Path
import tempfile
import unittest

from verify_event_revision_release import size_comparison


class ReleaseSizeBaselineTests(unittest.TestCase):
    def test_exact_baseline_and_difference(self):
        with tempfile.TemporaryDirectory() as folder:
            previous = Path(folder) / 'previous.jar'
            current = Path(folder) / 'current.jar'
            previous.write_bytes(b'previous')
            current.write_bytes(b'current-release')
            pin = hashlib.sha256(previous.read_bytes()).hexdigest()
            result = size_comparison(current, previous, pin)
            self.assertEqual(result['previous_bytes'], 8)
            self.assertEqual(result['bytes'], 15)
            self.assertEqual(result['change_bytes'], 7)
            self.assertEqual(result['change_percent'], 87.5)
            self.assertEqual(result['previous_sha256'], pin)
            previous.write_bytes(b'changed!')
            with self.assertRaisesRegex(ValueError, 'checksum changed'):
                size_comparison(current, previous, pin)

    def test_same_file_and_empty_baseline_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            previous = Path(folder) / 'previous.jar'
            current = Path(folder) / 'current.jar'
            previous.write_bytes(b'')
            current.write_bytes(b'current')
            pin = hashlib.sha256(b'').hexdigest()
            with self.assertRaisesRegex(ValueError, 'different files'):
                size_comparison(previous, previous, pin)
            with self.assertRaisesRegex(ValueError, 'empty'):
                size_comparison(current, previous, pin)


if __name__ == '__main__':
    unittest.main()
