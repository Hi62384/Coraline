import json,sys,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
import finish_verified_staged_audio as completion

class VerifiedStagedCompletionTests(unittest.TestCase):
    def test_continuation_requires_only_immutable_successful_workers_and_expired_window(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);previous=root/'build-character-cast/verified-staged-audio/first/report.json';previous.parent.mkdir(parents=True)
            worker=root/'build-character-cast/compact-voice/one/report.json';worker.parent.mkdir(parents=True)
            worker.write_text(json.dumps({'status':'passed','exit_code':0,'stop_free_mib':384,'memory_measurement_available':True}))
            manifests={name:'{}' for name in ('tools/full_mother_voice_manifest.json','tools/incidental_father_voice_manifest.json','tools/incidental_mother_voice_manifest.json')}
            for name,content in manifests.items():
                path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(content)
            base={'status':'failed','error':'Original deadline does not permit another bounded worker','historical_speech_ram_stops':15,'retry_failed_models':False,'worker_max_new_takes':1,
                  'steps':[{'report':str(worker),'sha256':completion.pin(worker),'status':'passed','exit_code':0}],
                  'manifest_sha256':{name:completion.pin(root/name) for name in manifests}}
            with patch.object(completion,'ROOT',root):
                previous.write_text(json.dumps(base));self.assertEqual(completion.successful_deadline_checkpoint(previous)[0],previous)
                for changed in ({'status':'running'},{'error':'Worker failed'},{'continuation_from':{'report':'older'}},{'steps':[]},{'retry_failed_models':True}):
                    previous.write_text(json.dumps(dict(base,**changed)))
                    with self.subTest(changed=changed),self.assertRaises(ValueError):completion.successful_deadline_checkpoint(previous)
                previous.write_text(json.dumps(base));worker.write_text('{}')
                with self.assertRaisesRegex(ValueError,'Failed or changed worker'):completion.successful_deadline_checkpoint(previous)

    def test_no_model_can_start_from_failed_unmeasured_or_different_allocator_probe(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);probe=root/'build-character-cast/compact-voice/probe/report.json';probe.parent.mkdir(parents=True)
            base={'status':'passed','exit_code':0,'stage':'synthesis','manifest':'tools/full_mother_voice_manifest.json','stop_free_mib':384,'aggregate_job_limit_mib':12288,'memory_measurement_available':True,'requested_cuda_allocator':'cudaMallocAsync','command':['tools/generate_staged_voice.py']}
            for changes in ({'status':'low_memory_cutoff','exit_code':1},{'memory_measurement_available':False},{'requested_cuda_allocator':'native'}):
                with self.subTest(changes=changes):
                    probe.write_text(json.dumps(dict(base,**changes)))
                    with patch.object(completion,'ROOT',root),patch.object(sys,'argv',['finish_verified_staged_audio.py','--successful-probe',str(probe),'--component-report',str(root/'unused/report.json')]),patch.object(completion.subprocess,'run') as model:
                        with self.assertRaisesRegex(ValueError,'Successful guarded staged speech is required'):completion.main()
                        model.assert_not_called()

if __name__=='__main__':unittest.main()
