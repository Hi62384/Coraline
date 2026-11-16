import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import hashlib
from run_parent_audio_chunks import wait_ready,may_resume_ram,resume_budget,recovery_headroom,generation_start_floor,cached_reference_profiles,adopt_cached_attempts,recover_after_success


class ParentAudioChunkTests(unittest.TestCase):
    def test_exhausted_chain_requires_new_success_and_preserves_all_history(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);prior_path=root/'build-character-cast/parent-audio-chunks/old/report.json';prior_path.parent.mkdir(parents=True)
            probe=root/'build-character-cast/compact-voice/probe/report.json';probe.parent.mkdir(parents=True)
            proof=root/'build-character-cast/checkpoint-copy-proofs/copy.json';proof.parent.mkdir(parents=True)
            tools=root/'tools';tools.mkdir();(tools/'full_mother_voice_manifest.json').write_text('{}')
            sources={}
            for name in ('bounded_checkpoint_copy.py','generate_cached_voice.py','low_memory_voice_loader.py'):
                (tools/name).write_text(name);sources[name]=hashlib.sha256(name.encode()).hexdigest()
            proof.write_text(json.dumps(dict(status='passed',original_load_state_dict=True,weights_precision_unchanged=True,all_values_verified=True,source_sha256=sources)))
            prior=dict(status='failed',maximum_ram_stops=12,ram_stops=12,deadline_seconds=43200,active_scope='full_mother',
                error='Finite RAM-stop budget exhausted; no further model started',started_utc='1970-01-01T00:00:00Z',
                steps=[{'status':'low_memory_cutoff'}]*12,coverage={'expected':354,'completed':283},
                reference_cache_profile={'mother':'verified','father':'verified'},inference_page_release_mode=True)
            prior_path.write_text(json.dumps(prior));original_bytes=prior_path.read_bytes()
            report=dict(status='passed',exit_code=0,memory_measurement_available=True,stop_free_mib=384,aggregate_job_limit_mib=12288,
                stage='synthesis',manifest='tools/full_mother_voice_manifest.json',command=['--require-reference-cache'],
                bounded_checkpoint_copy_proof=str(proof),bounded_checkpoint_copy_proof_sha256=hashlib.sha256(proof.read_bytes()).hexdigest(),start_free_gib=2.8)
            probe.write_text(json.dumps(report))
            with patch('run_parent_audio_chunks.generation_state',return_value={'expected':354,'completed':288}):
                result,left=recover_after_success(root,prior_path,probe,now=3600)
                self.assertEqual(result['ram_stops'],12);self.assertEqual(result['maximum_ram_stops'],13)
                self.assertEqual(result['steps'][:12],prior['steps']);self.assertEqual(left,39600)
                self.assertEqual(result['required_generation_start_free_gib'],2.8)
                self.assertEqual(prior_path.read_bytes(),original_bytes)
                recovered=root/'build-character-cast/parent-audio-chunks/recovered/report.json';recovered.parent.mkdir(parents=True)
                recovered.write_text(json.dumps(dict(result,status='waiting_for_headroom',deadline_seconds=43200)))
                resumed,remaining=resume_budget(root,recovered,now=3700)
                self.assertEqual(resumed['ram_stops'],12);self.assertEqual(resumed['maximum_ram_stops'],13)
                self.assertEqual(remaining,39500);self.assertEqual(resumed['recovery_from_exhausted'],result['recovery_from_exhausted'])
                for change in ({'status':'low_memory_cutoff'},{'stop_free_mib':128},{'exit_code':1}):
                    probe.write_text(json.dumps(dict(report,**change)))
                    with self.assertRaisesRegex(ValueError,'successful guarded'):recover_after_success(root,prior_path,probe,now=3600)
                probe.write_text(json.dumps(report));(tools/'generate_cached_voice.py').write_text('changed')
                with self.assertRaisesRegex(ValueError,'source changed'):recover_after_success(root,prior_path,probe,now=3600)
            prior_path.write_text(json.dumps(dict(prior,maximum_ram_stops=13,ram_stops=13,recovery_from_exhausted={})))
            with self.assertRaisesRegex(ValueError,'recovered once'):recover_after_success(root,prior_path,probe,now=3600)

    def test_headroom_must_be_stable_and_wait_is_finite(self):
        now=[0]
        def sleep(seconds):now[0]+=seconds
        wait_ready(60,30,available=lambda:2.1,clock=lambda:now[0],sleep=sleep)
        self.assertEqual(now[0],30)
        now[0]=0
        with self.assertRaises(TimeoutError):wait_ready(5,available=lambda:1.99,clock=lambda:now[0],sleep=sleep)
        self.assertEqual(now[0],5)
        now[0]=0
        wait_ready(60,available=lambda:1 if now[0]==10 else 2.1,clock=lambda:now[0],sleep=sleep)
        self.assertEqual(now[0],26)

    def test_only_measured_unchanged_ram_cutoff_is_retryable(self):
        report={'status':'low_memory_cutoff','memory_measurement_available':True,'exit_code':1,'stop_free_mib':384}
        self.assertTrue(may_resume_ram(report))
        for field,value in [('status','failed'),('status','passed'),('memory_measurement_available',False),('exit_code',0),('stop_free_mib',128)]:
            with self.subTest(field=field,value=value):self.assertFalse(may_resume_ram(dict(report,**{field:value})))

    def test_approved_low_start_floor_does_not_remove_stability_or_deadline(self):
        now=[0]
        wait_ready(30,minimum_gib=640/1024,available=lambda:.7,clock=lambda:now[0],sleep=lambda n:now.__setitem__(0,now[0]+n))
        self.assertEqual(now[0],15)
        with self.assertRaises(ValueError):wait_ready(30,minimum_gib=.5)

    def test_restart_carries_original_deadline_and_consumed_retries(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);path=root/'build-character-cast/parent-audio-chunks/prior/report.json';path.parent.mkdir(parents=True)
            prior={'status':'waiting_for_headroom','started_utc':'1970-01-01T00:00:00Z','deadline_seconds':43200,
                   'maximum_ram_stops':12,'ram_stops':3,'steps':[{'status':'low_memory_cutoff'} for _ in range(3)]}
            path.write_text(json.dumps(prior))
            inherited,left=resume_budget(root,path,now=3600)
            self.assertEqual(left,39600);self.assertEqual(inherited['ram_stops'],3)
            self.assertEqual(inherited['steps'],prior['steps']);self.assertEqual(inherited['started_utc'],prior['started_utc'])
            for changes in ({'ram_stops':12},{'ram_stops':2},{'status':'generating'},{'deadline_seconds':86400}):
                path.write_text(json.dumps(dict(prior,**changes)))
                with self.subTest(changes=changes),self.assertRaises(ValueError):resume_budget(root,path,now=3600)
            path.write_text(json.dumps(prior))
            with self.assertRaises(ValueError):resume_budget(root,path,now=43200)

    def test_zero_progress_requires_more_measured_ram_without_reset_on_restart(self):
        self.assertAlmostEqual(recovery_headroom(1.5,{'start_free_gib':1.7},False),2.2)
        self.assertAlmostEqual(recovery_headroom(1.5,{'start_free_gib':1.7},True),2.2)
        self.assertEqual(recovery_headroom(3.8,{'start_free_gib':3.9},False),4)
        with self.assertRaises(ValueError):recovery_headroom(1.5,{'start_free_gib':float('nan')},False)
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);path=root/'build-character-cast/parent-audio-chunks/prior/report.json';path.parent.mkdir(parents=True)
            path.write_text(json.dumps({'status':'waiting_for_headroom','started_utc':'1970-01-01T00:00:00Z',
                'deadline_seconds':43200,'maximum_ram_stops':12,'ram_stops':7,'steps':[{'status':'low_memory_cutoff'} for _ in range(7)],
                'required_generation_start_free_gib':2.2}))
            inherited,left=resume_budget(root,path,now=3600)
            self.assertEqual(inherited['required_generation_start_free_gib'],2.2)
            self.assertEqual(inherited['ram_stops'],7);self.assertEqual(left,39600)

    def test_only_new_verified_loading_profile_can_change_old_start_floor(self):
        inherited={'required_generation_start_free_gib':2.35}
        self.assertEqual(generation_start_floor(1.5,inherited,None),2.35)
        self.assertEqual(generation_start_floor(1.5,inherited,{'verified':'profile'}),1.5)
        inherited['reference_cache_profile']={'prior':'verified profile'}
        self.assertEqual(generation_start_floor(1.5,inherited,{'regenerated':'same loading mode'}),2.35)

    def test_inference_paging_trial_changes_floor_only_once_not_retry_budget(self):
        inherited={'required_generation_start_free_gib':3.2,'reference_cache_profile':{'mother':'cached','father':'cached'},'ram_stops':11,'steps':[{'status':'low_memory_cutoff'}]*11}
        original=dict(inherited)
        self.assertEqual(generation_start_floor(2,inherited,{'verified':'cached'},True),2)
        self.assertEqual(inherited,original)
        inherited['inference_page_release_mode']=True
        self.assertEqual(generation_start_floor(2,inherited,{'verified':'cached'},True),3.2)
        with self.assertRaisesRegex(ValueError,'cannot revert'):generation_start_floor(2,inherited,{'verified':'cached'},False)
        with self.assertRaisesRegex(ValueError,'requires verified'):generation_start_floor(2,{},None,True)
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);path=root/'build-character-cast/parent-audio-chunks/prior/report.json';path.parent.mkdir(parents=True)
            prior=dict(inherited,status='waiting_for_headroom',started_utc='1970-01-01T00:00:00Z',deadline_seconds=43200,maximum_ram_stops=12)
            path.write_text(json.dumps(prior))
            resumed,left=resume_budget(root,path,now=3600)
            self.assertTrue(resumed['inference_page_release_mode']);self.assertEqual(resumed['ram_stops'],11)
            self.assertEqual(left,39600);self.assertEqual(generation_start_floor(2,resumed,{'verified':'cached'},True),3.2)

    def test_verified_flow_mode_does_not_reset_headroom_on_subsequent_restarts(self):
        inherited={'required_generation_start_free_gib':3.2,'reference_cache_profile':{'mother':'cached','father':'cached'},'inference_page_release_mode':True}
        self.assertEqual(generation_start_floor(1.5,inherited,{'verified':'cached'},True,{'verified':'flow'}),1.5)
        inherited['flow_staging_profile']={'verified':'flow'}
        self.assertEqual(generation_start_floor(1.5,inherited,{'verified':'cached'},True,{'new report':'same mode'}),3.2)
        with self.assertRaisesRegex(ValueError,'cannot revert'):generation_start_floor(1.5,inherited,{'verified':'cached'},True,None)
        with self.assertRaisesRegex(ValueError,'requires its verified'):generation_start_floor(1.5,{},None,False,{'verified':'flow'})

    def test_unmeasured_or_incomplete_reference_preparation_cannot_change_profile(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);path=root/'build-character-cast/compact-voice/prep/report.json';path.parent.mkdir(parents=True)
            path.write_text(json.dumps({'manifest':'tools/full_mother_voice_manifest.json','status':'passed','exit_code':0,
                'stage':'reference_conditioning','reference_conditioning_only':True,'certifies_speech_generation':False,
                'memory_measurement_available':False,'stop_free_mib':384}))
            with self.assertRaisesRegex(ValueError,'guarded reference preparation'):cached_reference_profiles(root,[path])

    def test_manual_probe_ram_stop_is_counted_once_and_cannot_exhaust_then_reset(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);path=root/'build-character-cast/compact-voice/probe/report.json';path.parent.mkdir(parents=True)
            report={'stage':'synthesis','command':['--require-reference-cache'],'manifest':'tools/full_mother_voice_manifest.json',
                    'status':'low_memory_cutoff','exit_code':1,'memory_measurement_available':True,
                    'stop_free_mib':384,'aggregate_job_limit_mib':12288}
            path.write_text(json.dumps(report))
            prior={'ram_stops':8,'steps':[{'wrapper_report':str(root/str(i)),'status':'low_memory_cutoff'} for i in range(8)]}
            adopted=adopt_cached_attempts(root,prior,[path])
            self.assertEqual(adopted['ram_stops'],9);self.assertEqual(prior['ram_stops'],8)
            self.assertEqual(len(adopted['steps']),9);self.assertEqual(len(prior['steps']),8)
            with self.assertRaisesRegex(ValueError,'already counted'):adopt_cached_attempts(root,adopted,[path])
            with self.assertRaisesRegex(ValueError,'budget exhausted'):adopt_cached_attempts(root,dict(prior,ram_stops=11),[path])


if __name__=='__main__':unittest.main()
