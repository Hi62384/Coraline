import hashlib,json,unittest
from pathlib import Path
from original_story_audio_catalog import originals

class StoryOriginalCatalogTests(unittest.TestCase):
    def test_every_supplied_chapter_is_in_caption_catalog(self):
        root=Path(__file__).resolve().parents[1];tracks=originals(root);assets=root/'src/main/resources/assets/coraline'
        self.assertEqual(sum(not entry['music'] and not entry.get('derived') for entry in tracks.values()),198)
        self.assertEqual(sum(entry.get('derived',False) for entry in tracks.values()),1)
        sounds=json.loads((assets/'sounds.json').read_text());lengths=json.loads((assets/'visit_audio_lengths.json').read_text())
        for name,entry in tracks.items():
            self.assertIn(name,sounds);self.assertGreater(lengths[name],0,name)
            source=root/entry['source'];self.assertEqual(source.stat().st_size,entry['source_bytes'])
            with source.open('rb') as stream:self.assertEqual(hashlib.file_digest(stream,'sha256').hexdigest(),entry['source_sha256'],name)
        for name in ['black_is_traditional_a','cocoa_beetles_g','mirror_prison_a','first_escape_a','missing_parents_cat_e','parent_rescue_n','rescue_mantis_intro','finale_s','farewell_c','crawling_hand_i','cat_encounter_i']:self.assertFalse(tracks[name]['music'],name)
        self.assertTrue(tracks['theatre_entrance']['music'])
        for name in ('magic_garden_hop_on','magic_garden_mother_said','wybie_line_a','tea_visit_j'):
            self.assertFalse(tracks[name]['music'],name)

if __name__=='__main__':unittest.main()
