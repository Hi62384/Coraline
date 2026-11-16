import json,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
from import_accepted_ghost_audio import verify,ROOT,EVENT
class AcceptedGhostAudioTests(unittest.TestCase):
    def test_exact_original_suffix_and_retained_reviewed_cues(self):
        receipt=verify(ROOT)
        self.assertEqual(receipt['offset_seconds'],11.6)
        self.assertEqual(receipt['transform'],'exact PCM suffix; no generated speech')
    def test_provenance_cannot_point_to_another_master(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);assets=root/'src/main/resources/assets/coraline';assets.mkdir(parents=True)
            receipt=json.loads((ROOT/'src/main/resources/assets/coraline/accepted_button_eyes_audio.json').read_text())[EVENT]
            receipt['derived_from']='voicelines/story/MirrorPrison/C.wav'
            (assets/'accepted_button_eyes_audio.json').write_text(json.dumps({EVENT:receipt}))
            with self.assertRaisesRegex(ValueError,'Unexpected accepted ghost source'):verify(root)
if __name__=='__main__':unittest.main()
