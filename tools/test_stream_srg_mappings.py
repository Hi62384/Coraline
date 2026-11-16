import tempfile
import unittest
from pathlib import Path
from stream_srg_mappings import convert


class StreamingMappingsTests(unittest.TestCase):
    def test_descriptors_follow_class_renames_and_parameters_are_omitted(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'source.tsrg'
            source.write_text('tsrg2 left right\na/A b/B\n\tfield renamed\n'
                              '\tmethod ([La/A;I)La/A; mapped\n\t\tstatic\n'
                              '\t\t0 old parameter\n', encoding='utf-8')
            output = root / 'out.srg'
            convert(source, output)
            self.assertEqual(output.read_text().splitlines(), [
                'CL: a/A b/B', 'FD: a/A/field b/B/renamed',
                'MD: a/A/method ([La/A;I)La/A; b/B/mapped ([Lb/B;I)Lb/B;'])

    def test_invalid_member_does_not_replace_existing_output(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'source.tsrg'
            source.write_text('tsrg2 left right\na/A b/B\n\tinvalid\n', encoding='utf-8')
            output = root / 'out.srg'
            output.write_text('previous', encoding='utf-8')
            with self.assertRaises(ValueError):
                convert(source, output)
            self.assertEqual(output.read_text(), 'previous')
            self.assertFalse(output.with_suffix('.pending.srg').exists())


if __name__ == '__main__':
    unittest.main()
