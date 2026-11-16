import hashlib,json,tempfile,unittest
from pathlib import Path
from voice_batch_scope import generation_state,effective_take_limit


class VoiceBatchScopeTests(unittest.TestCase):
    def test_only_explicit_bounded_workers_retire_early_without_changing_the_full_plan(self):
        self.assertIsNone(effective_take_limit(None,True))
        self.assertEqual(effective_take_limit(10,True),5)
        self.assertEqual(effective_take_limit(20,True),5)
        self.assertEqual(effective_take_limit(1,True),1)
        self.assertEqual(effective_take_limit(10,False),10)
        with self.assertRaises(ValueError):effective_take_limit(0,True)

    def test_partial_receipts_are_not_full_coverage_and_changed_files_fail(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);output=root/'voices';output.mkdir()
            manifest={'output':'voices','prefix':'line','reference_sha256':'a'*64,
                      'states':{'first':{'lines':['First.']},'second':{'lines':['Second.']}}}
            self.assertEqual(generation_state(root,manifest)['completed'],0)
            file=output/'first/line_01.wav';file.parent.mkdir();file.write_bytes(b'first PCM fixture')
            receipt={'reference_sha256':'a'*64,'lines':{'first/line_01.wav':{'text':'First.','sha256':hashlib.sha256(file.read_bytes()).hexdigest()}}}
            (output/'generation_report.json').write_text(json.dumps(receipt))
            self.assertEqual(generation_state(root,manifest),{'expected':2,'completed':1,'remaining':1,'generation_complete':False})
            file.write_bytes(b'changed')
            with self.assertRaisesRegex(ValueError,'changed master'):generation_state(root,manifest)

    def test_orphan_missing_receipt_and_escape_fail_closed(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);(root/'voices/first').mkdir(parents=True)
            manifest={'output':'voices','prefix':'line','reference_sha256':'a'*64,'states':{'first':{'lines':['First.']}}}
            file=root/'voices/first/line_01.wav';file.write_bytes(b'not receipted')
            with self.assertRaisesRegex(ValueError,'Unreceipted'):generation_state(root,manifest)
            manifest['states']['first']['source_directory']='../../outside'
            with self.assertRaisesRegex(ValueError,'Invalid'):generation_state(root,manifest)


if __name__=='__main__':unittest.main()
