import json
from pathlib import Path
import tempfile
import unittest
from voice_validation_cache import load,reusable,save


class ValidationCacheTests(unittest.TestCase):
    def row(self):return {'file':'line.wav','source_sha256':'source','expected_text_sha256':'text',
        'pcm_decode':'pass','word_error_rate':0.0,'reference_speaker_cosine':.8,'speaker_review':False,'needs_review':False}

    def test_only_identical_source_text_and_scoring_context_resume(self):
        with tempfile.TemporaryDirectory() as temporary:
            path=Path(temporary)/'checkpoint.json';context={'reference':'reference','model':'pinned'}
            cached=load(path,context);save(path,cached,self.row())
            self.assertEqual(reusable(load(path,context),'line.wav','source','text'),self.row())
            self.assertIsNone(reusable(cached,'line.wav','changed','text'))
            self.assertIsNone(reusable(cached,'line.wav','source','changed'))
            with self.assertRaisesRegex(RuntimeError,'context changed'):load(path,dict(context,model='other'))
            self.assertEqual(json.loads(path.read_text())['checks']['line.wav'],self.row())

    def test_nan_incomplete_or_inconsistent_rows_are_not_accepted(self):
        for updates in ({'word_error_rate':float('nan')},{'pcm_decode':'failed'},
                        {'reference_speaker_cosine':.1},{'needs_review':True}):
            row=dict(self.row(),**updates)
            self.assertIsNone(reusable({'checks':{'line.wav':row}},'line.wav','source','text'))

    def test_flagged_checks_remain_flagged_not_forged_into_passes(self):
        row=dict(self.row(),word_error_rate=.4,needs_review=True)
        self.assertTrue(reusable({'checks':{'line.wav':row}},'line.wav','source','text')['needs_review'])


if __name__=='__main__':unittest.main()
