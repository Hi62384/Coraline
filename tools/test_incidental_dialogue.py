import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from incidental_dialogue_catalog import catalogue, build, recordings, MANIFESTS
import import_incidental_dialogue_audio as importer

ROOT=Path(__file__).resolve().parents[1]


class IncidentalDialogueTests(unittest.TestCase):
    def test_exact_authored_words_and_speakers(self):
        rows=catalogue()
        self.assertEqual(len(rows),78)
        self.assertEqual(sum(row['speaker']=='Other Mother' for row in rows.values()),44)
        for row in rows.values():
            files=list((ROOT/'src/main/java/com/coraline').rglob(row['owner']))
            self.assertEqual(len(files),1,row['owner'])
            self.assertIn(row['text'],files[0].read_text(encoding='utf-8'),row['text'])
        runtime=json.loads((ROOT/'src/main/resources/assets/coraline/incidental_dialogue.json').read_text())
        self.assertEqual({(r['speaker'],r['text']) for r in runtime},
                         {(r['speaker'],r['text']) for r in rows.values()})
        self.assertEqual(len({r['sound'] for r in runtime}),78)

    def test_generated_manifests_are_deterministic_and_separate(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'tools').mkdir();(root/'src/main/resources/assets/coraline').mkdir(parents=True)
            for name in ('other_mother_welcome.json','other_father_activity_intros.json'):
                (root/'tools'/name).write_bytes((ROOT/'tools'/name).read_bytes())
            self.assertEqual(build(root),{'mother':44,'father':34,'total':78})
            for name in MANIFESTS:
                self.assertEqual((root/name).read_bytes(),(ROOT/name).read_bytes())
            records=recordings(root)
            self.assertEqual(len(records),78)
            for name,record in records.items():
                self.assertEqual(name,record['profile']['sound_name'])
                self.assertIn('/incidental_dialogue_v1/',record['source'])
                self.assertTrue(record['resource'].startswith('visit/incidental/'))

    def test_all_quality_proofs_precede_any_export(self):
        fake={str(i):{'event':str(i)} for i in range(78)}
        with patch.object(importer,'recordings',return_value=fake), \
             patch.object(importer,'validate',side_effect=ValueError('unverified recording')), \
             patch.object(importer.subprocess,'run') as launch:
            with self.assertRaisesRegex(ValueError,'unverified recording'):importer.main()
            launch.assert_not_called()


if __name__=='__main__':unittest.main()
