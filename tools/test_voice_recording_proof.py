import hashlib,json,tempfile,unittest
from pathlib import Path
from voice_recording_proof import validate,legacy_recordings

class VoiceProofTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.directory=self.root/'voicelines/othermother/events/full_dialogue_v1'
        source=self.directory/'example/line_01.wav';source.parent.mkdir(parents=True);source.write_bytes(b'checked master')
        reference=self.root/'reference.wav';reference.write_bytes(b'unchanged reference');reference_sha=hashlib.sha256(reference.read_bytes()).hexdigest()
        self.record={'event':'example','resource':'compact_events/full_wording/mother/example','source':str(source.relative_to(self.root)),
                     'text':'Complete original words.','reference':'reference.wav','reference_sha256':reference_sha}
        self.digest=hashlib.sha256(source.read_bytes()).hexdigest()
        self.generated={'reference_sha256':reference_sha,'lines':{'example/line_01.wav':{'text':self.record['text'],'sha256':self.digest}}}
        self.checked={'reference_unchanged':True,'checks':[{'file':'example/line_01.wav','pcm_decode':'pass','needs_review':False,
            'speaker_review':False,'word_error_rate':0.,'reference_speaker_cosine':.8,'source_sha256':self.digest,
            'expected_text_sha256':hashlib.sha256(self.record['text'].encode()).hexdigest()}]}
        self.save()
    def save(self):
        (self.directory/'generation_report.json').write_text(json.dumps(self.generated))
        (self.directory/'validation_report.json').write_text(json.dumps(self.checked))
    def test_verified_take_and_legacy_distinction(self):
        self.assertTrue(validate(self.root,self.record)['validation_source_hash_present'])
        self.checked['checks'][0].pop('source_sha256');self.save()
        self.assertFalse(validate(self.root,self.record)['validation_source_hash_present'])
    def test_changed_master_is_rejected(self):
        (self.root/self.record['source']).write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError,'proof mismatch'):validate(self.root,self.record)
    def test_changed_validation_or_missing_quality_cannot_ship(self):
        self.checked['checks'][0]['source_sha256']='b'*64;self.save()
        with self.assertRaisesRegex(ValueError,'changed since'):validate(self.root,self.record)
        self.checked['checks'][0].pop('source_sha256');self.checked['checks'][0]['needs_review']=True;self.save()
        with self.assertRaisesRegex(ValueError,'unresolved'):validate(self.root,self.record)
    def test_reference_and_script_cannot_drift(self):
        self.generated['lines']['example/line_01.wav']['text']='Different words.';self.save()
        with self.assertRaisesRegex(ValueError,'proof mismatch'):validate(self.root,self.record)
    def test_legacy_manifest_custom_receipts_are_checked(self):
        tools=self.root/'tools';tools.mkdir()
        profile={'lines':[self.record['text']],'filename':'line_01.wav'}
        manifest={'states':{'example':profile},'reference':self.record['reference'],
            'reference_sha256':self.record['reference_sha256'],
            'output':str(self.directory.relative_to(self.root)),
            'generation_report':'activity_generation_report.json',
            'validation_report':'activity_validation_report.json'}
        (tools/'other_mother_activities.json').write_text(json.dumps(manifest))
        (self.directory/'activity_generation_report.json').write_text(json.dumps(self.generated))
        (self.directory/'activity_validation_report.json').write_text(json.dumps(self.checked))
        records=legacy_recordings(self.root)
        self.assertEqual(set(records),{'other_mother_welcome_example_01'})
        receipt=validate(self.root,next(iter(records.values())))
        self.assertTrue(receipt['generation_report'].endswith('activity_generation_report.json'))
        self.checked['checks'][0]['needs_review']=True
        (self.directory/'activity_validation_report.json').write_text(json.dumps(self.checked))
        with self.assertRaisesRegex(ValueError,'unresolved'):validate(self.root,next(iter(records.values())))
    def test_legacy_empty_state_and_default_welcome_output(self):
        tools=self.root/'tools';tools.mkdir()
        directory=self.root/'voicelines/othermother/welcome';directory.mkdir(parents=True)
        (directory/'welcome_01.wav').write_bytes((self.root/self.record['source']).read_bytes())
        manifest={'states':{'':{'lines':[self.record['text']]}},
            'reference':self.record['reference'],'reference_sha256':self.record['reference_sha256']}
        (tools/'other_mother_welcome.json').write_text(json.dumps(manifest))
        generation={'reference_sha256':self.record['reference_sha256'],
            'lines':{'welcome_01.wav':self.generated['lines']['example/line_01.wav']}}
        checked=dict(self.checked,checks=[dict(self.checked['checks'][0],file='welcome_01.wav')])
        (directory/'generation_report.json').write_text(json.dumps(generation))
        (directory/'validation_report.json').write_text(json.dumps(checked))
        records=legacy_recordings(self.root)
        self.assertEqual(set(records),{'other_mother_welcome__01'})
        self.assertTrue(validate(self.root,next(iter(records.values())))['validation_source_hash_present'])

if __name__=='__main__':unittest.main()
