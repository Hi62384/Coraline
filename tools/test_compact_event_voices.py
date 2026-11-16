import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import compact_event_voices as compact
import visit_event_audio_catalog as catalog
REAL_MANIFESTS = list(catalog.EVENT_MANIFESTS)


class CompactVoiceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.filename = 'tools/example.json'
        self.patch = patch.object(catalog, 'EVENT_MANIFESTS', [self.filename])
        self.patch.start(); self.addCleanup(self.patch.stop)
        self.manifest = dict(output='voicelines/othermother/events/test', prefix='line',
            reference='voicelines/othermother/Sample.wav', reference_sha256='a'*64, states={})
        for state in ['invitation/low_low', 'invitation/high_low', 'invitation/low_high',
                      'invitation/high_high', 'caught/low_low', 'intro/cards', 'intro/tower']:
            self.manifest['states'][state] = dict(lines=[state+' first sentence.', state+' second sentence.'],
                emotion=[0]*8, direction='test', temperature=.72)
        compact.write_json(self.root/self.filename, self.manifest)

    def test_reduces_only_same_scene_and_tone(self):
        plan = compact.prepare(self.root)
        self.assertEqual(plan['missing_events_before'], 14)
        self.assertEqual(plan['new_unique_recordings'], 5)
        records = catalog.effective_recordings(self.root)
        self.assertEqual(len(records), 14)
        warm = records['other_mother_events_test_invitation_low_low_01']
        self.assertEqual(warm['source'], records['other_mother_events_test_invitation_high_low_02']['source'])
        self.assertNotEqual(warm['source'], records['other_mother_events_test_invitation_low_high_01']['source'])
        self.assertNotEqual(warm['source'], records['other_mother_events_test_caught_low_low_01']['source'])
        self.assertNotEqual(records['other_mother_events_test_intro_cards_01']['source'],
                            records['other_mother_events_test_intro_tower_01']['source'])

    def test_keeps_all_existing_recordings_and_source_bytes(self):
        originals = catalog.authored_recordings(self.root)
        for key in ['other_mother_events_test_invitation_low_low_01',
                    'other_mother_events_test_invitation_high_low_02']:
            path = self.root/originals[key]['source']
            path.parent.mkdir(parents=True, exist_ok=True); path.write_bytes(b'untouched master')
        compact.prepare(self.root)
        records = catalog.effective_recordings(self.root)
        for key, record in originals.items():
            path = self.root/record['source']
            if path.exists():
                self.assertEqual(records[key]['source'], record['source'])
                self.assertEqual(path.read_bytes(), b'untouched master')
        self.assertEqual(records['other_mother_events_test_invitation_high_low_01']['text'],
                         originals['other_mother_events_test_invitation_low_low_01']['text'])
        self.assertEqual(json.loads((self.root/self.filename).read_text()), self.manifest)

    def test_resume_does_not_silently_reselect_takes(self):
        compact.prepare(self.root)
        with self.assertRaises(RuntimeError):
            compact.prepare(self.root)

    def test_rejects_alias_cycles_and_traversal(self):
        plan = compact.prepare(self.root)
        source, target = next(iter(plan['aliases'].items()))
        plan['aliases'][target] = source
        compact.write_json(self.root/'tools/compact_event_voice_plan.json', plan)
        with self.assertRaises(ValueError):
            catalog.effective_recordings(self.root)
        del plan['aliases'][target]
        next(iter(plan['new_recordings'].values()))['source'] = 'voicelines/../escape.wav'
        compact.write_json(self.root/'tools/compact_event_voice_plan.json', plan)
        with self.assertRaises(ValueError):
            catalog.effective_recordings(self.root)

    def test_real_plan_has_stable_ids_and_no_cross_scene_aliases(self):
        # This fixture patches the manifest list; inspect the checked-in plan in
        # its real catalog independently, without loading any voice model.
        with patch.object(catalog, 'EVENT_MANIFESTS', REAL_MANIFESTS):
            originals = catalog.authored_recordings(compact.ROOT)
            effective = catalog.effective_recordings(compact.ROOT)
        self.assertEqual(set(originals), set(effective))
        plan = json.loads((compact.ROOT/'tools/compact_event_voice_plan.json').read_text())
        self.assertEqual(plan['missing_events_before'], 399)
        self.assertLessEqual(plan['new_unique_recordings'], 110)
        self.assertTrue(all(target not in plan['aliases'] for target in plan['aliases'].values()))
        for source, target in plan['aliases'].items():
            self.assertEqual(compact.group(originals[source]), compact.group(originals[target]))
            if not (compact.ROOT/'tools/full_event_voice_plan.json').exists():self.assertEqual(effective[source]['text'], originals[target]['text'])
        if (compact.ROOT/'tools/full_event_voice_plan.json').exists():
            self.assertTrue(all(effective[name]['text']==entry['text'] for name,entry in originals.items()),'Full release substituted different spoken dialogue')


if __name__ == '__main__':
    unittest.main()
