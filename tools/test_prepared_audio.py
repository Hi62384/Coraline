"""Synthetic byte fixtures, NOT valid recordings or native audio acceptance."""
import hashlib,json,tempfile,unittest,zipfile
from pathlib import Path
from unittest.mock import patch
from verify_prepared_audio import verify,digest

class PreparedAudioTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)
        self.source=self.root/'src/main/resources/assets/coraline';(self.source/'sounds').mkdir(parents=True)
        self.jar=self.root/'build/libs/audited.jar';self.jar.parent.mkdir(parents=True)
        (self.root/'build/reports').mkdir();(self.root/'build/reports/asset-size.json').write_text(json.dumps({'resampled_sounds':[],'duplicate_sounds':[]}))
        self.sounds={f'event_{i}':{'sounds':[{'name':f'coraline:voice_{i}','stream':True}]} for i in range(100)}
        (self.source/'sounds.json').write_text(json.dumps(self.sounds))
        receipts={}
        for i in range(29):
            name=f'fixture_master_{i}.bin';(self.root/name).write_bytes(b'test master bytes')
            receipts[str(i)]={'source':name,'source_sha256':digest(self.root/name)}
        (self.source/'fixture_audio.json').write_text(json.dumps(receipts))
        with zipfile.ZipFile(self.jar,'w') as archive:
            archive.writestr('assets/coraline/sounds.json',json.dumps(self.sounds))
            for name in ('story_subtitles.json','numbered_event_dialogue.json'):
                (self.source/name).write_bytes(b'{}\n');archive.writestr('assets/coraline/'+name,b'{}\n')
            for i in range(100):
                data=f'synthetic bytes {i}'.encode();(self.source/'sounds'/f'voice_{i}.ogg').write_bytes(data)
                archive.writestr(f'assets/coraline/sounds/voice_{i}.ogg',data)
        self.pin=digest(self.jar)
    def tearDown(self):self.temp.cleanup()
    def test_identical_prepared_inputs_keep_normal_optimizer_required(self):
        result=verify(self.root,self.jar,self.pin)
        self.assertEqual(result['byte_identical_referenced_recordings'],100)
        self.assertEqual(result['verified_source_master_receipts'],29)
        self.assertTrue(result['normal_processResources_still_required'])
    def test_wrong_archive_pin_fails_closed(self):
        with self.assertRaisesRegex(RuntimeError,'pin mismatch'):verify(self.root,self.jar,'0'*64)
    def test_caption_deferral_preserves_existing_bytes(self):
        result=verify(self.root,self.jar,self.pin,preserve_captions=True)
        self.assertTrue(result['existing_captions_preserved'])
    def test_caption_deferral_rejects_new_text(self):
        (self.source/'story_subtitles.json').write_text('{"new":"text"}')
        with self.assertRaisesRegex(RuntimeError,'caption data changed'):verify(self.root,self.jar,self.pin,preserve_captions=True)
    def test_changed_master_fails_closed(self):
        (self.root/'fixture_master_3.bin').write_bytes(b'changed source master')
        with self.assertRaisesRegex(RuntimeError,'master changed'):verify(self.root,self.jar,self.pin)
    def test_changed_playback_settings_fail_closed(self):
        self.sounds['event_0']['sounds'][0]['volume']=.2;(self.source/'sounds.json').write_text(json.dumps(self.sounds))
        with self.assertRaisesRegex(RuntimeError,'definitions changed'):verify(self.root,self.jar,self.pin)
    def test_changed_decoded_audio_fails_closed(self):
        (self.source/'sounds/voice_0.ogg').write_bytes(b'changed audio')
        with patch('verify_prepared_audio.pcm_key',return_value='48000:1:NEW'),patch('verify_prepared_audio.packed_pcm_key',return_value='48000:1:OLD'):
            with self.assertRaisesRegex(RuntimeError,'recording changed'):verify(self.root,self.jar,self.pin)
    def test_only_proven_container_changes_are_accepted(self):
        (self.source/'sounds/voice_0.ogg').write_bytes(b'different container')
        with patch('verify_prepared_audio.pcm_key',return_value='48000:1:SAME'),patch('verify_prepared_audio.packed_pcm_key',return_value='48000:1:SAME'):
            result=verify(self.root,self.jar,self.pin)
        self.assertEqual(result['verified_container_only_changes'],1)
        self.assertEqual(result['byte_identical_referenced_recordings'],99)
    def rescue_additions(self):
        from import_rescue_chapters import sources
        # Synthetic fixtures exercise receipt policy only, not audio decoding.
        self.sounds['theatre_music']={'sounds':[{'name':'coraline:voice_0','stream':True}]}
        with zipfile.ZipFile(self.jar) as archive:prior={name:archive.read(name) for name in archive.namelist()}
        prior['assets/coraline/sounds.json']=json.dumps(self.sounds).encode()
        with zipfile.ZipFile(self.jar,'w') as archive:
            for name,data in prior.items():archive.writestr(name,data)
        self.pin=digest(self.jar);receipts={}
        for event,relative in sources().items():
            master=self.root/relative;master.parent.mkdir(parents=True,exist_ok=True);master.write_bytes(b'new supplied master fixture')
            receipt={'source':relative,'source_sha256':digest(master)}
            if event=='rescue_theatre_music':self.sounds[event]=json.loads(json.dumps(self.sounds['theatre_music']))
            else:
                recording=self.source/'sounds/rescue_chapters'/f'{event}.ogg';recording.parent.mkdir(exist_ok=True);recording.write_bytes(('new synthetic export '+event).encode())
                receipt['ogg_sha256']=digest(recording);self.sounds[event]={'sounds':[{'name':'coraline:rescue_chapters/'+event,'stream':True}]}
            receipts[event]=receipt
        (self.source/'rescue_chapters_audio.json').write_text(json.dumps(receipts));(self.source/'sounds.json').write_text(json.dumps(self.sounds))
    def test_exact_added_original_receipts_do_not_relax_prior_sound_pin(self):
        self.rescue_additions();result=verify(self.root,self.jar,self.pin,preserve_captions=True,added_original_manifest='rescue_chapters_audio.json')
        self.assertEqual(result['new_original_events'],33);self.assertEqual(result['new_original_export_receipts'],32)
        self.assertEqual(result['byte_identical_referenced_recordings'],100)
    def test_added_originals_cannot_change_old_playback(self):
        self.rescue_additions();self.sounds['event_1']['sounds'][0]['volume']=.2;(self.source/'sounds.json').write_text(json.dumps(self.sounds))
        with self.assertRaisesRegex(RuntimeError,'definitions changed'):verify(self.root,self.jar,self.pin,added_original_manifest='rescue_chapters_audio.json')
    def test_added_original_export_hash_must_match(self):
        self.rescue_additions();(self.source/'sounds/rescue_chapters/finale_a.ogg').write_bytes(b'altered new export')
        with self.assertRaisesRegex(RuntimeError,'export receipt mismatch'):verify(self.root,self.jar,self.pin,added_original_manifest='rescue_chapters_audio.json')
    def test_no_unlisted_events_can_piggyback_added_originals(self):
        self.rescue_additions();self.sounds['unaudited_extra']={'sounds':['coraline:voice_0']};(self.source/'sounds.json').write_text(json.dumps(self.sounds))
        with self.assertRaisesRegex(RuntimeError,'definitions changed'):verify(self.root,self.jar,self.pin,added_original_manifest='rescue_chapters_audio.json')

    def final_additions(self):
        from import_final_chapter_recordings import inspect_names,resource,expected_event,REVISION,RECEIPT
        self.originals={};receipts={};lengths={}
        self.measured={'ticks':80,'seconds':4.0,'sample_rate':48000,'channels':1}
        for event in inspect_names():
            relative='masters/'+event+'.wav';master=self.root/relative;master.parent.mkdir(exist_ok=True);master.write_bytes(b'synthetic master')
            self.originals[event]={'source':relative,'sha256':digest(master),'seconds':4.0,'sample_rate':48000,'channels':1}
            path=resource(event);recording=self.source/'sounds'/f'{path}.ogg';recording.parent.mkdir(exist_ok=True);recording.write_bytes(('synthetic export '+event).encode())
            receipts[event]={**self.measured,'source':relative,'source_sha256':digest(master),'ogg_sha256':digest(recording),'resource':path,'export_revision':REVISION,'decode_validated':True}
            self.sounds[event]=expected_event(event);lengths[event]=80
        (self.source/RECEIPT).write_text(json.dumps({'format':1,'recordings':receipts}))
        (self.source/'visit_audio_lengths.json').write_text(json.dumps(lengths));(self.source/'sounds.json').write_text(json.dumps(self.sounds))
    def verify_final_fixture(self):
        with patch('inspect_final_chapter_recordings.inspect_recordings',return_value=self.originals),patch('import_numbered_original_audio.ogg_measure',return_value=self.measured):
            return verify(self.root,self.jar,self.pin,preserve_captions=True,added_final_originals=True)
    def test_exact_final_originals_keep_old_archive_and_captions_pinned(self):
        self.final_additions();result=self.verify_final_fixture()
        self.assertEqual(result['new_original_events'],24);self.assertEqual(result['new_original_export_receipts'],24)
        self.assertTrue(result['existing_captions_preserved']);self.assertTrue(result['normal_processResources_still_required'])
    def test_final_originals_cannot_change_old_playback(self):
        self.final_additions();self.sounds['event_0']['sounds'][0]['volume']=.2;(self.source/'sounds.json').write_text(json.dumps(self.sounds))
        with self.assertRaisesRegex(RuntimeError,'definitions changed'):self.verify_final_fixture()
    def test_final_originals_reject_changed_export(self):
        self.final_additions();(self.source/'sounds/final_chapter_originals/crawling_hand_a.ogg').write_bytes(b'changed')
        with self.assertRaisesRegex(RuntimeError,'identity/settings mismatch'):self.verify_final_fixture()
    def test_final_originals_reject_incomplete_duration(self):
        self.final_additions();self.measured={**self.measured,'seconds':3.9}
        with self.assertRaisesRegex(RuntimeError,'full-duration/timing mismatch'):self.verify_final_fixture()
    def test_final_originals_reject_unlisted_event(self):
        self.final_additions();self.sounds['extra']={'sounds':['coraline:voice_0']};(self.source/'sounds.json').write_text(json.dumps(self.sounds))
        with self.assertRaisesRegex(RuntimeError,'definitions changed'):self.verify_final_fixture()

if __name__=='__main__':unittest.main()
