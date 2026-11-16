"""Complete supplied music, timing and source-master integrity checks."""
import hashlib,json,unittest
from pathlib import Path
from import_numbered_original_audio import ogg_measure
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/coraline'
class RequestedSceneMusic(unittest.TestCase):
    def test_all_requested_recordings_have_full_export_and_merged_timing(self):
        sounds=json.loads((ASSETS/'sounds.json').read_text())
        tables=[json.loads((ASSETS/name).read_text()) for name in ('visit_audio_lengths.json','scene_audio_lengths.json')]
        for event in ('accepted_little_me_music','the_cat_music','first_escape_music','missing_parents_music','parents_rescue_arrival_music','other_mother_game_proposal_music','spink_and_forcible_boss_music'):
            with self.subTest(event=event):
                receipt=json.loads((ASSETS/(event+'_audio.json')).read_text())
                source=ROOT/receipt['source'];audio=ASSETS/'sounds/scenes'/(event+'.ogg')
                self.assertEqual(receipt['source_sha256'],hashlib.sha256(source.read_bytes()).hexdigest())
                self.assertEqual(receipt['ogg_sha256'],hashlib.sha256(audio.read_bytes()).hexdigest())
                measured=ogg_measure(audio.read_bytes());self.assertEqual(measured,receipt['export'])
                self.assertLessEqual(measured['sample_rate'],48000)
                self.assertGreater(measured['seconds'],5)
                self.assertEqual(sounds[event]['sounds'],[{'name':'coraline:scenes/'+event,'stream':True}])
                for table in tables:self.assertEqual(table[event],measured['ticks'])
        # Other importers' existing dialogue entries must survive these imports.
        for event in ('parent_rescue_n','cat_encounter_i','missing_parents_cat_f'):self.assertGreater(tables[0][event],0)
    def test_missing_parents_cue_is_elapsed_time_metadata(self):
        receipt=json.loads((ASSETS/'missing_parents_music_audio.json').read_text())
        self.assertEqual(receipt['spawn_after_start_ticks'],900)
        self.assertNotIn('spawn_lead_ticks',receipt)
if __name__=='__main__':unittest.main()
