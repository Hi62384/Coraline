import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from audio_manifest_io import write_json_if_changed


class SharedManifestTests(unittest.TestCase):
    def test_unchanged_manifest_is_not_opened_for_writing(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'sounds.json'
            value={'existing_event':{'sounds':['existing/clip']}}
            write_json_if_changed(path,value)
            with patch('audio_manifest_io.tempfile.NamedTemporaryFile',side_effect=AssertionError('Unexpected rewrite')):
                write_json_if_changed(path,value)
            self.assertEqual(json.loads(path.read_text(encoding='utf-8')),value)

    def test_replacement_keeps_shared_entries_and_cleans_staging_file(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'sounds.json'
            value={'other_importer':12}
            write_json_if_changed(path,value)
            value['black_is_traditional_a']=25
            write_json_if_changed(path,value)
            self.assertEqual(json.loads(path.read_text(encoding='utf-8')),value)
            self.assertEqual(list(Path(folder).iterdir()),[path])

    def test_failed_replace_does_not_truncate_original(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'sounds.json'
            value={'other_importer':12}
            write_json_if_changed(path,value)
            original=path.read_bytes()
            with patch.object(Path,'replace',side_effect=OSError('File temporarily unavailable')):
                with self.assertRaises(OSError):write_json_if_changed(path,{'new_event':25})
            self.assertEqual(path.read_bytes(),original)
            self.assertEqual(list(Path(folder).iterdir()),[path])


if __name__=='__main__':unittest.main()
