import io
import unittest
from unittest.mock import patch
from import_legacy_original_metadata import merged_lengths,checked_recordings


class LegacyMetadataTests(unittest.TestCase):
    def test_duration_merge_preserves_every_other_importers_entry(self):
        existing={'first_escape_a':500,'old':1}
        self.assertEqual(merged_lengths(existing,{'old':{'ticks':42},'wybie_line_a':{'ticks':600}}),
                         {'first_escape_a':500,'old':42,'wybie_line_a':600})
        self.assertEqual(existing,{'first_escape_a':500,'old':1})

    def test_runtime_decode_is_not_reached_after_changed_master(self):
        with patch('pathlib.Path.open',return_value=io.BytesIO(b'changed')),patch('import_legacy_original_metadata.ogg_measure') as decode:
            with self.assertRaisesRegex(RuntimeError,'master changed'):
                checked_recordings('.',{'track':{'source':'a.wav','source_sha256':'invalid'}})
            decode.assert_not_called()


if __name__=='__main__':unittest.main()
