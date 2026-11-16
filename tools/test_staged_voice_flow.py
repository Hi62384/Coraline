import hashlib,json
from pathlib import Path
import tempfile,unittest
from staged_voice_flow import verify_proof


class StagedFlowProofTests(unittest.TestCase):
    def fixture(self,root,component):
        names=({'check_staged_flow.py','staged_voice_flow.py','bounded_checkpoint_copy.py','low_memory_voice_loader.py'} if component else
               {'staged_voice_flow.py','generate_staged_voice.py','generate_cached_voice.py','bounded_checkpoint_copy.py','low_memory_voice_loader.py'})
        (root/'tools').mkdir(exist_ok=True);hashes={}
        for name in names:
            (root/'tools'/name).write_text(name);hashes[name]=hashlib.sha256(name.encode()).hexdigest()
        path=root/'build-character-cast'/('flow-staging-checks' if component else 'flow-staging-proofs')/'proof.json';path.parent.mkdir(parents=True,exist_ok=True)
        proof=dict(status='passed',original_gpu_inference=True,original_precision=True,original_cache_dimensions=True,
                   all_values_verified=True,activations=2,verified_bytes=1024,verified_tensors=3,source_sha256=hashes)
        if component:proof.update(exact_output=True,exact_cpu_cuda_rng=True,certifies_speech_generation=False,steps=25,cfg=.7)
        path.write_text(json.dumps(proof));return path,proof

    def test_component_equivalence_is_distinct_from_speech_generation(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);path,proof=self.fixture(root,True)
            checked,sha=verify_proof(root,path,True)
            self.assertFalse(checked['certifies_speech_generation']);self.assertEqual(sha,hashlib.sha256(path.read_bytes()).hexdigest())
            for key,value in [('exact_output',False),('exact_cpu_cuda_rng',False),('certifies_speech_generation',True),('steps',24),('activations',1)]:
                path.write_text(json.dumps(dict(proof,**{key:value})))
                with self.subTest(key=key),self.assertRaisesRegex(ValueError,'Incomplete flow'):verify_proof(root,path,True)

    def test_staged_speech_requires_complete_copy_and_exact_source_pins(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);path,proof=self.fixture(root,False)
            verify_proof(root,path)
            for key,value in [('verified_bytes',0),('verified_tensors',0),('activations',0),('original_precision',False),('all_values_verified',False)]:
                path.write_text(json.dumps(dict(proof,**{key:value})))
                with self.subTest(key=key),self.assertRaisesRegex(ValueError,'Incomplete flow'):verify_proof(root,path)
            path.write_text(json.dumps(proof));(root/'tools/generate_staged_voice.py').write_text('changed')
            with self.assertRaisesRegex(ValueError,'source changed'):verify_proof(root,path)

    def test_unrelated_or_component_report_cannot_certify_staged_speech(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);path,proof=self.fixture(root,True)
            with self.assertRaisesRegex(ValueError,'Unexpected flow proof'):verify_proof(root,path,False)
            proof['source_sha256'].pop('staged_voice_flow.py');path.write_text(json.dumps(proof))
            with self.assertRaisesRegex(ValueError,'source proof'):verify_proof(root,path,True)


if __name__=='__main__':unittest.main()
