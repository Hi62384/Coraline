import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
import import_child_gibberish_audio as importer


class ChildAudioMerge(unittest.TestCase):
    def test_import_preserves_other_events_and_never_reencodes_verified_exports(self):
        with tempfile.TemporaryDirectory(prefix='child-audio-merge-',dir=importer.ROOT/'build-character-cast') as temporary:
            assets=Path(temporary);(assets/'lang').mkdir()
            original={'sounds.json':{'unrelated':{'sounds':['coraline:untouched']}},
                      'visit_audio_lengths.json':{'unrelated':234},
                      'scene_audio_lengths.json':{'unrelated':234},
                      'lang/en_us.json':{'unrelated':'Keep this label'}}
            for name,value in original.items():(assets/name).write_text(json.dumps(value),encoding='utf-8')
            with patch.object(sys,'argv',['import_child_gibberish_audio.py','--assets-root',str(assets)]):
                importer.main()
                before={p.name:p.read_bytes() for p in (assets/'sounds/ghost_victim').glob('*.ogg')}
                with patch.object(importer.subprocess,'run',side_effect=AssertionError('Verified original export was re-encoded')):
                    importer.main()
            self.assertEqual(len(before),4)
            self.assertEqual(before,{p.name:p.read_bytes() for p in (assets/'sounds/ghost_victim').glob('*.ogg')})
            for name,value in original.items():
                after=json.loads((assets/name).read_text(encoding='utf-8'))
                for key,entry in value.items():self.assertEqual(after[key],entry)
            sounds=json.loads((assets/'sounds.json').read_text(encoding='utf-8'))
            self.assertEqual(len(sounds[importer.EVENT]['sounds']),4)
            self.assertTrue(all(not sound['stream'] for sound in sounds[importer.EVENT]['sounds']))


if __name__=='__main__':unittest.main()
