import json
from pathlib import Path
import tempfile
import unittest
from verify_prepared_resources import verify


class PreparedResourceTests(unittest.TestCase):
    def fixture(self, base):
        resources = base / 'resources'
        for name in ('coraline.mixins.json', 'coraline.party.mixins.json',
                     'coraline.refmap.json', 'pack.mcmeta', 'assets/coraline/sounds.json',
                     'assets/coraline/visit_audio_lengths.json', 'assets/coraline/visit_voice_envelopes.json',
                     'META-INF/mods.toml'):
            path = resources / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('{}' if name.endswith(('.json', '.mcmeta')) else 'modId="fixture"')
        report = base / 'asset-size.json'
        report.write_text(json.dumps({'after_bytes': sum(p.stat().st_size for p in resources.rglob('*') if p.is_file())}))
        return resources, report

    def test_complete_optimized_cache_is_read_only(self):
        with tempfile.TemporaryDirectory() as folder:
            resources, report = self.fixture(Path(folder))
            before = {p: p.read_bytes() for p in resources.rglob('*') if p.is_file()}
            self.assertEqual(len(verify(resources, report)['critical_resource_sha256']), 8)
            self.assertEqual(before, {p: p.read_bytes() for p in before})

    def test_old_report_does_not_authorize_missing_mixin_config(self):
        with tempfile.TemporaryDirectory() as folder:
            resources, report = self.fixture(Path(folder))
            (resources / 'coraline.mixins.json').unlink()
            with self.assertRaisesRegex(RuntimeError, 'coraline.mixins.json'):
                verify(resources, report)

    def test_interrupted_or_added_output_does_not_match_receipt(self):
        with tempfile.TemporaryDirectory() as folder:
            resources, report = self.fixture(Path(folder))
            extra = resources / 'partial.ogg'
            extra.write_bytes(b'partial')
            with self.assertRaisesRegex(RuntimeError, 'optimizer receipt'):
                verify(resources, report)

    def test_invalid_receipt_or_json_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            resources, report = self.fixture(Path(folder))
            report.write_text('{"after_bytes": true}')
            with self.assertRaisesRegex(RuntimeError, 'byte-count'):
                verify(resources, report)
            resources, report = self.fixture(Path(folder))
            (resources / 'coraline.mixins.json').write_text('[]')
            with self.assertRaisesRegex(RuntimeError, 'Malformed'):
                verify(resources, report)


if __name__ == '__main__':
    unittest.main()
