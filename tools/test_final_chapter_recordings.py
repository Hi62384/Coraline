"""Manifest merge regressions. No converter, audio editing or game process."""
import copy
import unittest
from import_final_chapter_recordings import merge_tables, inspect_names, expected_event, resource


class FinalOriginalTests(unittest.TestCase):
    def outputs(self):
        return {name: {'ticks': 100 + index} for index, name in enumerate(sorted(inspect_names()))}

    def test_distinct_complete_names(self):
        names = inspect_names()
        self.assertEqual(24, len(names))
        self.assertEqual(14, sum(n.startswith('theatre_opening_') for n in names))
        self.assertEqual(9, sum(n.startswith('crawling_hand_') for n in names))
        self.assertTrue(names.isdisjoint({'theatre_music', *('theatre_' + c for c in 'abcdef')}))

    def test_merge_preserves_every_prior_entry_and_does_not_mutate_inputs(self):
        sounds = {'old': {'subtitle': 'old.caption', 'sounds': [{'name': 'coraline:prior', 'weight': 3, 'pitch': .9}]},
                  'alias': {'sounds': [{'name': 'old', 'type': 'event'}]}}
        lengths = {'old': 701, 'alias': 701, 'unrelated_intro': 290}
        before_sounds, before_lengths = copy.deepcopy(sounds), copy.deepcopy(lengths)
        merged_sounds, merged_lengths = merge_tables(sounds, lengths, self.outputs())
        for name in sounds:
            self.assertEqual(sounds[name], merged_sounds[name])
        for name in lengths:
            self.assertEqual(lengths[name], merged_lengths[name])
        self.assertEqual(before_sounds, sounds)
        self.assertEqual(before_lengths, lengths)
        for name in inspect_names():
            self.assertEqual(expected_event(name), merged_sounds[name])
            self.assertGreater(merged_lengths[name], 0)
            self.assertNotIn('subtitle', merged_sounds[name])

    def test_idempotent_merge(self):
        first = merge_tables({'prior': {'sounds': ['coraline:old']}}, {'prior': 300}, self.outputs())
        self.assertEqual(first, merge_tables(*first, self.outputs()))

    def test_existing_settings_conflict_is_not_overwritten(self):
        for name in inspect_names():
            for wrong in ({'sounds': ['coraline:different']}, {'subtitle': 'do.not.touch', **expected_event(name)}):
                with self.assertRaises(ValueError):
                    merge_tables({name: wrong}, {}, self.outputs())

    def test_missing_extra_or_invalid_duration_rejected(self):
        outputs = self.outputs()
        for name in list(outputs):
            missing = dict(outputs); missing.pop(name)
            with self.assertRaises(ValueError):
                merge_tables({}, {}, missing)
            for ticks in (0, -1, True, 12.5, '100'):
                invalid = dict(outputs); invalid[name] = {'ticks': ticks}
                with self.assertRaises(ValueError):
                    merge_tables({}, {}, invalid)
        with self.assertRaises(ValueError):
            merge_tables({}, {}, {**outputs, 'theatre_a': {'ticks': 100}})

    def test_resource_namespace_is_closed(self):
        for wrong in ('../escape', 'theatre_a', 'theatre_music', 'crawling_hand_j'):
            with self.assertRaises(ValueError):
                resource(wrong)
        self.assertEqual(24, len({resource(name) for name in inspect_names()}))


if __name__ == '__main__':
    unittest.main()
