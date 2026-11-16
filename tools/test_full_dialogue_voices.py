import copy,json,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
import visit_event_audio_catalog as catalog
from prepare_full_dialogue_voices import prepare

class FullDialogueTests(unittest.TestCase):
    def test_only_exact_words_and_same_voice_are_shared(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'tools').mkdir();manifest={'output':'voicelines/othermother/events/example','prefix':'line','reference':'voicelines/othermother/Sample.wav','reference_sha256':'a'*64,
                'states':{key:{'lines':[words],'emotion':[0]*8,'temperature':.72,'direction':'test'} for key,words in [('first','Exact first line.'),('duplicate','Exact first line.'),('alternate','A different line.')]} }
            (root/'tools/example.json').write_text(json.dumps(manifest))
            with patch.object(catalog,'EVENT_MANIFESTS',['tools/example.json']):
                report=prepare(root);self.assertEqual(report['distinct_scripts'],2);self.assertEqual(report['exact_duplicate_aliases'],1)
                records=catalog.effective_recordings(root);self.assertEqual(records['other_mother_events_example_first_01']['source'],records['other_mother_events_example_duplicate_01']['source'])
                self.assertNotEqual(records['other_mother_events_example_first_01']['source'],records['other_mother_events_example_alternate_01']['source'])
                plan_path=root/'tools/full_event_voice_plan.json';plan=json.loads(plan_path.read_text());next(iter(plan['new_recordings'].values()))['text']='Replaced script.';plan_path.write_text(json.dumps(plan))
                with self.assertRaisesRegex(ValueError,'changed an authored script'):catalog.effective_recordings(root)
    def test_all_583_live_events_keep_their_authored_text(self):
        root=Path(__file__).resolve().parents[1];authored=catalog.authored_recordings(root);actual=catalog.effective_recordings(root)
        self.assertEqual(len(actual),583)
        for name,record in authored.items():self.assertEqual(actual[name]['text'],record['text'],name)

if __name__=='__main__':unittest.main()
