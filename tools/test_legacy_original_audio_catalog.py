import unittest
from legacy_original_audio_catalog import sources


class LegacyOriginalInventoryTests(unittest.TestCase):
    def test_all_earlier_speech_and_both_garden_lines_are_explicit(self):
        tracks=sources()
        self.assertEqual(len(tracks),49)
        self.assertEqual(tracks['magic_garden_hop_on'][1],'Other Father')
        self.assertEqual(tracks['magic_garden_mother_said'][1],'Other Father')
        self.assertEqual(sum(name.startswith('wybie_visit_') for name in tracks),12)
        self.assertEqual(sum(name.startswith('bobinsky_visit_') for name in tracks),11)
        self.assertEqual(sum(name.startswith('tea_visit_') for name in tracks),10)

    def test_music_normal_parents_and_reference_samples_are_excluded(self):
        tracks=sources()
        for name in ('other_father_song','mom_chat','dad_chat','wybie_visit_music','ambient_garden'):
            self.assertNotIn(name,tracks)
        for source,speaker in tracks.values():
            self.assertTrue(source.startswith('voicelines/'))
            self.assertNotIn('Sample',source)


if __name__=='__main__':unittest.main()
